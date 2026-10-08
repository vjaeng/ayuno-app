package com.ayuno.domain.derived

import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import java.time.LocalDateTime

enum class PendingState { DONE, PENDING, LATE, NOT_DONE }

data class PendingItem(val metricTypeCode: String, val scheduledAt: LocalDateTime)

data class PendingStatus(val item: PendingItem, val state: PendingState)

/**
 * Pendientes de medición (derivado): horarios del plan del ayuno dentro de la ventana de su etapa.
 * Solo mediciones activas en el plan y en el catálogo (H-001: una desactivada no genera recordatorios).
 */
object Pending {
    const val LATE_AFTER_MINUTES = 15L

    fun items(source: FastWithPlan, activeTypes: Set<String>, from: LocalDateTime, to: LocalDateTime): List<PendingItem> {
        val windows = StageWindows.of(source.fast, source.plan)
        val result = mutableListOf<PendingItem>()
        for (metric in source.plan.metrics) {
            if (metric.metricTypeCode !in activeTypes) continue
            for (schedule in metric.schedules) {
                var day = from.toLocalDate()
                while (!day.isAfter(to.toLocalDate())) {
                    val at = day.atTime(schedule.timeOfDay)
                    if (!at.isBefore(from) && !at.isAfter(to) && StageWindows.stageAt(windows, at) == schedule.stageKind) {
                        result += PendingItem(metric.metricTypeCode, at)
                    }
                    day = day.plusDays(1)
                }
            }
        }
        return result.distinct().sortedWith(compareBy({ it.scheduledAt }, { it.metricTypeCode }))
    }

    fun status(
        item: PendingItem,
        now: LocalDateTime,
        measurements: List<Measurement>,
        readings: List<BloodPressureReading>,
    ): PendingStatus {
        val answers: List<String?> = if (item.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE) {
            readings.filter { it.scheduledFor == item.scheduledAt }.map { it.notDoneReason }
        } else {
            measurements.filter { it.metricTypeCode == item.metricTypeCode && it.scheduledFor == item.scheduledAt }.map { it.notDoneReason }
        }
        val state = when {
            answers.any { it == null } -> PendingState.DONE
            answers.isNotEmpty() -> PendingState.NOT_DONE
            now.isAfter(item.scheduledAt.plusMinutes(LATE_AFTER_MINUTES)) -> PendingState.LATE
            else -> PendingState.PENDING
        }
        return PendingStatus(item, state)
    }
}
