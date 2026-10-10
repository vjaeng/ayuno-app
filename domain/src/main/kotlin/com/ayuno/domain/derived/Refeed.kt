package com.ayuno.domain.derived

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.StageKind
import java.time.LocalDateTime

/** H-031: ocurrencias de la vuelta a comer y tramo de seguimiento tras el fin del ayuno. */
object RefeedSchedule {

    /**
     * Horas calculadas de un paso desde el fin real [end]. Un paso repetido suena al empezar y cada intervalo
     * mientras quede ventana: «cada 60 minutos durante 4 horas» son 4 ocurrencias (0, 1, 2 y 3 h).
     * La reintroducción del día N cae a la medianoche de ese día: es un paso de fecha, sin hora.
     */
    fun times(step: RefeedStep, end: LocalDateTime): List<LocalDateTime> = when (step.kind) {
        RefeedStepKind.TIMED -> {
            val first = end.plusMinutes((step.offsetMinutes ?: 0).toLong())
            val every = step.repeatEveryMinutes
            val window = step.repeatForMinutes
            if (every == null || window == null) {
                listOf(first)
            } else {
                generateSequence(0) { it + every }.takeWhile { it < window }.map { first.plusMinutes(it.toLong()) }.toList()
            }
        }
        RefeedStepKind.REINTRODUCTION -> listOf(end.toLocalDate().plusDays((step.dayOffset ?: 1).toLong()).atStartOfDay())
    }

    /** Casillas nuevas (sin marcar) de todos los pasos, en orden de hora. */
    fun occurrences(steps: List<RefeedStep>, end: LocalDateTime): List<RefeedCheck> =
        steps.flatMap { step -> times(step, end).map { RefeedCheck(Ids.newId(), step.id, it, null) } }.sortedBy { it.scheduledAt }

    /**
     * Fin del seguimiento de un ayuno terminado: lo último entre la etapa de vuelta a comer, la posterior y el día
     * de la última casilla. Nulo si el ayuno no ha terminado o no tiene nada que seguir.
     */
    fun followUpEnd(source: FastWithPlan, checks: List<RefeedCheck>): LocalDateTime? {
        if (source.fast.status != FastStatus.FINISHED || source.fast.endActual == null) return null
        val stageEnds = StageWindows.of(source.fast, source.plan).filter { it.kind == StageKind.REFEED || it.kind == StageKind.POST }.map { it.end }
        val checkEnds = checks.map { it.scheduledAt.toLocalDate().plusDays(1).atStartOfDay() }
        return (stageEnds + checkEnds).maxOrNull()
    }

    /** ¿Estamos dentro de la etapa de vuelta a comer de este ayuno? (Inicio muestra la vuelta a comer.) */
    fun inRefeedStage(source: FastWithPlan, now: LocalDateTime): Boolean =
        source.fast.status == FastStatus.FINISHED && StageWindows.stageAt(StageWindows.of(source.fast, source.plan), now) == StageKind.REFEED

    fun isReintroduction(step: RefeedStep?): Boolean = step?.kind == RefeedStepKind.REINTRODUCTION
}
