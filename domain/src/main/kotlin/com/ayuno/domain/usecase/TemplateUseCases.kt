package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.model.TrendOf
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.TemplateRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.time.LocalTimes
import java.time.Clock

/** H-003: duplicar una plantilla en otra independiente, con identificadores nuevos en todo su plan. */
class DuplicateTemplate(private val templates: TemplateRepository) {
    suspend operator fun invoke(id: String, copyName: String): Outcome<TemplateWithPlan> {
        val source = templates.get(id) ?: return invalid(ValidationError.TEMPLATE_NOT_FOUND)
        val copy = TemplateWithPlan(source.template.copy(id = Ids.newId(), name = copyName.trim()), source.plan.copyWithNewIds())
        templates.save(copy)
        return Outcome.Ok(copy)
    }
}

/**
 * H-003: borrar (en blando) o restaurar una plantilla, con constancia en la bitácora (S8). Los ayunos creados desde
 * ella no cambian: llevan su propia copia del plan (D-03).
 */
class SetTemplateDeleted(
    private val templates: TemplateRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: String, deleted: Boolean): Outcome<Unit> = transactions.inTransaction {
        templates.setDeleted(id, deleted)
        audit.append(
            EditLogEntry(
                id = Ids.newId(), entityName = "template", rowId = id, action = if (deleted) EditAction.DELETE else EditAction.RESTORE,
                editedAt = LocalTimes.now(clock), field = null, oldValue = null, newValue = null, reason = null,
            ),
        )
        Outcome.Ok(Unit)
    }
}

/** H-001 (plantilla, etapas, hitos, mediciones y horarios) y H-019/H-020 (reglas de la plantilla). */
class SaveTemplate(private val templates: TemplateRepository) {

    suspend operator fun invoke(input: TemplateWithPlan): Outcome<TemplateWithPlan> {
        val template = input.template.copy(name = input.template.name.trim())
        val errors = buildList {
            if (template.name.isEmpty()) add(ValidationError.NAME_REQUIRED)
            if (template.targetHours <= 0) add(ValidationError.TARGET_HOURS_NOT_POSITIVE)
            if (template.milestoneHours.any { it <= 0 }) add(ValidationError.MILESTONE_NOT_POSITIVE)
            addAll(PlanValidator.validate(input.plan))
        }.distinct()
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val oldRules = templates.get(template.id)?.plan?.rules.orEmpty().associateBy { it.id }
        val plan = input.plan.copy(
            refeed = input.plan.refeed.map(RefeedStepValidator::normalize),
            rules = input.plan.rules.map { RuleValidator.normalize(it, oldRules[it.id]) },
        )
        val saved = TemplateWithPlan(template.copy(milestoneHours = template.milestoneHours.distinct().sorted()), plan)
        templates.save(saved)
        return Outcome.Ok(saved)
    }
}

object PlanValidator {
    fun validate(plan: Plan): List<ValidationError> = buildList {
        plan.stages.filter { it.kind != StageKind.FAST }.forEach {
            if (it.durationValue == null || it.durationValue <= 0) add(ValidationError.STAGE_DURATION_REQUIRED)
        }
        plan.metrics.flatMap { it.schedules }.forEach {
            if (!plan.usesStage(it.stageKind)) add(ValidationError.SCHEDULE_STAGE_NOT_USED)
        }
        plan.rules.forEach { addAll(RuleValidator.validate(it)) }
        if (plan.rules.count { it.conditionKind == RuleConditionKind.MANUAL } > 1) add(ValidationError.RULE_MANUAL_DUPLICATED)
        plan.goals.forEach {
            // Una meta puede quedar sin cifra («por llenar», D-25).
            if (it.dailyAmount != null && it.dailyAmount <= 0) add(ValidationError.GOAL_NOT_POSITIVE)
            if (it.unit.isBlank()) add(ValidationError.UNIT_REQUIRED)
            if (it.nudgeAfterHours != null && it.nudgeAfterHours <= 0) add(ValidationError.NUDGE_HOURS_NOT_POSITIVE)
        }
        plan.metrics.forEach { m ->
            val seconds = m.standingCheckSeconds ?: return@forEach
            if (m.metricTypeCode != MetricType.BLOOD_PRESSURE_CODE || seconds.any { it <= 0 } || seconds != seconds.distinct().sorted()) {
                add(ValidationError.STANDING_SECONDS_INVALID)
            }
        }
        plan.checklist.forEach { if (it.text.isBlank()) add(ValidationError.TEXT_REQUIRED) }
        plan.notices.forEach {
            if (it.text.isBlank()) add(ValidationError.TEXT_REQUIRED)
            if (it.stageKinds.isEmpty()) add(ValidationError.NOTICE_STAGES_REQUIRED)
        }
        if (plan.refeed.isNotEmpty() && !plan.usesStage(StageKind.REFEED)) add(ValidationError.REFEED_STAGE_NOT_USED)
        plan.refeed.forEach { addAll(RefeedStepValidator.validate(it)) }
    }
}

