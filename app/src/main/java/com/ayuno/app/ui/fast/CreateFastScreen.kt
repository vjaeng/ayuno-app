package com.ayuno.app.ui.fast

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.reminders.ReminderNotifier
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

class CreateFastViewModel(private val container: AppContainer) : ViewModel() {
    val templates: StateFlow<List<Template>> = container.templates.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selected = MutableStateFlow<TemplateWithPlan?>(null)
    val selected: StateFlow<TemplateWithPlan?> = _selected.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    fun select(templateId: String?) {
        viewModelScope.launch { _selected.value = templateId?.let { container.templates.get(it) } }
    }

    fun create(input: CreateFast.Input, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.createFast(input)) {
                is Outcome.Ok -> { container.reminders.reschedule(); onDone() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}

/** H-004: crear un ayuno desde una plantilla (o desde cero) con dos datos. */
@Composable
fun CreateFastScreen(onBack: () -> Unit, onCreated: () -> Unit, viewModel: CreateFastViewModel = viewModel(factory = containerFactory { CreateFastViewModel(it) })) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var choice by remember { mutableStateOf<String?>(FROM_SCRATCH) }
    val today = remember { viewModel.now().toLocalDate() }
    var lastMeal by remember { mutableStateOf(today.atTime(LocalTime.of(18, 0))) }
    var clockStart by remember { mutableStateOf(today.atTime(LocalTime.of(22, 0))) }
    // H-004: el objetivo se calcula entre el inicio y el fin previstos; la plantilla propone el fin.
    var plannedEnd by remember { mutableStateOf(clockStart.plusHours(24)) }
    var notes by remember { mutableStateOf("") }

    ScreenScaffold(stringResource(R.string.create_title), onBack) {
        SectionTitle(stringResource(R.string.create_which_template))
        RadioList(
            options = templates.map { it.id to "${it.name} · ${Formats.number(it.targetHours)} h" } + (FROM_SCRATCH to stringResource(R.string.create_from_scratch)),
            selected = choice,
            onSelect = { id ->
                choice = id
                val templateId = id.takeIf { it != FROM_SCRATCH }
                viewModel.select(templateId)
                templates.firstOrNull { it.id == templateId }?.let { plannedEnd = clockStart.plusMinutes(Math.round(it.targetHours * 60)) }
            },
        )
        DateTimeField(stringResource(R.string.field_last_meal_planned), lastMeal, { lastMeal = it })
        DateTimeField(stringResource(R.string.field_clock_start_planned), clockStart, { newStart ->
            // Al mover el inicio se conserva el objetivo: el fin se desplaza lo mismo.
            plannedEnd = plannedEnd.plus(java.time.Duration.between(clockStart, newStart))
            clockStart = newStart
        })
        DateTimeField(stringResource(R.string.field_end_planned), plannedEnd, { plannedEnd = it })
        val targetHours = com.ayuno.domain.derived.FastClock.targetHoursBetween(clockStart, plannedEnd)
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
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.create_action), onClick = {
            viewModel.create(
                CreateFast.Input.withPlannedEnd(choice.takeIf { it != FROM_SCRATCH }, lastMeal, clockStart, plannedEnd, notes),
            ) {
                // Se pide el permiso al crear el primer ayuno (ADR-005) sin bloquear la vuelta a Inicio.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifier.canNotify(context)) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onCreated()
            }
        })
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
