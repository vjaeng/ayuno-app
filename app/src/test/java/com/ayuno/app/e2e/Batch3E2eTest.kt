package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

/** H-030 y H-018 con la vuelta a comer (H-031) de la tanda 3. */
@RunWith(AndroidJUnit4::class)
class Batch3E2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Fin real: domingo 25-oct 07:03. */
    private val end = LocalDateTime.of(2026, 10, 25, 7, 3)

    /** Plantilla con 6 h de vuelta a comer: caldo cada 60 min durante 4 h y suplementos el día 1 (por el caso de uso real). */
    private fun refeedTemplate(): TemplateWithPlan = seedTemplate(metrics = emptyList()).let { t ->
        val plan = t.plan.copy(
            stages = listOf(Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS)),
            refeed = listOf(
                RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Una taza de caldo", 0, 60, 240, null),
                RefeedStep(Ids.newId(), RefeedStepKind.REINTRODUCTION, "Vuelven los suplementos", null, null, null, 1),
            ),
        )
        runBlocking { (app.container.saveTemplate(t.copy(plan = plan)) as Outcome.Ok).value }
    }

    private fun endedFast(): String {
        val id = seedStartedFast(refeedTemplate())
        app.clock.now = Instant.parse("2026-10-25T12:03:00Z")
        runBlocking { app.container.endFast(EndFast.Input(id, end, EndReason.GOAL_MET, null, null)) as Outcome.Ok }
        return id
    }

    @Test
    fun `H030 al terminar, Inicio muestra la vuelta a comer y marcar un paso guarda la hora real`() {
        val id = endedFast()
        app.clock.now = Instant.parse("2026-10-25T12:05:00Z")
        compose.activityRule.scenario.recreate()
        compose.assertShown("domingo 25 de octubre · desde las 07:03")
        compose.assertShown("08:03 Una taza de caldo")
        compose.assertShown("lunes 26 de octubre · Vuelven los suplementos")
        compose.tap("07:03 Una taza de caldo")
        compose.assertShown("07:03 Una taza de caldo (hecho a las 07:05)")
        compose.eventually {
            val first = runBlocking { app.container.refeed.checks(id) }.first()
            assertEquals(end.plusMinutes(2), first.doneAt)
        }
    }

    @Test
    fun `H018 tras terminar, Hoy muestra los pasos de la vuelta a comer y abre la pantalla`() {
        endedFast()
        app.clock.now = Instant.parse("2026-10-25T13:30:00Z") // 08:30
        compose.activityRule.scenario.recreate()
        compose.tap("Hoy")
        compose.assertShown("! 07:03 · hora 56  Una taza de caldo")
        compose.assertShown("! 08:03 · hora 57  Una taza de caldo")
        compose.assertShown("○ 09:03 · hora 58  Una taza de caldo")
        compose.tap("○ 09:03 · hora 58  Una taza de caldo")
        compose.assertShown("Comidas anotadas")
        assertTrue(runBlocking { app.container.reminders.upcoming() }.any { it.item.scheduledAt == end.plusHours(2) })
    }
}
