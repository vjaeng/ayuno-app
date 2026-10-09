package com.ayuno.app.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.reminders.ReminderNotifier
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ErrorState
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.InfoBanner
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.Milestone
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import java.time.LocalDateTime

data class HomeActions(
    val onNewFast: () -> Unit,
    val onRecord: (fastId: String) -> Unit,
    val onRecordMetric: (fastId: String, metricCode: String, scheduledFor: LocalDateTime) -> Unit,
    val onSafety: () -> Unit,
    val onEndFast: (fastId: String) -> Unit,
    val onAlert: (alertId: String) -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = viewModel(factory = containerFactory { HomeViewModel(it) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()

    // Una alerta sin decidir vuelve a mostrarse al abrir (H-027).
    val undecided = (state as? HomeUiState.InProgress)?.undecidedAlertId
    LaunchedEffect(undecided) { undecided?.let(actions.onAlert) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceL),
    ) {
        PermissionBanners(viewModel)
        errors.forEach { FieldError(stringResource(it.message())) }
        when (val s = state) {
            HomeUiState.Loading -> LoadingState()
            HomeUiState.Error -> ErrorState(stringResource(R.string.error_load)) { }
            is HomeUiState.Idle -> IdleContent(s, viewModel, actions)
            is HomeUiState.InProgress -> InProgressContent(s, viewModel, actions)
        }
    }
}

@Composable
private fun PermissionBanners(viewModel: HomeViewModel) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(ReminderNotifier.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = ReminderNotifier.canNotify(context) }
    if (!canNotify) {
        InfoBanner(stringResource(R.string.banner_notifications_off), stringResource(R.string.action_allow)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }
    }
    if (!viewModel.exactAlarmsAllowed()) {
        InfoBanner(stringResource(R.string.banner_exact_alarms_off), stringResource(R.string.action_open_settings)) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}

@Composable
private fun IdleContent(state: HomeUiState.Idle, viewModel: HomeViewModel, actions: HomeActions) {
    if (state.planned == null && state.lastFinished == null) {
        EmptyState(stringResource(R.string.home_empty), stringResource(R.string.home_new_fast), actions.onNewFast)
        return
    }
    state.planned?.let { fast ->
        SectionTitle(stringResource(R.string.home_next_fast))
        Text(stringResource(R.string.home_last_meal_planned, Formats.dateTime(fast.lastMealPlanned)), style = MaterialTheme.typography.bodyLarge)
        fast.lastMealActual?.let { Text(stringResource(R.string.home_last_meal_actual, Formats.dateTime(it)), style = MaterialTheme.typography.bodyLarge) }
        Text(stringResource(R.string.home_clock_planned, Formats.dateTime(fast.clockStartPlanned)), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_target, Formats.number(fast.targetHours)), style = MaterialTheme.typography.bodyLarge)
        LastMealButton(fast, viewModel)
        PrimaryButton(stringResource(R.string.home_start_now), onClick = { viewModel.startFast(fast.id) })
    }
    state.lastFinished?.let { fast ->
        SectionTitle(stringResource(R.string.home_last_fast))
        fast.endActual?.let { Text(Formats.dateTime(it), style = MaterialTheme.typography.bodyLarge) }
        com.ayuno.domain.derived.FastClock.finalDuration(fast)?.let { Text(Formats.duration(it), style = MaterialTheme.typography.bodyLarge) }
        fast.endReason?.let { Text(endReasonText(it, fast.endReasonDetail), style = MaterialTheme.typography.bodyLarge) }
    }
    SecondaryButton(stringResource(R.string.home_new_fast), actions.onNewFast)
}

