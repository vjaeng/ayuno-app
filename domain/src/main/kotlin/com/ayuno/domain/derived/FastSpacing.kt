package com.ayuno.domain.derived

import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import java.time.LocalDateTime

/** Franja de un ayuno: de la última comida a su fin (MODELO-DATOS §2.5). */
data class FastWindow(val start: LocalDateTime, val end: LocalDateTime)

/**
 * Con qué ayuno choca un ayuno planificado y qué hora haría falta: [earliestLastMeal] si el otro va antes
 * (la última comida del nuevo no puede ser anterior), [latestEnd] si va después (el nuevo debe terminar antes).
 */
data class SpacingConflict(val other: Fast, val otherWindow: FastWindow, val earliestLastMeal: LocalDateTime?, val latestEnd: LocalDateTime?)

/** D-17: entre la franja de un ayuno que se planifica y la de cualquier otro deben pasar al menos [MIN_GAP_DAYS] días. */
object FastSpacing {
    const val MIN_GAP_DAYS = 7L

    fun window(fast: Fast, now: LocalDateTime): FastWindow = when (fast.status) {
        FastStatus.PLANNED -> FastWindow(fast.lastMealPlanned, FastClock.plannedEnd(fast))
        FastStatus.IN_PROGRESS -> FastWindow(fast.lastMealActual ?: fast.lastMealPlanned, maxOf(FastClock.plannedEnd(fast), now))
        FastStatus.FINISHED -> FastWindow(fast.lastMealActual ?: fast.lastMealPlanned, fast.endActual ?: FastClock.plannedEnd(fast))
    }

    /** El primer ayuno (por orden de franja) con el que choca [candidate]; el propio ayuno no cuenta. */
    fun conflict(candidate: Fast, others: List<Fast>, now: LocalDateTime): SpacingConflict? {
        val own = window(candidate, now)
        return others
            .filter { it.id != candidate.id }
            .map { it to window(it, now) }
            .sortedBy { it.second.start }
            .firstNotNullOfOrNull { (other, theirs) ->
                val farAfter = !own.start.isBefore(theirs.end.plusDays(MIN_GAP_DAYS))
                val farBefore = !own.end.plusDays(MIN_GAP_DAYS).isAfter(theirs.start)
                when {
                    farAfter || farBefore -> null
                    theirs.start.isAfter(own.start) -> SpacingConflict(other, theirs, null, theirs.start.minusDays(MIN_GAP_DAYS))
                    else -> SpacingConflict(other, theirs, theirs.end.plusDays(MIN_GAP_DAYS), null)
                }
            }
    }
}
