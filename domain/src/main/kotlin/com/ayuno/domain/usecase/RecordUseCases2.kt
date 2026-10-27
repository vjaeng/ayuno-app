package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.derived.HouseholdMeasures
import com.ayuno.domain.units.UnitConversions
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDateTime

/** H-010: registrar un síntoma con intensidad 0-10; un síntoma de alarma evalúa las reglas al instante. */
class RecordSymptom(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val transactions: TransactionRunner,
    clock: Clock,
) {
    private val recorder = AlertRecorder(catalog, records, alerts, clock)

    data class Input(val fastId: String, val symptomTypeCode: String, val notedAt: LocalDateTime, val severity: Int, val note: String?)

    suspend operator fun invoke(input: Input): Outcome<RecordResult<Symptom>> {
        val type = catalog.symptomTypes().firstOrNull { it.code == input.symptomTypeCode } ?: return invalid(ValidationError.SYMPTOM_NOT_FOUND)
        if (input.severity !in 0..10) return invalid(ValidationError.SEVERITY_OUT_OF_RANGE)
        val symptom = Symptom(Ids.newId(), input.fastId, type.code, input.notedAt.withNano(0), input.severity, input.note?.trim()?.ifEmpty { null })
        return transactions.inTransaction {
            val source = fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
            records.insertSymptom(symptom)
            Outcome.Ok(RecordResult.Saved(symptom, recorder.evaluate(source)))
        }
    }
}

/**
 * H-012, H-050, H-051: anotar una ingesta. La cantidad se teclea en la unidad del tipo o en una de sus medidas caseras;
 * se guarda lo tecleado y su equivalente (D-22). «Agua con sal» ([alongside]) guarda dos ingestas con el mismo
 * `group_id` y la misma hora, todo o nada, con una sola evaluación de reglas.
 */
class RecordIntake(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    alerts: AlertRepository,
    private val transactions: TransactionRunner,
    clock: Clock,
) {
    private val recorder = AlertRecorder(catalog, records, alerts, clock)

    /** [unit]: unidad del tipo (por defecto) o `code` de una medida casera del tipo. */
    data class Input(
        val fastId: String,
        val intakeTypeCode: String,
        val takenAt: LocalDateTime,
        val amount: Double?,
        val unit: String?,
        val note: String?,
        val alongside: Part? = null,
    )

    /** Segunda ingesta de la misma entrada («Agua con sal»). */
    data class Part(val intakeTypeCode: String, val amount: Double?, val unit: String?)

    suspend operator fun invoke(input: Input): Outcome<RecordResult<Intake>> {
        val types = catalog.intakeTypes()
        val groupId = input.alongside?.let { Ids.newId() }
        val first = build(types, input.fastId, Part(input.intakeTypeCode, input.amount, input.unit), input.takenAt, input.note, groupId)
        val second = input.alongside?.let { build(types, input.fastId, it, input.takenAt, null, groupId) }
        val intakes = listOfNotNull(first, second).map { (it as? Built.Ok)?.intake ?: return Outcome.Invalid(listOf((it as Built.Error).error)) }
        return transactions.inTransaction {
            val source = fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
            intakes.forEach { records.insertIntake(it) }
            Outcome.Ok(RecordResult.Saved(intakes.first(), recorder.evaluate(source)))
        }
    }

    private sealed interface Built {
        data class Ok(val intake: Intake) : Built
        data class Error(val error: ValidationError) : Built
    }

    private fun build(types: List<IntakeType>, fastId: String, part: Part, takenAt: LocalDateTime, note: String?, groupId: String?): Built {
        val type = types.firstOrNull { it.code == part.intakeTypeCode } ?: return Built.Error(ValidationError.INTAKE_TYPE_NOT_FOUND)
        if (part.amount == null || part.amount <= 0) return Built.Error(ValidationError.AMOUNT_NOT_POSITIVE)
        val unitEntered = part.unit?.trim()?.ifEmpty { null } ?: type.unit
        val amount = HouseholdMeasures.equivalent(type, part.amount, unitEntered)
            ?: convertible(part.amount, unitEntered, type.unit)
            ?: return Built.Error(ValidationError.MEASURE_NOT_FOUND)
        return Built.Ok(
            Intake(
                id = Ids.newId(), fastId = fastId, intakeTypeCode = type.code, takenAt = takenAt.withNano(0), amount = amount, unit = type.unit,
                note = note?.trim()?.ifEmpty { null }, amountEntered = part.amount, unitEntered = unitEntered, groupId = groupId,
            ),
        )
    }
}

private fun convertible(amount: Double, from: String, to: String): Double? =
    if (UnitConversions.canConvert(from, to)) UnitConversions.convert(amount, from, to) else null

/** H-051: un toque en el botón rápido guarda 1 de su medida a la hora actual, por la misma vía que el formulario. */
class RecordQuickIntake(private val catalog: CatalogRepository, private val recordIntake: RecordIntake, private val clock: Clock) {
    suspend operator fun invoke(fastId: String, intakeTypeCode: String): Outcome<RecordResult<Intake>> {
        val type = catalog.intakeTypes().firstOrNull { it.code == intakeTypeCode } ?: return invalid(ValidationError.INTAKE_TYPE_NOT_FOUND)
        val unit = type.quickUnit ?: return invalid(ValidationError.QUICK_UNIT_NOT_SET)
        return recordIntake(RecordIntake.Input(fastId, type.code, LocalTimes.now(clock), 1.0, unit.code, null))
    }
}

