package com.ayuno.app.ui.record

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.InfoBanner
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.Timer
import com.ayuno.app.ui.components.TimerMark
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.format.standingMoment
import com.ayuno.domain.derived.StandingMoment
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

sealed interface StandingState {
    data object Loading : StandingState
    data object NotFound : StandingState

    /**
     * [since]: hora de «Ya estoy de pie» (no se guarda: vive aquí y en las alarmas). [form]: lectura abierta;
     * su `target` es el tiempo del plan o nulo para «sin esperar». [handled]: tiempos ya pedidos (hechos o saltados).
     */
    data class Loaded(
        val sitting: BloodPressureReading,
        val checkSeconds: List<Int>,
        val standing: List<BloodPressureReading>,
        val since: LocalDateTime? = null,
        val now: LocalDateTime,
        val form: StandingForm? = null,
        val handled: Set<Int> = emptySet(),
        val finished: Boolean = false,
        val alarmsExact: Boolean = true,
    ) : StandingState {
        val elapsed: Long get() = since?.let { Duration.between(it, now).seconds.coerceAtLeast(0) } ?: 0
    }
}

data class StandingForm(val target: Int?, val takenAt: LocalDateTime)

/** H-053: temporizador de pie tras la toma sentado; una lectura enlazada por cada tiempo del plan. */
class StandingTimerViewModel(
    private val container: AppContainer,
    private val fastId: String,
    private val sittingId: String,
    initialSince: LocalDateTime?,
    tickMillis: Long = 1_000,
) : ViewModel() {
    private val _state = MutableStateFlow<StandingState>(StandingState.Loading)
    val state: StateFlow<StandingState> = _state.asStateFlow()

    private val _save = MutableStateFlow<SaveState>(SaveState.Idle)
    val save: StateFlow<SaveState> = _save.asStateFlow()

    init {
        viewModelScope.launch {
            val sitting = container.records.bloodPressure(fastId).firstOrNull { it.id == sittingId && it.position == Position.SITTING }
            if (sitting == null) {
                _state.value = StandingState.NotFound
                return@launch
            }
            val seconds = container.fasts.get(fastId)?.plan?.metrics
                ?.firstOrNull { it.metricTypeCode == MetricType.BLOOD_PRESSURE_CODE }?.standingCheckSeconds.orEmpty().sorted()
            val standing = standingOf(sitting)
            // Desde el aviso: los tiempos con lectura ya hecha no se vuelven a pedir.
            val done = standing.mapNotNull { StandingMoment.of(it, seconds) }.toSet()
            _state.value = StandingState.Loaded(sitting, seconds, standing, since = initialSince, now = now(), handled = done)
            while (true) {
                tick()
                delay(tickMillis)
            }
        }
    }

    private fun now(): LocalDateTime = LocalTimes.now(container.clock)

    private suspend fun standingOf(sitting: BloodPressureReading) = container.records.bloodPressure(fastId)
        .filter { it.pairedReadingId == sitting.id && it.position == Position.STANDING && it.notDoneReason == null }
        .sortedBy { it.takenAt }

    private fun loaded(block: (StandingState.Loaded) -> StandingState.Loaded) = _state.update { (it as? StandingState.Loaded)?.let(block) ?: it }

    /** Abre la lectura del último tiempo vencido sin pedir; si quedaba abierta la anterior, esa queda sin hacer. */
    fun tick() = loaded { s ->
        val now = now()
        val since = s.since
        if (since == null || s.finished) return@loaded s.copy(now = now)
        val due = s.checkSeconds.filter { !since.plusSeconds(it.toLong()).isAfter(now) }
        val latest = due.maxOrNull()
        if (latest == null || latest in s.handled) return@loaded s.copy(now = now)
        s.copy(now = now, handled = s.handled + due, form = StandingForm(latest, since.plusSeconds(latest.toLong())))
    }

    fun ready() {
        val s = _state.value as? StandingState.Loaded ?: return
        if (s.since != null) return
        val since = now()
        val exact = container.reminders.scheduleStanding(fastId, sittingId, since, s.checkSeconds)
        loaded { it.copy(since = since, now = since, alarmsExact = exact) }
    }

    fun withoutWaiting() = loaded { it.copy(form = StandingForm(null, now())) }

    fun setTakenAt(takenAt: LocalDateTime) = loaded { s -> s.copy(form = s.form?.copy(takenAt = takenAt)) }

    /** Cierra la lectura sin guardarla: ese tiempo queda sin hacer. */
    fun closeForm() = loaded { s -> s.copy(form = null).finishIfDone() }

    /** «Cancelar»: retira las alarmas pendientes; las lecturas ya guardadas se quedan. */
    fun cancel() {
        val s = _state.value as? StandingState.Loaded ?: return
        s.since?.let { container.reminders.cancelStanding(fastId, sittingId, it, s.checkSeconds) }
        loaded { it.copy(form = null, finished = true) }
    }

    private var last: (() -> Unit)? = null

    /** Guardar tal cual tras el aviso de valores fuera de lo habitual. */
    fun confirm() {
        _save.value = SaveState.Idle
        last?.invoke()
    }

    fun record(systolic: Int?, diastolic: Int?, pulse: Int?, note: String, confirmed: Boolean = false) {
        last = { record(systolic, diastolic, pulse, note, confirmed = true) }
        val s = _state.value as? StandingState.Loaded ?: return
        val form = s.form ?: return
        _save.value = SaveState.Saving
        viewModelScope.launch {
            val input = RecordBloodPressure.Input(
                fastId = fastId,
                takenAt = form.takenAt,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                position = Position.STANDING,
                pairedReadingId = sittingId,
                note = note,
                confirmedOutOfRange = confirmed,
                standingSince = s.since.takeIf { form.target != null },
            )
            _save.value = when (val result = container.recordBloodPressure(input)) {
                is Outcome.Invalid -> SaveState.Invalid(result.errors)
                is Outcome.Ok -> when (val r = result.value) {
                    is RecordResult.NeedsRangeConfirmation -> SaveState.NeedsConfirmation(r.fields)
                    is RecordResult.Saved -> {
                        form.target?.let { container.reminders.cancelStandingNotice(sittingId, it) }
                        container.reminders.reschedule()
                        val standing = standingOf(s.sitting)
                        loaded { it.copy(standing = standing, form = null).finishIfDone() }
                        SaveState.Saved(r.alerts, r.entry as? BloodPressureReading)
                    }
                }
            }
        }
    }

    fun resetSave() { _save.value = SaveState.Idle }

    /** Termina tras el último tiempo, o tras una lectura «sin esperar» si el temporizador no llegó a arrancar. */
    private fun StandingState.Loaded.finishIfDone(): StandingState.Loaded {
        val allAsked = since != null && checkSeconds.all { it in handled }
        val onlyWithoutWaiting = since == null && standing.isNotEmpty()
        return if (form == null && (allAsked || onlyWithoutWaiting)) copy(finished = true) else this
    }
}

