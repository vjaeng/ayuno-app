package com.ayuno.app.ui.planned

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.EmptyState
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
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.history.DeleteFastDialog
import com.ayuno.app.ui.history.FastMenu
import com.ayuno.app.ui.history.deleteFastAndReminders
import com.ayuno.app.ui.history.recordCount
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Un ayuno planificado de la lista con el nombre de su plantilla (nulo si desde cero o borrada). */
data class PlannedItem(val fast: Fast, val templateName: String?, val templateDeleted: Boolean)

sealed interface PlannedListState {
    data object Loading : PlannedListState
    data object Error : PlannedListState
    data class Loaded(val items: List<PlannedItem>) : PlannedListState
}

/** H-046: los ayunos planificados, del más próximo al más lejano. */
class PlannedListViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<PlannedListState> = combine(container.fasts.observeFasts(), container.templates.observeTemplates()) { fasts, templates ->
        val names = templates.associate { it.id to it.name }
        PlannedListState.Loaded(
            fasts.filter { it.status == FastStatus.PLANNED }.sortedBy { it.lastMealPlanned }.map { fast ->
                val name = fast.templateId?.let(names::get)
                PlannedItem(fast, name, fast.templateId != null && name == null)
            },
        ) as PlannedListState
    }
        .catch { emit(PlannedListState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannedListState.Loading)
}

@Composable
fun PlannedListScreen(onOpen: (fastId: String) -> Unit, onNew: () -> Unit) {
    val viewModel: PlannedListViewModel = viewModel(factory = containerFactory { PlannedListViewModel(it) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.tab_planned), onBack = null) {
        when (val s = state) {
            PlannedListState.Loading -> LoadingState()
            PlannedListState.Error -> ErrorState(stringResource(R.string.error_load)) { }
            is PlannedListState.Loaded -> {
                if (s.items.isEmpty()) EmptyState(stringResource(R.string.planned_empty))
                s.items.forEach { item ->
                    ListRow(
                        Formats.dateTime(item.fast.lastMealPlanned),
                        plannedSupporting(item),
                        onClick = { onOpen(item.fast.id) },
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    )
                }
                PrimaryButton(stringResource(R.string.planned_new), onNew)
            }
        }
    }
}

@Composable
private fun plannedSupporting(item: PlannedItem): String {
    val template = when {
        item.templateName != null -> item.templateName
        item.templateDeleted -> stringResource(R.string.planned_template_deleted)
        else -> stringResource(R.string.planned_from_scratch)
    }
    return stringResource(R.string.history_target, Formats.number(item.fast.targetHours, 2)) + " · " + template
}

/** Detalle de un planificado: [source] nulo si ya no está planificado (empezó o se borró). */
data class PlannedDetail(val source: FastWithPlan?, val anotherInProgress: Boolean, val recordCount: Int)

/** H-046: detalle de un ayuno planificado con lo que hace falta para prepararlo y empezarlo (H-005, H-044, H-045). */
class PlannedDetailViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    /** La lista previa vive en otra tabla: se fuerza el recálculo tras cada acción. */
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<PlannedDetail?> = combine(container.fasts.observeFasts(), refresh) { fasts, _ -> fasts }
        .map { fasts ->
            val source = container.fasts.get(fastId)?.takeIf { it.fast.status == FastStatus.PLANNED }
            PlannedDetail(source, fasts.any { it.status == FastStatus.IN_PROGRESS }, if (source != null) container.recordCount(fastId) else 0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed: StateFlow<Boolean> = _deleteFailed.asStateFlow()

    /** Al empezar o borrar, el ayuno deja de estar planificado antes de salir: no se muestra «ya no está planificado». */
    private val _leaving = MutableStateFlow(false)
    val leaving: StateFlow<Boolean> = _leaving.asStateFlow()

    fun start(onStarted: () -> Unit) {
        _leaving.value = true
        execute(onStarted) { container.startFastAskingWeight(fastId) }
    }

    fun registerLastMeal(at: LocalDateTime) = execute { container.registerLastMeal(fastId, at) }

    fun setChecklistItem(itemId: String, checked: Boolean) = execute { container.setChecklistItem(fastId, itemId, checked) }

    fun delete(onDeleted: () -> Unit) {
        _leaving.value = true
        viewModelScope.launch {
            if (container.deleteFastAndReminders(fastId)) onDeleted() else { _leaving.value = false; _deleteFailed.value = true }
        }
    }

    private fun execute(onOk: () -> Unit = {}, block: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            when (val result = block()) {
                is Outcome.Ok -> { _errors.value = emptyList(); refresh.value++; container.reminders.reschedule(); onOk() }
                is Outcome.Invalid -> { _leaving.value = false; _errors.value = result.errors }
            }
        }
    }
}

