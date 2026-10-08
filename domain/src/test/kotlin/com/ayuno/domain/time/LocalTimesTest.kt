package com.ayuno.domain.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class LocalTimesTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-23T12:01:40.789Z"), ZoneOffset.ofHours(-5))

    @Test
    fun `ahora se toma del reloj inyectado en hora local y truncado a segundos`() {
        assertEquals(LocalDateTime.of(2026, 10, 23, 7, 1, 40), LocalTimes.now(clock))
    }

    @Test
    fun `el formato persistido es hora local sin zona con segundos`() {
        assertEquals("2026-10-23T07:02:15", LocalTimes.format(LocalDateTime.of(2026, 10, 23, 7, 2, 15, 999_000_000)))
    }

    @Test
    fun `formatear y leer conserva la hora al segundo`() {
        val value = LocalDateTime.of(2026, 10, 23, 0, 0, 5)
        assertEquals(value, LocalTimes.parse(LocalTimes.format(value)))
    }

    @Test
    fun `el orden del texto coincide con el orden temporal`() {
        val earlier = LocalTimes.format(LocalDateTime.of(2026, 10, 23, 9, 59, 59))
        val later = LocalTimes.format(LocalDateTime.of(2026, 10, 23, 10, 0, 0))
        assert(earlier < later)
    }
}
