package com.ayuno.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayuno.app.di.AppContainer
import com.ayuno.domain.derived.DailyTotal
import com.ayuno.domain.derived.DailyTotals
import com.ayuno.domain.derived.Pending
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.derived.PendingRecords
import com.ayuno.domain.derived.PendingState
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordIntake
import com.ayuno.domain.usecase.RecordProductLog
import com.ayuno.domain.usecase.RecordResult
import com.ayuno.domain.usecase.RecordSymptom
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Resultado común de los formularios de registro sin rango que confirmar. */
sealed interface SimpleSave {
    data object Idle : SimpleSave
    data object Saving : SimpleSave
    data class Invalid(val errors: List<ValidationError>) : SimpleSave
    data class Saved(val alerts: List<Alert> = emptyList(), val endFastAt: LocalDateTime? = null) : SimpleSave
}

abstract class SimpleRecordViewModel(protected val container: AppContainer) : ViewModel() {
    protected val _save = MutableStateFlow<SimpleSave>(SimpleSave.Idle)
    val save: StateFlow<SimpleSave> = _save.asStateFlow()

    fun now(): LocalDateTime = LocalTimes.now(container.clock)
    fun reset() { _save.value = SimpleSave.Idle }

    protected fun <T> run(block: suspend () -> Outcome<T>, onOk: suspend (T) -> SimpleSave) {
        _save.value = SimpleSave.Saving
        viewModelScope.launch {
            _save.value = when (val r = block()) {
                is Outcome.Invalid -> SimpleSave.Invalid(r.errors)
                is Outcome.Ok -> { container.reminders.reschedule(); onOk(r.value) }
            }
        }
    }
}

/** H-010 */
class SymptomViewModel(container: AppContainer) : SimpleRecordViewModel(container) {
    val types: StateFlow<List<SymptomType>?> = container.catalog.observeSymptomTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun record(input: RecordSymptom.Input) = run({ container.recordSymptom(input) }) { r -> SimpleSave.Saved((r as RecordResult.Saved).alerts) }
}

/** H-012: si el tipo «rompe el ayuno» y el ayuno está en curso, se pregunta si se está terminando. */
class IntakeViewModel(container: AppContainer, private val fastId: String) : SimpleRecordViewModel(container) {
    val types: StateFlow<List<IntakeType>?> = container.catalog.observeIntakeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val totals: StateFlow<List<DailyTotal>> = combine(container.records.observeIntakes(fastId), container.catalog.observeIntakeTypes()) { intakes, types ->
        DailyTotals.of(now().toLocalDate(), intakes, types, container.fasts.get(fastId)?.plan?.goals.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun record(input: RecordIntake.Input) = run({ container.recordIntake(input) }) { r ->
        val saved = r as RecordResult.Saved
        val type = container.catalog.intakeTypes().firstOrNull { it.code == input.intakeTypeCode }
        val inProgress = container.fasts.get(fastId)?.fast?.status == FastStatus.IN_PROGRESS
        SimpleSave.Saved(saved.alerts, endFastAt = saved.entry.takenAt.takeIf { type?.breaksFast == true && inProgress })
    }
}

data class ProductChoice(val product: Product, val plan: PlanProduct?, val pendingAt: LocalDateTime?, val pendingAmount: Double?)

/** H-013 (a evitar), H-014 (sigue o sale), H-015 (tomas). */
class ProductViewModel(container: AppContainer, private val fastId: String, private val scheduledFor: LocalDateTime?) : SimpleRecordViewModel(container) {
    private val _choices = MutableStateFlow<List<ProductChoice>?>(null)
    val choices: StateFlow<List<ProductChoice>?> = _choices.asStateFlow()

    init {
        viewModelScope.launch {
            val source = container.fasts.get(fastId)
            val products = container.catalog.products().filter { it.active }
            val now = now()
            val records = PendingRecords(productLogs = container.records.productLogs(fastId))
            val pendings = source?.let {
                Pending.items(it, emptySet(), products, now.toLocalDate().atStartOfDay(), now.toLocalDate().plusDays(1).atStartOfDay())
                    .filter { i -> i.kind == PendingKind.PRODUCT }
                    .map { i -> Pending.status(i, now, records) }
                    .filter { st -> st.state == PendingState.PENDING || st.state == PendingState.LATE }
                    .map { st -> st.item }
            }.orEmpty()
            _choices.value = products.map { p ->
                val pending = pendings.firstOrNull { it.code == p.code && (scheduledFor == null || it.scheduledAt == scheduledFor) }
                ProductChoice(p, source?.plan?.products?.firstOrNull { it.productCode == p.code }, pending?.scheduledAt, pending?.amount)
            }.sortedWith(compareBy({ it.pendingAt == null }, { it.pendingAt }, { it.product.name }))
        }
    }

    fun record(input: RecordProductLog.Input) = run({ container.recordProductLog(input) }) { SimpleSave.Saved() }
}

/** H-035 */
class NoteViewModel(container: AppContainer) : SimpleRecordViewModel(container) {
    fun record(fastId: String, at: LocalDateTime, text: String) = run({ container.recordNote(fastId, at, text) }) { SimpleSave.Saved() }
}
