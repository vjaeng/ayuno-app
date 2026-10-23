package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.Coded
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Trend
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.units.UnitConversions
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime

/** Un campo cambiado: nombre canónico y valores serializados para `edit_log`. */
private data class FieldChange(val field: String, val old: String?, val new: String?)

private fun text(value: Any?): String? = when (value) {
    null -> null
    is Double -> BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
    is LocalDateTime -> LocalTimes.format(value)
    is Coded -> value.code
    else -> value.toString()
}

private fun changes(vararg pairs: Pair<String, Pair<Any?, Any?>>): List<FieldChange> =
    pairs.filter { (_, v) -> v.first != v.second }.map { (field, v) -> FieldChange(field, text(v.first), text(v.second)) }

private suspend fun AuditRepository.log(kind: EntryKind, rowId: String, changes: List<FieldChange>, reason: String?, clock: Clock) {
    val note = reason?.trim()?.ifEmpty { null }
    changes.forEach { append(editEntry(rowId, it.field, it.old, it.new, note, clock, entityName = kind.entityName)) }
}

/**
 * H-034, H-048 (D-20): corregir una medición. Conserva el id, recalcula `value_num` y deja una fila de `edit_log`
 * por campo cambiado; con el ayuno en curso vuelve a evaluar las reglas en la misma transacción. Una «no se hizo»
 * solo corrige su hora y su motivo.
 */
