package com.ayuno.app.ui.plan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.safety.RuleSentences
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.usecase.ValidationError

object RuleEditorDefaults {
    /** Regla nueva: sin valores sugeridos de umbral (la app no trae umbrales clínicos, H-019). */
    fun newRule() = Rule(
        id = Ids.newId(), name = "", active = true, conditionKind = RuleConditionKind.METRIC_BELOW, metricTypeCode = null,
        bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null, intakeTypeCode = null, checkTime = null,
        filterStageKinds = emptyList(), filterOnWaking = false, filterPosition = null, level = RuleLevel.ALERT, message = "",
        action = RuleAction.NONE, actionMinutes = null,
    )
}

/** H-019, H-020 (y nivel, mensaje y acción de la regla): «Si … durante … entonces …», con vista previa. */
@Composable
fun RuleEditor(initial: Rule, types: List<MetricType>, onCancel: () -> Unit, onSave: (Rule) -> List<ValidationError>) {
    var rule by remember { mutableStateOf(initial) }
    var threshold by remember { mutableStateOf(initial.threshold?.let { Formats.number(it, 2) }.orEmpty()) }
    var sustained by remember { mutableStateOf(initial.sustainedMinutes?.toString().orEmpty()) }
    var errors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    val context = LocalContext.current

    fun current() = rule.copy(
        threshold = Formats.parseNumber(threshold),
        sustainedMinutes = if (rule.sustainedMinutes != null || sustained.isNotBlank()) sustained.trim().toIntOrNull() ?: 0 else null,
    )

    ScreenScaffold(stringResource(R.string.rule_title), onCancel) {
        AyunoTextField(stringResource(R.string.field_name), rule.name, { rule = rule.copy(name = it) })

        SectionTitle(stringResource(R.string.rule_if))
        val numericTypes = types.filter { it.active && it.isNumeric }
        RadioList(numericTypes.map { it.code to it.name }, rule.metricTypeCode, { code ->
            rule = rule.copy(metricTypeCode = code, bpComponent = if (code == MetricType.BLOOD_PRESSURE_CODE) rule.bpComponent ?: BpComponent.SYSTOLIC else null)
        })
        if (rule.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE) {
            OptionSelector(
                listOf(BpComponent.SYSTOLIC to stringResource(R.string.field_systolic), BpComponent.DIASTOLIC to stringResource(R.string.field_diastolic), BpComponent.PULSE to stringResource(R.string.field_pulse)),
                rule.bpComponent,
                { rule = rule.copy(bpComponent = it) },
            )
        }
        OptionSelector(
            listOf(RuleConditionKind.METRIC_BELOW to stringResource(R.string.rule_below), RuleConditionKind.METRIC_ABOVE to stringResource(R.string.rule_above)),
            rule.conditionKind,
            { rule = rule.copy(conditionKind = it) },
        )
        val unit = types.firstOrNull { it.code == rule.metricTypeCode }?.unit
        AyunoTextField(stringResource(R.string.field_threshold), threshold, { threshold = it }, numeric = true, suffix = unit)
        CheckRow(stringResource(R.string.rule_sustained), rule.sustainedMinutes != null || sustained.isNotBlank()) { on ->
            if (on) { sustained = sustained.ifBlank { "" }; rule = rule.copy(sustainedMinutes = 0) } else { sustained = ""; rule = rule.copy(sustainedMinutes = null) }
        }
        if (rule.sustainedMinutes != null || sustained.isNotBlank()) {
            AyunoTextField(stringResource(R.string.field_sustained_minutes), sustained, { sustained = it }, numeric = true, suffix = stringResource(R.string.unit_minutes))
        }

        SectionTitle(stringResource(R.string.rule_then))
        OptionSelector(
            listOf(RuleLevel.NOTICE to stringResource(R.string.level_notice), RuleLevel.ALERT to stringResource(R.string.level_alert)),
            rule.level,
            { rule = rule.copy(level = it) },
        )
        AyunoTextField(stringResource(R.string.field_rule_message), rule.message, { rule = rule.copy(message = it) }, singleLine = false)
        RadioList(
            listOf(RuleAction.NONE to stringResource(R.string.action_none), RuleAction.OFFER_END to stringResource(R.string.action_offer_end)),
            rule.action,
            { rule = rule.copy(action = it) },
        )

        SectionTitle(stringResource(R.string.rule_preview))
        Text(RuleSentences.describe(context, current(), types), style = MaterialTheme.typography.bodyLarge)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_accept), onClick = { errors = onSave(current()) })
    }
}
