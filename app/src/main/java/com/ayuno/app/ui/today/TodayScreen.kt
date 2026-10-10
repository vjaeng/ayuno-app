package com.ayuno.app.ui.today

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.DayAgenda
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.PendingItem
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.derived.RefeedSchedule
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.time.LocalTimes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

data class TodayRow(val item: PendingItem, val state: PendingState, val label: String, val detail: String?, val fastHour: Long?)

data class TodayState(
    val fastId: String?,
    val day: LocalDate,
    val rows: List<TodayRow>,
    /** Productos que salen durante el ayuno (sección gris al pie, H-014). */
    val stopped: List<String>,
)

/**
 * H-018: el día natural con lo hecho, lo pendiente, lo atrasado y lo no hecho, del ayuno en curso o, tras
 * terminarlo, del que sigue en vuelta a comer, reintroducción o etapa posterior (H-031).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val container: AppContainer, tickMillis: Long = 60_000) : ViewModel() {
    private val ticker = flow { while (true) { emit(Unit); delay(tickMillis) } }

    val state: StateFlow<TodayState?> = container.fasts.observeFasts()
        .flatMapLatest { fasts -> flow { emit(followed(fasts)) } }
        .flatMapLatest { current ->
            if (current == null) {
                flow { emit(TodayState(null, LocalTimes.now(container.clock).toLocalDate(), emptyList(), emptyList())) }
            } else {
                val changes: List<Flow<Unit>> = listOf(
                    ticker,
                    container.records.observeMeasurements(current.id).map { },
                    container.records.observeBloodPressure(current.id).map { },
                    container.records.observeProductLogs(current.id).map { },
                    container.alerts.observeAlerts(current.id).map { },
                    container.refeed.observeChecks(current.id).map { },
                )
                merge(*changes.toTypedArray()).map { build(current.id) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** El ayuno del día: el que está en curso o el último terminado mientras dure su seguimiento. */
    private suspend fun followed(fasts: List<Fast>): Fast? {
        fasts.firstOrNull { it.status == FastStatus.IN_PROGRESS }?.let { return it }
        val now = LocalTimes.now(container.clock)
        return fasts.filter { it.status == FastStatus.FINISHED }.sortedByDescending { it.endActual }.firstOrNull { fast ->
            val source = container.fasts.get(fast.id) ?: return@firstOrNull false
            RefeedSchedule.followUpEnd(source, container.refeed.checks(fast.id))?.isAfter(now) == true
        }
    }

    private suspend fun build(fastId: String): TodayState {
        val c = container
        val now = LocalTimes.now(c.clock)
        val source = c.fasts.get(fastId)!!
        val types = c.catalog.metricTypes()
        val products = c.catalog.products()
        val measurements = c.records.measurements(fastId)
        val readings = c.records.bloodPressure(fastId)
        val logs = c.records.productLogs(fastId)
        val checks = c.refeed.checks(fastId)
        val steps = source.plan.refeed.associateBy { it.id }
        val agenda = DayAgenda.of(
            source, types.filter { it.active }.map { it.code }.toSet(), products, c.alerts.alerts(fastId),
            PendingRecords(measurements, readings, logs, checks), now.toLocalDate(), now,
        )
        val rows = agenda.map { s ->
            val item = s.item
            val label = when (item.kind) {
                PendingKind.METRIC -> (types.firstOrNull { it.code == item.code }?.name ?: item.code)
                PendingKind.PRODUCT -> products.firstOrNull { it.code == item.code }?.let { "${it.name} · ${Formats.number(item.amount ?: it.defaultAmount)} ${it.unit}" } ?: item.code
                PendingKind.REFEED -> checks.firstOrNull { it.id == item.code }?.let { steps[it.refeedStepId]?.description }.orEmpty()
            }
            val detail = when (item.kind) {
                PendingKind.PRODUCT -> logs.lastOrNull { it.productCode == item.code && it.scheduledAt == item.scheduledAt }?.let { log ->
                    if (log.status == ProductLogStatus.NOT_TAKEN) log.reason else log.takenAt?.let { Formats.time(it) }
                }
                PendingKind.REFEED -> checks.firstOrNull { it.id == item.code }?.doneAt?.let { Formats.time(it) }
                PendingKind.METRIC -> if (item.code == MetricType.BLOOD_PRESSURE_CODE) {
                    readings.lastOrNull { it.scheduledFor == item.scheduledAt || (item.fromRule && it.takenAt.isAfter(item.answeredAfter)) }
                        ?.let { r -> r.notDoneReason ?: "${r.systolic}/${r.diastolic} · ${Formats.time(r.takenAt)}" }
                } else {
                    measurements.lastOrNull { it.metricTypeCode == item.code && (it.scheduledFor == item.scheduledAt || (item.fromRule && it.takenAt.isAfter(item.answeredAfter))) }
                        ?.let { m -> m.notDoneReason ?: "${m.valueEntered?.let { Formats.number(it, 2) } ?: m.valueText.orEmpty()} ${m.unitEntered.orEmpty()} · ${Formats.time(m.takenAt)}" }
                }
            }
            TodayRow(item, s.state, label, detail, FastClock.fastHour(source.fast, item.scheduledAt))
        }
        val stopped = source.plan.products.filter { it.duringFast == DuringFast.STOPS }.mapNotNull { p -> products.firstOrNull { it.code == p.productCode }?.name }
        return TodayState(fastId, now.toLocalDate(), rows, stopped)
    }
}

