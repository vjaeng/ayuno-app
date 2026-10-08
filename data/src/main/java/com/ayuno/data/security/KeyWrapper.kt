package com.ayuno.data.security

/** Cifra y descifra material de clave con una clave que no sale del almacén seguro. */
interface KeyWrapper {
    fun wrap(plain: ByteArray): ByteArray
    fun unwrap(wrapped: ByteArray): ByteArray
}
