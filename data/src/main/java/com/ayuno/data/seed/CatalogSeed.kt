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
        insertCatalogs(db, context, now)
        insertExampleTemplate(db, context, now)
    }

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
        t.metrics.forEach { m ->
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
