package com.ayuno.domain.model

import java.time.LocalDateTime

/** Entradas que se pueden ver y corregir en «Mediciones de este ayuno» (H-048); [entityName] es la tabla en `edit_log`. */
enum class EntryKind(val entityName: String) { MEASUREMENT("measurement"), BLOOD_PRESSURE("blood_pressure_reading"), INTAKE("intake") }

/** Entrada del registro sobre la que se pueden evaluar reglas. */
sealed interface RecordEntry {
    val id: String
    val fastId: String
    val takenAt: LocalDateTime
}

data class Measurement(
    override val id: String,
    override val fastId: String,
    val metricTypeCode: String,
    override val takenAt: LocalDateTime,
    val scheduledFor: LocalDateTime?,
    val valueNum: Double?,
    val valueText: String?,
    val valueEntered: Double?,
    val unitEntered: String?,
    val source: GlucoseSource?,
    val trend: Trend?,
    val onWaking: Boolean,
    val notDoneReason: String?,
    val note: String?,
    /** H-055 (D-24): marcada «de partida». */
    val isBaseline: Boolean = false,
) : RecordEntry

data class BloodPressureReading(
    override val id: String,
    override val fastId: String,
    override val takenAt: LocalDateTime,
    val scheduledFor: LocalDateTime?,
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val position: Position,
    val onWaking: Boolean,
    val pairedReadingId: String?,
    val notDoneReason: String?,
    val note: String?,
    /** H-053: segundos de pie, calculados; nulo = «tomada sin esperar». */
    val secondsStanding: Int? = null,
    /** H-055 (D-24): marcada «de partida». */
    val isBaseline: Boolean = false,
) : RecordEntry {
    fun value(component: BpComponent): Int? = when (component) {
        BpComponent.SYSTOLIC -> systolic
        BpComponent.DIASTOLIC -> diastolic
        BpComponent.PULSE -> pulse
    }
}

/** `symptom` (H-010). */
data class Symptom(
    override val id: String,
    override val fastId: String,
    val symptomTypeCode: String,
    override val takenAt: LocalDateTime,
    val severity: Int,
    val note: String?,
) : RecordEntry

/**
 * `intake` (H-012). [amount] y [unit] son el equivalente en la unidad del tipo (lo que suman totales y reglas);
 * [amountEntered] y [unitEntered] lo tecleado, con la unidad del tipo o el `code` de una medida casera (D-22).
 */
data class Intake(
    override val id: String,
    override val fastId: String,
    val intakeTypeCode: String,
    override val takenAt: LocalDateTime,
    val amount: Double,
    val unit: String,
    val note: String?,
    val amountEntered: Double = amount,
    val unitEntered: String = unit,
    /** H-051: «Agua con sal», ingestas guardadas juntas. */
    val groupId: String? = null,
) : RecordEntry

/** `product_log` (H-015). */
data class ProductLog(
    val id: String,
    val fastId: String,
    val productCode: String,
    val scheduledAt: LocalDateTime?,
    val status: ProductLogStatus,
    val takenAt: LocalDateTime?,
    val plannedAmount: Double?,
    val amount: Double?,
    val unit: String?,
    val reason: String?,
)

/** `note` (H-035). */
data class Note(val id: String, val fastId: String, val notedAt: LocalDateTime, val text: String)

/** `refeed_check` (H-031): una ocurrencia de un paso de la vuelta a comer; [doneAt] es la hora real de la casilla. */
data class RefeedCheck(val id: String, val refeedStepId: String, val scheduledAt: LocalDateTime, val doneAt: LocalDateTime?)

/** `meal` (H-032). [toleranceDetail] solo con «otro». */
data class Meal(
    val id: String,
    val fastId: String,
    val eatenAt: LocalDateTime,
    val description: String,
    val approxAmount: String?,
    val tolerance: MealTolerance,
    val toleranceDetail: String?,
)
