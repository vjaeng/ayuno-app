package com.ayuno.app.ui.safety

import android.content.Context
import com.ayuno.app.R
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel

/** Una regla como frase completa en lenguaje llano (H-028): la misma en Seguridad y en la vista previa del editor. */
object RuleSentences {
    fun describe(context: Context, rule: Rule, types: List<MetricType>): String {
        val type = types.firstOrNull { it.code == rule.metricTypeCode }
        val subject = when {
            type?.isBloodPressure == true -> context.getString(
                when (rule.bpComponent) {
                    BpComponent.DIASTOLIC -> R.string.sentence_bp_diastolic
                    BpComponent.PULSE -> R.string.sentence_bp_pulse
                    else -> R.string.sentence_bp_systolic
                },
            ) + when (rule.filterPosition) {
                Position.STANDING -> context.getString(R.string.sentence_standing)
                Position.SITTING -> context.getString(R.string.sentence_sitting)
                Position.LYING -> context.getString(R.string.sentence_lying)
                null -> ""
            }
            type != null -> type.name.lowercase()
            else -> rule.metricTypeCode.orEmpty()
        }
        val unit = if (type?.isBloodPressure == true && rule.bpComponent == BpComponent.PULSE) "" else type?.unit.orEmpty()
        val threshold = rule.threshold?.let { Formats.number(it) }.orEmpty()
        val condition = when (rule.conditionKind) {
            RuleConditionKind.METRIC_BELOW -> context.getString(R.string.sentence_below, subject, threshold, unit)
            RuleConditionKind.METRIC_ABOVE -> context.getString(R.string.sentence_above, subject, threshold, unit)
            else -> rule.name
        }.trim()
        val sustained = rule.sustainedMinutes?.let { context.getString(R.string.sentence_sustained, context.resources.getQuantityString(R.plurals.minutes, it, it)) }.orEmpty()
        val waking = if (rule.filterOnWaking) context.getString(R.string.sentence_on_waking) else ""
        val level = context.getString(if (rule.level == RuleLevel.ALERT) R.string.sentence_level_alert else R.string.sentence_level_notice)
        val action = when (rule.action) {
            RuleAction.OFFER_END -> context.getString(R.string.sentence_offer_end)
            RuleAction.SCHEDULE_MEASUREMENT -> (rule.actionMinutes ?: 0).let { context.getString(R.string.sentence_schedule, context.resources.getQuantityString(R.plurals.minutes, it, it)) }
            RuleAction.NONE -> ""
        }
        return context.getString(R.string.sentence_rule, condition + sustained + waking, level + action)
    }
}
