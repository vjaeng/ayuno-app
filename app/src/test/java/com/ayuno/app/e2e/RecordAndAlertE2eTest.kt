package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.UserChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H-008, H-009, H-019, H-020, H-027 desde la interfaz. */
@RunWith(AndroidJUnit4::class)
class RecordAndAlertE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `H008 anotar una glucosa cuesta 3 toques desde Inicio y guarda origen y hora`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        val taps = TapCounter()
        compose.tap("Anotar", taps)
        compose.tap("Glucosa", taps)
        compose.type("Valor", "72")
        compose.tap("Guardar", taps)
        assertTrue("Toques: ${taps.taps}", taps.taps <= 3)
        compose.eventually { assertEquals(1, runBlocking { app.container.records.measurements(id) }.size) }
        val m = runBlocking { app.container.records.measurements(id) }.single()
        assertEquals(72.0, m.valueNum!!, 0.0)
        assertEquals(GlucoseSource.CGM, m.source)
        assertEquals(java.time.LocalDateTime.of(2026, 10, 22, 22, 10), m.takenAt)
        compose.assertShown("Glucosa: 72 mg/dL", substring = true)
    }

    @Test
    fun `H008 un ayuno desde cero tambien muestra en Anotar las mediciones activas del catalogo`() {
        val id = runBlocking {
            val lastMeal = java.time.LocalDateTime.of(2026, 10, 22, 18, 0)
            val created = app.container.createFast(com.ayuno.domain.usecase.CreateFast.Input(null, lastMeal, lastMeal.plusHours(4), 57.0, null))
            (created as com.ayuno.domain.usecase.Outcome.Ok).value.fast.id
        }
        app.clock.now = java.time.Instant.parse("2026-10-23T03:10:00Z")
        runBlocking { app.container.startFast(id) }
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar")
        listOf("Glucosa", "Presión arterial", "Temperatura").forEach { compose.assertShown(it) }
        compose.tap("Peso")
        compose.type("Valor", "71.4")
        compose.tap("Guardar")
        compose.eventually { assertEquals("weight", runBlocking { app.container.records.measurements(id) }.single().metricTypeCode) }
    }

    @Test
    fun `H008 un valor fuera de rango pide confirmacion y se guarda tal cual`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Glucosa")
        compose.type("Valor", "640")
        compose.tap("Guardar")
        compose.assertShown("¿Seguro?")
        compose.eventually { assertTrue(runBlocking { app.container.records.measurements(id) }.isEmpty()) }
        compose.tap("Guardar tal cual")
        compose.eventually { assertEquals(640.0, runBlocking { app.container.records.measurements(id) }.single().valueEntered!!, 0.0) }
    }

    @Test
    fun `H009 presion sentado y luego de pie, vinculadas, con la diferencia`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Presión arterial")
        compose.type("Sistólica", "118"); compose.type("Diastólica", "76"); compose.type("Pulso (opcional)", "64")
        compose.tap("Guardar")
        compose.tap("Anotar de pie")
        compose.type("Sistólica", "104"); compose.type("Diastólica", "70"); compose.type("Pulso (opcional)", "76")
        compose.tap("Guardar")
        compose.assertShown("Sistólica -14 · diastólica -6 · pulso +12")
        compose.eventually { assertEquals(2, runBlocking { app.container.records.bloodPressure(id) }.size) }
        val readings = runBlocking { app.container.records.bloodPressure(id) }
        assertEquals(listOf(Position.SITTING, Position.STANDING), readings.map { it.position })
        assertEquals(readings[0].id, readings[1].pairedReadingId)
    }

    @Test
    fun `H020 H027 dos glucosas bajas separadas 20 minutos disparan la alerta roja y seguir exige motivo`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(glucoseBelow55For20Min())))
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Glucosa"); compose.type("Valor", "52"); compose.tap("Guardar")
        compose.eventually { assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty()) }
        app.clock.advanceMinutes(20)
        compose.tap("Anotar"); compose.tap("Glucosa"); compose.type("Valor", "53"); compose.tap("Guardar")

        compose.assertShown("Rompe con caldo tibio con sal.")
        compose.assertShown("Alerta ·", substring = true)
        compose.tap("Sigo")
        compose.assertShown("Escribe el motivo.")
        compose.type("Si sigues, escribe por qué", "Me siento bien, vuelvo a medir")
        compose.tap("Sigo")
        compose.assertShown("Anotar")
        compose.eventually { assertEquals(UserChoice.CONTINUE, runBlocking { app.container.alerts.alerts(id) }.single().userChoice) }
        val alert = runBlocking { app.container.alerts.alerts(id) }.single()
        assertEquals(UserChoice.CONTINUE, alert.userChoice)
        assertEquals(2, alert.triggerEntryIds.size)
        compose.eventually { assertEquals(FastStatus.IN_PROGRESS, runBlocking { app.container.fasts.get(id)!!.fast.status }) }
    }

    @Test
    fun `H027 terminar desde la alerta lleva al fin con la regla prellenada y no termina por su cuenta`() {
        val id = seedStartedFast(seedTemplate(rules = listOf(glucoseBelow55For20Min().copy(sustainedMinutes = null))))
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Glucosa"); compose.type("Valor", "50"); compose.tap("Guardar")
        compose.tap("Termino el ayuno")
        compose.assertShown("¿Por qué terminas?")
        compose.eventually { assertEquals(FastStatus.IN_PROGRESS, runBlocking { app.container.fasts.get(id)!!.fast.status }) }
        compose.eventually { assertEquals(UserChoice.END_FAST, runBlocking { app.container.alerts.alerts(id) }.single().userChoice) }
    }
}
