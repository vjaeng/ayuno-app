package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** H-007 */
class CatalogUseCasesTest {

    @Test
    fun `se puede desactivar un tipo precargado y vuelve a activarse`() = runTest {
        val w = TestWorld()
        w.setMetricTypeActive("glucose", false)
        assertFalse(w.catalog.metricType("glucose")!!.active)
        w.setMetricTypeActive("glucose", true)
        assertTrue(w.catalog.metricType("glucose")!!.active)
    }

    @Test
    fun `se puede anadir una medicion propia con nombre, unidad, si es numerica y rango`() = runTest {
        val w = TestWorld()
        val result = w.saveMetricType(SaveMetricType.Input(null, "Perímetro de cuello", "cm", true, 20.0, 60.0))
        val saved = (result as Outcome.Ok).value
        assertEquals("perimetro_de_cuello", saved.code)
        assertEquals("cm", saved.unit)
        assertEquals("cm", saved.entryUnit)
        assertTrue(saved.active)
    }

    @Test
    fun `codigos de mediciones propias no se repiten`() = runTest {
        val w = TestWorld()
        w.saveMetricType(SaveMetricType.Input(null, "Cuello", "cm", true, null, null))
        val second = (w.saveMetricType(SaveMetricType.Input(null, "Cuello", "cm", true, null, null)) as Outcome.Ok).value
        assertEquals("cuello_2", second.code)
    }

    @Test
    fun `rango con minimo mayor o igual que maximo se rechaza`() = runTest {
        val result = TestWorld().saveMetricType(SaveMetricType.Input(null, "X", "cm", true, 10.0, 10.0))
        assertEquals(listOf(ValidationError.RANGE_MIN_NOT_BELOW_MAX), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `editar un tipo existente no cambia su unidad`() = runTest {
        val w = TestWorld()
        w.saveMetricType(SaveMetricType.Input("weight", "Peso corporal", "lb", false, 30.0, 250.0))
        val weight = w.catalog.metricType("weight")!!
        assertEquals("Peso corporal", weight.name)
        assertEquals("kg", weight.unit)
        assertTrue(weight.isNumeric)
        assertEquals(30.0, weight.minValid)
    }
}
