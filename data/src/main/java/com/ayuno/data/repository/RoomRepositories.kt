package com.ayuno.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.ayuno.data.db.AyunoDatabase
import com.ayuno.data.db.MetricOptionEntity
import com.ayuno.data.seed.CatalogSeed
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureRanges
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanOwner
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.port.AlertRepository
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.port.CatalogRepository
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.RecordRepository
import com.ayuno.domain.port.TemplateRepository
import com.ayuno.domain.time.LocalTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.model.SymptomType
import java.time.Clock

private fun Clock.stamp(): String = LocalTimes.format(LocalTimes.now(this))

class RoomCatalogRepository(
    private val database: AyunoDatabase,
    private val context: Context,
    private val clock: Clock,
) : CatalogRepository {
    private val dao get() = database.catalogDao()
    private val ranges by lazy { CatalogSeed.bloodPressureRanges(context) }

    override fun observeMetricTypes(): Flow<List<MetricType>> =
        combine(dao.observeMetricTypes(), dao.observeMetricOptions()) { types, options -> types.map { it.toDomain(options) } }
    override suspend fun metricTypes(): List<MetricType> { val options = dao.metricOptions(); return dao.metricTypes().map { it.toDomain(options) } }
    override suspend fun metricType(code: String): MetricType? = dao.metricType(code)?.toDomain(dao.optionsOf(code))

    /** Guarda el tipo y sus palabras (H-049); las palabras que ya no están se borran en blando. */
    override suspend fun saveMetricType(type: MetricType) = database.withTransaction {
        val existing = dao.metricType(type.code)
        val now = clock.stamp()
        dao.upsertMetricType(type.toEntity(existing?.createdAt ?: now, if (existing != null) now else null))
        val old = dao.optionsOf(type.code).associateBy { it.id }
        val changed = type.options.filter { o -> old[o.id]?.let { it.deletedAt != null || it.label != o.label || it.value != o.value || it.position != o.position } ?: true }
        dao.upsertMetricOptions(changed.map { o ->
            MetricOptionEntity(
                o.id, type.code, o.label, o.value, o.position,
                createdAt = old[o.id]?.createdAt ?: now, updatedAt = if (old.containsKey(o.id)) now else null, deletedAt = null,
            )
        })
        dao.softDeleteMetricOptions(type.code, type.options.map { it.id }, now)
    }

    override suspend fun bloodPressureRanges(): BloodPressureRanges = ranges

    private val dao2 get() = database.catalog2Dao()

    override fun observeSymptomTypes(): Flow<List<SymptomType>> = dao2.observeSymptomTypes().map { l -> l.map { it.toDomain() } }
    override suspend fun symptomTypes(): List<SymptomType> = dao2.symptomTypes().map { it.toDomain() }
    override suspend fun saveSymptomType(type: SymptomType) {
        val existing = dao2.symptomType(type.code); val now = clock.stamp()
        dao2.upsertSymptomType(type.toEntity(existing?.createdAt ?: now, if (existing != null) now else null))
    }

    override fun observeIntakeTypes(): Flow<List<IntakeType>> = dao2.observeIntakeTypes().map { l -> l.map { it.toDomain() } }
    override suspend fun intakeTypes(): List<IntakeType> = dao2.intakeTypes().map { it.toDomain() }
    override suspend fun saveIntakeType(type: IntakeType) {
        val existing = dao2.intakeType(type.code); val now = clock.stamp()
        dao2.upsertIntakeType(type.toEntity(existing?.createdAt ?: now, if (existing != null) now else null))
    }

    override fun observeProducts(): Flow<List<Product>> =
        combine(dao2.observeProducts(), dao2.observeProductSchedules()) { products, schedules -> products.map { it.toDomain(schedules) } }
    override suspend fun products(): List<Product> { val schedules = dao2.productSchedules(); return dao2.products().map { it.toDomain(schedules) } }
    override suspend fun product(code: String): Product? = dao2.product(code)?.toDomain(dao2.schedulesOf(code))

    override suspend fun saveProduct(product: Product) {
        val now = clock.stamp()
        val existing = dao2.product(product.code)
        dao2.upsertProduct(product.toEntity(existing?.createdAt ?: now, if (existing != null) now else null))
        val old = dao2.schedulesOf(product.code).associateBy { it.id }
        dao2.upsertProductSchedules(product.schedules.map { s ->
            com.ayuno.data.db.ProductScheduleEntity(
                s.id, product.code, s.timeOfDay.text(), s.amount,
                createdAt = old[s.id]?.createdAt ?: now, updatedAt = if (old.containsKey(s.id)) now else null, deletedAt = null,
            )
        })
        dao2.softDeleteProductSchedules(product.code, product.schedules.map { it.id }, now)
    }
}

