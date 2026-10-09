package com.ayuno.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.MetricType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecordGridViewModel(container: AppContainer, fastId: String) : ViewModel() {
    /** Mediciones activas del plan del ayuno y del catálogo (H-001: una desactivada no aparece). */
    val metrics: StateFlow<List<MetricType>?> = container.catalog.observeMetricTypes()
        .map { types ->
            val planned = container.fasts.get(fastId)?.plan?.metrics?.map { it.metricTypeCode }.orEmpty()
            planned.mapNotNull { code -> types.firstOrNull { it.code == code && it.active } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** H-008: rejilla de botones grandes con las mediciones activas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordGridScreen(fastId: String, onBack: () -> Unit, onSelect: (MetricType) -> Unit) {
    val viewModel: RecordGridViewModel = viewModel(factory = containerFactory { RecordGridViewModel(it, fastId) })
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.record_title), onBack) {
        when (val list = metrics) {
            null -> LoadingState()
            emptyList<MetricType>() -> EmptyState(stringResource(R.string.record_empty))
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM), verticalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
                list.forEach { type -> Tile(type.name) { onSelect(type) } }
            }
        }
    }
}

@Composable
private fun Tile(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = AyunoThemeTokens.colors.surfaceVariant,
        shape = RoundedCornerShape(Dimens.radiusM),
        modifier = Modifier.size(Dimens.tileSize),
    ) {
        Box(Modifier.padding(Dimens.spaceS), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
