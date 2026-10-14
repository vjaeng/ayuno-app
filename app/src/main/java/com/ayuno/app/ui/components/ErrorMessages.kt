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
    ValidationError.FAST_TOO_CLOSE -> R.string.error_fast_too_close
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
    ValidationError.WEIGHT_OUT_OF_RANGE_UNCONFIRMED -> R.string.error_weight_out_of_range
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
    ValidationError.SEVERITY_OUT_OF_RANGE -> R.string.error_severity
    ValidationError.AMOUNT_NOT_POSITIVE -> R.string.error_amount
    ValidationError.CODE_INVALID -> R.string.error_code_invalid
    ValidationError.CODE_DUPLICATED -> R.string.error_code_duplicated
    ValidationError.GOAL_NOT_POSITIVE -> R.string.error_goal
    ValidationError.NOTICE_STAGES_REQUIRED -> R.string.error_notice_stages
    ValidationError.TEXT_REQUIRED -> R.string.error_text_required
    ValidationError.RULE_INTAKE_REQUIRED -> R.string.error_rule_intake
    ValidationError.RULE_CHECK_TIME_REQUIRED -> R.string.error_rule_check_time
    ValidationError.RULE_MANUAL_DUPLICATED -> R.string.error_rule_manual_duplicated
    ValidationError.PRODUCT_NOT_FOUND -> R.string.error_not_found
    ValidationError.SYMPTOM_NOT_FOUND -> R.string.error_not_found
    ValidationError.INTAKE_TYPE_NOT_FOUND -> R.string.error_not_found
    ValidationError.TAKEN_AT_REQUIRED -> R.string.error_taken_at
    ValidationError.SODIUM_FACTOR_NOT_POSITIVE -> R.string.error_sodium_factor
    ValidationError.PLANNED_END_NOT_AFTER_START -> R.string.error_planned_end
    ValidationError.QUIET_END_REQUIRED -> R.string.error_quiet_end
    ValidationError.RULE_ACTION_MINUTES_REQUIRED -> R.string.error_rule_action_minutes
    ValidationError.RULE_ACTION_NEEDS_METRIC -> R.string.error_rule_action_metric
    ValidationError.REFEED_STAGE_NOT_USED -> R.string.error_refeed_stage
    ValidationError.REFEED_OFFSET_REQUIRED -> R.string.error_refeed_offset
    ValidationError.REFEED_REPEAT_FOR_REQUIRED -> R.string.error_refeed_repeat_for
    ValidationError.REFEED_DAY_REQUIRED -> R.string.error_refeed_day
    ValidationError.REFEED_CHECK_NOT_FOUND -> R.string.error_refeed_check
    ValidationError.TOLERANCE_DETAIL_REQUIRED -> R.string.error_tolerance_detail
    ValidationError.TOLERANCE_REQUIRED -> R.string.error_tolerance_required
    ValidationError.NOTHING_TO_EXPORT -> R.string.data_nothing_to_export
}
