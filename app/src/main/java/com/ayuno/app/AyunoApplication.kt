package com.ayuno.app

import android.app.Application
import com.ayuno.app.di.AppContainer
import com.ayuno.app.reminders.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class AyunoApplication : Application() {
    val container: AppContainer by lazy { createContainer() }

    /** Ámbito de la aplicación para trabajo que no pertenece a una pantalla (reprogramar recordatorios). */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    protected open fun createContainer(): AppContainer = AppContainer(this)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannels(this)
        appScope.launch { com.ayuno.app.exchange.ExportFiles.clear(this@AyunoApplication) }
        appScope.launch { container.reminders.reschedule() }
    }
}
