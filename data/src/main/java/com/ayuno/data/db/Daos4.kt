package com.ayuno.data.db

import androidx.room.Dao
import androidx.room.Query

/** EV-001: retirar el plan copiado de un ayuno (H-044, D-16) y borrar un ayuno para siempre (H-045, D-15). */
@Dao
interface FastLifecycleDao {
    @Query("SELECT * FROM rule WHERE fast_id = :fastId AND deleted_at IS NOT NULL ORDER BY rowid")
    suspend fun replacedRules(fastId: String): List<RuleEntity>

    // ---------- Retirar el plan copiado (borrado blando) ----------

    @Query("UPDATE metric_schedule SET deleted_at = :now WHERE plan_metric_id IN (SELECT id FROM plan_metric WHERE fast_id = :fastId) AND deleted_at IS NULL")
    suspend fun retireSchedules(fastId: String, now: String)

    @Query("UPDATE stage SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireStages(fastId: String, now: String)

    @Query("UPDATE plan_metric SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retirePlanMetrics(fastId: String, now: String)

    @Query("UPDATE rule SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireRules(fastId: String, now: String)

    @Query("UPDATE intake_goal SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireGoals(fastId: String, now: String)

    @Query("UPDATE checklist_item SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireChecklist(fastId: String, now: String)

    @Query("UPDATE stage_notice SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireNotices(fastId: String, now: String)

    @Query("UPDATE plan_product SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retirePlanProducts(fastId: String, now: String)

    @Query("UPDATE refeed_step SET deleted_at = :now WHERE fast_id = :fastId AND deleted_at IS NULL")
    suspend fun retireRefeedSteps(fastId: String, now: String)

    // ---------- Borrado definitivo (incluye filas ya borradas en blando) ----------

    @Query(
        """DELETE FROM edit_log WHERE row_id = :fastId
        OR row_id IN (SELECT id FROM measurement WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM blood_pressure_reading WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM symptom WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM intake WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM product_log WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM alert WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM note WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM meal WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM stage WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM plan_metric WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM metric_schedule WHERE plan_metric_id IN (SELECT id FROM plan_metric WHERE fast_id = :fastId))
        OR row_id IN (SELECT id FROM rule WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM intake_goal WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM checklist_item WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM stage_notice WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM plan_product WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM refeed_step WHERE fast_id = :fastId)
        OR row_id IN (SELECT id FROM refeed_check WHERE refeed_step_id IN (SELECT id FROM refeed_step WHERE fast_id = :fastId))""",
    )
    suspend fun deleteEditLog(fastId: String)

    @Query("DELETE FROM metric_schedule WHERE plan_metric_id IN (SELECT id FROM plan_metric WHERE fast_id = :fastId)")
    suspend fun deleteSchedules(fastId: String)

    @Query("DELETE FROM refeed_check WHERE refeed_step_id IN (SELECT id FROM refeed_step WHERE fast_id = :fastId)")
    suspend fun deleteRefeedChecks(fastId: String)

    @Query("DELETE FROM measurement WHERE fast_id = :fastId")
    suspend fun deleteMeasurements(fastId: String)

    @Query("DELETE FROM blood_pressure_reading WHERE fast_id = :fastId")
    suspend fun deleteBloodPressure(fastId: String)

    @Query("DELETE FROM symptom WHERE fast_id = :fastId")
    suspend fun deleteSymptoms(fastId: String)

    @Query("DELETE FROM intake WHERE fast_id = :fastId")
    suspend fun deleteIntakes(fastId: String)

    @Query("DELETE FROM product_log WHERE fast_id = :fastId")
    suspend fun deleteProductLogs(fastId: String)

    @Query("DELETE FROM alert WHERE fast_id = :fastId")
    suspend fun deleteAlerts(fastId: String)

    @Query("DELETE FROM note WHERE fast_id = :fastId")
    suspend fun deleteNotes(fastId: String)

    @Query("DELETE FROM meal WHERE fast_id = :fastId")
    suspend fun deleteMeals(fastId: String)

    @Query("DELETE FROM stage WHERE fast_id = :fastId")
    suspend fun deleteStages(fastId: String)

    @Query("DELETE FROM plan_metric WHERE fast_id = :fastId")
    suspend fun deletePlanMetrics(fastId: String)

    @Query("DELETE FROM rule WHERE fast_id = :fastId")
    suspend fun deleteRules(fastId: String)

    @Query("DELETE FROM intake_goal WHERE fast_id = :fastId")
    suspend fun deleteGoals(fastId: String)

    @Query("DELETE FROM checklist_item WHERE fast_id = :fastId")
    suspend fun deleteChecklist(fastId: String)

    @Query("DELETE FROM stage_notice WHERE fast_id = :fastId")
    suspend fun deleteNotices(fastId: String)

    @Query("DELETE FROM plan_product WHERE fast_id = :fastId")
    suspend fun deletePlanProducts(fastId: String)

    @Query("DELETE FROM refeed_step WHERE fast_id = :fastId")
    suspend fun deleteRefeedSteps(fastId: String)

    @Query("DELETE FROM fast WHERE id = :fastId")
    suspend fun deleteFast(fastId: String)
}
