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
import com.ayuno.app.ui.navigation.RecordRequest
import com.ayuno.app.ui.theme.AyunoTheme
import com.ayuno.domain.time.LocalTimes

class MainActivity : ComponentActivity() {
    private var recordRequest by mutableStateOf<RecordRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Todas las pantallas muestran o pueden mostrar datos de salud (ADR-SEG-002, Q-04).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        recordRequest = intent.toRecordRequest()
        setContent {
            AyunoTheme {
                AyunoApp(recordRequest, onRecordRequestHandled = { recordRequest = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toRecordRequest()?.let { recordRequest = it }
    }

    private fun Intent.toRecordRequest(): RecordRequest? {
        val fastId = getStringExtra(ReminderNotifier.EXTRA_FAST_ID) ?: return null
        val metric = getStringExtra(ReminderNotifier.EXTRA_METRIC_CODE) ?: return null
        val at = getStringExtra(ReminderNotifier.EXTRA_SCHEDULED_FOR)?.let { runCatching { LocalTimes.parse(it) }.getOrNull() } ?: return null
        return RecordRequest(fastId, metric, at)
    }
}
