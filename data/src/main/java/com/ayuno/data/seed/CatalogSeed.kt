package com.ayuno.data.seed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayuno.domain.model.BloodPressureRanges
import com.ayuno.domain.time.LocalTimes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.UUID

/** Datos iniciales y configuración fuera del código (ADR-008): `assets/seed/` y `assets/config/`. */
object CatalogSeed {
    private val json = Json { ignoreUnknownKeys = false }

    @Serializable
    private data class SeedMetricType(
        val code: String,
        val name: String,
        val unit: String,
        @SerialName("alt_unit") val altUnit: String?,
        @SerialName("entry_unit") val entryUnit: String,
        val numeric: Boolean,
        @SerialName("min_valid") val minValid: Double?,
        @SerialName("max_valid") val maxValid: Double?,
    )

    @Serializable
    private data class SeedOption(val label: String, val value: Double)

    @Serializable
    private data class SeedMetricOptions(@SerialName("metric_type_code") val metricTypeCode: String, val options: List<SeedOption>)

    @Serializable
    private data class SeedSymptomType(
        val code: String,
        val name: String,
        @SerialName("group_name") val groupName: String?,
        @SerialName("is_alarm") val isAlarm: Boolean,
    )

    @Serializable
    private data class SeedIntakeType(
        val code: String,
        val name: String,
        val unit: String,
        @SerialName("breaks_fast") val breaksFast: Boolean,
        @SerialName("sodium_factor") val sodiumFactor: Double?,
        @SerialName("quick_unit_code") val quickUnitCode: String? = null,
    )

    @Serializable
    private data class SeedHouseholdUnit(
        @SerialName("intake_type_code") val intakeTypeCode: String,
        val code: String,
        val name: String,
        val equivalent: Double,
        val imprecise: Boolean,
    )

    @Serializable
    private data class SeedGoal(@SerialName("intake_type_code") val intakeTypeCode: String, @SerialName("nudge_after_hours") val nudgeAfterHours: Double?)

    @Serializable
    private data class SeedStage(val kind: String, @SerialName("duration_value") val durationValue: Double?, @SerialName("duration_unit") val durationUnit: String)

    @Serializable
    private data class SeedSchedule(@SerialName("stage_kind") val stageKind: String, @SerialName("time_of_day") val timeOfDay: String)

    @Serializable
    private data class SeedMetric(@SerialName("metric_type_code") val metricTypeCode: String, val schedules: List<SeedSchedule>)

    @Serializable
    private data class SeedRule(
        val name: String,
        val active: Boolean,
        @SerialName("condition_kind") val conditionKind: String,
        @SerialName("metric_type_code") val metricTypeCode: String?,
        @SerialName("intake_type_code") val intakeTypeCode: String? = null,
        val level: String,
        val message: String,
        val action: String,
    )

    @Serializable
    private data class SeedTemplate(
        val name: String,
        @SerialName("target_hours") val targetHours: Double,
        @SerialName("milestone_hours") val milestoneHours: List<Double>,
        val stages: List<SeedStage>,
        val metrics: List<SeedMetric>,
        val rules: List<SeedRule>,
        val goals: List<SeedGoal> = emptyList(),
    )

    @Serializable
    private data class Range(val min: Double, val max: Double)

    @Serializable
    private data class SeedBloodPressureRanges(val systolic: Range, val diastolic: Range, val pulse: Range)

    fun insertInto(db: SupportSQLiteDatabase, context: Context, clock: Clock) {
        val now = LocalTimes.format(LocalTimes.now(clock))
        val types = json.decodeFromString<List<SeedMetricType>>(read(context, "seed/metric_types.json"))
        types.forEach { t ->
            db.insert(
                "metric_type",
                SQLiteDatabase.CONFLICT_IGNORE,
                ContentValues().apply {
                    put("code", t.code)
                    put("name", t.name)
                    put("unit", t.unit)
                    put("alt_unit", t.altUnit)
                    put("entry_unit", t.entryUnit)
                    put("is_numeric", t.numeric)
                    put("min_valid", t.minValid)
                    put("max_valid", t.maxValid)
                    put("active", true)
                    put("created_at", now)
                },
            )
        }
        insertMetricOptions(db, context, now)
        insertCatalogs(db, context, now)
        insertHouseholdUnits(db, context, now)
        insertExampleTemplate(db, context, now)
        updateExampleForReview(db, context, now)
    }

