package com.ayuno.app.ui.record

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.ayuno.app.ui.components.InfoBanner
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Trend
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.units.UnitConversions
import com.ayuno.domain.usecase.CorrectBloodPressure
import com.ayuno.domain.usecase.CorrectMeasurement
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EntryUi {
    data object Loading : EntryUi
    data object NotFound : EntryUi
    data class Loaded(
        val fast: Fast,
        val measurement: Measurement?,
        val reading: BloodPressureReading?,
        val type: MetricType?,
        val paired: Boolean,
        val changes: List<EditLogEntry>,
    ) : EntryUi
}

/** H-034 (mediciones y presión), H-048: corregir o borrar una entrada con su bitácora (TRX corregir-entrada). */
class CorrectEntryViewModel(private val container: AppContainer, private val kind: EntryKind, private val id: String) : ViewModel() {
    private val reload = MutableStateFlow(0)

    val state: StateFlow<EntryUi> = combine(container.audit.observeEntriesFor(id), reload) { changes, _ -> load(changes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUi.Loading)

    private suspend fun load(changes: List<EditLogEntry>): EntryUi {
        val r = container.records
        return when (kind) {
            EntryKind.MEASUREMENT -> {
                val m = r.measurement(id) ?: return EntryUi.NotFound
                val fast = container.fasts.get(m.fastId)?.fast ?: return EntryUi.NotFound
                EntryUi.Loaded(fast, m, null, container.catalog.metricType(m.metricTypeCode), false, changes)
            }
            EntryKind.BLOOD_PRESSURE -> {
                val reading = r.bloodPressureReading(id) ?: return EntryUi.NotFound
                val fast = container.fasts.get(reading.fastId)?.fast ?: return EntryUi.NotFound
                val paired = reading.pairedReadingId != null || r.bloodPressure(reading.fastId).any { it.pairedReadingId == reading.id }
                EntryUi.Loaded(fast, null, reading, container.catalog.metricType(MetricType.BLOOD_PRESSURE_CODE), paired, changes)
            }
        }
    }

    private val _save = MutableStateFlow<SaveState>(SaveState.Idle)
    val save: StateFlow<SaveState> = _save.asStateFlow()

    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed: StateFlow<Boolean> = _deleteFailed.asStateFlow()

    fun correctMeasurement(input: CorrectMeasurement.Input) = launch { container.correctMeasurement(input) }

    fun correctBloodPressure(input: CorrectBloodPressure.Input) = launch { container.correctBloodPressure(input) }

    fun reset() { _save.value = SaveState.Idle }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (runCatching { container.setEntryDeleted(kind, id, deleted = true) }.getOrNull()) {
                is Outcome.Ok -> { container.reminders.reschedule(); onDeleted() }
                else -> _deleteFailed.value = true
            }
        }
    }

    private fun <T> launch(block: suspend () -> Outcome<RecordResult<T>>) {
        _save.value = SaveState.Saving
        viewModelScope.launch {
            _save.value = when (val result = runCatching { block() }.getOrNull()) {
                null -> SaveState.Failed
                is Outcome.Invalid -> SaveState.Invalid(result.errors)
                is Outcome.Ok -> when (val r = result.value) {
                    is RecordResult.NeedsRangeConfirmation -> SaveState.NeedsConfirmation(r.fields)
                    is RecordResult.Saved -> {
                        container.reminders.reschedule()
                        SaveState.Saved(r.alerts)
                    }
                }
            }
        }
    }
}

