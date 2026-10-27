package com.ayuno.data.exchange

import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.Coded
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Trend
import com.ayuno.domain.model.TrendOf
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.WarningKind
import com.ayuno.domain.model.UserChoice

/**
 * Formato de intercambio `fasting-log` (ADR-007). Cada lista del archivo es una tabla de la base con sus columnas
 * canónicas (las da `PRAGMA table_info`); aquí se declara lo que la base no sabe: qué columnas son booleanas,
 * listas, enumerados o referencias, y quién es el propietario del plan. Exportar, validar al importar, las tablas
 * CSV y el esquema publicado salen de esta misma descripción.
 */
internal object ExchangeFormat {
    const val SCHEMA = "fasting-log"
    const val VERSION = "1.2"
    const val MAJOR = 1

    /** Columnas de fecha y hora local sin zona, con segundos (`2026-10-23T07:02:15`). */
    private val DATETIME = setOf("last_meal_planned", "last_meal_actual", "clock_start_planned", "clock_start_actual", "end_actual", "scheduled_for")

    /** [OBJECT_LIST]: lista de objetos con su propio formato (`fast.started_with_warnings`, D-25). */
    enum class Kind { TEXT, INTEGER, REAL, BOOLEAN, DATETIME, DATE, TIME, STRING_LIST, NUMBER_LIST, INTEGER_LIST, OBJECT_LIST }

    data class Table(
        val table: String,
        /** Nombre de la lista en el archivo. */
        val key: String,
        val catalog: Boolean = false,
        val pk: String = "id",
        val booleans: Set<String> = emptySet(),
        val stringLists: Set<String> = emptySet(),
        val numberLists: Set<String> = emptySet(),
        val integerLists: Set<String> = emptySet(),
        val objectLists: Set<String> = emptySet(),
        /** Valores admitidos (en las listas, de cada elemento). */
        val enums: Map<String, Set<String>> = emptyMap(),
        /** Columna → tabla a la que apunta (por su clave primaria). */
        val refs: Map<String, String> = emptyMap(),
        /** Entidad de plan: exactamente uno de `template_id` / `fast_id` (D-03). */
        val planOwned: Boolean = false,
    ) {
        fun kindOf(column: String, affinity: String): Kind = when {
            column in booleans -> Kind.BOOLEAN
            column in stringLists -> Kind.STRING_LIST
            column in numberLists -> Kind.NUMBER_LIST
            column in integerLists -> Kind.INTEGER_LIST
            column in objectLists -> Kind.OBJECT_LIST
            column in DATETIME || column.endsWith("_at") -> Kind.DATETIME
            column == "birth_date" -> Kind.DATE
            column == "time_of_day" || column == "check_time" -> Kind.TIME
            affinity == "INTEGER" -> Kind.INTEGER
            affinity == "REAL" -> Kind.REAL
            else -> Kind.TEXT
        }
    }

    private inline fun <reified T> codes(): Set<String> where T : Enum<T>, T : Coded = enumValues<T>().map { it.code }.toSet()

    private val PLAN_OWNER = mapOf("template_id" to "template", "fast_id" to "fast")

