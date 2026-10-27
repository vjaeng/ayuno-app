package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.TrendOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.ayuno.domain.model.Rule as PlanRule

/** H-056: «el cambio del pulso al ponerme de pie sube en 3 parejas seguidas». */
@RunWith(AndroidJUnit4::class)
class H056TrendRuleE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val pairTrend = PlanRule(
        Ids.newId(), "Pulso de pie en aumento", true, RuleConditionKind.TREND, "blood_pressure", BpComponent.PULSE, null, null, null, null, null,
        emptyList(), false, null, RuleLevel.NOTICE, "El salto del pulso va a más.", RuleAction.NONE, null,
        direction = RuleDirection.ABOVE, trendCount = 3, trendOf = TrendOf.PAIR_DIFFERENCE,
    )

    private fun pair(sitting: String, standing: String) {
        compose.tap("Anotar"); compose.tap("Presión arterial")
        compose.type("Sistólica", "118"); compose.type("Diastólica", "76"); compose.type("Pulso (opcional)", sitting)
        compose.tap("Guardar")
        compose.tap("Anotar sin esperar")
        compose.type("Sistólica", "114"); compose.type("Diastólica", "74"); compose.type("Pulso (opcional)", standing)
        compose.tap("Guardar")
    }

    @Test
    fun `H056 diferencias 20, 25 y 43 disparan en la tercera con la racha en Por`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(pairTrend)))
        compose.activityRule.scenario.recreate()
        pair("80", "100"); compose.tap("Hecho")
        app.clock.advanceMinutes(30)
        pair("80", "105"); compose.tap("Hecho")
        assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty())
        app.clock.advanceMinutes(30)
        pair("83", "126")
        compose.assertShown("El salto del pulso va a más.")
        compose.assertShown("+20, +25, +43", substring = true)
        assertEquals(6, runBlocking { app.container.alerts.alerts(id) }.single().triggerEntryIds.size)
    }
}
