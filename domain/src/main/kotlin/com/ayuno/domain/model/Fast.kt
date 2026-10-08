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

data class FastWithPlan(val fast: Fast, val plan: Plan)