    val TABLES: List<Table> = listOf(
        Table("metric_type", "metric_types", catalog = true, pk = "code", booleans = setOf("is_numeric", "active")),
        // 1.1 (EV-005): palabras de las mediciones; un archivo 1.0 no las trae.
        Table("metric_option", "metric_options", catalog = true, refs = mapOf("metric_type_code" to "metric_type")),
        Table("symptom_type", "symptom_types", catalog = true, pk = "code", booleans = setOf("is_alarm", "active")),
        Table("intake_type", "intake_types", catalog = true, pk = "code", booleans = setOf("breaks_fast")),
        // 1.2 (EV-006): medidas caseras; `intake_type.quick_unit_code` e `intake.unit_entered` apuntan a su `code` por tipo.
        Table("household_unit", "household_units", catalog = true, booleans = setOf("imprecise"), refs = mapOf("intake_type_code" to "intake_type")),
        Table("product", "products", catalog = true, pk = "code", booleans = setOf("avoid", "active"), enums = mapOf("kind" to codes<ProductKind>())),
        Table("product_schedule", "product_schedules", catalog = true, refs = mapOf("product_code" to "product")),
        Table("template", "templates", numberLists = setOf("milestone_hours")),
        Table(
            "fast", "fasts", numberLists = setOf("milestone_hours"), objectLists = setOf("started_with_warnings"),
            enums = mapOf("status" to codes<FastStatus>(), "end_reason" to codes<EndReason>()),
            refs = mapOf("template_id" to "template", "end_rule_id" to "rule"),
        ),
        Table(
            "stage", "stages", planOwned = true, refs = PLAN_OWNER,
            enums = mapOf("kind" to codes<StageKind>(), "duration_unit" to codes<DurationUnit>()),
        ),
        Table("plan_metric", "plan_metrics", planOwned = true, integerLists = setOf("standing_check_seconds"), refs = PLAN_OWNER + ("metric_type_code" to "metric_type")),
        Table("metric_schedule", "metric_schedules", enums = mapOf("stage_kind" to codes<StageKind>()), refs = mapOf("plan_metric_id" to "plan_metric")),
        Table(
            "rule", "rules", planOwned = true, booleans = setOf("active", "filter_on_waking", "is_placeholder"), stringLists = setOf("filter_stage_kinds"),
            enums = mapOf(
                "direction" to codes<RuleDirection>(), "trend_of" to codes<TrendOf>(),
                "condition_kind" to codes<RuleConditionKind>(), "bp_component" to codes<BpComponent>(), "filter_stage_kinds" to codes<StageKind>(),
                "filter_position" to codes<Position>(), "level" to codes<RuleLevel>(), "action" to codes<RuleAction>(),
            ),
            refs = PLAN_OWNER + mapOf("metric_type_code" to "metric_type", "symptom_type_code" to "symptom_type", "intake_type_code" to "intake_type"),
        ),
        Table("intake_goal", "intake_goals", planOwned = true, refs = PLAN_OWNER + ("intake_type_code" to "intake_type")),
        Table("checklist_item", "checklist_items", planOwned = true, booleans = setOf("is_checked"), refs = PLAN_OWNER),
        Table("stage_notice", "stage_notices", planOwned = true, stringLists = setOf("stage_kinds"), enums = mapOf("stage_kinds" to codes<StageKind>()), refs = PLAN_OWNER),
        Table(
            "plan_product", "plan_products", planOwned = true, booleans = setOf("optional_enabled"),
            enums = mapOf("during_fast" to codes<DuringFast>()), refs = PLAN_OWNER + ("product_code" to "product"),
        ),
        Table("refeed_step", "refeed_steps", planOwned = true, enums = mapOf("kind" to codes<RefeedStepKind>()), refs = PLAN_OWNER),
        Table(
            "measurement", "measurements", booleans = setOf("on_waking", "is_baseline"),
            enums = mapOf("source" to codes<GlucoseSource>(), "trend" to codes<Trend>()),
            refs = mapOf("fast_id" to "fast", "metric_type_code" to "metric_type"),
        ),
        Table(
            "blood_pressure_reading", "blood_pressure_readings", booleans = setOf("on_waking", "is_baseline"),
            enums = mapOf("position" to codes<Position>()), refs = mapOf("fast_id" to "fast", "paired_reading_id" to "blood_pressure_reading"),
        ),
        Table("symptom", "symptoms", refs = mapOf("fast_id" to "fast", "symptom_type_code" to "symptom_type")),
        Table("intake", "intakes", refs = mapOf("fast_id" to "fast", "intake_type_code" to "intake_type")),
        Table("product_log", "product_logs", enums = mapOf("status" to codes<ProductLogStatus>()), refs = mapOf("fast_id" to "fast", "product_code" to "product")),
        Table("alert", "alerts", stringLists = setOf("trigger_entry_ids"), enums = mapOf("user_choice" to codes<UserChoice>()), refs = mapOf("fast_id" to "fast", "rule_id" to "rule")),
        Table("note", "notes", refs = mapOf("fast_id" to "fast")),
        Table("refeed_check", "refeed_checks", refs = mapOf("refeed_step_id" to "refeed_step")),
        Table("meal", "meals", enums = mapOf("tolerance" to codes<MealTolerance>()), refs = mapOf("fast_id" to "fast")),
        Table("edit_log", "edit_logs", enums = mapOf("action" to codes<EditAction>())),
    )

    val BY_TABLE: Map<String, Table> = TABLES.associateBy { it.table }

    /** `fast.started_with_warnings`: claves de cada elemento y valores de `kind`. */
    val WARNING_KINDS: Set<String> = codes<WarningKind>()

    /**
     * Valores de los campos de 1.2 en un archivo 1.0 o 1.1 (importar.md): lo tecleado de una ingesta es su cantidad y
     * unidad; las marcas, false; los tiempos de pie de la presión, los de fábrica; el resto, nulo.
     */
    fun defaultFor(table: String, column: String, row: Map<String, kotlinx.serialization.json.JsonElement>): kotlinx.serialization.json.JsonElement? = when ("$table.$column") {
        "intake.amount_entered" -> row["amount"]
        "intake.unit_entered" -> row["unit"]
        "rule.is_placeholder", "measurement.is_baseline", "blood_pressure_reading.is_baseline" -> kotlinx.serialization.json.JsonPrimitive(false)
        "plan_metric.standing_check_seconds" ->
            if ((row["metric_type_code"] as? kotlinx.serialization.json.JsonPrimitive)?.content == com.ayuno.domain.model.MetricType.BLOOD_PRESSURE_CODE) {
                kotlinx.serialization.json.JsonArray(com.ayuno.domain.model.PlanMetric.DEFAULT_STANDING_SECONDS.map { kotlinx.serialization.json.JsonPrimitive(it) })
            } else {
                null
            }
        else -> null
    }

    /** Claves de la cabecera (`person` lleva los campos canónicos de `person_profile`). */
    val HEADER = listOf("schema", "schema_version", "app_version", "exported_at", "tz_offset", "person", "catalogs")
    val PERSON_FIELDS = listOf("full_name", "id_number", "birth_date")

    val DATETIME_PATTERN = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")
    val DATE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")
    val TIME_PATTERN = Regex("""\d{2}:\d{2}""")
}
