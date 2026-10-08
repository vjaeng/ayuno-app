package com.ayuno.domain.time

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Hora local sin zona, al segundo (ADR-004). Toda lectura de «ahora» pasa por un [Clock] inyectado.
 */
object LocalTimes {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")

    fun now(clock: Clock): LocalDateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)

    /** Formato persistido y exportado: `AAAA-MM-DDTHH:MM:SS`, sin zona. */
    fun format(value: LocalDateTime): String = value.truncatedTo(ChronoUnit.SECONDS).format(FORMAT)

    fun parse(text: String): LocalDateTime = LocalDateTime.parse(text, FORMAT)
}
