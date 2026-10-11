package com.ayuno.domain.model

import java.time.LocalDateTime

data class Fast(
    val id: String,
    val templateId: String?,
    val status: FastStatus,
    val targetHours: Double,
    val milestoneHours: List<Double>,
    val lastMealPlanned: LocalDateTime,
    val lastMealActual: LocalDateTime?,
    val clockStartPlanned: LocalDateTime,
    val clockStartActual: LocalDateTime?,
    val endActual: LocalDateTime?,
    val endReason: EndReason?,
    val endReasonDetail: String?,
    val endRuleId: String?,
    val tzOffset: String,
    val notes: String?,
)

/**
 * [replacedRules]: reglas de copias del plan sustituidas al cambiar la plantilla (D-16). No se evalúan, pero las
 * alertas que dispararon siguen resolviendo su regla (nudo EV1-01).
 */
data class FastWithPlan(val fast: Fast, val plan: Plan, val replacedRules: List<Rule> = emptyList()) {
    fun ruleById(id: String?): Rule? = id?.let { plan.rules.firstOrNull { r -> r.id == it } ?: replacedRules.firstOrNull { r -> r.id == it } }
}
