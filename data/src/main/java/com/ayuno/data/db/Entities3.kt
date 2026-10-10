package com.ayuno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tanda 3: vuelta a comer (nombres canónicos de MODELO-DATOS).

@Entity(tableName = "refeed_step", indices = [Index("template_id"), Index("fast_id")])
data class RefeedStepEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "fast_id") val fastId: String?,
    val kind: String,
    val description: String,
    @ColumnInfo(name = "offset_minutes") val offsetMinutes: Int?,
    @ColumnInfo(name = "repeat_every_minutes") val repeatEveryMinutes: Int?,
    @ColumnInfo(name = "repeat_for_minutes") val repeatForMinutes: Int?,
    @ColumnInfo(name = "day_offset") val dayOffset: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

/** Sin `fast_id` (MODELO §3): cuelga del paso copiado al ayuno. */
@Entity(tableName = "refeed_check", indices = [Index("refeed_step_id")])
data class RefeedCheckEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "refeed_step_id") val refeedStepId: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: String,
    @ColumnInfo(name = "done_at") val doneAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

@Entity(tableName = "meal", indices = [Index("fast_id")])
data class MealEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fast_id") val fastId: String,
    @ColumnInfo(name = "eaten_at") val eatenAt: String,
    val description: String,
    @ColumnInfo(name = "approx_amount") val approxAmount: String?,
    val tolerance: String,
    @ColumnInfo(name = "tolerance_detail") val toleranceDetail: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)

/** Registro único (H-038), como `settings`: datos de identidad que solo viajan si la persona lo activa. */
@Entity(tableName = "person_profile")
data class PersonProfileEntity(
    @PrimaryKey val id: Int = SettingsEntity.SINGLETON_ID,
    @ColumnInfo(name = "full_name") val fullName: String?,
    @ColumnInfo(name = "id_number") val idNumber: String?,
    @ColumnInfo(name = "birth_date") val birthDate: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
)
