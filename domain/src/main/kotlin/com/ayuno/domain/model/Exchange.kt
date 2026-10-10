package com.ayuno.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/** `person_profile` (H-038, registro único): solo viaja en el archivo si `settings.export_person_enabled`. */
data class PersonProfile(val fullName: String? = null, val idNumber: String? = null, val birthDate: LocalDate? = null)

/** Qué exportar (H-036): un ayuno ([fastId]) o todo (nulo). La cabecera la decide el caso de uso. */
data class ExportRequest(
    val fastId: String?,
    val person: PersonProfile,
    val appVersion: String,
    val exportedAt: LocalDateTime,
    val tzOffset: String,
)

/** Archivo listo para entregar: nombre y contenido JSON (UTF-8 sin BOM). */
data class ExportedFile(val fileName: String, val json: String)

/** Recuento de una lista del archivo frente a lo que ya hay en el teléfono (por `id` o por `code`). */
data class ImportCount(val total: Int, val new: Int)

/** Resumen previo a importar (H-039, H-040): nada se ha escrito todavía. */
data class ImportPreview(
    val counts: Map<String, ImportCount>,
    /** Claves desconocidas, como `lista.campo` (se importa igual y se ignoran). */
    val ignoredFields: List<String>,
)

/** Resultado de importar: filas nuevas, actualizadas y conservadas (la del teléfono era más reciente). */
data class ImportResult(val created: Int, val updated: Int, val keptLocal: Int, val ignoredFields: List<String>)

/** Por qué se rechaza un archivo; en todos los casos no se modifica nada (ADR-SEG-006). */
sealed interface ImportProblem {
    data object TooLarge : ImportProblem
    data object NotOurFile : ImportProblem
    data object Empty : ImportProblem
    data class NewerVersion(val version: String) : ImportProblem

    /** Primera fila inválida: lista, posición (desde 1) y campo. */
    data class InvalidRow(val list: String, val index: Int, val field: String) : ImportProblem
}

sealed interface ImportCheck<out T> {
    data class Ok<T>(val value: T) : ImportCheck<T>
    data class Rejected(val problem: ImportProblem) : ImportCheck<Nothing>
}
