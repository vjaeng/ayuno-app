package com.ayuno.domain.units

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnitConversionsTest {
    @Test
    fun `157_4 lb equivalen a 71_4 kg`() = assertEquals(71.4, UnitConversions.convert(157.4, "lb", "kg"), 0.05)

    @Test
    fun `glucosa en mmol por litro se convierte a mg por decilitro`() = assertEquals(72.06, UnitConversions.convert(4.0, "mmol/L", "mg/dL"), 0.01)

    @Test
    fun `temperatura en Fahrenheit se convierte a Celsius`() = assertEquals(37.0, UnitConversions.convert(98.6, "°F", "°C"), 0.01)

    @Test
    fun `misma unidad no cambia el valor`() = assertEquals(64.0, UnitConversions.convert(64.0, "mg/dL", "mg/dL"))

    @Test
    fun `unidades sin conversion conocida fallan`() {
        assertThrows<IllegalArgumentException> { UnitConversions.convert(1.0, "kg", "mg/dL") }
    }
}
