package com.ayuno.app.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.EmptyState
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Template
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** H-001 (lista) y H-003 (duplicar, borrar y restaurar). */
class TemplateListViewModel(private val container: AppContainer) : ViewModel() {
    val templates: StateFlow<List<Template>?> = container.templates.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val deleted: StateFlow<List<Template>> = container.templates.observeDeleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun duplicate(id: String, copyName: String) = viewModelScope.launch { container.duplicateTemplate(id, copyName) }
    fun setDeleted(id: String, deleted: Boolean) = viewModelScope.launch { container.setTemplateDeleted(id, deleted) }
}

@Composable
fun TemplateListScreen(onBack: () -> Unit, onEdit: (id: String?) -> Unit, viewModel: TemplateListViewModel = viewModel(factory = containerFactory { TemplateListViewModel(it) })) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Template?>(null) }
    var showDeleted by remember { mutableStateOf(false) }
    val copyPrefix = stringResource(R.string.templates_copy_of)
    ScreenScaffold(stringResource(R.string.templates_title), onBack) {
        val list = templates ?: run { LoadingState(); return@ScreenScaffold }
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.templates_empty), stringResource(R.string.templates_create)) { onEdit(null) }
        } else {
            list.forEach { t ->
                ListRow(t.name, stringResource(R.string.templates_target, Formats.number(t.targetHours)), { onEdit(t.id) }) {
                    TemplateMenu(
                        name = t.name,
                        onDuplicate = { viewModel.duplicate(t.id, "$copyPrefix ${t.name}") },
                        onDelete = { deleting = t },
                    )
                }
            }
            SecondaryButton(stringResource(R.string.templates_create), onClick = { onEdit(null) })
        }
        if (deleted.isNotEmpty()) {
            ListRow(stringResource(R.string.templates_deleted, deleted.size), null, onClick = { showDeleted = !showDeleted })
            if (showDeleted || list.isEmpty()) {
                SectionTitle(stringResource(R.string.templates_deleted_title))
                deleted.forEach { t ->
                    ListRow(t.name, stringResource(R.string.templates_target, Formats.number(t.targetHours)), onClick = {}) {
                        TextButton(onClick = { viewModel.setDeleted(t.id, false) }) { Text(stringResource(R.string.templates_restore)) }
                    }
                }
            }
        }
    }

    deleting?.let { t ->
        // DialogoConfirmacion: la acción segura (Cancelar) a la derecha.
        AlertDialog(
            onDismissRequest = { deleting = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = { Text(stringResource(R.string.templates_delete_question, t.name)) },
            dismissButton = { TextButton(onClick = { viewModel.setDeleted(t.id, true); deleting = null }) { Text(stringResource(R.string.templates_delete)) } },
            confirmButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TemplateMenu(name: String, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(Dimens.touchMin)) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.templates_menu, name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.templates_duplicate)) }, onClick = { open = false; onDuplicate() })
            DropdownMenuItem(text = { Text(stringResource(R.string.templates_delete)) }, onClick = { open = false; onDelete() })
        }
    }
}
