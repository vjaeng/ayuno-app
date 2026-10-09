package com.ayuno.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class RoomTransactionRunnerTest {

    private lateinit var database: AyunoDatabase
    private lateinit var runner: RoomTransactionRunner

    @Before
    fun setUp() {
        database = AyunoDatabaseFactory.createInMemoryForTests(ApplicationProvider.getApplicationContext(), Clock.systemDefaultZone())
        runner = RoomTransactionRunner(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `confirma lo escrito cuando el bloque termina bien`() = runTest {
        runner.inTransaction { database.settingsDao().upsert(SettingsEntity(appLockEnabled = true)) }
        assertEquals(true, database.settingsDao().get()?.appLockEnabled)
    }

    @Test
    fun `revierte todo lo escrito si el bloque falla`() = runTest {
        runCatching {
            runner.inTransaction {
                database.settingsDao().upsert(SettingsEntity(appLockEnabled = true))
                error("fallo a mitad")
            }
        }
        assertNull(database.settingsDao().get())
    }
}
