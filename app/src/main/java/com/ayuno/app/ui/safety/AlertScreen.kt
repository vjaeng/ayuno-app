package com.ayuno.app.ui.safety

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlertDetails(
    val alert: Alert,
    val rule: Rule,
    val triggers: List<String>,
    val types: List<MetricType>,
    val symptomTypes: List<com.ayuno.domain.model.SymptomType>,
    val intakeTypes: List<com.ayuno.domain.model.IntakeType>,
)

class AlertViewModel(private val container: AppContainer, private val alertId: String) : ViewModel() {
    private val _details = MutableStateFlow<AlertDetails?>(null)
    val details: StateFlow<AlertDetails?> = _details.asStateFlow()
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    init {
        viewModelScope.launch {
            val alert = container.alerts.get(alertId) ?: return@launch
            val source = container.fasts.get(alert.fastId) ?: return@launch
            val rule = source.ruleById(alert.ruleId) ?: return@launch
            val types = container.catalog.metricTypes()
            val names = types.associate { it.code to it.name }
            val measurements = container.records.measurements(alert.fastId).associateBy { it.id }
            val readings = container.records.bloodPressure(alert.fastId).associateBy { it.id }
            val symptomTypes = container.catalog.symptomTypes()
            val intakeTypes = container.catalog.intakeTypes()
            val symptoms = container.records.symptoms(alert.fastId).associateBy { it.id }
            val intakes = container.records.intakes(alert.fastId).associateBy { it.id }
            val triggers = alert.triggerEntryIds.mapNotNull { id ->
                measurements[id]?.let { m -> "${names[m.metricTypeCode]} ${Formats.measurementValue(m, types.firstOrNull { it.code == m.metricTypeCode }, decimals = 1)} · ${Formats.time(m.takenAt)}" }
                    ?: readings[id]?.let { "${names[MetricType.BLOOD_PRESSURE_CODE]} ${it.systolic}/${it.diastolic} · ${Formats.time(it.takenAt)}" }
                    ?: symptoms[id]?.let { s -> "${symptomTypes.firstOrNull { it.code == s.symptomTypeCode }?.name} ${s.severity}/10 · ${Formats.time(s.takenAt)}" }
                    ?: intakes[id]?.let { i -> "${intakeTypes.firstOrNull { it.code == i.intakeTypeCode }?.name} ${Formats.number(i.amount)} ${i.unit} · ${Formats.time(i.takenAt)}" }
            }
            _details.value = AlertDetails(alert, rule, triggers, types, symptomTypes, intakeTypes)
        }
    }

    fun decide(choice: UserChoice, reason: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = container.decideAlert(alertId, choice, reason)) {
                is Outcome.Ok -> onDone()
                is Outcome.Invalid -> _errors.value = result.errors
            }
        }
    }
}

/**
 * H-027 (y nivel de H-025): aviso amarillo o alerta roja a pantalla completa. No se cierra sin decidir: la app
 * nunca termina el ayuno por su cuenta; «Termino el ayuno» lleva a la pantalla de fin con la regla prellenada.
 */
@Composable
fun AlertScreen(alertId: String, onEndFast: (fastId: String, ruleId: String) -> Unit, onContinue: () -> Unit) {
    val viewModel: AlertViewModel = viewModel(key = alertId, factory = containerFactory { AlertViewModel(it, alertId) })
    val details by viewModel.details.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val colors = AyunoThemeTokens.colors
    BackHandler { /* hay que decidir */ }

    val d = details ?: run { LoadingState(); return }
    LaunchedEffect(d.alert.userChoice) { if (d.alert.userChoice != null) onContinue() }

    val isAlert = d.rule.level == RuleLevel.ALERT
    val background = if (isAlert) colors.alert else colors.noticeContainer
    val content = if (isAlert) colors.onAlert else colors.onNoticeContainer
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    Surface(color = background, contentColor = content, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceL),
        ) {
            Text(
                stringResource(if (isAlert) R.string.alert_title else R.string.notice_title, Formats.dateTime(d.alert.firedAt)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(d.rule.message, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            Text(RuleSentences.describe(context, d.rule, d.types, d.symptomTypes, d.intakeTypes), style = MaterialTheme.typography.bodyLarge)
            if (d.triggers.isNotEmpty()) Text(stringResource(R.string.alert_triggers, d.triggers.joinToString(" · ")), style = MaterialTheme.typography.bodyLarge)

            val endButton: @Composable () -> Unit = {
                DecisionButton(stringResource(R.string.alert_end_fast), filled = true, background, content) {
                    viewModel.decide(UserChoice.END_FAST, null) { onEndFast(d.alert.fastId, d.rule.id) }
                }
            }
            if (d.rule.action == RuleAction.OFFER_END) endButton()
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.alert_continue_reason)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = content, unfocusedTextColor = content, focusedLabelColor = content, unfocusedLabelColor = content,
                    focusedBorderColor = content, unfocusedBorderColor = content, cursorColor = content,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            errors.forEach { FieldError(stringResource(it.message())) }
            DecisionButton(stringResource(R.string.alert_continue), filled = false, background, content) {
                viewModel.decide(UserChoice.CONTINUE, reason, onContinue)
            }
            if (d.rule.action != RuleAction.OFFER_END) endButton()
        }
    }
}

@Composable
private fun DecisionButton(text: String, filled: Boolean, background: Color, content: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.buttonHeight),
        shape = RoundedCornerShape(Dimens.radiusM),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) content else background,
            contentColor = if (filled) background else content,
        ),
        border = if (filled) null else androidx.compose.foundation.BorderStroke(2.dp, content),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

