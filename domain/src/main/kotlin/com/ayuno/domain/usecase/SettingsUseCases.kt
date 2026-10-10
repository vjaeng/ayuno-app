package com.ayuno.domain.usecase

import com.ayuno.domain.model.Settings
import com.ayuno.domain.port.SettingsRepository
import java.time.LocalTime

/** H-017: silencio nocturno configurable (vacío = sin silencio). */
class SaveQuietHours(private val settings: SettingsRepository) {
    suspend operator fun invoke(start: LocalTime?, end: LocalTime?): Outcome<Settings> {
        if (start != null && end == null) return invalid(ValidationError.QUIET_END_REQUIRED)
        val updated = settings.get().copy(quietStart = start, quietEnd = end.takeIf { start != null })
        settings.save(updated)
        return Outcome.Ok(updated)
    }
}