@Composable
fun PlannedDetailScreen(fastId: String, onBack: () -> Unit, onChange: () -> Unit, onStarted: () -> Unit, onDeleted: () -> Unit = onBack) {
    val viewModel: PlannedDetailViewModel = viewModel(key = "planned-$fastId", factory = containerFactory { PlannedDetailViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val deleteFailed by viewModel.deleteFailed.collectAsStateWithLifecycle()
    val leaving by viewModel.leaving.collectAsStateWithLifecycle()
    var confirmingDelete by remember { mutableStateOf(false) }
    val source = state?.source
    ScreenScaffold(
        source?.let { stringResource(R.string.planned_detail_title, Formats.date(it.fast.lastMealPlanned)) } ?: stringResource(R.string.tab_planned),
        onBack,
        actions = { if (source != null) FastMenu(onChange = onChange, onDelete = { confirmingDelete = true }) },
    ) {
        val d = state?.takeUnless { leaving && it.source == null } ?: run { LoadingState(); return@ScreenScaffold }
        val fast = d.source?.fast ?: run { ErrorState(stringResource(R.string.planned_gone), onBack); return@ScreenScaffold }
        Text(
            fast.lastMealActual?.let { stringResource(R.string.home_last_meal_planned_vs_actual, Formats.dateTime(fast.lastMealPlanned), Formats.dateTime(it)) }
                ?: stringResource(R.string.home_last_meal_planned, Formats.dateTime(fast.lastMealPlanned)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(stringResource(R.string.home_clock_planned, Formats.dateTime(fast.clockStartPlanned)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_planned_end, Formats.dateTime(FastClock.plannedEnd(fast))), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_target, Formats.number(fast.targetHours, 2)), style = MaterialTheme.typography.bodyLarge)
        fast.notes?.let { Text(stringResource(R.string.home_notes, it), style = MaterialTheme.typography.bodyLarge) }
        val checklist = d.source.plan.checklist
        if (checklist.isNotEmpty()) {
            SectionTitle(stringResource(R.string.home_checklist, checklist.count { it.isChecked }, checklist.size))
            checklist.forEach { item -> CheckRow(item.text, item.isChecked) { viewModel.setChecklistItem(item.id, it) } }
        }
        LastMealButton(fast, viewModel)
        if (deleteFailed) FieldError(stringResource(R.string.error_delete_fast))
        errors.forEach { FieldError(stringResource(it.message())) }
        // H-046: con otro ayuno en curso no se ofrece empezar y se explica por qué.
        if (d.anotherInProgress) InfoBanner(stringResource(R.string.planned_other_in_progress)) else PrimaryButton(stringResource(R.string.home_start_now), onClick = { viewModel.start(onStarted) })
    }

    val d = state
    if (confirmingDelete && d?.source != null) {
        DeleteFastDialog(d.source.fast, d.recordCount, onDismiss = { confirmingDelete = false }, onConfirm = { confirmingDelete = false; viewModel.delete(onDeleted) })
    }
}

@Composable
private fun LastMealButton(fast: Fast, viewModel: PlannedDetailViewModel) {
    var editing by remember { mutableStateOf(false) }
    SecondaryButton(
        stringResource(if (fast.lastMealActual == null) R.string.home_register_last_meal else R.string.home_correct_last_meal),
        onClick = { editing = true },
    )
    if (editing) {
        var value by remember { mutableStateOf(fast.lastMealActual ?: fast.lastMealPlanned) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.home_last_meal_title)) },
            text = { DateTimeField(stringResource(R.string.field_last_meal_actual), value, { value = it }) },
            confirmButton = { TextButton(onClick = { editing = false; viewModel.registerLastMeal(value) }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
