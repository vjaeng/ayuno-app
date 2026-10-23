package com.ayuno.data.exchange

import com.ayuno.data.exchange.ExchangeFormat.Kind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Genera `schema/fasting-log-<versión>.schema.json` (JSON Schema draft 2020-12) desde [ExchangeFormat]. */
internal object SchemaGenerator {
    private const val DATETIME = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"
    private const val DATE = "^\\d{4}-\\d{2}-\\d{2}$"
    private const val TIME = "^\\d{2}:\\d{2}$"

    fun generate(columns: (ExchangeFormat.Table) -> List<Column>): JsonObject = buildJsonObject {
        put("\$schema", "https://json-schema.org/draft/2020-12/schema")
        put("title", "fasting-log ${ExchangeFormat.VERSION}")
        put("description", "Registro de ayunos exportado por la app (ADR-007). null = no anotado; horas locales sin zona; desfase en tz_offset.")
        put("type", "object")
        val lists = ExchangeFormat.TABLES.filterNot { it.catalog }
        val catalogs = ExchangeFormat.TABLES.filter { it.catalog }
        put("required", strings(ExchangeFormat.HEADER + lists.map { it.key }))
        put("additionalProperties", false)
        put(
            "properties",
            buildJsonObject {
                put("schema", buildJsonObject { put("const", ExchangeFormat.SCHEMA) })
                put("schema_version", buildJsonObject { put("const", ExchangeFormat.VERSION) })
                put("app_version", buildJsonObject { put("type", "string") })
                put("exported_at", JsonObject(type("string", true) + ("pattern" to JsonPrimitive(DATETIME))))
                put("tz_offset", JsonObject(type("string", true) + ("pattern" to JsonPrimitive("^[+-]\\d{2}:\\d{2}$"))))
                put(
                    "person",
                    objectOf(
                        ExchangeFormat.PERSON_FIELDS,
                        mapOf(
                            "full_name" to JsonObject(type("string", false)),
                            "id_number" to JsonObject(type("string", false)),
                            "birth_date" to JsonObject(type("string", false) + ("pattern" to JsonPrimitive(DATE))),
                        ),
                    ),
                )
                put("catalogs", objectOf(catalogs.map { it.key }, catalogs.associate { it.key to arrayOf(it) }))
                lists.forEach { put(it.key, arrayOf(it)) }
            },
        )
        put("\$defs", JsonObject(ExchangeFormat.TABLES.associate { t -> t.table to objectOf(columns(t).map { it.name }, columns(t).associate { it.name to property(t, it) }) }))
    }

    private fun strings(values: List<String>) = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }

    private fun objectOf(required: List<String>, properties: Map<String, JsonElement>) = buildJsonObject {
        put("type", "object")
        put("required", strings(required))
        put("additionalProperties", false)
        put("properties", JsonObject(properties))
    }

    private fun arrayOf(table: ExchangeFormat.Table): JsonElement = buildJsonObject {
        put("type", "array")
        put("items", buildJsonObject { put("\$ref", "#/\$defs/${table.table}") })
    }

    private fun type(name: String, notNull: Boolean): Map<String, JsonElement> =
        mapOf("type" to if (notNull) JsonPrimitive(name) else buildJsonArray { add(JsonPrimitive(name)); add(JsonPrimitive("null")) })

    private fun property(table: ExchangeFormat.Table, column: Column): JsonObject {
        val allowed = table.enums[column.name]?.sorted()
        val notNull = column.notNull
        return JsonObject(
            when (column.kind) {
                Kind.BOOLEAN -> type("boolean", notNull)
                Kind.INTEGER -> type("integer", notNull)
                Kind.REAL -> type("number", notNull)
                Kind.DATETIME -> type("string", notNull) + ("pattern" to JsonPrimitive(DATETIME))
                Kind.DATE -> type("string", notNull) + ("pattern" to JsonPrimitive(DATE))
                Kind.TIME -> type("string", notNull) + ("pattern" to JsonPrimitive(TIME))
                Kind.NUMBER_LIST -> type("array", notNull) + ("items" to buildJsonObject { put("type", "number") })
                Kind.STRING_LIST -> type("array", notNull) + (
                    "items" to buildJsonObject {
                        put("type", "string")
                        allowed?.let { put("enum", strings(it)) }
                    }
                    )
                Kind.TEXT -> if (allowed != null) {
                    mapOf("enum" to JsonArray(allowed.map { JsonPrimitive(it) } + if (notNull) emptyList() else listOf(JsonNull)))
                } else {
                    type("string", notNull)
                }
            },
        )
    }
}
