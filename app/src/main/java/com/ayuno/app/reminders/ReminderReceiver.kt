package com.ayuno.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ayuno.app.AyunoApplication
import com.ayuno.domain.time.LocalTimes
import kotlinx.coroutines.launch

/** Suena la alarma: notifica lo que vence y programa la siguiente. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as AyunoApplication
        val pending = goAsync()
        app.appScope.launch {
            try {
                intent.getStringExtra(EXTRA_SCHEDULED_AT)?.let { LocalTimes.parse(it) }?.let { at ->
                    val names = app.container.catalog.metricTypes().associate { it.code to it.name }
                    app.container.reminders.dueAt(at).forEach { due ->
                        ReminderNotifier.notify(context, due, names[due.item.metricTypeCode] ?: due.item.metricTypeCode)
                    }
                }
                app.container.reminders.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.ayuno.app.REMINDER"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
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
