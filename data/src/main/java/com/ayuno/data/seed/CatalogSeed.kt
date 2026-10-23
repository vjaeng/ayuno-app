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
    )

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
        insertExampleTemplate(db, context, now)
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
        t.rules.forEachIndexed { i, r ->
            insert(
                "rule",
                ContentValues().apply {
                    put("id", exampleId("rule/$i")); put("template_id", templateId); put("name", r.name); put("active", r.active)
                    put("condition_kind", r.conditionKind); put("metric_type_code", r.metricTypeCode); put("filter_stage_kinds", "[]")
                    put("filter_on_waking", false); put("level", r.level); put("message", r.message); put("action", r.action)
                },
            )
        }
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
