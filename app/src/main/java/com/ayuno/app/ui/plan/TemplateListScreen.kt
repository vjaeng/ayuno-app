package com.ayuno.app.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.model.Template
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TemplateListViewModel(container: AppContainer) : ViewModel() {
    val templates: StateFlow<List<Template>?> = container.templates.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TemplateListScreen(onBack: () -> Unit, onEdit: (id: String?) -> Unit, viewModel: TemplateListViewModel = viewModel(factory = containerFactory { TemplateListViewModel(it) })) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.templates_title), onBack) {
        val list = templates ?: run { LoadingState(); return@ScreenScaffold }
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.templates_empty), stringResource(R.string.templates_create)) { onEdit(null) }
        } else {
            list.forEach { t -> ListRow(t.name, stringResource(R.string.templates_target, Formats.number(t.targetHours)), { onEdit(t.id) }) }
            SecondaryButton(stringResource(R.string.templates_create), onClick = { onEdit(null) })
        }
    }
}
