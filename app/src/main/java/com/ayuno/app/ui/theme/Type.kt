package com.ayuno.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Escala tipográfica de `specs/GUIA-ESTILO.md` §1.2. Mínimo 17 sp en todo estilo, también en los que usa Material por defecto. */
object AyunoType {
    private const val TABULAR = "tnum"

    val displayClock = TextStyle(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val headline = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium)
    val title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val bodyStrong = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val label = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val numericInput = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
}

val AyunoTypography = Typography(
    displayLarge = AyunoType.displayClock,
    displayMedium = AyunoType.displayClock,
    displaySmall = AyunoType.headline,
    headlineLarge = AyunoType.headline,
    headlineMedium = AyunoType.headline,
    headlineSmall = AyunoType.headline,
    titleLarge = AyunoType.title,
    titleMedium = AyunoType.title,
    titleSmall = AyunoType.label,
    bodyLarge = AyunoType.body,
    bodyMedium = AyunoType.body,
    bodySmall = AyunoType.body,
    labelLarge = AyunoType.label,
    labelMedium = AyunoType.label,
    labelSmall = AyunoType.label,
)
