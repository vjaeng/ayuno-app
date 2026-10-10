package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.model.Trend
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class RepositoriesTest {

    private val w = DataWorld()

    @After
    fun tearDown() = w.database.close()

    private val rule = Rule(
        Ids.newId(), "Glucosa baja", true, RuleConditionKind.METRIC_BELOW, "glucose", null, 55.0, 20, null, null, null,
        listOf(StageKind.FAST), false, null, RuleLevel.ALERT, "Rompe con caldo.", RuleAction.OFFER_END, null,
    )

    private fun template(rules: List<Rule> = listOf(rule)) = TemplateWithPlan(
        Template(Ids.newId(), "Ayuno de 57 horas", 57.0, listOf(12.0, 24.0)),
        Plan(
            stages = listOf(Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS)),
            metrics = listOf(PlanMetric(Ids.newId(), "glucose", listOf(MetricSchedule(Ids.newId(), StageKind.FAST, LocalTime.of(7, 0))))),
            rules = rules,
        ),
    )

    @Test
    fun `H-007 la base nace con el catalogo precargado y los rangos de presion configurados`() = runTest {
        val codes = w.catalog.metricTypes().map { it.code }
        assertEquals(listOf("glucose", "ketones", "blood_pressure", "weight", "waist", "temperature", "hunger", "energy", "mood", "mental_clarity"), codes)
        val weight = w.catalog.metricType("weight")!!
        assertEquals("lb", weight.altUnit)
        assertTrue(weight.active)
        assertEquals(50.0..250.0, w.catalog.bloodPressureRanges().systolic)
    }

    @Test
    fun `H-007 desactivar un tipo se persiste`() = runTest {
        w.catalog.saveMetricType(w.catalog.metricType("ketones")!!.copy(active = false))
        assertEquals(false, w.catalog.metricType("ketones")!!.active)
    }

    @Test
    fun `una entidad de plan sin propietario o con dos propietarios se rechaza`() {
        val db = w.database.openHelper.writableDatabase
        val noOwner = runCatching {
            db.execSQL("INSERT INTO stage (id, template_id, fast_id, kind, duration_value, duration_unit, created_at) VALUES ('s', NULL, NULL, 'post', 1, 'days', 'x')")
        }
        assertTrue(noOwner.isFailure)
        val twoOwners = runCatching {
            db.execSQL("INSERT INTO rule (id, template_id, fast_id, name, active, condition_kind, filter_stage_kinds, filter_on_waking, level, message, action, created_at) VALUES ('r', 't', 'f', 'n', 1, 'manual', '[]', 0, 'alert', 'm', 'none', 'x')")
        }
        assertTrue(twoOwners.isFailure)
    }

    @Test
    fun `H-001 la plantilla y su plan se guardan y se leen completos`() = runTest {
        val t = template()
        w.saveTemplate(t)
        val stored = w.templates.get(t.template.id)!!
        assertEquals(t.template, stored.template)
        assertEquals(t.plan, stored.plan)
    }

    @Test
    fun `H-001 al quitar una regla o un horario de la plantilla se borran en blando y conservan created_at los demas`() = runTest {
        val t = template()
        w.saveTemplate(t)
        val created = w.database.planDao().stages(t.template.id, null).single().createdAt
        w.now = w.now.plusSeconds(3600)
        val metric = t.plan.metrics.single()
        w.saveTemplate(t.copy(plan = t.plan.copy(rules = emptyList(), metrics = listOf(metric.copy(schedules = emptyList())))))
        val stored = w.templates.get(t.template.id)!!
        assertTrue(stored.plan.rules.isEmpty())
        assertTrue(stored.plan.metrics.single().schedules.isEmpty())
        val stage = w.database.planDao().stages(t.template.id, null).single()
        assertEquals(created, stage.createdAt)
        assertNotNull(stage.updatedAt)
        val deletedRule = w.database.openHelper.readableDatabase.query("SELECT deleted_at FROM rule WHERE id = '${rule.id}'")
        deletedRule.moveToFirst()
        assertNotNull(deletedRule.getString(0))
    }

    @Test
    fun `H-004 crear un ayuno copia el plan en la base y H-005 lo empieza`() = runTest {
        val t = template()
        w.saveTemplate(t)
        val created = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, "notas")) as Outcome.Ok).value
        val stored = w.fasts.get(created.fast.id)!!
        assertEquals(created.fast, stored.fast)
        assertEquals(created.plan, stored.plan)
        assertEquals(1, stored.plan.rules.size)
        w.now = Instant.parse("2026-10-23T03:10:05Z")
        w.startFast(created.fast.id)
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 10, 5), w.fasts.get(created.fast.id)!!.fast.clockStartActual)
        // La plantilla sigue intacta con su propia regla.
        assertEquals(rule.id, w.templates.get(t.template.id)!!.plan.rules.single().id)
    }

    @Test
    fun `H-005 la correccion de la hora de inicio queda en edit_log`() = runTest {
        val t = template(); w.saveTemplate(t)
        val fast = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast
        w.now = Instant.parse("2026-10-23T03:10:00Z"); w.startFast(fast.id)
        w.correctClockStart(fast.id, LocalDateTime.of(2026, 10, 22, 22, 0), "Lo pulsé tarde")
        val entry = w.audit.entriesFor(fast.id).single()
        assertEquals("2026-10-22T22:10:00", entry.oldValue)
        assertEquals("Lo pulsé tarde", entry.reason)
    }

    @Test
    fun `H-008 H-019 H-020 H-027 medicion, presion y alerta se persisten con todos sus campos`() = runTest {
        val t = template(); w.saveTemplate(t)
        val fast = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast
        w.now = Instant.parse("2026-10-23T03:10:00Z"); w.startFast(fast.id)
        val t0 = LocalDateTime.of(2026, 10, 23, 12, 0, 15)
        w.recordMeasurement(RecordMeasurement.Input(fast.id, "glucose", t0, t0.withSecond(0), 52.0, "mg/dL", source = GlucoseSource.CGM, trend = Trend.FALLING, onWaking = true, note = "temblor"))
        val second = w.recordMeasurement(RecordMeasurement.Input(fast.id, "glucose", t0.plusMinutes(20), null, 53.0, "mg/dL", source = GlucoseSource.CGM))
        val alerts = ((second as Outcome.Ok).value as RecordResult.Saved).alerts
        assertEquals(1, alerts.size)

        val stored = w.records.measurements(fast.id)
        assertEquals(t0, stored[0].takenAt)
        assertEquals(Trend.FALLING, stored[0].trend)
        assertTrue(stored[0].onWaking)
        assertEquals(stored.map { it.id }, w.alerts.alerts(fast.id).single().triggerEntryIds)

        w.decideAlert(alerts.single().id, UserChoice.CONTINUE, "Vuelvo a medir")
        assertEquals("Vuelvo a medir", w.alerts.get(alerts.single().id)!!.choiceReason)

        val sitting = ((w.recordBloodPressure(RecordBloodPressure.Input(fast.id, t0, systolic = 118, diastolic = 76, pulse = 64, position = Position.SITTING)) as Outcome.Ok).value as RecordResult.Saved).entry
        w.recordBloodPressure(RecordBloodPressure.Input(fast.id, t0.plusMinutes(1), systolic = 104, diastolic = 70, position = Position.STANDING, pairedReadingId = sitting.id))
        val readings = w.records.bloodPressure(fast.id)
        assertEquals(sitting, readings.first())
        assertEquals(sitting.id, readings.last().pairedReadingId)
        assertNull(readings.last().pulse)
    }

    @Test
    fun `H-030 el fin del ayuno se persiste`() = runTest {
        val t = template(); w.saveTemplate(t)
        val fast = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast
        w.now = Instant.parse("2026-10-23T03:10:00Z"); w.startFast(fast.id)
        w.now = Instant.parse("2026-10-25T12:10:00Z")
        val end = com.ayuno.domain.usecase.EndFast(w.fasts, w.refeed, w.tx, w.clock)
        end(com.ayuno.domain.usecase.EndFast.Input(fast.id, LocalDateTime.of(2026, 10, 25, 7, 10), EndReason.GOAL_MET, null, null))
        val stored = w.fasts.get(fast.id)!!.fast
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 10), stored.endActual)
        assertEquals(EndReason.GOAL_MET, stored.endReason)
    }
}
