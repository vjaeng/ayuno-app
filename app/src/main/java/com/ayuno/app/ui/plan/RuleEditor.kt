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
import com.ayuno.app.ui.components.TimeField
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.safety.RuleSentences
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.usecase.ValidationError
import java.time.LocalTime

object RuleEditorDefaults {
    /** Regla nueva: sin valores sugeridos de umbral (la app no trae umbrales clínicos, H-019). */
    fun newRule() = Rule(
        id = Ids.newId(), name = "", active = true, conditionKind = RuleConditionKind.METRIC_BELOW, metricTypeCode = null,
        bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null, intakeTypeCode = null, checkTime = null,
        filterStageKinds = emptyList(), filterOnWaking = false, filterPosition = null, level = RuleLevel.ALERT, message = "",
        action = RuleAction.NONE, actionMinutes = null,
    )
}

private enum class ConditionGroup { METRIC, SYMPTOM, INTAKE, MANUAL }

private fun RuleConditionKind.group() = when (this) {
    RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE -> ConditionGroup.METRIC
    RuleConditionKind.ALARM_SYMPTOM -> ConditionGroup.SYMPTOM
    RuleConditionKind.INTAKE_BELOW_GOAL -> ConditionGroup.INTAKE
    RuleConditionKind.MANUAL -> ConditionGroup.MANUAL
}

/**
 * Constructor «Si … entonces …» con vista previa (H-019 a H-023, nivel y mensaje de H-025, acción de H-026 parcial).
 * Los campos que no aplican al tipo de condición se ocultan.
 */
@Composable
fun RuleEditor(
    initial: Rule,
    types: List<MetricType>,
    symptoms: List<SymptomType>,
    intakes: List<IntakeType>,
    onCancel: () -> Unit,
    onSave: (Rule) -> List<ValidationError>,
) {
    var rule by remember { mutableStateOf(initial) }
    var threshold by remember { mutableStateOf(initial.threshold?.let { Formats.number(it, 2) }.orEmpty()) }
    var sustainedOn by remember { mutableStateOf(initial.sustainedMinutes != null) }
    var sustained by remember { mutableStateOf(initial.sustainedMinutes?.toString().orEmpty()) }
    var errors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    val context = LocalContext.current
    val group = rule.conditionKind.group()

    fun current(): Rule = when (group) {
        ConditionGroup.METRIC -> rule.copy(
            threshold = Formats.parseNumber(threshold),
            sustainedMinutes = if (sustainedOn) sustained.trim().toIntOrNull() ?: 0 else null,
            symptomTypeCode = null, intakeTypeCode = null, checkTime = null,
        )
        ConditionGroup.SYMPTOM -> rule.copy(metricTypeCode = null, bpComponent = null, threshold = null, sustainedMinutes = null, intakeTypeCode = null, checkTime = null)
        ConditionGroup.INTAKE -> rule.copy(metricTypeCode = null, bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null)
        ConditionGroup.MANUAL -> rule.copy(metricTypeCode = null, bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null, intakeTypeCode = null, checkTime = null)
    }

    ScreenScaffold(stringResource(R.string.rule_title), onCancel) {
        AyunoTextField(stringResource(R.string.field_name), rule.name, { rule = rule.copy(name = it) })

        SectionTitle(stringResource(R.string.rule_if))
        RadioList(
            listOf(
                ConditionGroup.METRIC to stringResource(R.string.rule_kind_metric),
                ConditionGroup.SYMPTOM to stringResource(R.string.rule_kind_symptom),
                ConditionGroup.INTAKE to stringResource(R.string.rule_kind_intake),
                ConditionGroup.MANUAL to stringResource(R.string.rule_kind_manual),
            ),
            group,
            { g ->
                rule = rule.copy(
                    conditionKind = when (g) {
                        ConditionGroup.METRIC -> RuleConditionKind.METRIC_BELOW
                        ConditionGroup.SYMPTOM -> RuleConditionKind.ALARM_SYMPTOM
                        ConditionGroup.INTAKE -> RuleConditionKind.INTAKE_BELOW_GOAL
                        ConditionGroup.MANUAL -> RuleConditionKind.MANUAL
                    },
                    checkTime = if (g == ConditionGroup.INTAKE) rule.checkTime ?: LocalTime.of(14, 0) else rule.checkTime,
                )
            },
        )
        when (group) {
            ConditionGroup.METRIC -> {
                RadioList(types.filter { it.active && it.isNumeric }.map { it.code to it.name }, rule.metricTypeCode, { code ->
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
                AyunoTextField(stringResource(R.string.field_threshold), threshold, { threshold = it }, numeric = true, suffix = types.firstOrNull { it.code == rule.metricTypeCode }?.unit)
                CheckRow(stringResource(R.string.rule_sustained), sustainedOn) { sustainedOn = it }
                if (sustainedOn) AyunoTextField(stringResource(R.string.field_sustained_minutes), sustained, { sustained = it }, numeric = true, suffix = stringResource(R.string.unit_minutes))
            }
            ConditionGroup.SYMPTOM -> RadioList(
                listOf<Pair<String?, String>>(null to stringResource(R.string.rule_any_alarm_symptom)) +
                    symptoms.filter { it.isAlarm && it.active }.map { it.code to it.name },
                rule.symptomTypeCode,
                { rule = rule.copy(symptomTypeCode = it) },
            )
            ConditionGroup.INTAKE -> {
                RadioList(intakes.map { it.code to it.name }, rule.intakeTypeCode, { rule = rule.copy(intakeTypeCode = it) })
                TimeField(stringResource(R.string.field_check_time), rule.checkTime ?: LocalTime.of(14, 0)) { rule = rule.copy(checkTime = it) }
                Text(stringResource(R.string.rule_intake_needs_goal), style = MaterialTheme.typography.bodyMedium)
            }
            ConditionGroup.MANUAL -> Text(stringResource(R.string.rule_manual_hint), style = MaterialTheme.typography.bodyLarge)
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
        Text(RuleSentences.describe(context, current(), types, symptoms, intakes), style = MaterialTheme.typography.bodyLarge)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_accept), onClick = {
            if (group == ConditionGroup.INTAKE && rule.checkTime == null) rule = rule.copy(checkTime = LocalTime.of(14, 0))
            errors = onSave(current())
        })
    }
}
