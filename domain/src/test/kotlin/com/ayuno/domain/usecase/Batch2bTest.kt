package com.ayuno.domain.usecase

import com.ayuno.domain.derived.DayAgenda
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.derived.WakingProposal
import com.ayuno.domain.fakes.FakeSettings
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.Settings
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Batch2bTest {

    private val night = Settings(quietStart = LocalTime.of(23, 0), quietEnd = LocalTime.of(6, 30))

    // ---------- H-017 ----------

    @Test
    fun `H-017 el silencio nocturno admite franjas que cruzan la medianoche`() {
        assertTrue(night.isQuiet(LocalTime.of(23, 30)))
        assertTrue(night.isQuiet(LocalTime.of(3, 0)))
        assertFalse(night.isQuiet(LocalTime.of(6, 30)))
        assertFalse(night.isQuiet(LocalTime.of(12, 0)))
        assertFalse(Settings().isQuiet(LocalTime.of(3, 0)))
    }

    @Test
    fun `H-017 guardar el silencio exige fin si hay inicio y se puede quitar`() = runTest {
        val settings = FakeSettings()
        val save = SaveQuietHours(settings)
        assertEquals(listOf(ValidationError.QUIET_END_REQUIRED), (save(LocalTime.of(23, 0), null) as Outcome.Invalid).errors)
        save(LocalTime.of(23, 0), LocalTime.of(6, 30))
        assertEquals(LocalTime.of(6, 30), settings.get().quietEnd)
        save(null, null)
        assertEquals(Settings(), settings.get())
    }

    // ---------- H-026 ----------

    private fun TestWorld.remeasureRule(minutes: Int = 15) = glucoseBelow(55.0).copy(action = RuleAction.SCHEDULE_MEASUREMENT, actionMinutes = minutes)

    @Test
    fun `H-026 medir de nuevo exige minutos y una condicion de medicion`() {
        val w = TestWorld()
        assertTrue(RuleValidator.validate(w.remeasureRule()).isEmpty())
        assertEquals(listOf(ValidationError.RULE_ACTION_MINUTES_REQUIRED), RuleValidator.validate(w.remeasureRule().copy(actionMinutes = null)))
        val symptom = w.remeasureRule().copy(conditionKind = RuleConditionKind.ALARM_SYMPTOM, metricTypeCode = null, threshold = null)
        assertEquals(listOf(ValidationError.RULE_ACTION_NEEDS_METRIC), RuleValidator.validate(symptom))
    }

    @Test
    fun `H-026 una regla con medir de nuevo en 15 minutos programa ese recordatorio, que ignora el silencio y se cumple con la siguiente lectura`() = runTest {
        val w = TestWorld()
        val f = w.startedFast(w.plan57h(listOf(w.remeasureRule())))
        val at = LocalDateTime.of(2026, 10, 23, 3, 0) // de madrugada, en silencio
        w.clock.instant = Instant.parse("2026-10-23T08:00:00Z")
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 50.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        val source = w.fasts.get(f.id)!!
        val firedAt = w.alerts.items.value.single().firedAt
        val day = DayAgenda.of(source, setOf("glucose"), emptyList(), w.alerts.items.value, PendingRecords(w.records.measurements.value), firedAt.toLocalDate(), firedAt)
        val followUp = day.single { it.item.fromRule }
        assertEquals(firedAt.plusMinutes(15), followUp.item.scheduledAt)
        assertEquals(PendingState.PENDING, followUp.state)
        assertTrue(DayAgenda.isNotified(followUp.item, night))

        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", firedAt.plusMinutes(16), valueEntered = 70.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        val after = DayAgenda.of(source, setOf("glucose"), emptyList(), w.alerts.items.value, PendingRecords(w.records.measurements.value), firedAt.toLocalDate(), firedAt.plusMinutes(20))
        assertEquals(PendingState.DONE, after.single { it.item.fromRule }.state)
    }

    // ---------- H-018 ----------

    @Test
    fun `H-018 la agenda del dia junta mediciones y tomas con su estado`() = runTest {
        val w = TestWorld()
        val f = w.startedFast()
        val source = w.fasts.get(f.id)!!
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at7, at7, 72.0, "mg/dL", source = GlucoseSource.CGM))
        val day = DayAgenda.of(source, setOf("glucose"), emptyList(), emptyList(), PendingRecords(w.records.measurements.value), LocalDate.of(2026, 10, 23), LocalDateTime.of(2026, 10, 23, 12, 30))
        assertEquals(listOf(7, 12, 18, 22), day.map { it.item.scheduledAt.hour })
        assertEquals(listOf(PendingState.DONE, PendingState.LATE, PendingState.PENDING, PendingState.PENDING), day.map { it.state })
        assertTrue(day.all { it.item.kind == PendingKind.METRIC })
    }

    @Test
    fun `H-017 los pendientes en silencio no se notifican`() {
        val item = com.ayuno.domain.derived.PendingItem(PendingKind.METRIC, "glucose", LocalDateTime.of(2026, 10, 23, 3, 0))
        assertFalse(DayAgenda.isNotified(item, night))
        assertTrue(DayAgenda.isNotified(item.copy(scheduledAt = LocalDateTime.of(2026, 10, 23, 7, 0)), night))
    }

    // ---------- H-008 ----------

    @Test
    fun `H-008 se propone al despertar en la primera lectura tras el fin del silencio`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val now = LocalDateTime.of(2026, 10, 23, 6, 45)
        assertTrue(WakingProposal.propose(now, night, emptyList(), emptyList()))
        assertFalse(WakingProposal.propose(LocalDateTime.of(2026, 10, 23, 6, 0), night, emptyList(), emptyList()))
        assertFalse(WakingProposal.propose(now, Settings(), emptyList(), emptyList()))
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", LocalDateTime.of(2026, 10, 23, 6, 40), valueEntered = 80.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        assertFalse(WakingProposal.propose(now, night, w.records.measurements.value, emptyList()))
    }
}
