package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Trend
import com.ayuno.domain.model.UserChoice
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RecordAndAlertUseCasesTest {

    private val at = LocalDateTime.of(2026, 10, 23, 12, 1, 40)

    // ---------- H-008 ----------

    @Test
    fun `H-008 la glucosa guarda origen, tendencia, hora con segundos, nota y al despertar`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at.withNano(500), valueEntered = 64.0, unitEntered = "mg/dL",
            source = GlucoseSource.CGM, trend = Trend.STEADY, onWaking = true, note = " temblor leve "))
        val m = w.records.measurements.value.single()
        assertEquals(at, m.takenAt)
        assertEquals(64.0, m.valueNum)
        assertEquals(GlucoseSource.CGM, m.source)
        assertEquals(Trend.STEADY, m.trend)
        assertTrue(m.onWaking)
        assertEquals("temblor leve", m.note)
    }

    @Test
    fun `H-008 la glucosa exige origen`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val result = w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 64.0, unitEntered = "mg/dL"))
        assertEquals(listOf(ValidationError.SOURCE_REQUIRED), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `H-008 la glucosa en mmol por litro se guarda tecleada y convertida`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 4.0, unitEntered = "mmol/L", source = GlucoseSource.GLUCOMETER))
        val m = w.records.measurements.value.single()
        assertEquals(4.0, m.valueEntered)
        assertEquals("mmol/L", m.unitEntered)
        assertEquals(72.06, m.valueNum!!, 0.01)
    }

    @Test
    fun `H-008 un peso en lb se guarda en lb y tambien en kg`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, valueEntered = 157.4, unitEntered = "lb"))
        val m = w.records.measurements.value.single()
        assertEquals(157.4, m.valueEntered)
        assertEquals("lb", m.unitEntered)
        assertEquals(71.4, m.valueNum!!, 0.05)
    }

    @Test
    fun `H-008 fuera de rango pide confirmacion y luego se guarda tal cual`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val first = w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 640.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        assertInstanceOf(RecordResult.NeedsRangeConfirmation::class.java, (first as Outcome.Ok).value)
        assertTrue(w.records.measurements.value.isEmpty())
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 640.0, unitEntered = "mg/dL", source = GlucoseSource.CGM, confirmedOutOfRange = true))
        assertEquals(640.0, w.records.measurements.value.single().valueEntered)
    }

    @Test
    fun `H-008 unidad no permitida para el tipo se rechaza`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val result = w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, valueEntered = 70.0, unitEntered = "cm"))
        assertEquals(listOf(ValidationError.UNIT_NOT_ALLOWED), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `H-008 sin valor ni motivo se rechaza y un tipo desactivado no se anota`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.VALUE_REQUIRED), (w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at)) as Outcome.Invalid).errors)
        w.setMetricTypeActive("weight", false)
        assertEquals(listOf(ValidationError.METRIC_NOT_ACTIVE), (w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, valueEntered = 70.0)) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-008 la unidad por defecto es la de entrada del tipo`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, valueEntered = 70.0))
        assertEquals("kg", w.records.measurements.value.single().unitEntered)
    }

    // ---------- H-009 ----------

    @Test
    fun `H-009 sentado y de pie quedan vinculadas`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val sitting = ((w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 118, diastolic = 76, pulse = 64, position = Position.SITTING)) as Outcome.Ok).value as RecordResult.Saved).entry
        w.recordBloodPressure(RecordBloodPressure.Input(f.id, at.plusMinutes(1), systolic = 104, diastolic = 70, pulse = 76, position = Position.STANDING, pairedReadingId = sitting.id))
        val standing = w.records.readings.value.last()
        assertEquals(sitting.id, standing.pairedReadingId)
        assertEquals(Position.STANDING, standing.position)
    }

    @Test
    fun `H-009 se puede guardar solo la toma sentado, sin pulso`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 118, diastolic = 76, position = Position.SITTING))
        assertNull(w.records.readings.value.single().pulse)
    }

    @Test
    fun `H-009 la diastolica debe ser menor que la sistolica y el vinculo debe ser de pie a sentado`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.DIASTOLIC_NOT_BELOW_SYSTOLIC),
            (w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 70, diastolic = 80, position = Position.SITTING)) as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.PAIRED_READING_INVALID),
            (w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 110, diastolic = 70, position = Position.STANDING, pairedReadingId = "no-existe")) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-009 fuera del rango configurado pide confirmacion`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val result = w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 260, diastolic = 70, position = Position.SITTING))
        assertEquals(listOf("systolic"), ((result as Outcome.Ok).value as RecordResult.NeedsRangeConfirmation).fields)
    }

    // ---------- H-017 (parcial, «no se hizo» sobre un pendiente) ----------

    @Test
    fun `no se hizo exige motivo y excluye el valor`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.VALUE_AND_NOT_DONE),
            (w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, valueEntered = 70.0, notDoneReason = "x")) as Outcome.Invalid).errors)
        w.recordMeasurement(RecordMeasurement.Input(f.id, "weight", at, scheduledFor = at, notDoneReason = "Sin báscula"))
        val m = w.records.measurements.value.single()
        assertNull(m.valueNum)
        assertEquals("Sin báscula", m.notDoneReason)
    }

    // ---------- H-027 ----------

    @Test
    fun `H-027 seguir exige motivo, la decision queda registrada y el ayuno sigue en curso`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 50.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        val alert = w.alerts.items.value.single()
        assertEquals(listOf(ValidationError.REASON_REQUIRED), (w.decideAlert(alert.id, UserChoice.CONTINUE, " ") as Outcome.Invalid).errors)
        w.decideAlert(alert.id, UserChoice.CONTINUE, "Me siento bien, vuelvo a medir")
        val decided = w.alerts.items.value.single()
        assertEquals(UserChoice.CONTINUE, decided.userChoice)
        assertEquals("Me siento bien, vuelvo a medir", decided.choiceReason)
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(f.id)!!.fast.status)
        assertEquals(listOf(ValidationError.ALERT_ALREADY_DECIDED), (w.decideAlert(alert.id, UserChoice.END_FAST, null) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-027 elegir terminar registra la decision pero no termina el ayuno por su cuenta`() = runTest {
        val w = TestWorld(); val f = w.startedFast(w.plan57h(listOf(w.glucoseBelow(55.0))))
        w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, valueEntered = 50.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        val alert = w.alerts.items.value.single()
        w.decideAlert(alert.id, UserChoice.END_FAST, "ignorado")
        assertEquals(UserChoice.END_FAST, w.alerts.items.value.single().userChoice)
        assertNull(w.alerts.items.value.single().choiceReason)
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(f.id)!!.fast.status)
    }
}
