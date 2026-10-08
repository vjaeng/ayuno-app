package com.ayuno.domain.port

/**
 * Unidad de trabajo: todo lo que ocurre dentro de [inTransaction] se confirma junto o no se confirma.
 * Las operaciones multi-entidad (transacciones clave) la usan siempre.
 */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
