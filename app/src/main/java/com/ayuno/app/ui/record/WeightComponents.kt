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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.WeightChange
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.units.UnitConversions
import com.ayuno.domain.usecase.RecordMeasurement

/** H-047: campo de peso con la unidad por defecto del catálogo y la otra a elegir. */
@Composable
fun WeightField(type: MetricType, label: String, value: String, onValueChange: (String) -> Unit, unit: String, onUnitChange: (String) -> Unit) {
    AyunoTextField(label = label, value = value, onValueChange = onValueChange, numeric = true, suffix = unit)
    type.altUnit?.let { alt -> OptionSelector(listOf(type.unit to type.unit, alt to alt), unit, onUnitChange) }
}

/** Diálogo «¿Seguro?» de un valor fuera del rango válido (H-008), compartido por los formularios de peso. */
@Composable
fun OutOfRangeDialog(type: MetricType, value: String, onSaveAsIs: () -> Unit, onCorrect: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCorrect,
        title = { Text(stringResource(R.string.record_out_of_range_title)) },
        text = { Text(stringResource(R.string.record_out_of_range_text, value, Formats.number(type.minValid ?: 0.0), Formats.number(type.maxValid ?: 0.0))) },
        confirmButton = { TextButton(onClick = onSaveAsIs) { Text(stringResource(R.string.record_save_as_is)) } },
        dismissButton = { TextButton(onClick = onCorrect) { Text(stringResource(R.string.record_correct)) } },
    )
}

/**
 * H-047: hoja «¿Cuánto pesas al empezar?», justo después de empezar. Guarda por la vía normal de una medición (evalúa
 * reglas); «Ahora no» no guarda nada.
 */
@Composable
fun StartWeightDialog(fastId: String, onDone: () -> Unit) {
    val viewModel: RecordFormViewModel = viewModel(key = "start-weight-$fastId", factory = containerFactory { RecordFormViewModel(it, MetricType.WEIGHT_CODE) })
    val type by viewModel.type.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val current = type ?: return
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(current.entryUnit) }
    fun input(confirmed: Boolean = false) = RecordMeasurement.Input(
        fastId = fastId,
        metricTypeCode = current.code,
        takenAt = viewModel.now(),
        valueEntered = Formats.parseNumber(value),
        unitEntered = unit,
        confirmedOutOfRange = confirmed,
    )
    LaunchedEffect(save) { if (save is SaveState.Saved) onDone() }

    // Sin el ancho por defecto de la plataforma: con un campo de texto, Robolectric entra en un bucle de medida.
    AlertDialog(
        onDismissRequest = onDone,
        modifier = Modifier.padding(horizontal = Dimens.spaceXl),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.weight_start_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
                WeightField(current, stringResource(R.string.field_value), value, { value = it }, unit, { unit = it })
                Text(stringResource(R.string.weight_start_already_started), style = MaterialTheme.typography.bodyLarge)
                (save as? SaveState.Invalid)?.errors?.forEach { FieldError(stringResource(it.message())) }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.recordMeasurement(input()) }, enabled = Formats.parseNumber(value) != null && save !is SaveState.Saving) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.action_not_now)) } },
    )
    if (save is SaveState.NeedsConfirmation) {
        OutOfRangeDialog(current, value, onSaveAsIs = { viewModel.recordMeasurement(input(confirmed = true)) }, onCorrect = viewModel::reset)
    }
}

/** «Peso: 71,4 kg → 68,9 kg · −2,5 kg» en la unidad por defecto, sin colores de juicio (H-047); nulo sin pesos. */
@Composable
fun weightChangeText(change: WeightChange, type: MetricType): String? {
    fun shown(kg: Double) = UnitConversions.convert(kg, type.unit, type.entryUnit)
    fun text(kg: Double) = "${Formats.number(shown(kg))} ${type.entryUnit}"
    val initial = change.initial?.valueNum ?: return null
    val final = change.final?.valueNum ?: return stringResource(R.string.weight_single, text(initial))
    val diff = shown(final) - shown(initial)
    val sign = if (diff > 0) "+" else if (diff < 0) "−" else ""
    return stringResource(R.string.weight_change, text(initial), text(final), "$sign${Formats.number(kotlin.math.abs(diff))} ${type.entryUnit}")
}
