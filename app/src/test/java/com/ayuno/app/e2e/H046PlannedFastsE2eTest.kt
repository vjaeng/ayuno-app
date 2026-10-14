package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.FastStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** EV-002: H-046 (ayunos planificados), H-006 (Inicio sin ayuno simplificado) y H-004 (semana de separación). */
@RunWith(AndroidJUnit4::class)
class H046PlannedFastsE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val first = "jueves 22 de octubre, 18:00"
    private val second = "jueves 5 de noviembre, 18:00"

    @Test
    fun `H046 la pestana lista los planificados en orden y se empieza el elegido desde su detalle`() {
        val template = seedTemplate()
        val laterId = seedPlannedFast(template, LocalDateTime.of(2026, 11, 5, 18, 0))
        seedPlannedFast(template)
        compose.tap("Planificados")
        compose.waitForText(second)
        val top = { text: String -> compose.onAllNodesWithText(text)[0].fetchSemanticsNode().boundsInRoot.top }
        assertTrue("el más próximo va primero", top(first) < top(second))
        compose.assertShown("57 horas · Ayuno de 57 horas", substring = true)

        compose.tap(second)
        compose.assertShown("Ayuno del jueves 5 de noviembre")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("Anotar") // de vuelta en Inicio con el reloj
        compose.eventually { assertEquals(FastStatus.IN_PROGRESS, runBlocking { app.container.fasts.get(laterId)!!.fast.status }) }

        compose.tap("Planificados")
        compose.waitForText(first)
        assertTrue(compose.onAllNodesWithText(second).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `H046 con otro en curso se puede planificar pero no empezar y se explica por que`() {
        val template = seedTemplate()
        seedStartedFast(template)
        val nextId = seedPlannedFast(template, LocalDateTime.of(2026, 11, 5, 18, 0))
        compose.tap("Planificados")
        compose.tap(second)
        compose.assertShown("Termina el ayuno en curso para poder empezar este.")
        assertTrue(compose.onAllNodesWithText("Empieza el ayuno ahora").fetchSemanticsNodes().isEmpty())
        assertEquals(FastStatus.PLANNED, runBlocking { app.container.fasts.get(nextId)!!.fast.status })
    }

    @Test
    fun `H046 sin planificados la pestana explica como planificar y al crear vuelve a la lista`() {
        seedTemplate()
        compose.tap("Planificados")
        compose.assertShown("No tienes ayunos planificados.", substring = true)
        compose.tap("Planificar ayuno")
        compose.tap("Ayuno de 57 horas · 57 h")
        compose.tap("Crear ayuno")
        compose.assertShown(first)
        compose.eventually { assertEquals(1, runBlocking { app.container.fasts.all() }.size) }
    }

    @Test
    fun `H046 desde el menu del detalle se borra y desaparece de la lista`() {
        val template = seedTemplate()
        val id = seedPlannedFast(template)
        compose.tap("Planificados")
        compose.tap(first)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(MENU).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(MENU).performClick()
        compose.tap("Borrar ayuno")
        compose.tap("Borrar para siempre")
        compose.assertShown("No tienes ayunos planificados.", substring = true)
        compose.eventually { assertEquals(null, runBlocking { app.container.fasts.get(id) }) }
    }

    @Test
    fun `H006 sin ayuno en curso Inicio muestra solo el proximo con una accion principal`() {
        val template = seedTemplate()
        seedPlannedFast(template, LocalDateTime.of(2026, 11, 5, 18, 0))
        seedPlannedFast(template)
        compose.assertShown("Próximo ayuno")
        compose.assertShown("Última comida prevista: $first")
        compose.assertShown("Empieza el ayuno ahora")
        compose.assertShown("Ver y preparar este ayuno")
        listOf("Registrar la última comida", "Cambiar", "Planificar ayuno", second).forEach { text ->
            assertTrue("«$text» no debe estar en Inicio", compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty())
        }
        compose.tap("Ver y preparar este ayuno")
        compose.assertShown("Ayuno del jueves 22 de octubre")
    }

    @Test
    fun `H004 planificar a menos de una semana del anterior muestra con cual choca y no se guarda`() {
        seedPlannedFast(seedTemplate())
        compose.tap("Planificados")
        compose.tap("Planificar ayuno")
        compose.tap("Ayuno de 57 horas · 57 h")
        compose.tap("Crear ayuno") // propone el jueves 22 a las 18:00, igual que el ya planificado
        compose.assertShown(
            "Tiene que pasar una semana desde el ayuno que termina el domingo 25 de octubre, 07:00. Puedes planificarlo desde el domingo 1 de noviembre, 07:00.",
        )
        assertEquals(1, runBlocking { app.container.fasts.all() }.size)
    }

    @Test
    fun `H004 al planificar viene propuesta la primera plantilla, no desde cero`() {
        val template = seedTemplate()
        compose.tap("Planificados")
        compose.tap("Planificar ayuno")
        compose.tap("Crear ayuno")
        compose.eventually { assertEquals(template.template.id, runBlocking { app.container.fasts.all() }.single().templateId) }
    }
}

private const val MENU = "Más acciones de este ayuno"
