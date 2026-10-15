package com.ayuno.domain.usecase

import com.ayuno.domain.derived.FastMeasurements
import com.ayuno.domain.derived.MeasurementItem
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.BloodPressureReading
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** EV-004 · H-034 (mediciones y presión) y H-048 · D-20 · TRX corregir-entrada. */
class H048CorrectMeasurementsTest {

    private val at = LocalDateTime.of(2026, 10, 23, 12, 1, 40)

    private suspend fun TestWorld.glucose(fastId: String, value: Double, time: LocalDateTime = at): Measurement {
        val r = recordMeasurement(RecordMeasurement.Input(fastId, "glucose", time, valueEntered = value, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        return ((r as Outcome.Ok).value as RecordResult.Saved).entry
    }

    private suspend fun TestWorld.correct(m: Measurement, value: Double, reason: String? = "error de dedo", confirmed: Boolean = false) =
        correctMeasurement(CorrectMeasurement.Input(m.id, m.takenAt, valueEntered = value, unitEntered = m.unitEntered, source = m.source, reason = reason, confirmedOutOfRange = confirmed))

    private suspend fun TestWorld.pressure(fastId: String, sys: Int, dia: Int, position: Position = Position.SITTING, paired: String? = null): BloodPressureReading {
        val r = recordBloodPressure(RecordBloodPressure.Input(fastId, at, systolic = sys, diastolic = dia, pulse = 62, position = position, pairedReadingId = paired))
        return ((r as Outcome.Ok).value as RecordResult.Saved).entry
    }

    // ---------- Corregir (H-034) ----------

    @Test
    fun `H-034 corregir conserva el id, recalcula el equivalente y deja una fila por campo con el motivo`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val m = w.glucose(f.id, 46.0)
        w.correctMeasurement(CorrectMeasurement.Input(m.id, m.takenAt, valueEntered = 3.6, unitEntered = "mmol/L", source = GlucoseSource.CGM, reason = " error de dedo "))
        val after = w.records.measurements.value.single()
        assertEquals(m.id, after.id)
        assertEquals(3.6, after.valueEntered)
        assertEquals("mmol/L", after.unitEntered)
        assertEquals(64.85, after.valueNum!!, 0.01)
        val log = w.audit.entriesFor(m.id)
        assertEquals(setOf("value_entered", "unit_entered", "value_num"), log.map { it.field }.toSet())
        assertTrue(log.all { it.action == EditAction.EDIT && it.entityName == "measurement" && it.reason == "error de dedo" })
        assertEquals("46" to "3.6", log.first { it.field == "value_entered" }.let { it.oldValue to it.newValue })
    }

    @Test
    fun `H-034 guardar sin cambios no escribe nada`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val m = w.glucose(f.id, 64.0)
        val result = w.correct(m, 64.0)
        assertInstanceOf(RecordResult.Saved::class.java, (result as Outcome.Ok).value)
        assertTrue(w.audit.entries.isEmpty())
        assertEquals(m, w.records.measurements.value.single())
    }

