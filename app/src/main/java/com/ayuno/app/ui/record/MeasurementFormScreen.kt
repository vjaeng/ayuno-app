package com.ayuno.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withResumed
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Trend
import com.ayuno.domain.units.UnitConversions
import com.ayuno.domain.usecase.RecordMeasurement
import java.time.LocalDateTime

/** H-008: formulario de una sola pantalla para una medición. */
@Composable
fun MeasurementFormScreen(
    fastId: String,
    metricCode: String,
    scheduledFor: LocalDateTime?,
    onBack: () -> Unit,
    onSaved: (alerts: List<Alert>) -> Unit,
    askNotDone: Boolean = false,
) {
    val viewModel: RecordFormViewModel = viewModel(key = "m-$metricCode-$scheduledFor", factory = containerFactory { RecordFormViewModel(it, metricCode) })
    val type by viewModel.type.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()

    var takenAt by remember { mutableStateOf(scheduledFor ?: viewModel.now()) }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf<String?>(null) }
    // Origen preseleccionado (wireframe de UX-UI): anotar una glucosa cuesta 3 toques (H-008).
    var source by remember { mutableStateOf<GlucoseSource?>(GlucoseSource.CGM) }
    var trend by remember { mutableStateOf<Trend?>(null) }
    var onWaking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var notDoneDialog by rememberNotDoneDialog(askNotDone)
    LaunchedEffect(fastId) { if (viewModel.proposeOnWaking(fastId)) onWaking = true }

    val current = type
    ScreenScaffold(current?.name ?: "", onBack) {
        if (current == null) { LoadingState(); return@ScreenScaffold }
        val selectedUnit = unit ?: current.entryUnit
        fun input(confirmed: Boolean = false, notDone: String? = null) = RecordMeasurement.Input(
            fastId = fastId,
            metricTypeCode = current.code,
            takenAt = takenAt,
            scheduledFor = scheduledFor,
            valueEntered = if (current.isNumeric) Formats.parseNumber(value) else null,
            unitEntered = selectedUnit.takeIf { current.isNumeric },
            valueText = value.takeIf { !current.isNumeric },
            source = source,
            trend = trend,
            onWaking = onWaking,
            note = note,
            notDoneReason = notDone,
            confirmedOutOfRange = confirmed,
        )

        LaunchedEffect(save) { (save as? SaveState.Saved)?.let { onSaved(it.alerts) } }

        DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
        AyunoTextField(
            label = stringResource(R.string.field_value),
            value = value,
            onValueChange = { value = it },
            numeric = current.isNumeric,
            suffix = selectedUnit.takeIf { current.isNumeric },
        )
        if (current.isNumeric && current.altUnit != null) {
            OptionSelector(listOf(current.unit to current.unit, current.altUnit!! to current.altUnit!!), selectedUnit, { unit = it })
            val number = Formats.parseNumber(value)
            if (number != null && selectedUnit != current.unit) {
                Text(
                    stringResource(R.string.record_equivalent, Formats.number(number, 2), selectedUnit, Formats.number(UnitConversions.convert(number, selectedUnit, current.unit)), current.unit),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (current.isGlucose) {
            Text(stringResource(R.string.field_source), style = MaterialTheme.typography.bodyLarge)
            OptionSelector(
                listOf(GlucoseSource.CGM to stringResource(R.string.source_cgm), GlucoseSource.GLUCOMETER to stringResource(R.string.source_glucometer)),
                source,
                { source = it },
            )
            Text(stringResource(R.string.field_trend), style = MaterialTheme.typography.bodyLarge)
            OptionSelector(
                listOf(
                    Trend.RISING_FAST to stringResource(R.string.trend_rising_fast), Trend.RISING to stringResource(R.string.trend_rising),
                    Trend.STEADY to stringResource(R.string.trend_steady), Trend.FALLING to stringResource(R.string.trend_falling),
                    Trend.FALLING_FAST to stringResource(R.string.trend_falling_fast),
                ),
                trend,
                { trend = if (trend == it) null else it },
            )
        }
        CheckRow(stringResource(R.string.field_on_waking), onWaking) { onWaking = it }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)

        (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.recordMeasurement(input()) }, enabled = save !is SaveState.Saving)
        SecondaryButton(stringResource(R.string.record_not_done), onClick = { notDoneDialog = true })

        if (save is SaveState.NeedsConfirmation) {
            AlertDialog(
                onDismissRequest = viewModel::reset,
                title = { Text(stringResource(R.string.record_out_of_range_title)) },
                text = {
                    Text(stringResource(R.string.record_out_of_range_text, value, Formats.number(current.minValid ?: 0.0), Formats.number(current.maxValid ?: 0.0)))
                },
                confirmButton = { TextButton(onClick = { viewModel.recordMeasurement(input(confirmed = true)) }) { Text(stringResource(R.string.record_save_as_is)) } },
                dismissButton = { TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.record_correct)) } },
            )
        }
        if (notDoneDialog) {
            NotDoneDialog(onDismiss = { notDoneDialog = false }) { reason ->
                notDoneDialog = false
                value = ""
                viewModel.recordMeasurement(input(notDone = reason).copy(valueEntered = null, valueText = null))
            }
        }
    }
}

/** El diálogo pedido al abrir (desde Hoy o la notificación) espera a que la pantalla termine de entrar. */
@Composable
fun rememberNotDoneDialog(askOnOpen: Boolean): MutableState<Boolean> {
    val open = remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(askOnOpen) { if (askOnOpen) lifecycle.withResumed { open.value = true } }
    return open
}

@Composable
fun NotDoneDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    // Sin el ancho por defecto de la plataforma: con un campo de texto, Robolectric entra en un bucle de medida.
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = Dimens.spaceXl),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.record_not_done)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceM)) { AyunoTextField(stringResource(R.string.field_reason), reason, { reason = it }) } },
        confirmButton = { TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