@Composable
private fun LastMealButton(fast: Fast, viewModel: HomeViewModel) {
    var editing by remember { mutableStateOf(false) }
    SecondaryButton(
        stringResource(if (fast.lastMealActual == null) R.string.home_register_last_meal else R.string.home_correct_last_meal),
        onClick = { editing = true },
    )
    if (editing) {
        var value by remember { mutableStateOf(fast.lastMealActual ?: fast.lastMealPlanned) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.home_last_meal_title)) },
            text = { DateTimeField(stringResource(R.string.field_last_meal_actual), value, { value = it }) },
            confirmButton = { TextButton(onClick = { editing = false; viewModel.registerLastMeal(fast.id, value) }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun InProgressContent(state: HomeUiState.InProgress, viewModel: HomeViewModel, actions: HomeActions) {
    val colors = AyunoThemeTokens.colors
    var showDetail by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        FilledTonalButton(onClick = actions.onSafety, modifier = Modifier.heightIn(min = Dimens.touchMin)) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Spacer(Modifier.size(Dimens.spaceS))
            Text(stringResource(R.string.safety_button))
        }
    }

    val clockText = Formats.duration(state.elapsed)
    val hours = state.elapsed.toHours().toInt()
    val minutes = (state.elapsed.toMinutes() % 60).toInt()
    val clockDescription = stringResource(
        R.string.home_clock_description,
        pluralStringResource(R.plurals.hours, hours, hours),
        pluralStringResource(R.plurals.minutes, minutes, minutes),
    )
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(Dimens.radiusL),
        onClick = { showDetail = !showDetail },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.spaceXl), verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
            Text(clockText, style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { contentDescription = clockDescription })
            Text(stringResource(R.string.home_remaining, Formats.duration(state.remaining)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.home_planned_end, Formats.dateTime(state.plannedEnd)), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (showDetail) ClockDetail(state.fast, viewModel, actions)
    Milestones(state.milestones)

    state.next?.let { next ->
        SectionTitle(stringResource(R.string.home_next_pending))
        com.ayuno.app.ui.components.ListRow(
            title = next.metricName,
            supporting = stringResource(if (next.late) R.string.pending_late_at else R.string.pending_at, Formats.dateTime(next.at)),
            onClick = { actions.onRecordMetric(state.fast.id, next.metricCode, next.at) },
        )
    }
    state.last?.let { last ->
        SectionTitle(stringResource(R.string.home_last_measurement))
        Text(stringResource(R.string.home_last_measurement_value, last.metricName, last.value, last.unit, Formats.dateTime(last.at)), style = MaterialTheme.typography.bodyLarge)
    }
    PrimaryButton(stringResource(R.string.home_record), onClick = { actions.onRecord(state.fast.id) })
}

@Composable
private fun ClockDetail(fast: Fast, viewModel: HomeViewModel, actions: HomeActions) {
    var correcting by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        Text(stringResource(R.string.home_planned_vs_actual, Formats.dateTime(fast.clockStartPlanned), Formats.dateTime(fast.clockStartActual!!)), style = MaterialTheme.typography.bodyLarge)
        Text(
            fast.lastMealActual?.let { stringResource(R.string.home_last_meal_planned_vs_actual, Formats.dateTime(fast.lastMealPlanned), Formats.dateTime(it)) }
                ?: stringResource(R.string.home_last_meal_planned, Formats.dateTime(fast.lastMealPlanned)),
            style = MaterialTheme.typography.bodyLarge,
        )
        fast.notes?.let { Text(stringResource(R.string.home_notes, it), style = MaterialTheme.typography.bodyLarge) }
        LastMealButton(fast, viewModel)
        SecondaryButton(stringResource(R.string.home_correct_start), onClick = { correcting = true })
        SecondaryButton(stringResource(R.string.end_title), onClick = { actions.onEndFast(fast.id) })
    }
    if (correcting) {
        var value by remember { mutableStateOf(fast.clockStartActual!!) }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { correcting = false },
            title = { Text(stringResource(R.string.home_correct_start)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
                    DateTimeField(stringResource(R.string.field_clock_start_actual), value, { value = it }, showSeconds = true)
                    AyunoTextField(stringResource(R.string.field_reason), reason, { reason = it })
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.correctClockStart(fast.id, value, reason) { correcting = false } }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { correcting = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Milestones(milestones: List<Milestone>) {
    val colors = AyunoThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        milestones.forEach { m ->
            val label = stringResource(if (m.reached) R.string.milestone_reached else R.string.milestone_pending, Formats.number(m.hours), Formats.dateTime(m.at))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = Dimens.touchMin)) {
                Surface(
                    shape = CircleShape,
                    color = if (m.reached) colors.primary else colors.surface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, colors.primary),
                    modifier = Modifier.size(16.dp),
                ) {}
                Spacer(Modifier.size(Dimens.spaceM))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun endReasonText(reason: EndReason, detail: String?): String = when (reason) {
    EndReason.GOAL_MET -> stringResource(R.string.end_reason_goal_met)
    EndReason.ALERT_RULE -> stringResource(R.string.end_reason_alert_rule)
    EndReason.PERSONAL_DECISION -> stringResource(R.string.end_reason_personal)
    EndReason.OTHER -> detail ?: stringResource(R.string.end_reason_other)
}

