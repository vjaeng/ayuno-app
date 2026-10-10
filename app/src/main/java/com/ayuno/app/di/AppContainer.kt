package com.ayuno.app.di

import android.content.Context
import com.ayuno.app.reminders.ReminderScheduler
import com.ayuno.data.db.AyunoDatabase
import com.ayuno.data.db.AyunoDatabaseFactory
import com.ayuno.data.db.RoomTransactionRunner
import com.ayuno.data.repository.RoomAlertRepository
import com.ayuno.data.repository.RoomAuditRepository
import com.ayuno.data.repository.RoomCatalogRepository
import com.ayuno.data.repository.RoomFastRepository
import com.ayuno.data.repository.RoomRecordRepository
import com.ayuno.data.repository.RoomTemplateRepository
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.TemplateRepository
import com.ayuno.domain.port.TransactionRunner
import com.ayuno.domain.usecase.CorrectClockStart
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.DecideAlert
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RegisterLastMeal
import com.ayuno.domain.usecase.SaveMetricType
import com.ayuno.domain.usecase.SaveTemplate
import com.ayuno.domain.usecase.SetMetricTypeActive
import com.ayuno.domain.usecase.StartFast
import java.time.Clock

/**
 * Raíz de composición (ADR-009): único lugar de `:app` que conoce `:data`.
 * La base se abre de forma perezosa; las pruebas inyectan una base en memoria y un reloj controlado.
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    databaseProvider: ((Context, Clock) -> AyunoDatabase)? = null,
) {
    private val appContext = context.applicationContext

    val database: AyunoDatabase by lazy {
        (databaseProvider ?: AyunoDatabaseFactory::create)(appContext, clock)
    }

    val transactions: TransactionRunner by lazy { RoomTransactionRunner(database) }
    val catalog: CatalogRepository by lazy { RoomCatalogRepository(database, appContext, clock) }
    val templates: TemplateRepository by lazy { RoomTemplateRepository(database, clock) }
    val fasts: FastRepository by lazy { RoomFastRepository(database, clock) }
    val records: RecordRepository by lazy { RoomRecordRepository(database, clock) }
    val alerts: AlertRepository by lazy { RoomAlertRepository(database, clock) }
    private val audit by lazy { RoomAuditRepository(database, clock) }

    val saveMetricType by lazy { SaveMetricType(catalog) }
    val setMetricTypeActive by lazy { SetMetricTypeActive(catalog) }
    val saveTemplate by lazy { SaveTemplate(templates) }
    val createFast by lazy { CreateFast(templates, fasts, transactions, clock) }
    val startFast by lazy { StartFast(fasts, transactions, clock) }
    val registerLastMeal by lazy { RegisterLastMeal(fasts, audit, transactions, clock) }
    val correctClockStart by lazy { CorrectClockStart(fasts, audit, transactions, clock) }
    val refeed: com.ayuno.domain.port.RefeedRepository by lazy { com.ayuno.data.repository.RoomRefeedRepository(database, clock) }
    val endFast by lazy { EndFast(fasts, refeed, transactions, clock) }
    val setRefeedCheck by lazy { com.ayuno.domain.usecase.SetRefeedCheck(refeed, audit, transactions, clock) }
    val recordMeal by lazy { com.ayuno.domain.usecase.RecordMeal(fasts, refeed) }
    val recordMeasurement by lazy { RecordMeasurement(catalog, fasts, records, alerts, transactions, clock) }
    val recordBloodPressure by lazy { RecordBloodPressure(catalog, fasts, records, alerts, transactions, clock) }
    val decideAlert by lazy { DecideAlert(alerts) }
    val saveSymptomType by lazy { com.ayuno.domain.usecase.SaveSymptomType(catalog) }
    val setSymptomTypeActive by lazy { com.ayuno.domain.usecase.SetSymptomTypeActive(catalog) }
    val saveIntakeType by lazy { com.ayuno.domain.usecase.SaveIntakeType(catalog) }
    val saveProduct by lazy { com.ayuno.domain.usecase.SaveProduct(catalog) }
    val setProductActive by lazy { com.ayuno.domain.usecase.SetProductActive(catalog) }
    val recordSymptom by lazy { com.ayuno.domain.usecase.RecordSymptom(catalog, fasts, records, alerts, transactions, clock) }
    val recordIntake by lazy { com.ayuno.domain.usecase.RecordIntake(catalog, fasts, records, alerts, transactions, clock) }
    val recordProductLog by lazy { com.ayuno.domain.usecase.RecordProductLog(catalog, fasts, records, transactions) }
    val recordNote by lazy { com.ayuno.domain.usecase.RecordNote(fasts, records) }
    val triggerManualAlert by lazy { com.ayuno.domain.usecase.TriggerManualAlert(fasts, alerts, transactions, clock) }
    val evaluateRules by lazy { com.ayuno.domain.usecase.EvaluateRules(catalog, fasts, records, alerts, transactions, clock) }
    val setChecklistItem by lazy { com.ayuno.domain.usecase.SetChecklistItem(fasts) }

    val settings: com.ayuno.domain.port.SettingsRepository by lazy { com.ayuno.data.repository.RoomSettingsRepository(database) }
    val saveQuietHours by lazy { com.ayuno.domain.usecase.SaveQuietHours(settings) }

    val personProfile: com.ayuno.domain.port.PersonProfileRepository by lazy { com.ayuno.data.repository.RoomPersonProfileRepository(database, clock) }
    val savePersonProfile by lazy { com.ayuno.domain.usecase.SavePersonProfile(personProfile, settings) }
    private val exchange by lazy { com.ayuno.data.exchange.JsonDataExchange(database) }
    val exportData by lazy { com.ayuno.domain.usecase.ExportData(fasts, settings, personProfile, exchange, clock, com.ayuno.app.BuildConfig.VERSION_NAME) }
    val previewImport by lazy { com.ayuno.domain.usecase.PreviewImport(exchange) }
    val importData by lazy { com.ayuno.domain.usecase.ImportData(exchange) }
    fun exportTables(json: String): ByteArray = exchange.tables(json)

    val reminders: ReminderScheduler by lazy { ReminderScheduler(appContext, fasts, catalog, records, alerts, refeed, settings, clock) }
}
