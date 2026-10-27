package com.ayuno.app.e2e

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.app.reminders.NotificationActionReceiver
import com.ayuno.app.reminders.ReminderReceiver
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.Settings
import com.ayuno.domain.usecase.Outcome
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

/** H-052: la pregunta «¿Has tomado sal desde las …?» tras N horas despierto sin anotar ese tipo. */
@RunWith(AndroidJUnit4::class)
class H052IntakeNudgeE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val notifications get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    private fun nextAlarmAt(): LocalDateTime? = shadowOf(app.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()?.let {
        Instant.ofEpochMilli(it.triggerAtTime).atZone(ZoneId.of("-05:00")).toLocalDateTime()
    }

    /** Sal con meta y pregunta tras 6 h; silencio de 23:00 a 06:30. Ayuno empezado el jueves a las 22:10. */
    private fun seed(): String {
        runBlocking { app.container.settings.save(Settings(quietStart = LocalTime.of(23, 0), quietEnd = LocalTime.of(6, 30))) }
        val base = seedTemplate(metrics = emptyList())
        val template = runBlocking {
            (app.container.saveTemplate(base.copy(plan = base.plan.copy(goals = listOf(IntakeGoal(Ids.newId(), "salt", 2.5, "g", nudgeAfterHours = 6.0))))) as Outcome.Ok).value
        }
        return seedStartedFast(template)
    }

    @Test
    fun `H052 sin ingestas pregunta tras 6 h despierto, Todavia no la cierra y Hoy la muestra hasta anotar`() {
        val id = seed()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { app.container.reminders.reschedule() }
        // 50 min antes del silencio y 5 h 10 min tras él.
        val askAt = LocalDateTime.of(2026, 10, 23, 11, 40)
        assertEquals(askAt, nextAlarmAt())

        app.clock.now = Instant.parse("2026-10-23T16:40:00Z")
        runBlocking { ReminderReceiver.handle(app, askAt) }
        val notification = notifications.single()
        assertEquals("¿Has tomado sal desde las 22:10?", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(listOf("Sí, anotar", "Todavía no"), notification.actions.map { it.title.toString() })

        // Una vez por hueco: la siguiente alarma ya no es esta pregunta.
        assertTrue(nextAlarmAt() == null || nextAlarmAt()!!.isAfter(askAt))

        val notYet = shadowOf(notification.actions[1].actionIntent).savedIntent
        NotificationActionReceiver().onReceive(app, notYet)
        assertTrue(notifications.isEmpty())

        app.clock.now = Instant.parse("2026-10-23T16:45:00Z")
        compose.activityRule.scenario.recreate()
        compose.tap("Hoy")
        compose.tap("? 11:40  ¿Has tomado sal desde las 22:10?")
        compose.type("Cantidad", "1")
        compose.tap("Guardar")
        compose.eventually { assertEquals(1, runBlocking { app.container.records.intakes(id) }.size) }
        // La ingesta mueve el ancla: la próxima pregunta, 6 h despierto después de las 11:45.
        compose.eventually { assertEquals(LocalDateTime.of(2026, 10, 23, 17, 45), nextAlarmAt()) }
        compose.tap("Hoy")
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithTextCount("¿Has tomado sal", substring = true) == 0)
    }
}

private fun Rule4.onAllNodesWithTextCount(text: String, substring: Boolean) =
    onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().size