@Composable
fun TodayScreen(
    onOpen: (fastId: String, item: PendingItem, notDone: Boolean) -> Unit,
    viewModel: TodayViewModel = viewModel(factory = containerFactory { TodayViewModel(it) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.tab_today), onBack = null) {
        val s = state ?: run { LoadingState(); return@ScreenScaffold }
        Text(Formats.date(s.day.atStartOfDay()), style = MaterialTheme.typography.titleLarge)
        val fastId = s.fastId
        if (fastId == null) { EmptyState(stringResource(R.string.today_no_fast)); return@ScreenScaffold }
        if (s.rows.isEmpty()) EmptyState(stringResource(R.string.today_empty))
        // Agrupado por franjas para no superar 9 filas por bloque (Miller); las filas ya llegan en orden de hora.
        s.rows.groupBy { slotOf(it.item.scheduledAt) }.forEach { (slot, rows) ->
            SectionTitle(stringResource(slot))
            rows.forEach { row -> Row(row, onOpen = { notDone -> onOpen(fastId, row.item, notDone) }) }
        }
        if (s.stopped.isNotEmpty()) {
            SectionTitle(stringResource(R.string.today_stopped_title))
            s.stopped.forEach { Text(it, style = MaterialTheme.typography.bodyLarge, color = AyunoThemeTokens.colors.onSurfaceVariant) }
        }
    }
}

private fun slotOf(at: LocalDateTime): Int = when (at.hour) {
    in 0..11 -> R.string.today_morning
    in 12..18 -> R.string.today_afternoon
    else -> R.string.today_evening
}

@Composable
private fun Row(row: TodayRow, onOpen: (notDone: Boolean) -> Unit) {
    // Estado con icono y texto, nunca solo con color (GUIA-ESTILO, FilaPendiente).
    val (mark, stateText) = when (row.state) {
        PendingState.DONE -> "✔" to stringResource(R.string.today_done)
        PendingState.PENDING -> "○" to stringResource(R.string.today_pending)
        PendingState.LATE -> "!" to stringResource(R.string.today_late)
        PendingState.NOT_DONE -> "✖" to stringResource(R.string.today_not_done)
    }
    // Un paso de reintroducción es de fecha, sin hora.
    val time = if (row.item.allDay) {
        stringResource(R.string.today_all_day)
    } else {
        Formats.time(row.item.scheduledAt) + (row.fastHour?.let { " · " + stringResource(R.string.fast_hour, it) } ?: "")
    }
    val title = "$mark $time  ${row.label}" + (if (row.item.fromRule) " " + stringResource(R.string.today_from_rule) else "")
    val supporting = listOfNotNull(stateText, row.detail).joinToString(" · ")
    val open = row.state == PendingState.PENDING || row.state == PendingState.LATE
    val refeed = row.item.kind == PendingKind.REFEED
    ListRow(title, supporting, onClick = { if (open || refeed) onOpen(false) }) {
        if (open && !refeed) TextButton(onClick = { onOpen(true) }, modifier = Modifier.heightIn(min = Dimens.touchMin)) { Text(stringResource(R.string.record_not_done)) }
    }
}
