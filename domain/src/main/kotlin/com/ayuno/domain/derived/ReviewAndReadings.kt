package com.ayuno.domain.derived

import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.StartWarning
import com.ayuno.domain.model.WarningKind
import kotlin.math.abs

/**
 * Derivado «Revisión previa» (MODELO §2.5, H-057, D-25): reglas «de ejemplo» (activas o no), metas sin cifra y lista
 * previa sin marcar del plan del ayuno, con una copia del texto que se mostrará.
 */
object PreStartReview {
    fun of(plan: Plan, intakeTypes: List<IntakeType>): List<StartWarning> =
        plan.rules.filter { it.isPlaceholder }.map { StartWarning(WarningKind.PLACEHOLDER_RULE, it.id, it.name) } +
            plan.goals.filter { it.dailyAmount == null }.map { g ->
                StartWarning(WarningKind.GOAL_WITHOUT_AMOUNT, g.id, intakeTypes.firstOrNull { it.code == g.intakeTypeCode }?.name ?: g.intakeTypeCode)
            } +
            plan.checklist.filter { !it.isChecked }.map { StartWarning(WarningKind.UNCHECKED_ITEM, it.id, it.text) }
}

/** Derivados de la presión entre lecturas (MODELO §2.5, H-009, H-053 a H-056, D-24). */
object StandingMoment {

    /**
     * Momento de pie de una lectura: el tiempo del plan más cercano a sus segundos (empate → el menor); nulo =
     * «sin esperar» (sin segundos o sin tiempos en el plan).
     */
    fun of(reading: BloodPressureReading, checkSeconds: List<Int>?): Int? {
        val seconds = reading.secondsStanding ?: return null
        return checkSeconds.orEmpty().sorted().minByOrNull { abs(it - seconds) }
    }

    /** Diferencia de pareja de una cifra: de pie − sentado; nula si falta alguna de las dos. */
    fun difference(standing: BloodPressureReading, sitting: BloodPressureReading, component: BpComponent): Int? {
        val up = standing.value(component) ?: return null
        val down = sitting.value(component) ?: return null
        return up - down
    }
}

/** Derivado «Valor de partida» (MODELO §2.5, H-055): la marcada «de partida» o, si no hay, la primera con valor. */
object Baseline {
    fun measurement(measurements: List<Measurement>, metricTypeCode: String): Measurement? {
        val withValue = measurements.filter { it.metricTypeCode == metricTypeCode && it.valueNum != null && it.notDoneReason == null }
        return withValue.firstOrNull { it.isBaseline } ?: withValue.minByOrNull { it.takenAt }
    }

    fun bloodPressure(readings: List<BloodPressureReading>, position: Position?): BloodPressureReading? {
        val withValue = readings.filter { it.notDoneReason == null && it.systolic != null && (position == null || it.position == position) }
        return withValue.firstOrNull { it.isBaseline } ?: withValue.minByOrNull { it.takenAt }
    }
}
