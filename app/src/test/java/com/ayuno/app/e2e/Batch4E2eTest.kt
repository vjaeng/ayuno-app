package com.ayuno.app.e2e

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordMeasurement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.LocalDateTime

/** H-036 (exportar un ayuno) y H-040 (importar sin duplicar; versión mayor rechazada). */
@RunWith(AndroidJUnit4::class)
class Batch4E2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun seededFast(): String {
        val id = seedStartedFast(seedTemplate(metrics = listOf("glucose")))
        val at7 = LocalDateTime.of(2026, 10, 23, 7, 0)
        runBlocking { app.container.recordMeasurement(RecordMeasurement.Input(id, "glucose", at7.plusMinutes(2), at7, 72.0, "mg/dL", source = GlucoseSource.CGM)) }
        return id
    }

    @Test
    fun `H036 desde el detalle se exporta el ayuno a un archivo con su nombre y se entrega por la hoja de compartir`() {
        val id = seededFast()
        compose.activityRule.scenario.recreate()
        compose.tap("Historial")
        compose.tap("● En curso · desde el jueves 22 de octubre")
        compose.tap("Exportar")
        val name = "ayuno_2026-10-22_${id.take(8)}.json"
        compose.assertShown("Archivo: $name")
        compose.tap("Exportar")
        compose.eventually {
            val chooser = shadowOf(compose.activity).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION")
            val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND, send.action)
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        }
        val file = File(File(app.cacheDir, "exports"), name)
        val text = file.readText()
        assertTrue(text.contains("\"schema\": \"fasting-log\""))
        assertTrue(text.contains(id))
        assertTrue(text.contains("\"taken_at\": \"2026-10-23T07:02:00\""))
    }

    private fun pickFile(content: String, name: String) {
        val file = File(app.cacheDir, name).apply { writeText(content) }
        compose.tap("Importar archivo…")
        val shadow = shadowOf(compose.activity)
        val request = shadow.nextStartedActivityForResult
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
        shadow.receiveResult(request.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
    }

    @Test
    fun `H040 importar una copia ya presente muestra el resumen y no duplica nada`() {
        val id = seededFast()
        val backup = runBlocking { (app.container.exportData(null) as Outcome.Ok).value.json }
        compose.activityRule.scenario.recreate()
        compose.tap("Más")
        compose.tap("Exportar e importar")
        pickFile(backup, "copia.json")
        compose.assertShown("Importar copia.json")
        compose.assertShown("Contiene: 1 ayunos, 1 plantillas, 0 productos y 1 registros.")
        compose.assertShown("Nuevos: 0 ayunos, 0 plantillas, 0 productos y 0 registros.")
        compose.tap("Importar")
        compose.assertShown("Importado: 0 nuevos, 0 actualizados", substring = true)
        assertEquals(1, runBlocking { app.container.fasts.all() }.size)
        assertEquals(1, runBlocking { app.container.records.measurements(id) }.size)
    }

    @Test
    fun `H040 un archivo de una version de formato mayor se rechaza con un mensaje claro`() {
        seededFast()
        val newer = runBlocking { (app.container.exportData(null) as Outcome.Ok).value.json }.replace("\"schema_version\": \"1.0\"", "\"schema_version\": \"2.0\"")
        compose.activityRule.scenario.recreate()
        compose.tap("Más")
        compose.tap("Exportar e importar")
        pickFile(newer, "nueva.json")
        compose.assertShown("Este archivo es de una versión más nueva de la app (formato 2.0). Actualiza la app para importarlo.")
    }
}
