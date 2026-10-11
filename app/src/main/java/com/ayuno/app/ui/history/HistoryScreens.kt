package com.ayuno.app.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.window.DialogProperties
import com.ayuno.app.ui.components.FieldError
import com.ayuno.domain.usecase.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.home.endReasonText
import com.ayuno.app.ui.home.totalText
import com.ayuno.app.ui.refeed.toleranceText
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.DailyTotal
import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.derived.FastRecords
import com.ayuno.domain.derived.Timeline
import com.ayuno.domain.derived.TimelineEntry
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.UserChoice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Un ayuno de la lista con el nombre de la regla que lo terminó, si fue por una regla. */
data class HistoryItem(val fast: Fast, val endRuleName: String?)

sealed interface HistoryState {
    data object Loading : HistoryState
    data object Error : HistoryState
    data class Loaded(val items: List<HistoryItem>) : HistoryState
}

/** H-033: en curso primero y luego los terminados del más reciente al más antiguo; los planificados viven en su pestaña (EV-002). */
class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<HistoryState> = container.fasts.observeFasts()
        .map { fasts ->
            val order = compareBy<Fast>({ listOf(FastStatus.IN_PROGRESS, FastStatus.FINISHED).indexOf(it.status) })
                .thenByDescending { FastClock.effectiveStart(it) }
            HistoryState.Loaded(
                fasts.filter { it.status != FastStatus.PLANNED }.sortedWith(order).map { fast ->
                    val rule = fast.endRuleId?.let { id -> container.fasts.get(fast.id)?.ruleById(id)?.name }
                    HistoryItem(fast, rule)
                },
            ) as HistoryState
        }
        .catch { emit(HistoryState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState.Loading)
}

@Composable
fun HistoryScreen(onOpen: (fastId: String) -> Unit, onPlanned: () -> Unit) {
    val viewModel: HistoryViewModel = viewModel(factory = containerFactory { HistoryViewModel(it) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.tab_history), onBack = null) {
        when (val s = state) {
            HistoryState.Loading -> LoadingState()
            HistoryState.Error -> ErrorState(stringResource(R.string.error_load)) { }
            is HistoryState.Loaded -> {
                if (s.items.isEmpty()) EmptyState(stringResource(R.string.history_empty), stringResource(R.string.planned_open_tab), onPlanned)
                s.items.forEach { item -> ListRow(historyTitle(item.fast), historySupporting(item), onClick = { onOpen(item.fast.id) }) }
            }
        }
    }
}

@Composable
private fun historyTitle(fast: Fast): String = when (fast.status) {
    FastStatus.IN_PROGRESS -> stringResource(R.string.history_in_progress, Formats.date(FastClock.effectiveStart(fast)))
    FastStatus.PLANNED -> Formats.date(fast.lastMealPlanned)
    FastStatus.FINISHED -> Formats.date(FastClock.effectiveStart(fast)) + (FastClock.finalDuration(fast)?.let { " · " + Formats.duration(it) } ?: "")
}

@Composable
private fun historySupporting(item: HistoryItem): String {
    val target = stringResource(R.string.history_target, Formats.number(item.fast.targetHours, 2))
    val reason = item.fast.endReason?.let { endText(it, item.fast.endReasonDetail, item.endRuleName) }
    return listOfNotNull(target, reason).joinToString(" · ")
}

@Composable
private fun endText(reason: EndReason, detail: String?, ruleName: String?): String =
    if (reason == EndReason.ALERT_RULE && ruleName != null) stringResource(R.string.history_end_rule, ruleName) else endReasonText(reason, detail)

/** Un día de la línea de tiempo con sus totales de ingesta. */
data class TimelineDay(val day: LocalDate, val totals: List<DailyTotal>, val entries: List<TimelineEntry>)

