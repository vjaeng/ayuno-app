package com.ayuno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tanda 2: síntomas, ingestas, productos, notas y el resto del plan (nombres canónicos de MODELO-DATOS).

@Entity(tableName = "symptom_type")
data class SymptomTypeEntity(
    @PrimaryKey val code: String,
    val name: String,
    @ColumnInfo(name = "group_name") val groupName: String?,
    @ColumnInfo(name = "is_alarm") val isAlarm: Boolean,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "symptom", indices = [Index("fast_id")])
data class SymptomEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "symptom_type_code") val symptomTypeCode: String,
    @ColumnInfo(name = "noted_at") val notedAt: String,
    val severity: Int,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "intake_type")
data class IntakeTypeEntity(
    @PrimaryKey val code: String,
    val name: String,
    val unit: String,
    @ColumnInfo(name = "breaks_fast") val breaksFast: Boolean,
    @ColumnInfo(name = "sodium_factor") val sodiumFactor: Double?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "intake", indices = [Index("fast_id")])
data class IntakeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "intake_type_code") val intakeTypeCode: String,
    @ColumnInfo(name = "taken_at") val takenAt: String,
    val amount: Double,
    val unit: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "intake_goal", indices = [Index("template_id"), Index("fast_id")])
data class IntakeGoalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    @ColumnInfo(name = "intake_type_code") val intakeTypeCode: String,
    @ColumnInfo(name = "daily_amount") val dailyAmount: Double,
    val unit: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "checklist_item", indices = [Index("template_id"), Index("fast_id")])
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    val text: String,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "stage_notice", indices = [Index("template_id"), Index("fast_id")])
data class StageNoticeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    val text: String,
    @ColumnInfo(name = "stage_kinds") val stageKinds: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey val code: String,
    val name: String,
    val kind: String,
    @ColumnInfo(name = "default_amount") val defaultAmount: Double,
    val unit: String,
    val avoid: Boolean,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "product_schedule", indices = [Index("product_code")])
data class ProductScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_code") val productCode: String,
    @ColumnInfo(name = "time_of_day") val timeOfDay: String,
    val amount: Double,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "plan_product", indices = [Index("template_id"), Index("fast_id")])
data class PlanProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    @ColumnInfo(name = "product_code") val productCode: String,
    @ColumnInfo(name = "during_fast") val duringFast: String,
    @ColumnInfo(name = "optional_enabled") val optionalEnabled: Boolean,
    val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "product_log", indices = [Index("fast_id")])
data class ProductLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "product_code") val productCode: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: String?,
    val status: String,
    @ColumnInfo(name = "taken_at") val takenAt: String?,
    @ColumnInfo(name = "planned_amount") val plannedAmount: Double?,
    val amount: Double?,
    val unit: String?,
    val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "note", indices = [Index("fast_id")])
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "noted_at") val notedAt: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)
