package com.ayuno.app.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayuno.app.R
import com.ayuno.app.ui.catalog.MetricCatalogScreen
import com.ayuno.app.ui.catalog.MetricTypeEditorScreen
import com.ayuno.app.ui.fast.CreateFastScreen
import com.ayuno.app.ui.fast.EndFastScreen
import com.ayuno.app.ui.home.HomeActions
import com.ayuno.app.ui.home.HomeScreen
import com.ayuno.app.ui.more.MoreScreen
import com.ayuno.app.ui.plan.TemplateEditorScreen
import com.ayuno.app.ui.plan.TemplateListScreen
import com.ayuno.app.ui.record.BloodPressureFormScreen
import com.ayuno.app.ui.record.MeasurementFormScreen
import com.ayuno.app.ui.record.RecordGridScreen
import com.ayuno.app.ui.safety.AlertScreen
import com.ayuno.app.ui.safety.SafetyScreen
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.time.LocalTimes
import java.time.LocalDateTime

/** Pestañas de la navegación global (`specs/UX-UI/README.md`). */
enum class TopLevelDestination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    TODAY("today", R.string.tab_today, Icons.Filled.DateRange),
    HISTORY("history", R.string.tab_history, Icons.AutoMirrored.Filled.List),
    MORE("more", R.string.tab_more, Icons.Filled.Menu),
}

/** Formulario a abrir desde una notificación de recordatorio. */
data class RecordRequest(val fastId: String, val metricCode: String, val scheduledFor: LocalDateTime)

private object Routes {
    const val NEW_FAST = "fast/new"
    const val RECORD = "record/{fastId}"
    const val MEASURE = "measure/{fastId}/{metric}?scheduledFor={scheduledFor}"
    const val ALERT = "alert/{alertId}"
    const val END = "end/{fastId}?ruleId={ruleId}"
    const val SAFETY = "safety"
    const val TEMPLATES = "templates"
    const val TEMPLATE = "template?id={id}"
    const val METRICS = "catalog/metrics"
    const val METRIC = "catalog/metric?code={code}"

    fun record(fastId: String) = "record/$fastId"
    fun measure(fastId: String, metric: String, scheduledFor: LocalDateTime?) =
        "measure/$fastId/$metric" + (scheduledFor?.let { "?scheduledFor=" + Uri.encode(LocalTimes.format(it)) } ?: "")
    fun alert(id: String) = "alert/$id"
    fun end(fastId: String, ruleId: String? = null) = "end/$fastId" + (ruleId?.let { "?ruleId=$it" } ?: "")
    fun template(id: String?) = "template" + (id?.let { "?id=$it" } ?: "")
    fun metric(code: String?) = "catalog/metric" + (code?.let { "?code=$it" } ?: "")
}

private fun optional(name: String) = navArgument(name) { type = NavType.StringType; nullable = true; defaultValue = null }

