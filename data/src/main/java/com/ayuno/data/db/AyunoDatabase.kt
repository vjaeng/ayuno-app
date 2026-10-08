package com.ayuno.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos de la app (ADR-003). Hasta la primera entrega se mantiene en la versión 1 y el esquema
 * crece historia a historia; a partir de la primera instalación real, todo cambio lleva migración probada.
 */
@Database(
    entities = [SettingsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AyunoDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}
