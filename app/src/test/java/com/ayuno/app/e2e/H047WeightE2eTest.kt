package com.ayuno.app.e2e

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

/** EV-003 · H-047 (con H-005, H-030 y H-033): peso al empezar y al terminar, y cuánto cambió. */
@RunWith(AndroidJUnit4::class)
class H047WeightE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun weights(id: String) = runBlocking { app.container.records.measurements(id) }.filter { it.metricTypeCode == "weight" }

    @Test
    fun `H047 al empezar se pide el peso y se guarda con la hora y la unidad`() {
        val id = seedPlannedFast(seedTemplate())
        app.clock.now = Instant.parse("2026-10-23T03:10:00Z")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("¿Cuánto pesas al empezar?")
        compose.assertShown("El ayuno ya ha empezado.")
        compose.type("Valor", "71,4")
        compose.tap("Guardar")
        compose.eventually { assertEquals(1, weights(id).size) }
        val m = weights(id).single()
        assertEquals(71.4, m.valueEntered!!, 0.0)
        assertEquals("kg", m.unitEntered)
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 10), m.takenAt)
        compose.eventually { assertTrue(compose.onAllNodesWithTextList("¿Cuánto pesas al empezar?").isEmpty()) }
    }

    @Test
    fun `H047 Ahora no salta el peso sin guardar nada y no se vuelve a preguntar`() {
        val id = seedPlannedFast(seedTemplate())
        app.clock.now = Instant.parse("2026-10-23T03:10:00Z")
        compose.tap("Empieza el ayuno ahora")
        compose.tap("Ahora no")
        compose.assertShown("Anotar")
        compose.activityRule.scenario.recreate()
        compose.assertShown("Anotar")
        assertTrue(compose.onAllNodesWithTextList("¿Cuánto pesas al empezar?").isEmpty())
        assertTrue(weights(id).isEmpty())
    }

    @Test
    fun `H047 con Peso desactivado no se pregunta`() {
        runBlocking { app.container.setMetricTypeActive("weight", false) }
        seedPlannedFast(seedTemplate())
        app.clock.now = Instant.parse("2026-10-23T03:10:00Z")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("Anotar")
        assertTrue(compose.onAllNodesWithTextList("¿Cuánto pesas al empezar?").isEmpty())
    }

    @Test
    fun `H030 H033 terminar con peso muestra cuanto cambio en el resultado y en el detalle`() {
        val id = seedStartedFast(seedTemplate())
        runBlocking {
            app.container.recordMeasurement(RecordMeasurement.Input(id, "weight", LocalDateTime.of(2026, 10, 22, 22, 10), valueEntered = 72.0, unitEntered = "kg"))
        }
        app.clock.advanceMinutes(57 * 60)
        compose.activityRule.scenario.recreate()
        compose.tap("Seguridad")
        compose.tap("Terminar el ayuno")
        compose.tap("Objetivo cumplido")
        compose.type("Peso (opcional)", "69,5")
        compose.onAllNodes(hasText("Terminar el ayuno") and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        compose.assertShown("Ayuno terminado")
        compose.assertShown("Peso: 72 kg → 69,5 kg · −2,5 kg")
        val end = weights(id).maxBy { it.takenAt }
        assertEquals(69.5, end.valueNum!!, 0.0)
        assertEquals(runBlocking { app.container.fasts.get(id)!!.fast.endActual }, end.takenAt)
        assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty())

        compose.tap("Volver al inicio")
        compose.tap("Historial")
        compose.waitForText("jueves 22 de octubre", substring = true)
        compose.onAllNodes(hasText("jueves 22 de octubre", substring = true) and hasClickAction()).onFirst().performClick()
        compose.assertShown("Peso: 72 kg → 69,5 kg · −2,5 kg")
    }
}

private fun Rule4.onAllNodesWithTextList(text: String) = onAllNodes(hasText(text)).fetchSemanticsNodes()
