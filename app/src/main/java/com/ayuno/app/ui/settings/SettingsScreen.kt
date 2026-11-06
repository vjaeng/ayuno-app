package com.ayuno.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.SwitchRow
import com.ayuno.app.ui.components.TimeField
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Settings
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<Settings?> = container.settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** H-043: solo los tipos con unidad alternativa (glucosa, peso, cintura, temperatura). */
    val unitTypes: StateFlow<List<MetricType>> = container.catalog.observeMetricTypes()
        .map { types -> types.filter { it.altUnit != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    fun setQuiet(start: LocalTime?, end: LocalTime?) = viewModelScope.launch {
        when (val r = container.saveQuietHours(start, end)) {
            is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule() }
            is Outcome.Invalid -> _errors.value = r.errors
        }
    }

    fun setEntryUnit(code: String, unit: String) = viewModelScope.launch {
        when (val r = container.setEntryUnit(code, unit)) {
            is Outcome.Ok -> _errors.value = emptyList()
            is Outcome.Invalid -> _errors.value = r.errors
        }
    }
}

/** Ajustes generales: silencio nocturno (H-017), unidades (H-043) y datos personales (H-038). Contacto y bloqueo llegan con sus historias. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onPersonalData: () -> Unit = {}, viewModel: SettingsViewModel = viewModel(factory = containerFactory { SettingsViewModel(it) })) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val unitTypes by viewModel.unitTypes.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.settings_title), onBack) {
        val s = settings ?: run { LoadingState(); return@ScreenScaffold }
        SectionTitle(stringResource(R.string.settings_reminders))
        val on = s.quietStart != null
        SwitchRow(stringResource(R.string.settings_quiet), on, { enabled ->
            if (enabled) viewModel.setQuiet(LocalTime.of(23, 0), LocalTime.of(6, 30)) else viewModel.setQuiet(null, null)
        }, stringResource(R.string.settings_quiet_hint))
        if (on) {
            TimeField(stringResource(R.string.settings_quiet_start), s.quietStart!!) { viewModel.setQuiet(it, s.quietEnd) }
            TimeField(stringResource(R.string.settings_quiet_end), s.quietEnd ?: LocalTime.of(6, 30)) { viewModel.setQuiet(s.quietStart, it) }
        }
        errors.forEach { FieldError(stringResource(it.message())) }
        Text(stringResource(R.string.settings_quiet_rules), style = MaterialTheme.typography.bodyMedium)
        if (unitTypes.isNotEmpty()) {
            SectionTitle(stringResource(R.string.settings_units))
            unitTypes.forEach { t ->
                Text(t.name, style = MaterialTheme.typography.bodyLarge)
                OptionSelector(listOf(t.unit to t.unit, t.altUnit!! to t.altUnit!!), t.entryUnit, { viewModel.setEntryUnit(t.code, it) })
            }
            Text(stringResource(R.string.settings_units_hint), style = MaterialTheme.typography.bodyMedium)
        }
        SectionTitle(stringResource(R.string.settings_privacy))
        ListRow(
            stringResource(R.string.person_title),
            stringResource(if (s.exportPersonEnabled) R.string.data_person_included else R.string.data_person_not_included),
            onClick = onPersonalData,
        )
    }
}
