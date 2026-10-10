package com.ayuno.domain.derived

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.Settings
import java.time.LocalDate
import java.time.LocalDateTime

/** H-018: lo que toca un día natural (mediciones, tomas, recordatorios de reglas y vuelta a comer) con su estado. */
object DayAgenda {
    fun of(
        source: FastWithPlan,
        activeTypes: Set<String>,
        products: List<Product>,
        alerts: List<Alert>,
        records: PendingRecords,
        day: LocalDate,
        now: LocalDateTime,
    ): List<PendingStatus> {
        val from = day.atStartOfDay()
        val to = day.plusDays(1).atStartOfDay().minusSeconds(1)
        val planned = Pending.items(source, activeTypes, products, from, to)
        val followUps = Pending.ruleFollowUps(source, alerts).filter { !it.scheduledAt.isBefore(from) && !it.scheduledAt.isAfter(to) }
        val refeed = Pending.refeedItems(source.plan, records.refeedChecks, from, to)
        return (planned + followUps + refeed).sortedBy { it.scheduledAt }.map { Pending.status(it, now, records) }
    }

    /**
     * ¿Se recuerda este pendiente? Los de reglas siempre; los pasos de fecha (reintroducción) nunca; el resto, no
     * durante el silencio nocturno (H-017).
     */
    fun isNotified(item: PendingItem, settings: Settings): Boolean =
        item.fromRule || (!item.allDay && !settings.isQuiet(item.scheduledAt.toLocalTime()))
}

/**
 * H-008: se propone marcar «al despertar» en la primera lectura tras el fin del silencio nocturno de hoy
 * (si no hay ninguna medición ni presión anotada desde entonces).
 */
object WakingProposal {
    fun propose(now: LocalDateTime, settings: Settings, measurements: List<Measurement>, readings: List<BloodPressureReading>): Boolean {
        val end = settings.quietEnd ?: return false
        if (settings.quietStart == null) return false
        val wake = now.toLocalDate().atTime(end)
        if (now.isBefore(wake)) return false
        val since = { t: LocalDateTime -> !t.isBefore(wake) && !t.isAfter(now) }
        return measurements.none { since(it.takenAt) } && readings.none { since(it.takenAt) }
    }
}
