package com.ayuno.app.ui.fast

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.DateTimeField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.RadioList
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.home.endReasonText
import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.time.LocalTimes
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class EndFastViewModel(private val container: AppContainer, fastId: String) : ViewModel() {
    private val _source = MutableStateFlow<FastWithPlan?>(null)
    val source: StateFlow<FastWithPlan?> = _source.asStateFlow()
    private val _ended = MutableStateFlow<Fast?>(null)
    val ended: StateFlow<Fast?> = _ended.asStateFlow()
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    init { viewModelScope.launch { _source.value = container.fasts.get(fastId) } }

    fun now(): LocalDateTime = LocalTimes.now(container.clock)

    fun end(input: EndFast.Input) {
        viewModelScope.launch {
            when (val result = container.endFast(input)) {
                is Outcome.Ok -> { container.reminders.reschedule(); _ended.value = result.value }
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}

/** H-030: terminar con hora real y motivo obligatorio; el resultado se muestra sin juicio. */
@Composable
fun EndFastScreen(fastId: String, ruleId: String?, endAt: LocalDateTime? = null, onBack: () -> Unit, onDone: () -> Unit) {
    val viewModel: EndFastViewModel = viewModel(key = fastId, factory = containerFactory { EndFastViewModel(it, fastId) })
    val source by viewModel.source.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()

    // Desde una ingesta que rompe el ayuno, la hora de fin propuesta es la de esa ingesta (UX fin-terminar).
    var endAt by remember { mutableStateOf(endAt ?: viewModel.now()) }
    var reason by remember { mutableStateOf(if (ruleId != null) EndReason.ALERT_RULE else null) }
    var selectedRule by remember { mutableStateOf(ruleId) }
    var detail by remember { mutableStateOf("") }

    ended?.let { fast ->
        ScreenScaffold(stringResource(R.string.end_result_title), onBack = null) {
            Text(Formats.dateTime(fast.endActual!!), style = MaterialTheme.typography.bodyLarge)
            FastClock.finalDuration(fast)?.let { Text(Formats.duration(it), style = MaterialTheme.typography.headlineMedium) }
            Text(stringResource(R.string.end_result_reason, endReasonText(fast.endReason!!, fast.endReasonDetail)), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(stringResource(R.string.end_back_home), onClick = onDone)
        }
        return
    }

    ScreenScaffold(stringResource(R.string.end_title), onBack) {
        val s = source ?: run { LoadingState(); return@ScreenScaffold }
        DateTimeField(stringResource(R.string.field_end_actual), endAt, { endAt = it }, showSeconds = true)
        s.fast.clockStartActual?.let { start ->
            val offset = ZoneOffset.of(s.fast.tzOffset)
            val duration = Duration.between(start.toInstant(offset), endAt.toInstant(offset))
            if (!duration.isNegative) Text(stringResource(R.string.end_duration, Formats.duration(duration)), style = MaterialTheme.typography.bodyLarge)
        }
        SectionTitle(stringResource(R.string.end_why))
        RadioList(
            listOf(
                EndReason.GOAL_MET to stringResource(R.string.end_reason_goal_met),
                EndReason.ALERT_RULE to stringResource(R.string.end_reason_alert_rule),
                EndReason.PERSONAL_DECISION to stringResource(R.string.end_reason_personal),
                EndReason.OTHER to stringResource(R.string.end_reason_other),
            ),
            reason,
            { reason = it },
        )
        if (reason == EndReason.ALERT_RULE) {
            RadioList(s.plan.rules.map { it.id to it.name }, selectedRule, { selectedRule = it })
        }
        if (reason == EndReason.OTHER) AyunoTextField(stringResource(R.string.field_end_detail), detail, { detail = it })
        Text(stringResource(R.string.end_cannot_resume), style = MaterialTheme.typography.bodyLarge)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.end_title), enabled = reason != null, onClick = {
            viewModel.end(EndFast.Input(fastId, endAt, reason!!, detail, selectedRule))
        })
    }
}