/** H-015: marcar una toma como tomada, no tomada (con motivo) o con dosis cambiada (sin borrar la planificada). */
class RecordProductLog(
    private val catalog: CatalogRepository,
    private val fasts: FastRepository,
    private val records: RecordRepository,
    private val transactions: TransactionRunner,
) {
    data class Input(
        val fastId: String,
        val productCode: String,
        val scheduledAt: LocalDateTime?,
        val status: ProductLogStatus,
        val takenAt: LocalDateTime?,
        val amount: Double? = null,
        val reason: String? = null,
    )

    suspend operator fun invoke(input: Input): Outcome<ProductLog> {
        val product = catalog.product(input.productCode) ?: return invalid(ValidationError.PRODUCT_NOT_FOUND)
        val reason = input.reason?.trim()?.ifEmpty { null }
        val planned = input.scheduledAt?.let { at -> product.schedules.firstOrNull { it.timeOfDay == at.toLocalTime() }?.amount } ?: product.defaultAmount
        val errors = buildList {
            if (input.status != ProductLogStatus.NOT_TAKEN && input.takenAt == null) add(ValidationError.TAKEN_AT_REQUIRED)
            if (input.status != ProductLogStatus.TAKEN && reason == null) add(ValidationError.REASON_REQUIRED)
            if (input.status == ProductLogStatus.DOSE_CHANGED && (input.amount == null || input.amount <= 0)) add(ValidationError.AMOUNT_NOT_POSITIVE)
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val log = ProductLog(
            id = Ids.newId(),
            fastId = input.fastId,
            productCode = product.code,
            scheduledAt = input.scheduledAt?.withNano(0),
            status = input.status,
            takenAt = input.takenAt?.withNano(0).takeIf { input.status != ProductLogStatus.NOT_TAKEN },
            plannedAmount = planned,
            amount = when (input.status) {
                ProductLogStatus.TAKEN -> planned
                ProductLogStatus.DOSE_CHANGED -> input.amount
                ProductLogStatus.NOT_TAKEN -> null
            },
            unit = product.unit.takeIf { input.status != ProductLogStatus.NOT_TAKEN },
            reason = reason.takeIf { input.status != ProductLogStatus.TAKEN },
        )
        return transactions.inTransaction {
            fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
            records.insertProductLog(log)
            Outcome.Ok(log)
        }
    }
}

/** H-035: nota libre con hora. */
class RecordNote(private val fasts: FastRepository, private val records: RecordRepository) {
    suspend operator fun invoke(fastId: String, notedAt: LocalDateTime, text: String): Outcome<Note> {
        if (text.isBlank()) return invalid(ValidationError.TEXT_REQUIRED)
        fasts.get(fastId) ?: return invalid(ValidationError.FAST_NOT_FOUND)
        val note = Note(Ids.newId(), fastId, notedAt.withNano(0), text.trim())
        records.insertNote(note)
        return Outcome.Ok(note)
    }
}

/**
 * H-023: el botón «algo no va bien» dispara la regla manual del ayuno. Si el plan no tiene ninguna (decisión de UX
 * aceptada: alerta roja genérica), se crea en el plan del ayuno una regla manual con un texto neutro de la app,
 * en la misma transacción, para que la alerta quede registrada como cualquier otra (H-027).
 */
class TriggerManualAlert(
    private val fasts: FastRepository,
    private val alerts: AlertRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(fastId: String, defaultName: String, defaultMessage: String): Outcome<Alert> = transactions.inTransaction {
        val source = fasts.get(fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        val rule = source.plan.rules.firstOrNull { it.conditionKind == RuleConditionKind.MANUAL && it.active }
            ?: manualRule(defaultName, defaultMessage).also { fasts.updatePlan(fastId, source.plan.copy(rules = source.plan.rules + it)) }
        val alert = Alert(Ids.newId(), fastId, rule.id, LocalTimes.now(clock), emptyList(), null, null)
        alerts.insert(alert)
        Outcome.Ok(alert)
    }

    private fun manualRule(name: String, message: String) = Rule(
        id = Ids.newId(), name = name, active = true, conditionKind = RuleConditionKind.MANUAL, metricTypeCode = null,
        bpComponent = null, threshold = null, sustainedMinutes = null, symptomTypeCode = null, intakeTypeCode = null,
        checkTime = null, filterStageKinds = emptyList(), filterOnWaking = false, filterPosition = null,
        level = RuleLevel.ALERT, message = message, action = RuleAction.OFFER_END, actionMinutes = null,
    )
}

/** H-019/H-022: evaluar las reglas al llegar una hora programada (sin entrada nueva). */
class EvaluateRules(
    catalog: CatalogRepository,
    private val fasts: FastRepository,
    records: RecordRepository,
    alerts: AlertRepository,
    private val transactions: TransactionRunner,
    clock: Clock,
) {
    private val recorder = AlertRecorder(catalog, records, alerts, clock)

    suspend operator fun invoke(fastId: String): List<Alert> = transactions.inTransaction {
        val source: FastWithPlan = fasts.get(fastId) ?: return@inTransaction emptyList()
        recorder.evaluate(source)
    }
}

/** H-002: marcar o desmarcar un elemento de la lista previa del ayuno; nunca bloquea empezar. */
class SetChecklistItem(private val fasts: FastRepository) {
    suspend operator fun invoke(fastId: String, itemId: String, checked: Boolean): Outcome<Unit> {
        val source = fasts.get(fastId) ?: return invalid(ValidationError.FAST_NOT_FOUND)
        fasts.updatePlan(fastId, source.plan.copy(checklist = source.plan.checklist.map { if (it.id == itemId) it.copy(isChecked = checked) else it }))
        return Outcome.Ok(Unit)
    }
}
