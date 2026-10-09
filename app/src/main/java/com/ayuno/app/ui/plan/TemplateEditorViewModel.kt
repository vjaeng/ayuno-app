package com.ayuno.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.StageNotice
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RuleValidator
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Borrador editable de una plantilla (H-001) con sus reglas (H-019, H-020). */
data class TemplateDraft(
    val id: String,
    val name: String = "",
    val targetHours: String = "",
    val milestones: String = "12, 24, 36, 48",
    val stageDurations: Map<StageKind, String?> = emptyMap(),
    val stageIds: Map<StageKind, String> = emptyMap(),
    val metrics: Map<String, PlanMetric> = emptyMap(),
    val rules: List<Rule> = emptyList(),
    /** Meta por tipo de ingesta: id, cantidad tecleada y unidad. */
    val goals: Map<String, Triple<String, String, String>> = emptyMap(),
    val checklist: List<ChecklistItem> = emptyList(),
    val notices: List<StageNotice> = emptyList(),
    val products: Map<String, PlanProduct> = emptyMap(),
) {
    fun usesStage(kind: StageKind) = kind == StageKind.FAST || stageDurations.containsKey(kind)
}

class TemplateEditorViewModel(private val container: AppContainer, templateId: String?) : ViewModel() {
    private val _draft = MutableStateFlow<TemplateDraft?>(null)
    val draft: StateFlow<TemplateDraft?> = _draft.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    val metricTypes: StateFlow<List<MetricType>> = container.catalog.observeMetricTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val symptomTypes: StateFlow<List<SymptomType>> = container.catalog.observeSymptomTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val intakeTypes: StateFlow<List<IntakeType>> = container.catalog.observeIntakeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products: StateFlow<List<Product>> = container.catalog.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val existing = templateId?.let { container.templates.get(it) }
            _draft.value = existing?.let(::fromTemplate) ?: TemplateDraft(id = Ids.newId())
        }
    }

    private fun fromTemplate(t: TemplateWithPlan) = TemplateDraft(
        id = t.template.id,
        name = t.template.name,
        targetHours = Formats.number(t.template.targetHours),
        milestones = t.template.milestoneHours.joinToString(", ") { Formats.number(it) },
        stageDurations = t.plan.stages.filter { it.kind != StageKind.FAST }.associate { it.kind to it.durationValue?.let { d -> Formats.number(d) } },
        stageIds = t.plan.stages.associate { it.kind to it.id },
        metrics = t.plan.metrics.associateBy { it.metricTypeCode },
        rules = t.plan.rules,
        goals = t.plan.goals.associate { it.intakeTypeCode to Triple(it.id, Formats.number(it.dailyAmount, 2), it.unit) },
        checklist = t.plan.checklist,
        notices = t.plan.notices,
        products = t.plan.products.associateBy { it.productCode },
    )

    fun edit(change: (TemplateDraft) -> TemplateDraft) = _draft.update { it?.let(change) }

    fun toggleStage(kind: StageKind, on: Boolean) = edit { d ->
        d.copy(stageDurations = if (on) d.stageDurations + (kind to d.stageDurations[kind]) else d.stageDurations - kind)
    }

    fun toggleMetric(code: String, on: Boolean) = edit { d ->
        d.copy(metrics = if (on) d.metrics + (code to (d.metrics[code] ?: PlanMetric(Ids.newId(), code, emptyList()))) else d.metrics - code)
    }

    fun addSchedule(code: String, stage: StageKind, time: LocalTime) = edit { d ->
        val metric = d.metrics[code] ?: return@edit d
        if (metric.schedules.any { it.stageKind == stage && it.timeOfDay == time }) return@edit d
        d.copy(metrics = d.metrics + (code to metric.copy(schedules = (metric.schedules + MetricSchedule(Ids.newId(), stage, time)).sortedBy { it.timeOfDay })))
    }

    fun removeSchedule(code: String, scheduleId: String) = edit { d ->
        val metric = d.metrics[code] ?: return@edit d
        d.copy(metrics = d.metrics + (code to metric.copy(schedules = metric.schedules.filterNot { it.id == scheduleId })))
    }

    /** Valida y guarda una regla en el borrador; devuelve los errores (vacío si se aceptó). */
    fun putRule(rule: Rule): List<ValidationError> {
        val errors = RuleValidator.validate(rule)
        if (errors.isEmpty()) edit { d -> d.copy(rules = d.rules.filterNot { it.id == rule.id } + rule) }
        return errors
    }

    fun setGoal(intakeCode: String, amount: String?, unit: String) = edit { d ->
        d.copy(goals = if (amount == null) d.goals - intakeCode else d.goals + (intakeCode to Triple(d.goals[intakeCode]?.first ?: Ids.newId(), amount, unit)))
    }

    fun addChecklistItem(text: String) = edit { d -> if (text.isBlank()) d else d.copy(checklist = d.checklist + ChecklistItem(Ids.newId(), text.trim(), false)) }
    fun removeChecklistItem(id: String) = edit { d -> d.copy(checklist = d.checklist.filterNot { it.id == id }) }

    fun addNotice(text: String, stages: List<StageKind>) = edit { d -> d.copy(notices = d.notices + StageNotice(Ids.newId(), text.trim(), stages)) }
    fun removeNotice(id: String) = edit { d -> d.copy(notices = d.notices.filterNot { it.id == id }) }

    fun setProduct(code: String, duringFast: DuringFast?, optionalEnabled: Boolean = false, reason: String? = null) = edit { d ->
        if (duringFast == null) return@edit d.copy(products = d.products - code)
        val current = d.products[code]
        d.copy(products = d.products + (code to PlanProduct(current?.id ?: Ids.newId(), code, duringFast, optionalEnabled, reason ?: current?.reason)))
    }

    fun setRuleActive(ruleId: String, active: Boolean) = edit { d -> d.copy(rules = d.rules.map { if (it.id == ruleId) it.copy(active = active) else it }) }

    fun save(onDone: () -> Unit) {
        val d = _draft.value ?: return
        val milestones = d.milestones.split(',', ';', ' ').filter { it.isNotBlank() }.map { Formats.parseNumber(it) ?: -1.0 }
        val stages = d.stageDurations.map { (kind, duration) ->
            Stage(
                id = d.stageIds[kind] ?: Ids.newId(),
                kind = kind,
                durationValue = duration?.let { Formats.parseNumber(it) },
                durationUnit = if (kind == StageKind.REFEED) DurationUnit.HOURS else DurationUnit.DAYS,
            )
        }
        val template = TemplateWithPlan(
            Template(d.id, d.name, Formats.parseNumber(d.targetHours) ?: 0.0, milestones),
            Plan(
                stages = stages,
                metrics = d.metrics.values.toList(),
                rules = d.rules,
                goals = d.goals.map { (code, g) -> IntakeGoal(g.first, code, Formats.parseNumber(g.second) ?: 0.0, g.third) },
                checklist = d.checklist,
                notices = d.notices,
                products = d.products.values.toList(),
            ),
        )
        viewModelScope.launch {
            when (val result = container.saveTemplate(template)) {
                is Outcome.Ok -> { _errors.value = emptyList(); onDone() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}
