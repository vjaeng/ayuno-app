package com.ayuno.app

import android.app.Application
import com.ayuno.app.di.AppContainer

class AyunoApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
