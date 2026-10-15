package com.ayuno.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.data.exchange.JsonDataExchange
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.ExportRequest
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.PersonProfile
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.port.AuditRepository
import com.ayuno.domain.usecase.CorrectMeasurement
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RecordResult
import com.ayuno.domain.usecase.SetEntryDeleted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** EV-004 · D-20 · TRX corregir-entrada: corrección y borrado sobre Room, atómicos y exportados con el mismo id. */
@RunWith(AndroidJUnit4::class)
class H048CorrectEntryRoomTest {

    private val w = DataWorld()
    private val at = LocalDateTime.of(2026, 10, 23, 12, 1, 40)
    private val correct = CorrectMeasurement(w.catalog, w.fasts, w.records, w.alerts, w.audit, w.tx, w.clock)
    private val setDeleted = SetEntryDeleted(w.records, w.audit, w.tx, w.clock)

    @After
    fun tearDown() = w.database.close()

    private suspend fun glucose(): Measurement {
        val t = TemplateWithPlan(Template(Ids.newId(), "T", 57.0, emptyList()), Plan(stages = emptyList(), metrics = emptyList(), rules = emptyList()))
        w.saveTemplate(t)
        val fastId = (w.createFast(CreateFast.Input(t.template.id, LocalDateTime.of(2026, 10, 22, 18, 0), LocalDateTime.of(2026, 10, 22, 22, 0), 57.0, null)) as Outcome.Ok).value.fast.id
        w.startFast(fastId)
        val r = w.recordMeasurement(RecordMeasurement.Input(fastId, "glucose", at, valueEntered = 46.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        return ((r as Outcome.Ok).value as RecordResult.Saved).entry
    }

    @Test
    fun `H-034 la correccion persiste con el mismo id y su bitacora, y queda marcada como corregida`() = runTest {
        val m = glucose()
        correct(CorrectMeasurement.Input(m.id, at, valueEntered = 64.0, unitEntered = "mg/dL", source = GlucoseSource.CGM, reason = "error de dedo"))
        val after = w.records.measurements(m.fastId).single()
        assertEquals(m.id, after.id)
        assertEquals(64.0, after.valueNum!!, 0.0)
        assertEquals(setOf("value_entered", "value_num"), w.audit.entriesFor(m.id).map { it.field }.toSet())
        assertEquals(setOf(m.id), w.audit.observeEditedRowIds(EntryKind.entries.map { it.entityName }).first())
        assertEquals(2, w.audit.observeEntriesFor(m.id).first().size)
    }

    @Test
    fun `TRX si falla la bitacora la medicion no cambia`() = runTest {
        val m = glucose()
        val failing = object : AuditRepository by w.audit {
            override suspend fun append(entry: EditLogEntry) = throw IllegalStateException("disco lleno")
        }
        val result = runCatching {
            CorrectMeasurement(w.catalog, w.fasts, w.records, w.alerts, failing, w.tx, w.clock)(
                CorrectMeasurement.Input(m.id, at, valueEntered = 64.0, unitEntered = "mg/dL", source = GlucoseSource.CGM),
            )
        }
        assertTrue(result.isFailure)
        assertEquals(46.0, w.records.measurements(m.fastId).single().valueNum!!, 0.0)
    }

    @Test
    fun `H-034 borrar oculta, restaurar devuelve y la exportacion lleva la borrada marcada, sin duplicar`() = runTest {
        val m = glucose()
        correct(CorrectMeasurement.Input(m.id, at, valueEntered = 64.0, unitEntered = "mg/dL", source = GlucoseSource.CGM))
        setDeleted(EntryKind.MEASUREMENT, m.id, true)
        assertTrue(w.records.measurements(m.fastId).isEmpty())
        assertEquals(listOf(m.id), w.records.observeDeletedMeasurements(m.fastId).first().map { it.id })
        assertNotNull(w.records.measurement(m.id, includeDeleted = true))

        val json = JsonDataExchange(w.database).export(ExportRequest(m.fastId, PersonProfile(null, null, null), "test", at, "-05:00"))
        val rows = findById(Json.parseToJsonElement(json), m.id)
        assertEquals(1, rows.size)
        assertNotEquals(JsonNull, rows.single()["deleted_at"])
        assertTrue(json.contains(EditAction.DELETE.code))

        setDeleted(EntryKind.MEASUREMENT, m.id, false)
        assertEquals(listOf(m.id), w.records.measurements(m.fastId).map { it.id })
        assertEquals(listOf(EditAction.EDIT, EditAction.EDIT, EditAction.DELETE, EditAction.RESTORE), w.audit.entriesFor(m.id).map { it.action })
    }

    private fun findById(element: JsonElement, id: String): List<JsonObject> = when (element) {
        is JsonObject -> listOfNotNull(element.takeIf { (it["id"] as? JsonPrimitive)?.content == id }) + element.values.flatMap { findById(it, id) }
        is JsonArray -> element.flatMap { findById(it, id) }
        else -> emptyList()
    }
}
