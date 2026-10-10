package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.Meal
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RefeedRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDateTime

/**
 * H-031: marcar la casilla de un paso guarda la hora real; desmarcarla (tras confirmar en la interfaz) la vacía y
 * deja constancia de la hora que tenía (S8).
 */
class SetRefeedCheck(
    private val refeed: RefeedRepository,
    private val audit: AuditRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock,
) {
    suspend operator fun invoke(checkId: String, done: Boolean): Outcome<RefeedCheck> = transactions.inTransaction {
        val check = refeed.check(checkId) ?: return@inTransaction invalid(ValidationError.REFEED_CHECK_NOT_FOUND)
        val old = check.doneAt
        if (done == (old != null)) return@inTransaction Outcome.Ok(check)
        val updated = check.copy(doneAt = if (done) LocalTimes.now(clock) else null)
        refeed.updateCheck(updated)
        if (old != null) audit.append(editEntry(check.id, "done_at", LocalTimes.format(old), null, null, clock, entityName = "refeed_check"))
        Outcome.Ok(updated)
    }
}

/** H-032: anotar una comida con hora, descripción, cantidad aproximada y cómo cayó («otro» con texto). */
class RecordMeal(private val fasts: FastRepository, private val refeed: RefeedRepository) {
    data class Input(
        val fastId: String,
        val eatenAt: LocalDateTime,
        val description: String,
        val approxAmount: String?,
        val tolerance: MealTolerance?,
        val toleranceDetail: String?,
    )

    suspend operator fun invoke(input: Input): Outcome<Meal> {
        val detail = input.toleranceDetail?.trim()?.ifEmpty { null }
        val errors = buildList {
            if (input.description.isBlank()) add(ValidationError.TEXT_REQUIRED)
            if (input.tolerance == null) add(ValidationError.TOLERANCE_REQUIRED)
            if (input.tolerance == MealTolerance.OTHER && detail == null) add(ValidationError.TOLERANCE_DETAIL_REQUIRED)
        }
        val tolerance = input.tolerance
        if (errors.isNotEmpty() || tolerance == null) return Outcome.Invalid(errors)
        fasts.get(input.fastId) ?: return invalid(ValidationError.FAST_NOT_FOUND)
        val meal = Meal(
            id = Ids.newId(),
            fastId = input.fastId,
            eatenAt = input.eatenAt.withNano(0),
            description = input.description.trim(),
            approxAmount = input.approxAmount?.trim()?.ifEmpty { null },
            tolerance = tolerance,
            toleranceDetail = detail.takeIf { tolerance == MealTolerance.OTHER },
        )
        refeed.insertMeal(meal)
        return Outcome.Ok(meal)
    }
}