/** H-031: un paso por horas necesita su hora tras el fin (y ventana si se repite); uno de reintroducción, su día. */
object RefeedStepValidator {
    fun validate(step: RefeedStep): List<ValidationError> = buildList {
        if (step.description.isBlank()) add(ValidationError.TEXT_REQUIRED)
        when (step.kind) {
            RefeedStepKind.TIMED -> {
                if (step.offsetMinutes == null || step.offsetMinutes < 0) add(ValidationError.REFEED_OFFSET_REQUIRED)
                if (step.repeatEveryMinutes != null) {
                    if (step.repeatEveryMinutes <= 0) add(ValidationError.AMOUNT_NOT_POSITIVE)
                    if (step.repeatForMinutes == null || step.repeatForMinutes <= 0) add(ValidationError.REFEED_REPEAT_FOR_REQUIRED)
                }
            }
            RefeedStepKind.REINTRODUCTION -> if (step.dayOffset == null || step.dayOffset < 1) add(ValidationError.REFEED_DAY_REQUIRED)
        }
    }

    /** Normaliza según el tipo: lo que no aplica queda vacío (el modelo lo exige así). */
    fun normalize(step: RefeedStep): RefeedStep = when (step.kind) {
        RefeedStepKind.TIMED -> step.copy(
            description = step.description.trim(),
            repeatForMinutes = step.repeatForMinutes.takeIf { step.repeatEveryMinutes != null },
            dayOffset = null,
        )
        RefeedStepKind.REINTRODUCTION -> step.copy(description = step.description.trim(), offsetMinutes = null, repeatEveryMinutes = null, repeatForMinutes = null)
    }
}

/**
 * Validación de reglas: condiciones de medición (H-019, H-020), síntoma de alarma (H-021), ingesta bajo la meta
 * (H-022), manual (H-023) y entre lecturas (H-054 a H-056); acciones «ninguna», «ofrecer terminar» y «medir de nuevo
 * en N minutos» (H-026), esta solo en condiciones sobre una medición: el recordatorio necesita saber qué medir. Las
 * cifras solo son obligatorias para activar la regla (H-003: las de ejemplo vienen inactivas y sin cifra).
 */
object RuleValidator {
    val SUPPORTED_CONDITIONS = RuleConditionKind.entries.toSet()
    private val METRIC_CONDITIONS = setOf(RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE)
    private val BETWEEN_READINGS = setOf(RuleConditionKind.PAIR_DIFFERENCE, RuleConditionKind.CHANGE_FROM_BASELINE, RuleConditionKind.TREND)
    val SUPPORTED_ACTIONS = RuleAction.entries.toSet()

