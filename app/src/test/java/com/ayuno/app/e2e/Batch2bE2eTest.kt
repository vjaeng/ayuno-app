package com.ayuno.app.e2e

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.app.reminders.DueReminder
import com.ayuno.app.reminders.NotificationActionReceiver
import com.ayuno.app.reminders.ReminderNotifier
import com.ayuno.domain.derived.PendingItem
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** H-008 (al despertar), H-017, H-018, H-026. */
@RunWith(AndroidJUnit4::class)
class Batch2bE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun nextAlarmAt(): LocalDateTime? = shadowOf(app.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()?.let {
        Instant.ofEpochMilli(it.triggerAtTime).atZone(ZoneId.of("-05:00")).toLocalDateTime()
    }

    @Test
    fun `H018 Hoy muestra lo hecho, lo atrasado y lo pendiente con la hora del ayuno y abre el formulario`() {
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        runBlocking { app.container.recordMeasurement(RecordMeasurement.Input(id, "glucose", at7.plusMinutes(2), at7, 72.0, "mg/dL", source = GlucoseSource.CGM)) }
        app.clock.now = Instant.parse("2026-10-23T17:30:00Z") // viernes 12:30
        compose.activityRule.scenario.recreate()
        compose.tap("Hoy")
        compose.assertShown("viernes 23 de octubre")
        compose.assertShown("✔ 07:00 · hora 8  Glucosa")
        compose.assertShown("Hecho · 72 mg/dL · 07:02")
        compose.assertShown("! 12:00 · hora 13  Glucosa")
        compose.assertShown("○ 18:00 · hora 19  Glucosa")
        compose.tap("! 12:00 · hora 13  Glucosa")
        compose.assertShown("viernes 23 de octubre, 12:00:00", substring = true)
    }

    @Test
    fun `H017 H018 no se hizo desde Hoy pide el motivo y queda registrado`() {
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        app.clock.now = Instant.parse("2026-10-23T17:30:00Z")
        compose.activityRule.scenario.recreate()
        compose.tap("Hoy")
        compose.waitForText("No se hizo")
        compose.onAllNodes(hasText("No se hizo") and hasClickAction())[0].performClick()
        compose.type("Motivo", "Sin sensor")
        compose.onNode(hasText("Guardar") and hasAnyAncestor(isDialog())).performClick()
        compose.eventually { assertEquals("Sin sensor", runBlocking { app.container.records.measurements(id) }.single().notDoneReason) }
    }

    @Test
    fun `H017 el silencio nocturno se configura y los recordatorios del plan no suenan en esa franja`() {
        seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        compose.tap("Más"); compose.tap("Ajustes"); compose.tap("Silencio nocturno")
        compose.eventually { assertEquals(LocalTime.of(23, 0), runBlocking { app.container.settings.get() }.quietStart) }
        runBlocking {
            app.container.saveQuietHours(LocalTime.of(6, 0), LocalTime.of(8, 0))
            app.container.reminders.reschedule()
        }
        assertEquals(LocalDateTime.of(2026, 10, 23, 12, 0), nextAlarmAt())
    }

    @Test
    fun `H017 la notificacion trae anotar, posponer y no se hizo y posponer programa otro aviso`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        val reminder = DueReminder(id, PendingItem(PendingKind.METRIC, "glucose", LocalDateTime.of(2026, 10, 23, 7, 0)))
        ReminderNotifier.notifyReminder(app, reminder, "Glucosa")
        val notification = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals(listOf("Anotar", "+10 min", "+30 min", "No se hizo"), notification.actions.map { it.title.toString() })

        app.clock.now = Instant.parse("2026-10-23T12:01:00Z") // 07:01
        val snooze = Intent(app, NotificationActionReceiver::class.java).setAction(NotificationActionReceiver.ACTION_SNOOZE)
            .putExtra(ReminderNotifier.EXTRA_FAST_ID, id).putExtra(ReminderNotifier.EXTRA_KIND, "METRIC")
            .putExtra(ReminderNotifier.EXTRA_CODE, "glucose").putExtra(ReminderNotifier.EXTRA_SCHEDULED_FOR, "2026-10-23T07:00:00")
            .putExtra(ReminderNotifier.EXTRA_SNOOZE_MINUTES, 10L)
        NotificationActionReceiver().onReceive(app, snooze)
        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertTrue(alarms.any { Instant.ofEpochMilli(it.triggerAtTime).atZone(ZoneId.of("-05:00")).toLocalDateTime() == LocalDateTime.of(2026, 10, 23, 7, 11) })
    }

    @Test
    fun `H026 una regla con medir de nuevo programa el recordatorio aunque sea silencio y aparece en Hoy`() {
        val rule = glucoseBelow55For20Min().copy(sustainedMinutes = null, action = RuleAction.SCHEDULE_MEASUREMENT, actionMinutes = 15, message = "Mira el sensor en 15 minutos.")
        val id = seedStartedFast(seedTemplate(rules = listOf(rule), metrics = listOf("glucose")))
        app.clock.now = Instant.parse("2026-10-23T08:00:00Z") // viernes 03:00, de madrugada
        runBlocking {
            app.container.saveQuietHours(LocalTime.of(23, 0), LocalTime.of(6, 30))
            app.container.recordMeasurement(RecordMeasurement.Input(id, "glucose", LocalDateTime.of(2026, 10, 23, 3, 0), valueEntered = 50.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
            val alert = app.container.alerts.alerts(id).single()
            assertTrue(app.container.decideAlert(alert.id, UserChoice.CONTINUE, "Vuelvo a medir") is Outcome.Ok)
            app.container.reminders.reschedule()
        }
        assertEquals(LocalDateTime.of(2026, 10, 23, 3, 15), nextAlarmAt())
        compose.activityRule.scenario.recreate()
        compose.tap("Hoy")
        compose.assertShown("○ 03:15 · hora 5  Glucosa (por una regla)")
    }

    @Test
    fun `H008 la primera lectura tras el silencio se propone al despertar`() {
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        runBlocking { app.container.saveQuietHours(LocalTime.of(23, 0), LocalTime.of(6, 30)) }
        app.clock.now = Instant.parse("2026-10-23T11:50:00Z") // viernes 06:50
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Glucosa"); compose.type("Valor", "80"); compose.tap("Guardar")
        compose.eventually { assertTrue(runBlocking { app.container.records.measurements(id) }.single().onWaking) }
    }
}
