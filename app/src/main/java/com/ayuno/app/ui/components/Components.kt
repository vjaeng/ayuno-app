package com.ayuno.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ayuno.app.R
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens

// Componentes canónicos de specs/GUIA-ESTILO.md §2. Ninguno usa rojo: el rojo es solo de AlertaRoja.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(Dimens.touchMin)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceL),
            content = content,
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.buttonHeight),
        shape = RoundedCornerShape(Dimens.radiusM),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.buttonHeight),
        shape = RoundedCornerShape(Dimens.radiusM),
        border = BorderStroke(1.dp, AyunoThemeTokens.colors.outline),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

/** CampoNumerico / CampoTexto: error con borde fuerte e icono ⓘ, nunca rojo. */
@Composable
fun AyunoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    error: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    /** Dato sensible (documento de identidad): oculto salvo mientras se edita. */
    masked: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            isError = error != null,
            suffix = suffix?.let { { Text(it) } },
            textStyle = if (numeric) MaterialTheme.typography.displaySmall else MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            visualTransformation = if (masked && !focused) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        if (error != null) FieldError(error)
    }
}

@Composable
fun FieldError(text: String) {
    Row(Modifier.padding(top = Dimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = AyunoThemeTokens.colors.onSurface)
        Spacer(Modifier.size(Dimens.spaceS))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = AyunoThemeTokens.colors.onSurface)
    }
}

/** SelectorOpciones: 2-5 opciones excluyentes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSelector(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.heightIn(min = Dimens.touchMin),
            ) { Text(label, maxLines = 2) }
        }
    }
}

/** Lista de opciones en columna (más de 5, o etiquetas largas). */
@Composable
fun <T> RadioList(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    Column {
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.touchMin)
                    .clickable(role = Role.RadioButton) { onSelect(value) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Spacer(Modifier.size(Dimens.spaceM))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchMin)
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.size(Dimens.spaceM))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Interruptor con etiqueta tocable en toda la fila. */
@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, supporting: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.rowMinHeight)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Fila tocable de lista (≥ 56 dp). */
@Composable
fun ListRow(title: String, supporting: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.rowMinHeight)
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
        }
        trailing()
    }
}

/** FranjaInformativa: avisos persistentes no clínicos. */
@Composable
fun InfoBanner(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Surface(color = AyunoThemeTokens.colors.surfaceVariant, shape = RoundedCornerShape(Dimens.radiusM)) {
        Column(Modifier.fillMaxWidth().padding(Dimens.spaceL)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.size(Dimens.spaceM))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, modifier = Modifier.heightIn(min = Dimens.touchMin)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun EmptyState(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = Dimens.spaceXl), verticalArrangement = Arrangement.spacedBy(Dimens.spaceL)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) SecondaryButton(actionLabel, onAction)
    }
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(Dimens.spaceXxl), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.spaceL)) {
        FieldError(text)
        SecondaryButton(stringResource(R.string.action_retry), onRetry)
    }
}
