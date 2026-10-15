package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordResult
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule as JRule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime

/** EV-004 · H-048, H-034 (mediciones y presión), H-006 (acceso desde Inicio), H-018 (tocar un hecho). */
@RunWith(AndroidJUnit4::class)
class H048MeasurementsE2eTest {

    @get:JRule
    val compose = createAndroidComposeRule<MainActivity>()

    private val lowGlucose = Rule(
        Ids.newId(), "Glucosa baja", true, RuleConditionKind.METRIC_BELOW, "glucose", null, 55.0, null, null, null, null,
        emptyList(), false, null, RuleLevel.ALERT, "Rompe con caldo tibio con sal.", RuleAction.OFFER_END, null,
    )

    /** Ayuno en curso con una glucosa anotada por el caso de uso real; reloj el viernes a las 12:00. */
    private fun fastWithGlucose(value: Double, rules: List<Rule> = emptyList(), scheduledFor: LocalDateTime? = null): Pair<String, String> {
        val fastId = seedStartedFast(seedTemplate(rules))
        val at = scheduledFor ?: LocalDateTime.of(2026, 10, 23, 7, 2)
        val result = runBlocking {
            app.container.recordMeasurement(
                RecordMeasurement.Input(fastId, "glucose", at, scheduledFor = scheduledFor, valueEntered = value, unitEntered = "mg/dL", source = GlucoseSource.CGM),
            )
        }
        app.clock.now = Instant.parse("2026-10-23T17:00:00Z")
        compose.activityRule.scenario.recreate()
        return fastId to (((result as Outcome.Ok).value as RecordResult.Saved).entry.id)
    }

    @Test
    fun `H048 desde Inicio veo la lista y corrijo una glucosa con rastro`() {
        val (fastId, id) = fastWithGlucose(46.0)
        compose.assertShown("1 anotada")
        compose.tap("Mediciones de este ayuno")
        compose.assertShown("viernes", substring = true)
        compose.tap("Glucosa 46 mg/dL")
        compose.assertShown("Si el valor nuevo cumple una de tus reglas, verás su aviso.")
        compose.type("Valor", "64")
        compose.type("Motivo del cambio (opcional)", "error de dedo")
        compose.tap("Guardar")
        compose.assertShown("Glucosa 64 mg/dL")
        compose.assertShown("corregida", substring = true)
        val m = runBlocking { app.container.records.measurements(fastId) }.single()
        assertEquals(id, m.id)
        assertEquals(64.0, m.valueNum!!, 0.0)
        val log = runBlocking { app.container.audit.entriesFor(id) }
        assertTrue(log.all { it.action == EditAction.EDIT && it.reason == "error de dedo" })
        assertTrue(log.any { it.field == "value_entered" && it.oldValue == "46" && it.newValue == "64" })
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.assertShown("Glucosa: 64 mg/dL", substring = true)
    }

    @Test
    fun `H034 una correccion que cumple una regla muestra su alerta`() {
        fastWithGlucose(72.0, rules = listOf(lowGlucose))
        compose.tap("Mediciones de este ayuno")
        compose.tap("Glucosa 72 mg/dL")
        compose.type("Valor", "46")
        compose.tap("Guardar")
        compose.assertShown("Rompe con caldo tibio con sal.")
    }

    @Test
    fun `H034 borrar con confirmacion la quita de la lista y se restaura desde Borradas`() {
        val (fastId, id) = fastWithGlucose(72.0)
        compose.tap("Mediciones de este ayuno")
        compose.tap("Glucosa 72 mg/dL")
        compose.waitForText("Si el valor nuevo", substring = true)
        compose.tapDescription("Más acciones de esta medición")
        compose.tap("Borrar")
        compose.assertShown("¿Borrar esta medición?")
        compose.tap("Borrar")
        compose.assertShown("Borradas (1)")
        compose.assertShown("Aún no has anotado ninguna medición en este ayuno.")
        assertTrue(runBlocking { app.container.records.measurements(fastId) }.isEmpty())
        compose.tap("Borradas (1)")
        compose.tap("Restaurar")
        compose.assertShown("Glucosa 72 mg/dL")
        compose.eventually {
            assertEquals(listOf(EditAction.DELETE, EditAction.RESTORE), runBlocking { app.container.audit.entriesFor(id) }.map { it.action })
        }
    }

    @Test
    fun `H018 tocar un hecho en Hoy abre la medicion para corregirla`() {
        fastWithGlucose(72.0, scheduledFor = LocalDateTime.of(2026, 10, 23, 7, 0))
        compose.tap("Hoy")
        compose.waitForText("Glucosa", substring = true)
        compose.tapContaining("72 mg/dL")
        compose.assertShown("Si el valor nuevo cumple una de tus reglas, verás su aviso.")
    }

    @Test
    fun `H048 sin mediciones la lista lo dice y ofrece Anotar`() {
        seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.assertShown("Ninguna anotada todavía")
        compose.tap("Mediciones de este ayuno")
        compose.assertShown("Aún no has anotado ninguna medición en este ayuno.")
        compose.tap("Anotar")
        compose.assertShown("Síntoma", substring = true)
    }

    @Test
    fun `H048 en un ayuno terminado se corrige desde su detalle en Historial, sin aviso de reglas`() {
        val (fastId, _) = fastWithGlucose(72.0)
        runBlocking {
            app.container.endFast(com.ayuno.domain.usecase.EndFast.Input(fastId, LocalDateTime.of(2026, 10, 23, 11, 0), com.ayuno.domain.model.EndReason.PERSONAL_DECISION, null, null))
        }
        compose.activityRule.scenario.recreate()
        compose.tap("Historial")
        compose.tapContaining("57 h")
        compose.tapContaining("Glucosa 72 mg/dL")
        compose.type("Valor", "70")
        compose.tap("Guardar")
        compose.eventually { assertEquals(70.0, runBlocking { app.container.records.measurements(fastId) }.single().valueNum!!, 0.0) }
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText("Si el valor nuevo", substring = true)).fetchSemanticsNodes().isEmpty())
    }
}
