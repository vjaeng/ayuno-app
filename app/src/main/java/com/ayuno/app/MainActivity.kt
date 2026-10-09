package com.ayuno.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ayuno.app.reminders.ReminderNotifier
import com.ayuno.app.ui.navigation.AyunoApp
import com.ayuno.app.ui.navigation.DeepLink
import com.ayuno.domain.derived.PendingKind
import com.ayuno.app.ui.theme.AyunoTheme
import com.ayuno.domain.time.LocalTimes

class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Todas las pantallas muestran o pueden mostrar datos de salud (ADR-SEG-002, Q-04).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        deepLink = intent.toDeepLink()
        setContent {
            AyunoTheme {
                AyunoApp(deepLink, onDeepLinkHandled = { deepLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toDeepLink()?.let { deepLink = it }
    }

    private fun Intent.toDeepLink(): DeepLink? {
        getStringExtra(ReminderNotifier.EXTRA_ALERT_ID)?.let { return DeepLink.OpenAlert(it) }
        val fastId = getStringExtra(ReminderNotifier.EXTRA_FAST_ID) ?: return null
        val code = getStringExtra(ReminderNotifier.EXTRA_CODE) ?: return null
        val at = getStringExtra(ReminderNotifier.EXTRA_SCHEDULED_FOR)?.let { runCatching { LocalTimes.parse(it) }.getOrNull() } ?: return null
        return if (getStringExtra(ReminderNotifier.EXTRA_KIND) == PendingKind.PRODUCT.name) DeepLink.Product(fastId, at) else DeepLink.Record(fastId, code, at)
    }
}
