package com.ayuno.app

import com.ayuno.app.di.AppContainer
import com.ayuno.data.db.AyunoDatabaseFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Reloj de prueba avanzable, en hora de Colombia. */
class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("-05:00")
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
    fun advanceMinutes(minutes: Long) { now = now.plusSeconds(minutes * 60) }
}

/** Aplicación de prueba: base en memoria sin cifrar (Robolectric no carga SQLCipher) y reloj controlado. */
class TestAyunoApplication : AyunoApplication() {
    val clock = MutableClock(Instant.parse("2026-10-22T23:00:00Z")) // jueves 22-oct 18:00 local

    override fun createContainer(): AppContainer =
        AppContainer(this, clock) { context, c -> AyunoDatabaseFactory.createInMemoryForTests(context, c) }
}
