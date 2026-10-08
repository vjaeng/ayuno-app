package com.ayuno.domain.derived

import com.ayuno.domain.model.Fast
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Milestone(val hours: Double, val at: LocalDateTime, val reached: Boolean)

/** Reloj del ayuno: siempre calculado desde la hora de inicio guardada, nunca contado (H-006, ADR-004). */
object FastClock {

    fun effectiveStart(fast: Fast): LocalDateTime = fast.clockStartActual ?: fast.clockStartPlanned

    /** Fin previsto: inicio real (o previsto si aún no empezó) + objetivo. */
    fun plannedEnd(fast: Fast): LocalDateTime = effectiveStart(fast).plusMinutes(hoursToMinutes(fast.targetHours))

    fun elapsed(fast: Fast, now: Instant): Duration? {
        val start = fast.clockStartActual ?: return null
        val until = fast.endActual?.let { toInstant(fast, it) } ?: now
        return Duration.between(toInstant(fast, start), until).coerceAtLeast(Duration.ZERO)
    }

    fun remaining(fast: Fast, now: Instant): Duration =
        Duration.between(now, toInstant(fast, plannedEnd(fast))).coerceAtLeast(Duration.ZERO)

    fun finalDuration(fast: Fast): Duration? {
        val start = fast.clockStartActual ?: return null
        val end = fast.endActual ?: return null
        return Duration.between(toInstant(fast, start), toInstant(fast, end))
    }

    fun milestones(fast: Fast, now: Instant): List<Milestone> {
        val start = effectiveStart(fast)
        val reachedUntil = fast.endActual?.let { toInstant(fast, it) } ?: now
        return (fast.milestoneHours + fast.targetHours).distinct().sorted().map { hours ->
            val at = start.plusMinutes(hoursToMinutes(hours))
            Milestone(hours, at, fast.clockStartActual != null && !toInstant(fast, at).isAfter(reachedUntil))
        }
    }

    /** «Hora 33»: horas completas desde el inicio real del reloj hasta [time]. */
    fun fastHour(fast: Fast, time: LocalDateTime): Long? {
        val start = fast.clockStartActual ?: return null
        if (time.isBefore(start)) return null
        return Duration.between(start, time).toHours()
    }

    fun toInstant(fast: Fast, time: LocalDateTime): Instant = time.toInstant(ZoneOffset.of(fast.tzOffset))

    private fun hoursToMinutes(hours: Double): Long = Math.round(hours * 60)
}