    /**
     * H-050, H-051: medidas caseras de fábrica con identificadores fijos y botón rápido de cada tipo. También la usa
     * la migración 8→9: solo para tipos que existen, sin duplicar, y el botón solo si el tipo no tiene ya uno y no rompe
     * el ayuno. Son equivalencias de cocina, no dosis.
     */
    fun insertHouseholdUnits(db: SupportSQLiteDatabase, context: Context, now: String) {
        val types = db.query("SELECT code, breaks_fast, quick_unit_code FROM intake_type").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), (c.getInt(1) == 1) to c.isNull(2)) }
        }
        val units = json.decodeFromString<List<SeedHouseholdUnit>>(read(context, "seed/household_units.json")).filter { it.intakeTypeCode in types }
        units.groupBy { it.intakeTypeCode }.forEach { (type, list) ->
            val taken = db.query("SELECT code FROM household_unit WHERE intake_type_code = ? AND deleted_at IS NULL", arrayOf(type)).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
            list.filter { it.code !in taken }.forEachIndexed { i, u ->
                db.insert("household_unit", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply {
                    put("id", seedId("household_unit/$type/${u.code}")); put("code", u.code); put("intake_type_code", type); put("name", u.name)
                    put("equivalent", u.equivalent); put("imprecise", u.imprecise); put("position", taken.size + i); put("created_at", now)
                })
            }
        }
        json.decodeFromString<List<SeedIntakeType>>(read(context, "seed/intake_types.json")).filter { it.quickUnitCode != null }.forEach { t ->
            val (breaksFast, noQuick) = types[t.code] ?: return@forEach
            if (breaksFast || !noQuick) return@forEach
            db.update(
                "intake_type", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply { put("quick_unit_code", t.quickUnitCode) },
                "code = ? AND EXISTS (SELECT 1 FROM household_unit WHERE intake_type_code = ? AND code = ? AND deleted_at IS NULL)",
                arrayOf(t.code, t.code, t.quickUnitCode),
            )
        }
    }

    /**
     * H-003, H-057 (D-25): la plantilla de ejemplo marca «de ejemplo» sus reglas de fábrica (solo si siguen idénticas a
     * la semilla) y recibe sus metas sin cifra con «preguntar tras 6 h» y la regla de sal, si le faltan. Al crear la base
     * y en la migración 8→9; ninguna plantilla propia ni ningún ayuno cambia.
     */
    fun updateExampleForReview(db: SupportSQLiteDatabase, context: Context, now: String) {
        val templateId = exampleId("template")
        val exists = db.query("SELECT COUNT(*) FROM template WHERE id = ?", arrayOf(templateId)).use { it.moveToFirst() && it.getInt(0) > 0 }
        if (!exists) return
        val t = json.decodeFromString<SeedTemplate>(read(context, "seed/example_template.json"))
        t.rules.forEachIndexed { i, r ->
            insertExampleRule(db, templateId, i, r, now)
            db.update(
                "rule", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply { put("is_placeholder", true) },
                "id = ? AND template_id = ? AND name = ? AND message = ? AND threshold IS NULL AND check_time IS NULL AND goal_percent IS NULL",
                arrayOf(exampleId("rule/$i"), templateId, r.name, r.message),
            )
        }
        val present = db.query("SELECT intake_type_code FROM intake_goal WHERE template_id = ? AND deleted_at IS NULL", arrayOf(templateId)).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        t.goals.filter { it.intakeTypeCode !in present }.forEach { g ->
            val unit = db.query("SELECT unit FROM intake_type WHERE code = ?", arrayOf(g.intakeTypeCode)).use { if (it.moveToFirst()) it.getString(0) else null } ?: return@forEach
            db.insert("intake_goal", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply {
                put("id", exampleId("goal/${g.intakeTypeCode}")); put("template_id", templateId); put("intake_type_code", g.intakeTypeCode)
                putNull("daily_amount"); put("unit", unit); put("nudge_after_hours", g.nudgeAfterHours); put("created_at", now)
            })
        }
    }

    private fun insertExampleRule(db: SupportSQLiteDatabase, templateId: String, index: Int, r: SeedRule, now: String) {
        db.insert(
            "rule",
            SQLiteDatabase.CONFLICT_IGNORE,
            ContentValues().apply {
                put("id", exampleId("rule/$index")); put("template_id", templateId); put("name", r.name); put("active", r.active)
                put("condition_kind", r.conditionKind); put("metric_type_code", r.metricTypeCode); put("intake_type_code", r.intakeTypeCode)
                put("filter_stage_kinds", "[]"); put("filter_on_waking", false); put("level", r.level); put("message", r.message)
                put("action", r.action); put("created_at", now)
            },
        )
    }

    /**
     * H-049: palabras de fábrica de las escalas, con identificadores fijos (restaurar en otro teléfono no las duplica).
     * También la usa la migración 7→8: solo para tipos que existen, son numéricos y aún no tienen palabras; no toca
     * ninguna otra tabla.
     */
    fun insertMetricOptions(db: SupportSQLiteDatabase, context: Context, now: String) {
        json.decodeFromString<List<SeedMetricOptions>>(read(context, "seed/metric_options.json")).forEach { group ->
            val numeric = db.query("SELECT is_numeric FROM metric_type WHERE code = ?", arrayOf(group.metricTypeCode))
                .use { it.moveToFirst() && it.getInt(0) == 1 }
            val hasOptions = db.query("SELECT COUNT(*) FROM metric_option WHERE metric_type_code = ?", arrayOf(group.metricTypeCode))
                .use { it.moveToFirst() && it.getInt(0) > 0 }
            if (!numeric || hasOptions) return@forEach
            group.options.forEachIndexed { i, o ->
                db.insert(
                    "metric_option",
                    SQLiteDatabase.CONFLICT_IGNORE,
                    ContentValues().apply {
                        put("id", seedId("metric_option/${group.metricTypeCode}/$i")); put("metric_type_code", group.metricTypeCode)
                        put("label", o.label); put("value", o.value); put("position", i); put("created_at", now)
                    },
                )
            }
        }
    }

    private fun seedId(path: String): String = UUID.nameUUIDFromBytes("ayuno-catalogo/$path".toByteArray()).toString()

    /** Identificadores fijos: la plantilla de ejemplo es la misma en cada instalación y no se duplica al importar. */
    private fun exampleId(path: String): String = UUID.nameUUIDFromBytes("ayuno-ejemplo/$path".toByteArray()).toString()

    /**
     * H-003: plantilla de ejemplo con etapas, mediciones y estructura de reglas, sin umbrales ni dosis. La regla de
     * medición va inactiva y sin umbral: la persona pone el suyo al activarla. También la usa la migración 4→5.
     */
    fun insertExampleTemplate(db: SupportSQLiteDatabase, context: Context, now: String) {
        val t = json.decodeFromString<SeedTemplate>(read(context, "seed/example_template.json"))
        val templateId = exampleId("template")
        fun insert(table: String, values: ContentValues) = db.insert(table, SQLiteDatabase.CONFLICT_IGNORE, values.apply { put("created_at", now) })
        insert(
            "template",
            ContentValues().apply {
                put("id", templateId); put("name", t.name); put("target_hours", t.targetHours)
                put("milestone_hours", t.milestoneHours.joinToString(",", "[", "]"))
            },
        )
        t.stages.forEach { s ->
            insert(
                "stage",
                ContentValues().apply {
                    put("id", exampleId("stage/${s.kind}")); put("template_id", templateId); put("kind", s.kind)
                    put("duration_value", s.durationValue); put("duration_unit", s.durationUnit)
                },
            )
        }
        insertPlanMetrics(db, t.metrics, templateId, now)
        t.rules.forEachIndexed { i, r -> insertExampleRule(db, templateId, i, r, now) }
    }

    /** Síntomas y tipos de ingesta de ejemplo (H-010, H-011); también los usa la migración 1→2. */
    /**
     * La plantilla de ejemplo incluye todas las mediciones del catálogo; solo las tres primeras llevan horario, las
     * demás se pueden anotar sin recordatorio. La migración 5→6 las añade a la de ejemplo ya instalada si sigue existiendo.
     */
    fun insertExampleMetrics(db: SupportSQLiteDatabase, context: Context, now: String) {
        val templateId = exampleId("template")
        val exists = db.query("SELECT COUNT(*) FROM template WHERE id = ?", arrayOf(templateId)).use { it.moveToFirst() && it.getInt(0) > 0 }
        if (!exists) return
        val metrics = json.decodeFromString<SeedTemplate>(read(context, "seed/example_template.json")).metrics
        val present = db.query("SELECT metric_type_code FROM plan_metric WHERE template_id = ?", arrayOf(templateId)).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        insertPlanMetrics(db, metrics.filter { it.metricTypeCode !in present }, templateId, now)
    }

    /**
     * Migración 6→7: la plantilla de ejemplo instalada pasa a los hitos del archivo de ejemplo, solo si conserva los
     * de antes (si la persona los cambió, se respetan). Los ayunos ya creados guardan su copia y no cambian.
     */
    fun updateExampleMilestones(db: SupportSQLiteDatabase, context: Context, now: String) {
        val milestones = json.decodeFromString<SeedTemplate>(read(context, "seed/example_template.json")).milestoneHours
        db.update(
            "template",
            SQLiteDatabase.CONFLICT_NONE,
            ContentValues().apply { put("milestone_hours", milestones.joinToString(",", "[", "]")); put("updated_at", now) },
            "id = ? AND milestone_hours = ?",
            arrayOf(exampleId("template"), PREVIOUS_EXAMPLE_MILESTONES),
        )
    }

    private const val PREVIOUS_EXAMPLE_MILESTONES = "[12.0,24.0]"

    private fun insertPlanMetrics(db: SupportSQLiteDatabase, metrics: List<SeedMetric>, templateId: String, now: String) {
        fun insert(table: String, values: ContentValues) = db.insert(table, SQLiteDatabase.CONFLICT_IGNORE, values.apply { put("created_at", now) })
        metrics.forEach { m ->
            val metricId = exampleId("metric/${m.metricTypeCode}")
            insert("plan_metric", ContentValues().apply { put("id", metricId); put("template_id", templateId); put("metric_type_code", m.metricTypeCode) })
            m.schedules.forEach { s ->
                insert(
                    "metric_schedule",
                    ContentValues().apply {
                        put("id", exampleId("schedule/${m.metricTypeCode}/${s.stageKind}/${s.timeOfDay}")); put("plan_metric_id", metricId)
                        put("stage_kind", s.stageKind); put("time_of_day", s.timeOfDay)
                    },
                )
            }
        }
    }

    fun insertCatalogs(db: SupportSQLiteDatabase, context: Context, now: String) {
        json.decodeFromString<List<SeedSymptomType>>(read(context, "seed/symptom_types.json")).forEach { t ->
            db.insert("symptom_type", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply {
                put("code", t.code); put("name", t.name); put("group_name", t.groupName); put("is_alarm", t.isAlarm)
                put("active", true); put("created_at", now)
            })
        }
        json.decodeFromString<List<SeedIntakeType>>(read(context, "seed/intake_types.json")).forEach { t ->
            db.insert("intake_type", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply {
                put("code", t.code); put("name", t.name); put("unit", t.unit); put("breaks_fast", t.breaksFast)
                put("sodium_factor", t.sodiumFactor); put("created_at", now)
            })
        }
    }

    fun bloodPressureRanges(context: Context): BloodPressureRanges {
        val r = json.decodeFromString<SeedBloodPressureRanges>(read(context, "config/blood_pressure_ranges.json"))
        return BloodPressureRanges(r.systolic.min..r.systolic.max, r.diastolic.min..r.diastolic.max, r.pulse.min..r.pulse.max)
    }

    private fun read(context: Context, path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }
}
