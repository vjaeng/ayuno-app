package com.ayuno.app.ui.fast

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.reminders.ReminderNotifier
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.ChangePlannedFast
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.derived.FastSpacing
import com.ayuno.domain.derived.SpacingConflict
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

/** Ayuno que se cambia (H-044): cargando, inexistente o listo. */
sealed interface EditTarget {
    data object None : EditTarget
    data object Loading : EditTarget
    data object NotFound : EditTarget
    data class Ready(val fast: Fast) : EditTarget
}

class CreateFastViewModel(private val container: AppContainer, private val fastId: String? = null) : ViewModel() {
    val templates: StateFlow<List<Template>> = container.templates.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selected = MutableStateFlow<TemplateWithPlan?>(null)
    val selected: StateFlow<TemplateWithPlan?> = _selected.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    /** D-17: con qué ayuno choca y desde o hasta cuándo se puede, para el mensaje de la semana de separación. */
    private val _tooClose = MutableStateFlow<SpacingConflict?>(null)
    val tooClose: StateFlow<SpacingConflict?> = _tooClose.asStateFlow()

    private val _target = MutableStateFlow<EditTarget>(if (fastId == null) EditTarget.None else EditTarget.Loading)
    val target: StateFlow<EditTarget> = _target.asStateFlow()

    init {
        if (fastId != null) {
            viewModelScope.launch {
                val source = container.fasts.get(fastId)
                _target.value = source?.let { EditTarget.Ready(it.fast) } ?: EditTarget.NotFound
                source?.fast?.templateId?.let { select(it) }
            }
        }
    }

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    fun select(templateId: String?) {
        viewModelScope.launch { _selected.value = templateId?.let { container.templates.get(it) } }
    }

    fun create(input: CreateFast.Input, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.createFast(input)) {
                is Outcome.Ok -> { container.reminders.reschedule(); onDone() }
                is Outcome.Invalid -> showErrors(result.errors, null, input.lastMealPlanned, input.clockStartPlanned, input.targetHours)
            }
        }
    }

    /** H-044: guarda los cambios de un ayuno planificado y recalcula los recordatorios. */
    fun change(input: ChangePlannedFast.Input, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.changePlannedFast(input)) {
                is Outcome.Ok -> { container.reminders.reschedule(); onDone() }
                is Outcome.Invalid -> showErrors(result.errors, input.fastId, input.lastMealPlanned, input.clockStartPlanned, input.targetHours)
            }
        }
    }

    private suspend fun showErrors(errors: List<ValidationError>, fastId: String?, lastMeal: LocalDateTime, start: LocalDateTime, hours: Double) {
        _tooClose.value = if (ValidationError.FAST_TOO_CLOSE in errors) {
            val candidate = Fast(fastId ?: "", null, FastStatus.PLANNED, hours, emptyList(), lastMeal.withNano(0), null, start.withNano(0), null, null, null, null, null, "+00:00", null)
            FastSpacing.conflict(candidate, container.fasts.all(), now())
        } else {
            null
        }
        _errors.value = errors
    }
}

/** H-004: crear un ayuno desde una plantilla (o desde cero) con dos datos. */
@Composable
fun CreateFastScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    val viewModel: CreateFastViewModel = viewModel(factory = containerFactory { CreateFastViewModel(it) })
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val today = remember { viewModel.now().toLocalDate() }
    val clockStart = today.atTime(LocalTime.of(22, 0))
    FastForm(
        title = stringResource(R.string.create_title),
        action = stringResource(R.string.create_action),
        initial = FormValues(null, today.atTime(LocalTime.of(18, 0)), clockStart, clockStart.plusHours(24), ""),
        original = null,
        viewModel = viewModel,
        onBack = onBack,
        onSubmit = { v ->
            viewModel.create(CreateFast.Input.withPlannedEnd(v.templateId, v.lastMeal, v.clockStart, v.plannedEnd, v.notes)) {
                // Se pide el permiso al crear el primer ayuno (ADR-005) sin bloquear la vuelta a Inicio.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifier.canNotify(context)) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onCreated()
            }
        },
    )
}

/** H-044: cambiar un ayuno planificado con el mismo formulario, partiendo de sus valores actuales. */
@Composable
fun ChangeFastScreen(fastId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: CreateFastViewModel = viewModel(key = "change-$fastId", factory = containerFactory { CreateFastViewModel(it, fastId) })
    val target by viewModel.target.collectAsStateWithLifecycle()
    when (val t = target) {
        is EditTarget.Ready -> {
            val fast = t.fast
            val values = FormValues(
                fast.templateId, fast.lastMealPlanned, fast.clockStartPlanned,
                FastClock.plannedEnd(fast), fast.notes.orEmpty(),
            )
            FastForm(
                title = stringResource(R.string.change_title),
                action = stringResource(R.string.change_action),
                initial = values,
                original = values,
                viewModel = viewModel,
                onBack = onBack,
                onSubmit = { v ->
                    viewModel.change(ChangePlannedFast.Input.withPlannedEnd(fast.id, v.templateId, v.lastMeal, v.clockStart, v.plannedEnd, v.notes), onSaved)
                },
            )
        }
        EditTarget.NotFound -> ScreenScaffold(stringResource(R.string.change_title), onBack) { ErrorState(stringResource(R.string.error_not_found), onBack) }
        else -> ScreenScaffold(stringResource(R.string.change_title), onBack) { LoadingState() }
    }
}

