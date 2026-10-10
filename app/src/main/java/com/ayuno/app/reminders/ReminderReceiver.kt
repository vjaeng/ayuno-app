package com.ayuno.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ayuno.app.AyunoApplication
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.time.LocalTimes
import kotlinx.coroutines.launch

/** Suena la alarma: notifica lo que vence, evalúa las reglas del ayuno en curso y programa la siguiente. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AyunoApplication
        val pending = goAsync()
        app.appScope.launch {
            try {
                if (intent.action == ACTION_SNOOZED) handleSnoozed(app, intent) else handle(app, intent.getStringExtra(EXTRA_SCHEDULED_AT)?.let { LocalTimes.parse(it) })
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.ayuno.app.REMINDER"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        const val ACTION_SNOOZED = "com.ayuno.app.REMINDER_SNOOZED"

        /** Aviso pospuesto: se repite solo si sigue sin hacerse. */
        suspend fun handleSnoozed(app: AyunoApplication, intent: Intent) {
            val reminder = intent.toReminder() ?: return
            if (app.container.reminders.stillPending(reminder)) ReminderNotifier.notifyReminder(app, reminder, label(app, reminder))
        }

        private suspend fun label(app: AyunoApplication, due: DueReminder): String {
            val c = app.container
            return when (due.item.kind) {
                PendingKind.METRIC -> c.catalog.metricType(due.item.code)?.name ?: due.item.code
                PendingKind.PRODUCT -> c.catalog.product(due.item.code)?.let { p ->
                    "${p.name} · ${Formats.number(due.item.amount ?: p.defaultAmount)} ${p.unit}"
                } ?: due.item.code
                PendingKind.REFEED -> c.refeed.check(due.item.code)?.let { check ->
                    c.fasts.get(due.fastId)?.plan?.refeed?.firstOrNull { it.id == check.refeedStepId }?.description
                } ?: ""
            }
        }

        suspend fun handle(app: AyunoApplication, at: java.time.LocalDateTime?) {
            val c = app.container
            if (at != null) {
                c.reminders.dueAt(at).forEach { due -> ReminderNotifier.notifyReminder(app, due, label(app, due)) }
            }
            c.fasts.all().firstOrNull { it.status == FastStatus.IN_PROGRESS }?.let { fast ->
                val rules = c.fasts.get(fast.id)?.plan?.rules.orEmpty().associateBy { it.id }
                c.evaluateRules(fast.id).forEach { alert ->
                    rules[alert.ruleId]?.let { ReminderNotifier.notifyAlert(app, alert, it.level, it.message) }
                }
            }
            c.reminders.reschedule()
        }
    }
}

/** Reinicio, cambio de hora o de zona, actualización de la app: vuelve a programar (H-016). */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val app = context.applicationContext as AyunoApplication
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.container.reminders.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
