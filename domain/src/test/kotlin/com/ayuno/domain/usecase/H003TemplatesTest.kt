package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EditAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class H003TemplatesTest {

    @Test
    fun `H-003 duplicar crea una plantilla independiente con todo el contenido`() = runTest {
        val w = TestWorld()
        val original = w.template(w.plan57h(listOf(w.glucoseBelow(55.0))))
        val copy = (w.duplicateTemplate(original.template.id, "Copia de Ayuno de 57 horas") as Outcome.Ok).value
        assertEquals("Copia de Ayuno de 57 horas", copy.template.name)
        assertEquals(original.template.targetHours, copy.template.targetHours)
        assertEquals(original.template.milestoneHours, copy.template.milestoneHours)
        assertEquals(original.plan.metrics.map { it.metricTypeCode }, copy.plan.metrics.map { it.metricTypeCode })
        assertEquals(original.plan.rules.map { it.name }, copy.plan.rules.map { it.name })
        val originalIds = (original.plan.stages.map { it.id } + original.plan.rules.map { it.id } + original.plan.metrics.map { it.id }).toSet()
        assertTrue((copy.plan.stages.map { it.id } + copy.plan.rules.map { it.id } + copy.plan.metrics.map { it.id }).none { it in originalIds })

        w.saveTemplate(copy.copy(template = copy.template.copy(targetHours = 36.0)))
        assertEquals(57.0, w.templates.get(original.template.id)!!.template.targetHours)
    }

    @Test
    fun `H-003 borrar no toca los ayunos creados desde ella y se puede restaurar con constancia`() = runTest {
        val w = TestWorld()
        val fast = w.plannedFast()
        val templateId = fast.fast.templateId!!
        w.setTemplateDeleted(templateId, true)
        assertNull(w.templates.get(templateId))
        assertEquals(listOf(templateId), w.templates.observeDeleted().first().map { it.id })
        assertTrue(w.templates.observeTemplates().first().isEmpty())
        assertEquals(fast, w.fasts.get(fast.fast.id))

        w.setTemplateDeleted(templateId, false)
        assertEquals(templateId, w.templates.get(templateId)!!.template.id)
        assertEquals(listOf(EditAction.DELETE, EditAction.RESTORE), w.audit.entriesFor(templateId).map { it.action })
        assertEquals("template", w.audit.entriesFor(templateId).first().entityName)
    }

    @Test
    fun `H-003 una regla inactiva puede quedar sin umbral pero al activarla lo exige`() {
        val w = TestWorld()
        val draft = w.glucoseBelow(55.0).copy(threshold = null, active = false)
        assertTrue(RuleValidator.validate(draft).isEmpty())
        assertEquals(listOf(ValidationError.RULE_THRESHOLD_REQUIRED), RuleValidator.validate(draft.copy(active = true)))
    }
}
