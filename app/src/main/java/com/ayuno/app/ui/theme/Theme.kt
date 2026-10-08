package com.ayuno.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAyunoColors = staticCompositionLocalOf { LightAyunoColors }

/**
 * Tema de la app. Sin color dinámico de Android (alteraría el rojo reservado y el contraste verificado).
 * El rol `error` de Material se mapea a `outline_strong`: ningún componente estándar pinta rojo por su cuenta.
 */
@Composable
fun AyunoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkAyunoColors else LightAyunoColors
    CompositionLocalProvider(LocalAyunoColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(darkTheme),
            typography = AyunoTypography,
            content = content,
        )
    }
}

private fun AyunoColors.toMaterialScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = primary,
        onTertiary = onPrimary,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerHigh = surfaceVariant,
        outline = outline,
        outlineVariant = outline,
        error = outlineStrong,
        onError = surface,
        errorContainer = surfaceVariant,
        onErrorContainer = onSurface,
    )
}

object AyunoThemeTokens {
    val colors: AyunoColors
        @Composable get() = LocalAyunoColors.current
}
