package com.ayuno.domain.usecase

import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.port.TemplateRepository

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
        val plan = input.plan.copy(refeed = input.plan.refeed.map(RefeedStepValidator::normalize))
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
            if (it.dailyAmount <= 0) add(ValidationError.GOAL_NOT_POSITIVE)
            if (it.unit.isBlank()) add(ValidationError.UNIT_REQUIRED)
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
 * (H-022) y manual (H-023); acciones «ninguna», «ofrecer terminar» y «medir de nuevo en N minutos» (H-026), esta
 * solo en condiciones de medición: el recordatorio necesita saber qué medir.
 */
object RuleValidator {
    val SUPPORTED_CONDITIONS = RuleConditionKind.entries.toSet()
    private val METRIC_CONDITIONS = setOf(RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE)
    val SUPPORTED_ACTIONS = RuleAction.entries.toSet()

    fun validate(rule: Rule): List<ValidationError> = buildList {
        if (rule.name.isBlank()) add(ValidationError.NAME_REQUIRED)
        if (rule.message.isBlank()) add(ValidationError.RULE_MESSAGE_REQUIRED)
        if (rule.conditionKind !in SUPPORTED_CONDITIONS) add(ValidationError.RULE_CONDITION_UNSUPPORTED)
        if (rule.action !in SUPPORTED_ACTIONS) add(ValidationError.RULE_ACTION_UNSUPPORTED)
        if (rule.action == RuleAction.SCHEDULE_MEASUREMENT) {
            if (rule.actionMinutes == null || rule.actionMinutes <= 0) add(ValidationError.RULE_ACTION_MINUTES_REQUIRED)
            if (rule.conditionKind !in METRIC_CONDITIONS) add(ValidationError.RULE_ACTION_NEEDS_METRIC)
        }
        if (rule.conditionKind == RuleConditionKind.INTAKE_BELOW_GOAL) {
            if (rule.intakeTypeCode.isNullOrBlank()) add(ValidationError.RULE_INTAKE_REQUIRED)
            if (rule.checkTime == null) add(ValidationError.RULE_CHECK_TIME_REQUIRED)
        }
        if (rule.conditionKind in METRIC_CONDITIONS) {
            if (rule.metricTypeCode.isNullOrBlank()) add(ValidationError.RULE_METRIC_REQUIRED)
            if (rule.threshold == null) add(ValidationError.RULE_THRESHOLD_REQUIRED)
            if (rule.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE && rule.bpComponent == null) add(ValidationError.RULE_BP_COMPONENT_REQUIRED)
            if (rule.sustainedMinutes != null && rule.sustainedMinutes <= 0) add(ValidationError.RULE_SUSTAINED_NOT_POSITIVE)
        }
    }
}
