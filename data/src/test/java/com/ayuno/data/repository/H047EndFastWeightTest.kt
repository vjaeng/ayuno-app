package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

/** EV-003 · D-19: el peso al terminar va en la misma transacción que el fin y la vuelta a comer (TRX terminar-ayuno). */
@RunWith(AndroidJUnit4::class)
class H047EndFastWeightTest {

    private val w = DataWorld()
    private val end = LocalDateTime.of(2026, 10, 25, 7, 3)

    @After
    fun tearDown() = w.database.close()

    private suspend fun startedFastWithRefeed(): String {
        val step = RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Una taza de caldo", 0, 60, 120, null)
        val plan = Plan(stages = listOf(Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS)), metrics = emptyList(), rules = emptyList(), refeed = listOf(step))
        val t = TemplateWithPlan(Template(Ids.newId(), "T", 57.0, emptyList()), plan)
        w.saveTemplate(t)
        val id = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast.id
        w.startFast(id)
        w.now = Instant.parse("2026-10-25T12:05:00Z")
        return id
    }

    @Test
    fun `H-030 el peso del fin persiste con la hora de fin junto al fin y las casillas`() = runTest {
        val id = startedFastWithRefeed()
        EndFast(w.fasts, w.refeed, w.catalog, w.records, w.tx, w.clock)(EndFast.Input(id, end, EndReason.GOAL_MET, null, null, EndFast.Weight(150.0, "lb")))
        val weight = w.records.measurements(id).single()
        assertEquals(end, weight.takenAt)
        assertEquals("lb", weight.unitEntered)
        assertEquals(68.039, weight.valueNum!!, 0.001)
        assertEquals(FastStatus.FINISHED, w.fasts.get(id)!!.fast.status)
        assertTrue(w.refeed.checks(id).isNotEmpty())
    }

    @Test
    fun `H-047 si falla el guardado del peso no queda ni el fin ni las casillas`() = runTest {
        val id = startedFastWithRefeed()
        val failing = object : RecordRepository by w.records {
            override suspend fun insertMeasurement(measurement: Measurement) = throw IllegalStateException("disco lleno")
        }
        val result = runCatching {
            EndFast(w.fasts, w.refeed, w.catalog, failing, w.tx, w.clock)(EndFast.Input(id, end, EndReason.GOAL_MET, null, null, EndFast.Weight(70.0, "kg")))
        }
        assertTrue(result.isFailure)
        assertEquals(FastStatus.IN_PROGRESS, w.fasts.get(id)!!.fast.status)
        assertTrue(w.refeed.checks(id).isEmpty())
        assertTrue(w.records.measurements(id).isEmpty())
    }
}
