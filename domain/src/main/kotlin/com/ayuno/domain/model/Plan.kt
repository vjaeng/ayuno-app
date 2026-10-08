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

/** Plan completo de una plantilla o de un ayuno. Lo que aún no tiene historia construida no aparece aquí. */
data class Plan(
    val stages: List<Stage>,
    val metrics: List<PlanMetric>,
    val rules: List<Rule>,
) {
    fun usesStage(kind: StageKind): Boolean = kind == StageKind.FAST || stages.any { it.kind == kind }

    companion object {
        val EMPTY = Plan(emptyList(), emptyList(), emptyList())
    }
}

data class TemplateWithPlan(val template: Template, val plan: Plan)
