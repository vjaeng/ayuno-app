package com.ayuno.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Candado de esquemas: una versión instalada en un teléfono no puede cambiar. Si se modifica una entidad sin subir
 * la versión, Room regenera `schemas/…/N.json` con otro identificador y esta prueba falla (evita el cierre al abrir
 * que ocurrió al instalar la tanda 2a sobre la tanda 1). Al publicar una versión nueva, añadir aquí su identificador.
 */
@RunWith(AndroidJUnit4::class)
class ReleasedSchemasTest {

    private val released = mapOf(
        1 to "51a4ff4c3231f1dc5b086725bb887340", // tanda 1 — primera instalación real
        2 to "990883c8c7ecb51462bd7224d57d1ce7", // tanda 2a
    )

    @Test
    fun `los esquemas de versiones instaladas no cambian`() {
        val assets = ApplicationProvider.getApplicationContext<android.content.Context>().assets
        released.forEach { (version, hash) ->
            val json = assets.open("com.ayuno.data.db.AyunoDatabase/$version.json").bufferedReader().use { it.readText() }
            val actual = Regex("\"identityHash\":\\s*\"([0-9a-f]+)\"").find(json)!!.groupValues[1]
            assertEquals("El esquema de la versión $version cambió sin subir la versión de la base", hash, actual)
        }
    }
}
