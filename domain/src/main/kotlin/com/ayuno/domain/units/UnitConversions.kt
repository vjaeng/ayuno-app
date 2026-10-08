package com.ayuno.domain.units

/**
 * Conversión entre la unidad tecleada y la del catálogo (P-09: constantes del dominio, son física).
 * Solo para las unidades de los tipos precargados; las mediciones propias no tienen unidad alternativa.
 */
object UnitConversions {
    private const val MG_DL_PER_MMOL_L = 18.0156
    private const val KG_PER_LB = 0.45359237
    private const val CM_PER_IN = 2.54

    fun convert(value: Double, from: String, to: String): Double = when {
        from == to -> value
        from == "mmol/L" && to == "mg/dL" -> value * MG_DL_PER_MMOL_L
        from == "mg/dL" && to == "mmol/L" -> value / MG_DL_PER_MMOL_L
        from == "lb" && to == "kg" -> value * KG_PER_LB
        from == "kg" && to == "lb" -> value / KG_PER_LB
        from == "in" && to == "cm" -> value * CM_PER_IN
        from == "cm" && to == "in" -> value / CM_PER_IN
        from == "°F" && to == "°C" -> (value - 32.0) * 5.0 / 9.0
        from == "°C" && to == "°F" -> value * 9.0 / 5.0 + 32.0
        else -> throw IllegalArgumentException("Sin conversión de $from a $to")
    }

    fun canConvert(from: String, to: String): Boolean = runCatching { convert(1.0, from, to) }.isSuccess
}
