package com.ayuno.app.di

import android.content.Context
import com.ayuno.data.db.AyunoDatabase
import com.ayuno.data.db.AyunoDatabaseFactory
import com.ayuno.data.db.RoomTransactionRunner
import com.ayuno.domain.port.TransactionRunner
import java.time.Clock

/**
 * Raíz de composición (ADR-009): único lugar de `:app` que conoce `:data`.
 * Todo se crea de forma perezosa: abrir la app no abre la base hasta que una pantalla la necesita.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    val database: AyunoDatabase by lazy { AyunoDatabaseFactory.create(appContext) }

    val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }
}
