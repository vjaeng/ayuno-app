package com.ayuno.domain.derived

import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import java.time.LocalDate
import java.time.LocalDateTime

/** Una fila de «Mediciones de este ayuno» (H-048). [edited]: tiene al menos una corrección en la bitácora. */
sealed interface MeasurementItem {
    val id: String
    val at: LocalDateTime
    val metricTypeCode: String
    val kind: EntryKind
    val edited: Boolean

    data class OfMeasurement(val value: Measurement, override val edited: Boolean) : MeasurementItem {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val metricTypeCode get() = value.metricTypeCode
        override val kind get() = EntryKind.MEASUREMENT
    }

    data class OfBloodPressure(val value: BloodPressureReading, override val edited: Boolean) : MeasurementItem {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val metricTypeCode get() = MetricType.BLOOD_PRESSURE_CODE
        override val kind get() = EntryKind.BLOOD_PRESSURE
    }
}

/** Derivado «Mediciones del ayuno» (MODELO-DATOS §2.5, D-20): solo las visibles, de la más reciente a la más antigua. */
object FastMeasurements {

    fun of(measurements: List<Measurement>, readings: List<BloodPressureReading>, editedIds: Set<String>): List<MeasurementItem> =
        (measurements.map { MeasurementItem.OfMeasurement(it, it.id in editedIds) } + readings.map { MeasurementItem.OfBloodPressure(it, it.id in editedIds) })
            .sortedWith(compareByDescending<MeasurementItem> { it.at }.thenBy { it.id })

    /** Tipos presentes, en el orden en que aparecen en la lista (filtro por tipo). */
    fun types(items: List<MeasurementItem>): List<String> = items.map { it.metricTypeCode }.distinct()

    /** [code] nulo = todas. */
    fun filter(items: List<MeasurementItem>, code: String?): List<MeasurementItem> =
        if (code == null) items else items.filter { it.metricTypeCode == code }

    /** Agrupa por día natural, del más reciente al más antiguo, conservando el orden de [items]. */
    fun byDay(items: List<MeasurementItem>): List<Pair<LocalDate, List<MeasurementItem>>> =
        items.groupBy { it.at.toLocalDate() }.toList().sortedByDescending { it.first }
}
