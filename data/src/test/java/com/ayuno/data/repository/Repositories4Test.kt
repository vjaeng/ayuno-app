package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.DuplicateTemplate
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.SetTemplateDeleted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** H-003 con Room: plantilla de ejemplo al instalar, duplicar, borrar y restaurar. */
@RunWith(AndroidJUnit4::class)
class Repositories4Test {

    private val w = DataWorld()

    @After
    fun tearDown() = w.database.close()

    private suspend fun example() = w.templates.observeTemplates().first().single { it.name == "Ejemplo (sin valores clínicos)" }

    @Test
    fun `H-003 al instalar hay una plantilla de ejemplo con etapas, mediciones y reglas sin umbrales ni productos`() = runTest {
        val plan = w.templates.get(example().id)!!.plan
        assertEquals(setOf(StageKind.PREPARATION, StageKind.REFEED, StageKind.POST), plan.stages.map { it.kind }.toSet())
        // Todas las mediciones del catálogo; solo glucosa, peso y presión con horario.
        assertEquals(w.catalog.metricTypes().map { it.code }.toSet(), plan.metrics.map { it.metricTypeCode }.toSet())
        assertEquals(setOf("glucose", "weight", "blood_pressure"), plan.metrics.filter { it.schedules.isNotEmpty() }.map { it.metricTypeCode }.toSet())
        assertEquals(setOf(RuleConditionKind.METRIC_BELOW, RuleConditionKind.MANUAL), plan.rules.map { it.conditionKind }.toSet())
        assertTrue(plan.rules.all { it.threshold == null })
        assertTrue(plan.products.isEmpty())
        // Se edita como cualquier otra: guardar sin tocar nada es válido.
        assertTrue(w.saveTemplate(w.templates.get(example().id)!!) is Outcome.Ok)
    }

    @Test
    fun `H-003 la de ejemplo se duplica, se borra sin tocar los ayunos y se restaura`() = runTest {
        val id = example().id
        val copy = (DuplicateTemplate(w.templates)(id, "Copia de Ejemplo") as Outcome.Ok).value
        assertEquals(w.templates.get(id)!!.plan.metrics.size, w.templates.get(copy.template.id)!!.plan.metrics.size)

        val fast = (w.createFast(CreateFast.Input(id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 36.0, null)) as Outcome.Ok).value
        val delete = SetTemplateDeleted(w.templates, w.audit, w.tx, w.clock)
        delete(id, true)
        assertNull(w.templates.get(id))
        assertEquals(listOf(id), w.templates.observeDeleted().first().map { it.id })
        assertEquals(fast.plan.metrics.size, w.fasts.get(fast.fast.id)!!.plan.metrics.size)

        delete(id, false)
        assertEquals(w.catalog.metricTypes().size, w.templates.get(id)!!.plan.metrics.size)
        assertTrue(w.templates.observeDeleted().first().isEmpty())
    }
}
