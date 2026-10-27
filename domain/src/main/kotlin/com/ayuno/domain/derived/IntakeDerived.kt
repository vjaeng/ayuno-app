package com.ayuno.domain.derived

import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.HouseholdUnit
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Settings
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Derivado «Equivalente casero» (MODELO §2.5, H-050, D-22). */
object HouseholdMeasures {

    /** Equivalente en la unidad del tipo de lo tecleado; nulo si [unitEntered] no es la unidad ni una medida del tipo. */
    fun equivalent(type: IntakeType, amountEntered: Double, unitEntered: String): Double? = when {
        unitEntered == type.unit -> amountEntered
        else -> type.unit(unitEntered)?.let { amountEntered * it.equivalent }
    }

    /**
     * Línea de la medida imprecisa: cuánto es una y, si la meta tiene cifra, a cuántas de esa medida (redondeado) y
     * a qué fracción de la medida exacta más grande equivale la meta.
     */
    data class ImpreciseLine(val unit: HouseholdUnit, val goalInUnits: Int?, val goalInLargest: Pair<HouseholdUnit, Double>?)

    fun impreciseLine(type: IntakeType, unit: HouseholdUnit, goal: IntakeGoal?): ImpreciseLine? {
        if (!unit.imprecise) return null
        val amount = goal?.dailyAmount?.takeIf { goal.unit == type.unit } ?: return ImpreciseLine(unit, null, null)
        val largest = type.units.filter { !it.imprecise }.maxByOrNull { it.equivalent }
        return ImpreciseLine(unit, (amount / unit.equivalent).roundToInt(), largest?.let { it to amount / it.equivalent })
    }
}

/** Derivado «Avance esperado a esta hora» (MODELO §2.5, H-006): la meta repartida por igual en el día despierto. */
object ExpectedIntake {

    fun at(goal: IntakeGoal, settings: Settings, now: LocalDateTime): Double? {
        val amount = goal.dailyAmount ?: return null
        val sleep = settings.quietStart ?: return null
        val wake = settings.quietEnd ?: return null
        val day = now.toLocalDate()
        val start = day.atTime(wake)
        val end = if (sleep > wake) day.atTime(sleep) else day.plusDays(1).atTime(sleep)
        return when {
            now.isBefore(start) -> 0.0
            !now.isBefore(end) -> amount
            else -> amount * Duration.between(start, now).seconds / Duration.between(start, end).seconds
        }
    }
}

/** Pregunta pendiente por un tipo de ingesta (H-052): desde [anchor] sin anotar; suena a [at]. */
data class IntakeNudgeItem(val intakeTypeCode: String, val anchor: LocalDateTime, val at: LocalDateTime)

/** Derivado «Próxima pregunta por tipo» (MODELO §2.5, H-052, D-23). */
object IntakeNudge {

    /**
     * Una por cada meta del ayuno en curso con «preguntar tras N h»: el ancla es la última ingesta de ese tipo (o el
     * inicio del reloj) y la pregunta cae tras N horas despierto, saltando el silencio nocturno. Una ingesta nueva
     * mueve el ancla; con el mismo ancla no hay otra pregunta.
     */
    fun items(source: FastWithPlan, intakes: List<Intake>, settings: Settings): List<IntakeNudgeItem> {
        val start = source.fast.clockStartActual ?: return emptyList()
        if (source.fast.endActual != null) return emptyList()
        return source.plan.goals.mapNotNull { goal ->
            val hours = goal.nudgeAfterHours ?: return@mapNotNull null
            val anchor = intakes.filter { it.intakeTypeCode == goal.intakeTypeCode }.maxOfOrNull { it.takenAt }?.takeIf { it.isAfter(start) } ?: start
            IntakeNudgeItem(goal.intakeTypeCode, anchor, addAwake(anchor, Duration.ofSeconds((hours * 3600).toLong()), settings))
        }
    }

    /** [from] + [awake] contando solo el tiempo fuera del silencio nocturno. */
    fun addAwake(from: LocalDateTime, awake: Duration, settings: Settings): LocalDateTime {
        val sleep = settings.quietStart
        val wake = settings.quietEnd
        if (sleep == null || wake == null || sleep == wake) return from.plus(awake)
        var t = from
        var left = awake
        // Si se empieza dentro del silencio, se cuenta desde que acaba.
        if (settings.isQuiet(t.toLocalTime())) t = nextWake(t, settings)
        while (true) {
            val quietFrom = nextSleep(t, settings)
            val span = Duration.between(t, quietFrom)
            if (left <= span) return t.plus(left)
            left = left.minus(span)
            t = nextWake(quietFrom, settings)
        }
    }

    private fun nextSleep(t: LocalDateTime, settings: Settings): LocalDateTime {
        val at = t.toLocalDate().atTime(settings.quietStart!!)
        return if (at.isAfter(t)) at else at.plusDays(1)
    }

    private fun nextWake(t: LocalDateTime, settings: Settings): LocalDateTime {
        val at = t.toLocalDate().atTime(settings.quietEnd!!)
        return if (at.isAfter(t)) at else at.plusDays(1)
    }
}

/** Fecha del día natural de [time], para agrupar ingestas por día. */
internal fun dayOf(time: LocalDateTime): LocalDate = time.toLocalDate()
