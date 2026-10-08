package com.ayuno.app.e2e

import android.view.WindowManager
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Esqueleto: navegación global de 4 pestañas y pantalla protegida contra capturas (ADR-SEG-002). */
@RunWith(AndroidJUnit4::class)
class NavigationShellE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `las cuatro pestanas estan disponibles e Inicio es la inicial`() {
        listOf("Inicio", "Hoy", "Historial", "Más").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Inicio").assertIsSelected()
    }

    @Test
    fun `tocar una pestana la selecciona`() {
        compose.onNodeWithText("Hoy").performClick()
        compose.onNodeWithText("Hoy").assertIsSelected()
    }

    @Test
    fun `la ventana lleva FLAG_SECURE`() {
        val flags = compose.activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }
}
