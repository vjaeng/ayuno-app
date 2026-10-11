package com.ayuno.domain.fakes

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
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.RefeedRepository
import com.ayuno.domain.port.TemplateRepository
import com.ayuno.domain.port.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Reloj de prueba avanzable, en hora de Colombia (-05:00). */
class TestClock(var instant: Instant = Instant.parse("2026-10-22T23:00:00Z"), private val zone: ZoneId = ZoneId.of("-05:00")) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(instant, zone)
    override fun instant(): Instant = instant
    fun advanceMinutes(minutes: Long) { instant = instant.plusSeconds(minutes * 60) }
}

object SeedCatalog {
    val glucose = MetricType("glucose", "Glucosa", "mg/dL", "mmol/L", "mg/dL", true, 20.0, 600.0, true)
    val weight = MetricType("weight", "Peso", "kg", "lb", "kg", true, 20.0, 300.0, true)
    val bloodPressure = MetricType("blood_pressure", "Presión arterial", "mmHg", null, "mmHg", true, null, null, true)
    val hunger = MetricType("hunger", "Hambre", "0-10", null, "0-10", true, 0.0, 10.0, true)
    val all = listOf(glucose, weight, bloodPressure, hunger)
    val headache = SymptomType("headache", "Dolor de cabeza", "Esperables", false, true)
    val blurredVision = SymptomType("blurred_vision", "Visión borrosa", "Alarma", true, true)
    val water = IntakeType("water", "Agua", "mL", false, null)
    val salt = IntakeType("salt", "Sal", "g", false, 0.393)
    val broth = IntakeType("broth", "Caldo", "mL", true, null)
    val bpRanges = BloodPressureRanges(50.0..250.0, 30.0..150.0, 30.0..220.0)
}

class FakeCatalog(types: List<MetricType> = SeedCatalog.all) : CatalogRepository {
    val types = MutableStateFlow(types)
    override fun observeMetricTypes(): Flow<List<MetricType>> = types
    override suspend fun metricTypes() = types.value
    override suspend fun metricType(code: String) = types.value.firstOrNull { it.code == code }
    override suspend fun saveMetricType(type: MetricType) {
        types.value = types.value.filterNot { it.code == type.code } + type
    }
    override suspend fun bloodPressureRanges() = SeedCatalog.bpRanges

    val symptomTypes = MutableStateFlow(listOf(SeedCatalog.headache, SeedCatalog.blurredVision))
    override fun observeSymptomTypes(): Flow<List<SymptomType>> = symptomTypes
    override suspend fun symptomTypes() = symptomTypes.value
    override suspend fun saveSymptomType(type: SymptomType) { symptomTypes.value = symptomTypes.value.filterNot { it.code == type.code } + type }

    val intakeTypes = MutableStateFlow(listOf(SeedCatalog.water, SeedCatalog.salt, SeedCatalog.broth))
    override fun observeIntakeTypes(): Flow<List<IntakeType>> = intakeTypes
    override suspend fun intakeTypes() = intakeTypes.value
    override suspend fun saveIntakeType(type: IntakeType) { intakeTypes.value = intakeTypes.value.filterNot { it.code == type.code } + type }

    val products = MutableStateFlow<List<Product>>(emptyList())
    override fun observeProducts(): Flow<List<Product>> = products
    override suspend fun products() = products.value
    override suspend fun product(code: String) = products.value.firstOrNull { it.code == code }
    override suspend fun saveProduct(product: Product) { products.value = products.value.filterNot { it.code == product.code } + product }
}

class FakeTemplates : TemplateRepository {
    val items = MutableStateFlow<Map<String, TemplateWithPlan>>(emptyMap())
    override suspend fun get(id: String) = items.value[id]?.takeIf { id !in deleted.value }
    override suspend fun save(template: TemplateWithPlan) { items.value = items.value + (template.template.id to template); deleted.value = deleted.value - template.template.id }
    val deleted = MutableStateFlow<Set<String>>(emptySet())
    override fun observeTemplates(): Flow<List<Template>> = items.map { m -> m.values.filter { it.template.id !in deleted.value }.map { it.template } }
    override fun observeDeleted(): Flow<List<Template>> = items.map { m -> m.values.filter { it.template.id in deleted.value }.map { it.template } }
    override suspend fun setDeleted(id: String, deleted: Boolean) { this.deleted.value = if (deleted) this.deleted.value + id else this.deleted.value - id }
}

class FakeFasts : FastRepository {
    val items = MutableStateFlow<Map<String, FastWithPlan>>(emptyMap())
    override fun observeFasts(): Flow<List<Fast>> = items.map { m -> m.values.map { it.fast } }
    override suspend fun all() = items.value.values.map { it.fast }
    override suspend fun get(id: String) = items.value[id]
    override suspend fun insert(fast: FastWithPlan) { items.value = items.value + (fast.fast.id to fast) }
    override suspend fun update(fast: Fast) {
        val current = items.value.getValue(fast.id)
        items.value = items.value + (fast.id to current.copy(fast = fast))
    }
    override suspend fun updatePlan(fastId: String, plan: Plan) {
        val current = items.value.getValue(fastId)
        items.value = items.value + (fastId to current.copy(plan = plan))
    }
    override suspend fun replacePlan(fastId: String, plan: Plan) {
        val current = items.value.getValue(fastId)
        items.value = items.value + (fastId to current.copy(plan = plan, replacedRules = current.replacedRules + current.plan.rules))
    }
    override suspend fun deleteForever(fastId: String) { items.value = items.value - fastId }
}

