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
)

data class Alert(
    val id: String,
    val fastId: String,
    val ruleId: String,
    val firedAt: LocalDateTime,
    val triggerEntryIds: List<String>,
    val userChoice: UserChoice?,
    val choiceReason: String?,
)
