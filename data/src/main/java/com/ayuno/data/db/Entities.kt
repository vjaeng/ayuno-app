package com.ayuno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tablas y columnas con los nombres canónicos de specs/MODELO-DATOS.md (ADR-003).
// Control: id, created_at, updated_at, deleted_at; propietario de plan template_id XOR fast_id (triggers en AyunoDatabase).

@Entity(tableName = "metric_type")
data class MetricTypeEntity(
    @PrimaryKey val code: String,
    val name: String,
    val unit: String,
    @ColumnInfo(name = "alt_unit") val altUnit: String?,
    @ColumnInfo(name = "entry_unit") val entryUnit: String,
    @ColumnInfo(name = "is_numeric") val isNumeric: Boolean,
    @ColumnInfo(name = "min_valid") val minValid: Double?,
    @ColumnInfo(name = "max_valid") val maxValid: Double?,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "template")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "target_hours") val targetHours: Double,
    @ColumnInfo(name = "milestone_hours") val milestoneHours: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "stage", indices = [Index("template_id"), Index("fast_id")])
data class StageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    val kind: String,
    @ColumnInfo(name = "duration_value") val durationValue: Double?,
    @ColumnInfo(name = "duration_unit") val durationUnit: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "plan_metric", indices = [Index("template_id"), Index("fast_id")])
data class PlanMetricEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    @ColumnInfo(name = "metric_type_code") val metricTypeCode: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "metric_schedule", indices = [Index("plan_metric_id")])
data class MetricScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plan_metric_id") val planMetricId: String,
    @ColumnInfo(name = "stage_kind") val stageKind: String,
    @ColumnInfo(name = "time_of_day") val timeOfDay: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "rule", indices = [Index("template_id"), Index("fast_id")])
data class RuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    val name: String,
    val active: Boolean,
    @ColumnInfo(name = "condition_kind") val conditionKind: String,
    @ColumnInfo(name = "metric_type_code") val metricTypeCode: String?,
    @ColumnInfo(name = "bp_component") val bpComponent: String?,
    val threshold: Double?,
    @ColumnInfo(name = "sustained_minutes") val sustainedMinutes: Int?,
    @ColumnInfo(name = "symptom_type_code") val symptomTypeCode: String?,
    @ColumnInfo(name = "intake_type_code") val intakeTypeCode: String?,
    @ColumnInfo(name = "check_time") val checkTime: String?,
    @ColumnInfo(name = "filter_stage_kinds") val filterStageKinds: String,
    @ColumnInfo(name = "filter_on_waking") val filterOnWaking: Boolean,
    @ColumnInfo(name = "filter_position") val filterPosition: String?,
    val level: String,
    val message: String,
    val action: String,
    @ColumnInfo(name = "action_minutes") val actionMinutes: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "fast", indices = [Index("status")])
data class FastEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    val status: String,
    @ColumnInfo(name = "target_hours") val targetHours: Double,
    @ColumnInfo(name = "milestone_hours") val milestoneHours: String,
    @ColumnInfo(name = "last_meal_planned") val lastMealPlanned: String,
    @ColumnInfo(name = "last_meal_actual") val lastMealActual: String?,
    @ColumnInfo(name = "clock_start_planned") val clockStartPlanned: String,
    @ColumnInfo(name = "clock_start_actual") val clockStartActual: String?,
    @ColumnInfo(name = "end_actual") val endActual: String?,
    @ColumnInfo(name = "end_reason") val endReason: String?,
    @ColumnInfo(name = "end_reason_detail") val endReasonDetail: String?,
    @ColumnInfo(name = "end_rule_id") val endRuleId: String?,
    @ColumnInfo(name = "tz_offset") val tzOffset: String,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "measurement", indices = [Index("fast_id")])
data class MeasurementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "metric_type_code") val metricTypeCode: String,
    @ColumnInfo(name = "taken_at") val takenAt: String,
    @ColumnInfo(name = "scheduled_for") val scheduledFor: String?,
    @ColumnInfo(name = "value_num") val valueNum: Double?,
    @ColumnInfo(name = "value_text") val valueText: String?,
    @ColumnInfo(name = "value_entered") val valueEntered: Double?,
    @ColumnInfo(name = "unit_entered") val unitEntered: String?,
    val source: String?,
    val trend: String?,
    @ColumnInfo(name = "on_waking") val onWaking: Boolean,
    @ColumnInfo(name = "not_done_reason") val notDoneReason: String?,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "blood_pressure_reading", indices = [Index("fast_id")])
data class BloodPressureReadingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "taken_at") val takenAt: String,
    @ColumnInfo(name = "scheduled_for") val scheduledFor: String?,
    val systolic: Int?,
    val diastolic: Int?,
    val pulse: Int?,
    val position: String,
    @ColumnInfo(name = "on_waking") val onWaking: Boolean,
    @ColumnInfo(name = "paired_reading_id") val pairedReadingId: String?,
    @ColumnInfo(name = "not_done_reason") val notDoneReason: String?,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "alert", indices = [Index("fast_id")])
data class AlertEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "fired_at") val firedAt: String,
    @ColumnInfo(name = "trigger_entry_ids") val triggerEntryIds: String,
    @ColumnInfo(name = "user_choice") val userChoice: String?,
    @ColumnInfo(name = "choice_reason") val choiceReason: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "edit_log", indices = [Index("row_id")])
data class EditLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entity_name") val entityName: String,
    @ColumnInfo(name = "row_id") val rowId: String,
    val action: String,
    @ColumnInfo(name = "edited_at") val editedAt: String,
    val field: String?,
    @ColumnInfo(name = "old_value") val oldValue: String?,
    @ColumnInfo(name = "new_value") val newValue: String?,
    val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
)