/** Detalle de entrada (UX-UI historial-revisar.md): el formulario de Anotar con los valores actuales. */
@Composable
fun CorrectEntryScreen(kind: EntryKind, id: String, onBack: () -> Unit, onSaved: (alerts: List<Alert>) -> Unit) {
    val viewModel: CorrectEntryViewModel = viewModel(key = "entry-$id", factory = containerFactory { CorrectEntryViewModel(it, kind, id) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val deleteFailed by viewModel.deleteFailed.collectAsStateWithLifecycle()
    var confirmingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(save) { (save as? SaveState.Saved)?.let { onSaved(it.alerts) } }

    val loaded = state as? EntryUi.Loaded
    ScreenScaffold(
        loaded?.type?.name.orEmpty(),
        onBack,
        actions = { if (loaded != null) EntryMenu(onDelete = { confirmingDelete = true }) },
    ) {
        when (val s = state) {
            EntryUi.Loading -> LoadingState()
            EntryUi.NotFound -> EmptyState(stringResource(R.string.error_entry_not_found), stringResource(R.string.action_back), onBack)
            is EntryUi.Loaded -> {
                if (deleteFailed) FieldError(stringResource(R.string.correct_save_error))
                s.measurement?.let { m -> s.type?.let { MeasurementCorrection(s.fast, m, it, save, viewModel) } }
                s.reading?.let { BloodPressureCorrection(s.fast, it, s.paired, save, viewModel) }
                Changes(s.changes)
            }
        }
    }

    if (confirmingDelete && loaded != null) {
        val what = loaded.measurement?.let { "${loaded.type?.name.orEmpty()} ${it.valueEntered?.let { v -> Formats.number(v, 2) } ?: it.valueText.orEmpty()} ${it.unitEntered.orEmpty()}".trim() }
            ?: loaded.reading?.let { "${loaded.type?.name.orEmpty()} ${it.systolic ?: ""}/${it.diastolic ?: ""}" }.orEmpty()
        val at = loaded.measurement?.takenAt ?: loaded.reading!!.takenAt
        // DialogoConfirmacion: la acción segura (Cancelar) a la derecha, sin rojo.
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(horizontal = Dimens.spaceXl),
            title = { Text(stringResource(R.string.correct_delete_title)) },
            text = { Text(stringResource(R.string.correct_delete_text, what, Formats.dateTime(at))) },
            dismissButton = { TextButton(onClick = { confirmingDelete = false; viewModel.delete(onBack) }) { Text(stringResource(R.string.correct_delete)) } },
            confirmButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** MenuAcciones: «Borrar» (acción rara y destructiva, lejos de «Guardar»). */
@Composable
private fun EntryMenu(onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(Dimens.touchMin)) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.correct_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.correct_delete)) }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun MeasurementCorrection(fast: Fast, m: Measurement, type: MetricType, save: SaveState, viewModel: CorrectEntryViewModel) {
    var takenAt by remember(m.id) { mutableStateOf(m.takenAt) }
    val initialValue = m.valueEntered?.let { Formats.number(it, 4) } ?: m.valueText.orEmpty()
    var value by remember(m.id) { mutableStateOf(initialValue) }
    var unit by remember(m.id) { mutableStateOf(m.unitEntered ?: type.entryUnit) }
    var source by remember(m.id) { mutableStateOf(m.source) }
    var trend by remember(m.id) { mutableStateOf(m.trend) }
    var onWaking by remember(m.id) { mutableStateOf(m.onWaking) }
    var note by remember(m.id) { mutableStateOf(m.note.orEmpty()) }
    var notDone by remember(m.id) { mutableStateOf(m.notDoneReason.orEmpty()) }
    var reason by remember(m.id) { mutableStateOf("") }
    val isNotDone = m.notDoneReason != null

    fun input(confirmed: Boolean = false) = CorrectMeasurement.Input(
        id = m.id,
        takenAt = takenAt,
        valueEntered = if (!isNotDone && type.isNumeric) Formats.parseNumber(value) else null,
        unitEntered = unit.takeIf { !isNotDone && type.isNumeric },
        valueText = value.takeIf { !isNotDone && !type.isNumeric },
        source = source,
        trend = trend,
        onWaking = onWaking,
        note = note,
        notDoneReason = notDone.takeIf { isNotDone },
        reason = reason,
        confirmedOutOfRange = confirmed,
    )
    val changed = takenAt != m.takenAt || value != initialValue || unit != (m.unitEntered ?: type.entryUnit) || source != m.source ||
        trend != m.trend || onWaking != m.onWaking || note != m.note.orEmpty() || notDone != m.notDoneReason.orEmpty()

    DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
    if (isNotDone) {
        Text(stringResource(R.string.correct_not_done_info), style = MaterialTheme.typography.bodyLarge)
        AyunoTextField(stringResource(R.string.field_reason), notDone, { notDone = it })
    } else {
        AyunoTextField(stringResource(R.string.field_value), value, { value = it }, numeric = type.isNumeric, suffix = unit.takeIf { type.isNumeric })
        if (type.isNumeric && type.altUnit != null) {
            OptionSelector(listOf(type.unit to type.unit, type.altUnit!! to type.altUnit!!), unit, { unit = it })
            val number = Formats.parseNumber(value)
            if (number != null && unit != type.unit) {
                Text(
                    stringResource(R.string.record_equivalent, Formats.number(number, 2), unit, Formats.number(UnitConversions.convert(number, unit, type.unit)), type.unit),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (type.isGlucose) GlucoseFields(source, trend, { source = it }, { trend = if (trend == it) null else it })
        CheckRow(stringResource(R.string.field_on_waking), onWaking) { onWaking = it }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
    }
    CorrectionFooter(fast, reason, { reason = it }, save, changed, onSave = { viewModel.correctMeasurement(input()) })
    if (save is SaveState.NeedsConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::reset,
            title = { Text(stringResource(R.string.record_out_of_range_title)) },
            text = { Text(stringResource(R.string.record_out_of_range_text, value, Formats.number(type.minValid ?: 0.0), Formats.number(type.maxValid ?: 0.0))) },
            confirmButton = { TextButton(onClick = { viewModel.correctMeasurement(input(confirmed = true)) }) { Text(stringResource(R.string.record_save_as_is)) } },
            dismissButton = { TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.record_correct)) } },
        )
    }
}