data class FastDetail(
    val source: FastWithPlan,
    val endRuleName: String?,
    val days: List<TimelineDay>,
    val metricNames: Map<String, String>,
    val symptomNames: Map<String, String>,
    val intakeNames: Map<String, String>,
    val productNames: Map<String, String>,
    /** Derivado «recuento de registros del ayuno» (MODELO-DATOS §2.5) para la confirmación de borrado (H-045). */
    val recordCount: Int,
)

/** H-033: línea de tiempo del ayuno por días (H-027 alertas, H-035 notas, H-031/H-032 vuelta a comer). */
class FastDetailViewModel(private val container: AppContainer, private val fastId: String) : ViewModel() {
    val state: StateFlow<FastDetail?> = container.fasts.observeFasts().map { load() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun load(): FastDetail? {
        val c = container
        val source = c.fasts.get(fastId) ?: return null
        val records = FastRecords(
            measurements = c.records.measurements(fastId),
            readings = c.records.bloodPressure(fastId),
            symptoms = c.records.symptoms(fastId),
            intakes = c.records.intakes(fastId),
            productLogs = c.records.productLogs(fastId),
            alerts = c.alerts.alerts(fastId),
            notes = c.records.notes(fastId),
            refeedChecks = c.refeed.checks(fastId),
            meals = c.refeed.meals(fastId),
        )
        val intakeTypes = c.catalog.intakeTypes()
        val days = Timeline.byDay(Timeline.of(source.fast, source.plan.refeed, records)).map { (day, entries) ->
            TimelineDay(day, DailyTotals.of(day, records.intakes, intakeTypes, emptyList()), entries)
        }
        return FastDetail(
            source = source,
            endRuleName = source.ruleById(source.fast.endRuleId)?.name,
            days = days,
            metricNames = c.catalog.metricTypes().associate { it.code to it.name },
            symptomNames = c.catalog.symptomTypes().associate { it.code to it.name },
            intakeNames = intakeTypes.associate { it.code to it.name },
            productNames = c.catalog.products().associate { it.code to it.name },
            recordCount = records.count(),
        )
    }

    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed: StateFlow<Boolean> = _deleteFailed.asStateFlow()

    /** H-045: borrado definitivo; después se retiran sus avisos y se recalculan los recordatorios. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (container.deleteFastAndReminders(fastId)) onDeleted() else _deleteFailed.value = true
        }
    }
}

@Composable
fun FastDetailScreen(fastId: String, onBack: () -> Unit, onExport: () -> Unit = {}, onDeleted: () -> Unit = onBack) {
    val viewModel: FastDetailViewModel = viewModel(key = "detail-$fastId", factory = containerFactory { FastDetailViewModel(it, fastId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleteFailed by viewModel.deleteFailed.collectAsStateWithLifecycle()
    var confirmingDelete by remember { mutableStateOf(false) }
    ScreenScaffold(
        stringResource(R.string.history_detail_title),
        onBack,
        actions = {
            if (state != null) FastMenu(onChange = null, onDelete = { confirmingDelete = true })
        },
    ) {
        val d = state ?: run { LoadingState(); return@ScreenScaffold }
        val fast = d.source.fast
        val start = FastClock.effectiveStart(fast)
        val range = fast.endActual?.let { stringResource(R.string.history_range, Formats.date(start), Formats.date(it)) } ?: Formats.date(start)
        Text(range, style = MaterialTheme.typography.titleLarge)
        if (deleteFailed) FieldError(stringResource(R.string.error_delete_fast))
        val summary = listOfNotNull(
            FastClock.finalDuration(fast)?.let { Formats.duration(it) },
            stringResource(R.string.history_target, Formats.number(fast.targetHours, 2)),
            fast.endReason?.let { endText(it, fast.endReasonDetail, d.endRuleName) },
        ).joinToString(" · ")
        Text(summary, style = MaterialTheme.typography.bodyLarge)
        fast.notes?.let { Text(stringResource(R.string.history_notes, it), style = MaterialTheme.typography.bodyLarge) }
        SecondaryButton(stringResource(R.string.export_action), onClick = onExport)
        if (d.days.isEmpty()) EmptyState(stringResource(R.string.history_no_entries))
        d.days.forEach { day ->
            SectionTitle(Formats.date(day.day.atStartOfDay()))
            if (day.totals.isNotEmpty()) {
                Text(stringResource(R.string.history_totals, day.totals.map { totalText(it) }.joinToString(" · ")), style = MaterialTheme.typography.bodyLarge)
            }
            day.entries.forEach { TimelineItem(fast, it, d) }
        }
    }

    val d = state
    if (confirmingDelete && d != null) {
        DeleteFastDialog(d.source.fast, d.recordCount, onDismiss = { confirmingDelete = false }, onConfirm = { confirmingDelete = false; viewModel.delete(onDeleted) })
    }
}

/** H-045: confirmación «Borrar para siempre» con el recuento de registros; sin rojo (GUIA-ESTILO §2). */
@Composable
fun DeleteFastDialog(fast: Fast, recordCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val start = FastClock.effectiveStart(fast)
    val range = fast.endActual?.let { stringResource(R.string.history_range, Formats.date(start), Formats.date(it)) } ?: Formats.date(start)
    val question = if (recordCount == 0) {
        stringResource(R.string.fast_delete_question_empty, range)
    } else {
        pluralStringResource(R.plurals.fast_delete_question, recordCount, range, recordCount)
    }
    // DialogoConfirmacion: la acción segura (Cancelar) a la derecha.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.fast_delete_title)) },
        text = {
            Text(question + if (fast.status == FastStatus.IN_PROGRESS) "\n" + stringResource(R.string.fast_delete_in_progress) else "")
        },
        dismissButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.fast_delete_confirm)) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** H-045: borra el ayuno, retira sus avisos y recalcula los recordatorios; `false` si no se pudo borrar. */
suspend fun AppContainer.deleteFastAndReminders(fastId: String): Boolean {
    val result = runCatching { deleteFast(fastId) }.getOrNull()
    if (result !is Outcome.Ok) return false
    reminders.cancelNotificationsFor(fastId)
    reminders.reschedule()
    return true
}

/** Derivado «recuento de registros del ayuno» (MODELO-DATOS §2.5). */
fun FastRecords.count(): Int = measurements.size + readings.size + symptoms.size + intakes.size + productLogs.size + alerts.size + notes.size + meals.size

/** Registros del ayuno que cuentan para la confirmación de borrado (H-045). */
suspend fun AppContainer.recordCount(fastId: String): Int = FastRecords(
    measurements = records.measurements(fastId),
    readings = records.bloodPressure(fastId),
    symptoms = records.symptoms(fastId),
    intakes = records.intakes(fastId),
    productLogs = records.productLogs(fastId),
    alerts = alerts.alerts(fastId),
    notes = records.notes(fastId),
    refeedChecks = emptyList(),
    meals = refeed.meals(fastId),
).count()

/** MenuAcciones del detalle: «Cambiar» (H-044, solo con [onChange]) y «Borrar ayuno» (H-045). */
@Composable
fun FastMenu(onChange: (() -> Unit)?, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(Dimens.touchMin)) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.fast_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (onChange != null) DropdownMenuItem(text = { Text(stringResource(R.string.fast_change)) }, onClick = { open = false; onChange() })
            DropdownMenuItem(text = { Text(stringResource(R.string.fast_delete)) }, onClick = { open = false; onDelete() })
        }
    }
}

