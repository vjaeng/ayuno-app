package com.ayuno.domain.model

import java.time.LocalDateTime
import java.time.LocalTime

data class Rule(
    val id: String,
    val name: String,
    val active: Boolean,
    val conditionKind: RuleConditionKind,
    val metricTypeCode: String?,
    val bpComponent: BpComponent?,
    val threshold: Double?,
    val sustainedMinutes: Int?,
    val symptomTypeCode: String?,
    val intakeTypeCode: String?,
    val checkTime: LocalTime?,
    val filterStageKinds: List<StageKind>,
    val filterOnWaking: Boolean,
    val filterPosition: Position?,
    val level: RuleLevel,
    val message: String,
    val action: RuleAction,
    val actionMinutes: Int?,
    /** EV-006 (D-24): obligatorio en `pair_difference`, `change_from_baseline` y `trend`. */
    val direction: RuleDirection? = null,
    val trendCount: Int? = null,
    val trendOf: TrendOf? = null,
    /** H-022 (EV-006): porcentaje de la meta; nulo = 100. */
    val goalPercent: Double? = null,
    /** D-25: la trajo la app de ejemplo; deja de serlo al cambiar nombre, mensaje o cifra. */
    val isPlaceholder: Boolean = false,
) {
    /** D-25: ¿cambió algo que hace que la regla ya no sea «de ejemplo»? */
    fun editedFrom(old: Rule): Boolean =
        name != old.name || message != old.message || threshold != old.threshold || goalPercent != old.goalPercent || checkTime != old.checkTime

    companion object {
        /** H-056: la racha más corta admitida y la de fábrica. */
        const val MIN_TREND_COUNT = 3
    }
}

data class Alert(
    val id: String,
    val fastId: String,
    val ruleId: String,
    val firedAt: LocalDateTime,
    val triggerEntryIds: List<String>,
    val userChoice: UserChoice?,
    val choiceReason: String?,
)
