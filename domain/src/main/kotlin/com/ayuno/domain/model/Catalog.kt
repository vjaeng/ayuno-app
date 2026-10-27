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
    /** H-049: palabras para elegir en lugar de teclear (sin las borradas), en orden de `position`. */
    val options: List<MetricOption> = emptyList(),
) {
    val isBloodPressure: Boolean get() = code == BLOOD_PRESSURE_CODE
    val isGlucose: Boolean get() = code == GLUCOSE_CODE

    fun isOutOfRange(value: Double): Boolean =
        (minValid != null && value < minValid) || (maxValid != null && value > maxValid)

    /** Derivado «Palabra de una medición» (MODELO §2.5, D-21): la palabra cuyo número es el guardado, si hay. */
    fun optionFor(valueNum: Double?): MetricOption? = valueNum?.let { v -> options.firstOrNull { it.value == v } }

    companion object {
        const val MAX_OPTIONS = 9
        const val BLOOD_PRESSURE_CODE = "blood_pressure"
        const val GLUCOSE_CODE = "glucose"
        const val WEIGHT_CODE = "weight"
    }
}

/** `metric_option` (H-049, D-21): palabra con el número que se guarda al elegirla. */
data class MetricOption(
    val id: String,
    val metricTypeCode: String,
    val label: String,
    val value: Double,
    val position: Int,
)

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
    /** H-051: `code` de la medida del botón rápido; nulo = sin botón. */
    val quickUnitCode: String? = null,
    /** H-050: medidas caseras (sin las borradas), en orden de `position`. */
    val units: List<HouseholdUnit> = emptyList(),
) {
    fun unit(code: String?): HouseholdUnit? = code?.let { c -> units.firstOrNull { it.code == c } }
    val quickUnit: HouseholdUnit? get() = if (breaksFast) null else unit(quickUnitCode)
}

/** `household_unit` (H-050, D-22): medida casera con su equivalencia en la unidad del tipo. */
data class HouseholdUnit(
    val id: String,
    val code: String,
    val intakeTypeCode: String,
    val name: String,
    val equivalent: Double,
    val imprecise: Boolean,
    val position: Int,
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
