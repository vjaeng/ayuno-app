package com.ayuno.domain.derived

import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.StageKind
import java.time.LocalDateTime

/** Tramo de una etapa: inicio inclusive, fin exclusivo. */
data class StageWindow(val kind: StageKind, val start: LocalDateTime, val end: LocalDateTime) {
    operator fun contains(time: LocalDateTime): Boolean = !time.isBefore(start) && time.isBefore(end)
}

/**
 * Fechas de las etapas (derivado, MODELO §2.5): preparación = N días hasta el inicio del reloj (desde la
 * medianoche del día de la última comida − N); ayuno = inicio del reloj → fin; vuelta a comer = fin → fin + N h;
 * posterior = N días tras la vuelta a comer (o tras el fin si no la hay).
 */
object StageWindows {

    fun of(fast: Fast, plan: Plan): List<StageWindow> {
        val start = FastClock.effectiveStart(fast)
        val end = fast.endActual ?: FastClock.plannedEnd(fast)
        val windows = mutableListOf<StageWindow>()
        plan.stages.firstOrNull { it.kind == StageKind.PREPARATION }?.durationValue?.let { days ->
            val prepStart = fast.lastMealPlanned.toLocalDate().minusDays(days.toLong()).atStartOfDay()
            if (prepStart.isBefore(start)) windows += StageWindow(StageKind.PREPARATION, prepStart, start)
        }
        windows += StageWindow(StageKind.FAST, start, end)
        var afterFast = end
        plan.stages.firstOrNull { it.kind == StageKind.REFEED }?.durationValue?.let { hours ->
            val refeedEnd = end.plusMinutes((hours * 60).toLong())
            windows += StageWindow(StageKind.REFEED, end, refeedEnd)
            afterFast = refeedEnd
        }
        plan.stages.firstOrNull { it.kind == StageKind.POST }?.durationValue?.let { days ->
            windows += StageWindow(StageKind.POST, afterFast, afterFast.plusMinutes((days * 24 * 60).toLong()))
        }
        return windows
    }

    fun stageAt(windows: List<StageWindow>, time: LocalDateTime): StageKind? = windows.firstOrNull { time in it }?.kind
}
