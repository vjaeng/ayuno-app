package com.ayuno.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.home.totalText
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.usecase.RecordIntake
import com.ayuno.domain.usecase.RecordProductLog
import com.ayuno.domain.usecase.RecordSymptom
import java.time.LocalDateTime

/** H-010: síntoma de un toque con intensidad 0-10; los de alarma primero y distinguidos. */
@Composable
fun SymptomFormScreen(fastId: String, onBack: () -> Unit, onSaved: (List<Alert>) -> Unit) {
    val viewModel: SymptomViewModel = viewModel(factory = containerFactory { SymptomViewModel(it) })
    val types by viewModel.types.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    var notedAt by remember { mutableStateOf(viewModel.now()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var severity by remember { mutableStateOf(5f) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(save) { (save as? SimpleSave.Saved)?.let { onSaved(it.alerts) } }

    ScreenScaffold(stringResource(R.string.record_symptom), onBack) {
        val list = types?.filter { it.active } ?: run { LoadingState(); return@ScreenScaffold }
        if (list.isEmpty()) { EmptyState(stringResource(R.string.symptom_empty)); return@ScreenScaffold }
        DateTimeField(stringResource(R.string.field_taken_at), notedAt, { notedAt = it }, showSeconds = true)
        list.groupBy { if (it.isAlarm) null else it.groupName }.forEach { (group, items) ->
            SectionTitle(group ?: if (items.first().isAlarm) stringResource(R.string.symptom_alarm_group) else stringResource(R.string.symptom_other_group))
            RadioList(
                items.map { it.code to (if (it.isAlarm) "⚠ " else "") + it.name },
                selected,
                { selected = it },
            )
        }
        val severityText = stringResource(R.string.symptom_intensity, severity.toInt())
        Text(severityText, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = severity, onValueChange = { severity = it }, valueRange = 0f..10f, steps = 9,
            modifier = Modifier.semantics { contentDescription = severityText },
        )
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
        (save as? SimpleSave.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), enabled = selected != null && save !is SimpleSave.Saving, onClick = {
            viewModel.record(RecordSymptom.Input(fastId, selected!!, notedAt, severity.toInt(), note))
        })
    }
}

/** H-012: líquido o electrolito con cantidad; sodio calculado; total del día frente a la meta. */
@Composable
fun IntakeFormScreen(fastId: String, onBack: () -> Unit, onSaved: (alerts: List<Alert>, endFastAt: LocalDateTime?) -> Unit) {
    val viewModel: IntakeViewModel = viewModel(key = "intake-$fastId", factory = containerFactory { IntakeViewModel(it, fastId) })
    val types by viewModel.types.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    var takenAt by remember { mutableStateOf(viewModel.now()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var askEnd by remember { mutableStateOf<SimpleSave.Saved?>(null) }
    LaunchedEffect(save) {
        val saved = save as? SimpleSave.Saved ?: return@LaunchedEffect
        if (saved.endFastAt != null && saved.alerts.isEmpty()) askEnd = saved else onSaved(saved.alerts, null)
        viewModel.reset()
    }

    ScreenScaffold(stringResource(R.string.record_intake), onBack) {
        val list = types ?: run { LoadingState(); return@ScreenScaffold }
        DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
        RadioList(list.map { it.code to "${it.name} (${it.unit})" }, selected, { selected = it })
        val type = list.firstOrNull { it.code == selected }
        AyunoTextField(stringResource(R.string.field_amount), amount, { amount = it }, numeric = true, suffix = type?.unit)
        val number = Formats.parseNumber(amount)
        if (type?.sodiumFactor != null && number != null) {
            Text(stringResource(R.string.intake_sodium, Formats.number(number * type.sodiumFactor!!, 2)), style = MaterialTheme.typography.bodyLarge)
        }
        totals.firstOrNull { it.intakeType.code == selected }?.let { Text(totalText(it), style = MaterialTheme.typography.bodyLarge) }
        AyunoTextField(stringResource(R.string.field_note), note, { note = it }, singleLine = false)
        (save as? SimpleSave.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), enabled = type != null && save !is SimpleSave.Saving, onClick = {
            viewModel.record(RecordIntake.Input(fastId, type!!.code, takenAt, number, null, note))
        })
    }
    askEnd?.let { saved ->
        val name = types?.firstOrNull { it.code == selected }?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { askEnd = null; onSaved(saved.alerts, null) },
            title = { Text(stringResource(R.string.intake_breaks_title, name)) },
            confirmButton = { TextButton(onClick = { askEnd = null; onSaved(saved.alerts, saved.endFastAt) }) { Text(stringResource(R.string.intake_breaks_yes)) } },
            dismissButton = { TextButton(onClick = { askEnd = null; onSaved(saved.alerts, null) }) { Text(stringResource(R.string.intake_breaks_no)) } },
        )
    }
}

/** H-015: tomas pendientes primero; los que salen en gris; aviso si es «a evitar». */
@Composable
fun ProductFormScreen(
    fastId: String,
    scheduledFor: LocalDateTime?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    productCode: String? = null,
    notDone: Boolean = false,
) {
    val viewModel: ProductViewModel = viewModel(key = "product-$fastId-$scheduledFor", factory = containerFactory { ProductViewModel(it, fastId, scheduledFor) })
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    var chosen by remember { mutableStateOf<ProductChoice?>(null) }
    var confirmAvoid by remember { mutableStateOf<ProductChoice?>(null) }
    LaunchedEffect(save) { if (save is SimpleSave.Saved) onSaved() }
    // Desde la notificación (H-017) se abre directamente la toma de ese producto.
    LaunchedEffect(choices) { if (chosen == null && productCode != null) chosen = choices?.firstOrNull { it.product.code == productCode } }

    chosen?.let { choice ->
        ProductLogForm(choice, viewModel, fastId, onBack = { chosen = null }, startNotTaken = notDone && choice.product.code == productCode)
        return
    }
    ScreenScaffold(stringResource(R.string.record_product), onBack) {
        val list = choices ?: run { LoadingState(); return@ScreenScaffold }
        if (list.isEmpty()) { EmptyState(stringResource(R.string.product_empty)); return@ScreenScaffold }
        list.forEach { c ->
            val stops = c.plan?.duringFast == DuringFast.STOPS
            val supporting = listOfNotNull(
                c.pendingAt?.let { stringResource(R.string.product_pending_at, Formats.time(it), Formats.number(c.pendingAmount ?: c.product.defaultAmount), c.product.unit) },
                stringResource(R.string.product_stops).takeIf { stops },
                stringResource(R.string.product_avoid).takeIf { c.product.avoid },
            ).joinToString(" · ").ifEmpty { null }
            ListRow(
                title = c.product.name,
                supporting = supporting,
                onClick = { if (c.product.avoid) confirmAvoid = c else chosen = c },
            ) {}
            if (stops) Text(stringResource(R.string.product_stops_hint), style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
        }
    }
    confirmAvoid?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmAvoid = null },
            title = { Text(stringResource(R.string.product_avoid_title)) },
            text = { Text(stringResource(R.string.product_avoid_text, c.product.name)) },
            confirmButton = { TextButton(onClick = { confirmAvoid = null; chosen = c }) { Text(stringResource(R.string.product_record_anyway)) } },
            dismissButton = { TextButton(onClick = { confirmAvoid = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ProductLogForm(choice: ProductChoice, viewModel: ProductViewModel, fastId: String, onBack: () -> Unit, startNotTaken: Boolean = false) {
    val save by viewModel.save.collectAsStateWithLifecycle()
    var takenAt by remember { mutableStateOf(viewModel.now()) }
    var status by remember { mutableStateOf(if (startNotTaken) ProductLogStatus.NOT_TAKEN else ProductLogStatus.TAKEN) }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val planned = choice.pendingAmount ?: choice.product.defaultAmount
    ScreenScaffold(choice.product.name, onBack) {
        Text(stringResource(R.string.product_planned, Formats.number(planned), choice.product.unit), style = MaterialTheme.typography.bodyLarge)
        RadioList(
            listOf(
                ProductLogStatus.TAKEN to stringResource(R.string.product_taken),
                ProductLogStatus.NOT_TAKEN to stringResource(R.string.product_not_taken),
                ProductLogStatus.DOSE_CHANGED to stringResource(R.string.product_other_dose),
            ),
            status,
            { status = it },
        )
        if (status != ProductLogStatus.NOT_TAKEN) DateTimeField(stringResource(R.string.field_taken_at), takenAt, { takenAt = it }, showSeconds = true)
        if (status == ProductLogStatus.DOSE_CHANGED) AyunoTextField(stringResource(R.string.field_amount), amount, { amount = it }, numeric = true, suffix = choice.product.unit)
        if (status != ProductLogStatus.TAKEN) AyunoTextField(stringResource(R.string.field_reason), reason, { reason = it })
        (save as? SimpleSave.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), enabled = save !is SimpleSave.Saving, onClick = {
            viewModel.record(RecordProductLog.Input(fastId, choice.product.code, choice.pendingAt, status, takenAt, Formats.parseNumber(amount), reason))
        })
    }
}

/** H-035: nota libre con hora. */
@Composable
fun NoteFormScreen(fastId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: NoteViewModel = viewModel(factory = containerFactory { NoteViewModel(it) })
    val save by viewModel.save.collectAsStateWithLifecycle()
    var notedAt by remember { mutableStateOf(viewModel.now()) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(save) { if (save is SimpleSave.Saved) onSaved() }
    ScreenScaffold(stringResource(R.string.record_note), onBack) {
        DateTimeField(stringResource(R.string.field_taken_at), notedAt, { notedAt = it })
        AyunoTextField(stringResource(R.string.field_note_text), text, { text = it }, singleLine = false)
        (save as? SimpleSave.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), enabled = save !is SimpleSave.Saving, onClick = { viewModel.record(fastId, notedAt, text) })
    }
}