class FakeRecords : RecordRepository {
    val measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val readings = MutableStateFlow<List<BloodPressureReading>>(emptyList())
    override fun observeMeasurements(fastId: String) = measurements.map { l -> l.filter { it.fastId == fastId } }
    override fun observeBloodPressure(fastId: String) = readings.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun measurements(fastId: String) = measurements.value.filter { it.fastId == fastId }
    override suspend fun bloodPressure(fastId: String) = readings.value.filter { it.fastId == fastId }
    override suspend fun insertMeasurement(measurement: Measurement) { measurements.value = measurements.value + measurement }
    override suspend fun insertBloodPressure(reading: BloodPressureReading) { readings.value = readings.value + reading }

    val symptoms = MutableStateFlow<List<Symptom>>(emptyList())
    override fun observeSymptoms(fastId: String) = symptoms.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun symptoms(fastId: String) = symptoms.value.filter { it.fastId == fastId }
    override suspend fun insertSymptom(symptom: Symptom) { symptoms.value = symptoms.value + symptom }

    val intakes = MutableStateFlow<List<Intake>>(emptyList())
    override fun observeIntakes(fastId: String) = intakes.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun intakes(fastId: String) = intakes.value.filter { it.fastId == fastId }
    override suspend fun insertIntake(intake: Intake) { intakes.value = intakes.value + intake }

    val productLogs = MutableStateFlow<List<ProductLog>>(emptyList())
    override fun observeProductLogs(fastId: String) = productLogs.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun productLogs(fastId: String) = productLogs.value.filter { it.fastId == fastId }
    override suspend fun insertProductLog(log: ProductLog) { productLogs.value = productLogs.value + log }

    val notes = MutableStateFlow<List<Note>>(emptyList())
    override fun observeNotes(fastId: String) = notes.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun notes(fastId: String) = notes.value.filter { it.fastId == fastId }
    override suspend fun insertNote(note: Note) { notes.value = notes.value + note }
}

/** Las casillas no llevan `fast_id` (cuelgan del paso copiado al ayuno): se resuelven por el plan del ayuno. */
class FakeRefeed(private val fasts: FakeFasts) : RefeedRepository {
    val checks = MutableStateFlow<List<RefeedCheck>>(emptyList())
    val meals = MutableStateFlow<List<Meal>>(emptyList())
    private fun stepIds(fastId: String) = fasts.items.value[fastId]?.plan?.refeed.orEmpty().map { it.id }.toSet()
    override fun observeChecks(fastId: String) = checks.map { l -> l.filter { it.refeedStepId in stepIds(fastId) } }
    override suspend fun checks(fastId: String) = checks.value.filter { it.refeedStepId in stepIds(fastId) }.sortedBy { it.scheduledAt }
    override suspend fun check(id: String) = checks.value.firstOrNull { it.id == id }
    override suspend fun insertChecks(checks: List<RefeedCheck>) { this.checks.value = this.checks.value + checks }
    override suspend fun updateCheck(check: RefeedCheck) { checks.value = checks.value.map { if (it.id == check.id) check else it } }
    override fun observeMeals(fastId: String) = meals.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun meals(fastId: String) = meals.value.filter { it.fastId == fastId }
    override suspend fun insertMeal(meal: Meal) { meals.value = meals.value + meal }
}

class FakeAlerts : AlertRepository {
    val items = MutableStateFlow<List<Alert>>(emptyList())
    override fun observeAlerts(fastId: String) = items.map { l -> l.filter { it.fastId == fastId } }
    override suspend fun alerts(fastId: String) = items.value.filter { it.fastId == fastId }
    override suspend fun get(id: String) = items.value.firstOrNull { it.id == id }
    override suspend fun insert(alert: Alert) { items.value = items.value + alert }
    override suspend fun update(alert: Alert) { items.value = items.value.map { if (it.id == alert.id) alert else it } }
}

class FakeAudit : AuditRepository {
    val entries = mutableListOf<EditLogEntry>()
    override suspend fun append(entry: EditLogEntry) { entries += entry }
    override suspend fun entriesFor(rowId: String) = entries.filter { it.rowId == rowId }
}

class FakeSettings : com.ayuno.domain.port.SettingsRepository {
    val value = MutableStateFlow(com.ayuno.domain.model.Settings())
    override fun observe() = value
    override suspend fun get() = value.value
    override suspend fun save(settings: com.ayuno.domain.model.Settings) { value.value = settings }
}

object DirectTransactions : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
}
