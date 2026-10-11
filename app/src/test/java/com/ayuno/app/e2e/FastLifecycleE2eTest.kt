package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

/** H-004, H-005, H-006, H-028, H-030: crear, empezar, ver el reloj y terminar el ayuno desde la interfaz. */
@RunWith(AndroidJUnit4::class)
class FastLifecycleE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `H004 sin ayunos la pantalla explica como empezar y crea un ayuno desde una plantilla`() {
        compose.assertShown("Crea tu primer ayuno", substring = true)
        seedTemplate()
        compose.tap("Planificar ayuno")
        compose.tap("Ayuno de 57 horas · 57 h")
        compose.assertShown("Objetivo: 57 horas") // calculado: jueves 22:00 + 57 h propuesto por la plantilla
        compose.tap("Crear ayuno")
        compose.assertShown("jueves 22 de octubre, 18:00") // de vuelta en Planificados (H-046)
        compose.tap("Inicio")
        compose.assertShown("Próximo ayuno")
        compose.assertShown("Fin previsto: domingo 25 de octubre, 07:00", substring = true)
        compose.assertShown("Empieza el ayuno ahora")
        compose.eventually { assertEquals(1, runBlocking { app.container.fasts.all() }.size) }
        val fast = runBlocking { app.container.fasts.all().single() }
        assertEquals(FastStatus.PLANNED, fast.status)
        assertEquals(57.0, fast.targetHours, 0.0)
        compose.eventually { assertEquals(2, runBlocking { app.container.fasts.get(fast.id)!!.plan.metrics.size }) }
    }

    @Test
    fun `H005 empezar el ayuno guarda la hora real y muestra el reloj`() {
        val id = seedPlannedFast(seedTemplate())
        app.clock.now = Instant.parse("2026-10-23T03:10:07Z")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("0 min")
        compose.assertShown("Anotar")
        compose.eventually { assertEquals(FastStatus.IN_PROGRESS, runBlocking { app.container.fasts.get(id)!!.fast }.status) }
        val fast = runBlocking { app.container.fasts.get(id)!!.fast }
        assertEquals(LocalDateTime.of(2026, 10, 22, 22, 10, 7), fast.clockStartActual)
    }

    @Test
    fun `H005 registrar la ultima comida real por defecto a la hora planificada`() {
        val id = seedPlannedFast(seedTemplate())
        compose.tap("Ver y preparar este ayuno") // EV-002: la última comida se registra en el detalle del planificado
        compose.tap("Registrar la última comida")
        compose.tap("Guardar")
        compose.assertShown("· real jueves 22 de octubre, 18:00", substring = true)
        compose.eventually { assertEquals(LocalDateTime.of(2026, 10, 22, 18, 0), runBlocking { app.container.fasts.get(id)!!.fast.lastMealActual }) }
    }

    @Test
    fun `H006 el reloj, el restante, el fin previsto y los hitos se calculan desde la hora guardada aun tras recrear la pantalla`() {
        seedStartedFast(seedTemplate())
        app.clock.advanceMinutes(33 * 60 + 12)
        compose.activityRule.scenario.recreate()
        compose.assertShown("33 h 12 min")
        compose.assertShown("Faltan 23 h 48 min")
        compose.assertShown("Fin previsto: domingo 25 de octubre, 07:10")
        compose.assertShown("24 horas · viernes 23 de octubre, 22:10 · alcanzado")
        compose.assertShown("Seguridad")
        compose.assertShown("Anotar")
    }

    @Test
    fun `H028 y H030 desde Seguridad se ven las reglas como frases y se termina el ayuno con su motivo`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(glucoseBelow55For20Min())))
        app.clock.advanceMinutes(57 * 60)
        compose.activityRule.scenario.recreate()
        compose.tap("Seguridad")
        compose.assertShown("• Si glucosa por debajo de 55 mg/dL durante 20 minutos: alerta roja y ofrecer terminar el ayuno.")
        compose.tap("Terminar el ayuno")
        compose.tap("Objetivo cumplido")
        compose.onAllNodes(androidx.compose.ui.test.hasText("Terminar el ayuno") and androidx.compose.ui.test.hasClickAction()).onLast().performClick()
        compose.assertShown("Ayuno terminado")
        compose.assertShown("57 h 0 min")
        compose.assertShown("Motivo: Objetivo cumplido")
        compose.eventually { assertEquals(FastStatus.FINISHED, runBlocking { app.container.fasts.get(id)!!.fast }.status) }
        val fast = runBlocking { app.container.fasts.get(id)!!.fast }
        assertEquals(EndReason.GOAL_MET, fast.endReason)
        compose.tap("Volver al inicio")
        compose.assertShown("Último ayuno")
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onLast() = get(fetchSemanticsNodes().size - 1)
