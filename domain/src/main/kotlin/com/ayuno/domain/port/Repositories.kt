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
import com.ayuno.domain.model.Meal
import com.ayuno.domain.model.ExportRequest
import com.ayuno.domain.model.ImportCheck
import com.ayuno.domain.model.ImportPreview
import com.ayuno.domain.model.ImportResult
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.PersonProfile
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.Settings
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.RefeedCheck
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

    /** H-003: plantillas borradas (en blando), para restaurarlas. */
    fun observeDeleted(): Flow<List<Template>>

    /** Borra (o restaura) solo la plantilla: su plan se conserva para restaurarla entera. */
    suspend fun setDeleted(id: String, deleted: Boolean)
}

interface FastRepository {
    fun observeFasts(): Flow<List<Fast>>
    suspend fun all(): List<Fast>
    suspend fun get(id: String): FastWithPlan?
    suspend fun insert(fast: FastWithPlan)
    suspend fun update(fast: Fast)

    /** Actualiza el plan copiado del ayuno (lista previa marcada, regla manual creada al vuelo). */
    suspend fun updatePlan(fastId: String, plan: Plan)

    /** H-044 (D-16): retira (borrado blando) la copia actual del plan y escribe [plan] en su lugar. */
    suspend fun replacePlan(fastId: String, plan: Plan)

    /** H-045 (D-15): elimina el ayuno y todo lo que cuelga de él, incluida su bitácora. */
    suspend fun deleteForever(fastId: String)
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
    suspend fun notes(fastId: String): List<Note>
    suspend fun insertNote(note: Note)
}

/** Vuelta a comer (H-031, H-032): casillas de los pasos del plan del ayuno y comidas. */
interface RefeedRepository {
    fun observeChecks(fastId: String): Flow<List<RefeedCheck>>
    suspend fun checks(fastId: String): List<RefeedCheck>
    suspend fun check(id: String): RefeedCheck?
    suspend fun insertChecks(checks: List<RefeedCheck>)
    suspend fun updateCheck(check: RefeedCheck)

    fun observeMeals(fastId: String): Flow<List<Meal>>
    suspend fun meals(fastId: String): List<Meal>
    suspend fun insertMeal(meal: Meal)
}

interface AlertRepository {
    fun observeAlerts(fastId: String): Flow<List<Alert>>
    suspend fun alerts(fastId: String): List<Alert>
    suspend fun get(id: String): Alert?
    suspend fun insert(alert: Alert)
    suspend fun update(alert: Alert)
}

interface SettingsRepository {
    fun observe(): Flow<Settings>
    suspend fun get(): Settings
    suspend fun save(settings: Settings)
}

interface PersonProfileRepository {
    fun observe(): Flow<PersonProfile>
    suspend fun get(): PersonProfile
    suspend fun save(profile: PersonProfile)
}

/**
 * Formato de intercambio (ADR-007) e importación todo o nada (ADR-SEG-006). Qué se exporta lo decide el caso de
 * uso con [ExportRequest]; esta capa serializa, valida y escribe.
 */
interface DataExchange {
    suspend fun export(request: ExportRequest): String

    /** H-037: archivo comprimido con una tabla por lista, con las mismas columnas que el JSON. */
    fun tables(json: String): ByteArray

    suspend fun preview(content: ByteArray): ImportCheck<ImportPreview>
    suspend fun import(content: ByteArray): ImportCheck<ImportResult>
}

interface AuditRepository {
    suspend fun append(entry: EditLogEntry)
    suspend fun entriesFor(rowId: String): List<EditLogEntry>
}
