package com.ayuno.data.exchange

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.ayuno.data.db.AyunoDatabase
import com.ayuno.data.exchange.ExchangeFormat.Kind
import com.ayuno.domain.model.ExportRequest
import com.ayuno.domain.model.ImportCheck
import com.ayuno.domain.model.ImportCount
import com.ayuno.domain.model.ImportPreview
import com.ayuno.domain.model.ImportProblem
import com.ayuno.domain.model.ImportResult
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.port.DataExchange
import com.ayuno.domain.time.LocalTimes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private typealias Row = Map<String, JsonElement>

/** Columna de una tabla: nombre canónico, cómo se escribe en el archivo y si admite nulo. */
internal data class Column(val name: String, val kind: Kind, val notNull: Boolean)

/**
 * ADR-007 y ADR-SEG-006: serializa la base al formato `fasting-log` y lo importa todo o nada. Toda entrada es
 * hostil hasta validarse (S1): tipos, nulos, enumerados, formatos de hora, propietario del plan y referencias
 * (a filas del archivo o de la base). Nada se escribe hasta que el archivo entero es válido.
 */
class JsonDataExchange(private val database: AyunoDatabase) : DataExchange {

    private val json = Json { prettyPrint = true }

    private val db get() = database.openHelper.writableDatabase

    internal fun columns(table: ExchangeFormat.Table): List<Column> =
        db.query("PRAGMA table_info(`${table.table}`)").use { c ->
            buildList {
                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndexOrThrow("name"))
                    val affinity = c.getString(c.getColumnIndexOrThrow("type")).uppercase()
                    add(Column(name, table.kindOf(name, affinity), c.getInt(c.getColumnIndexOrThrow("notnull")) == 1))
                }
            }
        }

    // ---------- Exportar ----------

    private fun readAll(table: ExchangeFormat.Table): List<Row> {
        val cols = columns(table)
        return db.query("SELECT * FROM `${table.table}` ORDER BY rowid").use { c -> buildList { while (c.moveToNext()) add(readRow(c, cols)) } }
    }

    private fun readRow(c: Cursor, cols: List<Column>): Row = cols.associate { col ->
        val i = c.getColumnIndexOrThrow(col.name)
        col.name to if (c.isNull(i)) {
            JsonNull
        } else {
            when (col.kind) {
                Kind.BOOLEAN -> JsonPrimitive(c.getLong(i) != 0L)
                Kind.INTEGER -> JsonPrimitive(c.getLong(i))
                Kind.REAL -> JsonPrimitive(c.getDouble(i))
                Kind.STRING_LIST, Kind.NUMBER_LIST, Kind.INTEGER_LIST, Kind.OBJECT_LIST -> json.parseToJsonElement(c.getString(i))
                else -> JsonPrimitive(c.getString(i))
            }
        }
    }

    private fun Row.text(column: String): String? = (this[column] as? JsonPrimitive)?.contentOrNull

    override suspend fun export(request: ExportRequest): String = database.withTransaction {
        val all = ExchangeFormat.TABLES.associate { it.table to readAll(it) }
        val selected = request.fastId?.let { selectFast(all, it) } ?: all
        val person = request.person
        val document = buildJsonObject {
            put("schema", ExchangeFormat.SCHEMA)
            put("schema_version", ExchangeFormat.VERSION)
            put("app_version", request.appVersion)
            put("exported_at", LocalTimes.format(request.exportedAt))
            put("tz_offset", request.tzOffset)
            put(
                "person",
                buildJsonObject {
                    put("full_name", person.fullName)
                    put("id_number", person.idNumber)
                    put("birth_date", person.birthDate?.toString())
                },
            )
            put("catalogs", JsonObject(ExchangeFormat.TABLES.filter { it.catalog }.associate { it.key to JsonArray(selected.getValue(it.table).map(::JsonObject)) }))
            ExchangeFormat.TABLES.filterNot { it.catalog }.forEach { put(it.key, JsonArray(selected.getValue(it.table).map(::JsonObject))) }
        }
        json.encodeToString(JsonObject.serializer(), document)
    }

    /** Un ayuno: su fila, su plan copiado, la plantilla usada con su plan, sus registros, catálogos y bitácora. */
    private fun selectFast(all: Map<String, List<Row>>, fastId: String): Map<String, List<Row>> {
        val fast = all.getValue("fast").filter { it.text("id") == fastId }
        val templateId = fast.firstOrNull()?.text("template_id")
        fun ownedHere(row: Row) = row.text("fast_id") == fastId || (templateId != null && row.text("template_id") == templateId)
        val result = mutableMapOf<String, List<Row>>()
        ExchangeFormat.TABLES.forEach { t ->
            result[t.table] = when {
                t.catalog -> all.getValue(t.table)
                t.table == "fast" -> fast
                t.table == "template" -> all.getValue("template").filter { templateId != null && it.text("id") == templateId }
                t.planOwned || t.refs["fast_id"] == "fast" -> all.getValue(t.table).filter(::ownedHere)
                else -> emptyList()
            }
        }
        val planMetrics = result.getValue("plan_metric").mapNotNull { it.text("id") }.toSet()
        result["metric_schedule"] = all.getValue("metric_schedule").filter { it.text("plan_metric_id") in planMetrics }
        val steps = result.getValue("refeed_step").mapNotNull { it.text("id") }.toSet()
        result["refeed_check"] = all.getValue("refeed_check").filter { it.text("refeed_step_id") in steps }
        val exportedIds = result.values.flatten().mapNotNull { it.text("id") }.toSet()
        result["edit_log"] = all.getValue("edit_log").filter { it.text("row_id") in exportedIds }
        return result
    }

    // ---------- Tablas (H-037) ----------

    override fun tables(json: String): ByteArray {
        val document = this.json.parseToJsonElement(json).let { it as JsonObject }
        val catalogs = document["catalogs"] as? JsonObject ?: JsonObject(emptyMap())
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            ExchangeFormat.TABLES.forEach { table ->
                val rows = ((if (table.catalog) catalogs[table.key] else document[table.key]) as? JsonArray).orEmpty().map { it as JsonObject }
                val header = columns(table).map { it.name }
                zip.putNextEntry(ZipEntry("${table.key}.csv"))
                val csv = buildString {
                    append(header.joinToString(",")).append("\r\n")
                    rows.forEach { row -> append(header.joinToString(",") { csvCell(row[it]) }).append("\r\n") }
                }
                zip.write(csv.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** Vacío para lo no anotado (nunca 0); comillas solo si hacen falta (RFC 4180); listas como su JSON. */
    private fun csvCell(value: JsonElement?): String {
        val text = when (value) {
            null, JsonNull -> return ""
            is JsonPrimitive -> value.content
            else -> value.toString()
        }
        return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + text.replace("\"", "\"\"") + "\"" else text
    }

    // ---------- Importar ----------

    /** Archivo ya validado: filas por tabla y campos ignorados. */
    private class Parsed(val rows: Map<String, List<JsonObject>>, val ignored: List<String>)

    override suspend fun preview(content: ByteArray): ImportCheck<ImportPreview> = database.withTransaction {
        when (val parsed = parse(content)) {
            is ImportCheck.Rejected -> parsed
            is ImportCheck.Ok -> {
                val counts = ExchangeFormat.TABLES.mapNotNull { table ->
                    val rows = parsed.value.rows[table.table].orEmpty()
                    if (rows.isEmpty()) return@mapNotNull null
                    val existing = existingKeys(table)
                    table.key to ImportCount(rows.size, rows.count { (it[table.pk] as JsonPrimitive).content !in existing })
                }.toMap()
                ImportCheck.Ok(ImportPreview(counts, parsed.value.ignored))
            }
        }
    }

    override suspend fun import(content: ByteArray): ImportCheck<ImportResult> = database.withTransaction {
        when (val parsed = parse(content)) {
            is ImportCheck.Rejected -> parsed
            is ImportCheck.Ok -> {
                var created = 0
                var updated = 0
                var kept = 0
                ExchangeFormat.TABLES.forEach { table ->
                    val cols = columns(table)
                    parsed.value.rows[table.table].orEmpty().forEach { row ->
                        val key = (row[table.pk] as JsonPrimitive).content
                        val local = stampOf(table, key)
                        when {
                            local == null -> { insert(table, cols, row); created++ }
                            // Gana la fila con `updated_at` (o `created_at`) más reciente; empate = ya estaba igual.
                            stamp(row) > local -> { insert(table, cols, row); updated++ }
                            else -> kept++
                        }
                    }
                }
                ImportCheck.Ok(ImportResult(created, updated, kept, parsed.value.ignored))
            }
        }
    }

    private fun stamp(row: JsonObject): String = (row["updated_at"] as? JsonPrimitive)?.contentOrNull ?: (row["created_at"] as JsonPrimitive).content

    private fun stampOf(table: ExchangeFormat.Table, key: String): String? =
        db.query("SELECT COALESCE(updated_at, created_at) FROM `${table.table}` WHERE `${table.pk}` = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun existingKeys(table: ExchangeFormat.Table): Set<String> =
        db.query("SELECT `${table.pk}` FROM `${table.table}`").use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }

    private fun insert(table: ExchangeFormat.Table, cols: List<Column>, row: JsonObject) {
        val values = ContentValues()
        cols.forEach { col ->
            val v = row[col.name]
            if (v == null || v is JsonNull) {
                values.putNull(col.name)
                return@forEach
            }
            when (col.kind) {
                Kind.BOOLEAN -> values.put(col.name, if ((v as JsonPrimitive).booleanOrNull == true) 1 else 0)
                Kind.INTEGER -> values.put(col.name, (v as JsonPrimitive).longOrNull)
                Kind.REAL -> values.put(col.name, (v as JsonPrimitive).doubleOrNull)
                // Mismo texto que escriben los repositorios (`[12.0,24.0]`, `["fast"]`).
                Kind.NUMBER_LIST -> values.put(col.name, (v as JsonArray).joinToString(",", "[", "]") { (it as JsonPrimitive).content.toDouble().toString() })
                Kind.STRING_LIST, Kind.OBJECT_LIST -> values.put(col.name, JsonArray(v as JsonArray).toString())
                Kind.INTEGER_LIST -> values.put(col.name, (v as JsonArray).joinToString(",", "[", "]") { (it as JsonPrimitive).content })
                else -> values.put(col.name, (v as JsonPrimitive).content)
            }
        }
        db.insert(table.table, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    private fun parse(content: ByteArray): ImportCheck<Parsed> {
        val text = content.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return reject(ImportProblem.NotOurFile)
        if ((root["schema"] as? JsonPrimitive)?.contentOrNull != ExchangeFormat.SCHEMA) return reject(ImportProblem.NotOurFile)
        val version = (root["schema_version"] as? JsonPrimitive)?.contentOrNull ?: return reject(ImportProblem.NotOurFile)
        val major = version.substringBefore('.').toIntOrNull() ?: return reject(ImportProblem.NotOurFile)
        if (major > ExchangeFormat.MAJOR) return reject(ImportProblem.NewerVersion(version))
        // EV-006: un 1.0 o 1.1 no trae los campos de 1.2; toman su valor por defecto antes de validar.
        val beforeV12 = major == 1 && (version.substringAfter('.', "0").toIntOrNull() ?: 0) < 2

        val ignored = mutableListOf<String>()
        val listKeys = ExchangeFormat.TABLES.filterNot { it.catalog }.map { it.key }.toSet()
        root.keys.filter { it !in ExchangeFormat.HEADER && it !in listKeys }.forEach { ignored += it }
        val catalogs = root["catalogs"] as? JsonObject ?: JsonObject(emptyMap())
        val catalogKeys = ExchangeFormat.TABLES.filter { it.catalog }.map { it.key }.toSet()
        catalogs.keys.filter { it !in catalogKeys }.forEach { ignored += "catalogs.$it" }

        val rows = mutableMapOf<String, List<JsonObject>>()
        for (table in ExchangeFormat.TABLES) {
            val element = (if (table.catalog) catalogs[table.key] else root[table.key]) ?: continue
            val array = element as? JsonArray ?: return reject(ImportProblem.InvalidRow(table.key, 0, table.key))
            val cols = columns(table)
            val names = cols.map { it.name }.toSet()
            val list = mutableListOf<JsonObject>()
            array.forEachIndexed { i, item ->
                val raw = item as? JsonObject ?: return reject(ImportProblem.InvalidRow(table.key, i + 1, table.key))
                val row = if (beforeV12) withDefaults(table, cols, raw) else raw
                row.keys.filter { it !in names }.forEach { ignored += "${table.key}.$it" }
                cols.firstOrNull { !valid(table, it, row[it.name]) }?.let { return reject(ImportProblem.InvalidRow(table.key, i + 1, it.name)) }
                if (table.planOwned && (row.isNull("template_id") == row.isNull("fast_id"))) return reject(ImportProblem.InvalidRow(table.key, i + 1, "template_id"))
                list += row
            }
            rows[table.table] = list
        }
        if (rows.values.all { it.isEmpty() }) return reject(ImportProblem.Empty)

        // Referencias: a filas del propio archivo o ya guardadas en el teléfono.
        val fileKeys = ExchangeFormat.TABLES.associate { t -> t.table to rows[t.table].orEmpty().map { (it[t.pk] as JsonPrimitive).content }.toSet() }
        val localKeys = mutableMapOf<String, Set<String>>()
        for (table in ExchangeFormat.TABLES) {
            rows[table.table].orEmpty().forEachIndexed { i, row ->
                table.refs.forEach { (column, target) ->
                    val value = (row[column] as? JsonPrimitive)?.contentOrNull ?: return@forEach
                    val targetTable = ExchangeFormat.BY_TABLE.getValue(target)
                    val known = value in fileKeys.getValue(target) || value in localKeys.getOrPut(target) { existingKeys(targetTable) }
                    if (!known) return reject(ImportProblem.InvalidRow(table.key, i + 1, column))
                }
            }
        }
        metricOptionProblem(rows)?.let { return reject(it) }
        ev006Problem(rows)?.let { return reject(it) }
        return ImportCheck.Ok(Parsed(rows, ignored.distinct()))
    }

    /**
     * H-049 (S1): las palabras que quedarían tras importar (gana la fila más reciente, como al escribir) deben ser de un
     * tipo numérico, caber en su rango y no repetir texto ni número en su tipo, como mucho [MetricType.MAX_OPTIONS].
     */
    private fun metricOptionProblem(rows: Map<String, List<JsonObject>>): ImportProblem? {
        val fileOptions = rows["metric_option"].orEmpty()
        if (fileOptions.isEmpty()) return null
        fun JsonObject.str(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
        fun JsonObject.num(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull
        data class Type(val numeric: Boolean, val min: Double?, val max: Double?)
        val types = db.query("SELECT code, is_numeric, min_valid, max_valid FROM metric_type").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), Type(c.getInt(1) == 1, c.getDoubleOrNull(2), c.getDoubleOrNull(3))) }
        }.toMutableMap()
        rows["metric_type"].orEmpty().forEach { t ->
            types[t.str("code")!!] = Type((t["is_numeric"] as? JsonPrimitive)?.booleanOrNull == true, t.num("min_valid"), t.num("max_valid"))
        }
        data class Option(val id: String, val type: String, val label: String, val value: Double, val deleted: Boolean, val stamp: String, val index: Int)
        val merged = db.query("SELECT id, metric_type_code, label, value, deleted_at, COALESCE(updated_at, created_at) FROM metric_option").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), Option(c.getString(0), c.getString(1), c.getString(2), c.getDouble(3), !c.isNull(4), c.getString(5), 0)) }
        }.toMutableMap()
        fileOptions.forEachIndexed { i, o ->
            val option = Option(o.str("id")!!, o.str("metric_type_code")!!, o.str("label")!!, o.num("value")!!, !o.isNull("deleted_at"), stamp(o), i + 1)
            val local = merged[option.id]
            if (local == null || option.stamp > local.stamp) merged[option.id] = option
        }
        val list = "metric_options"
        merged.values.filter { it.index > 0 }.forEach { o ->
            val type = types[o.type] ?: return ImportProblem.InvalidRow(list, o.index, "metric_type_code")
            if (o.label.isBlank()) return ImportProblem.InvalidRow(list, o.index, "label")
            if (!type.numeric) return ImportProblem.InvalidRow(list, o.index, "metric_type_code")
            if ((type.min != null && o.value < type.min) || (type.max != null && o.value > type.max)) return ImportProblem.InvalidRow(list, o.index, "value")
        }
        merged.values.filterNot { it.deleted }.groupBy { it.type }.values.forEach { group ->
            val first = group.filter { it.index > 0 }.minByOrNull { it.index } ?: return@forEach
            if (group.size > MetricType.MAX_OPTIONS) return ImportProblem.InvalidRow(list, first.index, "metric_type_code")
            group.groupBy { it.label.trim().lowercase() }.values.firstOrNull { it.size > 1 }?.let { dup ->
                return ImportProblem.InvalidRow(list, dup.filter { it.index > 0 }.minOfOrNull { it.index } ?: first.index, "label")
            }
            group.groupBy { it.value }.values.firstOrNull { it.size > 1 }?.let { dup ->
                return ImportProblem.InvalidRow(list, dup.filter { it.index > 0 }.minOfOrNull { it.index } ?: first.index, "value")
            }
        }
        return null
    }

    private fun withDefaults(table: ExchangeFormat.Table, cols: List<Column>, row: JsonObject): JsonObject {
        val missing = cols.filter { it.name !in row }.mapNotNull { col -> ExchangeFormat.defaultFor(table.table, col.name, row)?.let { col.name to it } }
        return if (missing.isEmpty()) row else JsonObject(row + missing)
    }

    /**
     * EV-006 (S1, importar.md): medidas con equivalencia > 0 y `code` único por tipo; lo tecleado de cada ingesta en la
     * unidad del tipo o una medida suya, con cantidades > 0; botón rápido de una medida del tipo y nunca en uno que rompe
     * el ayuno; reglas nuevas con su dirección, racha ≥ 3, tendencia de la diferencia solo en presión y porcentaje 1-100;
     * segundos de pie ≥ 0 solo en una de pie enlazada a una sentada del mismo ayuno; como mucho una «de partida» por
     * ayuno, medición y posición.
     */
    private fun ev006Problem(rows: Map<String, List<JsonObject>>): ImportProblem? {
        fun JsonObject.str(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
        fun JsonObject.num(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull
        fun JsonObject.flag(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull == true
        fun JsonObject.alive() = isNull("deleted_at")

        // Tipos y medidas que quedarían: los del teléfono con los del archivo encima.
        val typeUnit = db.query("SELECT code, unit, breaks_fast FROM intake_type").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1) to (c.getInt(2) == 1)) }
        }.toMutableMap()
        rows["intake_type"].orEmpty().forEach { typeUnit[it.str("code")!!] = it.str("unit")!! to it.flag("breaks_fast") }
        data class Unit(val type: String, val code: String, val alive: Boolean)
        val units = db.query("SELECT id, intake_type_code, code, deleted_at FROM household_unit").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), Unit(c.getString(1), c.getString(2), c.isNull(3))) }
        }.toMutableMap()
        rows["household_unit"].orEmpty().forEachIndexed { i, u ->
            if ((u.num("equivalent") ?: 0.0) <= 0) return ImportProblem.InvalidRow("household_units", i + 1, "equivalent")
            if (u.str("code").isNullOrBlank()) return ImportProblem.InvalidRow("household_units", i + 1, "code")
            units[u.str("id")!!] = Unit(u.str("intake_type_code")!!, u.str("code")!!, u.alive())
        }
        val aliveCodes = units.values.filter { it.alive }.groupBy { it.type }
        aliveCodes.values.forEach { list ->
            list.groupBy { it.code }.values.firstOrNull { it.size > 1 }?.let { dup ->
                val index = rows["household_unit"].orEmpty().indexOfFirst { it.str("code") == dup.first().code && it.str("intake_type_code") == dup.first().type }
                return ImportProblem.InvalidRow("household_units", index.coerceAtLeast(0) + 1, "code")
            }
        }
        fun hasUnit(type: String, code: String) = aliveCodes[type].orEmpty().any { it.code == code }
        rows["intake_type"].orEmpty().forEachIndexed { i, t ->
            val quick = t.str("quick_unit_code") ?: return@forEachIndexed
            if (t.flag("breaks_fast") || !hasUnit(t.str("code")!!, quick)) return ImportProblem.InvalidRow("intake_types", i + 1, "quick_unit_code")
        }
        rows["intake"].orEmpty().forEachIndexed { i, r ->
            if ((r.num("amount") ?: 0.0) <= 0) return ImportProblem.InvalidRow("intakes", i + 1, "amount")
            if ((r.num("amount_entered") ?: 0.0) <= 0) return ImportProblem.InvalidRow("intakes", i + 1, "amount_entered")
            val type = r.str("intake_type_code")!!
            val entered = r.str("unit_entered")!!
            val unit = typeUnit[type]?.first
            // Unidades antiguas convertibles (p. ej. L frente a mL) siguen valiendo, como al anotar.
            val ok = entered == unit || hasUnit(type, entered) || (unit != null && com.ayuno.domain.units.UnitConversions.canConvert(entered, unit))
            if (!ok) return ImportProblem.InvalidRow("intakes", i + 1, "unit_entered")
        }
        val between = setOf("pair_difference", "change_from_baseline", "trend")
        rows["rule"].orEmpty().forEachIndexed { i, r ->
            val kind = r.str("condition_kind")
            if (kind in between && r.str("direction") == null) return ImportProblem.InvalidRow("rules", i + 1, "direction")
            if (kind == "trend" && (r.num("trend_count") ?: 0.0) < com.ayuno.domain.model.Rule.MIN_TREND_COUNT) return ImportProblem.InvalidRow("rules", i + 1, "trend_count")
            if (r.str("trend_of") == "pair_difference" && r.str("metric_type_code") != MetricType.BLOOD_PRESSURE_CODE) return ImportProblem.InvalidRow("rules", i + 1, "trend_of")
            r.num("goal_percent")?.let { if (it < 1 || it > 100) return ImportProblem.InvalidRow("rules", i + 1, "goal_percent") }
        }
        rows["plan_metric"].orEmpty().forEachIndexed { i, m ->
            val seconds = (m["standing_check_seconds"] as? JsonArray)?.map { (it as JsonPrimitive).content.toLong() } ?: return@forEachIndexed
            if (m.str("metric_type_code") != MetricType.BLOOD_PRESSURE_CODE || seconds.any { it <= 0 } || seconds != seconds.distinct().sorted()) {
                return ImportProblem.InvalidRow("plan_metrics", i + 1, "standing_check_seconds")
            }
        }
        val readings = rows["blood_pressure_reading"].orEmpty()
        val localReadings = db.query("SELECT id, fast_id, position FROM blood_pressure_reading").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1) to c.getString(2)) }
        }.toMutableMap()
        readings.forEach { localReadings[it.str("id")!!] = it.str("fast_id")!! to it.str("position")!! }
        readings.forEachIndexed { i, r ->
            val seconds = r.num("seconds_standing") ?: return@forEachIndexed
            val paired = r.str("paired_reading_id")?.let { localReadings[it] }
            if (seconds < 0 || r.str("position") != "standing" || paired == null || paired.first != r.str("fast_id") || paired.second != "sitting") {
                return ImportProblem.InvalidRow("blood_pressure_readings", i + 1, "seconds_standing")
            }
        }
        fun duplicatedBaseline(list: List<JsonObject>, group: (JsonObject) -> String?): Int? =
            list.withIndex().filter { (_, r) -> r.flag("is_baseline") && r.alive() }.groupBy { (_, r) -> r.str("fast_id") to group(r) }
                .values.firstOrNull { it.size > 1 }?.get(1)?.index
        duplicatedBaseline(rows["measurement"].orEmpty()) { it.str("metric_type_code") }?.let { return ImportProblem.InvalidRow("measurements", it + 1, "is_baseline") }
        duplicatedBaseline(readings) { it.str("position") }?.let { return ImportProblem.InvalidRow("blood_pressure_readings", it + 1, "is_baseline") }
        return null
    }

    private fun Cursor.getDoubleOrNull(i: Int): Double? = if (isNull(i)) null else getDouble(i)

    private fun JsonObject.isNull(key: String) = this[key] == null || this[key] is JsonNull

    private fun reject(problem: ImportProblem) = ImportCheck.Rejected(problem)

    private fun valid(table: ExchangeFormat.Table, column: Column, value: JsonElement?): Boolean {
        if (value == null || value is JsonNull) return !column.notNull
        val allowed = table.enums[column.name]
        return when (column.kind) {
            Kind.BOOLEAN -> value is JsonPrimitive && !value.isString && value.booleanOrNull != null
            Kind.INTEGER -> value is JsonPrimitive && !value.isString && value.longOrNull != null
            Kind.REAL -> value is JsonPrimitive && !value.isString && value.doubleOrNull != null
            Kind.NUMBER_LIST -> value is JsonArray && value.all { it is JsonPrimitive && !it.isString && it.doubleOrNull != null }
            Kind.STRING_LIST -> value is JsonArray && value.all { it is JsonPrimitive && it.isString && (allowed == null || it.content in allowed) }
            Kind.INTEGER_LIST -> value is JsonArray && value.all { it is JsonPrimitive && !it.isString && it.longOrNull != null }
            Kind.OBJECT_LIST -> value is JsonArray && value.isNotEmpty() && value.all { validWarning(it) }
            Kind.DATETIME -> value.isStringMatching(ExchangeFormat.DATETIME_PATTERN) && runCatching { LocalTimes.parse(value.jsonPrimitive()) }.isSuccess
            Kind.DATE -> value.isStringMatching(ExchangeFormat.DATE_PATTERN) && runCatching { LocalDate.parse(value.jsonPrimitive()) }.isSuccess
            Kind.TIME -> value.isStringMatching(ExchangeFormat.TIME_PATTERN) && runCatching { LocalTime.parse(value.jsonPrimitive()) }.isSuccess
            Kind.TEXT -> value is JsonPrimitive && value.isString && (allowed == null || value.content in allowed)
        }
    }

    /** D-25: `{kind, ref_id, text}` con `kind` conocido y `text` no vacío. */
    private fun validWarning(item: JsonElement): Boolean {
        val o = item as? JsonObject ?: return false
        if (o.keys != setOf("kind", "ref_id", "text")) return false
        val kind = (o["kind"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val ref = (o["ref_id"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val text = (o["text"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return kind in ExchangeFormat.WARNING_KINDS && !ref.isNullOrBlank() && !text.isNullOrBlank()
    }

    private fun JsonElement.isStringMatching(pattern: Regex) = this is JsonPrimitive && isString && pattern.matches(content)
    private fun JsonElement.jsonPrimitive() = (this as JsonPrimitive).content
}
