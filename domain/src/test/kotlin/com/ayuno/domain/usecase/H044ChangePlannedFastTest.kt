package com.ayuno.domain.usecase

import com.ayuno.domain.derived.Pending
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.id.Ids
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class H044ChangePlannedFastTest {

    private val lastMeal = LocalDateTime.of(2026, 10, 22, 18, 0)
    private val start = LocalDateTime.of(2026, 10, 22, 22, 0)

    private suspend fun TestWorld.otherTemplate(plan: Plan = Plan.EMPTY): TemplateWithPlan =
        (saveTemplate(TemplateWithPlan(Template(Ids.newId(), "Ayuno de 36 horas", 36.0, listOf(12.0, 24.0)), plan)) as Outcome.Ok).value

    private fun same(f: FastWithPlan, templateId: String? = f.fast.templateId) = ChangePlannedFast.Input(
        f.fast.id, templateId, f.fast.lastMealPlanned, f.fast.clockStartPlanned, f.fast.targetHours, f.fast.notes,
    )

    @Test
    fun `H-044 cambia horas previstas y notas, recalcula el objetivo y deja cada cambio en la bitacora`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        val input = ChangePlannedFast.Input.withPlannedEnd(
            f.fast.id, f.fast.templateId, lastMeal.plusHours(1), start.plusHours(1), start.plusHours(1 + 36), " Semana tranquila ", reason = "cambio de turno",
        )
        val changed = (w.changePlannedFast(input) as Outcome.Ok).value.fast
        assertEquals(lastMeal.plusHours(1), changed.lastMealPlanned)
        assertEquals(start.plusHours(1), changed.clockStartPlanned)
        assertEquals(36.0, changed.targetHours)
        assertEquals("Semana tranquila", changed.notes)
        assertEquals(changed, w.fasts.get(f.fast.id)!!.fast)

        val log = w.audit.entriesFor(f.fast.id)
        assertEquals(setOf("last_meal_planned", "clock_start_planned", "target_hours", "notes"), log.map { it.field }.toSet())
        assertTrue(log.all { it.action == EditAction.EDIT && it.entityName == "fast" && it.reason == "cambio de turno" })
        val target = log.single { it.field == "target_hours" }
        assertEquals("57.0", target.oldValue)
        assertEquals("36.0", target.newValue)
        assertEquals(f.plan, w.fasts.get(f.fast.id)!!.plan)
    }

    @Test
    fun `H-044 sin diferencias no escribe nada`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        assertEquals(f, (w.changePlannedFast(same(f)) as Outcome.Ok).value)
        assertTrue(w.audit.entriesFor(f.fast.id).isEmpty())
    }

    @Test
    fun `H-044 un ayuno en curso o terminado no se cambia`() = runTest {
        val w = TestWorld()
        val started = w.startedFast()
        val source = w.fasts.get(started.id)!!
        val result = w.changePlannedFast(same(source).copy(notes = "otra"))
        assertEquals(listOf(ValidationError.FAST_NOT_PLANNED), (result as Outcome.Invalid).errors)
        assertNull(w.fasts.get(started.id)!!.fast.notes)
    }

    @Test
    fun `H-044 aplica las validaciones de crear`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        val result = w.changePlannedFast(same(f).copy(clockStartPlanned = lastMeal.minusHours(1), targetHours = 0.0))
        assertEquals(
            listOf(ValidationError.CLOCK_START_BEFORE_LAST_MEAL, ValidationError.PLANNED_END_NOT_AFTER_START),
            (result as Outcome.Invalid).errors,
        )
        assertEquals(f, w.fasts.get(f.fast.id))
    }

    @Test
    fun `H-044 cambiar la plantilla sustituye el plan entero con identificadores nuevos y copia sus hitos`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val other = w.otherTemplate(Plan(stages = w.plan57h().stages.take(1), metrics = emptyList(), rules = emptyList()))
        val changed = (w.changePlannedFast(same(f, other.template.id)) as Outcome.Ok).value

        assertEquals(other.template.id, changed.fast.templateId)
        assertEquals(listOf(12.0, 24.0), changed.fast.milestoneHours)
        assertEquals(other.plan.stages.map { it.kind }, changed.plan.stages.map { it.kind })
        assertTrue(changed.plan.rules.isEmpty())
        val templateIds = (other.plan.stages + f.plan.stages).map { it.id }.toSet()
        assertTrue(changed.plan.stages.none { it.id in templateIds })
        assertEquals(setOf("template_id", "milestone_hours"), w.audit.entriesFor(f.fast.id).map { it.field }.toSet())
    }

    @Test
    fun `H-044 pasar a desde cero deja el plan vacio`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        val changed = (w.changePlannedFast(same(f, templateId = null)) as Outcome.Ok).value
        assertNull(changed.fast.templateId)
        assertEquals(Plan.EMPTY, changed.plan)
        assertTrue(changed.fast.milestoneHours.isEmpty())
    }

    @Test
    fun `H-044 plantilla inexistente no cambia nada`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        val result = w.changePlannedFast(same(f, templateId = "no-existe"))
        assertEquals(listOf(ValidationError.TEMPLATE_NOT_FOUND), (result as Outcome.Invalid).errors)
        assertEquals(f, w.fasts.get(f.fast.id))
    }

    @Test
    fun `H-044 lo ya anotado y las alertas conservan su regla al cambiar la plantilla (EV1-01)`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(55.0).copy(action = RuleAction.SCHEDULE_MEASUREMENT, actionMinutes = 15)
        val f = w.plannedFast(w.plan57h(listOf(rule)))
        val saved = w.recordMeasurement(RecordMeasurement.Input(f.fast.id, "glucose", lastMeal, valueEntered = 50.0, unitEntered = "mg/dL", source = GlucoseSource.GLUCOMETER))
        val alert = ((saved as Outcome.Ok).value as RecordResult.Saved).alerts.single()

        val other = w.otherTemplate()
        val changed = (w.changePlannedFast(same(f, other.template.id)) as Outcome.Ok).value
        assertEquals(1, w.records.measurements.value.size)
        assertTrue(changed.plan.rules.isEmpty())
        assertNotNull(changed.ruleById(alert.ruleId))
        assertEquals(rule.name, changed.ruleById(alert.ruleId)!!.name)
        assertEquals(1, Pending.ruleFollowUps(changed, listOf(alert)).size)

        // Las reglas sustituidas ya no se evalúan.
        val again = w.recordMeasurement(RecordMeasurement.Input(f.fast.id, "glucose", lastMeal.plusMinutes(20), valueEntered = 40.0, unitEntered = "mg/dL", source = GlucoseSource.GLUCOMETER))
        assertTrue(((again as Outcome.Ok).value as RecordResult.Saved).alerts.isEmpty())
    }
}
