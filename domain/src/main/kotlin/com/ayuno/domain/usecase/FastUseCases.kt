package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Plan
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.TemplateRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun offsetText(clock: Clock): String {
    val offset = clock.zone.rules.getOffset(clock.instant())
    return if (offset == ZoneOffset.UTC) "+00:00" else offset.id
}

/** Copia del plan con identificadores nuevos (D-03): el ayuno no cambia si cambia la plantilla. */
internal fun Plan.copyWithNewIds(): Plan = Plan(
    stages = stages.map { it.copy(id = Ids.newId()) },
    metrics = metrics.map { m -> m.copy(id = Ids.newId(), schedules = m.schedules.map { it.copy(id = Ids.newId()) }) },
    rules = rules.map { it.copy(id = Ids.newId()) },
    goals = goals.map { it.copy(id = Ids.newId()) },
    checklist = checklist.map { it.copy(id = Ids.newId()) },
    notices = notices.map { it.copy(id = Ids.newId()) },
    products = products.map { it.copy(id = Ids.newId()) },
)

/** H-004: crear un ayuno desde una plantilla (o desde cero) con la última comida y el inicio previsto. */
class CreateFast(
    private val templates: TemplateRepository,
    private val fasts: FastRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    data class Input(
        val templateId: String?,
        val lastMealPlanned: LocalDateTime,
        val clockStartPlanned: LocalDateTime,
        val targetHours: Double,
        val notes: String?,
    ) {
        companion object {
            /** H-004: el objetivo se calcula entre el inicio y el fin previstos. */
            fun withPlannedEnd(templateId: String?, lastMeal: LocalDateTime, start: LocalDateTime, end: LocalDateTime, notes: String?) =
                Input(templateId, lastMeal, start, com.ayuno.domain.derived.FastClock.targetHoursBetween(start, end), notes)
        }
    }

    suspend operator fun invoke(input: Input): Outcome<FastWithPlan> {
        val source = input.templateId?.let { templates.get(it) ?: return invalid(ValidationError.TEMPLATE_NOT_FOUND) }
        val errors = buildList {
            if (input.clockStartPlanned.isBefore(input.lastMealPlanned)) add(ValidationError.CLOCK_START_BEFORE_LAST_MEAL)
            if (input.targetHours <= 0) add(ValidationError.PLANNED_END_NOT_AFTER_START)
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val fast = Fast(
            id = Ids.newId(),
            templateId = source?.template?.id,
            status = FastStatus.PLANNED,
            targetHours = input.targetHours,
            milestoneHours = source?.template?.milestoneHours.orEmpty(),
            lastMealPlanned = input.lastMealPlanned.withNano(0),
            lastMealActual = null,
            clockStartPlanned = input.clockStartPlanned.withNano(0),
            clockStartActual = null,
            endActual = null,
            endReason = null,
            endReasonDetail = null,
            endRuleId = null,
            tzOffset = offsetText(clock),
            notes = input.notes?.trim()?.ifEmpty { null },
        )
        val created = FastWithPlan(fast, source?.plan?.copyWithNewIds() ?: Plan.EMPTY)
        transactions.inTransaction { fasts.insert(created) }
        return Outcome.Ok(created)
    }
}

/** H-004: editar las notas generales del ayuno. */
class UpdateFastNotes(private val fasts: FastRepository) {
    suspend operator fun invoke(fastId: String, notes: String?): Outcome<Fast> {
        val fast = fasts.get(fastId)?.fast ?: return invalid(ValidationError.FAST_NOT_FOUND)
        val updated = fast.copy(notes = notes?.trim()?.ifEmpty { null })
        fasts.update(updated)
        return Outcome.Ok(updated)
    }
}

/** H-005: «Empieza el ayuno ahora». */
class StartFast(
    private val fasts: FastRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(fastId: String): Outcome<Fast> = transactions.inTransaction {
        val fast = fasts.get(fastId)?.fast ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        if (fast.status != FastStatus.PLANNED) return@inTransaction invalid(ValidationError.FAST_NOT_PLANNED)
        if (fasts.all().any { it.status == FastStatus.IN_PROGRESS }) return@inTransaction invalid(ValidationError.ANOTHER_FAST_IN_PROGRESS)
        val started = fast.copy(
            status = FastStatus.IN_PROGRESS,
            clockStartActual = LocalTimes.now(clock),
            tzOffset = offsetText(clock),
        )
        fasts.update(started)
        Outcome.Ok(started)
    }
}

/** H-005: registrar (y corregir) la hora real de la última comida; las correcciones dejan rastro (S8). */
class RegisterLastMeal(
    private val fasts: FastRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(fastId: String, at: LocalDateTime, reason: String? = null): Outcome<Fast> = transactions.inTransaction {
        val fast = fasts.get(fastId)?.fast ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        val value = at.withNano(0)
        val updated = fast.copy(lastMealActual = value)
        fasts.update(updated)
        fast.lastMealActual?.let { old ->
            audit.append(editEntry(fast.id, "last_meal_actual", LocalTimes.format(old), LocalTimes.format(value), reason, clock))
        }
        Outcome.Ok(updated)
    }
}

/** H-005: corregir la hora de inicio del reloj, con motivo obligatorio y constancia del valor anterior. */
class CorrectClockStart(
    private val fasts: FastRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(fastId: String, at: LocalDateTime, reason: String): Outcome<Fast> = transactions.inTransaction {
        if (reason.isBlank()) return@inTransaction invalid(ValidationError.REASON_REQUIRED)
        val fast = fasts.get(fastId)?.fast ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        val old = fast.clockStartActual ?: return@inTransaction invalid(ValidationError.FAST_NOT_STARTED)
        val value = at.withNano(0)
        val updated = fast.copy(clockStartActual = value)
        fasts.update(updated)
        audit.append(editEntry(fast.id, "clock_start_actual", LocalTimes.format(old), LocalTimes.format(value), reason.trim(), clock))
        Outcome.Ok(updated)
    }
}

/** H-030: terminar el ayuno con hora real y motivo obligatorio; un ayuno terminado no se reanuda. */
class EndFast(
    private val fasts: FastRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    data class Input(
        val fastId: String,
        val endAt: LocalDateTime,
        val reason: EndReason,
        val detail: String?,
        val ruleId: String?,
    )

    suspend operator fun invoke(input: Input): Outcome<Fast> = transactions.inTransaction {
        val source = fasts.get(input.fastId) ?: return@inTransaction invalid(ValidationError.FAST_NOT_FOUND)
        val fast = source.fast
        if (fast.status != FastStatus.IN_PROGRESS) return@inTransaction invalid(ValidationError.FAST_NOT_IN_PROGRESS)
        val start = fast.clockStartActual ?: return@inTransaction invalid(ValidationError.FAST_NOT_STARTED)
        val end = input.endAt.withNano(0)
        val errors = buildList {
            if (end.isBefore(start)) add(ValidationError.END_BEFORE_START)
            if (end.isAfter(LocalTimes.now(clock))) add(ValidationError.END_IN_FUTURE)
            if (input.reason == EndReason.OTHER && input.detail.isNullOrBlank()) add(ValidationError.END_DETAIL_REQUIRED)
            if (input.reason == EndReason.ALERT_RULE && source.plan.rules.none { it.id == input.ruleId }) add(ValidationError.END_RULE_REQUIRED)
        }
        if (errors.isNotEmpty()) return@inTransaction Outcome.Invalid(errors)
        val ended = fast.copy(
            status = FastStatus.FINISHED,
            endActual = end,
            endReason = input.reason,
            endReasonDetail = input.detail?.trim()?.takeIf { input.reason == EndReason.OTHER },
            endRuleId = input.ruleId.takeIf { input.reason == EndReason.ALERT_RULE },
        )
        fasts.update(ended)
        Outcome.Ok(ended)
    }
}

internal fun editEntry(rowId: String, field: String, old: String?, new: String?, reason: String?, clock: Clock) = EditLogEntry(
    id = Ids.newId(),
    entityName = "fast",
    rowId = rowId,
    action = EditAction.EDIT,
    editedAt = LocalTimes.now(clock),
    field = field,
    oldValue = old,
    newValue = new,
    reason = reason?.trim()?.ifEmpty { null },
)
