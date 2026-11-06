package com.ayuno.app.ui.home

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EntryIcons
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.derived.DailyTotal
import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.ExpectedIntake
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.Milestone
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.derived.RefeedSchedule
import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordResult
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

data class NextPending(val kind: PendingKind, val label: String, val code: String, val at: LocalDateTime, val late: Boolean, @DrawableRes val icon: Int)
data class LastMeasurement(val metricName: String, val value: String, val at: LocalDateTime, @DrawableRes val icon: Int)

/** BotonRapido de un líquido con medida de botón (H-051). */
data class QuickButtonUi(val intakeTypeCode: String, val typeName: String, val unitCode: String, val unitName: String)

/** Snackbar tras guardar con el BotonRapido: «Sal: +1 pizca (0.3 g)» y su «Deshacer». */
data class QuickSaved(val intakeId: String, val typeName: String, val unitName: String, val amount: Double, val unit: String)

/** Avisos de una sola vez que la pantalla consume. */
sealed interface HomeEvent {
    data class OpenAlert(val alertId: String) : HomeEvent
    data class OpenReview(val fastId: String) : HomeEvent
    data class Saved(val saved: QuickSaved) : HomeEvent
}

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
        /** H-048: mediciones y presiones visibles del ayuno. */
        val measurementCount: Int = 0,
        /** H-006 (EV-006): «a esta hora» por tipo de la meta, en la unidad de la meta; solo con silencio nocturno. */
        val expected: Map<String, Double> = emptyMap(),
        val quickButtons: List<QuickButtonUi> = emptyList(),
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
                    container.catalog.observeIntakeTypes().map { },
                    container.settings.observe().map { },
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
        val typesByCode = types.associateBy { it.code }
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
                NextPending(s.item.kind, label, s.item.code, s.item.scheduledAt, s.state == PendingState.LATE, EntryIcons.pending(s.item.kind, s.item.code, typesByCode))
            }
        val lastMeasurement = measurements.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { m ->
                LastMeasurement(
                    names[m.metricTypeCode] ?: m.metricTypeCode,
                    Formats.measurementValue(m, typesByCode[m.metricTypeCode]),
                    m.takenAt,
                    EntryIcons.metric(m.metricTypeCode, typesByCode),
                )
            }
        val lastReading = readings.filter { it.notDoneReason == null }.maxByOrNull { it.takenAt }
            ?.let { r -> LastMeasurement(names[MetricType.BLOOD_PRESSURE_CODE] ?: "", "${r.systolic}/${r.diastolic} mmHg", r.takenAt, EntryIcons.bloodPressure) }
        val stage = StageWindows.stageAt(StageWindows.of(fast, source.plan), now)
        val intakeTypes = c.catalog.intakeTypes()
        val settings = c.settings.get()
        val expected = source.plan.goals.mapNotNull { goal -> ExpectedIntake.at(goal, settings, now)?.let { goal.intakeTypeCode to it } }.toMap()
        val quick = intakeTypes.mapNotNull { t -> t.quickUnit?.let { QuickButtonUi(t.code, t.name, it.code, it.name) } }
        return HomeUiState.InProgress(
            fast = fast,
            elapsed = FastClock.elapsed(fast, nowInstant) ?: Duration.ZERO,
            remaining = FastClock.remaining(fast, nowInstant),
            plannedEnd = FastClock.plannedEnd(fast),
            milestones = FastClock.milestones(fast, nowInstant),
            next = next,
            last = listOfNotNull(lastMeasurement, lastReading).maxByOrNull { it.at },
            totals = DailyTotals.of(now.toLocalDate(), c.records.intakes(fastId), intakeTypes, source.plan.goals),
            notices = source.plan.notices.filter { stage != null && stage in it.stageKinds }.map { it.text },
            undecidedAlertId = c.alerts.alerts(fastId).firstOrNull { it.userChoice == null }?.id,
            measurementCount = measurements.size + readings.size,
            expected = expected,
            quickButtons = quick,
        )
    }

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()
    fun consumeEvent() { _events.value = null }

    /** Tipo cuyo BotonRapido está guardando: deshabilitado hasta confirmar la escritura. */
    private val _saving = MutableStateFlow<String?>(null)
    val saving: StateFlow<String?> = _saving.asStateFlow()

    /** H-051: un toque guarda 1 de la medida del botón; el Snackbar solo aparece tras confirmar la escritura. */
    fun quickIntake(fastId: String, button: QuickButtonUi) {
        if (_saving.value != null) return
        _saving.value = button.intakeTypeCode
        viewModelScope.launch {
            try {
                when (val result = container.recordQuickIntake(fastId, button.intakeTypeCode)) {
                    is Outcome.Ok -> {
                        val saved = result.value as RecordResult.Saved
                        _errors.value = emptyList()
                        refresh.value++
                        container.reminders.reschedule()
                        _events.value = saved.alerts.firstOrNull()?.let { HomeEvent.OpenAlert(it.id) }
                            ?: HomeEvent.Saved(QuickSaved(saved.entry.id, button.typeName, button.unitName, saved.entry.amount, saved.entry.unit))
                    }
                    is Outcome.Invalid -> _errors.value = result.errors
                }
            } finally {
                _saving.value = null
            }
        }
    }

    /** «Deshacer»: borrado blando con bitácora; las alertas que hubiera disparado se quedan (S8). */
    fun undoQuickIntake(intakeId: String) = execute { container.setEntryDeleted(EntryKind.INTAKE, intakeId, true) }

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    /** H-005, H-057: con pendientes en la revisión previa, abre la revisión en vez de empezar. */
    fun startFast(fastId: String) {
        viewModelScope.launch {
            when (val result = container.startFastAskingWeight(fastId)) {
                is Outcome.Ok -> { _errors.value = emptyList(); refresh.value++; container.reminders.reschedule() }
                is Outcome.Invalid ->
                    if (ValidationError.PRE_START_REVIEW_PENDING in result.errors) _events.value = HomeEvent.OpenReview(fastId) else _errors.value = result.errors
            }
        }
    }

    /** H-047: ayuno al que ofrecer el peso al empezar, si «Peso» está activo en el catálogo. */
    val startWeightPrompt: StateFlow<String?> = combine(container.startWeightPrompt, container.catalog.observeMetricTypes()) { fastId, types ->
        fastId?.takeIf { types.any { it.code == MetricType.WEIGHT_CODE && it.active } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissStartWeight() { container.startWeightPrompt.value = null }

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
