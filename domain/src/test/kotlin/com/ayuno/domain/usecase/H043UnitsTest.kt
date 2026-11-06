package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** H-043: elegir mis unidades por defecto. */
class H043UnitsTest {

    private val at = LocalDateTime.of(2026, 11, 6, 8, 0)

    @Test
    fun `el peso por defecto pasa a libras y lo que se anota sin unidad usa libras`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertEquals("lb", (w.setEntryUnit("weight", "lb") as Outcome.Ok).value.entryUnit)
        assertEquals("lb", w.catalog.metricType("weight")!!.entryUnit)
        w.recordMeasurement(RecordMeasurement.Input(fast.id, "weight", at, valueEntered = 152.0))
        val m = w.records.measurements(fast.id).single()
        assertEquals("lb", m.unitEntered)
        assertEquals(68.946, m.valueNum!!, 0.001)
    }

    @Test
    fun `cambiar una unidad por defecto no altera las entradas ya guardadas`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.recordMeasurement(RecordMeasurement.Input(fast.id, "weight", at, valueEntered = 70.0, unitEntered = "kg"))
        val before = w.records.measurements(fast.id).single()
        w.setEntryUnit("weight", "lb")
        val after = w.records.measurements(fast.id).single()
        assertEquals(before, after)
        assertEquals("kg", after.unitEntered)
    }

    @Test
    fun `se puede volver a la unidad principal`() = runTest {
        val w = TestWorld()
        w.setEntryUnit("glucose", "mmol/L")
        assertEquals("mg/dL", (w.setEntryUnit("glucose", "mg/dL") as Outcome.Ok).value.entryUnit)
    }

    @Test
    fun `solo se acepta la unidad principal o la alternativa, y solo en tipos con alternativa`() = runTest {
        val w = TestWorld()
        assertEquals(listOf(ValidationError.UNIT_NOT_ALLOWED), (w.setEntryUnit("weight", "st") as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.UNIT_NOT_ALLOWED), (w.setEntryUnit("hunger", "de 0 a 10") as Outcome.Invalid).errors)
        assertEquals(listOf(ValidationError.METRIC_NOT_FOUND), (w.setEntryUnit("nada", "kg") as Outcome.Invalid).errors)
        assertEquals("kg", w.catalog.metricType("weight")!!.entryUnit)
    }
}
