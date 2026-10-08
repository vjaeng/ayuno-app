package com.ayuno.data.db

import android.content.Context
import androidx.room.Room
import com.ayuno.data.security.AndroidKeystoreKeyWrapper
import com.ayuno.data.security.DatabaseKeyStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/** Abre la base cifrada con SQLCipher; la clave vive envuelta por Android Keystore (ADR-SEG-001). */
object AyunoDatabaseFactory {
    private const val DATABASE_NAME = "ayuno.db"
    private const val KEY_FILE_NAME = "db_key.bin"

    fun create(context: Context): AyunoDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyStore(
            file = File(context.noBackupFilesDir, KEY_FILE_NAME),
            wrapper = AndroidKeystoreKeyWrapper(),
        ).getOrCreatePassphrase()
        return Room.databaseBuilder(context, AyunoDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