    fun validate(rule: Rule): List<ValidationError> = buildList {
        if (rule.name.isBlank()) add(ValidationError.NAME_REQUIRED)
        if (rule.message.isBlank()) add(ValidationError.RULE_MESSAGE_REQUIRED)
        if (rule.conditionKind !in SUPPORTED_CONDITIONS) add(ValidationError.RULE_CONDITION_UNSUPPORTED)
        if (rule.action !in SUPPORTED_ACTIONS) add(ValidationError.RULE_ACTION_UNSUPPORTED)
        if (rule.action == RuleAction.SCHEDULE_MEASUREMENT) {
            if (rule.actionMinutes == null || rule.actionMinutes <= 0) add(ValidationError.RULE_ACTION_MINUTES_REQUIRED)
            if (rule.conditionKind !in METRIC_CONDITIONS + BETWEEN_READINGS) add(ValidationError.RULE_ACTION_NEEDS_METRIC)
        }
        if (rule.conditionKind == RuleConditionKind.INTAKE_BELOW_GOAL) {
            if (rule.intakeTypeCode.isNullOrBlank()) add(ValidationError.RULE_INTAKE_REQUIRED)
            if (rule.checkTime == null && rule.active) add(ValidationError.RULE_CHECK_TIME_REQUIRED)
            if (rule.goalPercent != null && rule.goalPercent !in 1.0..100.0) add(ValidationError.RULE_GOAL_PERCENT_OUT_OF_RANGE)
        }
        if (rule.conditionKind in METRIC_CONDITIONS + BETWEEN_READINGS) {
            if (rule.metricTypeCode.isNullOrBlank()) add(ValidationError.RULE_METRIC_REQUIRED)
            if (rule.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE && rule.bpComponent == null) add(ValidationError.RULE_BP_COMPONENT_REQUIRED)
        }
        if (rule.conditionKind in METRIC_CONDITIONS) {
            // Una regla inactiva puede quedar sin umbral (estructura de la plantilla de ejemplo, H-003): nunca se evalúa.
            if (rule.threshold == null && rule.active) add(ValidationError.RULE_THRESHOLD_REQUIRED)
            if (rule.sustainedMinutes != null && rule.sustainedMinutes <= 0) add(ValidationError.RULE_SUSTAINED_NOT_POSITIVE)
        }
        if (rule.conditionKind in BETWEEN_READINGS && rule.direction == null) add(ValidationError.RULE_DIRECTION_REQUIRED)
        when (rule.conditionKind) {
            RuleConditionKind.PAIR_DIFFERENCE -> {
                if (rule.metricTypeCode != null && rule.metricTypeCode != MetricType.BLOOD_PRESSURE_CODE) add(ValidationError.RULE_TREND_NEEDS_BLOOD_PRESSURE)
                if (rule.threshold == null && rule.active) add(ValidationError.RULE_THRESHOLD_REQUIRED)
            }
            RuleConditionKind.CHANGE_FROM_BASELINE -> {
                if (rule.threshold == null && rule.active) add(ValidationError.RULE_THRESHOLD_REQUIRED)
                if (rule.threshold != null && rule.threshold <= 0) add(ValidationError.RULE_THRESHOLD_NOT_POSITIVE)
            }
            RuleConditionKind.TREND -> {
                if ((rule.trendCount ?: 0) < Rule.MIN_TREND_COUNT) add(ValidationError.RULE_TREND_COUNT_TOO_LOW)
                if (rule.trendOf == TrendOf.PAIR_DIFFERENCE && rule.metricTypeCode != MetricType.BLOOD_PRESSURE_CODE) add(ValidationError.RULE_TREND_NEEDS_BLOOD_PRESSURE)
            }
            else -> Unit
        }
    }

    /**
     * Deja la regla coherente con su tipo de condición (lo que no aplica, vacío) y pone a false «de ejemplo» si cambió
     * su nombre, su mensaje o una cifra respecto a [old] (D-25).
     */
    fun normalize(rule: Rule, old: Rule?): Rule {
        val between = rule.conditionKind in BETWEEN_READINGS
        val cleaned = rule.copy(
            direction = rule.direction.takeIf { between },
            trendCount = if (rule.conditionKind == RuleConditionKind.TREND) rule.trendCount ?: Rule.MIN_TREND_COUNT else null,
            trendOf = if (rule.conditionKind == RuleConditionKind.TREND) rule.trendOf ?: TrendOf.VALUE else null,
            threshold = rule.threshold.takeIf { rule.conditionKind != RuleConditionKind.TREND },
            metricTypeCode = if (rule.conditionKind == RuleConditionKind.PAIR_DIFFERENCE) MetricType.BLOOD_PRESSURE_CODE else rule.metricTypeCode,
            sustainedMinutes = rule.sustainedMinutes.takeIf { rule.conditionKind in METRIC_CONDITIONS },
            goalPercent = rule.goalPercent.takeIf { rule.conditionKind == RuleConditionKind.INTAKE_BELOW_GOAL },
        )
        return if (old != null && old.isPlaceholder && cleaned.editedFrom(old)) cleaned.copy(isPlaceholder = false) else cleaned
    }
}

/**
 * H-057: cambiar las reglas y metas de la copia del plan de un ayuno aún planificado («Arreglar ›» de la revisión
 * previa). La plantilla no cambia (D-03); una regla «de ejemplo» deja de serlo al cambiar su nombre, mensaje o cifra.
 */
class SaveFastPlan(private val fasts: FastRepository) {
    suspend operator fun invoke(fastId: String, rules: List<Rule>, goals: List<com.ayuno.domain.model.IntakeGoal>): Outcome<Plan> {
        val source = fasts.get(fastId) ?: return invalid(ValidationError.FAST_NOT_FOUND)
        if (source.fast.status != com.ayuno.domain.model.FastStatus.PLANNED) return invalid(ValidationError.FAST_NOT_PLANNED)
        val oldRules = source.plan.rules.associateBy { it.id }
        val plan = source.plan.copy(rules = rules.map { RuleValidator.normalize(it, oldRules[it.id]) }, goals = goals)
        val errors = PlanValidator.validate(plan).distinct()
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        fasts.updatePlan(fastId, plan)
        return Outcome.Ok(plan)
    }
}
