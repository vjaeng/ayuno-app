package com.ayuno.domain.rules

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** Tabla obligatoria de `specs/PRUEBAS.md` §4.1 (condiciones de medición: H-019, H-020). */
class RuleEngineTest {

    private val base = LocalDateTime.of(2026, 10, 23, 12, 0)

    private suspend fun TestWorld.fastWith(rule: Rule): Fast = startedFast(plan57h(listOf(rule)))

    private suspend fun TestWorld.glucose(fast: Fast, minutesAfterBase: Long, value: Double): Int {
        val result = recordMeasurement(
            RecordMeasurement.Input(fast.id, "glucose", base.plusMinutes(minutesAfterBase), valueEntered = value, unitEntered = "mg/dL", source = GlucoseSource.CGM),
        )
        return ((result as Outcome.Ok).value as RecordResult.Saved).alerts.size
    }

    @Test
    fun `una lectura bajo X con regla bajo X dispara`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0))
        assertEquals(1, w.glucose(f, 0, 52.0))
    }

    @Test
    fun `una lectura bajo X con regla sostenida 20 min no dispara`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        assertEquals(0, w.glucose(f, 0, 52.0))
    }

    @Test
    fun `dos lecturas bajo X separadas 19 min no disparan`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        w.glucose(f, 0, 52.0)
        assertEquals(0, w.glucose(f, 19, 53.0))
    }

    @Test
    fun `dos lecturas bajo X separadas 20 min disparan con ambas lecturas`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        w.glucose(f, 0, 52.0)
        assertEquals(1, w.glucose(f, 20, 53.0))
        val alert = w.alerts.items.value.single()
        assertEquals(w.records.measurements.value.map { it.id }, alert.triggerEntryIds)
    }

    @Test
    fun `una lectura normal entre las dos bajas impide el disparo`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        w.glucose(f, 0, 52.0)
        w.glucose(f, 10, 60.0)
        assertEquals(0, w.glucose(f, 25, 53.0))
    }

    @Test
    fun `una tercera lectura baja sin lectura normal no vuelve a disparar`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        w.glucose(f, 0, 52.0)
        w.glucose(f, 20, 53.0)
        assertEquals(0, w.glucose(f, 40, 51.0))
    }

    @Test
    fun `tras una lectura normal la regla se rearma`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0, sustained = 20))
        w.glucose(f, 0, 52.0)
        w.glucose(f, 20, 53.0)
        w.glucose(f, 30, 70.0)
        w.glucose(f, 40, 52.0)
        assertEquals(1, w.glucose(f, 60, 54.0))
    }

    @Test
    fun `por encima de X dispara con valores mayores`() = runTest {
        val w = TestWorld()
        val f = w.fastWith(w.glucoseBelow(180.0).copy(conditionKind = com.ayuno.domain.model.RuleConditionKind.METRIC_ABOVE))
        assertEquals(0, w.glucose(f, 0, 150.0))
        assertEquals(1, w.glucose(f, 5, 190.0))
    }

    @Test
    fun `una regla desactivada no dispara`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0).copy(active = false))
        assertEquals(0, w.glucose(f, 0, 40.0))
    }

    @Test
    fun `regla de presion usa el componente y la posicion`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.standingSystolicBelow(90.0))
        val sitting = w.recordBloodPressure(RecordBloodPressure.Input(f.id, base, systolic = 85, diastolic = 60, position = Position.SITTING))
        assertEquals(0, ((sitting as Outcome.Ok).value as RecordResult.Saved).alerts.size)
        val sittingId = (sitting.value as RecordResult.Saved).entry.id
        val standing = w.recordBloodPressure(RecordBloodPressure.Input(f.id, base.plusMinutes(1), systolic = 88, diastolic = 60, position = Position.STANDING, pairedReadingId = sittingId))
        assertEquals(1, ((standing as Outcome.Ok).value as RecordResult.Saved).alerts.size)
    }

    @Test
    fun `ninguna regla cambia el estado del ayuno`() = runTest {
        val w = TestWorld(); val f = w.fastWith(w.glucoseBelow(55.0))
        w.glucose(f, 0, 30.0)
        assertTrue(w.alerts.items.value.isNotEmpty())
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(f.id)!!.fast.status)
    }
}
