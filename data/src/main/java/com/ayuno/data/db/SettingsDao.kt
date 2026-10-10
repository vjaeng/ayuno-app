package com.ayuno.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): kotlinx.coroutines.flow.Flow<SettingsEntity?>

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}
