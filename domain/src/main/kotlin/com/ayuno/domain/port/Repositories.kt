package com.ayuno.domain.port

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureRanges
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeMetricTypes(): Flow<List<MetricType>>
    suspend fun metricTypes(): List<MetricType>
    suspend fun metricType(code: String): MetricType?
    suspend fun saveMetricType(type: MetricType)
    suspend fun bloodPressureRanges(): BloodPressureRanges

    fun observeSymptomTypes(): Flow<List<SymptomType>>
    suspend fun symptomTypes(): List<SymptomType>
    suspend fun saveSymptomType(type: SymptomType)

    fun observeIntakeTypes(): Flow<List<IntakeType>>
    suspend fun intakeTypes(): List<IntakeType>
    suspend fun saveIntakeType(type: IntakeType)

    fun observeProducts(): Flow<List<Product>>
    suspend fun products(): List<Product>
    suspend fun product(code: String): Product?

    /** Guarda el producto y sus horarios; los horarios que ya no están se borran en blando. */
    suspend fun saveProduct(product: Product)
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

    /** Actualiza el plan copiado del ayuno (lista previa marcada, regla manual creada al vuelo). */
    suspend fun updatePlan(fastId: String, plan: Plan)
}

interface RecordRepository {
    fun observeMeasurements(fastId: String): Flow<List<Measurement>>
    fun observeBloodPressure(fastId: String): Flow<List<BloodPressureReading>>
    suspend fun measurements(fastId: String): List<Measurement>
    suspend fun bloodPressure(fastId: String): List<BloodPressureReading>
    suspend fun insertMeasurement(measurement: Measurement)
    suspend fun insertBloodPressure(reading: BloodPressureReading)

    fun observeSymptoms(fastId: String): Flow<List<Symptom>>
    suspend fun symptoms(fastId: String): List<Symptom>
    suspend fun insertSymptom(symptom: Symptom)

    fun observeIntakes(fastId: String): Flow<List<Intake>>
    suspend fun intakes(fastId: String): List<Intake>
    suspend fun insertIntake(intake: Intake)

    fun observeProductLogs(fastId: String): Flow<List<ProductLog>>
    suspend fun productLogs(fastId: String): List<ProductLog>
    suspend fun insertProductLog(log: ProductLog)

    fun observeNotes(fastId: String): Flow<List<Note>>
    suspend fun insertNote(note: Note)
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
