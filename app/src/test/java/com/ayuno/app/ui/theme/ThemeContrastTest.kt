package com.ayuno.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica la afirmación de `specs/GUIA-ESTILO.md` §1.1: todo par texto/fondo cumple contraste AA (≥ 4.5:1). */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private fun pairs(c: AyunoColors) = mapOf(
        "on_surface / background" to (c.onSurface to c.background),
        "on_surface / surface" to (c.onSurface to c.surface),
        "on_surface / surface_variant" to (c.onSurface to c.surfaceVariant),
        "on_surface_variant / surface" to (c.onSurfaceVariant to c.surface),
        "on_surface_variant / background" to (c.onSurfaceVariant to c.background),
        "on_primary / primary" to (c.onPrimary to c.primary),
        "on_primary_container / primary_container" to (c.onPrimaryContainer to c.primaryContainer),
        "primary / surface (texto de botón secundario)" to (c.primary to c.surface),
        "on_notice_container / notice_container" to (c.onNoticeContainer to c.noticeContainer),
        "on_alert / alert" to (c.onAlert to c.alert),
    )

    @Test
    fun `tema claro cumple contraste AA`() = assertAa(pairs(LightAyunoColors))

    @Test
    fun `tema oscuro cumple contraste AA`() = assertAa(pairs(DarkAyunoColors))

    private fun assertAa(pairs: Map<String, Pair<Color, Color>>) {
        val failing = pairs.mapValues { (_, p) -> contrast(p.first, p.second) }.filterValues { it < 4.5 }
        assertTrue("Pares bajo 4.5:1: $failing", failing.isEmpty())
    }
}