@Composable
private fun BloodPressureCorrection(fast: Fast, r: BloodPressureReading, paired: Boolean, save: SaveState, viewModel: CorrectEntryViewModel) {
    var takenAt by remember(r.id) { mutableStateOf(r.takenAt) }
    var position by remember(r.id) { mutableStateOf(r.position) }
    var systolic by remember(r.id) { mutableStateOf(r.systolic?.toString().orEmpty()) }
    var diastolic by remember(r.id) { mutableStateOf(r.diastolic?.toString().orEmpty()) }
    var pulse by remember(r.id) { mutableStateOf(r.pulse?.toString().orEmpty()) }
    var onWaking by remember(r.id) { mutableStateOf(r.onWaking) }
    var note by remember(r.id) { mutableStateOf(r.note.orEmpty()) }
    var notDone by remember(r.id) { mutableStateOf(r.notDoneReason.orEmpty()) }
    var reason by remember(r.id) { mutableStateOf("") }
    val isNotDone = r.notDoneReason != null

    fun input(confirmed: Boolean = false) = CorrectBloodPressure.Input(
        id = r.id,
        takenAt = takenAt,
        systolic = systolic.trim().toIntOrNull().takeIf { !isNotDone },
        diastolic = diastolic.trim().toIntOrNull().takeIf { !isNotDone },
        pulse = pulse.trim().toIntOrNull().takeIf { !isNotDone },
        position = position,
        onWaking = onWaking,
        note = note,
        notDoneReason = notDone.takeIf { isNotDone },
        reason = reason,
        confirmedOutOfRange = confirmed,
    )
    val changed = takenAt != r.takenAt || position != r.position || systolic != r.systolic?.toString().orEmpty() ||
        diastolic != r.diastolic?.toString().orEmpty() || pulse != r.pulse?.toString().orEmpty() || onWaking != r.onWaking ||
        note != r.note.orEmpty() || notDone != r.notDoneReason.orEmpty()

    DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
    if (paired) {
        Text(positionText(position), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.correct_paired_info), style = MaterialTheme.typography.bodyLarge)
    } else {
        OptionSelector(
            listOf(Position.SITTING to positionText(Position.SITTING), Position.STANDING to positionText(Position.STANDING), Position.LYING to positionText(Position.LYING)),
            position,
            { position = it },
        )
    }
    if (isNotDone) {
        Text(stringResource(R.string.correct_not_done_info), style = MaterialTheme.typography.bodyLarge)
        AyunoTextField(stringResource(R.string.field_reason), notDone, { notDone = it })
    } else {
        AyunoTextField(stringResource(R.string.field_systolic), systolic, { systolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_diastolic), diastolic, { diastolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_pulse), pulse, { pulse = it }, numeric = true, suffix = stringResource(R.string.unit_per_minute))
        CheckRow(stringResource(R.string.field_on_waking), onWaking) { onWaking = it }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
    }
    CorrectionFooter(fast, reason, { reason = it }, save, changed, onSave = { viewModel.correctBloodPressure(input()) })
    if (save is SaveState.NeedsConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::reset,
            title = { Text(stringResource(R.string.record_out_of_range_title)) },
            text = { Text(stringResource(R.string.bp_out_of_range_text)) },
            confirmButton = { TextButton(onClick = { viewModel.correctBloodPressure(input(confirmed = true)) }) { Text(stringResource(R.string.record_save_as_is)) } },
            dismissButton = { TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.record_correct)) } },
        )
    }
}

