package com.ayuno.domain.usecase

import com.ayuno.domain.derived.StageWindows
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Trend
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.rules.RuleContext
import com.ayuno.domain.rules.RuleEngine
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.units.UnitConversions
import java.time.Clock
import java.time.LocalDateTime

/** Resultado de guardar una entrada: pide confirmar un valor fuera de rango, o la guarda junto con las alertas que disparó. */
sealed interface RecordResult<out T> {
    data class NeedsRangeConfirmation(val fields: List<String>) : RecordResult<Nothing>
    data class Saved<T>(val entry: T, val alerts: List<Alert>) : RecordResult<T>
}

/**
 * Evalúa las reglas del ayuno con la entrada ya escrita y registra las alertas. Se llama dentro de la misma
 * transacción que guarda la entrada (ADR-006).
 */
internal class AlertRecorder(
    private val records: RecordRepository,
    private val alerts: AlertRepository,
    private val clock: Clock,
) {
    suspend fun evaluate(source: FastWithPlan): List<Alert> {
        val fastId = source.fast.id
        val context = RuleContext(
            rules = source.plan.rules,
            measurements = records.measurements(fastId),
            bloodPressure = records.bloodPressure(fastId),
            alerts = alerts.alerts(fastId),
            stageWindows = StageWindows.of(source.fast, source.plan),
        )
        val now = LocalTimes.now(clock)
        return RuleEngine.evaluate(context).map { hit ->
            Alert(Ids.newId(), fastId, hit.rule.id, now, hit.triggerEntryIds, userChoice = null, choiceReason = null)
                .also { alerts.insert(it) }
        }
    }
}

/** H-008: anotar una medición (valor tecleado y su equivalente, fuera de rango confirmado, «no se hizo»). */
class RecordMeasurement(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val transactions: TransactionRunner,
    clock: Clock,
) {
    private val recorder = AlertRecorder(records, alerts, clock)

    data class Input(
        val fastId: String,
        val metricTypeCode: String,
        val takenAt: LocalDateTime,
        val scheduledFor: LocalDateTime? = null,
        val valueEntered: Double? = null,
        val unitEntered: String? = null,
        val valueText: String? = null,
        val source: GlucoseSource? = null,
        val trend: Trend? = null,
        val onWaking: Boolean = false,
        val note: String? = null,
        val notDoneReason: String? = null,
        val confirmedOutOfRange: Boolean = false,
    )

    suspend operator fun invoke(input: Input): Outcome<RecordResult<Measurement>> {
        val type = catalog.metricType(input.metricTypeCode) ?: return invalid(ValidationError.METRIC_NOT_FOUND)
        val notDone = input.notDoneReason?.trim()?.ifEmpty { null }
        val hasValue = if (type.isNumeric) input.valueEntered != null else !input.valueText.isNullOrBlank()
        when {
            notDone != null && hasValue -> return invalid(ValidationError.VALUE_AND_NOT_DONE)
            notDone == null && !hasValue -> return invalid(ValidationError.VALUE_REQUIRED)
            notDone == null && !type.active -> return invalid(ValidationError.METRIC_NOT_ACTIVE)
        }
        var valueNum: Double? = null
        var unitEntered: String? = null
        if (notDone == null && type.isNumeric) {
            unitEntered = input.unitEntered ?: type.entryUnit
            if (unitEntered != type.unit && unitEntered != type.altUnit) return invalid(ValidationError.UNIT_NOT_ALLOWED)
            valueNum = UnitConversions.convert(input.valueEntered!!, unitEntered, type.unit)
            if (type.isGlucose && input.source == null) return invalid(ValidationError.SOURCE_REQUIRED)
            if (type.isOutOfRange(valueNum) && !input.confirmedOutOfRange) {
                return Outcome.Ok(RecordResult.NeedsRangeConfirmation(listOf(type.code)))
            }
        }
        val measurement = Measurement(
            id = Ids.newId(),
            fastId = input.fastId,
            metricTypeCode = type.code,
            takenAt = input.takenAt.withNano(0),
            scheduledFor = input.scheduledFor?.withNano(0),
            valueNum = valueNum,
            valueText = input.valueText?.trim().takeIf { notDone == null && !type.isNumeric },
            valueEntered = input.valueEntered.takeIf { notDone == null && type.isNumeric },
            unitEntered = unitEntered,
            source = input.source.takeIf { notDone == null && type.isGlucose },
            trend = input.trend.takeIf { notDone == null && type.isGlucose },
            onWaking = input.onWaking,
            notDoneReason = notDone,
            note = input.note?.trim()?.ifEmpty { null },
        )
        return transactions.inTransaction {
            val source = fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
            records.insertMeasurement(measurement)
            Outcome.Ok(RecordResult.Saved(measurement, recorder.evaluate(source)))
        }
    }
}

