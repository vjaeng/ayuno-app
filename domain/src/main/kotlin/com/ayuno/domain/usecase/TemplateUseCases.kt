package com.ayuno.domain.usecase

import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Plan
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
        val saved = TemplateWithPlan(template.copy(milestoneHours = template.milestoneHours.distinct().sorted()), input.plan)
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
    }
}

/**
 * Validación de reglas: condiciones de medición (H-019, H-020), síntoma de alarma (H-021), ingesta bajo la meta
 * (H-022) y manual (H-023); acciones «ninguna» y «ofrecer terminar». «Medir de nuevo» llega con H-026.
 */
object RuleValidator {
    val SUPPORTED_CONDITIONS = RuleConditionKind.entries.toSet()
    private val METRIC_CONDITIONS = setOf(RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE)
    val SUPPORTED_ACTIONS = setOf(RuleAction.NONE, RuleAction.OFFER_END)

    fun validate(rule: Rule): List<ValidationError> = buildList {
        if (rule.name.isBlank()) add(ValidationError.NAME_REQUIRED)
        if (rule.message.isBlank()) add(ValidationError.RULE_MESSAGE_REQUIRED)
        if (rule.conditionKind !in SUPPORTED_CONDITIONS) add(ValidationError.RULE_CONDITION_UNSUPPORTED)
        if (rule.action !in SUPPORTED_ACTIONS) add(ValidationError.RULE_ACTION_UNSUPPORTED)
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
