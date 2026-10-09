package com.ayuno.app.ui.catalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.SaveMetricType
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MetricCatalogViewModel(private val container: AppContainer) : ViewModel() {
    val types: StateFlow<List<MetricType>?> = container.catalog.observeMetricTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    fun setActive(code: String, active: Boolean) = viewModelScope.launch { container.setMetricTypeActive(code, active); container.reminders.reschedule() }

    fun save(input: SaveMetricType.Input, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = container.saveMetricType(input)) {
            is Outcome.Ok -> { _errors.value = emptyList(); onDone() }
            is Outcome.Invalid -> _errors.value = r.errors
        }
    }

    fun clearErrors() { _errors.value = emptyList() }
}

/** H-007: catálogo de mediciones precargado, desactivable, editable y ampliable. */
@Composable
fun MetricCatalogScreen(onBack: () -> Unit, onEdit: (code: String?) -> Unit, viewModel: MetricCatalogViewModel = viewModel(factory = containerFactory { MetricCatalogViewModel(it) })) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.catalog_metrics_title), onBack) {
        val list = types ?: run { LoadingState(); return@ScreenScaffold }
        list.forEach { t ->
            ListRow(
                title = t.name,
                supporting = listOfNotNull(t.unit, t.altUnit?.let { stringResource(R.string.catalog_or_unit, it) }).joinToString(" "),
                onClick = { onEdit(t.code) },
            ) { Switch(checked = t.active, onCheckedChange = { viewModel.setActive(t.code, it) }) }
        }
        SecondaryButton(stringResource(R.string.catalog_add_metric), onClick = { onEdit(null) })
    }
}

@Composable
fun MetricTypeEditorScreen(code: String?, onBack: () -> Unit, viewModel: MetricCatalogViewModel = viewModel(factory = containerFactory { MetricCatalogViewModel(it) })) {
    val types by viewModel.types.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.clearErrors() }
    val existing = code?.let { c -> types?.firstOrNull { it.code == c } }
    if (code != null && existing == null) { LoadingState(); return }

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var unit by remember(existing) { mutableStateOf(existing?.unit.orEmpty()) }
    var numeric by remember(existing) { mutableStateOf(existing?.isNumeric ?: true) }
    var min by remember(existing) { mutableStateOf(existing?.minValid?.let { Formats.number(it, 2) }.orEmpty()) }
    var max by remember(existing) { mutableStateOf(existing?.maxValid?.let { Formats.number(it, 2) }.orEmpty()) }

    ScreenScaffold(stringResource(if (existing == null) R.string.catalog_new_metric else R.string.catalog_edit_metric), onBack) {
        AyunoTextField(stringResource(R.string.field_name), name, { name = it })
        if (existing == null) {
            AyunoTextField(stringResource(R.string.field_unit), unit, { unit = it })
            CheckRow(stringResource(R.string.field_is_numeric), numeric) { numeric = it }
        } else {
            Text(stringResource(R.string.catalog_unit_fixed, existing.unit), style = MaterialTheme.typography.bodyLarge)
        }
        if (numeric) {
            AyunoTextField(stringResource(R.string.field_min_valid), min, { min = it }, numeric = true)
            AyunoTextField(stringResource(R.string.field_max_valid), max, { max = it }, numeric = true)
        }
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = {
            viewModel.save(
                SaveMetricType.Input(code, name, unit, numeric, Formats.parseNumber(min).takeIf { numeric }, Formats.parseNumber(max).takeIf { numeric }),
                onBack,
            )
        })
    }
}
