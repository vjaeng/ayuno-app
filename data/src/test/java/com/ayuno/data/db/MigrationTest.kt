package com.ayuno.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayuno.data.db.AyunoDatabase.Companion.ayunoSetup
import com.ayuno.data.repository.RoomCatalogRepository
import com.ayuno.data.repository.RoomFastRepository
import com.ayuno.domain.model.FastStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

/**
 * La base de la primera instalación real (versión 1, tanda 1) se migra a la versión 2 sin perder datos.
 * Reproduce el cierre al abrir la app tras instalar la tanda 2a encima de la tanda 1.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AyunoDatabase::class.java)

    private fun createV1WithAFast() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO fast (id, template_id, status, target_hours, milestone_hours, last_meal_planned, clock_start_planned, " +
                    "clock_start_actual, tz_offset, created_at) VALUES ('f1', NULL, 'in_progress', 57.0, '[12.0,24.0]', " +
                    "'2026-10-22T18:00:00', '2026-10-22T22:00:00', '2026-10-22T22:10:00', '-05:00', '2026-10-22T18:00:00')",
            )
            execSQL(
                "INSERT INTO measurement (id, fast_id, metric_type_code, taken_at, value_num, value_entered, unit_entered, on_waking, created_at) " +
                    "VALUES ('m1', 'f1', 'glucose', '2026-10-23T07:02:00', 72.0, 72.0, 'mg/dL', 0, '2026-10-23T07:02:00')",
            )
            close()
        }
    }

    @Test
    fun `la migracion 1 a 2 produce exactamente el esquema 2 de Room`() {
        createV1WithAFast()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(ApplicationProvider.getApplicationContext(), Clock.systemDefaultZone()))
    }

    @Test
    fun `la migracion 2 a 3 produce exactamente el esquema 3 de Room`() {
        createV1WithAFast()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(ApplicationProvider.getApplicationContext(), Clock.systemDefaultZone())).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.From2To3)
    }

    @Test
    fun `la migracion 3 a 4 produce exactamente el esquema 4 de Room`() {
        createV1WithAFast()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(ApplicationProvider.getApplicationContext(), Clock.systemDefaultZone())).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.From2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migrations.From3To4)
    }

    @Test
    fun `la migracion 4 a 5 conserva el esquema y anade la plantilla de ejemplo una sola vez`() {
        createV1WithAFast()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(context, clock)).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.From2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migrations.From3To4).close()
        val db = helper.runMigrationsAndValidate(dbName, 5, true, Migrations.From4To5(context, clock))
        Migrations.From4To5(context, clock).migrate(db)
        db.query("SELECT COUNT(*) FROM template WHERE name = 'Ejemplo (sin valores clínicos)'").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT COUNT(*) FROM fast").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test
    fun `la migracion 5 a 6 conserva el esquema y completa las mediciones de la plantilla de ejemplo sin duplicar`() {
        createV1WithAFast()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(context, clock)).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.From2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migrations.From3To4).close()
        helper.runMigrationsAndValidate(dbName, 5, true, Migrations.From4To5(context, clock)).close()
        val db = helper.runMigrationsAndValidate(dbName, 6, true, Migrations.From5To6(context, clock))
        Migrations.From5To6(context, clock).migrate(db)
        val example = "(SELECT id FROM template WHERE name = 'Ejemplo (sin valores clínicos)')"
        val codes = db.query("SELECT metric_type_code FROM plan_metric WHERE template_id = $example").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        val catalog = listOf("glucose", "ketones", "blood_pressure", "weight", "waist", "temperature", "hunger", "energy", "mood", "mental_clarity")
        assertEquals(catalog.sorted(), codes.sorted())
        db.query("SELECT COUNT(*) FROM fast").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test
    fun `la migracion 6 a 7 cambia los hitos de la plantilla de ejemplo sin tocar ayunos ni hitos cambiados`() {
        createV1WithAFast()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.From1To2(context, clock)).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migrations.From2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migrations.From3To4).close()
        helper.runMigrationsAndValidate(dbName, 5, true, Migrations.From4To5(context, clock)).close()
        helper.runMigrationsAndValidate(dbName, 6, true, Migrations.From5To6(context, clock)).use { db ->
            // Lo que instaló la 4→5 con el archivo de ejemplo de entonces, y una copia que la persona cambió.
            db.execSQL("UPDATE template SET milestone_hours = '[12.0,24.0]' WHERE name = 'Ejemplo (sin valores clínicos)'")
            db.execSQL("INSERT INTO template (id, name, target_hours, milestone_hours, created_at) VALUES ('mine', 'Mía', 36.0, '[12.0,24.0]', 'x')")
        }
        val db = helper.runMigrationsAndValidate(dbName, 7, true, Migrations.From6To7(context, clock))
        fun milestones(where: String) = db.query("SELECT milestone_hours FROM template WHERE $where").use { it.moveToFirst(); it.getString(0) }
        assertEquals("[24.0,36.0,48.0]", milestones("name = 'Ejemplo (sin valores clínicos)'"))
        assertEquals("[12.0,24.0]", milestones("id = 'mine'"))
        db.query("SELECT milestone_hours FROM fast WHERE id = 'f1'").use { it.moveToFirst(); assertEquals("[12.0,24.0]", it.getString(0)) }
    }

    @Test
    fun `la app nueva abre la base de la primera instalacion y conserva el ayuno y sus mediciones`() = runTest {
        createV1WithAFast()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        val database = Room.databaseBuilder(context, AyunoDatabase::class.java, dbName).ayunoSetup(context, clock).allowMainThreadQueries().build()
        try {
            val fast = RoomFastRepository(database, clock).all().single()
            assertEquals("f1", fast.id)
            assertEquals(FastStatus.IN_PROGRESS, fast.status)
            assertEquals(72.0, database.recordDao().measurements("f1").single().valueNum!!, 0.0)
            // Catálogos nuevos precargados por la migración (H-010, H-011).
            val catalog = RoomCatalogRepository(database, context, clock)
            assertTrue(catalog.symptomTypes().any { it.code == "headache" })
            assertTrue(catalog.intakeTypes().any { it.code == "salt" && it.sodiumFactor == 0.393 })
            // Los triggers de propietario también protegen las tablas de plan nuevas.
            val db = database.openHelper.writableDatabase
            assertTrue(runCatching {
                db.execSQL("INSERT INTO intake_goal (id, template_id, fast_id, intake_type_code, daily_amount, unit, created_at) VALUES ('g', NULL, NULL, 'salt', 4, 'g', 'x')")
            }.isFailure)
            assertTrue(runCatching {
                db.execSQL("INSERT INTO refeed_step (id, template_id, fast_id, kind, description, created_at) VALUES ('r', NULL, NULL, 'timed', 'x', 'x')")
            }.isFailure)
            // Tanda 3: el ayuno migrado sigue cargando su plan (sin vuelta a comer) y admite comidas.
            assertTrue(RoomFastRepository(database, clock).get("f1")!!.plan.refeed.isEmpty())
            assertTrue(database.refeedDao().meals("f1").isEmpty())
        } finally {
            database.close()
        }
    }
}
