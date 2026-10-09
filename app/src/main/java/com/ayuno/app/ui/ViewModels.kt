package com.ayuno.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ayuno.app.AyunoApplication
import com.ayuno.app.di.AppContainer

/** Fábrica de ViewModels con acceso a la raíz de composición (ADR-009). */
inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as AyunoApplication
            return create(app.container) as T
        }
    }
