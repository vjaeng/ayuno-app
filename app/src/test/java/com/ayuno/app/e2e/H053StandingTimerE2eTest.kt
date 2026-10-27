package com.ayuno.app.e2e

import android.app.AlarmManager
import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** H-053: temporizador de pie tras la toma sentado, con los tiempos del plan (60 y 180 s). */
@RunWith(AndroidJUnit4::class)
class H053StandingTimerE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun templateWithStanding(seconds: List<Int>?): TemplateWithPlan {
        val base = seedTemplate()
        val plan = base.plan.copy(
            metrics = base.plan.metrics.map { if (it.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE) it.copy(standingCheckSeconds = seconds) else it },
        )
        return runBlocking { (app.container.saveTemplate(base.copy(plan = plan)) as Outcome.Ok).value }
    }

    private fun recordSitting() {
        compose.tap("Anotar"); compose.tap("Presión arterial")
        compose.type("Sistólica", "118"); compose.type("Diastólica", "76"); compose.type("Pulso (opcional)", "83")
        compose.tap("Guardar")
    }

    private fun recordStanding(systolic: String, diastolic: String, pulse: String) {
        compose.type("Sistólica", systolic); compose.type("Diastólica", diastolic); compose.type("Pulso (opcional)", pulse)
        compose.tap("Guardar")
    }

    /** Pasa el tiempo en el reloj de la app y en el hilo principal, para que el temporizador dé su vuelta. */
    private fun passSeconds(seconds: Long) {
        app.clock.now = app.clock.now.plusSeconds(seconds)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    }

    private fun standingAlarms() = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        .count { it.operation?.let { op -> shadowOf(op).savedIntent.action } == "com.ayuno.app.STANDING" }

    @Test
    fun `H053 lecturas a 1 y 3 minutos con sus segundos calculados, enlazadas y con el resultado de la pareja`() {
        val id = seedStartedFast(templateWithStanding(listOf(60, 180)))
        compose.activityRule.scenario.recreate()
        recordSitting()
        compose.assertShown("Ponte de pie ahora. Te avisaré a 1 min y 3 min.")
        compose.tap("Ya estoy de pie")
        compose.assertShown("De pie desde las 22:10:00")
        assertEquals(2, standingAlarms())

        passSeconds(60)
        compose.assertShown("1 min de pie: toma la presión")
        compose.assertShown("de pie 60 s")
        recordStanding("114", "74", "114")
        compose.assertShown("✔ a 1 min")

        passSeconds(120)
        compose.assertShown("3 min de pie: toma la presión")
        recordStanding("112", "76", "126")
        compose.assertShown("Al ponerte de pie")
        compose.assertShown("a 1 min: -4 arriba · -2 abajo · +31 pulso")
        compose.assertShown("a 3 min: -6 arriba · 0 abajo · +43 pulso")

        val readings = runBlocking { app.container.records.bloodPressure(id) }.sortedBy { it.takenAt }
        assertEquals(listOf(Position.SITTING, Position.STANDING, Position.STANDING), readings.map { it.position })
        assertEquals(listOf(null, 60, 180), readings.map { it.secondsStanding })
        assertTrue(readings.drop(1).all { it.pairedReadingId == readings[0].id })
    }

    @Test
    fun `H053 cancelar retira las alarmas y deja las lecturas ya guardadas`() {
        val id = seedStartedFast(templateWithStanding(listOf(60, 180)))
        compose.activityRule.scenario.recreate()
        recordSitting()
        compose.tap("Ya estoy de pie")
        passSeconds(60)
        compose.assertShown("1 min de pie: toma la presión")
        recordStanding("114", "74", "114")
        compose.tap("Cancelar")
        compose.assertShown("a 1 min: -4 arriba · -2 abajo · +31 pulso")
        assertEquals(0, standingAlarms())
        assertEquals(2, runBlocking { app.container.records.bloodPressure(id) }.size)
    }

    @Test
    fun `H053 anotar sin esperar guarda la lectura sin segundos y el plan sin tiempos solo ofrece eso`() {
        val id = seedStartedFast(templateWithStanding(null))
        compose.activityRule.scenario.recreate()
        recordSitting()
        compose.assertShown("Tu plan no tiene tiempos para medir de pie.")
        compose.tap("Anotar sin esperar")
        compose.assertShown("tomada sin esperar")
        recordStanding("104", "70", "90")
        compose.assertShown("sin esperar: -14 arriba · -6 abajo · +7 pulso")
        val standing = runBlocking { app.container.records.bloodPressure(id) }.single { it.position == Position.STANDING }
        assertEquals(null, standing.secondsStanding)
    }
}
