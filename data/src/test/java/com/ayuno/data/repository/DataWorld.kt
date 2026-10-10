package com.ayuno.data.repository

import androidx.test.core.app.ApplicationProvider
import com.ayuno.data.db.AyunoDatabase
import com.ayuno.data.db.AyunoDatabaseFactory
import com.ayuno.data.db.RoomTransactionRunner
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.CorrectClockStart
import com.ayuno.domain.usecase.DecideAlert
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.SaveTemplate
import com.ayuno.domain.usecase.StartFast
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Repositorios Room reales (SQLite sin cifrar en memoria) con los casos de uso del dominio. */
class DataWorld {
    var now: Instant = Instant.parse("2026-10-22T23:00:00Z")
    val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("-05:00")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val database: AyunoDatabase = AyunoDatabaseFactory.createInMemoryForTests(context, clock)
    val tx = RoomTransactionRunner(database)
    val catalog = RoomCatalogRepository(database, context, clock)
    val templates = RoomTemplateRepository(database, clock)
    val fasts = RoomFastRepository(database, clock)
    val records = RoomRecordRepository(database, clock)
    val alerts = RoomAlertRepository(database, clock)
    val audit = RoomAuditRepository(database, clock)
    val refeed = RoomRefeedRepository(database, clock)

    val saveTemplate = SaveTemplate(templates)
    val createFast = CreateFast(templates, fasts, tx, clock)
    val startFast = StartFast(fasts, tx, clock)
    val correctClockStart = CorrectClockStart(fasts, audit, tx, clock)
    val recordMeasurement = RecordMeasurement(catalog, fasts, records, alerts, tx, clock)
    val recordBloodPressure = RecordBloodPressure(catalog, fasts, records, alerts, tx, clock)
    val decideAlert = DecideAlert(alerts)
}
