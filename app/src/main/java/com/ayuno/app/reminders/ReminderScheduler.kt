package com.ayuno.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ayuno.app.MainActivity
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingItem
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDateTime

/** Pendiente que toca recordar, con el ayuno al que pertenece. */
data class DueReminder(val fastId: String, val item: PendingItem)

/**
 * Programa los recordatorios con alarmas de despertador del sistema (ADR-005). Mantiene programada solo la
 * **siguiente** alarma; al sonar, notifica lo que vence a esa hora y programa la siguiente. Se vuelve a calcular
 * al abrir la app, tras cada cambio de plan o registro, al reiniciar el teléfono y al cambiar la hora o la zona.
 */
class ReminderScheduler(
    private val context: Context,
    private val fasts: FastRepository,
    private val catalog: CatalogRepository,
    private val records: RecordRepository,
    private val clock: Clock,
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    /** Recordatorios pendientes en las próximas [HORIZON_HOURS] horas, en orden. */
    suspend fun upcoming(from: LocalDateTime = LocalTimes.now(clock)): List<DueReminder> {
        val active = catalog.metricTypes().filter { it.active }.map { it.code }.toSet()
        val to = from.plusHours(HORIZON_HOURS)
        return sources().flatMap { source ->
            val measurements = records.measurements(source.fast.id)
            val readings = records.bloodPressure(source.fast.id)
            Pending.items(source, active, from, to)
                .filter { Pending.status(it, from, measurements, readings).state == PendingState.PENDING }
                .map { DueReminder(source.fast.id, it) }
        }.sortedBy { it.item.scheduledAt }
    }

    suspend fun reschedule() {
        val now = LocalTimes.now(clock)
        val next = upcoming(now.plusSeconds(1)).firstOrNull()
        if (next == null) {
            alarms.cancel(operation(null))
            return
        }
        val at = next.item.scheduledAt
        val triggerMillis = at.atZone(clock.zone).toInstant().toEpochMilli()
        val operation = operation(at)
        if (canScheduleExact()) {
            val show = PendingIntent.getActivity(context, SHOW_REQUEST, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, show), operation)
        } else {
            // Sin permiso de alarmas exactas: mejor un aviso algo tardío que ninguno. La app lo indica en Inicio.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        }
    }

    /** Lo que vence exactamente a [at] y sigue sin anotarse. */
    suspend fun dueAt(at: LocalDateTime): List<DueReminder> = upcoming(at).filter { it.item.scheduledAt == at }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    private suspend fun sources(): List<FastWithPlan> = fasts.all().mapNotNull { fasts.get(it.id) }

    private fun operation(at: LocalDateTime?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION)
        if (at != null) intent.putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, LocalTimes.format(at))
        return PendingIntent.getBroadcast(context, ALARM_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val HORIZON_HOURS = 48L
        const val ALARM_REQUEST = 1001
        const val SHOW_REQUEST = 1002
    }
}
