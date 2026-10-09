package com.ayuno.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface Catalog2Dao {
    @Query("SELECT * FROM symptom_type WHERE deleted_at IS NULL ORDER BY is_alarm DESC, group_name, name")
    fun observeSymptomTypes(): Flow<List<SymptomTypeEntity>>

    @Query("SELECT * FROM symptom_type WHERE deleted_at IS NULL ORDER BY is_alarm DESC, group_name, name")
    suspend fun symptomTypes(): List<SymptomTypeEntity>

    @Query("SELECT * FROM symptom_type WHERE code = :code")
    suspend fun symptomType(code: String): SymptomTypeEntity?

    @Upsert suspend fun upsertSymptomType(entity: SymptomTypeEntity)

    @Query("SELECT * FROM intake_type WHERE deleted_at IS NULL ORDER BY rowid")
    fun observeIntakeTypes(): Flow<List<IntakeTypeEntity>>

    @Query("SELECT * FROM intake_type WHERE deleted_at IS NULL ORDER BY rowid")
    suspend fun intakeTypes(): List<IntakeTypeEntity>

    @Query("SELECT * FROM intake_type WHERE code = :code")
    suspend fun intakeType(code: String): IntakeTypeEntity?

    @Upsert suspend fun upsertIntakeType(entity: IntakeTypeEntity)

    @Query("SELECT * FROM product WHERE deleted_at IS NULL ORDER BY name")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE deleted_at IS NULL ORDER BY name")
    suspend fun products(): List<ProductEntity>

    @Query("SELECT * FROM product WHERE code = :code AND deleted_at IS NULL")
    suspend fun product(code: String): ProductEntity?

    @Upsert suspend fun upsertProduct(entity: ProductEntity)

    @Query("SELECT * FROM product_schedule WHERE deleted_at IS NULL ORDER BY time_of_day")
    fun observeProductSchedules(): Flow<List<ProductScheduleEntity>>

    @Query("SELECT * FROM product_schedule WHERE deleted_at IS NULL ORDER BY time_of_day")
    suspend fun productSchedules(): List<ProductScheduleEntity>

    @Query("SELECT * FROM product_schedule WHERE product_code = :code")
    suspend fun schedulesOf(code: String): List<ProductScheduleEntity>

    @Upsert suspend fun upsertProductSchedules(entities: List<ProductScheduleEntity>)

    @Query("UPDATE product_schedule SET deleted_at = :now WHERE product_code = :code AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteProductSchedules(code: String, keep: List<String>, now: String)
}

@Dao
interface Plan2Dao {
    @Upsert suspend fun upsertGoals(entities: List<IntakeGoalEntity>)
    @Upsert suspend fun upsertChecklist(entities: List<ChecklistItemEntity>)
    @Upsert suspend fun upsertNotices(entities: List<StageNoticeEntity>)
    @Upsert suspend fun upsertPlanProducts(entities: List<PlanProductEntity>)

    @Query("SELECT * FROM intake_goal WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun goals(templateId: String?, fastId: String?): List<IntakeGoalEntity>

    @Query("SELECT * FROM checklist_item WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun checklist(templateId: String?, fastId: String?): List<ChecklistItemEntity>

    @Query("SELECT * FROM stage_notice WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun notices(templateId: String?, fastId: String?): List<StageNoticeEntity>

    @Query("SELECT * FROM plan_product WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun planProducts(templateId: String?, fastId: String?): List<PlanProductEntity>

    @Query("UPDATE intake_goal SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteGoals(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE checklist_item SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteChecklist(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE stage_notice SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteNotices(templateId: String, keep: List<String>, now: String)

    @Query("UPDATE plan_product SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeletePlanProducts(templateId: String, keep: List<String>, now: String)
}

@Dao
interface Record2Dao {
    @Query("SELECT * FROM symptom WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY noted_at")
    fun observeSymptoms(fastId: String): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptom WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY noted_at")
    suspend fun symptoms(fastId: String): List<SymptomEntity>

    @Insert suspend fun insertSymptom(entity: SymptomEntity)

    @Query("SELECT * FROM intake WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    fun observeIntakes(fastId: String): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intake WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY taken_at")
    suspend fun intakes(fastId: String): List<IntakeEntity>

    @Insert suspend fun insertIntake(entity: IntakeEntity)

    @Query("SELECT * FROM product_log WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY created_at")
    fun observeProductLogs(fastId: String): Flow<List<ProductLogEntity>>

    @Query("SELECT * FROM product_log WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY created_at")
    suspend fun productLogs(fastId: String): List<ProductLogEntity>

    @Insert suspend fun insertProductLog(entity: ProductLogEntity)

    @Query("SELECT * FROM note WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY noted_at")
    fun observeNotes(fastId: String): Flow<List<NoteEntity>>

    @Insert suspend fun insertNote(entity: NoteEntity)
}
