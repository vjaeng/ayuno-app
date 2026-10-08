package com.ayuno.data.db

import androidx.room.withTransaction
import com.ayuno.domain.port.TransactionRunner

class RoomTransactionRunner(private val database: AyunoDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction { block() }
}
