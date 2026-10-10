package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordMeal
import com.ayuno.domain.usecase.SetRefeedCheck
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class Repositories3Test {

    private val w = DataWorld()

    @After
    fun tearDown() = w.database.close()

    private val broth = RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Una taza de caldo", 0, 60, 120, null)
    private val supplements = RefeedStep(Ids.newId(), RefeedStepKind.REINTRODUCTION, "Vuelven los suplementos", null, null, null, 2)
    private val plan = Plan(
        stages = listOf(Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS)), metrics = emptyList(), rules = emptyList(),
        refeed = listOf(broth, supplements),
    )

    @Test
    fun `H-031 los pasos se guardan en la plantilla, quitar uno lo borra en blando y el ayuno los copia`() = runTest {
        val t = TemplateWithPlan(Template(Ids.newId(), "T", 57.0, emptyList()), plan)
        w.saveTemplate(t)
        assertEquals(plan.refeed, w.templates.get(t.template.id)!!.plan.refeed)
        w.saveTemplate(t.copy(plan = plan.copy(refeed = listOf(supplements))))
        assertEquals(listOf(supplements), w.templates.get(t.template.id)!!.plan.refeed)
        assertEquals(1, w.database.refeedDao().steps(t.template.id, null).size)

        val fast = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value
        val copy = w.fasts.get(fast.fast.id)!!.plan.refeed.single()
        assertEquals("Vuelven los suplementos", copy.description)
        assertTrue(copy.id != supplements.id)
    }

    @Test
    fun `H-030 H-031 al terminar se guardan las casillas y marcar una persiste la hora real`() = runTest {
        val t = TemplateWithPlan(Template(Ids.newId(), "T", 57.0, emptyList()), plan)
        w.saveTemplate(t)
        val fastId = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast.id
        w.startFast(fastId)
        w.now = Instant.parse("2026-10-25T12:03:00Z")
        val end = LocalDateTime.of(2026, 10, 25, 7, 3)
        EndFast(w.fasts, w.refeed, w.tx, w.clock)(EndFast.Input(fastId, end, EndReason.GOAL_MET, null, null))

        val checks = w.refeed.checks(fastId)
        assertEquals(listOf(end, end.plusHours(1), LocalDateTime.of(2026, 10, 27, 0, 0)), checks.map { it.scheduledAt })
        w.now = Instant.parse("2026-10-25T12:05:00Z")
        SetRefeedCheck(w.refeed, w.audit, w.tx, w.clock)(checks.first().id, true)
        assertEquals(end.plusMinutes(2), w.refeed.check(checks.first().id)!!.doneAt)
        SetRefeedCheck(w.refeed, w.audit, w.tx, w.clock)(checks.first().id, false)
        assertNull(w.refeed.check(checks.first().id)!!.doneAt)
        assertEquals("refeed_check", w.audit.entriesFor(checks.first().id).single().entityName)
    }

    @Test
    fun `H-032 la comida se persiste con todos sus campos`() = runTest {
        val fastId = (w.createFast(CreateFast.Input(null, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast.id
        val at = LocalDateTime.of(2026, 10, 25, 9, 40, 12)
        RecordMeal(w.fasts, w.refeed)(RecordMeal.Input(fastId, at, "Puré de calabaza", "un cuenco", MealTolerance.OTHER, "acidez"))
        val meal = w.refeed.meals(fastId).single()
        assertEquals(at, meal.eatenAt)
        assertEquals("un cuenco", meal.approxAmount)
        assertEquals(MealTolerance.OTHER, meal.tolerance)
        assertEquals("acidez", meal.toleranceDetail)
    }
}
