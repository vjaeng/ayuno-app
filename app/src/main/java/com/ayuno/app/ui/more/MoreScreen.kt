package com.ayuno.app.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ayuno.app.R
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.ScreenScaffold

@Composable
fun MoreScreen(onTemplates: () -> Unit, onMetricCatalog: () -> Unit) {
    ScreenScaffold(stringResource(R.string.tab_more), onBack = null) {
        ListRow(stringResource(R.string.templates_title), stringResource(R.string.more_templates_hint), onTemplates)
        ListRow(stringResource(R.string.catalog_metrics_title), stringResource(R.string.more_metrics_hint), onMetricCatalog)
    }
}
