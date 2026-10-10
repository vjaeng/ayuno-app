package com.ayuno.data.exchange

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.data.repository.DataWorld
import com.ayuno.data.repository.RoomPersonProfileRepository
import com.ayuno.data.repository.RoomSettingsRepository
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.ImportCheck
import com.ayuno.domain.model.ImportProblem
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.PersonProfile
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.StageNotice
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.ExportData
import com.ayuno.domain.usecase.ExportNames
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordIntake
import com.ayuno.domain.usecase.RecordMeal
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordNote
import com.ayuno.domain.usecase.RecordProductLog
import com.ayuno.domain.usecase.RecordSymptom
import com.ayuno.domain.usecase.SavePersonProfile
import com.ayuno.domain.usecase.SaveProduct
import com.ayuno.domain.usecase.SetRefeedCheck
import com.ayuno.domain.usecase.TriggerManualAlert
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.zip.ZipInputStream

/** H-036 a H-040: formato de intercambio, esquema publicado e importación todo o nada. */
@RunWith(AndroidJUnit4::class)
class ExchangeTest {

    private val worlds = mutableListOf<DataWorld>()
    private fun world() = DataWorld().also { worlds += it }

    @After
    fun tearDown() = worlds.forEach { it.database.close() }

    private val schemaDir = File(System.getProperty("schemaDir") ?: "../schema")
    private val schemaFile get() = File(schemaDir, "fasting-log-1.0.schema.json")
    private val exampleFile get() = File(schemaDir, "fasting-log-1.0.example.json")

    private class Seeded(val fastId: String, val templateId: String)

