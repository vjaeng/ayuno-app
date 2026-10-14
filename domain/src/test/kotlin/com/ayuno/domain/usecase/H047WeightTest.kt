package com.ayuno.domain.usecase

import com.ayuno.domain.derived.WeightChange
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.RuleConditionKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

/** EV-003 · D-19: peso al terminar en la misma operación (H-030) y peso inicial, final y diferencia (H-047). */
class H047WeightTest {

    // El ayuno empieza el jueves 22 oct a las 22:10 (hora local) y termina el domingo 25 a las 07:03.
    private val start = LocalDateTime.of(2026, 10, 22, 22, 10)
    private val end = LocalDateTime.of(2026, 10, 25, 7, 3)

    private suspend fun TestWorld.weigh(fast: Fast, at: LocalDateTime, value: Double, unit: String = "kg") =
        recordMeasurement(RecordMeasurement.Input(fast.id, "weight", at, valueEntered = value, unitEntered = unit))

    private suspend fun TestWorld.finish(fast: Fast, weight: EndFast.Weight? = null): Outcome<Fast> {
        clock.instant = Instant.parse("2026-10-25T12:05:00Z")
        return endFast(EndFast.Input(fast.id, end, EndReason.GOAL_MET, null, null, weight))
    }

    private suspend fun TestWorld.change(fast: Fast) = WeightChange.of(fasts.get(fast.id)!!.fast, records.measurements(fast.id))

    @Test
    fun `H-030 el peso al terminar se guarda con la hora de fin y la unidad tecleada`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        val ended = w.finish(fast, EndFast.Weight(152.0, "lb"))
        assertEquals(FastStatus.FINISHED, (ended as Outcome.Ok).value.status)
        val m = w.records.measurements(fast.id).single()
        assertEquals("weight", m.metricTypeCode)
        assertEquals(end, m.takenAt)
        assertEquals(152.0, m.valueEntered!!, 0.0)
        assertEquals("lb", m.unitEntered)
        assertEquals(68.946, m.valueNum!!, 0.001)
    }

    @Test
    fun `H-030 sin peso se termina igual y no se guarda medicion`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertTrue(w.finish(fast) is Outcome.Ok)
        assertTrue(w.records.measurements(fast.id).isEmpty())
    }

    @Test
    fun `H-047 un peso fuera de rango sin confirmar no termina el ayuno y confirmado si`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertEquals(listOf(ValidationError.WEIGHT_OUT_OF_RANGE_UNCONFIRMED), (w.finish(fast, EndFast.Weight(400.0, "kg")) as Outcome.Invalid).errors)
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(fast.id)!!.fast.status)
        assertTrue(w.records.measurements(fast.id).isEmpty())
        assertTrue(w.finish(fast, EndFast.Weight(400.0, "kg", confirmedOutOfRange = true)) is Outcome.Ok)
        assertEquals(400.0, w.records.measurements(fast.id).single().valueNum!!, 0.0)
    }

    @Test
    fun `H-047 unidad no permitida o peso desactivado no terminan el ayuno`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertEquals(listOf(ValidationError.UNIT_NOT_ALLOWED), (w.finish(fast, EndFast.Weight(70.0, "g")) as Outcome.Invalid).errors)
        w.setMetricTypeActive("weight", false)
        assertEquals(listOf(ValidationError.METRIC_NOT_ACTIVE), (w.finish(fast, EndFast.Weight(70.0, "kg")) as Outcome.Invalid).errors)
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(fast.id)!!.fast.status)
    }

    @Test
    fun `H-047 el peso del fin no evalua reglas aunque una se cumpla`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(80.0).copy(metricTypeCode = "weight", conditionKind = RuleConditionKind.METRIC_BELOW)
        val fast = w.startedFast(w.plan57h(listOf(rule)))
        assertTrue(w.finish(fast, EndFast.Weight(60.0, "kg")) is Outcome.Ok)
        assertTrue(w.alerts.alerts(fast.id).isEmpty())
    }

    @Test
    fun `H-047 inicial y final con pesos antes, durante y despues del fin`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.weigh(fast, start.minusMinutes(5), 72.0)
        w.weigh(fast, start.plusHours(10), 71.0)
        w.finish(fast, EndFast.Weight(69.5, "kg"))
        w.weigh(fast, end.plusHours(5), 69.9)
        val change = w.change(fast)
        assertEquals(72.0, change.initial!!.valueNum!!, 0.0)
        assertEquals(69.5, change.final!!.valueNum!!, 0.0)
        assertEquals(-2.5, change.difference!!, 1e-9)
    }

    @Test
    fun `H-047 sin peso antes del inicio cuenta el primero del ayuno y sin peso al fin el primero despues`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.weigh(fast, start.plusMinutes(1), 72.0)
        w.finish(fast)
        w.weigh(fast, end.plusDays(1), 70.0)
        w.weigh(fast, end.plusDays(2), 70.5)
        val change = w.change(fast)
        assertEquals(72.0, change.initial!!.valueNum!!, 0.0)
        assertEquals(70.0, change.final!!.valueNum!!, 0.0)
    }

    @Test
    fun `H-047 con un solo peso no hay diferencia y sin pesos no hay nada`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        assertNull(w.change(fast).initial)
        w.weigh(fast, start.plusMinutes(1), 72.0)
        w.finish(fast)
        val change = w.change(fast)
        assertEquals(72.0, change.initial!!.valueNum!!, 0.0)
        assertNull(change.final)
        assertNull(change.difference)
    }

    @Test
    fun `H-047 se compara en kg aunque se teclee en lb e ignora los no se hizo`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.weigh(fast, start.plusMinutes(1), 160.0, "lb")
        w.recordMeasurement(RecordMeasurement.Input(fast.id, "weight", start.plusHours(5), notDoneReason = "Sin báscula"))
        w.finish(fast, EndFast.Weight(70.0, "kg"))
        val change = w.change(fast)
        assertEquals(72.575, change.initial!!.valueNum!!, 0.001)
        assertEquals(70.0 - 72.5747792, change.difference!!, 0.001)
    }
}
