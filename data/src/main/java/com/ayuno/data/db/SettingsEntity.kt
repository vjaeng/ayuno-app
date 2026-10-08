package com.ayuno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** `settings` — registro único (MODELO-DATOS §2.4). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "quiet_start") val quietStart: String? = null,
    @ColumnInfo(name = "quiet_end") val quietEnd: String? = null,
    @ColumnInfo(name = "export_person_enabled") val exportPersonEnabled: Boolean = false,
    @ColumnInfo(name = "app_lock_enabled") val appLockEnabled: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
