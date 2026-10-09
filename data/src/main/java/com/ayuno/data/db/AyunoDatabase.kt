package com.ayuno.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayuno.data.seed.CatalogSeed
import java.time.Clock

/**
 * Base de datos de la app (ADR-003). Hasta la primera entrega se mantiene en la versión 1 y el esquema
 * crece historia a historia; a partir de la primera instalación real, todo cambio lleva migración probada.
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
    ],
    version = 1,
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

    companion object {
        /** Configuración común a la base real y a la de pruebas: integridad y datos iniciales. */
        fun <T : RoomDatabase> RoomDatabase.Builder<T>.ayunoSetup(context: Context, clock: Clock): RoomDatabase.Builder<T> =
            addCallback(CreateCallback(context.applicationContext, clock))
    }

    private class CreateCallback(private val context: Context, private val clock: Clock) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            // Propietario de plan: exactamente uno de template_id / fast_id (D-03). Room no declara CHECK.
            listOf("stage", "plan_metric", "rule").forEach { table ->
                listOf("INSERT", "UPDATE").forEach { op ->
                    db.execSQL(
                        """
                        CREATE TRIGGER ${table}_owner_xor_${op.lowercase()} BEFORE $op ON $table
                        WHEN (NEW.template_id IS NULL) = (NEW.fast_id IS NULL)
                        BEGIN SELECT RAISE(ABORT, '$table: exactamente un propietario'); END
                        """.trimIndent(),
                    )
                }
            }
            CatalogSeed.insertInto(db, context, clock)
        }
    }
}
