package com.ayuno.domain.model

import java.time.LocalTime

/** `settings` (registro único). Esta entrega usa el silencio nocturno (H-017); el resto llega con H-038 y H-042. */
data class Settings(
    val quietStart: LocalTime? = null,
    val quietEnd: LocalTime? = null,
    val exportPersonEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
) {
    /** ¿[time] cae en el silencio nocturno? Admite franjas que cruzan la medianoche (23:00–06:30). */
    fun isQuiet(time: LocalTime): Boolean {
        val start = quietStart ?: return false
        val end = quietEnd ?: return false
        return if (start <= end) time >= start && time < end else time >= start || time < end
    }
}
