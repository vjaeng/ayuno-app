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
    }

    private fun insertCatalogs(db: SupportSQLiteDatabase, context: Context, now: String) {
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
