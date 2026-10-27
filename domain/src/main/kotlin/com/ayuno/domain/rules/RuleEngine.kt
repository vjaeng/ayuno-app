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
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.TrendOf
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.Position
import com.ayuno.domain.derived.Baseline
import com.ayuno.domain.derived.StandingMoment
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
    /** H-053: tiempos de pie del plan (`plan_metric.standing_check_seconds` de la presión), para el momento de pie. */
    val standingCheckSeconds: List<Int>? = null,
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
 *
 * Condiciones entre lecturas (EV-006, D-24), con los mismos episodios:
 * - Diferencia de pareja (H-054): de pie − sentado en cada lectura de pie enlazada.
 * - Cambio desde la partida (H-055): contra el valor de partida; la propia lectura de partida nunca cuenta.
 * - Tendencia (H-056): N lecturas seguidas que suben (o bajan) cada vez; una igual corta la racha. En la diferencia
 *   de pareja, cada momento de pie se compara con su igual.
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
                RuleConditionKind.PAIR_DIFFERENCE -> evaluatePairDifference(rule, context)
                RuleConditionKind.CHANGE_FROM_BASELINE -> evaluateBaseline(rule, context)
                RuleConditionKind.TREND -> evaluateTrend(rule, context)
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
        // Meta sin cifra: la regla no se evalúa (D-25).
        val amount = total.goal?.dailyAmount ?: return null
        if (reached >= amount * (rule.goalPercent ?: 100.0) / 100.0) return null
        val todays = context.intakes.filter { it.intakeTypeCode == code && it.takenAt.toLocalDate() == now.toLocalDate() }.map { it.id }
        return RuleHit(rule, todays)
    }

    /** Punto de una serie: [ids] son las entradas que lo forman (la sentada y la de pie en una pareja). */
    private data class Point(val ids: List<String>, val takenAt: LocalDateTime, val value: Double)

    /**
     * Episodio sobre una serie (Q-01): [run] es la racha final que cumple; no salta si ya saltó con alguna lectura de
     * esa racha.
     */
    private fun episodeHit(rule: Rule, context: RuleContext, series: List<Point>, run: List<Point>, triggers: List<Point>): RuleHit? {
        if (run.isEmpty()) return null
        val previous = context.alerts.filter { it.ruleId == rule.id }.maxByOrNull { it.firedAt }
        if (previous != null) {
            val lastTrigger = series.filter { p -> p.ids.any { it in previous.triggerEntryIds } }.maxOfOrNull { it.takenAt } ?: previous.firedAt
            if (!run.first().takenAt.isAfter(lastTrigger)) return null
        }
        return RuleHit(rule, triggers.flatMap { it.ids }.distinct())
    }

    private fun compare(direction: RuleDirection?, value: Double, limit: Double): Boolean? = when (direction) {
        RuleDirection.ABOVE -> value > limit
        RuleDirection.BELOW -> value < limit
        null -> null
    }

    /** Diferencias de pareja de [component] por lectura de pie enlazada, en orden. */
    private fun pairPoints(rule: Rule, context: RuleContext, component: BpComponent): List<Pair<BloodPressureReading, Point>> {
        val byId = context.bloodPressure.associateBy { it.id }
        return context.bloodPressure
            .filter { it.position == Position.STANDING && it.notDoneReason == null && (!rule.filterOnWaking || it.onWaking) && inStage(rule, context, it.takenAt) }
            .mapNotNull { standing ->
                val sitting = byId[standing.pairedReadingId ?: return@mapNotNull null] ?: return@mapNotNull null
                val diff = StandingMoment.difference(standing, sitting, component) ?: return@mapNotNull null
                standing to Point(listOf(sitting.id, standing.id), standing.takenAt, diff.toDouble())
            }
            .sortedBy { it.second.takenAt }
    }

    private fun evaluatePairDifference(rule: Rule, context: RuleContext): RuleHit? {
        val threshold = rule.threshold ?: return null
        val component = rule.bpComponent ?: return null
        val series = pairPoints(rule, context, component).map { it.second }
        val run = series.takeLastWhile { compare(rule.direction, it.value, threshold) == true }
        return episodeHit(rule, context, series, run, listOfNotNull(run.lastOrNull()))
    }

    private fun evaluateBaseline(rule: Rule, context: RuleContext): RuleHit? {
        val distance = rule.threshold ?: return null
        val code = rule.metricTypeCode ?: return null
        val (baseId, base) = if (code == MetricType.BLOOD_PRESSURE_CODE) {
            val component = rule.bpComponent ?: return null
            val reading = Baseline.bloodPressure(context.bloodPressure, rule.filterPosition) ?: return null
            reading.id to (reading.value(component) ?: return null).toDouble()
        } else {
            val m = Baseline.measurement(context.measurements, code) ?: return null
            m.id to m.valueNum!!
        }
        val series = readingsFor(rule, context).filter { it.id != baseId }.sortedBy { it.takenAt }.map { Point(listOf(it.id), it.takenAt, it.value) }
        val limit = when (rule.direction) {
            RuleDirection.ABOVE -> base + distance
            RuleDirection.BELOW -> base - distance
            null -> return null
        }
        val run = series.takeLastWhile { compare(rule.direction, it.value, limit) == true }
        return episodeHit(rule, context, series, run, listOfNotNull(run.lastOrNull()))
    }

    private fun evaluateTrend(rule: Rule, context: RuleContext): RuleHit? {
        val count = (rule.trendCount ?: Rule.MIN_TREND_COUNT).coerceAtLeast(Rule.MIN_TREND_COUNT)
        val groups: List<List<Point>> = if (rule.trendOf == TrendOf.PAIR_DIFFERENCE) {
            val component = rule.bpComponent ?: return null
            pairPoints(rule, context, component)
                .groupBy { (standing, _) -> StandingMoment.of(standing, context.standingCheckSeconds) }
                .values.map { list -> list.map { it.second } }
        } else {
            listOf(readingsFor(rule, context).sortedBy { it.takenAt }.map { Point(listOf(it.id), it.takenAt, it.value) })
        }
        return groups.firstNotNullOfOrNull { series ->
            // Racha final: cada punto sube (o baja) respecto al anterior; una igual la corta.
            var start = series.size - 1
            while (start > 0 && compare(rule.direction, series[start].value, series[start - 1].value) == true) start--
            val streak = if (series.isEmpty()) emptyList() else series.subList(start, series.size)
            if (streak.size < count) return@firstNotNullOfOrNull null
            // Episodio: la racha cuenta desde el punto que la completa.
            episodeHit(rule, context, series, streak.drop(count - 1), streak.takeLast(count))
        }
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
