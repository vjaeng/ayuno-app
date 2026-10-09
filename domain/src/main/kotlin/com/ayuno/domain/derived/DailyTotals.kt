package com.ayuno.domain.derived

import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.units.UnitConversions
import java.time.LocalDate

/** Total del día de un tipo de ingesta frente a su meta (derivado, H-006, H-012, H-022). */
data class DailyTotal(
    val intakeType: IntakeType,
    val amount: Double,
    val goal: IntakeGoal?,
    /** Gramos de sodio (cantidad × factor), si el tipo tiene factor. */
    val sodium: Double?,
)

object DailyTotals {

    fun of(day: LocalDate, intakes: List<Intake>, types: List<IntakeType>, goals: List<IntakeGoal>): List<DailyTotal> {
        val todays = intakes.filter { it.takenAt.toLocalDate() == day }
        val codes = (todays.map { it.intakeTypeCode } + goals.map { it.intakeTypeCode }).distinct()
        return codes.mapNotNull { code ->
            val type = types.firstOrNull { it.code == code } ?: return@mapNotNull null
            val amount = todays.filter { it.intakeTypeCode == code }.sumOf { toTypeUnit(it, type) }
            DailyTotal(type, amount, goals.firstOrNull { it.intakeTypeCode == code }, type.sodiumFactor?.let { amount * it })
        }
    }

    /** Total del día en la unidad de la meta, para compararlo con ella. */
    fun amountForGoal(total: DailyTotal): Double? {
        val goal = total.goal ?: return null
        return if (UnitConversions.canConvert(total.intakeType.unit, goal.unit)) UnitConversions.convert(total.amount, total.intakeType.unit, goal.unit) else null
    }

    private fun toTypeUnit(intake: Intake, type: IntakeType): Double =
        if (UnitConversions.canConvert(intake.unit, type.unit)) UnitConversions.convert(intake.amount, intake.unit, type.unit) else 0.0
}
