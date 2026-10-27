package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.EditAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H-051 (y H-050 en el formulario): botones rápidos de Inicio, «Deshacer», pulsación larga y agua con sal. */
@RunWith(AndroidJUnit4::class)
class H051QuickIntakeE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `H051 un toque guarda 1 vaso de agua sin formulario y Deshacer lo quita dejando bitacora`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.assertShown("+1 pizca de sal")
        compose.assertShown("Mantén pulsado para cambiar la cantidad")
        compose.tap("+1 vaso de agua")
        compose.assertShown("Agua: +1 vaso (250 mL)")
        compose.eventually { assertEquals(1, runBlocking { app.container.records.intakes(id) }.size) }
        val intake = runBlocking { app.container.records.intakes(id) }.single()
        assertEquals(250.0, intake.amount, 0.0)
        assertEquals("glass", intake.unitEntered)
        assertEquals(1.0, intake.amountEntered, 0.0)
        compose.assertShown("Agua: 250 mL", substring = true)

        compose.tap("Deshacer")
        compose.eventually { assertTrue(runBlocking { app.container.records.intakes(id) }.isEmpty()) }
        assertTrue(runBlocking { app.container.audit.entriesFor(intake.id) }.any { it.action == EditAction.DELETE })
    }

    @Test
    fun `H051 mantener pulsado abre el formulario con ese tipo y esa medida para cambiar la cantidad`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.waitForText("+1 pizca de sal")
        compose.node("+1 pizca de sal").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.assertShown("1 pizca son unos 0,3 g.", substring = true)
        compose.type("Cantidad", "2")
        compose.assertShown("= 0,6 g", substring = true)
        compose.tap("Guardar")
        compose.eventually { assertEquals(1, runBlocking { app.container.records.intakes(id) }.size) }
        val intake = runBlocking { app.container.records.intakes(id) }.single()
        assertEquals("salt", intake.intakeTypeCode)
        assertEquals(0.6, intake.amount, 1e-9)
        assertEquals(2.0, intake.amountEntered, 0.0)
        assertEquals("pinch", intake.unitEntered)
    }

    @Test
    fun `H051 agua con sal disuelta guarda dos ingestas con la misma hora y el mismo grupo`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Líquido"); compose.tap("Agua (mL)")
        compose.tap("vaso")
        compose.type("Cantidad", "1")
        compose.tap("Lleva sal disuelta")
        compose.type("Sal", "1")
        compose.tap("¼ de cucharadita")
        compose.tap("Guardar")
        compose.eventually { assertEquals(2, runBlocking { app.container.records.intakes(id) }.size) }
        val intakes = runBlocking { app.container.records.intakes(id) }.associateBy { it.intakeTypeCode }
        assertEquals(250.0, intakes.getValue("water").amount, 0.0)
        assertEquals(1.25, intakes.getValue("salt").amount, 1e-9)
        assertNotNull(intakes.getValue("water").groupId)
        assertEquals(intakes.getValue("water").groupId, intakes.getValue("salt").groupId)
        assertEquals(intakes.getValue("water").takenAt, intakes.getValue("salt").takenAt)

        // Historial: una sola fila, como se anotó y con su equivalente.
        compose.tap("Historial")
        compose.tapContaining("En curso")
        compose.assertShown("Agua 1 vaso · 250 mL con Sal ¼ de cucharadita · 1,25 g", substring = true)
    }
}
