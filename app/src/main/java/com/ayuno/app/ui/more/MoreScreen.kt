package com.ayuno.app.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ayuno.app.R
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.ScreenScaffold

@Composable
fun MoreScreen(onTemplates: () -> Unit, onCatalogs: () -> Unit, onSettings: () -> Unit) {
    ScreenScaffold(stringResource(R.string.tab_more), onBack = null) {
        ListRow(stringResource(R.string.templates_title), stringResource(R.string.more_templates_hint), onTemplates)
        ListRow(stringResource(R.string.catalogs_title), stringResource(R.string.more_catalogs_hint), onCatalogs)
        ListRow(stringResource(R.string.settings_title), stringResource(R.string.more_settings_hint), onSettings)
    }
}
