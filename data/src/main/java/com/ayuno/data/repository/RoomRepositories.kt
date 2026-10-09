package com.ayuno.data.repository

import android.content.Context
import com.ayuno.data.db.AyunoDatabase
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
import kotlinx.coroutines.flow.map
import java.time.Clock

private fun Clock.stamp(): String = LocalTimes.format(LocalTimes.now(this))

class RoomCatalogRepository(
    private val database: AyunoDatabase,
    private val context: Context,
    private val clock: Clock,
) : CatalogRepository {
    private val dao get() = database.catalogDao()
    private val ranges by lazy { CatalogSeed.bloodPressureRanges(context) }

    override fun observeMetricTypes(): Flow<List<MetricType>> = dao.observeMetricTypes().map { l -> l.map { it.toDomain() } }
    override suspend fun metricTypes(): List<MetricType> = dao.metricTypes().map { it.toDomain() }
    override suspend fun metricType(code: String): MetricType? = dao.metricType(code)?.toDomain()

    override suspend fun saveMetricType(type: MetricType) {
        val existing = dao.metricType(type.code)
        val now = clock.stamp()
        dao.upsertMetricType(type.toEntity(existing?.createdAt ?: now, if (existing != null) now else null))
    }

    override suspend fun bloodPressureRanges(): BloodPressureRanges = ranges
}

/** Lectura y escritura del plan (etapas, mediciones con horarios, reglas) de una plantilla o un ayuno. */
internal class PlanStore(private val database: AyunoDatabase, private val clock: Clock) {
    private val dao get() = database.planDao()

    suspend fun load(owner: PlanOwner): Plan {
        val templateId = (owner as? PlanOwner.OfTemplate)?.id
        val fastId = (owner as? PlanOwner.OfFast)?.id
        val metrics = dao.planMetrics(templateId, fastId)
        val schedules = if (metrics.isEmpty()) emptyList() else dao.schedules(metrics.map { it.id })
        return Plan(
            stages = dao.stages(templateId, fastId).map { it.toDomain() },
            metrics = metrics.map { it.toDomain(schedules) },
            rules = dao.rules(templateId, fastId).map { it.toDomain() },
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
        if (templateId != null) {
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
        return FastWithPlan(fast.toDomain(), plans.load(PlanOwner.OfFast(id)))
    }

    override suspend fun insert(fast: FastWithPlan) {
        dao.insert(fast.fast.toEntity(clock.stamp(), null))
        plans.save(PlanOwner.OfFast(fast.fast.id), fast.plan)
    }

    override suspend fun update(fast: Fast) {
        val existing = dao.fast(fast.id) ?: error("Ayuno inexistente")
        dao.update(fast.toEntity(existing.createdAt, clock.stamp()))
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
}
