package com.ayuno.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens de color de `specs/GUIA-ESTILO.md` §1.1. Sin verde; el rojo (`alert`) es solo para alertas. */
@Immutable
data class AyunoColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineStrong: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val noticeContainer: Color,
    val onNoticeContainer: Color,
    val noticeAccent: Color,
    val alert: Color,
    val onAlert: Color,
    val focus: Color,
)

val LightAyunoColors = AyunoColors(
    background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEDE6),
    onSurface = Color(0xFF1B1C1A),
    onSurfaceVariant = Color(0xFF45473F),
    outline = Color(0xFF75786F),
    outlineStrong = Color(0xFF1B1C1A),
    primary = Color(0xFF1F5F8B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E4F3),
    onPrimaryContainer = Color(0xFF0B2E47),
    noticeContainer = Color(0xFFFFF4CC),
    onNoticeContainer = Color(0xFF4A3800),
    noticeAccent = Color(0xFFB58100),
    alert = Color(0xFFB3261E),
    onAlert = Color(0xFFFFFFFF),
    focus = Color(0xFF1F5F8B),
)

val DarkAyunoColors = AyunoColors(
    background = Color(0xFF121412),
    surface = Color(0xFF1B1D1B),
    surfaceVariant = Color(0xFF2A2D29),
    onSurface = Color(0xFFE4E3DE),
    onSurfaceVariant = Color(0xFFC5C7BD),
    outline = Color(0xFF8F9288),
    outlineStrong = Color(0xFFE4E3DE),
    primary = Color(0xFF9CCBF0),
    onPrimary = Color(0xFF003352),
    primaryContainer = Color(0xFF0E4A70),
    onPrimaryContainer = Color(0xFFD2E4F3),
    noticeContainer = Color(0xFF3D3000),
    onNoticeContainer = Color(0xFFFFE08A),
    noticeAccent = Color(0xFFE8B634),
    alert = Color(0xFF8C1D18),
    onAlert = Color(0xFFFFFFFF),
    focus = Color(0xFF9CCBF0),
)
