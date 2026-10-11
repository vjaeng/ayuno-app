package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** H-045 (borrar un ayuno para siempre) y H-044 (acceso a «Cambiar» desde el detalle del planificado, EV-002). */
@RunWith(AndroidJUnit4::class)
class H045DeleteFastE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun openDetail(tab: String, rowText: String) {
        compose.tap(tab)
        compose.tap(rowText)
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(MENU).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(MENU).performClick()
    }

    @Test
    fun `H045 borrar un ayuno en curso desde su detalle lo quita con sus registros y avisa de los recordatorios`() {
        val id = seedStartedFast(seedTemplate())
        runBlocking {
            app.container.recordMeasurement(
                RecordMeasurement.Input(id, "glucose", LocalDateTime.of(2026, 10, 22, 22, 10), valueEntered = 80.0, unitEntered = "mg/dL", source = GlucoseSource.GLUCOMETER),
            )
        }
        openDetail("Historial", "● En curso · desde el jueves 22 de octubre")
        compose.tap("Borrar ayuno")
        compose.assertShown("¿Borrar este ayuno para siempre?")
        compose.assertShown("y su registro, con su plan y sus cambios. No se puede deshacer.", substring = true)
        compose.assertShown("Sus recordatorios dejarán de sonar.", substring = true)
        compose.tap("Borrar para siempre")
        compose.assertShown("Aún no has hecho ningún ayuno.")
        compose.eventually { assertTrue(runBlocking { app.container.fasts.all() }.isEmpty()) }
        assertTrue(runBlocking { app.container.records.measurements(id) }.isEmpty())
        assertNull(runBlocking { app.container.fasts.get(id) })
    }

    @Test
    fun `H045 cancelar no borra nada`() {
        val id = seedPlannedFast(seedTemplate())
        openDetail("Planificados", "jueves 22 de octubre, 18:00")
        compose.tap("Borrar ayuno")
        compose.tap("Cancelar")
        compose.assertShown("Ayuno del jueves 22 de octubre")
        assertNotNull(runBlocking { app.container.fasts.get(id) })
    }

    @Test
    fun `H044 desde el detalle de un ayuno planificado se cambian sus notas`() {
        val id = seedPlannedFast(seedTemplate())
        openDetail("Planificados", "jueves 22 de octubre, 18:00")
        compose.tap("Cambiar")
        compose.assertShown("Cambiar ayuno")
        compose.type("Notas del ayuno (opcional)", "Semana tranquila")
        compose.tap("Guardar cambios")
        compose.eventually { assertEquals("Semana tranquila", runBlocking { app.container.fasts.get(id)!!.fast.notes }) }
        compose.assertShown("Notas: Semana tranquila") // vuelve al detalle del planificado
    }
}

private const val MENU = "Más acciones de este ayuno"
