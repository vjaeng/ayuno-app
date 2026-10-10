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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.usecase.RecordBloodPressure
import java.time.LocalDateTime

/** H-009: presión sentado y, justo después, de pie; muestra la diferencia calculada. */
@Composable
fun BloodPressureFormScreen(
    fastId: String,
    scheduledFor: LocalDateTime?,
    onBack: () -> Unit,
    onSaved: (alerts: List<Alert>) -> Unit,
    askNotDone: Boolean = false,
) {
    val viewModel: RecordFormViewModel = viewModel(key = "bp-$scheduledFor", factory = containerFactory { RecordFormViewModel(it, MetricType.BLOOD_PRESSURE_CODE) })
    val save by viewModel.save.collectAsStateWithLifecycle()

    var takenAt by remember { mutableStateOf(scheduledFor ?: viewModel.now()) }
    var position by remember { mutableStateOf(Position.SITTING) }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var onWaking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var pairedWith by remember { mutableStateOf<BloodPressureReading?>(null) }
    var askStanding by remember { mutableStateOf<BloodPressureReading?>(null) }
    var difference by remember { mutableStateOf<Pair<BloodPressureReading, BloodPressureReading>?>(null) }
    var notDoneDialog by rememberNotDoneDialog(askNotDone)
    LaunchedEffect(fastId) { if (viewModel.proposeOnWaking(fastId)) onWaking = true }

    fun input(confirmed: Boolean = false, notDone: String? = null) = RecordBloodPressure.Input(
        fastId = fastId,
        takenAt = takenAt,
        scheduledFor = scheduledFor.takeIf { pairedWith == null },
        systolic = systolic.trim().toIntOrNull().takeIf { notDone == null },
        diastolic = diastolic.trim().toIntOrNull().takeIf { notDone == null },
        pulse = pulse.trim().toIntOrNull().takeIf { notDone == null },
        position = position,
        onWaking = onWaking,
        pairedReadingId = pairedWith?.id,
        note = note,
        notDoneReason = notDone,
        confirmedOutOfRange = confirmed,
    )

    LaunchedEffect(save) {
        val saved = save as? SaveState.Saved ?: return@LaunchedEffect
        val reading = saved.bloodPressure
        when {
            saved.alerts.isNotEmpty() || reading == null || reading.notDoneReason != null -> onSaved(saved.alerts)
            reading.position == Position.SITTING -> askStanding = reading
            reading.position == Position.STANDING && pairedWith != null -> difference = pairedWith!! to reading
            else -> onSaved(emptyList())
        }
        viewModel.reset()
    }

    ScreenScaffold(stringResource(R.string.bp_title), onBack) {
        DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
        if (pairedWith == null) {
            OptionSelector(
                listOf(Position.SITTING to stringResource(R.string.position_sitting), Position.STANDING to stringResource(R.string.position_standing), Position.LYING to stringResource(R.string.position_lying)),
                position,
                { position = it },
            )
        } else {
            Text(stringResource(R.string.bp_now_standing), style = MaterialTheme.typography.titleLarge)
        }
        AyunoTextField(stringResource(R.string.field_systolic), systolic, { systolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_diastolic), diastolic, { diastolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_pulse), pulse, { pulse = it }, numeric = true, suffix = stringResource(R.string.unit_per_minute))
        CheckRow(stringResource(R.string.field_on_waking), onWaking) { onWaking = it }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
        (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.recordBloodPressure(input()) }, enabled = save !is SaveState.Saving)
        if (pairedWith == null) SecondaryButton(stringResource(R.string.record_not_done), onClick = { notDoneDialog = true })
    }

    if (save is SaveState.NeedsConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::reset,
            title = { Text(stringResource(R.string.record_out_of_range_title)) },
            text = { Text(stringResource(R.string.bp_out_of_range_text)) },
            confirmButton = { TextButton(onClick = { viewModel.recordBloodPressure(input(confirmed = true)) }) { Text(stringResource(R.string.record_save_as_is)) } },
            dismissButton = { TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.record_correct)) } },
        )
    }
    askStanding?.let { sitting ->
        AlertDialog(
            onDismissRequest = { askStanding = null; onSaved(emptyList()) },
            title = { Text(stringResource(R.string.bp_ask_standing)) },
            confirmButton = {
                TextButton(onClick = {
                    askStanding = null
                    pairedWith = sitting
                    position = Position.STANDING
                    systolic = ""; diastolic = ""; pulse = ""; note = ""
                    takenAt = viewModel.now()
                }) { Text(stringResource(R.string.bp_record_standing)) }
            },
            dismissButton = { TextButton(onClick = { askStanding = null; onSaved(emptyList()) }) { Text(stringResource(R.string.action_not_now)) } },
        )
    }
    difference?.let { (sitting, standing) ->
        AlertDialog(
            onDismissRequest = { difference = null; onSaved(emptyList()) },
            title = { Text(stringResource(R.string.bp_difference_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.bp_difference_text,
                        signed((standing.systolic ?: 0) - (sitting.systolic ?: 0)),
                        signed((standing.diastolic ?: 0) - (sitting.diastolic ?: 0)),
                        if (standing.pulse != null && sitting.pulse != null) signed(standing.pulse!! - sitting.pulse!!) else "—",
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { difference = null; onSaved(emptyList()) }) { Text(stringResource(R.string.action_accept)) } },
        )
    }
    if (notDoneDialog) {
        NotDoneDialog(onDismiss = { notDoneDialog = false }) { reason ->
            notDoneDialog = false
            viewModel.recordBloodPressure(input(notDone = reason))
        }
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