/** H-009: anotar la presión sentado y de pie, con la toma de pie vinculada a la sentada. */
class RecordBloodPressure(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val transactions: TransactionRunner,
    clock: Clock,
) {
    private val recorder = AlertRecorder(records, alerts, clock)

    data class Input(
        val fastId: String,
        val takenAt: LocalDateTime,
        val scheduledFor: LocalDateTime? = null,
        val systolic: Int? = null,
        val diastolic: Int? = null,
        val pulse: Int? = null,
        val position: Position,
        val onWaking: Boolean = false,
        val pairedReadingId: String? = null,
        val note: String? = null,
        val notDoneReason: String? = null,
        val confirmedOutOfRange: Boolean = false,
    )

    suspend operator fun invoke(input: Input): Outcome<RecordResult<BloodPressureReading>> {
        val notDone = input.notDoneReason?.trim()?.ifEmpty { null }
        val hasValue = input.systolic != null || input.diastolic != null
        when {
            notDone != null && (hasValue || input.pulse != null) -> return invalid(ValidationError.VALUE_AND_NOT_DONE)
            notDone == null && (input.systolic == null || input.diastolic == null) -> return invalid(ValidationError.VALUE_REQUIRED)
        }
        if (notDone == null) {
            if (input.diastolic!! >= input.systolic!!) return invalid(ValidationError.DIASTOLIC_NOT_BELOW_SYSTOLIC)
            val ranges = catalog.bloodPressureRanges()
            val outside = buildList {
                if (input.systolic.toDouble() !in ranges.systolic) add("systolic")
                if (input.diastolic.toDouble() !in ranges.diastolic) add("diastolic")
                if (input.pulse != null && input.pulse.toDouble() !in ranges.pulse) add("pulse")
            }
            if (outside.isNotEmpty() && !input.confirmedOutOfRange) return Outcome.Ok(RecordResult.NeedsRangeConfirmation(outside))
        }
        return transactions.inTransaction {
            val source = fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
            if (input.pairedReadingId != null) {
                val paired = records.bloodPressure(input.fastId).firstOrNull { it.id == input.pairedReadingId }
                if (input.position != Position.STANDING || paired == null || paired.position != Position.SITTING) {
                    return@inTransaction invalid(ValidationError.PAIRED_READING_INVALID)
                }
            }
            val reading = BloodPressureReading(
                id = Ids.newId(),
                fastId = input.fastId,
                takenAt = input.takenAt.withNano(0),
                scheduledFor = input.scheduledFor?.withNano(0),
                systolic = input.systolic.takeIf { notDone == null },
                diastolic = input.diastolic.takeIf { notDone == null },
                pulse = input.pulse.takeIf { notDone == null },
                position = input.position,
                onWaking = input.onWaking,
                pairedReadingId = input.pairedReadingId,
                notDoneReason = notDone,
                note = input.note?.trim()?.ifEmpty { null },
            )
            records.insertBloodPressure(reading)
            Outcome.Ok(RecordResult.Saved(reading, recorder.evaluate(source)))
        }
    }
}
