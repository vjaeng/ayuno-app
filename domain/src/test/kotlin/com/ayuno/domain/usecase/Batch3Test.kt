package com.ayuno.domain.usecase

import com.ayuno.domain.derived.DayAgenda
import com.ayuno.domain.derived.FastRecords
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.derived.RefeedSchedule
import com.ayuno.domain.derived.Timeline
import com.ayuno.domain.derived.TimelineKind
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Settings
import com.ayuno.domain.model.StageKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class Batch3Test {

    private val broth = RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Una taza de caldo", 0, 60, 240, null)
    private val softMeal = RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Comida blanda", 360, null, null, null)
    private val supplements = RefeedStep(Ids.newId(), RefeedStepKind.REINTRODUCTION, "Vuelven los suplementos", null, null, null, 1)

    /** Fin real: domingo 25-oct 07:03 (el reloj se pone a esa hora). */
    private val end = LocalDateTime.of(2026, 10, 25, 7, 3)

    private suspend fun TestWorld.endedWithRefeed(): String {
        val fast = startedFast(plan57h().copy(refeed = listOf(broth, softMeal, supplements)))
        clock.instant = Instant.parse("2026-10-25T12:03:00Z")
        endFast(EndFast.Input(fast.id, end, EndReason.GOAL_MET, null, null)) as Outcome.Ok
        return fast.id
    }

    // ---------- H-031: definir los pasos ----------

    @Test
    fun `H-031 un paso por horas exige su hora, la repeticion exige ventana y la reintroduccion su dia`() {
        assertTrue(RefeedStepValidator.validate(broth).isEmpty())
        assertEquals(listOf(ValidationError.REFEED_OFFSET_REQUIRED), RefeedStepValidator.validate(softMeal.copy(offsetMinutes = null)))
        assertEquals(listOf(ValidationError.REFEED_REPEAT_FOR_REQUIRED), RefeedStepValidator.validate(broth.copy(repeatForMinutes = null)))
        assertEquals(listOf(ValidationError.REFEED_DAY_REQUIRED), RefeedStepValidator.validate(supplements.copy(dayOffset = 0)))
        assertEquals(listOf(ValidationError.TEXT_REQUIRED), RefeedStepValidator.validate(softMeal.copy(description = " ")))
    }

    @Test
    fun `H-031 los pasos exigen la etapa de vuelta a comer y se guardan sin campos que no aplican`() = runTest {
        val w = TestWorld()
        val noRefeedStage = w.plan57h().let { it.copy(stages = it.stages.filter { s -> s.kind != StageKind.REFEED }, refeed = listOf(broth)) }
        val rejected = w.saveTemplate(w.template().copy(plan = noRefeedStage))
        assertEquals(listOf(ValidationError.REFEED_STAGE_NOT_USED), (rejected as Outcome.Invalid).errors)

        val saved = w.template(w.plan57h().copy(refeed = listOf(supplements.copy(offsetMinutes = 30, description = " Vuelven los suplementos "))))
        val step = saved.plan.refeed.single()
        assertNull(step.offsetMinutes)
        assertEquals("Vuelven los suplementos", step.description)
    }

    @Test
    fun `H-031 las horas se calculan desde el fin real y la repeticion cubre su ventana`() {
        assertEquals(listOf(7, 8, 9, 10).map { end.withHour(it) }, RefeedSchedule.times(broth, end))
        assertEquals(listOf(LocalDateTime.of(2026, 10, 25, 13, 3)), RefeedSchedule.times(softMeal, end))
        assertEquals(listOf(LocalDate.of(2026, 10, 26).atStartOfDay()), RefeedSchedule.times(supplements, end))
    }

    // ---------- H-004 / H-030: copia del plan y casillas al terminar ----------

    @Test
    fun `H-004 H-030 el ayuno copia la vuelta a comer y al terminar se generan sus casillas en la misma operacion`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val steps = w.fasts.get(fastId)!!.plan.refeed
        assertEquals(listOf("Una taza de caldo", "Comida blanda", "Vuelven los suplementos"), steps.map { it.description })
        assertTrue(steps.none { it.id == broth.id }, "la copia lleva identificadores nuevos")

        val checks = w.refeed.checks(fastId)
        assertEquals(6, checks.size)
        assertTrue(checks.all { c -> steps.any { it.id == c.refeedStepId } && c.doneAt == null })
        assertEquals(end, checks.first().scheduledAt)
    }

    @Test
    fun `H-030 un ayuno sin vuelta a comer termina sin casillas`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.clock.instant = Instant.parse("2026-10-25T12:03:00Z")
        w.endFast(EndFast.Input(fast.id, end, EndReason.GOAL_MET, null, null))
        assertTrue(w.refeed.checks.value.isEmpty())
    }

    // ---------- H-031: casilla ----------

    @Test
    fun `H-031 marcar la casilla guarda la hora real y desmarcarla deja constancia`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val first = w.refeed.checks(fastId).first()
        w.clock.advanceMinutes(2)
        val marked = (w.setRefeedCheck(first.id, true) as Outcome.Ok).value
        assertEquals(end.plusMinutes(2), marked.doneAt)
        assertEquals(end.plusMinutes(2), w.refeed.check(first.id)!!.doneAt)

        w.setRefeedCheck(first.id, false)
        assertNull(w.refeed.check(first.id)!!.doneAt)
        val log = w.audit.entriesFor(first.id).single()
        assertEquals("refeed_check", log.entityName)
        assertEquals("done_at", log.field)
        assertEquals("2026-10-25T07:05:00", log.oldValue)
    }

    // ---------- H-018 / H-016: el día y los recordatorios ----------

    @Test
    fun `H-018 el dia incluye los pasos con su estado y la reintroduccion no se atrasa hasta acabar su dia`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val source = w.fasts.get(fastId)!!
        val checks = w.refeed.checks(fastId)
        w.setRefeedCheck(checks.first().id, true)
        val now = LocalDateTime.of(2026, 10, 25, 9, 10)
        val records = PendingRecords(refeedChecks = w.refeed.checks(fastId))
        val today = DayAgenda.of(source, emptySet(), emptyList(), emptyList(), records, now.toLocalDate(), now).filter { it.item.kind == PendingKind.REFEED }
        assertEquals(listOf(PendingState.DONE, PendingState.LATE, PendingState.PENDING, PendingState.PENDING, PendingState.PENDING), today.map { it.state })

        val monday = LocalDateTime.of(2026, 10, 26, 22, 0)
        val reintro = DayAgenda.of(source, emptySet(), emptyList(), emptyList(), records, monday.toLocalDate(), monday).single()
        assertTrue(reintro.item.allDay)
        assertEquals(PendingState.PENDING, reintro.state)
        assertFalse(DayAgenda.isNotified(reintro.item, Settings()))
        assertTrue(DayAgenda.isNotified(today[1].item, Settings()))
    }

    @Test
    fun `H-031 la vuelta a comer dura su etapa y el seguimiento hasta la ultima casilla o la etapa posterior`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val source = w.fasts.get(fastId)!!
        assertTrue(RefeedSchedule.inRefeedStage(source, end.plusHours(5)))
        assertFalse(RefeedSchedule.inRefeedStage(source, end.plusHours(7)))
        // Etapa posterior de 4 días tras las 6 h de vuelta a comer.
        assertEquals(end.plusHours(6).plusDays(4), RefeedSchedule.followUpEnd(source, w.refeed.checks(fastId)))
    }

    // ---------- H-032 ----------

    @Test
    fun `H-032 la comida exige descripcion y como cayo, y otro exige texto`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val input = RecordMeal.Input(fastId, end.plusHours(2), "Puré de calabaza", "un cuenco", MealTolerance.FINE, "ignorado")
        val saved = (w.recordMeal(input) as Outcome.Ok).value
        assertEquals("un cuenco", saved.approxAmount)
        assertNull(saved.toleranceDetail)
        assertEquals(saved, w.refeed.meals(fastId).single())

        val missing = w.recordMeal(input.copy(description = " ", tolerance = null)) as Outcome.Invalid
        assertEquals(listOf(ValidationError.TEXT_REQUIRED, ValidationError.TOLERANCE_REQUIRED), missing.errors)
        val other = w.recordMeal(input.copy(tolerance = MealTolerance.OTHER, toleranceDetail = " ")) as Outcome.Invalid
        assertEquals(listOf(ValidationError.TOLERANCE_DETAIL_REQUIRED), other.errors)
        val otherOk = (w.recordMeal(input.copy(tolerance = MealTolerance.OTHER, toleranceDetail = "acidez")) as Outcome.Ok).value
        assertEquals("acidez", otherOk.toleranceDetail)
    }

    // ---------- H-033 ----------

    @Test
    fun `H-033 la linea de tiempo junta todas las entradas en orden de hora, por dia`() = runTest {
        val w = TestWorld()
        val fastId = w.endedWithRefeed()
        val friday7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        w.recordMeasurement(RecordMeasurement.Input(fastId, "glucose", friday7.plusMinutes(2), friday7, 72.0, "mg/dL", source = GlucoseSource.CGM))
        w.recordNote(fastId, LocalDateTime.of(2026, 10, 23, 12, 10), "leve temblor")
        w.recordIntake(RecordIntake.Input(fastId, "water", LocalDateTime.of(2026, 10, 23, 9, 0), 500.0, "mL", null))
        w.triggerManualAlert(fastId, "Algo no va bien", "Mensaje")
        w.setRefeedCheck(w.refeed.checks(fastId).first().id, true)
        w.recordMeal(RecordMeal.Input(fastId, end.plusHours(2), "Puré", null, MealTolerance.FINE, null))

        val source = w.fasts.get(fastId)!!
        val records = FastRecords(
            measurements = w.records.measurements(fastId), intakes = w.records.intakes(fastId), notes = w.records.notes(fastId),
            alerts = w.alerts.alerts(fastId), refeedChecks = w.refeed.checks(fastId), meals = w.refeed.meals(fastId),
        )
        val entries = Timeline.of(source.fast, source.plan.refeed, records)
        assertEquals(
            listOf(TimelineKind.MEASUREMENT, TimelineKind.INTAKE, TimelineKind.NOTE, TimelineKind.ALERT, TimelineKind.REFEED, TimelineKind.MEAL),
            entries.map { it.kind },
        )
        val days = Timeline.byDay(entries)
        assertEquals(listOf(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25)), days.map { it.first })
    }
}
