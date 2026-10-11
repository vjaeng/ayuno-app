package com.ayuno.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.DailyTotal
import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.Milestone
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.derived.RefeedSchedule
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

data class NextPending(val kind: PendingKind, val label: String, val code: String, val at: LocalDateTime, val late: Boolean)
data class LastMeasurement(val metricName: String, val value: String, val unit: String, val at: LocalDateTime)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState

    /**
     * Sin ayuno en curso: el próximo planificado (el de última comida más cercana) y el último terminado. Mientras dura su
     * etapa de vuelta a comer, [refeedFastId] (Inicio muestra la vuelta a comer); después, la reintroducción que
     * falta por marcar como recordatorio (H-031).
     */
    data class Idle(
        val planned: Fast?,
        val lastFinished: Fast?,
        val refeedFastId: String? = null,
        val pendingReintroduction: List<Pair<LocalDateTime, String>> = emptyList(),
    ) : HomeUiState

    data class InProgress(
        val fast: Fast,
        val elapsed: Duration,
        val remaining: Duration,
        val plannedEnd: LocalDateTime,
        val milestones: List<Milestone>,
        val next: NextPending?,
        val last: LastMeasurement?,
        val totals: List<DailyTotal>,
        val notices: List<String>,
        val undecidedAlertId: String?,
    ) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer, tickMillis: Long = 30_000) : ViewModel() {

    private val ticker = flow { while (true) { emit(Unit); delay(tickMillis) } }

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    /** Algunas acciones no cambian la tabla de ayunos observada: se fuerza el recálculo tras cada una. */
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<HomeUiState> = combine(container.fasts.observeFasts(), refresh) { fasts, _ -> fasts }
        .flatMapLatest { fasts ->
            val current = fasts.firstOrNull { it.status == FastStatus.IN_PROGRESS }
            if (current == null) {
                val planned = fasts.filter { it.status == FastStatus.PLANNED }.minByOrNull { it.lastMealPlanned }
                val last = fasts.filter { it.status == FastStatus.FINISHED }.maxByOrNull { it.endActual ?: it.clockStartPlanned }
                val checks = last?.let { container.refeed.observeChecks(it.id) } ?: flow { emit(emptyList()) }
                checks.map<List<RefeedCheck>, HomeUiState> { lastChecks ->
                    val source = last?.let { container.fasts.get(it.id) }
                    val now = LocalTimes.now(container.clock)
                    val inRefeed = source != null && RefeedSchedule.inRefeedStage(source, now)
                    val steps = source?.plan?.refeed.orEmpty().associateBy { it.id }
                    val reintroduction = if (inRefeed) emptyList() else lastChecks
                        .filter { it.doneAt == null && RefeedSchedule.isReintroduction(steps[it.refeedStepId]) }
                        .map { it.scheduledAt to steps[it.refeedStepId]?.description.orEmpty() }
                    HomeUiState.Idle(planned, last, source?.fast?.id?.takeIf { inRefeed }, reintroduction)
                }
            } else {
                // Cualquier cambio en el registro del ayuno o el paso del tiempo recalcula el estado.
                val changes: List<Flow<Unit>> = listOf(
                    ticker,
                    container.records.observeMeasurements(current.id).map { },
                    container.records.observeBloodPressure(current.id).map { },
                    container.records.observeIntakes(current.id).map { },
                    container.records.observeProductLogs(current.id).map { },
                    container.alerts.observeAlerts(current.id).map { },
                    container.catalog.observeMetricTypes().map { },
                )
                merge(*changes.toTypedArray()).map { inProgress(current.id) }
            }
        }
        .catch { emit(HomeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private suspend fun inProgress(fastId: String): HomeUiState {
        val c = container
        val nowInstant = c.clock.instant()
        val now = LocalTimes.now(c.clock)
        val source = c.fasts.get(fastId) ?: return HomeUiState.Error
        val fast = source.fast
        val types = c.catalog.metricTypes()
        val products = c.catalog.products()
        val measurements = c.records.measurements(fastId)
        val readings = c.records.bloodPressure(fastId)
        val records = PendingRecords(measurements, readings, c.records.productLogs(fastId))
        val names = types.associate { it.code to it.name }
        val active = types.filter { it.active }.map { it.code }.toSet()
        val next = Pending.items(source, active, products, now.minusHours(2), now.plusHours(48))
            .map { Pending.status(it, now, records) }
            .firstOrNull { it.state == PendingState.PENDING || it.state == PendingState.LATE }
            ?.let { s ->
                val label = when (s.item.kind) {
                    PendingKind.METRIC -> names[s.item.code] ?: s.item.code
                    PendingKind.PRODUCT -> products.firstOrNull { it.code == s.item.code }?.let { "${it.name} · ${Formats.number(s.item.amount ?: it.defaultAmount)} ${it.unit}" } ?: s.item.code
                    PendingKind.REFEED -> ""
                }
                NextPending(s.item.kind, label, s.item.code, s.item.scheduledAt, s.state == PendingState.LATE)
            }
        val lastMeasurement = measurements.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { m -> LastMeasurement(names[m.metricTypeCode] ?: m.metricTypeCode, m.valueEntered?.let { Formats.number(it, 2) } ?: m.valueText.orEmpty(), m.unitEntered.orEmpty(), m.takenAt) }
        val lastReading = readings.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { r -> LastMeasurement(names[MetricType.BLOOD_PRESSURE_CODE] ?: "", "${r.systolic}/${r.diastolic}", "mmHg", r.takenAt) }
        val stage = StageWindows.stageAt(StageWindows.of(fast, source.plan), now)
        return HomeUiState.InProgress(
            fast = fast,
            elapsed = FastClock.elapsed(fast, nowInstant) ?: Duration.ZERO,
            remaining = FastClock.remaining(fast, nowInstant),
            plannedEnd = FastClock.plannedEnd(fast),
            milestones = FastClock.milestones(fast, nowInstant),
            next = next,
            last = listOfNotNull(lastMeasurement, lastReading).maxByOrNull { it.at },
            totals = DailyTotals.of(now.toLocalDate(), c.records.intakes(fastId), c.catalog.intakeTypes(), source.plan.goals),
            notices = source.plan.notices.filter { stage != null && stage in it.stageKinds }.map { it.text },
            undecidedAlertId = c.alerts.alerts(fastId).firstOrNull { it.userChoice == null }?.id,
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
                is Outcome.Ok -> { _errors.value = emptyList(); refresh.value++; container.reminders.reschedule(); onOk() }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}
