package com.ayuno.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM metric_type WHERE deleted_at IS NULL ORDER BY rowid")
    fun observeMetricTypes(): Flow<List<MetricTypeEntity>>

    @Query("SELECT * FROM metric_type WHERE deleted_at IS NULL ORDER BY rowid")
    suspend fun metricTypes(): List<MetricTypeEntity>

    @Query("SELECT * FROM metric_type WHERE code = :code AND deleted_at IS NULL")
    suspend fun metricType(code: String): MetricTypeEntity?

    @Upsert
    suspend fun upsertMetricType(entity: MetricTypeEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM template WHERE deleted_at IS NULL ORDER BY name")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template WHERE id = :id AND deleted_at IS NULL")
    suspend fun template(id: String): TemplateEntity?

    @Upsert suspend fun upsertTemplate(entity: TemplateEntity)
    @Upsert suspend fun upsertStages(entities: List<StageEntity>)
    @Upsert suspend fun upsertPlanMetrics(entities: List<PlanMetricEntity>)
    @Upsert suspend fun upsertSchedules(entities: List<MetricScheduleEntity>)
    @Upsert suspend fun upsertRules(entities: List<RuleEntity>)

    @Query("SELECT * FROM stage WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL")
    suspend fun stages(templateId: String?, fastId: String?): List<StageEntity>

    @Query("SELECT * FROM plan_metric WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun planMetrics(templateId: String?, fastId: String?): List<PlanMetricEntity>

    @Query("SELECT * FROM metric_schedule WHERE plan_metric_id IN (:planMetricIds) AND deleted_at IS NULL ORDER BY time_of_day")
    suspend fun schedules(planMetricIds: List<String>): List<MetricScheduleEntity>

    @Query("SELECT * FROM rule WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun rules(templateId: String?, fastId: String?): List<RuleEntity>

    @Query("UPDATE stage SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteStages(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE plan_metric SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeletePlanMetrics(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE metric_schedule SET deleted_at = :now WHERE plan_metric_id IN (SELECT id FROM plan_metric WHERE template_id = :templateId) AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteSchedules(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE rule SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteRules(templateId: String, keep: List<String>, now: String)
}

@Dao
interface FastDao {
    @Query("SELECT * FROM fast WHERE deleted_at IS NULL ORDER BY clock_start_planned DESC")
    fun observeFasts(): Flow<List<FastEntity>>

    @Query("SELECT * FROM fast WHERE deleted_at IS NULL ORDER BY clock_start_planned DESC")
    suspend fun fasts(): List<FastEntity>

    @Query("SELECT * FROM fast WHERE id = :id AND deleted_at IS NULL")
    suspend fun fast(id: String): FastEntity?

    @Insert suspend fun insert(entity: FastEntity)
    @Update suspend fun update(entity: FastEntity)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM measurement WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    fun observeMeasurements(fastId: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurement WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    suspend fun measurements(fastId: String): List<MeasurementEntity>

    @Query("SELECT * FROM blood_pressure_reading WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    fun observeBloodPressure(fastId: String): Flow<List<BloodPressureReadingEntity>>

    @Query("SELECT * FROM blood_pressure_reading WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    suspend fun bloodPressure(fastId: String): List<BloodPressureReadingEntity>

    @Insert suspend fun insertMeasurement(entity: MeasurementEntity)
    @Insert suspend fun insertBloodPressure(entity: BloodPressureReadingEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alert WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY fired_at")
    fun observeAlerts(fastId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alert WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY fired_at")
    suspend fun alerts(fastId: String): List<AlertEntity>

    @Query("SELECT * FROM alert WHERE id = :id AND deleted_at IS NULL")
    suspend fun alert(id: String): AlertEntity?

    @Insert suspend fun insert(entity: AlertEntity)
    @Update suspend fun update(entity: AlertEntity)
}

@Dao
interface AuditDao {
    @Insert suspend fun insert(entity: EditLogEntity)

    @Query("SELECT * FROM edit_log WHERE row_id = :rowId ORDER BY edited_at")
    suspend fun entriesFor(rowId: String): List<EditLogEntity>
}