class CorrectMeasurement(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    private val recorder = AlertRecorder(catalog, records, alerts, clock)

    data class Input(
        val id: String,
        val takenAt: LocalDateTime,
        val valueEntered: Double? = null,
        val unitEntered: String? = null,
        val valueText: String? = null,
        val source: GlucoseSource? = null,
        val trend: Trend? = null,
        val onWaking: Boolean = false,
        val note: String? = null,
        val notDoneReason: String? = null,
        val reason: String? = null,
        val confirmedOutOfRange: Boolean = false,
        /** H-049: palabra elegida; su número sustituye a [valueEntered]. */
        val metricOptionId: String? = null,
    )

    suspend operator fun invoke(request: Input): Outcome<RecordResult<Measurement>> = transactions.inTransaction {
        val old = records.measurement(request.id) ?: return@inTransaction invalid(ValidationError.ENTRY_NOT_FOUND)
        val type = catalog.metricType(old.metricTypeCode) ?: return@inTransaction invalid(ValidationError.METRIC_NOT_FOUND)
        val input = request.metricOptionId?.let { id ->
            val option = type.options.firstOrNull { it.id == id } ?: return@inTransaction invalid(ValidationError.OPTION_NOT_FOUND)
            request.copy(valueEntered = option.value, unitEntered = type.entryUnit)
        } ?: request
        val takenAt = input.takenAt.withNano(0)
        val notDone = input.notDoneReason?.trim()?.ifEmpty { null }
        val hasValue = if (type.isNumeric) input.valueEntered != null else !input.valueText.isNullOrBlank()
        val corrected = if (old.notDoneReason != null) {
            if (hasValue) return@inTransaction invalid(ValidationError.FIELD_NOT_EDITABLE)
            old.copy(takenAt = takenAt, notDoneReason = notDone ?: return@inTransaction invalid(ValidationError.REASON_REQUIRED))
        } else {
            if (notDone != null) return@inTransaction invalid(ValidationError.FIELD_NOT_EDITABLE)
            if (!hasValue) return@inTransaction invalid(ValidationError.VALUE_REQUIRED)
            var valueNum: Double? = null
            var unit: String? = null
            if (type.isNumeric) {
                unit = input.unitEntered ?: type.entryUnit
                if (unit != type.unit && unit != type.altUnit) return@inTransaction invalid(ValidationError.UNIT_NOT_ALLOWED)
                valueNum = UnitConversions.convert(input.valueEntered!!, unit, type.unit)
                if (type.isGlucose && input.source == null) return@inTransaction invalid(ValidationError.SOURCE_REQUIRED)
                // Solo se pide confirmar si el valor cambia: el que ya estaba se confirmó al anotarlo.
                if (valueNum != old.valueNum && type.isOutOfRange(valueNum) && !input.confirmedOutOfRange) {
                    return@inTransaction Outcome.Ok(RecordResult.NeedsRangeConfirmation(listOf(type.code)))
                }
            }
            old.copy(
                takenAt = takenAt,
                valueNum = valueNum,
                valueText = input.valueText?.trim().takeIf { !type.isNumeric },
                valueEntered = input.valueEntered.takeIf { type.isNumeric },
                unitEntered = unit,
                source = input.source.takeIf { type.isGlucose },
                trend = input.trend.takeIf { type.isGlucose },
                onWaking = input.onWaking,
                note = input.note?.trim()?.ifEmpty { null },
            )
        }
        val changed = changes(
            "taken_at" to (old.takenAt to corrected.takenAt),
            "value_entered" to (old.valueEntered to corrected.valueEntered),
            "unit_entered" to (old.unitEntered to corrected.unitEntered),
            "value_num" to (old.valueNum to corrected.valueNum),
            "value_text" to (old.valueText to corrected.valueText),
            "source" to (old.source to corrected.source),
            "trend" to (old.trend to corrected.trend),
            "on_waking" to (old.onWaking to corrected.onWaking),
            "note" to (old.note to corrected.note),
            "not_done_reason" to (old.notDoneReason to corrected.notDoneReason),
        )
        if (changed.isEmpty()) return@inTransaction Outcome.Ok(RecordResult.Saved(old, emptyList()))
        val source = fasts.get(old.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        records.updateMeasurement(corrected)
        audit.log(EntryKind.MEASUREMENT, old.id, changed, input.reason, clock)
        val fired = if (source.fast.status == FastStatus.IN_PROGRESS) recorder.evaluate(source) else emptyList()
        Outcome.Ok(RecordResult.Saved(corrected, fired))
    }
}

/**
 * H-034, H-048 (D-20): corregir una toma de presión. El vínculo de pie ↔ sentada y la hora programada no se editan;
 * por eso tampoco la posición de una toma vinculada.
 */
class CorrectBloodPressure(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    private val recorder = AlertRecorder(catalog, records, alerts, clock)

    data class Input(
        val id: String,
        val takenAt: LocalDateTime,
        val systolic: Int? = null,
        val diastolic: Int? = null,
        val pulse: Int? = null,
        val position: Position,
        val onWaking: Boolean = false,
        val note: String? = null,
        val notDoneReason: String? = null,
        val reason: String? = null,
        val confirmedOutOfRange: Boolean = false,
    )

    suspend operator fun invoke(input: Input): Outcome<RecordResult<BloodPressureReading>> = transactions.inTransaction {
        val old = records.bloodPressureReading(input.id) ?: return@inTransaction invalid(ValidationError.ENTRY_NOT_FOUND)
        val takenAt = input.takenAt.withNano(0)
        val notDone = input.notDoneReason?.trim()?.ifEmpty { null }
        val hasValue = input.systolic != null || input.diastolic != null || input.pulse != null
        val paired = old.pairedReadingId != null || records.bloodPressure(old.fastId).any { it.pairedReadingId == old.id }
        if (input.position != old.position && paired) return@inTransaction invalid(ValidationError.FIELD_NOT_EDITABLE)
        val corrected = if (old.notDoneReason != null) {
            if (hasValue) return@inTransaction invalid(ValidationError.FIELD_NOT_EDITABLE)
            old.copy(takenAt = takenAt, notDoneReason = notDone ?: return@inTransaction invalid(ValidationError.REASON_REQUIRED))
        } else {
            if (notDone != null) return@inTransaction invalid(ValidationError.FIELD_NOT_EDITABLE)
            val systolic = input.systolic ?: return@inTransaction invalid(ValidationError.VALUE_REQUIRED)
            val diastolic = input.diastolic ?: return@inTransaction invalid(ValidationError.VALUE_REQUIRED)
            if (diastolic >= systolic) return@inTransaction invalid(ValidationError.DIASTOLIC_NOT_BELOW_SYSTOLIC)
            val ranges = catalog.bloodPressureRanges()
            val outside = buildList {
                if (systolic != old.systolic && systolic.toDouble() !in ranges.systolic) add("systolic")
                if (diastolic != old.diastolic && diastolic.toDouble() !in ranges.diastolic) add("diastolic")
                if (input.pulse != null && input.pulse != old.pulse && input.pulse.toDouble() !in ranges.pulse) add("pulse")
            }
            if (outside.isNotEmpty() && !input.confirmedOutOfRange) {
                return@inTransaction Outcome.Ok(RecordResult.NeedsRangeConfirmation(outside))
            }
            old.copy(
                takenAt = takenAt,
                systolic = systolic,
                diastolic = diastolic,
                pulse = input.pulse,
                position = input.position,
                onWaking = input.onWaking,
                note = input.note?.trim()?.ifEmpty { null },
            )
        }
        val changed = changes(
            "taken_at" to (old.takenAt to corrected.takenAt),
            "systolic" to (old.systolic to corrected.systolic),
            "diastolic" to (old.diastolic to corrected.diastolic),
            "pulse" to (old.pulse to corrected.pulse),
            "position" to (old.position to corrected.position),
            "on_waking" to (old.onWaking to corrected.onWaking),
            "note" to (old.note to corrected.note),
            "not_done_reason" to (old.notDoneReason to corrected.notDoneReason),
        )
        if (changed.isEmpty()) return@inTransaction Outcome.Ok(RecordResult.Saved(old, emptyList()))
        val source = fasts.get(old.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        records.updateBloodPressure(corrected)
        audit.log(EntryKind.BLOOD_PRESSURE, old.id, changed, input.reason, clock)
        val fired = if (source.fast.status == FastStatus.IN_PROGRESS) recorder.evaluate(source) else emptyList()
        Outcome.Ok(RecordResult.Saved(corrected, fired))
    }
}

/** H-034, H-048: borrar (en blando) o restaurar una medición o presión, con su fila en `edit_log`; no evalúa reglas (D-20). */
class SetEntryDeleted(
    private val records: RecordRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(kind: EntryKind, id: String, deleted: Boolean): Outcome<Unit> = transactions.inTransaction {
        val (exists, visible) = when (kind) {
            EntryKind.MEASUREMENT -> (records.measurement(id, includeDeleted = true) != null) to (records.measurement(id) != null)
            EntryKind.BLOOD_PRESSURE -> (records.bloodPressureReading(id, includeDeleted = true) != null) to (records.bloodPressureReading(id) != null)
        }
        if (!exists) return@inTransaction invalid(ValidationError.ENTRY_NOT_FOUND)
        if (visible != deleted) return@inTransaction Outcome.Ok(Unit)
        when (kind) {
            EntryKind.MEASUREMENT -> records.setMeasurementDeleted(id, deleted)
            EntryKind.BLOOD_PRESSURE -> records.setBloodPressureDeleted(id, deleted)
        }
        audit.append(
            EditLogEntry(
                id = Ids.newId(),
                entityName = kind.entityName,
                rowId = id,
                action = if (deleted) EditAction.DELETE else EditAction.RESTORE,
                editedAt = LocalTimes.now(clock),
                field = null,
                oldValue = null,
                newValue = null,
                reason = null,
            ),
        )
        Outcome.Ok(Unit)
    }
}
