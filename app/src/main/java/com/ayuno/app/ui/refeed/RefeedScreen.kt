package com.ayuno.app.ui.refeed

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.record.SimpleRecordViewModel
import com.ayuno.app.ui.record.SimpleSave
import com.ayuno.domain.derived.RefeedSchedule
import com.ayuno.domain.model.Meal
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordMeal
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Una casilla con lo que se muestra de su paso. */
data class RefeedRow(val check: RefeedCheck, val description: String)

data class RefeedState(
    val fastId: String,
    val endActual: LocalDateTime?,
    val timed: List<RefeedRow>,
    val reintroduction: List<RefeedRow>,
    val meals: List<Meal>,
)

/** H-031 (seguir el plan) y H-032 (comidas anotadas) de un ayuno terminado. */
class RefeedViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    private val fast: Flow<com.ayuno.domain.model.FastWithPlan?> = flow { emit(container.fasts.get(fastId)) }

    val state: StateFlow<RefeedState?> = combine(fast, container.refeed.observeChecks(fastId), container.refeed.observeMeals(fastId)) { source, checks, meals ->
        val steps = source?.plan?.refeed.orEmpty().associateBy { it.id }
        val rows = checks.map { RefeedRow(it, steps[it.refeedStepId]?.description.orEmpty()) }
        RefeedState(
            fastId = fastId,
            endActual = source?.fast?.endActual,
            timed = rows.filter { steps[it.check.refeedStepId]?.kind == RefeedStepKind.TIMED },
            reintroduction = rows.filter { RefeedSchedule.isReintroduction(steps[it.check.refeedStepId]) },
            meals = meals,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    fun setCheck(checkId: String, done: Boolean) {
        viewModelScope.launch {
            when (val result = container.setRefeedCheck(checkId, done)) {
                is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}

@Composable
fun RefeedScreen(fastId: String, onBack: () -> Unit, onMeal: () -> Unit) {
    ScreenScaffold(stringResource(R.string.stage_refeed), onBack) {
        RefeedContent(fastId, onMeal)
    }
}

/** Contenido de la vuelta a comer: también lo muestra Inicio mientras dura la etapa (decisión de UX). */
@Composable
fun RefeedContent(fastId: String, onMeal: () -> Unit) {
    val viewModel: RefeedViewModel = viewModel(key = "refeed-$fastId", factory = containerFactory { RefeedViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    var unchecking by remember { mutableStateOf<RefeedRow?>(null) }
    val s = state ?: run { LoadingState(); return }

    s.endActual?.let { Text(stringResource(R.string.refeed_since, Formats.date(it), Formats.time(it)), style = MaterialTheme.typography.bodyLarge) }
    errors.forEach { FieldError(stringResource(it.message())) }
    val toggle = { row: RefeedRow, on: Boolean -> if (on) viewModel.setCheck(row.check.id, true) else unchecking = row }

    if (s.timed.isEmpty() && s.reintroduction.isEmpty()) {
        EmptyState(stringResource(R.string.refeed_empty))
    }
    if (s.timed.isNotEmpty()) {
        SectionTitle(stringResource(R.string.refeed_steps_title))
        s.timed.forEach { row ->
            val done = row.check.doneAt?.let { " " + stringResource(R.string.refeed_done_at, Formats.time(it)) }.orEmpty()
            CheckRow("${Formats.time(row.check.scheduledAt)} ${row.description}$done", row.check.doneAt != null) { toggle(row, it) }
        }
    }

    SectionTitle(stringResource(R.string.refeed_meals_title))
    s.meals.forEach { meal ->
        Text("${Formats.time(meal.eatenAt)} ${meal.description} · ${toleranceText(meal.tolerance, meal.toleranceDetail)}", style = MaterialTheme.typography.bodyLarge)
    }
    SecondaryButton(stringResource(R.string.refeed_add_meal), onClick = onMeal)

    if (s.reintroduction.isNotEmpty()) {
        SectionTitle(stringResource(R.string.refeed_reintroduction_title))
        s.reintroduction.forEach { row ->
            CheckRow("${Formats.date(row.check.scheduledAt)} · ${row.description}", row.check.doneAt != null) { toggle(row, it) }
        }
    }

    unchecking?.let { row ->
        // DialogoConfirmacion: desmarcar borra la hora real (queda en el registro de cambios).
        AlertDialog(
            onDismissRequest = { unchecking = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = { Text(stringResource(R.string.refeed_uncheck_question, row.description)) },
            dismissButton = { TextButton(onClick = { viewModel.setCheck(row.check.id, false); unchecking = null }) { Text(stringResource(R.string.refeed_uncheck)) } },
            confirmButton = { TextButton(onClick = { unchecking = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
fun toleranceText(tolerance: MealTolerance, detail: String?): String = when (tolerance) {
    MealTolerance.FINE -> stringResource(R.string.tolerance_fine)
    MealTolerance.HEAVINESS -> stringResource(R.string.tolerance_heaviness)
    MealTolerance.NAUSEA -> stringResource(R.string.tolerance_nausea)
    MealTolerance.DIARRHEA -> stringResource(R.string.tolerance_diarrhea)
    MealTolerance.OTHER -> detail ?: stringResource(R.string.tolerance_other)
}

/** H-032 */
class MealViewModel(container: AppContainer) : SimpleRecordViewModel(container) {
    fun record(input: RecordMeal.Input) = run({ container.recordMeal(input) }) { SimpleSave.Saved() }
}

@Composable
fun MealFormScreen(fastId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: MealViewModel = viewModel(factory = containerFactory { MealViewModel(it) })
    val save by viewModel.save.collectAsStateWithLifecycle()
    var eatenAt by remember { mutableStateOf(viewModel.now()) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var tolerance by remember { mutableStateOf<MealTolerance?>(null) }
    var detail by remember { mutableStateOf("") }
    LaunchedEffect(save) { if (save is SimpleSave.Saved) onSaved() }
    ScreenScaffold(stringResource(R.string.meal_title), onBack) {
        DateTimeField(stringResource(R.string.field_taken_at), eatenAt, { eatenAt = it })
        AyunoTextField(stringResource(R.string.field_meal_description), description, { description = it })
        AyunoTextField(stringResource(R.string.field_meal_amount), amount, { amount = it })
        Text(stringResource(R.string.field_meal_tolerance), style = MaterialTheme.typography.titleMedium)
        RadioList(
            listOf(
                MealTolerance.FINE to stringResource(R.string.tolerance_fine),
                MealTolerance.HEAVINESS to stringResource(R.string.tolerance_heaviness),
                MealTolerance.NAUSEA to stringResource(R.string.tolerance_nausea),
                MealTolerance.DIARRHEA to stringResource(R.string.tolerance_diarrhea),
                MealTolerance.OTHER to stringResource(R.string.tolerance_other),
            ),
            tolerance,
            { tolerance = it },
        )
        if (tolerance == MealTolerance.OTHER) AyunoTextField(stringResource(R.string.field_tolerance_detail), detail, { detail = it })
        (save as? SimpleSave.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(
            stringResource(R.string.action_save),
            enabled = save !is SimpleSave.Saving,
            onClick = { viewModel.record(RecordMeal.Input(fastId, eatenAt, description, amount, tolerance, detail)) },
        )
    }
}
