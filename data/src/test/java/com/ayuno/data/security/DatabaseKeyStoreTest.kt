package com.ayuno.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseKeyStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** Envoltorio de prueba: invierte los bytes (Android Keystore no existe en la JVM). */
    private val fakeWrapper = object : KeyWrapper {
        override fun wrap(plain: ByteArray) = plain.reversedArray()
        override fun unwrap(wrapped: ByteArray) = wrapped.reversedArray()
    }

    @Test
    fun `genera una clave de 256 bits la primera vez`() {
        val key = DatabaseKeyStore(File(folder.root, "db_key.bin"), fakeWrapper).getOrCreatePassphrase()
        assertEquals(32, key.size)
    }

    @Test
    fun `devuelve la misma clave en aperturas posteriores`() {
        val file = File(folder.root, "db_key.bin")
        val first = DatabaseKeyStore(file, fakeWrapper).getOrCreatePassphrase()
        val second = DatabaseKeyStore(file, fakeWrapper).getOrCreatePassphrase()
        assertArrayEquals(first, second)
    }

    @Test
    fun `la clave se guarda envuelta, nunca en claro`() {
        val file = File(folder.root, "db_key.bin")
        val key = DatabaseKeyStore(file, fakeWrapper).getOrCreatePassphrase()
        assertFalse(file.readBytes().contentEquals(key))
    }
}
