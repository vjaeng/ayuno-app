package com.ayuno.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.Milestone
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

data class NextPending(val metricName: String, val metricCode: String, val at: LocalDateTime, val late: Boolean)
data class LastMeasurement(val metricName: String, val value: String, val unit: String, val at: LocalDateTime)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState

    /** Sin ayuno en curso: el próximo planificado y el último terminado (si existen). */
    data class Idle(val planned: Fast?, val lastFinished: Fast?) : HomeUiState

    data class InProgress(
        val fast: Fast,
        val elapsed: Duration,
        val remaining: Duration,
        val plannedEnd: LocalDateTime,
        val milestones: List<Milestone>,
        val next: NextPending?,
        val last: LastMeasurement?,
        val undecidedAlertId: String?,
    ) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer, tickMillis: Long = 30_000) : ViewModel() {

    private val ticker = flow { while (true) { emit(Unit); delay(tickMillis) } }

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    val state: StateFlow<HomeUiState> = container.fasts.observeFasts()
        .flatMapLatest { fasts ->
            val current = fasts.firstOrNull { it.status == FastStatus.IN_PROGRESS }
            if (current == null) {
                val planned = fasts.filter { it.status == FastStatus.PLANNED }.minByOrNull { it.clockStartPlanned }
                val last = fasts.filter { it.status == FastStatus.FINISHED }.maxByOrNull { it.endActual ?: it.clockStartPlanned }
                flow<HomeUiState> { emit(HomeUiState.Idle(planned, last)) }
            } else {
                combine(
                    container.records.observeMeasurements(current.id),
                    container.records.observeBloodPressure(current.id),
                    container.alerts.observeAlerts(current.id),
                    container.catalog.observeMetricTypes(),
                    ticker,
                ) { measurements, readings, alerts, types, _ -> inProgress(current, measurements, readings, alerts, types) as HomeUiState }
            }
        }
        .catch { emit(HomeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private suspend fun inProgress(
        fast: Fast,
        measurements: List<com.ayuno.domain.model.Measurement>,
        readings: List<com.ayuno.domain.model.BloodPressureReading>,
        alerts: List<com.ayuno.domain.model.Alert>,
        types: List<MetricType>,
    ): HomeUiState.InProgress {
        val nowInstant = container.clock.instant()
        val now = LocalTimes.now(container.clock)
        val source = container.fasts.get(fast.id)!!
        val names = types.associate { it.code to it.name }
        val active = types.filter { it.active }.map { it.code }.toSet()
        val next = Pending.items(source, active, now.minusHours(2), now.plusHours(48))
            .map { Pending.status(it, now, measurements, readings) }
            .firstOrNull { it.state == PendingState.PENDING || it.state == PendingState.LATE }
            ?.let { NextPending(names[it.item.metricTypeCode] ?: it.item.metricTypeCode, it.item.metricTypeCode, it.item.scheduledAt, it.state == PendingState.LATE) }
        val lastMeasurement = measurements.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { m -> LastMeasurement(names[m.metricTypeCode] ?: m.metricTypeCode, m.valueEntered?.let { com.ayuno.app.ui.format.Formats.number(it, 2) } ?: m.valueText.orEmpty(), m.unitEntered.orEmpty(), m.takenAt) }
        val lastReading = readings.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { r -> LastMeasurement(names[MetricType.BLOOD_PRESSURE_CODE] ?: "", "${r.systolic}/${r.diastolic}", "mmHg", r.takenAt) }
        return HomeUiState.InProgress(
            fast = fast,
            elapsed = FastClock.elapsed(fast, nowInstant) ?: Duration.ZERO,
            remaining = FastClock.remaining(fast, nowInstant),
            plannedEnd = FastClock.plannedEnd(fast),
            milestones = FastClock.milestones(fast, nowInstant),
            next = next,
            last = listOfNotNull(lastMeasurement, lastReading).maxByOrNull { it.at },
            undecidedAlertId = alerts.firstOrNull { it.userChoice == null }?.id,
        )
    }

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    fun startFast(fastId: String) = execute { container.startFast(fastId) }

    fun registerLastMeal(fastId: String, at: LocalDateTime) = execute { container.registerLastMeal(fastId, at) }

    fun correctClockStart(fastId: String, at: LocalDateTime, reason: String, onDone: () -> Unit) =
        execute(onDone) { container.correctClockStart(fastId, at, reason) }

    fun clearErrors() { _errors.value = emptyList() }

    fun exactAlarmsAllowed(): Boolean = container.reminders.canScheduleExact()

    private fun execute(onOk: () -> Unit = {}, block: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            when (val result = block()) {
                is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule(); onOk() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}
