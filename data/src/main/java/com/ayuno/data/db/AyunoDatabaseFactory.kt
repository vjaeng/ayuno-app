package com.ayuno.data.db

import android.content.Context
import androidx.room.Room
import com.ayuno.data.db.AyunoDatabase.Companion.ayunoSetup
import com.ayuno.data.security.AndroidKeystoreKeyWrapper
import com.ayuno.data.security.DatabaseKeyStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.time.Clock

/** Abre la base cifrada con SQLCipher; la clave vive envuelta por Android Keystore (ADR-SEG-001). */
object AyunoDatabaseFactory {
    private const val DATABASE_NAME = "ayuno.db"
    private const val KEY_FILE_NAME = "db_key.bin"

    fun create(context: Context, clock: Clock): AyunoDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyStore(
            file = File(context.noBackupFilesDir, KEY_FILE_NAME),
            wrapper = AndroidKeystoreKeyWrapper(),
        ).getOrCreatePassphrase()
        return Room.databaseBuilder(context, AyunoDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .ayunoSetup(context, clock)
            .build()
    }

    /** Base sin cifrar en memoria, solo para pruebas en la JVM (Robolectric no carga SQLCipher). */
    fun createInMemoryForTests(context: Context, clock: Clock): AyunoDatabase =
        Room.inMemoryDatabaseBuilder(context, AyunoDatabase::class.java)
            .ayunoSetup(context, clock)
            .allowMainThreadQueries()
            .build()
}
