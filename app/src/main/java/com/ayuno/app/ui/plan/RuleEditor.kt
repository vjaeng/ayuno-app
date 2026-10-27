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
import com.ayuno.app.ui.components.Badge
import com.ayuno.app.ui.components.InfoBanner
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
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.TrendOf
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

private enum class ConditionGroup { METRIC, PAIR, BASELINE, TREND, SYMPTOM, INTAKE, MANUAL }

private fun RuleConditionKind.group() = when (this) {
    RuleConditionKind.METRIC_BELOW, RuleConditionKind.METRIC_ABOVE -> ConditionGroup.METRIC
    RuleConditionKind.PAIR_DIFFERENCE -> ConditionGroup.PAIR
    RuleConditionKind.CHANGE_FROM_BASELINE -> ConditionGroup.BASELINE
    RuleConditionKind.TREND -> ConditionGroup.TREND
    RuleConditionKind.ALARM_SYMPTOM -> ConditionGroup.SYMPTOM
    RuleConditionKind.INTAKE_BELOW_GOAL -> ConditionGroup.INTAKE
    RuleConditionKind.MANUAL -> ConditionGroup.MANUAL
}

/**
 * Constructor «Si … entonces …» con vista previa (H-019 a H-023, nivel y mensaje de H-025, acción de H-026 parcial;
 * condiciones entre lecturas de H-054 a H-056). Los campos que no aplican al tipo de condición se ocultan.
 * [onFastPlan]: se edita la copia del plan de un ayuno desde la revisión previa (H-057), no la plantilla.
 */
