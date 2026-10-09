package com.ayuno.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayuno.data.seed.CatalogSeed
import java.time.Clock

/**
 * Base de datos de la app (ADR-003). Desde la primera instalación real (versión 1, tanda 1) todo cambio de
 * esquema sube la versión y lleva una migración probada en [Migrations]; nunca migración destructiva.
 */
@Database(
    entities = [
        SettingsEntity::class,
        MetricTypeEntity::class,
        TemplateEntity::class,
        StageEntity::class,
        PlanMetricEntity::class,
        MetricScheduleEntity::class,
        RuleEntity::class,
        FastEntity::class,
        MeasurementEntity::class,
        BloodPressureReadingEntity::class,
        AlertEntity::class,
        EditLogEntity::class,
        SymptomTypeEntity::class,
        SymptomEntity::class,
        IntakeTypeEntity::class,
        IntakeEntity::class,
        IntakeGoalEntity::class,
        ChecklistItemEntity::class,
        StageNoticeEntity::class,
        ProductEntity::class,
        ProductScheduleEntity::class,
        PlanProductEntity::class,
        ProductLogEntity::class,
        NoteEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AyunoDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun catalogDao(): CatalogDao
    abstract fun planDao(): PlanDao
    abstract fun fastDao(): FastDao
    abstract fun recordDao(): RecordDao
    abstract fun alertDao(): AlertDao
    abstract fun auditDao(): AuditDao
    abstract fun catalog2Dao(): Catalog2Dao
    abstract fun plan2Dao(): Plan2Dao
    abstract fun record2Dao(): Record2Dao

    companion object {
        /** Configuración común a la base real y a la de pruebas: integridad y datos iniciales. */
        fun <T : RoomDatabase> RoomDatabase.Builder<T>.ayunoSetup(context: Context, clock: Clock): RoomDatabase.Builder<T> =
            addCallback(CreateCallback(context.applicationContext, clock))
                .addMigrations(*Migrations.all(context.applicationContext, clock))

        private val PLAN_TABLES = listOf("stage", "plan_metric", "rule", "intake_goal", "checklist_item", "stage_notice", "plan_product")

        /** Propietario de plan: exactamente uno de template_id / fast_id (D-03). Room no declara CHECK. */
        internal fun createOwnerTriggers(db: SupportSQLiteDatabase, tables: List<String>) {
            tables.forEach { table ->
                listOf("INSERT", "UPDATE").forEach { op ->
                    db.execSQL(
                        """
                        CREATE TRIGGER IF NOT EXISTS ${table}_owner_xor_${op.lowercase()} BEFORE $op ON $table
                        WHEN (NEW.template_id IS NULL) = (NEW.fast_id IS NULL)
                        BEGIN SELECT RAISE(ABORT, '$table: exactamente un propietario'); END
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    private class CreateCallback(private val context: Context, private val clock: Clock) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            createOwnerTriggers(db, PLAN_TABLES)
            CatalogSeed.insertInto(db, context, clock)
        }
    }
}