@Composable
fun AyunoApp(recordRequest: RecordRequest? = null, onRecordRequestHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showTabs = TopLevelDestination.entries.any { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true }

    LaunchedEffect(recordRequest) {
        recordRequest?.let {
            navController.navigate(Routes.measure(it.fastId, it.metricCode, it.scheduledFor))
            onRecordRequestHandled()
        }
    }

    val afterSaving: (List<Alert>) -> Unit = { alerts ->
        if (alerts.isNotEmpty()) {
            navController.navigate(Routes.alert(alerts.first().id)) { popUpTo(TopLevelDestination.HOME.route) }
        } else {
            navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false)
        }
    }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = { navController.navigateToTab(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = TopLevelDestination.HOME.route, modifier = Modifier.padding(innerPadding)) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    HomeActions(
                        onNewFast = { navController.navigate(Routes.NEW_FAST) },
                        onRecord = { navController.navigate(Routes.record(it)) },
                        onRecordMetric = { fastId, metric, at -> navController.navigate(Routes.measure(fastId, metric, at)) },
                        onSafety = { navController.navigate(Routes.SAFETY) },
                        onEndFast = { navController.navigate(Routes.end(it)) },
                        onAlert = { navController.navigate(Routes.alert(it)) },
                    ),
                )
            }
            composable(TopLevelDestination.TODAY.route) { PendingSection(stringResource(R.string.tab_today)) }
            composable(TopLevelDestination.HISTORY.route) { PendingSection(stringResource(R.string.tab_history)) }
            composable(TopLevelDestination.MORE.route) {
                MoreScreen(onTemplates = { navController.navigate(Routes.TEMPLATES) }, onMetricCatalog = { navController.navigate(Routes.METRICS) })
            }
            composable(Routes.NEW_FAST) {
                CreateFastScreen(onBack = { navController.popBackStack() }, onCreated = { navController.popBackStack() })
            }
            composable(Routes.RECORD) { entry ->
                val fastId = entry.arguments?.getString("fastId")!!
                RecordGridScreen(fastId, onBack = { navController.popBackStack() }) { type ->
                    navController.navigate(Routes.measure(fastId, type.code, null))
                }
            }
            composable(Routes.MEASURE, arguments = listOf(optional("scheduledFor"))) { entry ->
                val fastId = entry.arguments?.getString("fastId")!!
                val metric = entry.arguments?.getString("metric")!!
                val scheduledFor = entry.arguments?.getString("scheduledFor")?.let { LocalTimes.parse(it) }
                if (metric == MetricType.BLOOD_PRESSURE_CODE) {
                    BloodPressureFormScreen(fastId, scheduledFor, onBack = { navController.popBackStack() }, onSaved = afterSaving)
                } else {
                    MeasurementFormScreen(fastId, metric, scheduledFor, onBack = { navController.popBackStack() }, onSaved = afterSaving)
                }
            }
            composable(Routes.ALERT) { entry ->
                AlertScreen(
                    alertId = entry.arguments?.getString("alertId")!!,
                    onEndFast = { fastId, ruleId -> navController.navigate(Routes.end(fastId, ruleId)) { popUpTo(TopLevelDestination.HOME.route) } },
                    onContinue = { navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false) },
                )
            }
            composable(Routes.END, arguments = listOf(optional("ruleId"))) { entry ->
                EndFastScreen(
                    fastId = entry.arguments?.getString("fastId")!!,
                    ruleId = entry.arguments?.getString("ruleId"),
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack(TopLevelDestination.HOME.route, inclusive = false) },
                )
            }
            composable(Routes.SAFETY) {
                SafetyScreen(onBack = { navController.popBackStack() }, onEndFast = { navController.navigate(Routes.end(it)) })
            }
            composable(Routes.TEMPLATES) {
                TemplateListScreen(onBack = { navController.popBackStack() }, onEdit = { navController.navigate(Routes.template(it)) })
            }
            composable(Routes.TEMPLATE, arguments = listOf(optional("id"))) { entry ->
                TemplateEditorScreen(entry.arguments?.getString("id"), onBack = { navController.popBackStack() })
            }
            composable(Routes.METRICS) {
                MetricCatalogScreen(onBack = { navController.popBackStack() }, onEdit = { navController.navigate(Routes.metric(it)) })
            }
            composable(Routes.METRIC, arguments = listOf(optional("code"))) { entry ->
                MetricTypeEditorScreen(entry.arguments?.getString("code"), onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavController.navigateToTab(destination: TopLevelDestination) = navigate(destination.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Marcador temporal de las pestañas cuyas historias aún no se han construido (Hoy: H-018; Historial: H-033). */
@Composable
private fun PendingSection(title: String) {
    Box(Modifier.fillMaxSize().padding(Dimens.screenPadding), contentAlignment = Alignment.Center) {
        Text(text = "$title\n\n${stringResource(R.string.section_pending)}", style = MaterialTheme.typography.bodyLarge)
    }
}