@Composable
fun RuleEditor(
    initial: Rule,
    types: List<MetricType>,
    symptoms: List<SymptomType>,
    intakes: List<IntakeType>,
    onCancel: () -> Unit,
    onSave: (Rule) -> List<ValidationError>,
    goals: List<IntakeGoal> = emptyList(),
    onFastPlan: Boolean = false,
) {
    var rule by remember { mutableStateOf(initial) }
    // La diferencia de pareja se teclea sin signo; la dirección decide el signo al guardar (H-054).
    var threshold by remember { mutableStateOf(initial.threshold?.let { Formats.number(kotlin.math.abs(it), 2) }.orEmpty()) }
    var sustainedOn by remember { mutableStateOf(initial.sustainedMinutes != null) }
    var sustained by remember { mutableStateOf(initial.sustainedMinutes?.toString().orEmpty()) }
    var trendCount by remember { mutableStateOf((initial.trendCount ?: Rule.MIN_TREND_COUNT).toString()) }
    var goalPercent by remember { mutableStateOf(initial.goalPercent?.let { Formats.number(it) }.orEmpty()) }
    var errors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    val context = LocalContext.current
    val group = rule.conditionKind.group()
    val isBp = rule.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE

    fun current(): Rule {
        val number = Formats.parseNumber(threshold)
        val none = rule.copy(
            metricTypeCode = null, bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null, intakeTypeCode = null,
            checkTime = null, direction = null, trendCount = null, trendOf = null, goalPercent = null,
        )
        return when (group) {
            ConditionGroup.METRIC -> none.copy(
                metricTypeCode = rule.metricTypeCode, bpComponent = rule.bpComponent, threshold = number,
                sustainedMinutes = if (sustainedOn) sustained.trim().toIntOrNull() ?: 0 else null,
            )
            ConditionGroup.PAIR -> none.copy(
                metricTypeCode = MetricType.BLOOD_PRESSURE_CODE, bpComponent = rule.bpComponent ?: BpComponent.SYSTOLIC, direction = rule.direction,
                threshold = number?.let { if (rule.direction == RuleDirection.BELOW) -kotlin.math.abs(it) else kotlin.math.abs(it) },
            )
            ConditionGroup.BASELINE -> none.copy(metricTypeCode = rule.metricTypeCode, bpComponent = rule.bpComponent, threshold = number, direction = rule.direction)
            ConditionGroup.TREND -> none.copy(
                metricTypeCode = rule.metricTypeCode, bpComponent = rule.bpComponent, direction = rule.direction,
                trendCount = trendCount.trim().toIntOrNull() ?: 0, trendOf = if (isBp) rule.trendOf ?: TrendOf.VALUE else TrendOf.VALUE,
            )
            ConditionGroup.SYMPTOM -> none.copy(symptomTypeCode = rule.symptomTypeCode)
            ConditionGroup.INTAKE -> none.copy(intakeTypeCode = rule.intakeTypeCode, checkTime = rule.checkTime, goalPercent = Formats.parseNumber(goalPercent))
            ConditionGroup.MANUAL -> none
        }.copy(filterPosition = rule.filterPosition.takeIf { isBp && group in setOf(ConditionGroup.METRIC, ConditionGroup.BASELINE, ConditionGroup.TREND) })
    }

    @Composable
    fun metricChoice() {
        RadioList(types.filter { it.active && it.isNumeric }.map { it.code to it.name }, rule.metricTypeCode, { code ->
            rule = rule.copy(metricTypeCode = code, bpComponent = if (code == MetricType.BLOOD_PRESSURE_CODE) rule.bpComponent ?: BpComponent.SYSTOLIC else null)
        })
    }

    @Composable
    fun componentChoice() {
        OptionSelector(
            listOf(BpComponent.PULSE to stringResource(R.string.rule_bp_pulse), BpComponent.SYSTOLIC to stringResource(R.string.rule_bp_systolic), BpComponent.DIASTOLIC to stringResource(R.string.rule_bp_diastolic)),
            rule.bpComponent,
            { rule = rule.copy(bpComponent = it) },
        )
    }

    @Composable
    fun positionChoice() {
        OptionSelector(
            listOf<Pair<Position?, String>>(null to stringResource(R.string.rule_position_any), Position.SITTING to stringResource(R.string.position_sitting), Position.STANDING to stringResource(R.string.position_standing)),
            rule.filterPosition,
            { rule = rule.copy(filterPosition = it) },
        )
    }

    ScreenScaffold(stringResource(R.string.rule_title), onCancel) {
        if (onFastPlan) InfoBanner(stringResource(R.string.plan_fast_banner))
        AyunoTextField(stringResource(R.string.field_name), rule.name, { rule = rule.copy(name = it) })
        if (rule.isPlaceholder) Badge(stringResource(R.string.badge_placeholder))

        SectionTitle(stringResource(R.string.rule_if))
        RadioList(
            listOf(
                ConditionGroup.METRIC to stringResource(R.string.rule_kind_metric),
                ConditionGroup.PAIR to stringResource(R.string.rule_kind_pair),
                ConditionGroup.BASELINE to stringResource(R.string.rule_kind_baseline),
                ConditionGroup.TREND to stringResource(R.string.rule_kind_trend),
                ConditionGroup.SYMPTOM to stringResource(R.string.rule_kind_symptom),
                ConditionGroup.INTAKE to stringResource(R.string.rule_kind_intake),
                ConditionGroup.MANUAL to stringResource(R.string.rule_kind_manual),
            ),
            group,
            { g ->
                rule = rule.copy(
                    conditionKind = when (g) {
                        ConditionGroup.METRIC -> RuleConditionKind.METRIC_BELOW
                        ConditionGroup.PAIR -> RuleConditionKind.PAIR_DIFFERENCE
                        ConditionGroup.BASELINE -> RuleConditionKind.CHANGE_FROM_BASELINE
                        ConditionGroup.TREND -> RuleConditionKind.TREND
                        ConditionGroup.SYMPTOM -> RuleConditionKind.ALARM_SYMPTOM
                        ConditionGroup.INTAKE -> RuleConditionKind.INTAKE_BELOW_GOAL
                        ConditionGroup.MANUAL -> RuleConditionKind.MANUAL
                    },
                    metricTypeCode = if (g == ConditionGroup.PAIR) MetricType.BLOOD_PRESSURE_CODE else rule.metricTypeCode,
                    bpComponent = if (g == ConditionGroup.PAIR) rule.bpComponent ?: BpComponent.SYSTOLIC else rule.bpComponent,
                    direction = if (g in setOf(ConditionGroup.PAIR, ConditionGroup.BASELINE, ConditionGroup.TREND)) rule.direction ?: RuleDirection.ABOVE else null,
                    checkTime = if (g == ConditionGroup.INTAKE) rule.checkTime ?: LocalTime.of(14, 0) else rule.checkTime,
                )
            },
        )
        when (group) {
            ConditionGroup.METRIC -> {
                metricChoice()
                if (isBp) { componentChoice(); positionChoice() }
                OptionSelector(
                    listOf(RuleConditionKind.METRIC_BELOW to stringResource(R.string.rule_below), RuleConditionKind.METRIC_ABOVE to stringResource(R.string.rule_above)),
                    rule.conditionKind,
                    { rule = rule.copy(conditionKind = it) },
                )
                AyunoTextField(stringResource(R.string.field_threshold), threshold, { threshold = it }, numeric = true, suffix = types.firstOrNull { it.code == rule.metricTypeCode }?.unit)
                CheckRow(stringResource(R.string.rule_sustained), sustainedOn) { sustainedOn = it }
                if (sustainedOn) AyunoTextField(stringResource(R.string.field_sustained_minutes), sustained, { sustained = it }, numeric = true, suffix = stringResource(R.string.unit_minutes))
            }
            ConditionGroup.PAIR -> {
                componentChoice()
                Text(stringResource(R.string.rule_pair_hint), style = MaterialTheme.typography.bodyLarge)
                OptionSelector(
                    listOf(RuleDirection.ABOVE to stringResource(R.string.rule_rises_more), RuleDirection.BELOW to stringResource(R.string.rule_falls_more)),
                    rule.direction,
                    { rule = rule.copy(direction = it) },
                )
                AyunoTextField(stringResource(R.string.field_difference), threshold, { threshold = it }, numeric = true)
            }
            ConditionGroup.BASELINE -> {
                metricChoice()
                if (isBp) { componentChoice(); positionChoice() }
                AyunoTextField(stringResource(R.string.field_baseline_distance), threshold, { threshold = it }, numeric = true, suffix = types.firstOrNull { it.code == rule.metricTypeCode }?.unit)
                OptionSelector(
                    listOf(RuleDirection.ABOVE to stringResource(R.string.rule_baseline_above), RuleDirection.BELOW to stringResource(R.string.rule_baseline_below)),
                    rule.direction,
                    { rule = rule.copy(direction = it) },
                )
                InfoBanner(stringResource(R.string.rule_baseline_info))
            }
            ConditionGroup.TREND -> {
                metricChoice()
                if (isBp) {
                    componentChoice()
                    OptionSelector(
                        listOf(TrendOf.VALUE to stringResource(R.string.rule_trend_value), TrendOf.PAIR_DIFFERENCE to stringResource(R.string.rule_trend_pair)),
                        rule.trendOf ?: TrendOf.VALUE,
                        { rule = rule.copy(trendOf = it) },
                    )
                    if (rule.trendOf != TrendOf.PAIR_DIFFERENCE) positionChoice()
                }
                AyunoTextField(stringResource(R.string.field_trend_count), trendCount, { trendCount = it }, numeric = true)
                OptionSelector(
                    listOf(RuleDirection.ABOVE to stringResource(R.string.rule_trend_up), RuleDirection.BELOW to stringResource(R.string.rule_trend_down)),
                    rule.direction,
                    { rule = rule.copy(direction = it) },
                )
            }
            ConditionGroup.SYMPTOM -> RadioList(
                listOf<Pair<String?, String>>(null to stringResource(R.string.rule_any_alarm_symptom)) +
                    symptoms.filter { it.isAlarm && it.active }.map { it.code to it.name },
                rule.symptomTypeCode,
                { rule = rule.copy(symptomTypeCode = it) },
            )
            ConditionGroup.INTAKE -> {
                RadioList(intakes.map { it.code to it.name }, rule.intakeTypeCode, { rule = rule.copy(intakeTypeCode = it) })
                AyunoTextField(stringResource(R.string.field_goal_percent), goalPercent, { goalPercent = it }, numeric = true, suffix = "%")
                TimeField(stringResource(R.string.field_check_time), rule.checkTime ?: LocalTime.of(14, 0)) { rule = rule.copy(checkTime = it) }
                Text(stringResource(R.string.rule_intake_needs_goal), style = MaterialTheme.typography.bodyMedium)
                if (goals.any { it.intakeTypeCode == rule.intakeTypeCode && it.dailyAmount == null }) InfoBanner(stringResource(R.string.rule_goal_without_amount))
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
        // «Medir de nuevo» solo con condiciones de medición: el recordatorio necesita saber qué medir (H-026).
        val actions = listOf(RuleAction.NONE to stringResource(R.string.action_none), RuleAction.OFFER_END to stringResource(R.string.action_offer_end)) +
            listOfNotNull((RuleAction.SCHEDULE_MEASUREMENT to stringResource(R.string.action_schedule_measurement)).takeIf { group in setOf(ConditionGroup.METRIC, ConditionGroup.PAIR, ConditionGroup.BASELINE, ConditionGroup.TREND) })
        RadioList(actions, rule.action, { rule = rule.copy(action = it, actionMinutes = if (it == RuleAction.SCHEDULE_MEASUREMENT) rule.actionMinutes ?: 15 else null) })
        if (rule.action == RuleAction.SCHEDULE_MEASUREMENT) {
            AyunoTextField(
                stringResource(R.string.field_action_minutes), rule.actionMinutes?.toString().orEmpty(),
                { rule = rule.copy(actionMinutes = it.trim().toIntOrNull()) }, numeric = true, suffix = stringResource(R.string.unit_minutes),
            )
        }

        SectionTitle(stringResource(R.string.rule_preview))
        Text(RuleSentences.describe(context, current(), types, symptoms, intakes), style = MaterialTheme.typography.bodyLarge)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_accept), onClick = {
            if (group == ConditionGroup.INTAKE && rule.checkTime == null) rule = rule.copy(checkTime = LocalTime.of(14, 0))
            errors = onSave(current())
        })
    }
}
