package com.ayuno.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayuno.data.db.AyunoDatabase.Companion.ayunoSetup
import com.ayuno.data.repository.RoomCatalogRepository
import com.ayuno.data.repository.RoomFastRepository
import com.ayuno.domain.model.FastStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    /** Columnas de cada tabla del esquema 7 tal como Room lo exportó: lo que hay en el teléfono antes de EV-005. */
    private fun v7Tables(): Map<String, List<Pair<String, String>>> {
        val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
        val schema = assets.open("com.ayuno.data.db.AyunoDatabase/7.json").bufferedReader().use { it.readText() }
        val entities = Json.parseToJsonElement(schema).jsonObject.getValue("database").jsonObject.getValue("entities").jsonArray
        return entities.associate { e ->
            val o = e.jsonObject
            o.getValue("tableName").jsonPrimitive.content to o.getValue("fields").jsonArray.map {
                it.jsonObject.getValue("columnName").jsonPrimitive.content to it.jsonObject.getValue("affinity").jsonPrimitive.content
            }
        }
    }

    private fun SupportSQLiteDatabase.snapshot(tables: Collection<String>): Map<String, List<List<String?>>> = tables.associateWith { table ->
        query("SELECT * FROM `$table` ORDER BY rowid").use { c ->
            buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) }
        }
    }

    /**
     * EV-005: la base del teléfono (versión 7) con un ayuno en curso. Además de filas reales de las escalas, cada
     * tabla recibe dos filas con valores en todas sus columnas, para comparar columna a columna tras migrar.
     */
    private fun createV7WithAFastInProgress(withFiller: Boolean = true): Map<String, List<Pair<String, String>>> {
        val tables = v7Tables()
        helper.createDatabase(dbName, 7).apply {
            val types = listOf("glucose" to 1, "hunger" to 1, "energy" to 1, "mood" to 0, "mental_clarity" to 1)
            types.forEach { (code, numeric) ->
                execSQL(
                    "INSERT INTO metric_type (code, name, unit, entry_unit, is_numeric, min_valid, max_valid, active, created_at) " +
                        "VALUES ('$code', '$code', 'de 0 a 10', 'de 0 a 10', $numeric, 0, 10, 1, '2026-10-08T10:00:00')",
                )
            }
            execSQL(
                "INSERT INTO fast (id, template_id, status, target_hours, milestone_hours, last_meal_planned, clock_start_planned, " +
                    "clock_start_actual, tz_offset, created_at) VALUES ('f-en-curso', NULL, 'in_progress', 72.0, '[24.0,36.0,48.0]', " +
                    "'2026-10-21T18:00:00', '2026-10-21T22:00:00', '2026-10-21T22:05:10', '-05:00', '2026-10-21T18:00:00')",
            )
            listOf("hunger" to 7.0, "hunger" to 4.0, "energy" to 3.0, "mental_clarity" to 6.0).forEachIndexed { i, (code, v) ->
                execSQL(
                    "INSERT INTO measurement (id, fast_id, metric_type_code, taken_at, value_num, value_entered, unit_entered, on_waking, created_at) " +
                        "VALUES ('m$i', 'f-en-curso', '$code', '2026-10-22T0$i:00:00', $v, $v, 'de 0 a 10', 0, '2026-10-22T0$i:00:01')",
                )
            }
            tables.forEach { (table, cols) ->
                if (!withFiller || table == "settings" || table == "person_profile") return@forEach
                repeat(2) { n ->
                    val values = cols.map { (name, affinity) ->
                        when {
                            name == "template_id" -> "NULL"
                            name == "fast_id" -> "'f-en-curso'"
                            affinity == "INTEGER" -> "${n + 1}"
                            affinity == "REAL" -> "${n + 1}.25"
                            else -> "'$table-$name-$n'"
                        }
                    }
                    execSQL("INSERT INTO `$table` (${cols.joinToString { "`${it.first}`" }}) VALUES (${values.joinToString()})")
                }
            }
            close()
        }
        return tables
    }

    @Test
    fun `la migracion 7 a 8 conserva cada fila de cada tabla y el ayuno en curso`() {
        val tables = createV7WithAFastInProgress()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val before = helper.runMigrationsAndValidate(dbName, 7, false).use { it.snapshot(tables.keys) }
        val db = helper.runMigrationsAndValidate(dbName, 8, true, Migrations.From7To8(context, Clock.systemDefaultZone()))
        assertEquals(before, db.snapshot(tables.keys))
        db.query("SELECT status, clock_start_actual FROM fast WHERE id = 'f-en-curso'").use {
            it.moveToFirst(); assertEquals("in_progress", it.getString(0)); assertEquals("2026-10-21T22:05:10", it.getString(1))
        }
        db.query("SELECT COUNT(*) FROM measurement WHERE fast_id = 'f-en-curso'").use { it.moveToFirst(); assertEquals(6, it.getInt(0)) }
    }

    @Test
    fun `la migracion 7 a 8 siembra las palabras de las escalas numericas existentes sin duplicar`() {
        createV7WithAFastInProgress()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        val db = helper.runMigrationsAndValidate(dbName, 8, true, Migrations.From7To8(context, clock))
        Migrations.From7To8(context, clock).migrate(db)
        fun labels(code: String) = db.query("SELECT label, value FROM metric_option WHERE metric_type_code = '$code' ORDER BY position").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getDouble(1)) }
        }
        assertEquals(listOf("Nada" to 0.0, "Poca" to 3.0, "Moderada" to 5.0, "Mucha" to 7.0, "Intensa" to 10.0), labels("hunger"))
        assertEquals(5, labels("energy").size)
        assertEquals(5, labels("mental_clarity").size)
        // Una escala que la persona hizo no numérica no recibe palabras; las que no existen tampoco.
        assertTrue(labels("mood").isEmpty())
        db.query("SELECT COUNT(*) FROM metric_option").use { it.moveToFirst(); assertEquals(15, it.getInt(0)) }
    }

    @Test
    fun `la app nueva abre la base 7 del telefono y muestra las palabras sin tocar las mediciones`() = runTest {
        createV7WithAFastInProgress(withFiller = false)
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val clock = Clock.systemDefaultZone()
        val database = Room.databaseBuilder(context, AyunoDatabase::class.java, dbName).ayunoSetup(context, clock).allowMainThreadQueries().build()
        try {
            val hunger = RoomCatalogRepository(database, context, clock).metricType("hunger")!!
            assertEquals("Mucha", hunger.optionFor(7.0)?.label)
            assertEquals(null, hunger.optionFor(4.0))
            val fast = RoomFastRepository(database, clock).get("f-en-curso")!!.fast
            assertEquals(FastStatus.IN_PROGRESS, fast.status)
            assertEquals(listOf(7.0, 4.0, 3.0, 6.0), database.recordDao().measurements("f-en-curso").sortedBy { it.id }.map { it.valueNum })
        } finally {
            database.close()
        }
    }
}
