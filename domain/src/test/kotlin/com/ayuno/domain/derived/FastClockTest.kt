package com.ayuno.domain.derived

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.StageKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class FastClockTest {

    @Test
    fun `H-004 el fin previsto de un ayuno planificado es inicio previsto mas objetivo`() = runTest {
        val fast = TestWorld().plannedFast().fast
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 0), FastClock.plannedEnd(fast))
    }

    @Test
    fun `H-005 empezar 10 minutos tarde recalcula fin previsto e hitos desde la hora real`() = runTest {
        val fast = TestWorld().startedFast()
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 10), fast.clockStartActual)
        assertEquals(LocalDateTime.of(2026, 10, 25, 7, 10), FastClock.plannedEnd(fast))
        val h48 = FastClock.milestones(fast, java.time.Instant.parse("2026-10-23T03:10:00Z")).first { it.hours == 48.0 }
        assertEquals(LocalDateTime.of(2026, 10, 24, 22, 10), h48.at)
    }

    @Test
    fun `H-006 transcurrido y restante se calculan desde la hora guardada aunque se recree todo`() = runTest {
        val world = TestWorld()
        val fast = world.startedFast()
        world.clock.advanceMinutes(33 * 60 + 12)
        val now = world.clock.instant()
        assertEquals(Duration.ofHours(33).plusMinutes(12), FastClock.elapsed(fast, now))
        // «Reinicio»: un objeto nuevo con los mismos datos guardados da el mismo valor.
        assertEquals(FastClock.elapsed(fast.copy(), now), FastClock.elapsed(fast, now))
        assertEquals(Duration.ofHours(23).plusMinutes(48), FastClock.remaining(fast, now))
    }

    @Test
    fun `H-006 los hitos alcanzados se marcan como alcanzados`() = runTest {
        val world = TestWorld()
        val fast = world.startedFast()
        world.clock.advanceMinutes(25 * 60)
        val milestones = FastClock.milestones(fast, world.clock.instant())
        assertTrue(milestones.first { it.hours == 24.0 }.reached)
        assertFalse(milestones.first { it.hours == 36.0 }.reached)
        assertEquals(listOf(12.0, 24.0, 36.0, 48.0, 57.0), milestones.map { it.hours })
    }

    @Test
    fun `hora del ayuno junto a la fecha real`() = runTest {
        val fast = TestWorld().startedFast()
        assertEquals(33L, FastClock.fastHour(fast, LocalDateTime.of(2026, 10, 24, 7, 15)))
    }

    @Test
    fun `H-004 las etapas se calculan desde la ultima comida, el inicio y el fin`() = runTest {
        val source = TestWorld().plannedFast()
        val windows = StageWindows.of(source.fast, source.plan)
        assertEquals(listOf(StageKind.PREPARATION, StageKind.FAST, StageKind.REFEED, StageKind.POST), windows.map { it.kind })
        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0), windows[0].start)
        assertEquals(LocalDateTime.of(2026, 10, 25, 13, 0), windows[2].end)
        assertEquals(LocalDateTime.of(2026, 10, 29, 13, 0), windows[3].end)
    }
}
