package com.ayuno.data.security

import java.io.File
import java.security.SecureRandom

/**
 * Clave aleatoria de 256 bits de la base, generada una sola vez y guardada envuelta en un archivo de
 * `noBackupFilesDir` (no viaja en copias del sistema). Perder el Keystore hace la base irrecuperable:
 * por eso existen la exportación y la copia cifrada.
 */
class DatabaseKeyStore(
    private val file: File,
    private val wrapper: KeyWrapper,
    private val random: SecureRandom = SecureRandom(),
) {
    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        if (file.exists()) return wrapper.unwrap(file.readBytes())
        val passphrase = ByteArray(KEY_BYTES).also(random::nextBytes)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(wrapper.wrap(passphrase))
        check(temp.renameTo(file)) { "No se pudo guardar la clave de la base" }
        return passphrase
    }

    private companion object {
        const val KEY_BYTES = 32
    }
}