    @Test
    fun `H-034 un valor nuevo fuera de rango pide confirmacion y no cambia nada hasta confirmar`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val m = w.glucose(f.id, 64.0)
        assertInstanceOf(RecordResult.NeedsRangeConfirmation::class.java, (w.correct(m, 640.0) as Outcome.Ok).value)
        assertEquals(64.0, w.records.measurements.value.single().valueNum)
        assertTrue(w.audit.entries.isEmpty())
        w.correct(m, 640.0, confirmed = true)
        assertEquals(640.0, w.records.measurements.value.single().valueNum)
    }

    @Test
    fun `H-034 corregir en curso evalua las reglas y una alerta previa no cambia`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val low = w.glucose(f.id, 50.0, at.minusHours(3))
        val previous = w.alerts.items.value.single()
        w.correct(low, 70.0)
        val m = w.glucose(f.id, 72.0)
        val result = w.correct(m, 46.0)
        val saved = (result as Outcome.Ok).value as RecordResult.Saved
        assertEquals(1, saved.alerts.size)
        assertEquals(listOf(m.id), saved.alerts.single().triggerEntryIds)
        assertEquals(previous, w.alerts.items.value.first { it.id == previous.id })
    }

    @Test
    fun `H-034 corregir sin cumplir ninguna regla no crea alertas`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val m = w.glucose(f.id, 72.0)
        val saved = (w.correct(m, 80.0) as Outcome.Ok).value as RecordResult.Saved
        assertTrue(saved.alerts.isEmpty())
        assertTrue(w.alerts.items.value.isEmpty())
    }

    @Test
    fun `H-048 en un ayuno terminado se corrige igual pero no se evaluan reglas`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val m = w.glucose(f.id, 72.0)
        w.clock.instant = java.time.Instant.parse("2026-10-23T20:00:00Z")
        w.endFast(EndFast.Input(f.id, at.plusHours(1), EndReason.PERSONAL_DECISION, null, null))
        val saved = (w.correct(m, 46.0) as Outcome.Ok).value as RecordResult.Saved
        assertEquals(46.0, saved.entry.valueNum)
        assertTrue(w.alerts.items.value.isEmpty())
    }

    @Test
    fun `D-20 una no se hizo solo corrige hora y motivo`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, notDoneReason = "sin tiras"))
        val m = w.records.measurements.value.single()
        val withValue = w.correctMeasurement(CorrectMeasurement.Input(m.id, at, valueEntered = 70.0, source = GlucoseSource.CGM, notDoneReason = "sin tiras"))
        assertEquals(listOf(ValidationError.FIELD_NOT_EDITABLE), (withValue as Outcome.Invalid).errors)
        val empty = w.correctMeasurement(CorrectMeasurement.Input(m.id, at, notDoneReason = " "))
        assertEquals(listOf(ValidationError.REASON_REQUIRED), (empty as Outcome.Invalid).errors)
        w.correctMeasurement(CorrectMeasurement.Input(m.id, at.plusMinutes(5), notDoneReason = "no me quedaban tiras"))
        val after = w.records.measurements.value.single()
        assertEquals("no me quedaban tiras", after.notDoneReason)
        assertEquals(at.plusMinutes(5), after.takenAt)
        assertNull(after.valueNum)
        assertEquals(setOf("taken_at", "not_done_reason"), w.audit.entriesFor(m.id).map { it.field }.toSet())
    }

    @Test
    fun `D-20 una medicion con valor no pasa a no se hizo`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val m = w.glucose(f.id, 64.0)
        val result = w.correctMeasurement(CorrectMeasurement.Input(m.id, at, notDoneReason = "me equivoque"))
        assertEquals(listOf(ValidationError.FIELD_NOT_EDITABLE), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `TRX una entrada inexistente o borrada no se corrige`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val m = w.glucose(f.id, 64.0)
        w.setEntryDeleted(EntryKind.MEASUREMENT, m.id, true)
        assertEquals(listOf(ValidationError.ENTRY_NOT_FOUND), (w.correct(m, 70.0) as Outcome.Invalid).errors)
        val missing = w.correctMeasurement(CorrectMeasurement.Input("no-existe", at, valueEntered = 1.0))
        assertEquals(listOf(ValidationError.ENTRY_NOT_FOUND), (missing as Outcome.Invalid).errors)
    }

    // ---------- Presión ----------

    @Test
    fun `H-034 corregir la presion deja rastro por componente y evalua reglas`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.standingSystolicBelow(100.0))))
        val sitting = w.pressure(f.id, 118, 76)
        val standing = w.pressure(f.id, 112, 74, Position.STANDING, paired = sitting.id)
        val result = w.correctBloodPressure(CorrectBloodPressure.Input(standing.id, at, systolic = 92, diastolic = 70, pulse = 62, position = Position.STANDING, reason = "tecleé mal"))
        val saved = (result as Outcome.Ok).value as RecordResult.Saved
        assertEquals(1, saved.alerts.size)
        assertEquals(sitting.id, w.records.readings.value.first { it.id == standing.id }.pairedReadingId)
        assertEquals(setOf("systolic", "diastolic"), w.audit.entriesFor(standing.id).map { it.field }.toSet())
        assertTrue(w.audit.entriesFor(standing.id).all { it.entityName == "blood_pressure_reading" })
    }

    @Test
    fun `D-20 una toma vinculada no cambia de posicion`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val sitting = w.pressure(f.id, 118, 76)
        w.pressure(f.id, 112, 74, Position.STANDING, paired = sitting.id)
        val result = w.correctBloodPressure(CorrectBloodPressure.Input(sitting.id, at, systolic = 118, diastolic = 76, pulse = 62, position = Position.LYING))
        assertEquals(listOf(ValidationError.FIELD_NOT_EDITABLE), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `H-034 la presion valida la diastolica bajo la sistolica`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val r = w.pressure(f.id, 118, 76)
        val result = w.correctBloodPressure(CorrectBloodPressure.Input(r.id, at, systolic = 70, diastolic = 76, position = Position.SITTING))
        assertEquals(listOf(ValidationError.DIASTOLIC_NOT_BELOW_SYSTOLIC), (result as Outcome.Invalid).errors)
    }

    // ---------- Borrar y restaurar ----------

    @Test
    fun `H-034 borrar oculta la medicion, deja rastro y no evalua reglas y restaurar la devuelve`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val m = w.glucose(f.id, 64.0)
        w.setEntryDeleted(EntryKind.MEASUREMENT, m.id, true)
        assertTrue(w.records.measurements(f.id).isEmpty())
        assertEquals(listOf(m), w.records.observeDeletedMeasurements(f.id).first())
        w.setEntryDeleted(EntryKind.MEASUREMENT, m.id, true)
        w.setEntryDeleted(EntryKind.MEASUREMENT, m.id, false)
        assertEquals(listOf(m), w.records.measurements(f.id))
        assertEquals(listOf(EditAction.DELETE, EditAction.RESTORE), w.audit.entriesFor(m.id).map { it.action })
        assertTrue(w.alerts.items.value.isEmpty())
    }

    @Test
    fun `H-034 borrar una presion y una inexistente`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val r = w.pressure(f.id, 118, 76)
        w.setEntryDeleted(EntryKind.BLOOD_PRESSURE, r.id, true)
        assertTrue(w.records.bloodPressure(f.id).isEmpty())
        assertEquals("blood_pressure_reading", w.audit.entriesFor(r.id).single().entityName)
        assertEquals(listOf(ValidationError.ENTRY_NOT_FOUND), (w.setEntryDeleted(EntryKind.MEASUREMENT, "x", true) as Outcome.Invalid).errors)
    }

    // ---------- Lista del ayuno (H-048) ----------

    @Test
    fun `H-048 la lista va de la mas reciente a la mas antigua, por dias, con filtro y marca corregida`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val early = w.glucose(f.id, 72.0, LocalDateTime.of(2026, 10, 22, 23, 0))
        val late = w.glucose(f.id, 46.0)
        val bp = w.pressure(f.id, 118, 76)
        val gone = w.glucose(f.id, 99.0, at.minusHours(1))
        w.setEntryDeleted(EntryKind.MEASUREMENT, gone.id, true)
        w.correct(late, 64.0)
        val edited = w.audit.observeEditedRowIds(EntryKind.entries.map { it.entityName }).first()
        val items = FastMeasurements.of(w.records.measurements(f.id), w.records.bloodPressure(f.id), edited)
        assertEquals(listOf(late.id, bp.id, early.id).toSet(), items.map { it.id }.toSet())
        assertEquals(early.id, items.last().id)
        assertTrue(items.first { it.id == late.id }.edited)
        assertTrue(!items.first { it.id == early.id }.edited)
        assertEquals(listOf(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 22)), FastMeasurements.byDay(items).map { it.first })
        assertEquals(listOf("glucose", "blood_pressure").toSet(), FastMeasurements.types(items).toSet())
        assertTrue(FastMeasurements.filter(items, "blood_pressure").single() is MeasurementItem.OfBloodPressure)
        assertEquals(3, FastMeasurements.filter(items, null).size)
    }
}
