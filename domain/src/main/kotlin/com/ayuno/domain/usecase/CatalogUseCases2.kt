package com.ayuno.domain.usecase

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductSchedule
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.port.CatalogRepository
import java.time.LocalTime

/** H-010: catálogo de síntomas editable, con grupo libre y marca de alarma. */
class SaveSymptomType(private val catalog: CatalogRepository) {
    data class Input(val code: String?, val name: String, val groupName: String?, val isAlarm: Boolean)

    suspend operator fun invoke(input: Input): Outcome<SymptomType> {
        val name = input.name.trim()
        val all = catalog.symptomTypes()
        val existing = input.code?.let { c -> all.firstOrNull { it.code == c } ?: return invalid(ValidationError.SYMPTOM_NOT_FOUND) }
        val errors = buildList {
            if (name.isEmpty()) add(ValidationError.NAME_REQUIRED)
            if (all.any { it.code != existing?.code && it.name.equals(name, ignoreCase = true) }) add(ValidationError.NAME_DUPLICATED)
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val saved = SymptomType(
            code = existing?.code ?: uniqueCode(name, all.map { it.code }.toSet()),
            name = name,
            groupName = input.groupName?.trim()?.ifEmpty { null },
            isAlarm = input.isAlarm,
            active = existing?.active ?: true,
        )
        catalog.saveSymptomType(saved)
        return Outcome.Ok(saved)
    }
}

/** H-010: desactivar un síntoma no borra los ya anotados. */
class SetSymptomTypeActive(private val catalog: CatalogRepository) {
    suspend operator fun invoke(code: String, active: Boolean): Outcome<SymptomType> {
        val type = catalog.symptomTypes().firstOrNull { it.code == code } ?: return invalid(ValidationError.SYMPTOM_NOT_FOUND)
        return type.copy(active = active).also { catalog.saveSymptomType(it) }.let { Outcome.Ok(it) }
    }
}

/** H-011: tipos de ingesta editables, con «rompe el ayuno» y factor de sodio. */
class SaveIntakeType(private val catalog: CatalogRepository) {
    data class Input(val code: String?, val name: String, val unit: String, val breaksFast: Boolean, val sodiumFactor: Double?)

    suspend operator fun invoke(input: Input): Outcome<IntakeType> {
        val name = input.name.trim()
        val all = catalog.intakeTypes()
        val existing = input.code?.let { c -> all.firstOrNull { it.code == c } ?: return invalid(ValidationError.INTAKE_TYPE_NOT_FOUND) }
        val errors = buildList {
            if (name.isEmpty()) add(ValidationError.NAME_REQUIRED)
            if (input.unit.isBlank()) add(ValidationError.UNIT_REQUIRED)
            if (input.sodiumFactor != null && input.sodiumFactor <= 0) add(ValidationError.SODIUM_FACTOR_NOT_POSITIVE)
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val saved = IntakeType(
            code = existing?.code ?: uniqueCode(name, all.map { it.code }.toSet()),
            name = name,
            unit = input.unit.trim(),
            breaksFast = input.breaksFast,
            sodiumFactor = input.sodiumFactor,
        )
        catalog.saveIntakeType(saved)
        return Outcome.Ok(saved)
    }
}

/** H-013: lista de productos con código inmutable, dosis, horarios y marca «a evitar». */
class SaveProduct(private val catalog: CatalogRepository) {
    data class Input(
        val existingCode: String?,
        val code: String?,
        val name: String,
        val kind: ProductKind,
        val defaultAmount: Double?,
        val unit: String,
        val avoid: Boolean,
        val schedules: List<Pair<LocalTime, Double>>,
    )

    suspend operator fun invoke(input: Input): Outcome<Product> {
        val name = input.name.trim()
        val all = catalog.products()
        val existing = input.existingCode?.let { c -> all.firstOrNull { it.code == c } ?: return invalid(ValidationError.PRODUCT_NOT_FOUND) }
        val code = existing?.code ?: input.code?.trim()?.ifEmpty { null } ?: uniqueCode(name, all.map { it.code }.toSet())
        val errors = buildList {
            if (name.isEmpty()) add(ValidationError.NAME_REQUIRED)
            if (input.unit.isBlank()) add(ValidationError.UNIT_REQUIRED)
            if (input.defaultAmount == null || input.defaultAmount <= 0 || input.schedules.any { it.second <= 0 }) add(ValidationError.AMOUNT_NOT_POSITIVE)
            if (existing == null && !CODE_PATTERN.matches(code)) add(ValidationError.CODE_INVALID)
            if (existing == null && all.any { it.code == code }) add(ValidationError.CODE_DUPLICATED)
        }
        if (errors.isNotEmpty()) return Outcome.Invalid(errors)
        val previous = existing?.schedules.orEmpty()
        val saved = Product(
            code = code,
            name = name,
            kind = input.kind,
            defaultAmount = input.defaultAmount!!,
            unit = input.unit.trim(),
            avoid = input.avoid,
            active = existing?.active ?: true,
            schedules = input.schedules.distinctBy { it.first }.sortedBy { it.first }.map { (time, amount) ->
                ProductSchedule(previous.firstOrNull { it.timeOfDay == time }?.id ?: Ids.newId(), time, amount)
            },
        )
        catalog.saveProduct(saved)
        return Outcome.Ok(saved)
    }
}

/** H-013: desactivar un producto. */
class SetProductActive(private val catalog: CatalogRepository) {
    suspend operator fun invoke(code: String, active: Boolean): Outcome<Product> {
        val product = catalog.product(code) ?: return invalid(ValidationError.PRODUCT_NOT_FOUND)
        return product.copy(active = active).also { catalog.saveProduct(it) }.let { Outcome.Ok(it) }
    }
}
