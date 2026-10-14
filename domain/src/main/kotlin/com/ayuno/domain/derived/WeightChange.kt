package com.ayuno.domain.derived

import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType

/** Peso inicial, final y diferencia de un ayuno (H-047, D-19). Valores en la unidad del catálogo (kg). */
data class WeightChange(val initial: Measurement?, val final: Measurement?) {
    /** Final − inicial; nulo si falta alguno. */
    val difference: Double? get() = if (initial != null && final != null) final.valueNum!! - initial.valueNum!! else null

    companion object {
        /**
         * Inicial: el último no después del inicio real del reloj o, si no hay, el primero del ayuno. Final: el último no
         * después del fin real y posterior al inicial o, si no hay, el primero tras el fin. Ignora los «no se hizo».
         */
        fun of(fast: Fast, measurements: List<Measurement>): WeightChange {
            val weights = measurements
                .filter { it.metricTypeCode == MetricType.WEIGHT_CODE && it.valueNum != null }
                .sortedBy { it.takenAt }
            val start = fast.clockStartActual
            val initial = start?.let { s -> weights.lastOrNull { !it.takenAt.isAfter(s) } } ?: weights.firstOrNull()
            val later = weights.filter { initial == null || it.takenAt.isAfter(initial.takenAt) }
            val end = fast.endActual
            val final = end?.let { e -> later.lastOrNull { !it.takenAt.isAfter(e) } ?: later.firstOrNull() }
            return WeightChange(initial, final)
        }
    }
}
