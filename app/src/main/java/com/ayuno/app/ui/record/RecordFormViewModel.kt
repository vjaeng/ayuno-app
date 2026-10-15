package com.ayuno.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordResult
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Qué pasó al guardar. */
sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data class NeedsConfirmation(val fields: List<String>) : SaveState
    data class Invalid(val errors: List<ValidationError>) : SaveState
    data class Saved(val alerts: List<Alert>, val bloodPressure: BloodPressureReading? = null) : SaveState

    /** La escritura falló (no una validación): lo tecleado se conserva para reintentar. */
    data object Failed : SaveState
}

class RecordFormViewModel(private val container: AppContainer, val metricCode: String) : ViewModel() {
    private val _type = MutableStateFlow<MetricType?>(null)
    val type: StateFlow<MetricType?> = _type.asStateFlow()

    private val _save = MutableStateFlow<SaveState>(SaveState.Idle)
    val save: StateFlow<SaveState> = _save.asStateFlow()

    init {
        viewModelScope.launch { _type.value = container.catalog.metricType(metricCode) }
    }

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    /** H-008: proponer «al despertar» en la primera lectura tras el fin del silencio nocturno. */
    suspend fun proposeOnWaking(fastId: String): Boolean = com.ayuno.domain.derived.WakingProposal.propose(
        now(), container.settings.get(), container.records.measurements(fastId), container.records.bloodPressure(fastId),
    )

    suspend fun pairedDifference(fastId: String, standing: BloodPressureReading): BloodPressureReading? =
        container.records.bloodPressure(fastId).firstOrNull { it.id == standing.pairedReadingId }

    fun recordMeasurement(input: RecordMeasurement.Input) = launch { container.recordMeasurement(input) }

    fun recordBloodPressure(input: RecordBloodPressure.Input) = launch { container.recordBloodPressure(input) }

    fun reset() { _save.value = SaveState.Idle }

    private fun <T> launch(block: suspend () -> Outcome<RecordResult<T>>) {
        _save.value = SaveState.Saving
        viewModelScope.launch {
            _save.value = when (val result = block()) {
                is Outcome.Invalid -> SaveState.Invalid(result.errors)
                is Outcome.Ok -> when (val r = result.value) {
                    is RecordResult.NeedsRangeConfirmation -> SaveState.NeedsConfirmation(r.fields)
                    is RecordResult.Saved -> {
                        container.reminders.reschedule()
                        SaveState.Saved(r.alerts, r.entry as? BloodPressureReading)
                    }
                }
            }
        }
    }
}
