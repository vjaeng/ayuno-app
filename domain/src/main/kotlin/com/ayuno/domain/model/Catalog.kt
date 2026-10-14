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
        const val WEIGHT_CODE = "weight"
    }
}

/** Rangos válidos de la presión, fuera del modelo (P-11, ADR-008). */
data class BloodPressureRanges(
    val systolic: ClosedFloatingPointRange<Double>,
    val diastolic: ClosedFloatingPointRange<Double>,
    val pulse: ClosedFloatingPointRange<Double>,
)

/** `symptom_type` (H-010). */
data class SymptomType(
    val code: String,
    val name: String,
    val groupName: String?,
    val isAlarm: Boolean,
    val active: Boolean,
)

/** `intake_type` (H-011). */
data class IntakeType(
    val code: String,
    val name: String,
    val unit: String,
    val breaksFast: Boolean,
    val sodiumFactor: Double?,
)

data class ProductSchedule(val id: String, val timeOfDay: java.time.LocalTime, val amount: Double)

/** `product` con sus `product_schedule` (H-013). */
data class Product(
    val code: String,
    val name: String,
    val kind: ProductKind,
    val defaultAmount: Double,
    val unit: String,
    val avoid: Boolean,
    val active: Boolean,
    val schedules: List<ProductSchedule>,
)
