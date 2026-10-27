package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.RuleLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.ayuno.domain.model.Rule as PlanRule

/** H-055: «el pulso sentado queda más de 15 por encima de mi valor de partida», y marcar la de partida. */
@RunWith(AndroidJUnit4::class)
class H055BaselineRuleE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val pulseUp15 = PlanRule(
        Ids.newId(), "Pulso sobre la partida", true, RuleConditionKind.CHANGE_FROM_BASELINE, "blood_pressure", BpComponent.PULSE, 15.0, null, null, null, null,
        emptyList(), false, Position.SITTING, RuleLevel.NOTICE, "Pulso alto respecto al inicio.", RuleAction.NONE, null, direction = RuleDirection.ABOVE,
    )

    private fun sitting(pulse: String) {
        compose.tap("Anotar"); compose.tap("Presión arterial")
        compose.type("Sistólica", "118"); compose.type("Diastólica", "76"); compose.type("Pulso (opcional)", pulse)
        compose.tap("Guardar")
    }

    @Test
    fun `H055 sin marca la primera es la de partida y 86 frente a 70 dispara con la partida en Por`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(pulseUp15)))
        compose.activityRule.scenario.recreate()
        sitting("70"); compose.tap("Ahora no")
        app.clock.advanceMinutes(30)
        sitting("84"); compose.tap("Ahora no")
        assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty())
        app.clock.advanceMinutes(30)
        sitting("86")
        compose.assertShown("Pulso alto respecto al inicio.")
        compose.assertShown("86; partida 70: +16", substring = true)
    }

    @Test
    fun `H055 marcar otra lectura como de partida cambia la referencia y queda en la bitacora`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(pulseUp15)))
        compose.activityRule.scenario.recreate()
        sitting("70"); compose.tap("Ahora no")
        app.clock.advanceMinutes(30)
        sitting("80"); compose.tap("Ahora no")

        compose.tap("Historial")
        compose.tapContaining("En curso")
        compose.tapContaining("pulso 80")
        compose.tapDescription("Más acciones de esta medición")
        compose.tap("Marcar como de partida")
        compose.assertShown("Presión arterial 118/76 marcada como de partida")
        compose.assertShown("de partida")
        val readings = runBlocking { app.container.records.bloodPressure(id) }.sortedBy { it.takenAt }
        assertEquals(listOf(false, true), readings.map { it.isBaseline })
        assertTrue(runBlocking { app.container.audit.entriesFor(readings[1].id) }.any { it.field == "is_baseline" })
    }
}
