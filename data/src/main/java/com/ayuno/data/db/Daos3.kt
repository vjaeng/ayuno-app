package com.ayuno.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RefeedDao {
    @Upsert suspend fun upsertSteps(entities: List<RefeedStepEntity>)

    @Query("SELECT * FROM refeed_step WHERE (template_id = :templateId OR fast_id = :fastId) AND deleted_at IS NULL ORDER BY rowid")
    suspend fun steps(templateId: String?, fastId: String?): List<RefeedStepEntity>

    @Query("UPDATE refeed_step SET deleted_at = :now WHERE template_id = :templateId AND deleted_at IS NULL AND id NOT IN (:keep)")
    suspend fun softDeleteSteps(templateId: String, keep: List<String>, now: String)

    @Query(
        "SELECT c.* FROM refeed_check c JOIN refeed_step s ON s.id = c.refeed_step_id " +
            "WHERE s.fast_id = :fastId AND c.deleted_at IS NULL ORDER BY c.scheduled_at, s.rowid",
    )
    fun observeChecks(fastId: String): Flow<List<RefeedCheckEntity>>

    @Query(
        "SELECT c.* FROM refeed_check c JOIN refeed_step s ON s.id = c.refeed_step_id " +
            "WHERE s.fast_id = :fastId AND c.deleted_at IS NULL ORDER BY c.scheduled_at, s.rowid",
    )
    suspend fun checks(fastId: String): List<RefeedCheckEntity>

    @Query("SELECT * FROM refeed_check WHERE id = :id AND deleted_at IS NULL")
    suspend fun check(id: String): RefeedCheckEntity?

    @Insert suspend fun insertChecks(entities: List<RefeedCheckEntity>)
    @Update suspend fun updateCheck(entity: RefeedCheckEntity)

    @Query("SELECT * FROM meal WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY eaten_at")
    fun observeMeals(fastId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE fast_id = :fastId AND deleted_at IS NULL ORDER BY eaten_at")
    suspend fun meals(fastId: String): List<MealEntity>

    @Insert suspend fun insertMeal(entity: MealEntity)
}
