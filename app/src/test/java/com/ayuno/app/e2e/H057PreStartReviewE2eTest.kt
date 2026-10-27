package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.WarningKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H-057 (y H-003): la revisión antes de empezar con la plantilla de ejemplo sin tocar. */
@RunWith(AndroidJUnit4::class)
class H057PreStartReviewE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun example() = runBlocking {
        val summary = app.container.templates.observeTemplates().first().single { it.name == "Ejemplo (sin valores clínicos)" }
        app.container.templates.get(summary.id)!!
    }

    @Test
    fun `H057 la revision lista lo que falta, arreglar una meta la actualiza y empezar igual lo deja guardado`() {
        val id = seedPlannedFast(example())
        compose.activityRule.scenario.recreate()
        compose.tap("Ver y preparar este ayuno")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("Antes de empezar")
        compose.assertShown("Reglas de ejemplo sin tocar (3)")
        compose.assertShown("Metas sin cifra (2)")

        // Arreglar: la meta de agua en el plan de este ayuno; al volver, la lista se recalcula.
        compose.tap("Agua")
        compose.assertShown("Estás cambiando el plan de este ayuno, no la plantilla.")
        compose.type("Meta diaria", "2000")
        compose.tap("Guardar")
        compose.assertShown("Metas sin cifra (1)")
        assertEquals(FastStatus.PLANNED, runBlocking { app.container.fasts.get(id) }!!.fast.status)

        compose.tap("Empezar de todos modos")
        compose.eventually { assertEquals(FastStatus.IN_PROGRESS, runBlocking { app.container.fasts.get(id) }!!.fast.status) }
        val warnings = runBlocking { app.container.fasts.get(id) }!!.fast.startedWithWarnings!!
        assertEquals(3, warnings.count { it.kind == WarningKind.PLACEHOLDER_RULE })
        assertEquals(listOf("Sal"), warnings.filter { it.kind == WarningKind.GOAL_WITHOUT_AMOUNT }.map { it.text })

        compose.tap("Historial")
        compose.tapContaining("En curso")
        compose.tap("Empezó con pendientes (4)")
        compose.assertShown("Metas sin cifra (1)")
    }
}
