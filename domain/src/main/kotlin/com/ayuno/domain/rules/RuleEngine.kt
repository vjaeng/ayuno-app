package com.ayuno.domain.rules

import com.ayuno.domain.derived.DailyTotal
import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.StageWindow
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.model.SymptomType
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
    val symptoms: List<Symptom> = emptyList(),
    val symptomTypes: List<SymptomType> = emptyList(),
    val intakes: List<Intake> = emptyList(),
    val intakeTypes: List<IntakeType> = emptyList(),
    val goals: List<IntakeGoal> = emptyList(),
    val now: LocalDateTime? = null,
)

/**
 * Motor de reglas puro (ADR-006). No tiene efectos: decide qué reglas se cumplen.
 *
 * - «Por debajo / por encima de X»: se mira la racha final de lecturas fuera del umbral.
 * - «Sostenida N min»: la racha (sin ninguna lectura dentro del umbral en medio) abarca ≥ N minutos.
 * - Episodios (Q-01): una regla no se repite mientras su racha siga abierta; se rearma con una lectura dentro del umbral.
 *
 * - Síntoma de alarma (H-021): salta por cada síntoma de alarma nuevo, con cualquier valor de las mediciones.
 * - Ingesta bajo la meta (H-022): a partir de su hora, una vez al día, si el total del día no llega a la meta.
 * - Manual (H-023): no la evalúa el motor; la dispara el botón «algo no va bien».
 */
object RuleEngine {

    private data class Reading(val id: String, val takenAt: LocalDateTime, val value: Double)

    fun evaluate(context: RuleContext): List<RuleHit> = context.rules
        .filter { it.active }
        .mapNotNull { rule ->
            when (rule.conditionKind) {
                RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE -> evaluateMetric(rule, context)
                RuleConditionKind.ALARM_SYMPTOM -> evaluateSymptom(rule, context)
                RuleConditionKind.INTAKE_BELOW_GOAL -> evaluateIntake(rule, context)
                RuleConditionKind.MANUAL -> null
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

    private fun evaluateSymptom(rule: Rule, context: RuleContext): RuleHit? {
        val alarmCodes = context.symptomTypes.filter { it.isAlarm }.map { it.code }.toSet()
        val alreadyAlerted = context.alerts.filter { it.ruleId == rule.id }.flatMap { it.triggerEntryIds }.toSet()
        val candidate = context.symptoms
            .filter { it.symptomTypeCode in alarmCodes && (rule.symptomTypeCode == null || it.symptomTypeCode == rule.symptomTypeCode) }
            .filter { inStage(rule, context, it.takenAt) && it.id !in alreadyAlerted }
            .maxByOrNull { it.takenAt } ?: return null
        return RuleHit(rule, listOf(candidate.id))
    }

    private fun evaluateIntake(rule: Rule, context: RuleContext): RuleHit? {
        val now = context.now ?: return null
        val checkTime = rule.checkTime ?: return null
        val code = rule.intakeTypeCode ?: return null
        if (now.toLocalTime().isBefore(checkTime) || !inStage(rule, context, now)) return null
        if (context.alerts.any { it.ruleId == rule.id && it.firedAt.toLocalDate() == now.toLocalDate() }) return null
        val total = DailyTotals.of(now.toLocalDate(), context.intakes, context.intakeTypes, context.goals).firstOrNull { it.intakeType.code == code }
            ?: context.goals.firstOrNull { it.intakeTypeCode == code }?.let { goal ->
                context.intakeTypes.firstOrNull { it.code == code }?.let { DailyTotal(it, 0.0, goal, null) }
            } ?: return null
        val reached = DailyTotals.amountForGoal(total) ?: return null
        val goal = total.goal ?: return null
        if (reached >= goal.dailyAmount) return null
        val todays = context.intakes.filter { it.intakeTypeCode == code && it.takenAt.toLocalDate() == now.toLocalDate() }.map { it.id }
        return RuleHit(rule, todays)
    }

    private fun inStage(rule: Rule, context: RuleContext, time: LocalDateTime): Boolean =
        rule.filterStageKinds.isEmpty() || StageWindows.stageAt(context.stageWindows, time) in rule.filterStageKinds

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
