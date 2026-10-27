package com.ayuno.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens

/** Insignia (GUIA-ESTILO, EV-006): etiqueta breve con texto, nunca solo color; no tocable. */
@Composable
fun Badge(text: String) {
    Surface(color = AyunoThemeTokens.colors.surfaceVariant, contentColor = AyunoThemeTokens.colors.onSurfaceVariant, shape = RoundedCornerShape(Dimens.radiusS)) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceXs))
    }
}

/**
 * BotonRapido (GUIA-ESTILO, H-051): toque = guardar; pulsación larga = abrir el formulario. Deshabilitado con
 * indicador mientras guarda, para no duplicar con dos toques.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickButton(text: String, description: String, longClickLabel: String, saving: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AyunoThemeTokens.colors
    Surface(
        color = colors.surfaceVariant,
        contentColor = colors.onSurface,
        shape = RoundedCornerShape(Dimens.radiusM),
        modifier = modifier
            .heightIn(min = Dimens.buttonHeight)
            .widthIn(min = Dimens.touchMin)
            .combinedClickable(enabled = !saving, role = Role.Button, onClickLabel = description, onLongClickLabel = longClickLabel, onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) { contentDescription = description; role = Role.Button; onLongClick(longClickLabel) { onLongClick(); true } },
    ) {
        Box(Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceS), contentAlignment = Alignment.Center) {
            if (saving) CircularProgressIndicator(Modifier.size(24.dp)) else Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Marca de un tiempo del Temporizador: «○ 1 min» pendiente o «✔ 1 min» hecha (con texto, no solo color). */
data class TimerMark(val label: String, val done: Boolean)

/**
 * Temporizador (GUIA-ESTILO, H-053): hora de inicio encima, cifra con dígitos tabulares y marcas de cada tiempo del
 * plan. [announcement] (p. ej. «1 minuto de pie: toma la presión») se anuncia como región educada, no cada segundo.
 */
@Composable
fun Timer(startedText: String, seconds: Long, marks: List<TimerMark>, announcement: String?) {
    val colors = AyunoThemeTokens.colors
    Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer, shape = RoundedCornerShape(Dimens.radiusL), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.spaceXl), verticalArrangement = Arrangement.spacedBy(Dimens.spaceS), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(startedText, style = MaterialTheme.typography.bodyLarge)
            Text(
                "%d:%02d".format(seconds / 60, seconds % 60),
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceL)) {
                marks.forEach { Text((if (it.done) "✔ " else "○ ") + it.label, style = MaterialTheme.typography.bodyLarge) }
            }
            if (announcement != null) {
                Text(announcement, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}