/** Lectura y escritura del plan (etapas, mediciones con horarios, reglas) de una plantilla o un ayuno. */
internal class PlanStore(private val database: AyunoDatabase, private val clock: Clock) {
    private val dao get() = database.planDao()

    suspend fun load(owner: PlanOwner): Plan {
        val templateId = (owner as? PlanOwner.OfTemplate)?.id
        val fastId = (owner as? PlanOwner.OfFast)?.id
        val metrics = dao.planMetrics(templateId, fastId)
        val schedules = if (metrics.isEmpty()) emptyList() else dao.schedules(metrics.map { it.id })
        val dao2 = database.plan2Dao()
        return Plan(
            stages = dao.stages(templateId, fastId).map { it.toDomain() },
            metrics = metrics.map { it.toDomain(schedules) },
            rules = dao.rules(templateId, fastId).map { it.toDomain() },
            goals = dao2.goals(templateId, fastId).map { it.toDomain() },
            checklist = dao2.checklist(templateId, fastId).map { it.toDomain() },
            notices = dao2.notices(templateId, fastId).map { it.toDomain() },
            products = dao2.planProducts(templateId, fastId).map { it.toDomain() },
            refeed = database.refeedDao().steps(templateId, fastId).map { it.toDomain() },
        )
    }

    /** Escribe el plan; en plantillas, lo que ya no está se borra en blando. Conserva `created_at` de lo existente. */
    suspend fun save(owner: PlanOwner, plan: Plan) {
        val now = clock.stamp()
        val templateId = (owner as? PlanOwner.OfTemplate)?.id
        val fastId = (owner as? PlanOwner.OfFast)?.id
        val oldStages = dao.stages(templateId, fastId).associateBy { it.id }
        val oldMetrics = dao.planMetrics(templateId, fastId).associateBy { it.id }
        val oldSchedules = if (oldMetrics.isEmpty()) emptyMap() else dao.schedules(oldMetrics.keys.toList()).associateBy { it.id }
        val oldRules = dao.rules(templateId, fastId).associateBy { it.id }

        dao.upsertStages(plan.stages.map { s ->
            s.toEntity(owner, now).let { e -> oldStages[s.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e }
        })
        dao.upsertPlanMetrics(plan.metrics.map { m ->
            m.toEntity(owner, now).let { e -> oldMetrics[m.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e }
        })
        dao.upsertSchedules(plan.metrics.flatMap { m ->
            m.schedules.map { s ->
                s.toEntity(m.id, now).let { e -> oldSchedules[s.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e }
            }
        })
        dao.upsertRules(plan.rules.map { r ->
            r.toEntity(owner, now).let { e -> oldRules[r.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e }
        })
        val dao2 = database.plan2Dao()
        val oldGoals = dao2.goals(templateId, fastId).associateBy { it.id }
        val oldChecklist = dao2.checklist(templateId, fastId).associateBy { it.id }
        val oldNotices = dao2.notices(templateId, fastId).associateBy { it.id }
        val oldProducts = dao2.planProducts(templateId, fastId).associateBy { it.id }
        dao2.upsertGoals(plan.goals.map { g -> g.toEntity(owner, now).let { e -> oldGoals[g.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e } })
        dao2.upsertChecklist(plan.checklist.map { c -> c.toEntity(owner, now).let { e -> oldChecklist[c.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e } })
        dao2.upsertNotices(plan.notices.map { n -> n.toEntity(owner, now).let { e -> oldNotices[n.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e } })
        dao2.upsertPlanProducts(plan.products.map { p -> p.toEntity(owner, now).let { e -> oldProducts[p.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e } })
        val refeedDao = database.refeedDao()
        val oldSteps = refeedDao.steps(templateId, fastId).associateBy { it.id }
        refeedDao.upsertSteps(plan.refeed.map { r -> r.toEntity(owner, now).let { e -> oldSteps[r.id]?.let { e.copy(createdAt = it.createdAt, updatedAt = now) } ?: e } })
        if (templateId != null) {
            dao2.softDeleteGoals(templateId, plan.goals.map { it.id }, now)
            dao2.softDeleteChecklist(templateId, plan.checklist.map { it.id }, now)
            dao2.softDeleteNotices(templateId, plan.notices.map { it.id }, now)
            dao2.softDeletePlanProducts(templateId, plan.products.map { it.id }, now)
            refeedDao.softDeleteSteps(templateId, plan.refeed.map { it.id }, now)
            dao.softDeleteStages(templateId, plan.stages.map { it.id }, now)
            dao.softDeletePlanMetrics(templateId, plan.metrics.map { it.id }, now)
            dao.softDeleteSchedules(templateId, plan.metrics.flatMap { m -> m.schedules.map { it.id } }, now)
            dao.softDeleteRules(templateId, plan.rules.map { it.id }, now)
        }
    }
}

class RoomTemplateRepository(private val database: AyunoDatabase, private val clock: Clock) : TemplateRepository {
    private val dao get() = database.planDao()
    private val plans = PlanStore(database, clock)

    override fun observeTemplates(): Flow<List<Template>> = dao.observeTemplates().map { l -> l.map { it.toDomain() } }
    override fun observeDeleted(): Flow<List<Template>> = dao.observeDeletedTemplates().map { l -> l.map { it.toDomain() } }

    override suspend fun setDeleted(id: String, deleted: Boolean) {
        val now = clock.stamp()
        dao.setTemplateDeleted(id, now.takeIf { deleted }, now)
    }

    override suspend fun get(id: String): TemplateWithPlan? {
        val template = dao.template(id) ?: return null
        return TemplateWithPlan(template.toDomain(), plans.load(PlanOwner.OfTemplate(id)))
    }

    override suspend fun save(template: TemplateWithPlan) {
        val t = template.template
        val now = clock.stamp()
        val existing = dao.template(t.id)
        dao.upsertTemplate(
            com.ayuno.data.db.TemplateEntity(
                t.id, t.name, t.targetHours, t.milestoneHours.toJson(),
                createdAt = existing?.createdAt ?: now, updatedAt = if (existing != null) now else null, deletedAt = null,
            ),
        )
        plans.save(PlanOwner.OfTemplate(t.id), template.plan)
    }
}

class RoomFastRepository(private val database: AyunoDatabase, private val clock: Clock) : FastRepository {
    private val dao get() = database.fastDao()
    private val plans = PlanStore(database, clock)

    override fun observeFasts(): Flow<List<Fast>> = dao.observeFasts().map { l -> l.map { it.toDomain() } }
    override suspend fun all(): List<Fast> = dao.fasts().map { it.toDomain() }

    override suspend fun get(id: String): FastWithPlan? {
        val fast = dao.fast(id) ?: return null
        val replaced = database.fastLifecycleDao().replacedRules(id).map { it.toDomain() }
        return FastWithPlan(fast.toDomain(), plans.load(PlanOwner.OfFast(id)), replaced)
    }

    override suspend fun insert(fast: FastWithPlan) {
        dao.insert(fast.fast.toEntity(clock.stamp(), null))
        plans.save(PlanOwner.OfFast(fast.fast.id), fast.plan)
    }

    override suspend fun update(fast: Fast) {
        val existing = dao.fast(fast.id) ?: error("Ayuno inexistente")
        dao.update(fast.toEntity(existing.createdAt, clock.stamp()))
    }

    /** Solo añade o modifica (lista previa marcada, regla manual creada al vuelo); no borra partes del plan copiado. */
    override suspend fun updatePlan(fastId: String, plan: Plan) = plans.save(PlanOwner.OfFast(fastId), plan)

    override suspend fun replacePlan(fastId: String, plan: Plan) {
        val now = clock.stamp()
        with(database.fastLifecycleDao()) {
            retireSchedules(fastId, now)
            retireStages(fastId, now)
            retirePlanMetrics(fastId, now)
            retireRules(fastId, now)
            retireGoals(fastId, now)
            retireChecklist(fastId, now)
            retireNotices(fastId, now)
            retirePlanProducts(fastId, now)
            retireRefeedSteps(fastId, now)
        }
        plans.save(PlanOwner.OfFast(fastId), plan)
    }

    /** De las hojas a la raíz: la bitácora y las filas que dependen de otras se borran antes que aquello de lo que cuelgan. */
    override suspend fun deleteForever(fastId: String) = with(database.fastLifecycleDao()) {
        deleteEditLog(fastId)
        deleteSchedules(fastId)
        deleteRefeedChecks(fastId)
        deleteMeasurements(fastId)
        deleteBloodPressure(fastId)
        deleteSymptoms(fastId)
        deleteIntakes(fastId)
        deleteProductLogs(fastId)
        deleteAlerts(fastId)
        deleteNotes(fastId)
        deleteMeals(fastId)
        deleteStages(fastId)
        deletePlanMetrics(fastId)
        deleteRules(fastId)
        deleteGoals(fastId)
        deleteChecklist(fastId)
        deleteNotices(fastId)
        deletePlanProducts(fastId)
        deleteRefeedSteps(fastId)
        deleteFast(fastId)
    }
}

class RoomRecordRepository(private val database: AyunoDatabase, private val clock: Clock) : RecordRepository {
    private val dao get() = database.recordDao()

    override fun observeMeasurements(fastId: String): Flow<List<Measurement>> = dao.observeMeasurements(fastId).map { l -> l.map { it.toDomain() } }
    override fun observeBloodPressure(fastId: String): Flow<List<BloodPressureReading>> = dao.observeBloodPressure(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun measurements(fastId: String): List<Measurement> = dao.measurements(fastId).map { it.toDomain() }
    override suspend fun bloodPressure(fastId: String): List<BloodPressureReading> = dao.bloodPressure(fastId).map { it.toDomain() }
    override suspend fun insertMeasurement(measurement: Measurement) = dao.insertMeasurement(measurement.toEntity(clock.stamp()))
    override suspend fun insertBloodPressure(reading: BloodPressureReading) = dao.insertBloodPressure(reading.toEntity(clock.stamp()))

    override suspend fun measurement(id: String, includeDeleted: Boolean) =
        dao.measurement(id)?.takeIf { includeDeleted || it.deletedAt == null }?.toDomain()
    override suspend fun bloodPressureReading(id: String, includeDeleted: Boolean) =
        dao.bloodPressureReading(id)?.takeIf { includeDeleted || it.deletedAt == null }?.toDomain()

    override suspend fun updateMeasurement(measurement: Measurement) {
        val existing = dao.measurement(measurement.id) ?: error("Medición inexistente")
        dao.updateMeasurement(measurement.toEntity(existing.createdAt).copy(updatedAt = clock.stamp(), deletedAt = existing.deletedAt))
    }

    override suspend fun updateBloodPressure(reading: BloodPressureReading) {
        val existing = dao.bloodPressureReading(reading.id) ?: error("Presión inexistente")
        dao.updateBloodPressure(reading.toEntity(existing.createdAt).copy(updatedAt = clock.stamp(), deletedAt = existing.deletedAt))
    }

    override suspend fun setMeasurementDeleted(id: String, deleted: Boolean) {
        val now = clock.stamp()
        dao.setMeasurementDeleted(id, now.takeIf { deleted }, now)
    }

    override suspend fun setBloodPressureDeleted(id: String, deleted: Boolean) {
        val now = clock.stamp()
        dao.setBloodPressureDeleted(id, now.takeIf { deleted }, now)
    }

    override fun observeDeletedMeasurements(fastId: String): Flow<List<Measurement>> = dao.observeDeletedMeasurements(fastId).map { l -> l.map { it.toDomain() } }
    override fun observeDeletedBloodPressure(fastId: String): Flow<List<BloodPressureReading>> = dao.observeDeletedBloodPressure(fastId).map { l -> l.map { it.toDomain() } }

    private val dao2 get() = database.record2Dao()
    override fun observeSymptoms(fastId: String): Flow<List<Symptom>> = dao2.observeSymptoms(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun symptoms(fastId: String): List<Symptom> = dao2.symptoms(fastId).map { it.toDomain() }
    override suspend fun insertSymptom(symptom: Symptom) = dao2.insertSymptom(symptom.toEntity(clock.stamp()))
    override fun observeIntakes(fastId: String): Flow<List<Intake>> = dao2.observeIntakes(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun intakes(fastId: String): List<Intake> = dao2.intakes(fastId).map { it.toDomain() }
    override suspend fun insertIntake(intake: Intake) = dao2.insertIntake(intake.toEntity(clock.stamp()))
    override fun observeProductLogs(fastId: String): Flow<List<ProductLog>> = dao2.observeProductLogs(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun productLogs(fastId: String): List<ProductLog> = dao2.productLogs(fastId).map { it.toDomain() }
    override suspend fun insertProductLog(log: ProductLog) = dao2.insertProductLog(log.toEntity(clock.stamp()))
    override fun observeNotes(fastId: String): Flow<List<Note>> = dao2.observeNotes(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun notes(fastId: String): List<Note> = dao2.notes(fastId).map { it.toDomain() }
    override suspend fun insertNote(note: Note) = dao2.insertNote(note.toEntity(clock.stamp()))
}

class RoomRefeedRepository(private val database: AyunoDatabase, private val clock: Clock) : com.ayuno.domain.port.RefeedRepository {
    private val dao get() = database.refeedDao()

    override fun observeChecks(fastId: String): Flow<List<com.ayuno.domain.model.RefeedCheck>> = dao.observeChecks(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun checks(fastId: String) = dao.checks(fastId).map { it.toDomain() }
    override suspend fun check(id: String) = dao.check(id)?.toDomain()
    override suspend fun insertChecks(checks: List<com.ayuno.domain.model.RefeedCheck>) {
        val now = clock.stamp()
        dao.insertChecks(checks.map { it.toEntity(now, null) })
    }

    override suspend fun updateCheck(check: com.ayuno.domain.model.RefeedCheck) {
        val existing = dao.check(check.id) ?: error("Casilla inexistente")
        dao.updateCheck(check.toEntity(existing.createdAt, clock.stamp()))
    }

    override fun observeMeals(fastId: String): Flow<List<com.ayuno.domain.model.Meal>> = dao.observeMeals(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun meals(fastId: String) = dao.meals(fastId).map { it.toDomain() }
    override suspend fun insertMeal(meal: com.ayuno.domain.model.Meal) = dao.insertMeal(meal.toEntity(clock.stamp()))
}

class RoomAlertRepository(private val database: AyunoDatabase, private val clock: Clock) : AlertRepository {
    private val dao get() = database.alertDao()

    override fun observeAlerts(fastId: String): Flow<List<Alert>> = dao.observeAlerts(fastId).map { l -> l.map { it.toDomain() } }
    override suspend fun alerts(fastId: String): List<Alert> = dao.alerts(fastId).map { it.toDomain() }
    override suspend fun get(id: String): Alert? = dao.alert(id)?.toDomain()
    override suspend fun insert(alert: Alert) = dao.insert(alert.toEntity(clock.stamp(), null))

    override suspend fun update(alert: Alert) {
        val existing = dao.alert(alert.id) ?: error("Alerta inexistente")
        dao.update(alert.toEntity(existing.createdAt, clock.stamp()))
    }
}

class RoomAuditRepository(private val database: AyunoDatabase, private val clock: Clock) : AuditRepository {
    override suspend fun append(entry: EditLogEntry) = database.auditDao().insert(entry.toEntity(clock.stamp()))
    override suspend fun entriesFor(rowId: String): List<EditLogEntry> = database.auditDao().entriesFor(rowId).map { it.toDomain() }
    override fun observeEntriesFor(rowId: String): Flow<List<EditLogEntry>> = database.auditDao().observeEntriesFor(rowId).map { l -> l.map { it.toDomain() } }
    override fun observeEditedRowIds(entityNames: List<String>): Flow<Set<String>> = database.auditDao().observeEditedRowIds(entityNames).map { it.toSet() }
}

/** Datos personales (registro único, H-038). Sin fila todavía = todo vacío. */
class RoomPersonProfileRepository(private val database: AyunoDatabase, private val clock: Clock) : com.ayuno.domain.port.PersonProfileRepository {
    private val dao get() = database.personProfileDao()

    private fun com.ayuno.data.db.PersonProfileEntity?.toDomain() = com.ayuno.domain.model.PersonProfile(
        fullName = this?.fullName, idNumber = this?.idNumber, birthDate = this?.birthDate?.let { java.time.LocalDate.parse(it) },
    )

    override fun observe(): Flow<com.ayuno.domain.model.PersonProfile> = dao.observe().map { it.toDomain() }
    override suspend fun get(): com.ayuno.domain.model.PersonProfile = dao.get().toDomain()

    override suspend fun save(profile: com.ayuno.domain.model.PersonProfile) {
        val existing = dao.get()
        val now = clock.stamp()
        dao.upsert(
            com.ayuno.data.db.PersonProfileEntity(
                fullName = profile.fullName, idNumber = profile.idNumber, birthDate = profile.birthDate?.toString(),
                createdAt = existing?.createdAt ?: now, updatedAt = if (existing != null) now else null, deletedAt = null,
            ),
        )
    }
}

/** Ajustes (registro único, H-017). Sin fila todavía = ajustes por defecto. */
class RoomSettingsRepository(private val database: AyunoDatabase) : com.ayuno.domain.port.SettingsRepository {
    private val dao get() = database.settingsDao()

    private fun com.ayuno.data.db.SettingsEntity?.toDomain() = com.ayuno.domain.model.Settings(
        quietStart = this?.quietStart?.localTime(),
        quietEnd = this?.quietEnd?.localTime(),
        exportPersonEnabled = this?.exportPersonEnabled ?: false,
        appLockEnabled = this?.appLockEnabled ?: false,
    )

    override fun observe(): Flow<com.ayuno.domain.model.Settings> = dao.observe().map { it.toDomain() }
    override suspend fun get(): com.ayuno.domain.model.Settings = dao.get().toDomain()

    override suspend fun save(settings: com.ayuno.domain.model.Settings) = dao.upsert(
        com.ayuno.data.db.SettingsEntity(
            quietStart = settings.quietStart?.text(),
            quietEnd = settings.quietEnd?.text(),
            exportPersonEnabled = settings.exportPersonEnabled,
            appLockEnabled = settings.appLockEnabled,
        ),
    )
}
