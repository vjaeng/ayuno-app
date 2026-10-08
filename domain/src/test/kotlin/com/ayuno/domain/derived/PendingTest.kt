package com.ayuno.domain.derived

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PendingTest {

    private val from = LocalDateTime.of(2026, 10, 22, 0, 0)
    private val to = LocalDateTime.of(2026, 10, 30, 0, 0)

    @Test
    fun `H-001 los horarios solo generan pendientes dentro de su etapa`() = runTest {
        val world = TestWorld()
        val source = world.fasts.get(world.startedFast().id)!!
        val items = Pending.items(source, setOf("glucose", "weight"), from, to)
        val glucose = items.filter { it.metricTypeCode == "glucose" }.map { it.scheduledAt }
        // Ayuno de jueves 22:10 a domingo 07:10: las 22:00 del jueves quedan fuera; el domingo 07:00 dentro.
        assertEquals(LocalDateTime.of(2026, 10, 23, 7, 0), glucose.first())
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 0), glucose.last())
        assertEquals(9, glucose.size)
        val weight = items.filter { it.metricTypeCode == "weight" }.map { it.scheduledAt }
        assertEquals(LocalDateTime.of(2026, 10, 26, 7, 0), weight.first())
    }

    @Test
    fun `H-001 una medicion desactivada no genera pendientes`() = runTest {
        val world = TestWorld()
        val source = world.fasts.get(world.startedFast().id)!!
        assertTrue(Pending.items(source, setOf("weight"), from, to).none { it.metricTypeCode == "glucose" })
    }

    @Test
    fun `estado del pendiente segun lo anotado`() = runTest {
        val world = TestWorld()
        val fast = world.startedFast()
        val source = world.fasts.get(fast.id)!!
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        val at12 = LocalDateTime.of(2026, 10, 23, 12, 0)
        world.recordMeasurement(RecordMeasurement.Input(fast.id, "glucose", at7.plusMinutes(2), at7, 72.0, "mg/dL", source = GlucoseSource.CGM))
        world.recordMeasurement(RecordMeasurement.Input(fast.id, "glucose", at12, at12, notDoneReason = "Sin sensor"))
        val measurements = world.records.measurements(fast.id)
        val items = Pending.items(source, setOf("glucose"), from, to)
        val now = LocalDateTime.of(2026, 10, 23, 18, 20)
        val states = items.associate { it.scheduledAt to Pending.status(it, now, measurements, emptyList()).state }
        assertEquals(PendingState.DONE, states[at7])
        assertEquals(PendingState.NOT_DONE, states[at12])
        assertEquals(PendingState.LATE, states[LocalDateTime.of(2026, 10, 23, 18, 0)])
        assertEquals(PendingState.PENDING, states[LocalDateTime.of(2026, 10, 23, 22, 0)])
    }
}
