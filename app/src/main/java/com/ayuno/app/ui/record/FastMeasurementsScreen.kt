package com.ayuno.app.ui.record

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.FilterChips
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.FastMeasurements
import com.ayuno.domain.derived.MeasurementItem
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Trend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MeasurementsUi {
    data object Loading : MeasurementsUi
    data object Error : MeasurementsUi
    data class Loaded(
        val fast: Fast,
        val items: List<MeasurementItem>,
        val deleted: List<MeasurementItem>,
        val names: Map<String, String>,
    ) : MeasurementsUi
}

/** H-048: mediciones y presiones visibles del ayuno, las borradas aparte y la marca «corregida». */
class FastMeasurementsViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<MeasurementsUi> = combine(
        combine(container.records.observeMeasurements(fastId), container.records.observeBloodPressure(fastId), ::Pair),
        combine(container.records.observeDeletedMeasurements(fastId), container.records.observeDeletedBloodPressure(fastId), ::Pair),
        container.audit.observeEditedRowIds(EntryKind.entries.map { it.entityName }),
        container.catalog.observeMetricTypes(),
        retry,
    ) { visible, deleted, edited, types, _ ->
        val fast = container.fasts.get(fastId)?.fast ?: return@combine MeasurementsUi.Error
        MeasurementsUi.Loaded(
            fast = fast,
            items = FastMeasurements.of(visible.first, visible.second, edited),
            deleted = FastMeasurements.of(deleted.first, deleted.second, edited),
            names = types.associate { it.code to it.name },
        ) as MeasurementsUi
    }
        .catch { emit(MeasurementsUi.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementsUi.Loading)

    fun retry() { retry.value++ }

    fun restore(item: MeasurementItem) {
        viewModelScope.launch {
            container.setEntryDeleted(item.kind, item.id, deleted = false)
            container.reminders.reschedule()
        }
    }
}

/** H-048: «Mediciones de este ayuno» (UX-UI historial-revisar.md). */
@Composable
fun FastMeasurementsScreen(
    fastId: String,
    onBack: () -> Unit,
    onOpen: (kind: EntryKind, id: String) -> Unit,
    onRecord: (fastId: String) -> Unit,
) {
    val viewModel: FastMeasurementsViewModel = viewModel(key = "measurements-$fastId", factory = containerFactory { FastMeasurementsViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(stringResource(R.string.measurements_title), onBack) {
        when (val s = state) {
            MeasurementsUi.Loading -> LoadingState()
            MeasurementsUi.Error -> ErrorState(stringResource(R.string.measurements_error), viewModel::retry)
            is MeasurementsUi.Loaded -> {
                val start = Formats.date(FastClock.effectiveStart(s.fast))
                val subtitle = if (s.fast.status == FastStatus.IN_PROGRESS) R.string.measurements_since else R.string.measurements_finished
                Text(stringResource(subtitle, start), style = MaterialTheme.typography.bodyLarge)
                if (s.items.isEmpty()) {
                    val canRecord = s.fast.status == FastStatus.IN_PROGRESS
                    EmptyState(stringResource(R.string.measurements_empty), stringResource(R.string.home_record).takeIf { canRecord }) { onRecord(fastId) }
                } else {
                    val types = FastMeasurements.types(s.items)
                    // Un filtro que ya no tiene filas (p. ej. tras borrar la última) vuelve a «Todas».
                    val active = filter?.takeIf { it in types }
                    FilterChips(
                        options = types.map { it to (s.names[it] ?: it) },
                        selected = active,
                        allLabel = stringResource(R.string.measurements_filter_all),
                        onSelect = { filter = it },
                        description = stringResource(R.string.measurements_filter_label),
                    )
                    FastMeasurements.byDay(FastMeasurements.filter(s.items, active)).forEach { (day, items) ->
                        SectionTitle(Formats.date(day.atStartOfDay()))
                        items.forEach { item ->
                            ListRow(
                                title = measurementTitle(item, s.names),
                                supporting = measurementSupporting(s.fast, item),
                                onClick = { onOpen(item.kind, item.id) },
                                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            )
                        }
                    }
                }
                if (s.deleted.isNotEmpty()) {
                    ListRow(stringResource(R.string.measurements_deleted, s.deleted.size), null, onClick = { showDeleted = !showDeleted })
                    if (showDeleted) {
                        SectionTitle(stringResource(R.string.measurements_deleted_title))
                        s.deleted.forEach { item ->
                            ListRow(measurementTitle(item, s.names), measurementSupporting(s.fast, item), onClick = {}) {
                                TextButton(onClick = { viewModel.restore(item) }, modifier = Modifier.heightIn(min = Dimens.touchMin)) {
                                    Text(stringResource(R.string.measurements_restore))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tipo y valor tecleado con su unidad; «no se hizo» si no hay valor. */
@Composable
fun measurementTitle(item: MeasurementItem, names: Map<String, String>): String {
    val name = names[item.metricTypeCode] ?: item.metricTypeCode
    val notDone = stringResource(R.string.measurements_not_done)
    return when (item) {
        is MeasurementItem.OfMeasurement -> {
            val m = item.value
            if (m.notDoneReason != null) "$name · $notDone" else "$name ${m.valueEntered?.let { Formats.number(it, 2) } ?: m.valueText.orEmpty()} ${m.unitEntered.orEmpty()}".trim()
        }
        is MeasurementItem.OfBloodPressure -> {
            val r = item.value
            if (r.notDoneReason != null) {
                "$name · $notDone"
            } else {
                "$name ${r.systolic}/${r.diastolic}" + (r.pulse?.let { " · " + stringResource(R.string.history_pulse, it) } ?: "")
            }
        }
    }
}

/** Hora real, hora del ayuno y detalles (origen, tendencia, posición, al despertar, motivo, corregida). */
@Composable
fun measurementSupporting(fast: Fast, item: MeasurementItem): String {
    val hour = FastClock.fastHour(fast, item.at)?.let { stringResource(R.string.fast_hour, it) }
    val details = when (item) {
        is MeasurementItem.OfMeasurement -> listOfNotNull(
            item.value.source?.let { sourceText(it) },
            item.value.trend?.let { trendText(it) }?.lowercase(),
            stringResource(R.string.measurements_on_waking).takeIf { item.value.onWaking },
            item.value.notDoneReason?.let { "«$it»" },
        )
        is MeasurementItem.OfBloodPressure -> listOfNotNull(
            positionText(item.value.position),
            stringResource(R.string.measurements_on_waking).takeIf { item.value.onWaking },
            item.value.notDoneReason?.let { "«$it»" },
        )
    }
    val edited = stringResource(R.string.measurements_edited).takeIf { item.edited }?.let { "✎ $it" }
    return (listOfNotNull(Formats.time(item.at), hour) + details + listOfNotNull(edited)).joinToString(" · ")
}

@Composable
fun sourceText(source: GlucoseSource): String = stringResource(if (source == GlucoseSource.CGM) R.string.source_cgm else R.string.source_glucometer)

@Composable
fun trendText(trend: Trend): String = stringResource(
    when (trend) {
        Trend.RISING_FAST -> R.string.trend_rising_fast
        Trend.RISING -> R.string.trend_rising
        Trend.STEADY -> R.string.trend_steady
        Trend.FALLING -> R.string.trend_falling
        Trend.FALLING_FAST -> R.string.trend_falling_fast
    },
)

@Composable
fun positionText(position: Position): String = stringResource(
    when (position) {
        Position.SITTING -> R.string.position_sitting
        Position.STANDING -> R.string.position_standing
        Position.LYING -> R.string.position_lying
    },
)
