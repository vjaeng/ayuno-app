package com.ayuno.app.ui.planned

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.InfoBanner
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.plan.GoalDraft
import com.ayuno.app.ui.plan.GoalsSection
import com.ayuno.app.ui.plan.RuleEditor
import com.ayuno.domain.derived.PreStartReview
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.StartWarning
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.model.WarningKind
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReviewState {
    data object Loading : ReviewState
    data object Error : ReviewState

    /** [warnings]: derivado «Revisión previa» sobre el plan del ayuno, recalculado al volver de «Arreglar». */
    data class Loaded(val source: FastWithPlan, val warnings: List<StartWarning>, val anotherInProgress: Boolean) : ReviewState
}

/** H-057: lo que le falta al plan del ayuno antes de empezar; se arregla aquí o se empieza igual. */
class PreStartReviewViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    private val _state = MutableStateFlow<ReviewState>(ReviewState.Loading)
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            val source = container.fasts.get(fastId)?.takeIf { it.fast.status == FastStatus.PLANNED }
            _state.value = if (source == null) {
                ReviewState.Error
            } else {
                ReviewState.Loaded(
                    source,
                    PreStartReview.of(source.plan, container.catalog.intakeTypes()),
                    container.fasts.all().any { it.status == FastStatus.IN_PROGRESS },
                )
            }
        }
    }

    fun setChecklistItem(itemId: String, checked: Boolean) {
        viewModelScope.launch { container.setChecklistItem(fastId, itemId, checked); reload() }
    }

    /** La revisión se vuelve a calcular dentro de la transacción de empezar; lo que quede se guarda (D-25). */
    fun start(anyway: Boolean, onStarted: () -> Unit) {
        if (_starting.value) return
        _starting.value = true
        viewModelScope.launch {
            when (val result = container.startFastAskingWeight(fastId, anyway)) {
                is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule(); onStarted() }
                is Outcome.Invalid -> { _starting.value = false; _errors.value = result.errors; reload() }
            }
        }
    }
}

@Composable
fun PreStartReviewScreen(
    fastId: String,
    onBack: () -> Unit,
    onFixRule: (ruleId: String) -> Unit,
    onFixGoals: () -> Unit,
    onStarted: () -> Unit,
) {
    val viewModel: PreStartReviewViewModel = viewModel(key = "review-$fastId", factory = containerFactory { PreStartReviewViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val starting by viewModel.starting.collectAsStateWithLifecycle()
    // Al volver de «Arreglar», la lista se recalcula.
    LaunchedEffect(Unit) { viewModel.reload() }

    ScreenScaffold(stringResource(R.string.review_title), onBack) {
        when (val s = state) {
            ReviewState.Loading -> LoadingState()
            ReviewState.Error -> {
                ErrorState(stringResource(R.string.error_load)) { viewModel.reload() }
                SecondaryButton(stringResource(R.string.action_back), onBack)
            }
            is ReviewState.Loaded -> {
                val rules = s.warnings.filter { it.kind == WarningKind.PLACEHOLDER_RULE }
                val goals = s.warnings.filter { it.kind == WarningKind.GOAL_WITHOUT_AMOUNT }
                val items = s.warnings.filter { it.kind == WarningKind.UNCHECKED_ITEM }
                if (s.warnings.isNotEmpty()) Text(stringResource(R.string.review_intro), style = MaterialTheme.typography.bodyLarge)
                if (rules.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.review_rules, rules.size))
                    rules.forEach { w ->
                        val active = s.source.plan.rules.firstOrNull { it.id == w.refId }?.active == true
                        ListRow(
                            w.text,
                            stringResource(R.string.badge_placeholder) + " · " + stringResource(if (active) R.string.review_rule_active else R.string.review_rule_inactive),
                            onClick = { onFixRule(w.refId) },
                            trailing = { FixHint() },
                        )
                    }
                }
                if (goals.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.review_goals, goals.size))
                    goals.forEach { w -> ListRow(w.text, stringResource(R.string.badge_no_amount), onClick = onFixGoals, trailing = { FixHint() }) }
                }
                if (items.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.review_checklist, items.size))
                    items.forEach { w -> CheckRow(w.text, false) { viewModel.setChecklistItem(w.refId, it) } }
                }
                errors.filterNot { it == ValidationError.PRE_START_REVIEW_PENDING }.forEach { FieldError(stringResource(it.message())) }
                SecondaryButton(stringResource(R.string.action_back), onBack, enabled = !starting)
                if (s.anotherInProgress) {
                    InfoBanner(stringResource(R.string.planned_other_in_progress))
                } else {
                    val anyway = s.warnings.isNotEmpty()
                    PrimaryButton(
                        stringResource(if (anyway) R.string.review_start_anyway else R.string.home_start_now),
                        onClick = { viewModel.start(anyway, onStarted) },
                        enabled = !starting,
                    )
                }
            }
        }
    }
}

