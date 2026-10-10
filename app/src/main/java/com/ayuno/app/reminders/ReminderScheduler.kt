package com.ayuno.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ayuno.app.MainActivity
import com.ayuno.domain.derived.DayAgenda
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.SettingsRepository
import com.ayuno.domain.derived.PendingItem
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDateTime

/** Pendiente que toca recordar, con el ayuno al que pertenece. */
data class DueReminder(val fastId: String, val item: PendingItem)

/**
 * Programa los recordatorios con alarmas de despertador del sistema (ADR-005, enmienda: solo la **siguiente**).
 * Los puntos de alarma son los pendientes (mediciones y tomas) y las horas de comprobación de las reglas de
 * ingesta (H-022); en cada alarma se notifica lo que vence y se evalúan las reglas del ayuno en curso (H-019).
 */
class ReminderScheduler(
    private val context: Context,
    private val fasts: FastRepository,
    private val catalog: CatalogRepository,
    private val records: RecordRepository,
    private val alerts: AlertRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    /**
     * Pendientes sin hacer en las próximas [HORIZON_HOURS] horas que se notifican: los del plan fuera del silencio
     * nocturno y los programados por reglas siempre (H-017, H-026).
     */
    suspend fun upcoming(from: LocalDateTime = LocalTimes.now(clock)): List<DueReminder> {
        val active = catalog.metricTypes().filter { it.active }.map { it.code }.toSet()
        val products = catalog.products()
        val quiet = settings.get()
        val to = from.plusHours(HORIZON_HOURS)
        return sources().flatMap { source ->
            val pending = PendingRecords(
                records.measurements(source.fast.id), records.bloodPressure(source.fast.id), records.productLogs(source.fast.id),
            )
            val followUps = Pending.ruleFollowUps(source, alerts.alerts(source.fast.id)).filter { !it.scheduledAt.isBefore(from) && !it.scheduledAt.isAfter(to) }
            (Pending.items(source, active, products, from, to) + followUps)
                .filter { DayAgenda.isNotified(it, quiet) }
                .filter { Pending.status(it, from, pending).state == PendingState.PENDING }
                .map { DueReminder(source.fast.id, it) }
        }.sortedBy { it.item.scheduledAt }
    }

    /** H-017: vuelve a avisar de [reminder] dentro de [minutes] minutos (alarma propia, no altera la cadena). */
    fun snooze(reminder: DueReminder, minutes: Long) {
        val at = LocalTimes.now(clock).plusMinutes(minutes)
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_SNOOZED)
            .putExtra(ReminderNotifier.EXTRA_FAST_ID, reminder.fastId)
            .putExtra(ReminderNotifier.EXTRA_KIND, reminder.item.kind.name)
            .putExtra(ReminderNotifier.EXTRA_CODE, reminder.item.code)
            .putExtra(ReminderNotifier.EXTRA_SCHEDULED_FOR, LocalTimes.format(reminder.item.scheduledAt))
        val operation = PendingIntent.getBroadcast(context, ReminderNotifier.idOf(reminder), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val millis = at.atZone(clock.zone).toInstant().toEpochMilli()
        if (canScheduleExact()) {
            val show = PendingIntent.getActivity(context, SHOW_REQUEST, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    /** El pendiente sigue sin hacerse (para un aviso pospuesto). */
    suspend fun stillPending(reminder: DueReminder): Boolean {
        val source = fasts.get(reminder.fastId) ?: return false
        val pending = PendingRecords(records.measurements(source.fast.id), records.bloodPressure(source.fast.id), records.productLogs(source.fast.id))
        val state = Pending.status(reminder.item, LocalTimes.now(clock), pending).state
        return state == PendingState.PENDING || state == PendingState.LATE
    }

    /** Horas de comprobación de reglas de ingesta del ayuno en curso. */
    private suspend fun ruleCheckPoints(from: LocalDateTime): List<LocalDateTime> {
        val current = fasts.all().firstOrNull { it.status == FastStatus.IN_PROGRESS } ?: return emptyList()
        val rules = fasts.get(current.id)?.plan?.rules.orEmpty().filter { it.active && it.conditionKind == RuleConditionKind.INTAKE_BELOW_GOAL }
        return rules.mapNotNull { it.checkTime }.flatMap { t -> listOf(from.toLocalDate().atTime(t), from.toLocalDate().plusDays(1).atTime(t)) }
            .filter { it.isAfter(from) }
    }

    suspend fun reschedule() {
        val from = LocalTimes.now(clock).plusSeconds(1)
        val next = (upcoming(from).map { it.item.scheduledAt } + ruleCheckPoints(from)).minOrNull()
        if (next == null) {
            alarms.cancel(operation(null))
            return
        }
        val triggerMillis = next.atZone(clock.zone).toInstant().toEpochMilli()
        val operation = operation(next)
        if (canScheduleExact()) {
            val show = PendingIntent.getActivity(context, SHOW_REQUEST, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, show), operation)
        } else {
            // Sin permiso de alarmas exactas: mejor un aviso algo tardío que ninguno. La app lo indica en Inicio.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        }
    }

    /** Lo que vence exactamente a [at] y sigue sin hacerse. */
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
