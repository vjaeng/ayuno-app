package com.ayuno.app.ui.components

import androidx.annotation.StringRes
import com.ayuno.app.R
import com.ayuno.domain.usecase.ValidationError

/** Traducción de los códigos del dominio a mensajes llanos: qué pasó + qué hacer (GUIA-ESTILO §5). */
@StringRes
fun ValidationError.message(): Int = when (this) {
    ValidationError.NAME_REQUIRED -> R.string.error_name_required
    ValidationError.NAME_DUPLICATED -> R.string.error_name_duplicated
    ValidationError.UNIT_REQUIRED -> R.string.error_unit_required
    ValidationError.TARGET_HOURS_NOT_POSITIVE -> R.string.error_target_hours
    ValidationError.MILESTONE_NOT_POSITIVE -> R.string.error_milestones
    ValidationError.STAGE_DURATION_REQUIRED -> R.string.error_stage_duration
    ValidationError.SCHEDULE_STAGE_NOT_USED -> R.string.error_schedule_stage
    ValidationError.RANGE_MIN_NOT_BELOW_MAX -> R.string.error_range
    ValidationError.CLOCK_START_BEFORE_LAST_MEAL -> R.string.error_clock_before_meal
    ValidationError.TEMPLATE_NOT_FOUND -> R.string.error_not_found
    ValidationError.FAST_NOT_FOUND -> R.string.error_not_found
    ValidationError.FAST_NOT_PLANNED -> R.string.error_fast_not_planned
    ValidationError.FAST_NOT_IN_PROGRESS -> R.string.error_fast_not_in_progress
    ValidationError.ANOTHER_FAST_IN_PROGRESS -> R.string.error_another_in_progress
    ValidationError.FAST_NOT_STARTED -> R.string.error_fast_not_started
    ValidationError.REASON_REQUIRED -> R.string.error_reason_required
    ValidationError.END_BEFORE_START -> R.string.error_end_before_start
    ValidationError.END_IN_FUTURE -> R.string.error_end_in_future
    ValidationError.END_RULE_REQUIRED -> R.string.error_end_rule
    ValidationError.END_DETAIL_REQUIRED -> R.string.error_end_detail
    ValidationError.METRIC_NOT_FOUND -> R.string.error_not_found
    ValidationError.METRIC_NOT_ACTIVE -> R.string.error_metric_inactive
    ValidationError.VALUE_REQUIRED -> R.string.error_value_required
    ValidationError.VALUE_AND_NOT_DONE -> R.string.error_value_and_not_done
    ValidationError.UNIT_NOT_ALLOWED -> R.string.error_unit_not_allowed
    ValidationError.SOURCE_REQUIRED -> R.string.error_source_required
    ValidationError.DIASTOLIC_NOT_BELOW_SYSTOLIC -> R.string.error_diastolic
    ValidationError.PAIRED_READING_INVALID -> R.string.error_paired
    ValidationError.RULE_METRIC_REQUIRED -> R.string.error_rule_metric
    ValidationError.RULE_THRESHOLD_REQUIRED -> R.string.error_rule_threshold
    ValidationError.RULE_BP_COMPONENT_REQUIRED -> R.string.error_rule_bp_component
    ValidationError.RULE_SUSTAINED_NOT_POSITIVE -> R.string.error_rule_sustained
    ValidationError.RULE_MESSAGE_REQUIRED -> R.string.error_rule_message
    ValidationError.RULE_CONDITION_UNSUPPORTED -> R.string.error_rule_unsupported
    ValidationError.RULE_ACTION_UNSUPPORTED -> R.string.error_rule_unsupported
    ValidationError.ALERT_NOT_FOUND -> R.string.error_not_found
    ValidationError.ALERT_ALREADY_DECIDED -> R.string.error_alert_decided
}