@Composable
private fun FixHint() {
    Text(stringResource(R.string.review_fix), style = MaterialTheme.typography.bodyLarge)
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
}

sealed interface FastPlanState {
    data object Loading : FastPlanState
    data object Error : FastPlanState
    data class Loaded(
        val source: FastWithPlan,
        val types: List<MetricType>,
        val symptoms: List<SymptomType>,
        val intakes: List<IntakeType>,
    ) : FastPlanState
}

/** H-057 («Arreglar ›»): reglas y metas de la copia del plan de un ayuno planificado; la plantilla no cambia (D-03). */
class FastPlanViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    private val _state = MutableStateFlow<FastPlanState>(FastPlanState.Loading)
    val state: StateFlow<FastPlanState> = _state.asStateFlow()

    private val _goals = MutableStateFlow<Map<String, GoalDraft>>(emptyMap())
    val goals: StateFlow<Map<String, GoalDraft>> = _goals.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    init {
        viewModelScope.launch {
            val source = container.fasts.get(fastId)?.takeIf { it.fast.status == FastStatus.PLANNED }
            _state.value = if (source == null) {
                FastPlanState.Error
            } else {
                _goals.value = source.plan.goals.associate { it.intakeTypeCode to GoalDraft.of(it) }
                FastPlanState.Loaded(source, container.catalog.metricTypes(), container.catalog.symptomTypes(), container.catalog.intakeTypes())
            }
        }
    }

    fun setGoal(code: String, goal: GoalDraft?) { _goals.value = if (goal == null) _goals.value - code else _goals.value + (code to goal) }

    /** Guarda una regla cambiada en el plan del ayuno; devuelve los errores (vacío si se guardó). */
    suspend fun saveRule(rule: com.ayuno.domain.model.Rule): List<ValidationError> {
        val source = (_state.value as? FastPlanState.Loaded)?.source ?: return listOf(ValidationError.FAST_NOT_FOUND)
        val rules = source.plan.rules.map { if (it.id == rule.id) rule else it }
        return when (val result = container.saveFastPlan(fastId, rules, source.plan.goals)) {
            is Outcome.Ok -> emptyList()
            is Outcome.Invalid -> result.errors
        }
    }

    fun saveGoals(onDone: () -> Unit) {
        val source = (_state.value as? FastPlanState.Loaded)?.source ?: return
        viewModelScope.launch {
            when (val result = container.saveFastPlan(fastId, source.plan.rules, _goals.value.map { (code, g) -> g.toGoal(code) })) {
                is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule(); onDone() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}

/** Editor del plan de un ayuno: [ruleId] abre esa regla; sin él, «Líquidos y metas». Al guardar vuelve a la revisión. */
@Composable
fun FastPlanScreen(fastId: String, ruleId: String?, onBack: () -> Unit) {
    val viewModel: FastPlanViewModel = viewModel(key = "fast-plan-$fastId-$ruleId", factory = containerFactory { FastPlanViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val s = state
    val rule = (s as? FastPlanState.Loaded)?.source?.plan?.rules?.firstOrNull { it.id == ruleId }
    if (s is FastPlanState.Loaded && rule != null) {
        // El editor de reglas valida en el momento; el guardado real (con «de ejemplo») lo decide el caso de uso.
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val pending = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<ValidationError>>(emptyList()) }
        RuleEditor(
            rule, s.types, s.symptoms, s.intakes, onCancel = onBack,
            onSave = { edited ->
                val local = com.ayuno.domain.usecase.RuleValidator.validate(edited)
                if (local.isEmpty()) scope.launch { viewModel.saveRule(edited).let { if (it.isEmpty()) onBack() else pending.value = it } }
                local + pending.value
            },
            goals = s.source.plan.goals,
            onFastPlan = true,
        )
        return
    }
    ScreenScaffold(stringResource(R.string.template_goals), onBack) {
        when (s) {
            FastPlanState.Loading -> LoadingState()
            FastPlanState.Error -> ErrorState(stringResource(R.string.planned_gone), onBack)
            is FastPlanState.Loaded -> {
                InfoBanner(stringResource(R.string.plan_fast_banner))
                GoalsSection(s.intakes, goals, viewModel::setGoal)
                errors.forEach { FieldError(stringResource(it.message())) }
                PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.saveGoals(onBack) })
            }
        }
    }
}
