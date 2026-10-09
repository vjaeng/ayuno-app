package com.ayuno.domain.usecase

import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.StageNotice
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

class Batch2UseCasesTest {

    private val at = LocalDateTime.of(2026, 10, 23, 12, 0)

    // ---------- H-002 ----------

    @Test
    fun `H-002 metas, lista previa y avisos se validan y se copian al ayuno`() = runTest {
        val w = TestWorld()
        val bad = w.plan57h().copy(
            goals = listOf(IntakeGoal(Ids.newId(), "salt", 0.0, "g")),
            checklist = listOf(ChecklistItem(Ids.newId(), " ", false)),
            notices = listOf(StageNotice(Ids.newId(), "No sacarse sangre", emptyList())),
        )
        val errors = (w.saveTemplate(com.ayuno.domain.model.TemplateWithPlan(com.ayuno.domain.model.Template(Ids.newId(), "X", 24.0, emptyList()), bad)) as Outcome.Invalid).errors
        assertTrue(errors.containsAll(listOf(ValidationError.GOAL_NOT_POSITIVE, ValidationError.TEXT_REQUIRED, ValidationError.NOTICE_STAGES_REQUIRED)))

        val good = w.plan57h().copy(
            goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g")),
            checklist = listOf(ChecklistItem(Ids.newId(), "Comprar sal", false), ChecklistItem(Ids.newId(), "Cargar el tensiómetro", false)),
            notices = listOf(StageNotice(Ids.newId(), "No sacarse sangre", listOf(StageKind.FAST, StageKind.POST))),
        )
        val fast = w.plannedFast(good)
        assertEquals(1, fast.plan.goals.size)
        assertEquals(2, fast.plan.checklist.size)
        assertEquals(listOf(StageKind.FAST, StageKind.POST), fast.plan.notices.single().stageKinds)
    }

    @Test
    fun `H-002 marcar la lista previa no impide empezar el ayuno`() = runTest {
        val w = TestWorld()
        val fast = w.plannedFast(w.plan57h().copy(checklist = listOf(ChecklistItem(Ids.newId(), "Comprar sal", false))))
        w.setChecklistItem(fast.fast.id, fast.plan.checklist.single().id, true)
        assertTrue(w.fasts.get(fast.fast.id)!!.plan.checklist.single().isChecked)
        val other = w.plannedFast(w.plan57h().copy(checklist = listOf(ChecklistItem(Ids.newId(), "Sin marcar", false))))
        assertTrue(w.startFast(other.fast.id) is Outcome.Ok)
    }

    // ---------- H-010 / H-021 ----------

