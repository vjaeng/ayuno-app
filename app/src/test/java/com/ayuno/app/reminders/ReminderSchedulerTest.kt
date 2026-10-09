package com.ayuno.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.e2e.app
import com.ayuno.app.e2e.seedStartedFast
import com.ayuno.app.e2e.seedTemplate
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** H-016 (nivel I): la siguiente alarma queda programada a la hora exacta y se reprograma ante eventos del sistema. */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {

    private val alarmManager get() = app.getSystemService(AlarmManager::class.java)

    private fun nextAlarmAt(): LocalDateTime? = shadowOf(alarmManager).peekNextScheduledAlarm()?.let {
        Instant.ofEpochMilli(it.triggerAtTime).atZone(ZoneId.of("-05:00")).toLocalDateTime()
    }

    private fun cancelAlarm() = alarmManager.cancel(
        PendingIntent.getBroadcast(
            app, 1001, Intent(app, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ),
    )

    @Test
    fun `tras empezar el ayuno la siguiente alarma es la proxima medicion del plan`() {
        seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        runBlocking { app.container.reminders.reschedule() }
        assertEquals(LocalDateTime.of(2026, 10, 23, 7, 0), nextAlarmAt())
    }

    @Test
    fun `una medicion ya anotada no se vuelve a recordar`() {
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        runBlocking {
            app.container.recordMeasurement(RecordMeasurement.Input(id, "glucose", at7, at7, 80.0, "mg/dL", source = GlucoseSource.CGM))
            app.container.reminders.reschedule()
        }
        assertEquals(LocalDateTime.of(2026, 10, 23, 12, 0), nextAlarmAt())
    }

    @Test
    fun `una medicion desactivada en el catalogo no genera recordatorios`() {
        seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        runBlocking {
            app.container.setMetricTypeActive("glucose", false)
            app.container.reminders.reschedule()
        }
        assertNull(nextAlarmAt())
    }

    @Test
    fun `reinicio y cambio de hora o de zona vuelven a programar la alarma`() {
        seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED).forEach { action ->
            cancelAlarm()
            assertNull(nextAlarmAt())
            SystemEventsReceiver().onReceive(app, Intent(action))
            waitUntil { nextAlarmAt() != null }
            assertEquals(action, LocalDateTime.of(2026, 10, 23, 7, 0), nextAlarmAt())
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(50) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (condition()) return
            Thread.sleep(20)
        }
    }
}
