package com.ayuno.domain.model

import java.time.LocalTime

/** Propietario de una entidad de plan: una plantilla **o** un ayuno (D-03). */
sealed interface PlanOwner {
    val id: String

    data class OfTemplate(override val id: String) : PlanOwner
    data class OfFast(override val id: String) : PlanOwner
}

data class Template(
    val id: String,
    val name: String,
    val targetHours: Double,
    val milestoneHours: List<Double>,
)

data class Stage(
    val id: String,
    val kind: StageKind,
    val durationValue: Double?,
    val durationUnit: DurationUnit,
)

data class MetricSchedule(
    val id: String,
    val stageKind: StageKind,
    val timeOfDay: LocalTime,
)

data class PlanMetric(
    val id: String,
    val metricTypeCode: String,
    val schedules: List<MetricSchedule>,
)

/** `intake_goal` (H-002). */
data class IntakeGoal(val id: String, val intakeTypeCode: String, val dailyAmount: Double, val unit: String)

/** `checklist_item` (H-002): solo informa, nunca bloquea empezar. */
data class ChecklistItem(val id: String, val text: String, val isChecked: Boolean)

/** `stage_notice` (H-002): aviso fijo visible solo durante sus etapas. */
data class StageNotice(val id: String, val text: String, val stageKinds: List<StageKind>)

/** `plan_product` (H-014): estado del producto durante el ayuno. */
data class PlanProduct(
    val id: String,
    val productCode: String,
    val duringFast: DuringFast,
    val optionalEnabled: Boolean,
    val reason: String?,
) {
    /** Solo se recuerdan los que siguen y los opcionales activados (H-014). */
    val isReminded: Boolean get() = duringFast == DuringFast.CONTINUES || (duringFast == DuringFast.OPTIONAL && optionalEnabled)
}

/**
 * `refeed_step` (H-031): paso de la vuelta a comer. `timed`: a [offsetMinutes] del fin real, repetido cada
 * [repeatEveryMinutes] durante [repeatForMinutes] si se indica; `reintroduction`: el día [dayOffset] tras el fin.
 */
data class RefeedStep(
    val id: String,
    val kind: RefeedStepKind,
    val description: String,
    val offsetMinutes: Int?,
    val repeatEveryMinutes: Int?,
    val repeatForMinutes: Int?,
    val dayOffset: Int?,
)

/** Plan completo de una plantilla o de un ayuno. */
data class Plan(
    val stages: List<Stage>,
    val metrics: List<PlanMetric>,
    val rules: List<Rule>,
    val goals: List<IntakeGoal> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val notices: List<StageNotice> = emptyList(),
    val products: List<PlanProduct> = emptyList(),
    val refeed: List<RefeedStep> = emptyList(),
) {
    fun usesStage(kind: StageKind): Boolean = kind == StageKind.FAST || stages.any { it.kind == kind }

    companion object {
        val EMPTY = Plan(emptyList(), emptyList(), emptyList())
    }
}

data class TemplateWithPlan(val template: Template, val plan: Plan)
