package com.ayuno.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.ayuno.app.R
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** SelectorFechaHora: «viernes 23 de octubre, 12:01:40 ✎»; abre los selectores nativos de fecha y hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: LocalDateTime, onChange: (LocalDateTime) -> Unit, showSeconds: Boolean = false) {
    var pickingDate by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val editLabel = stringResource(R.string.action_edit)

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchMin)
            .clickable(role = Role.Button, onClickLabel = editLabel) { pickingDate = true },
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showSeconds) Formats.dateTimeSeconds(value) else Formats.dateTime(value),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(vertical = Dimens.spaceXs),
            )
            Icon(Icons.Filled.Edit, contentDescription = null)
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingDate = false
                    pendingDate = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: value.toLocalDate()
                }) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }

    pendingDate?.let { date ->
        TimePickerDialog(
            initial = value.toLocalTime(),
            onDismiss = { pendingDate = null },
            onConfirm = { time ->
                pendingDate = null
                onChange(LocalDateTime.of(date, time))
            },
        )
    }
}

/** Selector de hora del día (horarios de recordatorio, hora de comprobación). */
@Composable
fun TimeField(label: String, value: LocalTime, onChange: (LocalTime) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().heightIn(min = Dimens.touchMin).clickable(role = Role.Button) { picking = true },
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AyunoThemeTokens.colors.onSurfaceVariant)
        Text(Formats.time(value), style = MaterialTheme.typography.bodyLarge)
    }
    if (picking) TimePickerDialog(value, onDismiss = { picking = false }, onConfirm = { picking = false; onChange(it) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.action_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state = state) },
    )
}
