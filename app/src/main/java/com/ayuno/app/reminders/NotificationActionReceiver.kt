package com.ayuno.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ayuno.app.AyunoApplication
import com.ayuno.domain.derived.PendingItem
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.time.LocalTimes

/** H-017: posponer desde la notificación. Cierra el aviso y vuelve a avisar dentro de 10 o 30 minutos. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return
        val reminder = intent.toReminder() ?: return
        val minutes = intent.getLongExtra(ReminderNotifier.EXTRA_SNOOZE_MINUTES, 10)
        ReminderNotifier.cancel(context, ReminderNotifier.idOf(reminder))
        (context.applicationContext as AyunoApplication).container.reminders.snooze(reminder, minutes)
    }

    companion object {
        const val ACTION_SNOOZE = "com.ayuno.app.SNOOZE"
    }
}

/** Reconstruye el pendiente de los datos de una notificación o de su alarma. */
fun Intent.toReminder(): DueReminder? {
    val fastId = getStringExtra(ReminderNotifier.EXTRA_FAST_ID) ?: return null
    val code = getStringExtra(ReminderNotifier.EXTRA_CODE) ?: return null
    val at = getStringExtra(ReminderNotifier.EXTRA_SCHEDULED_FOR)?.let { runCatching { LocalTimes.parse(it) }.getOrNull() } ?: return null
    val kind = runCatching { PendingKind.valueOf(getStringExtra(ReminderNotifier.EXTRA_KIND).orEmpty()) }.getOrDefault(PendingKind.METRIC)
    return DueReminder(fastId, PendingItem(kind, code, at))
}