/** Motivo opcional, aviso de reglas (solo en curso), errores y la única acción principal. */
@Composable
private fun CorrectionFooter(fast: Fast, reason: String, onReason: (String) -> Unit, save: SaveState, changed: Boolean, onSave: () -> Unit) {
    AyunoTextField(stringResource(R.string.correct_reason), reason, onReason)
    if (fast.status == FastStatus.IN_PROGRESS) InfoBanner(stringResource(R.string.correct_rules_info))
    (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
    if (save is SaveState.Failed) FieldError(stringResource(R.string.correct_save_error))
    PrimaryButton(stringResource(R.string.action_save), onClick = onSave, enabled = changed && save !is SaveState.Saving)
}

/** Bitácora de la entrada: cada cambio con su hora, valores y motivo; borrados y restauraciones. */
@Composable
private fun Changes(changes: List<EditLogEntry>) {
    if (changes.isEmpty()) return
    SectionTitle(stringResource(R.string.correct_changes))
    changes.sortedByDescending { it.editedAt }.forEach { e ->
        val line = when (e.action) {
            EditAction.EDIT -> stringResource(R.string.correct_change_line, fieldLabel(e.field), changeValue(e.field, e.oldValue), changeValue(e.field, e.newValue)) +
                (e.reason?.let { " · «$it»" } ?: "")
            EditAction.DELETE -> stringResource(R.string.correct_change_deleted)
            EditAction.RESTORE -> stringResource(R.string.correct_change_restored)
        }
        Column {
            Text(Formats.dateTime(e.editedAt), style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
            Text(line, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun fieldLabel(field: String?): String = when (field) {
    "taken_at" -> stringResource(R.string.field_label_taken_at)
    "value_entered", "value_text" -> stringResource(R.string.field_value)
    "value_num" -> stringResource(R.string.field_label_value_num)
    "unit_entered" -> stringResource(R.string.field_label_unit)
    "source" -> stringResource(R.string.field_source)
    "trend" -> stringResource(R.string.field_label_trend)
    "on_waking" -> stringResource(R.string.field_on_waking)
    "note" -> stringResource(R.string.field_label_note)
    "not_done_reason" -> stringResource(R.string.field_label_not_done)
    "systolic" -> stringResource(R.string.field_systolic)
    "diastolic" -> stringResource(R.string.field_diastolic)
    "pulse" -> stringResource(R.string.field_label_pulse)
    "position" -> stringResource(R.string.field_label_position)
    else -> field.orEmpty()
}

@Composable
private fun changeValue(field: String?, raw: String?): String {
    if (raw == null) return stringResource(R.string.correct_empty_value)
    return when (field) {
        "taken_at" -> runCatching { Formats.dateTimeSeconds(LocalTimes.parse(raw)) }.getOrDefault(raw)
        "source" -> GlucoseSource.entries.firstOrNull { it.code == raw }?.let { sourceText(it) } ?: raw
        "trend" -> Trend.entries.firstOrNull { it.code == raw }?.let { trendText(it) } ?: raw
        "position" -> Position.entries.firstOrNull { it.code == raw }?.let { positionText(it) } ?: raw
        "on_waking" -> stringResource(if (raw == "true") R.string.answer_yes else R.string.answer_no)
        else -> raw.replace('.', ',')
    }
}

/** Origen y tendencia de la glucosa (H-008), compartidos por Anotar y Corregir. */
@Composable
fun GlucoseFields(source: GlucoseSource?, trend: Trend?, onSource: (GlucoseSource) -> Unit, onTrend: (Trend) -> Unit) {
    Text(stringResource(R.string.field_source), style = MaterialTheme.typography.bodyLarge)
    OptionSelector(listOf(GlucoseSource.CGM to sourceText(GlucoseSource.CGM), GlucoseSource.GLUCOMETER to sourceText(GlucoseSource.GLUCOMETER)), source, onSource)
    Text(stringResource(R.string.field_trend), style = MaterialTheme.typography.bodyLarge)
    OptionSelector(Trend.entries.map { it to trendText(it) }, trend, onTrend)
}