/** ItemLineaTiempo: hora + hora del ayuno + tipo + valor. */
@Composable
private fun TimelineItem(fast: Fast, entry: TimelineEntry, d: FastDetail) {
    val hour = FastClock.fastHour(fast, entry.at)?.let { " · " + stringResource(R.string.fast_hour, it) }.orEmpty()
    Column(Modifier.fillMaxWidth().heightIn(min = Dimens.touchMin).padding(vertical = Dimens.spaceXs).semantics(mergeDescendants = true) {}) {
        Text(Formats.time(entry.at) + hour, style = MaterialTheme.typography.bodyMedium)
        Text(entryText(entry, d), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun entryText(entry: TimelineEntry, d: FastDetail): String {
    val notDone = stringResource(R.string.record_not_done)
    return when (entry) {
        is TimelineEntry.OfMeasurement -> {
            val m = entry.value
            val name = d.metricNames[m.metricTypeCode] ?: m.metricTypeCode
            m.notDoneReason?.let { "$name · $notDone · $it" }
                ?: "$name ${m.valueEntered?.let { Formats.number(it, 2) } ?: m.valueText.orEmpty()} ${m.unitEntered.orEmpty()}".trim()
        }
        is TimelineEntry.OfBloodPressure -> {
            val r = entry.value
            val name = d.metricNames[com.ayuno.domain.model.MetricType.BLOOD_PRESSURE_CODE].orEmpty()
            r.notDoneReason?.let { "$name · $notDone · $it" }
                ?: "$name ${r.systolic}/${r.diastolic}" + (r.pulse?.let { " · ${stringResource(R.string.history_pulse, it)}" } ?: "") + " · " + positionText(r.position)
        }
        is TimelineEntry.OfSymptom -> "${d.symptomNames[entry.value.symptomTypeCode] ?: entry.value.symptomTypeCode} ${entry.value.severity}/10"
        is TimelineEntry.OfIntake -> "${d.intakeNames[entry.value.intakeTypeCode] ?: entry.value.intakeTypeCode} ${Formats.number(entry.value.amount, 2)} ${entry.value.unit}"
        is TimelineEntry.OfProductLog -> {
            val log = entry.value
            val name = d.productNames[log.productCode] ?: log.productCode
            when (log.status) {
                ProductLogStatus.TAKEN -> "$name · ${stringResource(R.string.product_taken)}" + (log.amount?.let { " · ${Formats.number(it, 2)} ${log.unit.orEmpty()}" } ?: "")
                ProductLogStatus.NOT_TAKEN -> "$name · ${stringResource(R.string.product_not_taken)}" + (log.reason?.let { " · $it" } ?: "")
                ProductLogStatus.DOSE_CHANGED -> "$name · ${stringResource(R.string.product_other_dose)} · ${Formats.number(log.amount ?: 0.0, 2)} ${log.unit.orEmpty()}" + (log.reason?.let { " · $it" } ?: "")
            }
        }
        is TimelineEntry.OfAlert -> {
            val rule = d.source.ruleById(entry.value.ruleId)
            val level = stringResource(if (rule?.level == RuleLevel.NOTICE) R.string.level_notice else R.string.level_alert)
            val choice = when (entry.value.userChoice) {
                UserChoice.END_FAST -> stringResource(R.string.alert_end_fast)
                UserChoice.CONTINUE -> stringResource(R.string.alert_continue) + (entry.value.choiceReason?.let { ": $it" } ?: "")
                null -> stringResource(R.string.history_alert_undecided)
            }
            "${if (rule?.level == RuleLevel.NOTICE) "▲" else "■"} $level · ${rule?.name.orEmpty()} · $choice"
        }
        is TimelineEntry.OfNote -> stringResource(R.string.history_note, entry.value.text)
        is TimelineEntry.OfRefeed -> stringResource(R.string.history_refeed_step, entry.step?.description.orEmpty())
        is TimelineEntry.OfMeal -> stringResource(R.string.history_meal, entry.value.description, toleranceText(entry.value.tolerance, entry.value.toleranceDetail))
    }
}

@Composable
private fun positionText(position: Position): String = stringResource(
    when (position) {
        Position.SITTING -> R.string.position_sitting
        Position.STANDING -> R.string.position_standing
        Position.LYING -> R.string.position_lying
    },
).lowercase()
