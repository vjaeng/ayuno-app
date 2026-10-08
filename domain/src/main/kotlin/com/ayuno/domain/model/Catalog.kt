package com.ayuno.domain.model

/** `metric_type` (H-007). Las escalas 0-10 y la presión arterial son tipos del catálogo precargado. */
data class MetricType(
    val code: String,
    val name: String,
    val unit: String,
    val altUnit: String?,
    val entryUnit: String,
    val isNumeric: Boolean,
    val minValid: Double?,
    val maxValid: Double?,
    val active: Boolean,
) {
    val isBloodPressure: Boolean get() = code == BLOOD_PRESSURE_CODE
    val isGlucose: Boolean get() = code == GLUCOSE_CODE

    fun isOutOfRange(value: Double): Boolean =
        (minValid != null && value < minValid) || (maxValid != null && value > maxValid)

    companion object {
        const val BLOOD_PRESSURE_CODE = "blood_pressure"
        const val GLUCOSE_CODE = "glucose"
    }
}

/** Rangos válidos de la presión, fuera del modelo (P-11, ADR-008). */
data class BloodPressureRanges(
    val systolic: ClosedFloatingPointRange<Double>,
    val diastolic: ClosedFloatingPointRange<Double>,
    val pulse: ClosedFloatingPointRange<Double>,
)
