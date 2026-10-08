package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class TemplateAndFastUseCasesTest {

    // ---------- H-001 ----------

    @Test
    fun `H-001 crear una plantilla con nombre, objetivo, etapas, hitos y horarios`() = runTest {
        val w = TestWorld()
        val saved = w.template()
        val stored = w.templates.get(saved.template.id)!!
        assertEquals("Ayuno de 57 horas", stored.template.name)
        assertEquals(57.0, stored.template.targetHours)
        assertEquals(listOf(12.0, 24.0, 36.0, 48.0), stored.template.milestoneHours)
        assertEquals(setOf(StageKind.PREPARATION, StageKind.REFEED, StageKind.POST), stored.plan.stages.map { it.kind }.toSet())
        assertEquals(4, stored.plan.metrics.first { it.metricTypeCode == "glucose" }.schedules.size)
    }

    @Test
    fun `H-001 validaciones de plantilla`() = runTest {
        val w = TestWorld()
        val bad = TemplateWithPlan(
            Template(Ids.newId(), " ", 0.0, listOf(-1.0)),
            Plan(
                stages = listOf(Stage(Ids.newId(), StageKind.PREPARATION, null, DurationUnit.DAYS)),
                metrics = listOf(PlanMetric(Ids.newId(), "glucose", listOf(MetricSchedule(Ids.newId(), StageKind.REFEED, LocalTime.NOON)))),
                rules = emptyList(),
            ),
        )
        val errors = (w.saveTemplate(bad) as Outcome.Invalid).errors
        assertTrue(errors.containsAll(listOf(
            ValidationError.NAME_REQUIRED, ValidationError.TARGET_HOURS_NOT_POSITIVE, ValidationError.MILESTONE_NOT_POSITIVE,
            ValidationError.STAGE_DURATION_REQUIRED, ValidationError.SCHEDULE_STAGE_NOT_USED,
        )))
    }

    @Test
    fun `H-019 reglas no soportadas aun se rechazan al guardar la plantilla`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(55.0, action = RuleAction.SCHEDULE_MEASUREMENT).copy(message = "")
        val errors = (w.saveTemplate(TemplateWithPlan(Template(Ids.newId(), "X", 24.0, emptyList()), w.plan57h(listOf(rule)))) as Outcome.Invalid).errors
        assertTrue(errors.containsAll(listOf(ValidationError.RULE_ACTION_UNSUPPORTED, ValidationError.RULE_MESSAGE_REQUIRED)))
    }

    // ---------- H-004 ----------

    @Test
    fun `H-004 el ayuno nace planificado con copia del plan y no cambia si cambia la plantilla`() = runTest {
        val w = TestWorld()
        val created = w.plannedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        assertEquals(FastStatus.PLANNED, created.fast.status)
        assertEquals(LocalDateTime.of(2026, 10, 22, 18, 0), created.fast.lastMealPlanned)
        assertNull(created.fast.lastMealActual)
        val template = w.templates.items.value.values.single()
        assertNotEquals(template.plan.rules.single().id, created.plan.rules.single().id)
        w.saveTemplate(template.copy(template = template.template.copy(targetHours = 36.0), plan = Plan.EMPTY))
        val stored = w.fasts.get(created.fast.id)!!
        assertEquals(57.0, stored.fast.targetHours)
        assertEquals(1, stored.plan.rules.size)
        assertEquals(3, stored.plan.stages.size)
    }

    @Test
    fun `H-004 se puede crear un ayuno desde cero`() = runTest {
        val w = TestWorld()
        val result = w.createFast(CreateFast.Input(null, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 36.0, "  primera vez "))
        val fast = (result as Outcome.Ok).value
        assertNull(fast.fast.templateId)
        assertEquals(Plan.EMPTY, fast.plan)
        assertEquals("primera vez", fast.fast.notes)
    }

    @Test
    fun `H-004 el inicio del reloj no puede ser anterior a la ultima comida`() = runTest {
        val result = TestWorld().createFast(CreateFast.Input(null, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 17, 0), 36.0, null))
        assertEquals(listOf(ValidationError.CLOCK_START_BEFORE_LAST_MEAL), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `H-004 notas generales del ayuno editables`() = runTest {
        val w = TestWorld()
        val fast = w.plannedFast().fast
        UpdateFastNotes(w.fasts)(fast.id, "Semana tranquila")
        assertEquals("Semana tranquila", w.fasts.get(fast.id)!!.fast.notes)
    }

    // ---------- H-005 ----------

    @Test
    fun `H-005 empezar guarda la hora real con segundos y el desfase, y pasa a en curso`() = runTest {
        val w = TestWorld()
        val planned = w.plannedFast()
        w.clock.instant = java.time.Instant.parse("2026-10-23T03:10:07.900Z")
        val started = (w.startFast(planned.fast.id) as Outcome.Ok).value
        assertEquals(FastStatus.IN_PROGRESS, started.status)
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 10, 7), started.clockStartActual)
        assertEquals("-05:00", started.tzOffset)
    }

    @Test
    fun `H-005 solo puede haber un ayuno en curso`() = runTest {
        val w = TestWorld()
        w.startedFast()
        val second = w.plannedFast()
        assertEquals(listOf(ValidationError.ANOTHER_FAST_IN_PROGRESS), (w.startFast(second.fast.id) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-005 corregir la hora de inicio exige motivo y deja el valor anterior en la bitacora`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertEquals(listOf(ValidationError.REASON_REQUIRED), (w.correctClockStart(fast.id, LocalDateTime.of(2026, 10, 22, 22, 0), " ") as Outcome.Invalid).errors)
        w.correctClockStart(fast.id, LocalDateTime.of(2026, 10, 22, 22, 0), "Lo pulsé tarde")
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 0), w.fasts.get(fast.id)!!.fast.clockStartActual)
        val entry = w.audit.entries.single()
        assertEquals("clock_start_actual", entry.field)
        assertEquals("2026-10-22T22:10:00", entry.oldValue)
        assertEquals("2026-10-22T22:00:00", entry.newValue)
        assertEquals("Lo pulsé tarde", entry.reason)
    }

    @Test
    fun `H-005 registrar la ultima comida real y corregirla deja rastro`() = runTest {
        val w = TestWorld()
        val fast = w.plannedFast().fast
        w.registerLastMeal(fast.id, fast.lastMealPlanned)
        assertEquals(fast.lastMealPlanned, w.fasts.get(fast.id)!!.fast.lastMealActual)
        assertTrue(w.audit.entries.isEmpty())
        w.registerLastMeal(fast.id, LocalDateTime.of(2026, 10, 22, 18, 25))
        assertEquals(LocalDateTime.of(2026, 10, 22, 18, 25), w.fasts.get(fast.id)!!.fast.lastMealActual)
        assertEquals("last_meal_actual", w.audit.entries.single().field)
    }

    // ---------- H-030 ----------

    @Test
    fun `H-030 terminar guarda hora real y motivo y el ayuno no se reanuda`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.clock.advanceMinutes(57 * 60)
        val end = LocalDateTime.of(2026, 10, 25, 7, 10)
        val ended = (w.endFast(EndFast.Input(fast.id, end, EndReason.GOAL_MET, null, null)) as Outcome.Ok).value
        assertEquals(FastStatus.FINISHED, ended.status)
        assertEquals(end, ended.endActual)
        assertEquals(EndReason.GOAL_MET, ended.endReason)
        assertEquals(listOf(ValidationError.FAST_NOT_PLANNED), (w.startFast(fast.id) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-030 otro exige texto, regla de alerta exige la regla y la hora no puede ser antes del inicio ni futura`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val now = LocalDateTime.of(2026, 10, 22, 22, 10)
        assertEquals(listOf(ValidationError.END_DETAIL_REQUIRED), (w.endFast(EndFast.Input(fast.id, now, EndReason.OTHER, " ", null)) as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.END_RULE_REQUIRED), (w.endFast(EndFast.Input(fast.id, now, EndReason.ALERT_RULE, null, "otra")) as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.END_BEFORE_START), (w.endFast(EndFast.Input(fast.id, now.minusHours(1), EndReason.PERSONAL_DECISION, null, null)) as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.END_IN_FUTURE), (w.endFast(EndFast.Input(fast.id, now.plusHours(1), EndReason.PERSONAL_DECISION, null, null)) as Outcome.Invalid).errors)
        val ruleId = w.fasts.get(fast.id)!!.plan.rules.single().id
        val ended = (w.endFast(EndFast.Input(fast.id, now, EndReason.ALERT_RULE, "ignorado", ruleId)) as Outcome.Ok).value
        assertEquals(ruleId, ended.endRuleId)
        assertNull(ended.endReasonDetail)
    }
}
