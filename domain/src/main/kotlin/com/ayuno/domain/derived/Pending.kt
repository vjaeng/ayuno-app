package com.ayuno.domain.derived

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.StageKind
import java.time.LocalDateTime

enum class PendingState { DONE, PENDING, LATE, NOT_DONE }

enum class PendingKind { METRIC, PRODUCT }

/**
 * Algo que toca hacer a una hora: una medición o una toma de producto. [fromRule]: lo programó una regla
 * («medir de nuevo en N minutos», H-026); suena aunque sea silencio nocturno y cuenta como hecho con cualquier
 * lectura de esa medición posterior a [answeredAfter] (la hora de la alerta).
 */
data class PendingItem(
    val kind: PendingKind,
    val code: String,
    val scheduledAt: LocalDateTime,
    val amount: Double? = null,
    val fromRule: Boolean = false,
    val answeredAfter: LocalDateTime? = null,
)

data class PendingStatus(val item: PendingItem, val state: PendingState)

/** Registros con los que se decide si un pendiente está hecho. */
data class PendingRecords(
    val measurements: List<Measurement> = emptyList(),
    val readings: List<BloodPressureReading> = emptyList(),
    val productLogs: List<ProductLog> = emptyList(),
)

/**
 * Pendientes (derivado): horarios de medición del plan dentro de la ventana de su etapa (H-001) y tomas de los
 * productos que siguen u opcionales activados durante la etapa de ayuno (H-014). Lo desactivado no genera nada.
 */
object Pending {
    const val LATE_AFTER_MINUTES = 15L

    fun items(
        source: FastWithPlan,
        activeTypes: Set<String>,
        products: List<Product>,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<PendingItem> {
        val windows = StageWindows.of(source.fast, source.plan)
        val result = mutableListOf<PendingItem>()
        fun eachDay(time: java.time.LocalTime, stage: StageKind, make: (LocalDateTime) -> PendingItem) {
            var day = from.toLocalDate()
            while (!day.isAfter(to.toLocalDate())) {
                val at = day.atTime(time)
                if (!at.isBefore(from) && !at.isAfter(to) && StageWindows.stageAt(windows, at) == stage) result += make(at)
                day = day.plusDays(1)
            }
        }
        for (metric in source.plan.metrics) {
            if (metric.metricTypeCode !in activeTypes) continue
            metric.schedules.forEach { s -> eachDay(s.timeOfDay, s.stageKind) { PendingItem(PendingKind.METRIC, metric.metricTypeCode, it) } }
        }
        val byCode = products.filter { it.active }.associateBy { it.code }
        for (planned in source.plan.products.filter { it.isReminded }) {
            val product = byCode[planned.productCode] ?: continue
            product.schedules.forEach { s -> eachDay(s.timeOfDay, StageKind.FAST) { PendingItem(PendingKind.PRODUCT, product.code, it, s.amount) } }
        }
        return result.distinct().sortedWith(compareBy({ it.scheduledAt }, { it.kind }, { it.code }))
    }

    /** H-026: recordatorios programados por reglas con acción «medir de nuevo en N minutos». */
    fun ruleFollowUps(source: FastWithPlan, alerts: List<Alert>): List<PendingItem> {
        val rules = source.plan.rules.associateBy { it.id }
        return alerts.mapNotNull { alert ->
            val rule = rules[alert.ruleId] ?: return@mapNotNull null
            val code = rule.metricTypeCode ?: return@mapNotNull null
            val minutes = rule.actionMinutes ?: return@mapNotNull null
            if (rule.action != RuleAction.SCHEDULE_MEASUREMENT) return@mapNotNull null
            PendingItem(PendingKind.METRIC, code, alert.firedAt.plusMinutes(minutes.toLong()), fromRule = true, answeredAfter = alert.firedAt)
        }
    }

    fun status(item: PendingItem, now: LocalDateTime, records: PendingRecords): PendingStatus {
        if (item.fromRule) return PendingStatus(item, followUpState(item, now, records))
        // Cada respuesta es «hecho» (true) o «no se hizo» (false).
        val answers: List<Boolean> = when {
            item.kind == PendingKind.PRODUCT ->
                records.productLogs.filter { it.productCode == item.code && it.scheduledAt == item.scheduledAt }.map { it.status != ProductLogStatus.NOT_TAKEN }
            item.code == MetricType.BLOOD_PRESSURE_CODE ->
                records.readings.filter { it.scheduledFor == item.scheduledAt }.map { it.notDoneReason == null }
            else ->
                records.measurements.filter { it.metricTypeCode == item.code && it.scheduledFor == item.scheduledAt }.map { it.notDoneReason == null }
        }
        val state = when {
            answers.any { it } -> PendingState.DONE
            answers.isNotEmpty() -> PendingState.NOT_DONE
            now.isAfter(item.scheduledAt.plusMinutes(LATE_AFTER_MINUTES)) -> PendingState.LATE
            else -> PendingState.PENDING
        }
        return PendingStatus(item, state)
    }

    private fun followUpState(item: PendingItem, now: LocalDateTime, records: PendingRecords): PendingState {
        val after = item.answeredAfter ?: item.scheduledAt
        val answers: List<Pair<LocalDateTime?, String?>> = if (item.code == MetricType.BLOOD_PRESSURE_CODE) {
            records.readings.map { Triple(it.takenAt, it.scheduledFor, it.notDoneReason) }.filter { it.first.isAfter(after) || it.second == item.scheduledAt }.map { it.second to it.third }
        } else {
            records.measurements.filter { it.metricTypeCode == item.code }.map { Triple(it.takenAt, it.scheduledFor, it.notDoneReason) }
                .filter { it.first.isAfter(after) || it.second == item.scheduledAt }.map { it.second to it.third }
        }
        return when {
            answers.any { it.second == null } -> PendingState.DONE
            answers.any { it.first == item.scheduledAt } -> PendingState.NOT_DONE
            now.isAfter(item.scheduledAt.plusMinutes(LATE_AFTER_MINUTES)) -> PendingState.LATE
            else -> PendingState.PENDING
        }
    }
}
