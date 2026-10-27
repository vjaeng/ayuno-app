package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.Rule as PlanRule
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

/** H-054: «el pulso sube más de 30 al ponerme de pie», evaluada al guardar la lectura de pie enlazada. */
@RunWith(AndroidJUnit4::class)
class H054PairDifferenceRuleE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val pulseUp30 = PlanRule(
        Ids.newId(), "Pulso al levantarme", true, RuleConditionKind.PAIR_DIFFERENCE, "blood_pressure", BpComponent.PULSE, 30.0, null, null, null, null,
        emptyList(), false, null, RuleLevel.NOTICE, "Siéntate y vuelve a medir.", RuleAction.NONE, null, direction = RuleDirection.ABOVE,
    )

    private fun pair(sittingPulse: String, standingPulse: String) {
        compose.tap("Anotar"); compose.tap("Presión arterial")
        compose.type("Sistólica", "118"); compose.type("Diastólica", "76"); compose.type("Pulso (opcional)", sittingPulse)
        compose.tap("Guardar")
        compose.tap("Anotar sin esperar")
        compose.type("Sistólica", "114"); compose.type("Diastólica", "74"); compose.type("Pulso (opcional)", standingPulse)
        compose.tap("Guardar")
    }

    @Test
    fun `H054 la pareja 83 a 126 dispara el aviso con la pareja y su diferencia en Por`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(pulseUp30)))
        compose.activityRule.scenario.recreate()
        pair("83", "126")
        compose.assertShown("Siéntate y vuelve a medir.")
        compose.assertShown("sentado 83 → de pie 126 sin esperar: +43", substring = true)
        val alert = runBlocking { app.container.alerts.alerts(id) }.single()
        val readings = runBlocking { app.container.records.bloodPressure(id) }.sortedBy { it.takenAt }
        assertEquals(readings.map { it.id }, alert.triggerEntryIds)
    }

    @Test
    fun `H054 la pareja 60 a 80 no dispara y el resultado de la pareja se muestra`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(pulseUp30)))
        compose.activityRule.scenario.recreate()
        pair("60", "80")
        compose.assertShown("sin esperar: -4 arriba · -2 abajo · +20 pulso")
        assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty())
    }
}
