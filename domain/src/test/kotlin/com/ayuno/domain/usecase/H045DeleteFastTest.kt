package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class H045DeleteFastTest {

    @Test
    fun `H-045 borra un ayuno planificado, en curso o terminado`() = runTest {
        val w = TestWorld()
        val planned = w.plannedFast()
        assertEquals(Outcome.Ok(Unit), w.deleteFast(planned.fast.id))
        assertNull(w.fasts.get(planned.fast.id))

        val started = w.startedFast()
        assertEquals(FastStatus.IN_PROGRESS, started.status)
        w.deleteFast(started.id)
        assertTrue(w.fasts.all().none { it.status == FastStatus.IN_PROGRESS })

        val ended = w.startedFast()
        w.clock.advanceMinutes(60)
        w.endFast(EndFast.Input(ended.id, com.ayuno.domain.time.LocalTimes.now(w.clock), EndReason.PERSONAL_DECISION, null, null))
        w.deleteFast(ended.id)
        assertTrue(w.fasts.all().isEmpty())
    }

    @Test
    fun `H-045 la plantilla de origen y los demas ayunos no cambian`() = runTest {
        val w = TestWorld()
        val keep = w.plannedFast()
        val gone = w.plannedFast(weeksLater = 2)
        val template = w.templates.get(gone.fast.templateId!!)
        w.deleteFast(gone.fast.id)
        assertEquals(keep, w.fasts.get(keep.fast.id))
        assertEquals(template, w.templates.get(gone.fast.templateId!!))
    }

    @Test
    fun `H-045 un ayuno inexistente o ya borrado no escribe nada`() = runTest {
        val w = TestWorld()
        val f = w.plannedFast()
        w.deleteFast(f.fast.id)
        assertEquals(listOf(ValidationError.FAST_NOT_FOUND), (w.deleteFast(f.fast.id) as Outcome.Invalid).errors)
        assertNotNull(w.templates.get(f.fast.templateId!!))
    }
}
