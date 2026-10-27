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

/** H-009: presión; tras guardar sentado sin alertas, [onSitting] abre el temporizador de pie (H-053). */
@Composable
fun BloodPressureFormScreen(
    fastId: String,
    scheduledFor: LocalDateTime?,
    onBack: () -> Unit,
    onSaved: (alerts: List<Alert>) -> Unit,
    askNotDone: Boolean = false,
    onSitting: (BloodPressureReading) -> Unit = { onSaved(emptyList()) },
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
    var notDoneDialog by rememberNotDoneDialog(askNotDone)
    LaunchedEffect(fastId) { if (viewModel.proposeOnWaking(fastId)) onWaking = true }

    fun input(confirmed: Boolean = false, notDone: String? = null) = RecordBloodPressure.Input(
        fastId = fastId,
        takenAt = takenAt,
        scheduledFor = scheduledFor,
        systolic = systolic.trim().toIntOrNull().takeIf { notDone == null },
        diastolic = diastolic.trim().toIntOrNull().takeIf { notDone == null },
        pulse = pulse.trim().toIntOrNull().takeIf { notDone == null },
        position = position,
        onWaking = onWaking,
        note = note,
        notDoneReason = notDone,
        confirmedOutOfRange = confirmed,
    )

    LaunchedEffect(save) {
        val saved = save as? SaveState.Saved ?: return@LaunchedEffect
        val reading = saved.bloodPressure
        when {
            saved.alerts.isNotEmpty() || reading == null || reading.notDoneReason != null -> onSaved(saved.alerts)
            reading.position == Position.SITTING -> onSitting(reading)
            else -> onSaved(emptyList())
        }
        viewModel.reset()
    }

    ScreenScaffold(stringResource(R.string.bp_title), onBack) {
        DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
        OptionSelector(
            listOf(Position.SITTING to stringResource(R.string.position_sitting), Position.STANDING to stringResource(R.string.position_standing), Position.LYING to stringResource(R.string.position_lying)),
            position,
            { position = it },
        )
        AyunoTextField(stringResource(R.string.field_systolic), systolic, { systolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_diastolic), diastolic, { diastolic = it }, numeric = true, suffix = "mmHg")
        AyunoTextField(stringResource(R.string.field_pulse), pulse, { pulse = it }, numeric = true, suffix = stringResource(R.string.unit_per_minute))
        CheckRow(stringResource(R.string.field_on_waking), onWaking) { onWaking = it }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
        (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = { viewModel.recordBloodPressure(input()) }, enabled = save !is SaveState.Saving)
        SecondaryButton(stringResource(R.string.record_not_done), onClick = { notDoneDialog = true })
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
    if (notDoneDialog) {
        NotDoneDialog(onDismiss = { notDoneDialog = false }) { reason ->
            notDoneDialog = false
            viewModel.recordBloodPressure(input(notDone = reason))
        }
    }
}
