package com.ayuno.domain.usecase

import com.ayuno.domain.derived.FastClock
import com.ayuno.domain.model.ExportRequest
import com.ayuno.domain.model.ExportedFile
import com.ayuno.domain.model.ImportCheck
import com.ayuno.domain.model.ImportPreview
import com.ayuno.domain.model.ImportProblem
import com.ayuno.domain.model.ImportResult
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.PersonProfile
import com.ayuno.domain.port.DataExchange
import com.ayuno.domain.port.FastRepository
import com.ayuno.domain.port.PersonProfileRepository
import com.ayuno.domain.port.SettingsRepository
import com.ayuno.domain.time.LocalTimes
import java.time.Clock
import java.time.LocalDate

/** Nombres de archivo de la exportación (H-036 y decisiones de UX aceptadas). */
object ExportNames {
    /** `ayuno_<fecha del inicio real>_<8 primeros caracteres del id>.json`; sin inicio real, la del inicio previsto. */
    fun forFast(fast: Fast): String = "ayuno_${FastClock.effectiveStart(fast).toLocalDate()}_${fast.id.take(8)}.json"

    fun forAll(today: LocalDate): String = "ayuno_todo_$today.json"

    /** H-037: el comprimido de tablas lleva el mismo nombre con `.zip`. */
    fun tablesOf(jsonName: String): String = jsonName.removeSuffix(".json") + "_tablas.zip"
}

/**
 * H-036 y H-038: exportar un ayuno ([fastId]) o todo (nulo). El bloque de datos personales va vacío salvo que la
 * persona lo haya activado. Solo genera el archivo: entregarlo lo decide la persona en la hoja de compartir.
 */
class ExportData(
    private val fasts: FastRepository,
    private val settings: SettingsRepository,
    private val person: PersonProfileRepository,
    private val exchange: DataExchange,
    private val clock: Clock,
    private val appVersion: String,
) {
    suspend operator fun invoke(fastId: String?): Outcome<ExportedFile> {
        val fast = fastId?.let { fasts.get(it)?.fast ?: return invalid(ValidationError.FAST_NOT_FOUND) }
        if (fast == null && fasts.all().isEmpty()) return invalid(ValidationError.NOTHING_TO_EXPORT)
        val now = LocalTimes.now(clock)
        val profile = if (settings.get().exportPersonEnabled) person.get() else PersonProfile()
        val request = ExportRequest(fast?.id, profile, appVersion, now, fast?.tzOffset ?: offsetText(clock))
        val name = fast?.let(ExportNames::forFast) ?: ExportNames.forAll(now.toLocalDate())
        return Outcome.Ok(ExportedFile(name, exchange.export(request)))
    }
}

/** H-038: datos personales y si viajan en el archivo (por defecto, no). */
class SavePersonProfile(private val person: PersonProfileRepository, private val settings: SettingsRepository) {
    suspend operator fun invoke(profile: PersonProfile, includeInExport: Boolean): Outcome<PersonProfile> {
        val clean = PersonProfile(
            fullName = profile.fullName?.trim()?.ifEmpty { null },
            idNumber = profile.idNumber?.trim()?.ifEmpty { null },
            birthDate = profile.birthDate,
        )
        person.save(clean)
        settings.save(settings.get().copy(exportPersonEnabled = includeInExport))
        return Outcome.Ok(clean)
    }
}

/** Límite de tamaño antes de leer un archivo a importar (ADR-SEG-006). */
const val IMPORT_MAX_BYTES: Int = 50 * 1024 * 1024

/** H-039, H-040: qué traería el archivo, sin escribir nada. */
class PreviewImport(private val exchange: DataExchange) {
    suspend operator fun invoke(content: ByteArray): ImportCheck<ImportPreview> =
        if (content.size > IMPORT_MAX_BYTES) ImportCheck.Rejected(ImportProblem.TooLarge) else exchange.preview(content)
}

/**
 * H-039, H-040: importar todo lo que trae el archivo (plantillas y productos, o una copia completa), fusionando
 * por `id` o `code`: gana la fila con `updated_at` más reciente. Todo o nada.
 */
class ImportData(private val exchange: DataExchange) {
    suspend operator fun invoke(content: ByteArray): ImportCheck<ImportResult> =
        if (content.size > IMPORT_MAX_BYTES) ImportCheck.Rejected(ImportProblem.TooLarge) else exchange.import(content)
}
