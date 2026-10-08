package com.ayuno.domain.usecase

import com.ayuno.domain.model.MetricType
import com.ayuno.domain.port.CatalogRepository
import java.text.Normalizer

/** H-007: editar el catálogo de mediciones y añadir mediciones propias. */
class SaveMetricType(private val catalog: CatalogRepository) {

    data class Input(
        val code: String?,
        val name: String,
        val unit: String,
        val isNumeric: Boolean,
        val minValid: Double?,
        val maxValid: Double?,
    )

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
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)

        val saved = if (existing != null) {
            // En los tipos existentes la unidad y la naturaleza numérica no cambian: hay mediciones que dependen de ellas.
            existing.copy(name = name, minValid = input.minValid, maxValid = input.maxValid)
        } else {
            val unit = input.unit.trim()
            MetricType(
                code = uniqueCode(name, all.map { it.code }.toSet()),
                name = name,
                unit = unit,
                altUnit = null,
                entryUnit = unit,
                isNumeric = input.isNumeric,
                minValid = input.minValid,
                maxValid = input.maxValid,
                active = true,
            )
        }
        catalog.saveMetricType(saved)
        return Outcome.Ok(saved)
    }

    private fun uniqueCode(name: String, taken: Set<String>): String {
        val base = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "medicion" }
        if (base !in taken) return base
        return generateSequence(2) { it + 1 }.map { "${base}_$it" }.first { it !in taken }
    }
}

/** H-007: activar o desactivar un tipo; desactivar no borra lo ya anotado. */
class SetMetricTypeActive(private val catalog: CatalogRepository) {
    suspend operator fun invoke(code: String, active: Boolean): Outcome<MetricType> {
        val type = catalog.metricType(code) ?: return invalid(ValidationError.METRIC_NOT_FOUND)
        val updated = type.copy(active = active)
        catalog.saveMetricType(updated)
        return Outcome.Ok(updated)
    }
}
