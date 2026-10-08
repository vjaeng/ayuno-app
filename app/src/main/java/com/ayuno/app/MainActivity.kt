package com.ayuno.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ayuno.app.ui.navigation.AyunoApp
import com.ayuno.app.ui.theme.AyunoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Todas las pantallas muestran o pueden mostrar datos de salud (ADR-SEG-002, Q-04).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            AyunoTheme {
                AyunoApp()
            }
        }
    }
}