    @Test
    fun `H-010 catalogo de sintomas editable y registro con intensidad`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val nausea = (w.saveSymptomType(SaveSymptomType.Input(null, "Náusea", "Esperables", false)) as Outcome.Ok).value
        assertEquals("nausea", nausea.code)
        assertEquals(listOf(ValidationError.NAME_DUPLICATED), (w.saveSymptomType(SaveSymptomType.Input(null, "náusea", null, false)) as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.SEVERITY_OUT_OF_RANGE), (w.recordSymptom(RecordSymptom.Input(f.id, "nausea", at, 11, null)) as Outcome.Invalid).errors)
        w.recordSymptom(RecordSymptom.Input(f.id, "nausea", at, 4, " leve "))
        val s = w.records.symptoms.value.single()
        assertEquals(4, s.severity)
        assertEquals("leve", s.note)
    }

    @Test
    fun `H-021 un sintoma de alarma dispara su regla con cualquier valor y un sintoma normal no`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(0.0).copy(conditionKind = RuleConditionKind.ALARM_SYMPTOM, metricTypeCode = null, threshold = null)
        val f = w.startedFast(w.plan57h(listOf(rule)))
        val normal = w.recordSymptom(RecordSymptom.Input(f.id, "headache", at, 6, null))
        assertTrue(((normal as Outcome.Ok).value as RecordResult.Saved).alerts.isEmpty())
        val alarm = w.recordSymptom(RecordSymptom.Input(f.id, "blurred_vision", at.plusMinutes(1), 3, null))
        val alerts = ((alarm as Outcome.Ok).value as RecordResult.Saved).alerts
        assertEquals(listOf(w.records.symptoms.value.last().id), alerts.single().triggerEntryIds)
        // Otra lectura cualquiera no repite la alerta del mismo síntoma.
        w.recordSymptom(RecordSymptom.Input(f.id, "headache", at.plusMinutes(2), 2, null))
        assertEquals(1, w.alerts.items.value.size)
    }

    @Test
    fun `H-021 una regla para un sintoma concreto ignora otros sintomas de alarma`() = runTest {
        val w = TestWorld()
        w.saveSymptomType(SaveSymptomType.Input(null, "Confusión", "Alarma", true))
        val rule = w.glucoseBelow(0.0).copy(conditionKind = RuleConditionKind.ALARM_SYMPTOM, metricTypeCode = null, threshold = null, symptomTypeCode = "confusion")
        val f = w.startedFast(w.plan57h(listOf(rule)))
        w.recordSymptom(RecordSymptom.Input(f.id, "blurred_vision", at, 3, null))
        assertTrue(w.alerts.items.value.isEmpty())
        w.recordSymptom(RecordSymptom.Input(f.id, "confusion", at, 3, null))
        assertEquals(1, w.alerts.items.value.size)
    }

    // ---------- H-011 / H-012 / H-022 ----------

    @Test
    fun `H-011 tipos de ingesta con rompe el ayuno y factor de sodio editable`() = runTest {
        val w = TestWorld()
        val tea = (w.saveIntakeType(SaveIntakeType.Input(null, "Infusión", "mL", false, null)) as Outcome.Ok).value
        assertEquals("infusion", tea.code)
        w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.4))
        assertEquals(0.4, w.catalog.intakeTypes().first { it.code == "salt" }.sodiumFactor)
        assertEquals(listOf(ValidationError.SODIUM_FACTOR_NOT_POSITIVE), (w.saveIntakeType(SaveIntakeType.Input(null, "X", "g", false, 0.0)) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-012 5 g de sal muestran 1_97 g de sodio y los totales se comparan con la meta`() = runTest {
        val w = TestWorld()
        val f = w.startedFast(w.plan57h().copy(goals = listOf(IntakeGoal(Ids.newId(), "water", 2.0, "L"))))
        w.recordIntake(RecordIntake.Input(f.id, "salt", at, 5.0, null, null))
        w.recordIntake(RecordIntake.Input(f.id, "water", at, 1200.0, "mL", null))
        w.recordIntake(RecordIntake.Input(f.id, "water", at.plusDays(1), 900.0, "mL", null))
        val totals = DailyTotals.of(at.toLocalDate(), w.records.intakes.value, w.catalog.intakeTypes(), w.fasts.get(f.id)!!.plan.goals)
        assertEquals(1.97, totals.first { it.intakeType.code == "salt" }.sodium!!, 0.005)
        val water = totals.first { it.intakeType.code == "water" }
        assertEquals(1200.0, water.amount)
        assertEquals(1.2, DailyTotals.amountForGoal(water)!!, 0.0001)
        assertEquals(listOf(ValidationError.AMOUNT_NOT_POSITIVE), (w.recordIntake(RecordIntake.Input(f.id, "water", at, 0.0, null, null)) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-022 a partir de su hora, si el total no llega a la meta, salta una vez al dia`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(0.0).copy(
            conditionKind = RuleConditionKind.INTAKE_BELOW_GOAL, metricTypeCode = null, threshold = null,
            intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0),
        )
        val f = w.startedFast(w.plan57h(listOf(rule)).copy(goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g"))))
        w.clock.instant = Instant.parse("2026-10-23T18:00:00Z") // 13:00 local
        assertTrue(w.evaluateRules(f.id).isEmpty())
        w.clock.instant = Instant.parse("2026-10-23T19:00:00Z") // 14:00 local
        w.recordIntake(RecordIntake.Input(f.id, "salt", LocalDateTime.of(2026, 10, 23, 9, 0), 2.0, null, null))
        assertEquals(1, w.alerts.items.value.size)
        assertTrue(w.evaluateRules(f.id).isEmpty())
    }

    @Test
    fun `H-022 si la meta se alcanza antes de la hora no salta`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(0.0).copy(
            conditionKind = RuleConditionKind.INTAKE_BELOW_GOAL, metricTypeCode = null, threshold = null,
            intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0),
        )
        val f = w.startedFast(w.plan57h(listOf(rule)).copy(goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g"))))
        w.clock.instant = Instant.parse("2026-10-23T18:00:00Z")
        w.recordIntake(RecordIntake.Input(f.id, "salt", LocalDateTime.of(2026, 10, 23, 12, 0), 5.0, null, null))
        w.clock.instant = Instant.parse("2026-10-23T19:30:00Z")
        assertTrue(w.evaluateRules(f.id).isEmpty())
    }

    // ---------- H-023 ----------

    @Test
    fun `H-023 algo no va bien usa la regla manual del plan y queda registrado`() = runTest {
        val w = TestWorld()
        val manual = w.glucoseBelow(0.0).copy(conditionKind = RuleConditionKind.MANUAL, metricTypeCode = null, threshold = null, message = "Para y respira")
        val f = w.startedFast(w.plan57h(listOf(manual)))
        val alert = (w.triggerManualAlert(f.id, "Algo no va bien", "texto") as Outcome.Ok).value
        assertEquals(w.fasts.get(f.id)!!.plan.rules.single().id, alert.ruleId)
        assertTrue(alert.triggerEntryIds.isEmpty())
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(f.id)!!.fast.status)
    }

    @Test
    fun `H-023 sin regla manual se crea una en el plan del ayuno con el texto de la app`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val alert = (w.triggerManualAlert(f.id, "Algo no va bien", "Detente y valora si terminar el ayuno.") as Outcome.Ok).value
        val rule = w.fasts.get(f.id)!!.plan.rules.single()
        assertEquals(RuleConditionKind.MANUAL, rule.conditionKind)
        assertEquals("Detente y valora si terminar el ayuno.", rule.message)
        assertEquals(rule.id, alert.ruleId)
        assertTrue(w.templates.items.value.values.single().plan.rules.isEmpty())
    }

    @Test
    fun `H-023 solo una regla manual por plantilla`() = runTest {
        val w = TestWorld()
        val manual = w.glucoseBelow(0.0).copy(conditionKind = RuleConditionKind.MANUAL, metricTypeCode = null, threshold = null)
        val result = w.saveTemplate(com.ayuno.domain.model.TemplateWithPlan(com.ayuno.domain.model.Template(Ids.newId(), "X", 24.0, emptyList()), w.plan57h(listOf(manual, manual.copy(id = Ids.newId())))))
        assertEquals(listOf(ValidationError.RULE_MANUAL_DUPLICATED), (result as Outcome.Invalid).errors)
    }

    // ---------- H-013 / H-014 / H-015 ----------

    private suspend fun TestWorld.vitaminC() = (saveProduct(SaveProduct.Input(null, null, "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", false,
        listOf(LocalTime.of(8, 30) to 1.0, LocalTime.of(20, 0) to 3.0))) as Outcome.Ok).value

    @Test
    fun `H-013 producto con codigo inmutable, horarios y marca a evitar`() = runTest {
        val w = TestWorld()
        val c = w.vitaminC()
        assertEquals("vitamina_c", c.code)
        assertEquals(2, c.schedules.size)
        val edited = (w.saveProduct(SaveProduct.Input("vitamina_c", "otro_codigo", "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", true, listOf(LocalTime.of(8, 30) to 1.0))) as Outcome.Ok).value
        assertEquals("vitamina_c", edited.code)
        assertTrue(edited.avoid)
        assertEquals(c.schedules.first().id, edited.schedules.single().id)
        assertEquals(listOf(ValidationError.CODE_INVALID), (w.saveProduct(SaveProduct.Input(null, "Mal Código", "Y", ProductKind.MEDICATION, 1.0, "mg", false, emptyList())) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-014 solo se recuerdan los productos que siguen y los opcionales activados`() = runTest {
        val w = TestWorld()
        w.vitaminC()
        (w.saveProduct(SaveProduct.Input(null, "omega3", "Omega 3", ProductKind.SUPPLEMENT, 2.0, "cápsulas", false, listOf(LocalTime.of(8, 30) to 2.0))) as Outcome.Ok)
        (w.saveProduct(SaveProduct.Input(null, "taurina", "Taurina", ProductKind.SUPPLEMENT, 2.0, "cápsulas", false, listOf(LocalTime.of(12, 30) to 2.0))) as Outcome.Ok)
        val plan = w.plan57h().copy(products = listOf(
            PlanProduct(Ids.newId(), "vitamina_c", DuringFast.CONTINUES, false, null),
            PlanProduct(Ids.newId(), "omega3", DuringFast.STOPS, false, "Aceite"),
            PlanProduct(Ids.newId(), "taurina", DuringFast.OPTIONAL, false, null),
        ))
        val source = w.fasts.get(w.startedFast(plan).id)!!
        val day = LocalDateTime.of(2026, 10, 23, 0, 0)
        val items = Pending.items(source, emptySet(), w.catalog.products(), day, day.plusDays(1)).filter { it.kind == PendingKind.PRODUCT }
        assertEquals(listOf("vitamina_c", "vitamina_c"), items.map { it.code })
        assertEquals(listOf(1.0, 3.0), items.map { it.amount })
    }

    @Test
    fun `H-015 tomado, no tomado con motivo y dosis cambiada sin borrar la planificada`() = runTest {
        val w = TestWorld(); w.vitaminC()
        val f = w.startedFast()
        val at830 = LocalDateTime.of(2026, 10, 23, 8, 30)
        w.recordProductLog(RecordProductLog.Input(f.id, "vitamina_c", at830, ProductLogStatus.TAKEN, at830.plusMinutes(3)))
        val taken = w.records.productLogs.value.single()
        assertEquals(1.0, taken.amount); assertEquals(1.0, taken.plannedAmount); assertNull(taken.reason)

        val at20 = LocalDateTime.of(2026, 10, 23, 20, 0)
        assertEquals(listOf(ValidationError.REASON_REQUIRED), (w.recordProductLog(RecordProductLog.Input(f.id, "vitamina_c", at20, ProductLogStatus.NOT_TAKEN, null)) as Outcome.Invalid).errors)
        w.recordProductLog(RecordProductLog.Input(f.id, "vitamina_c", at20, ProductLogStatus.DOSE_CHANGED, at20, 1.5, "Diarrea"))
        val changed = w.records.productLogs.value.last()
        assertEquals(3.0, changed.plannedAmount); assertEquals(1.5, changed.amount); assertEquals("Diarrea", changed.reason)

        val state = Pending.status(com.ayuno.domain.derived.PendingItem(PendingKind.PRODUCT, "vitamina_c", at830), at830.plusHours(1), PendingRecords(productLogs = w.records.productLogs.value)).state
        assertEquals(PendingState.DONE, state)
    }

    // ---------- H-035 ----------

    @Test
    fun `H-035 nota libre con hora`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.TEXT_REQUIRED), (w.recordNote(f.id, at, "  ") as Outcome.Invalid).errors)
        w.recordNote(f.id, at, " leve temblor ")
        assertEquals("leve temblor", w.records.notes.value.single().text)
        assertFalse(w.records.notes.value.single().notedAt.nano != 0)
    }
}