@Composable
fun StandingTimerScreen(
    fastId: String,
    sittingId: String,
    since: LocalDateTime?,
    onBack: () -> Unit,
    onAlerts: (List<Alert>) -> Unit,
) {
    val viewModel: StandingTimerViewModel = viewModel(
        key = "standing-$sittingId",
        factory = containerFactory { StandingTimerViewModel(it, fastId, sittingId, since) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()

    LaunchedEffect(save) {
        val saved = save as? SaveState.Saved ?: return@LaunchedEffect
        viewModel.resetSave()
        // El temporizador sigue en segundo plano mientras se ve la alerta.
        if (saved.alerts.isNotEmpty()) onAlerts(saved.alerts)
    }

    ScreenScaffold(stringResource(R.string.standing_title), onBack) {
        when (val s = state) {
            StandingState.Loading -> LoadingState()
            StandingState.NotFound -> {
                ErrorState(stringResource(R.string.standing_not_found), onRetry = onBack)
            }
            is StandingState.Loaded -> when {
                s.finished -> StandingResult(s, onBack)
                s.form != null -> StandingReadingForm(s, s.form, save, viewModel)
                s.since != null -> StandingRunning(s, viewModel)
                else -> StandingReady(s, viewModel, onBack)
            }
        }
    }
    if (save is SaveState.NeedsConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::resetSave,
            title = { Text(stringResource(R.string.record_out_of_range_title)) },
            text = { Text(stringResource(R.string.bp_out_of_range_text)) },
            confirmButton = { TextButton(onClick = viewModel::confirm) { Text(stringResource(R.string.record_save_as_is)) } },
            dismissButton = { TextButton(onClick = viewModel::resetSave) { Text(stringResource(R.string.record_correct)) } },
        )
    }
}

@Composable
private fun SittingReference(sitting: BloodPressureReading) {
    Text(
        stringResource(
            R.string.standing_sitting_reference,
            sitting.systolic?.toString() ?: "—",
            sitting.diastolic?.toString() ?: "—",
            sitting.pulse?.toString() ?: "—",
            Formats.time(sitting.takenAt),
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun StandingReady(s: StandingState.Loaded, viewModel: StandingTimerViewModel, onBack: () -> Unit) {
    val resources = LocalContext.current.resources
    SittingReference(s.sitting)
    if (s.checkSeconds.isEmpty()) {
        Text(stringResource(R.string.standing_empty), style = MaterialTheme.typography.bodyLarge)
    } else {
        val moments = s.checkSeconds.joinToString(stringResource(R.string.standing_and)) { standingMoment(resources, it) }
        Text(stringResource(R.string.standing_intro, moments), style = MaterialTheme.typography.bodyLarge)
        PrimaryButton(stringResource(R.string.standing_ready), onClick = viewModel::ready)
    }
    SecondaryButton(stringResource(R.string.standing_without_waiting), onClick = viewModel::withoutWaiting)
    SecondaryButton(stringResource(R.string.action_not_now), onClick = onBack)
}

@Composable
private fun StandingRunning(s: StandingState.Loaded, viewModel: StandingTimerViewModel) {
    val resources = LocalContext.current.resources
    if (!s.alarmsExact) InfoBanner(stringResource(R.string.standing_alarm_inexact))
    SittingReference(s.sitting)
    val done = s.standing.mapNotNull { StandingMoment.of(it, s.checkSeconds) }.toSet()
    Timer(
        startedText = stringResource(R.string.standing_since, Formats.timeSeconds(s.since!!)),
        seconds = s.elapsed,
        marks = s.checkSeconds.map { TimerMark(stringResource(R.string.standing_at, standingMoment(resources, it)), it in done) },
        announcement = null,
    )
    SecondaryButton(stringResource(R.string.standing_without_waiting), onClick = viewModel::withoutWaiting)
    SecondaryButton(stringResource(R.string.action_cancel), onClick = viewModel::cancel)
}

@Composable
private fun StandingReadingForm(s: StandingState.Loaded, form: StandingForm, save: SaveState, viewModel: StandingTimerViewModel) {
    val resources = LocalContext.current.resources
    var systolic by remember(form.target) { mutableStateOf("") }
    var diastolic by remember(form.target) { mutableStateOf("") }
    var pulse by remember(form.target) { mutableStateOf("") }
    var note by remember(form.target) { mutableStateOf("") }

    val title = if (form.target != null) {
        stringResource(R.string.standing_announcement, standingMoment(resources, form.target))
    } else {
        stringResource(R.string.standing_form_title, standingMoment(resources, null))
    }
    Text(title, style = MaterialTheme.typography.titleLarge)
    DateTimeField(stringResource(R.string.field_taken_at), form.takenAt, viewModel::setTakenAt, showSeconds = true)
    val secondsText = s.since?.takeIf { form.target != null }?.let { since ->
        stringResource(R.string.standing_form_seconds, Duration.between(since, form.takenAt).seconds)
    } ?: stringResource(R.string.standing_form_without_waiting)
    Text(secondsText, style = MaterialTheme.typography.bodyLarge)
    AyunoTextField(stringResource(R.string.field_systolic), systolic, { systolic = it }, numeric = true, suffix = "mmHg")
    AyunoTextField(stringResource(R.string.field_diastolic), diastolic, { diastolic = it }, numeric = true, suffix = "mmHg")
    AyunoTextField(stringResource(R.string.field_pulse), pulse, { pulse = it }, numeric = true, suffix = stringResource(R.string.unit_per_minute))
    AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
    (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
    PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.record(systolic.trim().toIntOrNull(), diastolic.trim().toIntOrNull(), pulse.trim().toIntOrNull(), note) }, enabled = save !is SaveState.Saving)
    SecondaryButton(stringResource(R.string.action_not_now), onClick = viewModel::closeForm)
    if (s.since != null) {
        Text(stringResource(R.string.standing_timer_continues, "%d:%02d".format(s.elapsed / 60, s.elapsed % 60)), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Resultado de la pareja: por cada lectura de pie, cuánto cambiaron las tres cifras; sin colores de juicio. */
@Composable
private fun StandingResult(s: StandingState.Loaded, onDone: () -> Unit) {
    val resources = LocalContext.current.resources
    Text(stringResource(R.string.standing_result_title), style = MaterialTheme.typography.titleLarge)
    SittingReference(s.sitting)
    if (s.standing.isEmpty()) Text(stringResource(R.string.standing_result_empty), style = MaterialTheme.typography.bodyLarge)
    s.standing.forEach { reading ->
        val moment = StandingMoment.of(reading, s.checkSeconds) ?: reading.secondsStanding
        val label = if (reading.secondsStanding == null) standingMoment(resources, null) else stringResource(R.string.standing_at, standingMoment(resources, moment))
        fun diff(c: BpComponent) = StandingMoment.difference(reading, s.sitting, c)?.let(::signed) ?: "—"
        Text(
            stringResource(R.string.standing_result_row, label, diff(BpComponent.SYSTOLIC), diff(BpComponent.DIASTOLIC), diff(BpComponent.PULSE)),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    PrimaryButton(stringResource(R.string.action_done), onClick = onDone)
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