/** Datos del formulario de crear o cambiar; `templateId` nulo = desde cero. */
data class FormValues(
    val templateId: String?,
    val lastMeal: LocalDateTime,
    val clockStart: LocalDateTime,
    val plannedEnd: LocalDateTime,
    val notes: String,
)

@Composable
private fun FastForm(
    title: String,
    action: String,
    initial: FormValues,
    original: FormValues?,
    viewModel: CreateFastViewModel,
    onBack: () -> Unit,
    onSubmit: (FormValues) -> Unit,
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val tooClose by viewModel.tooClose.collectAsStateWithLifecycle()

    var choice by remember { mutableStateOf(initial.templateId ?: FROM_SCRATCH) }
    var lastMeal by remember { mutableStateOf(initial.lastMeal) }
    var clockStart by remember { mutableStateOf(initial.clockStart) }
    // H-004: el objetivo se calcula entre el inicio y el fin previstos; la plantilla propone el fin.
    var plannedEnd by remember { mutableStateOf(initial.plannedEnd) }
    var notes by remember { mutableStateOf(initial.notes) }
    // H-044: cambiar la plantilla de un ayuno existente reemplaza su plan; se confirma antes.
    var pendingChoice by remember { mutableStateOf<String?>(null) }

    fun applyChoice(id: String) {
        choice = id
        val templateId = id.takeIf { it != FROM_SCRATCH }
        viewModel.select(templateId)
        templates.firstOrNull { it.id == templateId }?.let { plannedEnd = clockStart.plusMinutes(Math.round(it.targetHours * 60)) }
    }

    ScreenScaffold(title, onBack) {
        SectionTitle(stringResource(R.string.create_which_template))
        RadioList(
            options = templates.map { it.id to "${it.name} · ${Formats.number(it.targetHours)} h" } + (FROM_SCRATCH to stringResource(R.string.create_from_scratch)),
            selected = choice,
            onSelect = { id ->
                val originalChoice = original?.let { it.templateId ?: FROM_SCRATCH }
                if (originalChoice != null && id != choice && id != originalChoice) pendingChoice = id else applyChoice(id)
            },
        )
        DateTimeField(stringResource(R.string.field_last_meal_planned), lastMeal, { lastMeal = it })
        DateTimeField(stringResource(R.string.field_clock_start_planned), clockStart, { newStart ->
            // Al mover el inicio se conserva el objetivo: el fin se desplaza lo mismo.
            plannedEnd = plannedEnd.plus(java.time.Duration.between(clockStart, newStart))
            clockStart = newStart
        })
        DateTimeField(stringResource(R.string.field_end_planned), plannedEnd, { plannedEnd = it })
        val targetHours = FastClock.targetHoursBetween(clockStart, plannedEnd)
        Text(
            if (targetHours > 0) stringResource(R.string.create_target_computed, Formats.number(targetHours, 2)) else stringResource(R.string.error_planned_end),
            style = MaterialTheme.typography.titleLarge,
        )
        AyunoTextField(stringResource(R.string.field_fast_notes), notes, { notes = it }, singleLine = false)

        if (targetHours > 0) {
            val preview = Fast(
                "preview", null, FastStatus.PLANNED, targetHours, emptyList(), lastMeal, null, clockStart, null, null, null, null, null, "+00:00", null,
            )
            val windows = StageWindows.of(preview, selected?.plan ?: Plan.EMPTY)
            windows.filter { it.kind != StageKind.FAST }.forEach { w ->
                Text(stringResource(R.string.create_stage_range, stageName(w.kind), Formats.dateTime(w.start), Formats.dateTime(w.end)), style = MaterialTheme.typography.bodyLarge)
            }
        }
        errors.forEach { error ->
            val conflict = tooClose
            val text = when {
                error != ValidationError.FAST_TOO_CLOSE || conflict == null -> stringResource(error.message())
                else -> conflict.earliestLastMeal
                    ?.let { stringResource(R.string.error_fast_too_close_after, Formats.dateTime(conflict.otherWindow.end), Formats.dateTime(it)) }
                    ?: stringResource(R.string.error_fast_too_close_before, Formats.dateTime(conflict.otherWindow.start))
            }
            FieldError(text)
        }
        val values = FormValues(choice.takeIf { it != FROM_SCRATCH }, lastMeal, clockStart, plannedEnd, notes)
        PrimaryButton(action, enabled = original == null || values != original, onClick = { onSubmit(values) })
    }

    pendingChoice?.let { id ->
        val name = templates.firstOrNull { it.id == id }?.name
        // DialogoConfirmacion: la acción segura (Cancelar) a la derecha.
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.change_template_title)) },
            text = {
                Text(
                    if (name != null) stringResource(R.string.change_template_question, name) else stringResource(R.string.change_template_question_scratch),
                )
            },
            dismissButton = { TextButton(onClick = { pendingChoice = null; applyChoice(id) }) { Text(stringResource(R.string.change_template_confirm)) } },
            confirmButton = { TextButton(onClick = { pendingChoice = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
fun stageName(kind: StageKind): String = stringResource(
    when (kind) {
        StageKind.PREPARATION -> R.string.stage_preparation
        StageKind.FAST -> R.string.stage_fast
        StageKind.REFEED -> R.string.stage_refeed
        StageKind.POST -> R.string.stage_post
    },
)

@Composable
fun stageNameText(kind: StageKind): String = stageName(kind)

private const val FROM_SCRATCH = "__scratch__"
