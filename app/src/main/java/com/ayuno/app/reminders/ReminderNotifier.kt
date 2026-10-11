package com.ayuno.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ayuno.app.MainActivity
import com.ayuno.app.R
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.time.LocalTimes

/** Notificaciones locales (H-016, H-027). En la pantalla de bloqueo solo se ve un texto genérico (ADR-SEG-002). */
object ReminderNotifier {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_ALERTS = "alerts"
    const val EXTRA_FAST_ID = "fast_id"
    const val EXTRA_KIND = "kind"
    const val EXTRA_CODE = "code"
    const val EXTRA_SCHEDULED_FOR = "scheduled_for"
    const val EXTRA_ALERT_ID = "alert_id"
    const val EXTRA_NOT_DONE = "not_done"
    const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"

    fun idOf(reminder: DueReminder): Int =
        "${reminder.fastId}|${reminder.item.kind}|${reminder.item.code}|${LocalTimes.format(reminder.item.scheduledAt)}".hashCode()

    private fun itemExtras(intent: Intent, reminder: DueReminder): Intent = intent
        .putExtra(EXTRA_FAST_ID, reminder.fastId)
        .putExtra(EXTRA_KIND, reminder.item.kind.name)
        .putExtra(EXTRA_CODE, reminder.item.code)
        .putExtra(EXTRA_SCHEDULED_FOR, LocalTimes.format(reminder.item.scheduledAt))

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_reminders_description)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_alerts_description)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * [label]: nombre de la medición o del producto (y su dosis). Acciones directas (H-017): anotar, posponer
     * 10 o 30 minutos y «no se hizo» (abre la app para pedir el motivo).
     */
    fun notifyReminder(context: Context, reminder: DueReminder, label: String) {
        if (!canNotify(context)) return
        val id = idOf(reminder)
        val open = itemExtras(Intent(context, MainActivity::class.java), reminder).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notDone = itemExtras(Intent(context, MainActivity::class.java), reminder).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_NOT_DONE, true)
        fun snooze(minutes: Long) = PendingIntent.getBroadcast(
            context, id + minutes.toInt(),
            itemExtras(Intent(context, NotificationActionReceiver::class.java).setAction(NotificationActionReceiver.ACTION_SNOOZE), reminder)
                .putExtra(EXTRA_SNOOZE_MINUTES, minutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val isProduct = reminder.item.kind == PendingKind.PRODUCT
        if (reminder.item.kind == PendingKind.REFEED) {
            // H-031: el paso se marca en la pantalla de vuelta a comer; aquí solo abrir o posponer.
            val refeedActions = listOf(
                NotificationCompat.Action(0, context.getString(R.string.notification_action_open_refeed), activity(context, id, open)),
                NotificationCompat.Action(0, context.getString(R.string.notification_action_snooze_10), snooze(10)),
                NotificationCompat.Action(0, context.getString(R.string.notification_action_snooze_30), snooze(30)),
            )
            val title = context.getString(R.string.notification_refeed_title, label)
            post(context, id, reminder.fastId, CHANNEL_REMINDERS, title, Formats.dateTime(reminder.item.scheduledAt), open, R.string.notification_public_title, refeedActions)
            return
        }
        val title = context.getString(if (isProduct) R.string.notification_product_title else R.string.notification_reminder_title, label)
        val actions = listOf(
            NotificationCompat.Action(0, context.getString(if (isProduct) R.string.notification_action_record_product else R.string.notification_action_record), activity(context, id, open)),
            NotificationCompat.Action(0, context.getString(R.string.notification_action_snooze_10), snooze(10)),
            NotificationCompat.Action(0, context.getString(R.string.notification_action_snooze_30), snooze(30)),
            NotificationCompat.Action(0, context.getString(R.string.record_not_done), activity(context, id + 1, notDone)),
        )
        post(context, id, reminder.fastId, CHANNEL_REMINDERS, title, Formats.dateTime(reminder.item.scheduledAt), open, R.string.notification_public_title, actions)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    /** H-045: retira los avisos visibles de un ayuno borrado (recordatorios y alertas). */
    fun cancelForFast(context: Context, fastId: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.notification.extras.getString(EXTRA_FAST_ID) == fastId }.forEach { manager.cancel(it.tag, it.id) }
    }

    private fun activity(context: Context, requestCode: Int, intent: Intent) =
        PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun notifyAlert(context: Context, alert: Alert, level: RuleLevel, message: String) {
        if (!canNotify(context)) return
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ALERT_ID, alert.id)
        val title = context.getString(if (level == RuleLevel.ALERT) R.string.notification_alert_title else R.string.notification_notice_title)
        post(context, alert.id.hashCode(), alert.fastId, CHANNEL_ALERTS, title, message, open, R.string.notification_public_alert)
    }

    private fun post(
        context: Context,
        id: Int,
        fastId: String,
        channel: String,
        title: String,
        text: String,
        open: Intent,
        publicTitle: Int,
        actions: List<NotificationCompat.Action> = emptyList(),
    ) {
        val content = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(publicTitle))
            .build()
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(content)
            .setAutoCancel(true)
            .addExtras(android.os.Bundle().apply { putString(EXTRA_FAST_ID, fastId) })
        actions.forEach { builder.addAction(it) }
        val notification = builder.build()
        @Suppress("MissingPermission") // comprobado en canNotify
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
