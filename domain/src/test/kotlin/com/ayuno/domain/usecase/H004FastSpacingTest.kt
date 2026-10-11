package com.ayuno.domain.usecase

import com.ayuno.domain.derived.FastSpacing
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.time.LocalTimes
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** EV-002 · D-17: una semana de separación entre ayunos al crear (H-004) y al cambiar las horas (H-044). */
class H004FastSpacingTest {

    private suspend fun TestWorld.create(lastMeal: LocalDateTime, hours: Double = 57.0): Outcome<FastWithPlan> {
        val template = template()
        return createFast(CreateFast.Input(template.template.id, lastMeal, lastMeal.plusHours(4), hours, null))
    }

    private fun Outcome<*>.errors() = (this as Outcome.Invalid).errors

    // El ayuno base: última comida jueves 22 oct 18:00, fin previsto domingo 25 oct 07:00.
    private val baseEnd = LocalDateTime.of(2026, 10, 25, 7, 0)

    @Test
    fun `H-004 un ayuno a menos de una semana del fin del anterior no se guarda`() = runTest {
        val w = TestWorld()
        w.plannedFast()
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.create(baseEnd.plusDays(7).minusMinutes(1)).errors())
        assertEquals(1, w.fasts.all().size)
    }

    @Test
    fun `H-004 exactamente una semana despues del fin se acepta`() = runTest {
        val w = TestWorld()
        w.plannedFast()
        assertTrue(w.create(baseEnd.plusDays(7)) is Outcome.Ok)
    }

    @Test
    fun `H-004 un ayuno que termina a menos de una semana del siguiente no se guarda`() = runTest {
        val w = TestWorld()
        val later = w.plannedFast(weeksLater = 2) // última comida jueves 5 nov 18:00
        val result = w.create(LocalDateTime.of(2026, 10, 27, 18, 0))
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), result.errors())
        assertTrue(w.create(LocalDateTime.of(2026, 10, 22, 18, 0)) is Outcome.Ok)

        val candidate = later.fast.copy(id = "x", lastMealPlanned = LocalDateTime.of(2026, 10, 27, 18, 0), clockStartPlanned = LocalDateTime.of(2026, 10, 27, 22, 0))
        val conflict = FastSpacing.conflict(candidate, listOf(later.fast), LocalTimes.now(w.clock))!!
        assertEquals(later.fast.id, conflict.other.id)
        assertEquals(LocalDateTime.of(2026, 10, 29, 18, 0), conflict.latestEnd)
        assertEquals(null, conflict.earliestLastMeal)
    }

    @Test
    fun `H-004 se puede planificar con otro en curso, a una semana de su fin previsto`() = runTest {
        val w = TestWorld()
        w.startedFast() // empezó a las 22:10: fin previsto domingo 25 oct 07:10
        val inProgressEnd = LocalDateTime.of(2026, 10, 25, 7, 10)
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.create(inProgressEnd.plusDays(7).minusMinutes(1)).errors())
        assertTrue(w.create(inProgressEnd.plusDays(7)) is Outcome.Ok)
    }

    @Test
    fun `H-004 un ayuno en curso que pasa de su fin previsto alarga su franja hasta ahora`() = runTest {
        val w = TestWorld()
        w.startedFast()
        w.clock.advanceMinutes(5 * 24 * 60) // martes 27 oct 22:10
        val now = LocalTimes.now(w.clock)
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.create(now.plusDays(7).minusMinutes(1)).errors())
        assertTrue(w.create(now.plusDays(7)) is Outcome.Ok)
    }

    @Test
    fun `H-004 cuenta tambien contra un terminado, desde su fin real`() = runTest {
        val w = TestWorld()
        val fast = w.startedFast()
        w.clock.advanceMinutes(24 * 60)
        val end = LocalTimes.now(w.clock) // viernes 23 oct 22:10
        w.endFast(EndFast.Input(fast.id, end, EndReason.PERSONAL_DECISION, null, null))
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.create(end.plusDays(7).minusSeconds(1)).errors())
        assertTrue(w.create(end.plusDays(7)) is Outcome.Ok)
    }

    @Test
    fun `H-044 cambiar las horas no cuenta el propio ayuno y respeta a los demas`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        w.plannedFast(weeksLater = 2)
        val shifted = ChangePlannedFast.Input(f.fast.id, f.fast.templateId, f.fast.lastMealPlanned.plusHours(2), f.fast.clockStartPlanned.plusHours(2), 57.0, null)
        assertTrue(w.changePlannedFast(shifted) is Outcome.Ok)

        val tooLate = shifted.copy(lastMealPlanned = LocalDateTime.of(2026, 10, 27, 18, 0), clockStartPlanned = LocalDateTime.of(2026, 10, 27, 22, 0))
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.changePlannedFast(tooLate).errors())
        assertEquals(f.fast.lastMealPlanned.plusHours(2), w.fasts.get(f.fast.id)!!.fast.lastMealPlanned)
    }

    @Test
    fun `H-044 cambiar solo las notas no comprueba la separacion`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        // Un archivo restaurado (H-040) puede traer ayunos más juntos: la importación no aplica D-17.
        val close = w.plannedFast(weeksLater = 2).fast
        w.fasts.update(close.copy(lastMealPlanned = LocalDateTime.of(2026, 10, 28, 18, 0), clockStartPlanned = LocalDateTime.of(2026, 10, 28, 22, 0)))
        val notesOnly = ChangePlannedFast.Input(f.fast.id, f.fast.templateId, f.fast.lastMealPlanned, f.fast.clockStartPlanned, f.fast.targetHours, "Semana tranquila")
        assertTrue(w.changePlannedFast(notesOnly) is Outcome.Ok)
    }

    @Test
    fun `H-004 un doble toque con los mismos datos crea un solo ayuno`() = runTest {
        val w = TestWorld()
        val template = w.template()
        val input = CreateFast.Input(template.template.id, LocalDateTime.of(2026, 11, 2, 20, 0), LocalDateTime.of(2026, 11, 3, 0, 0), 36.0, null)
        assertTrue(w.createFast(input) is Outcome.Ok)
        assertEquals(listOf(ValidationError.FAST_TOO_CLOSE), w.createFast(input).errors())
        assertEquals(1, w.fasts.all().size)
    }
}
