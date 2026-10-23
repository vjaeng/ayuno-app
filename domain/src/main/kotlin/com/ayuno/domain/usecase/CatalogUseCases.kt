package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.MetricOption
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.port.CatalogRepository
import java.text.Normalizer

/** H-007: editar el catálogo de mediciones y añadir mediciones propias; H-049: sus palabras para elegir. */
class SaveMetricType(private val catalog: CatalogRepository) {

    data class Input(
        val code: String?,
        val name: String,
        val unit: String,
        val isNumeric: Boolean,
        val minValid: Double?,
        val maxValid: Double?,
        /** Palabras en el orden en que se muestran; `null` deja las que hay (que deben caber en el rango nuevo). */
        val options: List<OptionDraft>? = null,
    )

    /** Palabra del editor: `id` nulo si es nueva; un número sin escribir llega como NaN (OPTION_VALUE_REQUIRED). */
    data class OptionDraft(val id: String?, val label: String, val value: Double)

    suspend operator fun invoke(input: Input): Outcome<MetricType> {
        val name = input.name.trim()
        val errors = buildList {
            if (name.isEmpty()) add(ValidationError.NAME_REQUIRED)
            if (input.minValid != null && input.maxValid != null && input.minValid >= input.maxValid) add(ValidationError.RANGE_MIN_NOT_BELOW_MAX)
        }
        val all = catalog.metricTypes()
        val existing = input.code?.let { code -> all.firstOrNull { it.code == code } }
        if (input.code != null && existing == null) return invalid(ValidationError.METRIC_NOT_FOUND)
        if (existing == null && input.unit.isBlank()) return Outcome.Invalid(errors + ValidationError.UNIT_REQUIRED)
        val isNumeric = existing?.isNumeric ?: input.isNumeric
        val code = existing?.code ?: uniqueCode(name, all.map { it.code }.toSet())
        val options = input.options?.mapIndexed { i, d -> MetricOption(d.id ?: Ids.newId(), code, d.label.trim(), d.value, i) }
            ?: existing?.options.orEmpty()
        val optionErrors = validateOptions(options, isNumeric, input.minValid, input.maxValid)
        if (errors.isNotEmpty() || optionErrors.isNotEmpty()) return Outcome.Invalid(errors + optionErrors)

        val saved = if (existing != null) {
            // En los tipos existentes la unidad y la naturaleza numérica no cambian: hay mediciones que dependen de ellas.
            existing.copy(name = name, minValid = input.minValid, maxValid = input.maxValid, options = options)
        } else {
            val unit = input.unit.trim()
            MetricType(
                code = code,
                name = name,
                unit = unit,
                altUnit = null,
                entryUnit = unit,
                isNumeric = input.isNumeric,
                minValid = input.minValid,
                maxValid = input.maxValid,
                active = true,
                options = options,
            )
        }
        catalog.saveMetricType(saved)
        return Outcome.Ok(saved)
    }

}

/** H-049: palabras de un tipo; textos y números sin repetir, dentro del rango y como mucho [MetricType.MAX_OPTIONS]. */
internal fun validateOptions(options: List<MetricOption>, isNumeric: Boolean, min: Double?, max: Double?): List<ValidationError> = buildList {
    if (options.isEmpty()) return@buildList
    if (!isNumeric) add(ValidationError.OPTIONS_NOT_NUMERIC)
    if (options.size > MetricType.MAX_OPTIONS) add(ValidationError.OPTIONS_TOO_MANY)
    if (options.any { it.label.isEmpty() }) add(ValidationError.OPTION_LABEL_REQUIRED)
    if (options.any { it.value.isNaN() }) add(ValidationError.OPTION_VALUE_REQUIRED)
    val labels = options.map { it.label.lowercase() }.filter { it.isNotEmpty() }
    if (labels.size != labels.toSet().size) add(ValidationError.OPTION_LABEL_DUPLICATED)
    if (options.map { it.value }.filterNot { it.isNaN() }.let { it.size != it.toSet().size }) add(ValidationError.OPTION_VALUE_DUPLICATED)
    if (options.any { (min != null && it.value < min) || (max != null && it.value > max) }) add(ValidationError.OPTION_VALUE_OUT_OF_RANGE)
}

/** Código de catálogo a partir de un nombre: minúsculas, sin tildes, `_`; único (estructuras de control). */
internal fun uniqueCode(name: String, taken: Set<String>): String {
    val base = codeFrom(name).ifEmpty { "item" }
    if (base !in taken) return base
    return generateSequence(2) { it + 1 }.map { "${base}_$it" }.first { it !in taken }
}

internal fun codeFrom(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')

internal val CODE_PATTERN = Regex("[a-z0-9_]+")

/** H-007: activar o desactivar un tipo; desactivar no borra lo ya anotado. */
class SetMetricTypeActive(private val catalog: CatalogRepository) {
    suspend operator fun invoke(code: String, active: Boolean): Outcome<MetricType> {
        val type = catalog.metricType(code) ?: return invalid(ValidationError.METRIC_NOT_FOUND)
        val updated = type.copy(active = active)
        catalog.saveMetricType(updated)
        return Outcome.Ok(updated)
    }
}
