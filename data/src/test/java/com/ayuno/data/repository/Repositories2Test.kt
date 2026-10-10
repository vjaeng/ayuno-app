package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.StageNotice
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordIntake
import com.ayuno.domain.usecase.RecordNote
import com.ayuno.domain.usecase.RecordProductLog
import com.ayuno.domain.usecase.RecordSymptom
import com.ayuno.domain.usecase.SaveProduct
import com.ayuno.domain.usecase.SetChecklistItem
import com.ayuno.domain.usecase.TriggerManualAlert
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class Repositories2Test {

    private val w = DataWorld()

    @After
    fun tearDown() = w.database.close()

    private val at = LocalDateTime.of(2026, 10, 23, 12, 0, 5)

    private suspend fun fastWith(plan: Plan): String {
        val t = TemplateWithPlan(Template(Ids.newId(), "T", 57.0, emptyList()), plan)
        w.saveTemplate(t)
        return (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast.id
    }

    @Test
    fun `H-010 H-011 la base nace con sintomas y tipos de ingesta de ejemplo`() = runTest {
        assertEquals(listOf("cold", "dizziness", "headache", "hunger", "insomnia", "nausea"), w.catalog.symptomTypes().map { it.code }.sorted())
        val salt = w.catalog.intakeTypes().first { it.code == "salt" }
        assertEquals(0.393, salt.sodiumFactor!!, 0.0)
        assertTrue(w.catalog.intakeTypes().first { it.code == "broth" }.breaksFast)
    }

    @Test
    fun `H-013 producto con horarios se guarda y quitar un horario lo borra en blando`() = runTest {
        val save = SaveProduct(w.catalog)
        save(SaveProduct.Input(null, null, "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", false, listOf(LocalTime.of(8, 30) to 1.0, LocalTime.of(20, 0) to 3.0)))
        assertEquals(2, w.catalog.product("vitamina_c")!!.schedules.size)
        save(SaveProduct.Input("vitamina_c", null, "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", true, listOf(LocalTime.of(8, 30) to 1.0)))
        val stored = w.catalog.product("vitamina_c")!!
        assertEquals(listOf(LocalTime.of(8, 30)), stored.schedules.map { it.timeOfDay })
        assertTrue(stored.avoid)
    }

    @Test
    fun `H-002 H-014 metas, lista previa, avisos y productos del plan se guardan y se copian al ayuno`() = runTest {
        SaveProduct(w.catalog)(SaveProduct.Input(null, null, "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", false, listOf(LocalTime.of(8, 30) to 1.0)))
        val plan = Plan(
            stages = emptyList(), metrics = emptyList(), rules = emptyList(),
            goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g")),
            checklist = listOf(ChecklistItem(Ids.newId(), "Comprar sal", false)),
            notices = listOf(StageNotice(Ids.newId(), "No sacarse sangre", listOf(StageKind.FAST, StageKind.POST))),
            products = listOf(PlanProduct(Ids.newId(), "vitamina_c", DuringFast.CONTINUES, false, "Sin calorías")),
        )
        val fastId = fastWith(plan)
        val copy = w.fasts.get(fastId)!!.plan
        assertEquals(4.0, copy.goals.single().dailyAmount, 0.0)
        assertEquals("Comprar sal", copy.checklist.single().text)
        assertEquals(listOf(StageKind.FAST, StageKind.POST), copy.notices.single().stageKinds)
        assertEquals(DuringFast.CONTINUES, copy.products.single().duringFast)

        SetChecklistItem(w.fasts)(fastId, copy.checklist.single().id, true)
        assertTrue(w.fasts.get(fastId)!!.plan.checklist.single().isChecked)
    }

    @Test
    fun `H-010 H-012 H-015 H-035 sintomas, ingestas, tomas y notas se persisten`() = runTest {
        SaveProduct(w.catalog)(SaveProduct.Input(null, null, "Magnesio", ProductKind.SUPPLEMENT, 400.0, "mg", false, listOf(LocalTime.of(20, 0) to 400.0)))
        val fastId = fastWith(Plan.EMPTY)
        RecordSymptom(w.catalog, w.fasts, w.records, w.alerts, w.tx, w.clock)(RecordSymptom.Input(fastId, "headache", at, 5, "leve"))
        RecordIntake(w.catalog, w.fasts, w.records, w.alerts, w.tx, w.clock)(RecordIntake.Input(fastId, "salt", at, 5.0, null, null))
        val at20 = LocalDateTime.of(2026, 10, 23, 20, 0)
        RecordProductLog(w.catalog, w.fasts, w.records, w.tx)(RecordProductLog.Input(fastId, "magnesio", at20, ProductLogStatus.DOSE_CHANGED, at20, 200.0, "Diarrea"))
        RecordNote(w.fasts, w.records)(fastId, at, "Todo bien")

        assertEquals(5, w.records.symptoms(fastId).single().severity)
        assertEquals(at, w.records.symptoms(fastId).single().takenAt)
        assertEquals("g", w.records.intakes(fastId).single().unit)
        val log = w.records.productLogs(fastId).single()
        assertEquals(400.0, log.plannedAmount!!, 0.0)
        assertEquals(200.0, log.amount!!, 0.0)
        assertEquals(at20, log.scheduledAt)
    }

    @Test
    fun `H-023 la regla manual creada al vuelo queda en el plan del ayuno y la alerta registrada`() = runTest {
        val fastId = fastWith(Plan.EMPTY)
        val alert = (TriggerManualAlert(w.fasts, w.alerts, w.tx, w.clock)(fastId, "Algo no va bien", "Detente.") as Outcome.Ok).value
        val rule = w.fasts.get(fastId)!!.plan.rules.single()
        assertEquals(rule.id, alert.ruleId)
        assertEquals(alert, w.alerts.get(alert.id))
    }

    @Test
    fun `H-017 el silencio nocturno se guarda y se lee`() = runTest {
        val repo = RoomSettingsRepository(w.database)
        assertEquals(com.ayuno.domain.model.Settings(), repo.get())
        com.ayuno.domain.usecase.SaveQuietHours(repo)(LocalTime.of(23, 0), LocalTime.of(6, 30))
        assertEquals(LocalTime.of(23, 0), repo.get().quietStart)
        assertEquals(LocalTime.of(6, 30), repo.get().quietEnd)
    }

    @Test
    fun `las nuevas entidades de plan tambien exigen un unico propietario`() {
        val db = w.database.openHelper.writableDatabase
        listOf(
            "INSERT INTO intake_goal (id, template_id, fast_id, intake_type_code, daily_amount, unit, created_at) VALUES ('g', NULL, NULL, 'salt', 4, 'g', 'x')",
            "INSERT INTO plan_product (id, template_id, fast_id, product_code, during_fast, optional_enabled, created_at) VALUES ('p', 't', 'f', 'c', 'continues', 0, 'x')",
        ).forEach { sql -> assertTrue(sql, runCatching { db.execSQL(sql) }.isFailure) }
    }
}
