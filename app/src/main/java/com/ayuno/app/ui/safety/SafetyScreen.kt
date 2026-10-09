package com.ayuno.app.ui.safety

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.containerFactory
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Rule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SafetyState(val fastId: String?, val rules: List<Rule>, val types: List<MetricType>)

class SafetyViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<SafetyState?> = combine(container.fasts.observeFasts(), container.catalog.observeMetricTypes()) { fasts, types ->
        val current = fasts.firstOrNull { it.status == FastStatus.IN_PROGRESS }
        val rules = current?.let { container.fasts.get(it.id)?.plan?.rules }.orEmpty().filter { it.active }
        SafetyState(current?.id, rules, types)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** H-028: reglas activas en lenguaje llano, siempre a un toque, y terminar el ayuno desde aquí. */
@Composable
fun SafetyScreen(onBack: () -> Unit, onEndFast: (fastId: String) -> Unit, viewModel: SafetyViewModel = viewModel(factory = containerFactory { SafetyViewModel(it) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ScreenScaffold(stringResource(R.string.safety_title), onBack) {
        val s = state ?: run { LoadingState(); return@ScreenScaffold }
        SectionTitle(stringResource(R.string.safety_active_rules))
        if (s.rules.isEmpty()) {
            EmptyState(stringResource(R.string.safety_no_rules))
        } else {
            s.rules.forEach { Text("• " + RuleSentences.describe(context, it, s.types), style = MaterialTheme.typography.bodyLarge) }
        }
        s.fastId?.let { id -> PrimaryButton(stringResource(R.string.end_title), onClick = { onEndFast(id) }) }
    }
}
