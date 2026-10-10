package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.first
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.domain.model.StageKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** H-001, H-007, H-019 desde la interfaz. */
@RunWith(AndroidJUnit4::class)
class PlanAndCatalogE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `H001 crear una plantilla con objetivo, hitos, etapa y medicion activa`() {
        compose.tap("Más"); compose.tap("Plantillas"); compose.tap("Crear plantilla")
        compose.type("Nombre", "Ayuno de 36 horas")
        compose.type("Objetivo", "36")
        compose.type("Hitos en horas, separados por comas", "12, 24")
        compose.tap("Vuelta a comer")
        compose.type("Duración", "6")
        compose.tap("Glucosa")
        compose.tap("Guardar")
        compose.assertShown("Crear plantilla") // de vuelta en la lista: guardado
        compose.assertShown("Ayuno de 36 horas")
        val saved = runBlocking {
            val id = app.container.templates.observeTemplates().first().single { it.name != EXAMPLE_TEMPLATE }.id
            app.container.templates.get(id)!!
        }
        assertEquals(36.0, saved.template.targetHours, 0.0)
        assertEquals(listOf(12.0, 24.0), saved.template.milestoneHours)
        assertEquals(StageKind.REFEED, saved.plan.stages.single().kind)
        assertEquals(6.0, saved.plan.stages.single().durationValue!!, 0.0)
        assertEquals("glucose", saved.plan.metrics.single().metricTypeCode)
    }

    @Test
    fun `H001 validaciones de plantilla se muestran en lenguaje llano`() {
        compose.tap("Más"); compose.tap("Plantillas"); compose.tap("Crear plantilla")
        compose.tap("Guardar")
        compose.assertShown("Escribe un nombre.")
        compose.assertShown("El objetivo debe ser un número de horas mayor que cero.")
    }

    @Test
    fun `H019 crear una regla con vista previa y sin umbral sugerido`() {
        compose.tap("Más"); compose.tap("Plantillas"); compose.tap("Crear plantilla")
        compose.type("Nombre", "Con regla"); compose.type("Objetivo", "24")
        compose.tap("Crear regla")
        compose.type("Nombre", "Glucosa baja")
        compose.tap("Glucosa")
        compose.type("Valor", "55")
        compose.type("Mensaje que se mostrará tal cual", "Toma sal en agua.")
        compose.assertShown("Si glucosa por debajo de 55 mg/dL: alerta roja.")
        compose.tap("Aceptar")
        compose.tap("Guardar")
        compose.assertShown("Crear plantilla")
        val rules = runBlocking {
            val id = app.container.templates.observeTemplates().first().single { it.name != EXAMPLE_TEMPLATE }.id
            app.container.templates.get(id)!!.plan.rules
        }
        assertEquals("Toma sal en agua.", rules.single().message)
        assertEquals(55.0, rules.single().threshold!!, 0.0)
    }

    @Test
    fun `H007 desactivar un tipo del catalogo y anadir una medicion propia`() {
        compose.tap("Más"); compose.tap("Catálogos"); compose.tap("Mediciones")
        compose.assertShown("Presión arterial")
        compose.onAllNodes(androidx.compose.ui.test.isToggleable())[1].performClick()
        compose.waitForIdle()
        compose.eventually { assertFalse(runBlocking { app.container.catalog.metricType("ketones")!!.active }) }
        compose.tap("Añadir una medición propia")
        compose.type("Nombre", "Perímetro de cuello")
        compose.type("Unidad", "cm")
        compose.tap("Guardar")
        compose.assertShown("Perímetro de cuello")
        compose.eventually { assertEquals("cm", runBlocking { app.container.catalog.metricType("perimetro_de_cuello")!!.unit }) }
    }
}

/** Plantilla de ejemplo que trae la app al instalar (H-003). */
private const val EXAMPLE_TEMPLATE = "Ejemplo (sin valores clínicos)"
