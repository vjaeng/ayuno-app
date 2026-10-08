package com.ayuno.domain.rules

import com.ayuno.domain.derived.StageWindow
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleConditionKind
import java.time.Duration
import java.time.LocalDateTime

/** Regla cumplida y entradas que la dispararon. */
data class RuleHit(val rule: Rule, val triggerEntryIds: List<String>)

/** Datos que el motor necesita: todo del ayuno en curso. */
data class RuleContext(
    val rules: List<Rule>,
    val measurements: List<Measurement>,
    val bloodPressure: List<BloodPressureReading>,
    val alerts: List<Alert>,
    val stageWindows: List<StageWindow>,
)

/**
 * Motor de reglas puro (ADR-006). No tiene efectos: decide qué reglas se cumplen.
 *
 * - «Por debajo / por encima de X»: se mira la racha final de lecturas fuera del umbral.
 * - «Sostenida N min»: la racha (sin ninguna lectura dentro del umbral en medio) abarca ≥ N minutos.
 * - Episodios (Q-01): una regla no se repite mientras su racha siga abierta; se rearma con una lectura dentro del umbral.
 *
 * Condiciones de síntoma, ingesta y manual se añaden con sus historias (H-021, H-022, H-023).
 */
object RuleEngine {

    private data class Reading(val id: String, val takenAt: LocalDateTime, val value: Double)

    fun evaluate(context: RuleContext): List<RuleHit> = context.rules
        .filter { it.active }
        .mapNotNull { rule ->
            when (rule.conditionKind) {
                RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE -> evaluateMetric(rule, context)
                else -> null
            }
        }

    private fun evaluateMetric(rule: Rule, context: RuleContext): RuleHit? {
        val threshold = rule.threshold ?: return null
        val below = rule.conditionKind == RuleConditionKind.METRIC_BELOW
        val readings = readingsFor(rule, context).sortedBy { it.takenAt }
        val outOfRange = { r: Reading -> if (below) r.value < threshold else r.value > threshold }

        val run = readings.takeLastWhile(outOfRange)
        if (run.isEmpty()) return null

        val previous = context.alerts.filter { it.ruleId == rule.id }.maxByOrNull { it.firedAt }
        if (previous != null) {
            val lastTrigger = readings.filter { it.id in previous.triggerEntryIds }.maxOfOrNull { it.takenAt } ?: previous.firedAt
            if (!run.first().takenAt.isAfter(lastTrigger)) return null
        }

        val minutes = rule.sustainedMinutes
        return if (minutes == null) {
            RuleHit(rule, listOf(run.last().id))
        } else {
            val span = Duration.between(run.first().takenAt, run.last().takenAt)
            if (run.size >= 2 && span >= Duration.ofMinutes(minutes.toLong())) RuleHit(rule, run.map { it.id }) else null
        }
    }

    private fun readingsFor(rule: Rule, context: RuleContext): List<Reading> {
        val code = rule.metricTypeCode ?: return emptyList()
        val inStage = { time: LocalDateTime ->
            rule.filterStageKinds.isEmpty() || StageWindows.stageAt(context.stageWindows, time) in rule.filterStageKinds
        }
        return if (code == MetricType.BLOOD_PRESSURE_CODE) {
            val component = rule.bpComponent ?: return emptyList()
            context.bloodPressure
                .filter { it.notDoneReason == null && (!rule.filterOnWaking || it.onWaking) && inStage(it.takenAt) }
                .filter { rule.filterPosition == null || it.position == rule.filterPosition }
                .mapNotNull { r -> r.value(component)?.let { Reading(r.id, r.takenAt, it.toDouble()) } }
        } else {
            context.measurements
                .filter { it.metricTypeCode == code && it.notDoneReason == null && (!rule.filterOnWaking || it.onWaking) && inStage(it.takenAt) }
                .mapNotNull { m -> m.valueNum?.let { Reading(m.id, m.takenAt, it) } }
        }
    }
}
