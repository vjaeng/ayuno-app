package com.ayuno.domain.derived

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.Meal
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.Symptom
import java.time.LocalDate
import java.time.LocalDateTime

/** Tipos de entrada de la línea de tiempo (filtro por tipo del Historial). */
enum class TimelineKind { MEASUREMENT, SYMPTOM, INTAKE, PRODUCT, ALERT, NOTE, REFEED, MEAL }

/** Una entrada de la línea de tiempo de un ayuno (H-033). */
sealed interface TimelineEntry {
    val id: String
    val at: LocalDateTime
    val kind: TimelineKind

    data class OfMeasurement(val value: Measurement) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val kind get() = TimelineKind.MEASUREMENT
    }

    data class OfBloodPressure(val value: BloodPressureReading) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val kind get() = TimelineKind.MEASUREMENT
    }

    data class OfSymptom(val value: Symptom) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val kind get() = TimelineKind.SYMPTOM
    }

    data class OfIntake(val value: Intake) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.takenAt
        override val kind get() = TimelineKind.INTAKE
    }

    /** Una toma «no tomada» no tiene hora real: se coloca en su hora programada. */
    data class OfProductLog(val value: ProductLog, override val at: LocalDateTime) : TimelineEntry {
        override val id get() = value.id
        override val kind get() = TimelineKind.PRODUCT
    }

    data class OfAlert(val value: Alert) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.firedAt
        override val kind get() = TimelineKind.ALERT
    }

    data class OfNote(val value: Note) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.notedAt
        override val kind get() = TimelineKind.NOTE
    }

    /** Solo las casillas marcadas: el paso hecho, en su hora real. */
    data class OfRefeed(val check: RefeedCheck, val step: RefeedStep?, override val at: LocalDateTime) : TimelineEntry {
        override val id get() = check.id
        override val kind get() = TimelineKind.REFEED
    }

    data class OfMeal(val value: Meal) : TimelineEntry {
        override val id get() = value.id
        override val at get() = value.eatenAt
        override val kind get() = TimelineKind.MEAL
    }
}

/** Todo lo registrado en un ayuno. */
data class FastRecords(
    val measurements: List<Measurement> = emptyList(),
    val readings: List<BloodPressureReading> = emptyList(),
    val symptoms: List<Symptom> = emptyList(),
    val intakes: List<Intake> = emptyList(),
    val productLogs: List<ProductLog> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val notes: List<Note> = emptyList(),
    val refeedChecks: List<RefeedCheck> = emptyList(),
    val meals: List<Meal> = emptyList(),
)

object Timeline {

    /** Todas las entradas del ayuno en orden de hora (empates: en el orden de tipos de [TimelineKind]). */
    fun of(fast: Fast, steps: List<RefeedStep>, records: FastRecords): List<TimelineEntry> {
        val byStep = steps.associateBy { it.id }
        val fallback = fast.clockStartActual ?: fast.clockStartPlanned
        return buildList {
            records.measurements.forEach { add(TimelineEntry.OfMeasurement(it)) }
            records.readings.forEach { add(TimelineEntry.OfBloodPressure(it)) }
            records.symptoms.forEach { add(TimelineEntry.OfSymptom(it)) }
            records.intakes.forEach { add(TimelineEntry.OfIntake(it)) }
            records.productLogs.forEach { add(TimelineEntry.OfProductLog(it, it.takenAt ?: it.scheduledAt ?: fallback)) }
            records.alerts.forEach { add(TimelineEntry.OfAlert(it)) }
            records.notes.forEach { add(TimelineEntry.OfNote(it)) }
            records.refeedChecks.forEach { c -> c.doneAt?.let { add(TimelineEntry.OfRefeed(c, byStep[c.refeedStepId], it)) } }
            records.meals.forEach { add(TimelineEntry.OfMeal(it)) }
        }.sortedWith(compareBy({ it.at }, { it.kind.ordinal }))
    }

    /** Agrupa por día natural, del más antiguo al más reciente. */
    fun byDay(entries: List<TimelineEntry>): List<Pair<LocalDate, List<TimelineEntry>>> =
        entries.groupBy { it.at.toLocalDate() }.toSortedMap().map { (day, list) -> day to list }
}
