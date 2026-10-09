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

    /** [label]: nombre de la medición o del producto (y su dosis). */
    fun notifyReminder(context: Context, reminder: DueReminder, label: String) {
        if (!canNotify(context)) return
        val scheduledFor = LocalTimes.format(reminder.item.scheduledAt)
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_FAST_ID, reminder.fastId)
            .putExtra(EXTRA_KIND, reminder.item.kind.name)
            .putExtra(EXTRA_CODE, reminder.item.code)
            .putExtra(EXTRA_SCHEDULED_FOR, scheduledFor)
        val id = "${reminder.fastId}|${reminder.item.kind}|${reminder.item.code}|$scheduledFor".hashCode()
        val title = context.getString(
            if (reminder.item.kind == PendingKind.PRODUCT) R.string.notification_product_title else R.string.notification_reminder_title,
            label,
        )
        post(context, id, CHANNEL_REMINDERS, title, Formats.dateTime(reminder.item.scheduledAt), open, R.string.notification_public_title)
    }

    fun notifyAlert(context: Context, alert: Alert, level: RuleLevel, message: String) {
        if (!canNotify(context)) return
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ALERT_ID, alert.id)
        val title = context.getString(if (level == RuleLevel.ALERT) R.string.notification_alert_title else R.string.notification_notice_title)
        post(context, alert.id.hashCode(), CHANNEL_ALERTS, title, message, open, R.string.notification_public_alert)
    }

    private fun post(context: Context, id: Int, channel: String, title: String, text: String, open: Intent, publicTitle: Int) {
        val content = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(publicTitle))
            .build()
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(content)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // comprobado en canNotify
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
