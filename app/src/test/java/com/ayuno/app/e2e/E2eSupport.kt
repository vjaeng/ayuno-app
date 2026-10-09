package com.ayuno.app.e2e

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.ayuno.app.MainActivity
import com.ayuno.app.TestAyunoApplication
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.LocalTime

typealias Rule4 = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

val app: TestAyunoApplication get() = ApplicationProvider.getApplicationContext()

/** Cuenta los toques de la persona en una prueba (H-008: 3 toques o menos). */
class TapCounter { var taps = 0 }

/** Espera a que aparezca un texto: Room entrega sus resultados desde sus propios hilos. */
fun Rule4.waitForText(text: String, substring: Boolean = false) {
    try {
        waitUntil(5_000) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    } catch (e: Throwable) {
        throw AssertionError("No apareció el texto «$text»", e)
    }
}

/** Reintenta una comprobación sobre la base hasta 5 s (las escrituras de los ViewModels son asíncronas). */
fun Rule4.eventually(assertion: () -> Unit) {
    val deadline = System.currentTimeMillis() + 5_000
    while (true) {
        waitForIdle()
        try { assertion(); return } catch (e: Throwable) {
            if (System.currentTimeMillis() > deadline) throw e
            Thread.sleep(20)
        }
    }
}

fun Rule4.tap(text: String, counter: TapCounter? = null) {
    waitForText(text)
    node(text).performClick()
    counter?.let { it.taps++ }
    waitForIdle()
}

/** Busca un texto exacto; si está en una columna desplazable, la desplaza hasta él. */
fun Rule4.node(text: String): SemanticsNodeInteraction {
    runCatching { onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    return onAllNodesWithText(text).let { all ->
        runCatching { all[0].performScrollTo() }
        all[0]
    }
}

fun Rule4.type(label: String, value: String) {
    waitForText(label)
    node(label).performTextReplacement(value)
    waitForIdle()
}

fun Rule4.assertShown(text: String, substring: Boolean = false) {
    waitForText(text, substring)
    onAllNodesWithText(text, substring = substring)[0].assertExists()
}

fun glucoseBelow55For20Min() = Rule(
    Ids.newId(), "Glucosa baja", true, RuleConditionKind.METRIC_BELOW, "glucose", null, 55.0, 20, null, null, null,
    emptyList(), false, null, RuleLevel.ALERT, "Rompe con caldo tibio con sal.", RuleAction.OFFER_END, null,
)

/** Plantilla creada con el caso de uso real (anti-fixture-fantasma). */
fun seedTemplate(rules: List<Rule> = emptyList(), metrics: List<String> = listOf("glucose", "blood_pressure")): TemplateWithPlan = runBlocking {
    val template = TemplateWithPlan(
        Template(Ids.newId(), "Ayuno de 57 horas", 57.0, listOf(12.0, 24.0, 36.0, 48.0)),
        Plan(
            stages = emptyList(),
            metrics = metrics.map { code ->
                PlanMetric(Ids.newId(), code, listOf(7, 12, 18, 22).map { MetricSchedule(Ids.newId(), StageKind.FAST, LocalTime.of(it, 0)) })
            },
            rules = rules,
        ),
    )
    (app.container.saveTemplate(template) as Outcome.Ok).value
}

/** Ayuno planificado con la plantilla: última comida jueves 18:00, reloj 22:00. */
fun seedPlannedFast(template: TemplateWithPlan): String = runBlocking {
    val result = app.container.createFast(
        CreateFast.Input(template.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null),
    )
    (result as Outcome.Ok).value.fast.id
}

/** Ayuno empezado a las 22:10 del jueves. */
fun seedStartedFast(template: TemplateWithPlan): String {
    val id = seedPlannedFast(template)
    app.clock.now = java.time.Instant.parse("2026-10-23T03:10:00Z")
    runBlocking { app.container.startFast(id) }
    return id
}
