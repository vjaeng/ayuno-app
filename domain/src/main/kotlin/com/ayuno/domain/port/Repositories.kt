package com.ayuno.domain.port

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureRanges
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeMetricTypes(): Flow<List<MetricType>>
    suspend fun metricTypes(): List<MetricType>
    suspend fun metricType(code: String): MetricType?
    suspend fun saveMetricType(type: MetricType)
    suspend fun bloodPressureRanges(): BloodPressureRanges
}

interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>
    suspend fun get(id: String): TemplateWithPlan?

    /** Guarda la plantilla y su plan; los hijos que ya no están se borran en blando. */
    suspend fun save(template: TemplateWithPlan)
}

interface FastRepository {
    fun observeFasts(): Flow<List<Fast>>
    suspend fun all(): List<Fast>
    suspend fun get(id: String): FastWithPlan?
    suspend fun insert(fast: FastWithPlan)
    suspend fun update(fast: Fast)
}

interface RecordRepository {
    fun observeMeasurements(fastId: String): Flow<List<Measurement>>
    fun observeBloodPressure(fastId: String): Flow<List<BloodPressureReading>>
    suspend fun measurements(fastId: String): List<Measurement>
    suspend fun bloodPressure(fastId: String): List<BloodPressureReading>
    suspend fun insertMeasurement(measurement: Measurement)
    suspend fun insertBloodPressure(reading: BloodPressureReading)
}

interface AlertRepository {
    fun observeAlerts(fastId: String): Flow<List<Alert>>
    suspend fun alerts(fastId: String): List<Alert>
    suspend fun get(id: String): Alert?
    suspend fun insert(alert: Alert)
    suspend fun update(alert: Alert)
}

interface AuditRepository {
    suspend fun append(entry: EditLogEntry)
    suspend fun entriesFor(rowId: String): List<EditLogEntry>
}
