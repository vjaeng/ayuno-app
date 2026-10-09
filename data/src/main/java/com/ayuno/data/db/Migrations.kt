package com.ayuno.data.db

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayuno.data.seed.CatalogSeed
import com.ayuno.domain.time.LocalTimes
import java.time.Clock

/**
 * Migraciones del esquema (ADR-003). Cada una se prueba con `MigrationTestHelper` contra el esquema exportado.
 * El SQL de las tablas nuevas es el que Room genera (`schemas/…/N.json`), para que la validación coincida.
 */
object Migrations {

    /** Versión 1 (tanda 1, primera instalación real) → 2 (tanda 2a: síntomas, ingestas, productos, notas, resto del plan). */
    class From1To2(private val context: Context, private val clock: Clock) : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            CREATE_V2.forEach(db::execSQL)
            AyunoDatabase.createOwnerTriggers(db, listOf("intake_goal", "checklist_item", "stage_notice", "plan_product"))
            CatalogSeed.insertCatalogs(db, context, LocalTimes.format(LocalTimes.now(clock)))
        }

        private companion object {
            val CREATE_V2 = listOf(
            """CREATE TABLE IF NOT EXISTS `symptom_type` (`code` TEXT NOT NULL, `name` TEXT NOT NULL, `group_name` TEXT, `is_alarm` INTEGER NOT NULL, `active` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`code`))""",
            """CREATE TABLE IF NOT EXISTS `symptom` (`id` TEXT NOT NULL, `fast_id` TEXT NOT NULL, `symptom_type_code` TEXT NOT NULL, `noted_at` TEXT NOT NULL, `severity` INTEGER NOT NULL, `note` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_symptom_fast_id` ON `symptom` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `intake_type` (`code` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `breaks_fast` INTEGER NOT NULL, `sodium_factor` REAL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`code`))""",
            """CREATE TABLE IF NOT EXISTS `intake` (`id` TEXT NOT NULL, `fast_id` TEXT NOT NULL, `intake_type_code` TEXT NOT NULL, `taken_at` TEXT NOT NULL, `amount` REAL NOT NULL, `unit` TEXT NOT NULL, `note` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_intake_fast_id` ON `intake` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `intake_goal` (`id` TEXT NOT NULL, `template_id` TEXT, `fast_id` TEXT, `intake_type_code` TEXT NOT NULL, `daily_amount` REAL NOT NULL, `unit` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_intake_goal_template_id` ON `intake_goal` (`template_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_intake_goal_fast_id` ON `intake_goal` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `checklist_item` (`id` TEXT NOT NULL, `template_id` TEXT, `fast_id` TEXT, `text` TEXT NOT NULL, `is_checked` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_checklist_item_template_id` ON `checklist_item` (`template_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_checklist_item_fast_id` ON `checklist_item` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `stage_notice` (`id` TEXT NOT NULL, `template_id` TEXT, `fast_id` TEXT, `text` TEXT NOT NULL, `stage_kinds` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_stage_notice_template_id` ON `stage_notice` (`template_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_stage_notice_fast_id` ON `stage_notice` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `product` (`code` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `default_amount` REAL NOT NULL, `unit` TEXT NOT NULL, `avoid` INTEGER NOT NULL, `active` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`code`))""",
            """CREATE TABLE IF NOT EXISTS `product_schedule` (`id` TEXT NOT NULL, `product_code` TEXT NOT NULL, `time_of_day` TEXT NOT NULL, `amount` REAL NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_product_schedule_product_code` ON `product_schedule` (`product_code`)""",
            """CREATE TABLE IF NOT EXISTS `plan_product` (`id` TEXT NOT NULL, `template_id` TEXT, `fast_id` TEXT, `product_code` TEXT NOT NULL, `during_fast` TEXT NOT NULL, `optional_enabled` INTEGER NOT NULL, `reason` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_plan_product_template_id` ON `plan_product` (`template_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_plan_product_fast_id` ON `plan_product` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `product_log` (`id` TEXT NOT NULL, `fast_id` TEXT NOT NULL, `product_code` TEXT NOT NULL, `scheduled_at` TEXT, `status` TEXT NOT NULL, `taken_at` TEXT, `planned_amount` REAL, `amount` REAL, `unit` TEXT, `reason` TEXT, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_product_log_fast_id` ON `product_log` (`fast_id`)""",
            """CREATE TABLE IF NOT EXISTS `note` (`id` TEXT NOT NULL, `fast_id` TEXT NOT NULL, `noted_at` TEXT NOT NULL, `text` TEXT NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT, `deleted_at` TEXT, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_note_fast_id` ON `note` (`fast_id`)""",
            )
        }
    }

    fun all(context: Context, clock: Clock): Array<Migration> = arrayOf(From1To2(context, clock))
}
