package com.ayuno.domain.model

import java.time.LocalDateTime

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

/** `intake` (H-012). */
data class Intake(
    override val id: String,
    override val fastId: String,
    val intakeTypeCode: String,
    override val takenAt: LocalDateTime,
    val amount: Double,
    val unit: String,
    val note: String?,
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
