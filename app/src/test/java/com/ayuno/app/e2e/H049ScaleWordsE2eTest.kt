package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Rule as JRule

/** EV-005 · H-049: anotar y corregir las escalas tocando una palabra; editar sus palabras en Catálogos. */
@RunWith(AndroidJUnit4::class)
class H049ScaleWordsE2eTest {

    @get:JRule
    val compose = createAndroidComposeRule<MainActivity>()

    private val hungerHigh = Rule(
        Ids.newId(), "Hambre alta", true, RuleConditionKind.METRIC_ABOVE, "hunger", null, 6.0, null, null, null, null,
        emptyList(), false, null, RuleLevel.NOTICE, "Bebe agua con sal y espera media hora.", RuleAction.NONE, null,
    )

    /** Ayuno en curso; reloj el viernes 23 a las 12:00. */
    private fun startedFast(rules: List<Rule> = emptyList()): String {
        val fastId = seedStartedFast(seedTemplate(rules))
        app.clock.now = Instant.parse("2026-10-23T17:00:00Z")
        compose.activityRule.scenario.recreate()
        return fastId
    }

    private fun hunger(fastId: String) = runBlocking { app.container.records.measurements(fastId) }.filter { it.metricTypeCode == "hunger" }

    @Test
    fun `H049 anoto el hambre tocando una palabra y la veo en Inicio y en la lista`() {
        val fastId = startedFast()
        compose.tap("Anotar"); compose.tap("Hambre")
        compose.assertShown("Moderada")
        compose.tap("Guardar")
        compose.assertShown("Elige una opción o marca «No se hizo».")
        compose.tap("Mucha"); compose.tap("Guardar")
        compose.eventually { assertEquals(listOf(7.0 to "de 0 a 10"), hunger(fastId).map { it.valueNum to it.unitEntered }) }
        compose.assertShown("Hambre: Mucha · 7", substring = true)
        compose.tap("Mediciones de este ayuno")
        compose.assertShown("Hambre Mucha · 7")
    }

    @Test
    fun `H049 una regla sobre la escala salta al elegir la palabra que la cumple`() {
        startedFast(listOf(hungerHigh))
        compose.tap("Anotar"); compose.tap("Hambre"); compose.tap("Mucha"); compose.tap("Guardar")
        compose.assertShown("Bebe agua con sal y espera media hora.")
    }

    @Test
    fun `H049 un numero tecleado antes se sigue viendo como numero y se corrige con las palabras`() {
        val fastId = startedFast()
        runBlocking {
            app.container.recordMeasurement(RecordMeasurement.Input(fastId, "hunger", LocalDateTime.of(2026, 10, 23, 9, 0), valueEntered = 4.0, unitEntered = "de 0 a 10"))
        }
        compose.activityRule.scenario.recreate()
        compose.tap("Mediciones de este ayuno")
        compose.tap("Hambre 4 de 0 a 10")
        compose.tap("Intensa"); compose.tap("Guardar")
        compose.assertShown("Hambre Intensa · 10")
        val m = hunger(fastId).single()
        assertEquals(10.0, m.valueNum!!, 0.0)
        val log = runBlocking { app.container.audit.entriesFor(m.id) }
        assertEquals("4" to "10", log.first { it.field == "value_num" }.let { it.oldValue to it.newValue })
    }

    @Test
    fun `H049 quitar una palabra en Catalogos no cambia lo anotado`() {
        val fastId = startedFast()
        compose.tap("Anotar"); compose.tap("Hambre"); compose.tap("Mucha"); compose.tap("Guardar")
        compose.eventually { assertEquals(1, hunger(fastId).size) }
        compose.tap("Más"); compose.tap("Catálogos"); compose.tap("Mediciones"); compose.tap("Hambre")
        compose.assertShown("Palabras para elegir")
        compose.tapDescription("Quitar Mucha")
        compose.tap("Guardar")
        compose.eventually {
            val options = runBlocking { app.container.catalog.metricType("hunger") }!!.options
            assertEquals(listOf("Nada", "Poca", "Moderada", "Intensa"), options.map { it.label })
        }
        assertEquals(7.0, hunger(fastId).single().valueNum!!, 0.0)
    }
}
