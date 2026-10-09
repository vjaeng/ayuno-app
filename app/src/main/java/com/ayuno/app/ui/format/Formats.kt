package com.ayuno.app.ui.format

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de la guía de estilo (§5): fechas con día de la semana, sin abreviaturas ni siglas. */
object Formats {
    private val spanish: Locale = Locale.forLanguageTag("es")
    private val dateTime = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", spanish)
    private val dateTimeSeconds = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm:ss", spanish)
    private val date = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
    private val time = DateTimeFormatter.ofPattern("HH:mm", spanish)

    fun dateTime(value: LocalDateTime): String = dateTime.format(value)
    fun dateTimeSeconds(value: LocalDateTime): String = dateTimeSeconds.format(value)
    fun date(value: LocalDateTime): String = date.format(value)
    fun time(value: LocalDateTime): String = time.format(value)
    fun time(value: LocalTime): String = time.format(value)

    /** «33 h 12 min». */
    fun duration(value: Duration): String {
        val hours = value.toHours()
        // toMinutesPart() es de API 31; la app soporta desde API 29.
        val minutes = value.toMinutes() % 60
        return if (hours > 0) "$hours h $minutes min" else "$minutes min"
    }

    /** Número sin «.0» sobrante y con coma decimal: 71,4 · 57. */
    fun number(value: Double, decimals: Int = 1): String {
        val rounded = String.format(spanish, "%.${decimals}f", value)
        return if (decimals == 0) rounded else rounded.trimEnd('0').trimEnd(',', '.')
    }

    /** Lee un número escrito con coma o punto decimal. */
    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
}
