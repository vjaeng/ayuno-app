package com.ayuno.app.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.SwitchRow
import com.ayuno.app.ui.components.TimePickerDialog
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.fast.stageName
import com.ayuno.app.ui.fast.stageNameText
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.DuringFast
import com.ayuno.app.ui.components.OptionSelector
import java.time.LocalTime

/** H-001: editor de plantilla — nombre, objetivo, hitos, etapas, mediciones con horarios y reglas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateEditorScreen(templateId: String?, onBack: () -> Unit) {
    val viewModel: TemplateEditorViewModel = viewModel(key = "template-$templateId", factory = containerFactory { TemplateEditorViewModel(it, templateId) })
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val types by viewModel.metricTypes.collectAsStateWithLifecycle()
    val symptomTypes by viewModel.symptomTypes.collectAsStateWithLifecycle()
    val intakeTypes by viewModel.intakeTypes.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    var editingRule by remember { mutableStateOf<Rule?>(null) }
    var addingTime by remember { mutableStateOf<Pair<String, StageKind>?>(null) }

    editingRule?.let { rule ->
        BackHandler { editingRule = null }
        RuleEditor(rule, types, symptomTypes, intakeTypes, onCancel = { editingRule = null }, onSave = { viewModel.putRule(it).also { e -> if (e.isEmpty()) editingRule = null } })
        return
    }

    ScreenScaffold(stringResource(if (templateId == null) R.string.template_new else R.string.template_edit), onBack) {
        val d = draft ?: run { LoadingState(); return@ScreenScaffold }
        AyunoTextField(stringResource(R.string.field_name), d.name, { v -> viewModel.edit { it.copy(name = v) } })
        AyunoTextField(stringResource(R.string.field_target_hours), d.targetHours, { v -> viewModel.edit { it.copy(targetHours = v) } }, numeric = true, suffix = stringResource(R.string.unit_hours))
        AyunoTextField(stringResource(R.string.field_milestones), d.milestones, { v -> viewModel.edit { it.copy(milestones = v) } })

        SectionTitle(stringResource(R.string.template_stages))
        listOf(StageKind.PREPARATION, StageKind.REFEED, StageKind.POST).forEach { kind ->
            val on = d.stageDurations.containsKey(kind)
            SwitchRow(stageName(kind), on, { viewModel.toggleStage(kind, it) })
            if (on) {
                AyunoTextField(
                    stringResource(R.string.field_stage_duration),
                    d.stageDurations[kind].orEmpty(),
                    { v -> viewModel.edit { it.copy(stageDurations = it.stageDurations + (kind to v)) } },
                    numeric = true,
                    suffix = stringResource(if (kind == StageKind.REFEED) R.string.unit_hours else R.string.unit_days),
                )
            }
        }

        if (d.usesStage(StageKind.REFEED) || d.refeed.isNotEmpty()) {
            RefeedStepsSection(d.refeed, onAdd = viewModel::addRefeedStep, onRemove = viewModel::removeRefeedStep)
        }

        SectionTitle(stringResource(R.string.template_metrics))
        types.filter { it.active }.forEach { type ->
            val metric = d.metrics[type.code]
            CheckRow(type.name, metric != null) { viewModel.toggleMetric(type.code, it) }
            if (metric != null) {
                StageKind.entries.filter { d.usesStage(it) }.forEach { stage ->
                    Text(stringResource(R.string.template_times_in_stage, stageName(stage)), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                        metric.schedules.filter { it.stageKind == stage }.forEach { s ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeSchedule(type.code, s.id) },
                                label = { Text(Formats.time(s.timeOfDay)) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove)) },
                            )
                        }
                        TextButton(onClick = { addingTime = type.code to stage }) { Text(stringResource(R.string.template_add_time)) }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.template_products))
        if (products.none { it.active }) Text(stringResource(R.string.template_no_products), style = MaterialTheme.typography.bodyLarge)
        products.filter { it.active }.forEach { product ->
            val planned = d.products[product.code]
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            OptionSelector(
                listOf<Pair<DuringFast?, String>>(
                    null to stringResource(R.string.during_not_included),
                    DuringFast.CONTINUES to stringResource(R.string.during_continues),
                    DuringFast.STOPS to stringResource(R.string.during_stops),
                    DuringFast.OPTIONAL to stringResource(R.string.during_optional),
                ),
                planned?.duringFast,
                { viewModel.setProduct(product.code, it, planned?.optionalEnabled ?: false) },
            )
            if (planned?.duringFast == DuringFast.OPTIONAL) {
                SwitchRow(stringResource(R.string.template_optional_enabled), planned.optionalEnabled, { viewModel.setProduct(product.code, DuringFast.OPTIONAL, it) })
            }
            if (planned != null) {
                AyunoTextField(stringResource(R.string.field_reason_optional), planned.reason.orEmpty(), { viewModel.setProduct(product.code, planned.duringFast, planned.optionalEnabled, it) })
            }
        }

        SectionTitle(stringResource(R.string.template_goals))
        intakeTypes.forEach { type ->
            val goal = d.goals[type.code]
            CheckRow(type.name, goal != null) { on -> viewModel.setGoal(type.code, if (on) goal?.second ?: "" else null, type.unit) }
            if (goal != null) {
                AyunoTextField(stringResource(R.string.field_daily_goal), goal.second, { viewModel.setGoal(type.code, it, type.unit) }, numeric = true, suffix = type.unit)
            }
        }

        SectionTitle(stringResource(R.string.template_checklist))
        d.checklist.forEach { item ->
            ListRow(item.text, null, onClick = {}) {
                TextButton(onClick = { viewModel.removeChecklistItem(item.id) }) { Text(stringResource(R.string.action_remove)) }
            }
        }
        var newItem by remember { mutableStateOf("") }
        AyunoTextField(stringResource(R.string.field_checklist_item), newItem, { newItem = it })
        TextButton(onClick = { viewModel.addChecklistItem(newItem); newItem = "" }) { Text(stringResource(R.string.template_add_item)) }

        SectionTitle(stringResource(R.string.template_notices))
        d.notices.forEach { notice ->
            ListRow(notice.text, notice.stageKinds.map { stageNameText(it) }.joinToString(", "), onClick = {}) {
                TextButton(onClick = { viewModel.removeNotice(notice.id) }) { Text(stringResource(R.string.action_remove)) }
            }
        }
        var noticeText by remember { mutableStateOf("") }
        var noticeStages by remember { mutableStateOf(setOf(StageKind.FAST)) }
        AyunoTextField(stringResource(R.string.field_notice_text), noticeText, { noticeText = it }, singleLine = false)
        StageKind.entries.filter { d.usesStage(it) }.forEach { stage ->
            CheckRow(stageName(stage), stage in noticeStages) { on -> noticeStages = if (on) noticeStages + stage else noticeStages - stage }
        }
        TextButton(onClick = { viewModel.addNotice(noticeText, noticeStages.toList()); noticeText = "" }) { Text(stringResource(R.string.template_add_notice)) }

        SectionTitle(stringResource(R.string.template_rules))
        if (d.rules.isEmpty()) Text(stringResource(R.string.template_no_rules), style = MaterialTheme.typography.bodyLarge)
        d.rules.forEach { rule ->
            ListRow(
                title = rule.name,
                supporting = stringResource(if (rule.level == RuleLevel.ALERT) R.string.level_alert else R.string.level_notice),
                onClick = { editingRule = rule },
            ) { Switch(checked = rule.active, onCheckedChange = { viewModel.setRuleActive(rule.id, it) }) }
        }
        SecondaryButton(stringResource(R.string.template_add_rule), onClick = { editingRule = RuleEditorDefaults.newRule() })

        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.save(onBack) })
    }

    addingTime?.let { (code, stage) ->
        TimePickerDialog(LocalTime.of(7, 0), onDismiss = { addingTime = null }, onConfirm = { viewModel.addSchedule(code, stage, it); addingTime = null })
    }
}