    /** Un ayuno completo creado con los casos de uso reales: plan entero, todos los tipos de entrada, alerta, fin, vuelta a comer y bitácora. */
    private suspend fun DataWorld.seed(): Seeded {
        SaveProduct(catalog)(SaveProduct.Input(null, null, "Magnesio", ProductKind.SUPPLEMENT, 400.0, "mg", false, listOf(LocalTime.of(20, 0) to 400.0)))
        val rule = Rule(
            Ids.newId(), "Glucosa baja", true, RuleConditionKind.METRIC_BELOW, "glucose", null, 55.0, 20, null, null, null,
            listOf(StageKind.FAST), false, null, RuleLevel.ALERT, "Rompe con caldo tibio con sal.", RuleAction.OFFER_END, null,
        )
        val plan = Plan(
            stages = listOf(Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS), Stage(Ids.newId(), StageKind.POST, 2.0, DurationUnit.DAYS)),
            metrics = listOf(PlanMetric(Ids.newId(), "glucose", listOf(MetricSchedule(Ids.newId(), StageKind.FAST, LocalTime.of(7, 0))))),
            rules = listOf(rule),
            goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g")),
            checklist = listOf(ChecklistItem(Ids.newId(), "Comprar sal", false)),
            notices = listOf(StageNotice(Ids.newId(), "No sacarse sangre", listOf(StageKind.FAST, StageKind.POST))),
            products = listOf(PlanProduct(Ids.newId(), "magnesio", DuringFast.CONTINUES, false, null)),
            refeed = listOf(RefeedStep(Ids.newId(), RefeedStepKind.TIMED, "Una taza de caldo", 0, 60, 120, null)),
        )
        val template = TemplateWithPlan(Template(Ids.newId(), "Ayuno de 57 horas", 57.0, listOf(12.0, 24.0)), plan)
        saveTemplate(template)
        val fastId = (createFast(CreateFast.Input(template.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, "Semana tranquila")) as Outcome.Ok).value.fast.id
        now = Instant.parse("2026-10-23T03:10:00Z")
        startFast(fastId)
        now = Instant.parse("2026-10-23T03:15:00Z")
        correctClockStart(fastId, LocalDateTime.of(2026, 10, 22, 22, 5), "Lo puse tarde")
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        now = Instant.parse("2026-10-23T12:02:15Z")
        recordMeasurement(RecordMeasurement.Input(fastId, "glucose", at7.plusMinutes(2).plusSeconds(15), at7, 72.0, "mg/dL", source = GlucoseSource.CGM))
        recordBloodPressure(RecordBloodPressure.Input(fastId, at7.plusMinutes(10), systolic = 118, diastolic = 76, pulse = 60, position = Position.SITTING))
        RecordSymptom(catalog, fasts, records, alerts, tx, clock)(RecordSymptom.Input(fastId, "headache", at7.plusHours(5), 4, null))
        RecordIntake(catalog, fasts, records, alerts, tx, clock)(RecordIntake.Input(fastId, "salt", at7.plusHours(2), 2.5, null, null))
        val at20 = LocalDateTime.of(2026, 10, 23, 20, 0)
        RecordProductLog(catalog, fasts, records, tx)(RecordProductLog.Input(fastId, "magnesio", at20, ProductLogStatus.NOT_TAKEN, null, null, "Diarrea"))
        RecordNote(fasts, records)(fastId, at7.plusHours(5).plusMinutes(10), "leve temblor")
        TriggerManualAlert(fasts, alerts, tx, clock)(fastId, "Algo no va bien", "Mensaje")
        now = Instant.parse("2026-10-25T12:03:00Z")
        EndFast(fasts, refeed, tx, clock)(EndFast.Input(fastId, LocalDateTime.of(2026, 10, 25, 7, 3), EndReason.GOAL_MET, null, null))
        now = Instant.parse("2026-10-25T12:05:00Z")
        SetRefeedCheck(refeed, audit, tx, clock)(refeed.checks(fastId).first().id, true)
        RecordMeal(fasts, refeed)(RecordMeal.Input(fastId, LocalDateTime.of(2026, 10, 25, 9, 40), "Puré de calabaza", null, MealTolerance.FINE, null))
        now = Instant.parse("2026-10-25T19:03:11Z")
        return Seeded(fastId, template.template.id)
    }

    private fun DataWorld.exporter() = JsonDataExchange(database)
    private fun DataWorld.export() = ExportData(fasts, RoomSettingsRepository(database), RoomPersonProfileRepository(database, clock), exporter(), clock, "0.5.0")

    private fun parse(text: String) = Json.parseToJsonElement(text).jsonObject

    private fun schemaErrors(document: String) =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaFile.readText()).validate(document, com.networknt.schema.InputFormat.JSON)

    // ---------- Esquema publicado ----------

    @Test
    fun `H-036 el esquema publicado coincide con el formato y las exportaciones lo cumplen`() = runTest {
        val w = world()
        val generated = Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), SchemaGenerator.generate { w.exporter().columns(it) }) + "\n"
        val s = w.seed()
        val single = (w.export()(s.fastId) as Outcome.Ok).value.json
        if (System.getProperty("regenerateSchema") == "true") {
            schemaFile.writeText(generated)
            exampleFile.writeText(single + "\n")
        }
        assertEquals("El esquema cambió: regenerar con -PregenerateSchema=true y subir la versión si es incompatible", generated, schemaFile.readText())
        assertEquals(emptySet<Any>(), schemaErrors(single))
        assertEquals(emptySet<Any>(), schemaErrors((w.export()(null) as Outcome.Ok).value.json))
        assertEquals(emptySet<Any>(), schemaErrors(exampleFile.readText()))
        // El validador no es indulgente: un número escrito como texto o una clave de más incumplen el esquema.
        val doc = parse(single)
        val m = doc["measurements"]!!.jsonArray.single().jsonObject
        val textNumber = JsonObject(doc + ("measurements" to JsonArray(listOf(JsonObject(m + ("value_num" to JsonPrimitive("72")))))))
        assertTrue(schemaErrors(textNumber.toString()).isNotEmpty())
        assertTrue(schemaErrors(JsonObject(doc + ("extra" to JsonPrimitive(1))).toString()).isNotEmpty())
    }

    // ---------- H-036 ----------

    @Test
    fun `H-036 el archivo de un ayuno lleva su nombre, cabecera, su plan, la plantilla usada, catalogos y todas sus entradas`() = runTest {
        val w = world()
        val s = w.seed()
        val file = (w.export()(s.fastId) as Outcome.Ok).value
        assertEquals("ayuno_2026-10-22_${s.fastId.take(8)}.json", file.fileName)
        assertTrue("sin BOM", !file.json.startsWith("\uFEFF"))
        val doc = parse(file.json)
        assertEquals("fasting-log", doc["schema"]!!.jsonPrimitive.content)
        assertEquals("-05:00", doc["tz_offset"]!!.jsonPrimitive.content)
        assertEquals("2026-10-25T14:03:11", doc["exported_at"]!!.jsonPrimitive.content)
        listOf("full_name", "id_number", "birth_date").forEach { assertEquals(JsonNull, doc["person"]!!.jsonObject[it]) }
        assertEquals(listOf(s.templateId), doc["templates"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content })
        assertTrue(doc["catalogs"]!!.jsonObject["products"]!!.jsonArray.any { it.jsonObject["code"]!!.jsonPrimitive.content == "magnesio" })
        listOf("stages", "plan_metrics", "metric_schedules", "rules", "intake_goals", "checklist_items", "stage_notices", "plan_products", "refeed_steps").forEach {
            // Plan de la plantilla y copia del ayuno.
            assertTrue(it, doc[it]!!.jsonArray.size >= 2 || it == "metric_schedules" && doc[it]!!.jsonArray.size == 2)
        }
        listOf("measurements", "blood_pressure_readings", "symptoms", "intakes", "product_logs", "alerts", "notes", "refeed_checks", "meals", "edit_logs").forEach {
            assertTrue(it, doc[it]!!.jsonArray.isNotEmpty())
        }
        val m = doc["measurements"]!!.jsonArray.single().jsonObject
        assertEquals("2026-10-23T07:02:15", m["taken_at"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, m["value_text"])
        assertEquals(JsonNull, m["note"])
        assertTrue(m.containsKey("deleted_at"))
        assertEquals(72.0, m["value_num"]!!.jsonPrimitive.content.toDouble(), 0.0)
        assertTrue("número, no texto", !m["value_num"]!!.jsonPrimitive.isString)
        assertEquals(false, m["on_waking"]!!.jsonPrimitive.content.toBoolean())
        val log = doc["product_logs"]!!.jsonArray.single().jsonObject
        assertEquals("no anotado sale vacío, nunca cero", JsonNull, log["amount"])
        assertEquals(JsonArray(listOf(JsonPrimitive(12.0), JsonPrimitive(24.0))), doc["fasts"]!!.jsonArray.single().jsonObject["milestone_hours"])
    }

    @Test
    fun `H-036 reexportar da los mismos identificadores y exportar todo incluye todos los ayunos`() = runTest {
        val w = world()
        val s = w.seed()
        val other = (w.createFast(CreateFast.Input(null, LocalDateTime.of(2026, 11, 5, 18, 0), LocalDateTime.of(2026, 11, 5, 22, 0), 36.0, null)) as Outcome.Ok).value.fast.id
        fun ids(text: String) = parse(text).filterKeys { it != "exported_at" }
        val first = (w.export()(s.fastId) as Outcome.Ok).value.json
        w.now = w.now.plusSeconds(3600)
        val second = (w.export()(s.fastId) as Outcome.Ok).value.json
        assertEquals(ids(first), ids(second))
        val all = (w.export()(null) as Outcome.Ok).value
        assertEquals("ayuno_todo_2026-10-25.json", all.fileName)
        assertEquals(setOf(s.fastId, other), parse(all.json)["fasts"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet())
        assertEquals("ayuno_2026-11-05_${other.take(8)}.json", ExportNames.forFast(w.fasts.get(other)!!.fast))
    }

    // ---------- H-038 ----------

    @Test
    fun `H-038 los datos personales solo viajan si la persona lo activa`() = runTest {
        val w = world()
        val s = w.seed()
        val save = SavePersonProfile(RoomPersonProfileRepository(w.database, w.clock), RoomSettingsRepository(w.database))
        save(PersonProfile(" Ana Pérez ", "123", LocalDate.of(1980, 5, 1)), includeInExport = false)
        val hidden = parse((w.export()(s.fastId) as Outcome.Ok).value.json)["person"]!!.jsonObject
        assertEquals(JsonNull, hidden["full_name"])
        save(PersonProfile("Ana Pérez", "123", LocalDate.of(1980, 5, 1)), includeInExport = true)
        val shown = parse((w.export()(s.fastId) as Outcome.Ok).value.json)["person"]!!.jsonObject
        assertEquals("Ana Pérez", shown["full_name"]!!.jsonPrimitive.content)
        assertEquals("1980-05-01", shown["birth_date"]!!.jsonPrimitive.content)
    }

    // ---------- H-037 ----------

    @Test
    fun `H-037 el comprimido trae una tabla por lista con las mismas columnas y vacios para lo no anotado`() = runTest {
        val w = world()
        val s = w.seed()
        val json = (w.export()(s.fastId) as Outcome.Ok).value.json
        val tables = mutableMapOf<String, List<String>>()
        ZipInputStream(ByteArrayInputStream(w.exporter().tables(json))).use { zip ->
            generateSequence { zip.nextEntry }.forEach { tables[it.name] = zip.readBytes().toString(Charsets.UTF_8).trimEnd().split("\r\n") }
        }
        assertEquals(ExchangeFormat.TABLES.map { "${it.key}.csv" }.toSet(), tables.keys)
        val measurement = parse(json)["measurements"]!!.jsonArray.single().jsonObject
        val csv = tables.getValue("measurements.csv")
        assertEquals(measurement.keys.joinToString(","), csv[0])
        val cells = csv[1].split(",")
        assertEquals("", cells[measurement.keys.indexOf("value_text")])
        assertEquals("72.0", cells[measurement.keys.indexOf("value_num")])
    }

    // ---------- H-040 ----------

    private fun DataWorld.snapshot(): Map<String, List<Map<String, JsonElement>>> =
        ExchangeFormat.TABLES.associate { t ->
            val cols = exporter().columns(t)
            t.table to database.openHelper.readableDatabase.query("SELECT * FROM `${t.table}` ORDER BY `${t.pk}`").use { c ->
                buildList {
                    while (c.moveToNext()) add(cols.associate { col -> col.name to (if (c.isNull(c.getColumnIndexOrThrow(col.name))) JsonNull else JsonPrimitive(c.getString(c.getColumnIndexOrThrow(col.name)))) })
                }
            }
        }

    @Test
    fun `H-040 restaurar en un telefono nuevo deja todo igual al segundo e importar dos veces no duplica`() = runTest {
        val source = world()
        source.seed()
        val backup = (source.export()(null) as Outcome.Ok).value.json.toByteArray()

        val fresh = world()
        val preview = (fresh.exporter().preview(backup) as ImportCheck.Ok).value
        assertEquals(1, preview.counts.getValue("fasts").new)
        val result = (fresh.exporter().import(backup) as ImportCheck.Ok).value
        assertTrue(result.created > 0)
        assertEquals(source.snapshot().filterKeys { it !in setOf("metric_type", "symptom_type", "intake_type") }, fresh.snapshot().filterKeys { it !in setOf("metric_type", "symptom_type", "intake_type") })
        val fastId = source.fasts.all().single().id
        assertEquals(source.records.measurements(fastId), fresh.records.measurements(fastId))
        assertEquals(source.fasts.get(fastId), fresh.fasts.get(fastId))

        val before = fresh.snapshot()
        val again = (fresh.exporter().import(backup) as ImportCheck.Ok).value
        assertEquals(0, again.created)
        assertEquals(0, again.updated)
        assertEquals(before, fresh.snapshot())
    }

    @Test
    fun `H-040 los campos desconocidos se listan y se importa igual`() = runTest {
        val source = world()
        val s = source.seed()
        val doc = parse((source.export()(s.fastId) as Outcome.Ok).value.json).toMutableMap()
        doc["weather"] = JsonPrimitive("soleado")
        doc["notes"] = JsonArray(doc["notes"]!!.jsonArray.map { JsonObject(it.jsonObject + ("mood" to JsonPrimitive("bien"))) })
        val fresh = world()
        val result = (fresh.exporter().import(JsonObject(doc).toString().toByteArray()) as ImportCheck.Ok).value
        assertEquals(listOf("weather", "notes.mood"), result.ignoredFields)
        assertEquals("leve temblor", fresh.records.notes(s.fastId).single().text)
    }

    @Test
    fun `H-040 una version de formato mayor o un archivo ajeno se rechazan sin tocar nada`() = runTest {
        val source = world()
        val s = source.seed()
        val doc = parse((source.export()(s.fastId) as Outcome.Ok).value.json)
        val fresh = world()
        val before = fresh.snapshot()
        val newer = JsonObject(doc + ("schema_version" to JsonPrimitive("2.0"))).toString().toByteArray()
        assertEquals(ImportCheck.Rejected(ImportProblem.NewerVersion("2.0")), fresh.exporter().import(newer))
        assertEquals(ImportCheck.Rejected(ImportProblem.NotOurFile), fresh.exporter().import("hola".toByteArray()))
        assertEquals(ImportCheck.Rejected(ImportProblem.NotOurFile), fresh.exporter().import("""{"schema":"otro"}""".toByteArray()))
        assertEquals(before, fresh.snapshot())
    }

    @Test
    fun `H-039 H-040 una fila invalida o una referencia rota rechazan todo el archivo`() = runTest {
        val source = world()
        val s = source.seed()
        val doc = parse((source.export()(s.fastId) as Outcome.Ok).value.json)
        val fresh = world()
        val before = fresh.snapshot()
        fun with(list: String, change: (JsonObject) -> JsonObject) =
            JsonObject(doc + (list to JsonArray(doc[list]!!.jsonArray.mapIndexed { i, r -> if (i == 0) change(r.jsonObject) else r })))
        val badEnum = with("measurements") { JsonObject(it + ("source" to JsonPrimitive("satelite"))) }
        assertEquals(ImportCheck.Rejected(ImportProblem.InvalidRow("measurements", 1, "source")), fresh.exporter().import(badEnum.toString().toByteArray()))
        val badTime = with("notes") { JsonObject(it + ("noted_at" to JsonPrimitive("2026-10-23T12:10:00Z"))) }
        assertEquals(ImportCheck.Rejected(ImportProblem.InvalidRow("notes", 1, "noted_at")), fresh.exporter().import(badTime.toString().toByteArray()))
        val orphan = with("measurements") { JsonObject(it + ("fast_id" to JsonPrimitive("no-existe"))) }
        assertEquals(ImportCheck.Rejected(ImportProblem.InvalidRow("measurements", 1, "fast_id")), fresh.exporter().import(orphan.toString().toByteArray()))
        val textNumber = with("intakes") { JsonObject(it + ("amount" to JsonPrimitive("2.5"))) }
        assertEquals(ImportCheck.Rejected(ImportProblem.InvalidRow("intakes", 1, "amount")), fresh.exporter().import(textNumber.toString().toByteArray()))
        assertEquals(before, fresh.snapshot())
    }

    @Test
    fun `H-039 importar plantillas y productos no los duplica y un producto con el mismo codigo se actualiza si es mas reciente`() = runTest {
        val source = world()
        val s = source.seed()
        val doc = parse((source.export()(s.fastId) as Outcome.Ok).value.json)
        // Archivo de plantillas y productos: solo catálogos y la plantilla con su plan.
        val planLists = listOf("stages", "plan_metrics", "rules", "intake_goals", "checklist_items", "stage_notices", "plan_products", "refeed_steps")
        val templateMetrics = doc["plan_metrics"]!!.jsonArray.filter { it.jsonObject["template_id"] != JsonNull }.map { it.jsonObject["id"]!! }
        val onlyTemplates = JsonObject(
            doc.mapValues { (key, value) ->
                when (key) {
                    in planLists -> JsonArray(value.jsonArray.filter { it.jsonObject["template_id"] != JsonNull })
                    "metric_schedules" -> JsonArray(value.jsonArray.filter { it.jsonObject["plan_metric_id"] in templateMetrics })
                    "fasts", "measurements", "blood_pressure_readings", "symptoms", "intakes", "product_logs", "alerts", "notes", "refeed_checks", "meals", "edit_logs" -> JsonArray(emptyList())
                    else -> value
                }
            },
        )
        val target = world()
        target.now = Instant.parse("2026-10-20T12:00:00Z") // su producto es anterior al del archivo
        SaveProduct(target.catalog)(SaveProduct.Input(null, null, "Magnesio", ProductKind.SUPPLEMENT, 200.0, "mg", false, emptyList()))
        val bytes = onlyTemplates.toString().toByteArray()
        val result = (target.exporter().import(bytes) as ImportCheck.Ok).value
        assertTrue(result.updated >= 1)
        assertEquals(s.templateId, target.templates.get(s.templateId)!!.template.id)
        assertEquals(1, target.catalog.products().count { it.code == "magnesio" })
        assertEquals(400.0, target.catalog.product("magnesio")!!.defaultAmount, 0.0)
        assertEquals(1, target.templates.get(s.templateId)!!.plan.refeed.size)
        val again = (target.exporter().import(bytes) as ImportCheck.Ok).value
        assertEquals(0, again.created)
        assertNotEquals(0, again.keptLocal)
        assertTrue(target.fasts.all().isEmpty())
    }
}
