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
import com.ayuno.domain.time.LocalTimes

/** Notificaciones locales de recordatorio (H-016). En la pantalla de bloqueo solo se ve un texto genérico (ADR-SEG-002). */
object ReminderNotifier {
    const val CHANNEL_REMINDERS = "reminders"
    const val EXTRA_FAST_ID = "fast_id"
    const val EXTRA_METRIC_CODE = "metric_code"
    const val EXTRA_SCHEDULED_FOR = "scheduled_for"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(context: Context, reminder: DueReminder, metricName: String) {
        if (!canNotify(context)) return
        val scheduledFor = LocalTimes.format(reminder.item.scheduledAt)
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_FAST_ID, reminder.fastId)
            .putExtra(EXTRA_METRIC_CODE, reminder.item.metricTypeCode)
            .putExtra(EXTRA_SCHEDULED_FOR, scheduledFor)
        val id = "${reminder.fastId}|${reminder.item.metricTypeCode}|$scheduledFor".hashCode()
        val content = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_public_title))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_reminder_title, metricName))
            .setContentText(Formats.dateTime(reminder.item.scheduledAt))
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
