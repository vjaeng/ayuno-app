package com.ayuno.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.ayuno.app.R
import com.ayuno.app.ui.theme.AyunoThemeTokens
import com.ayuno.app.ui.theme.Dimens
import com.ayuno.domain.derived.PendingKind
import com.ayuno.domain.model.MetricType

/**
 * Iconos por tipo de anotación (GUIA §2.1): los mismos que `my-health-tracker` para que un tipo se reconozca igual en
 * las dos apps. Siempre junto al nombre, sin color por tipo.
 */
object EntryIcons {
    @DrawableRes val symptom: Int = R.drawable.ic_entry_symptom

    @DrawableRes val intake: Int = R.drawable.ic_entry_intake

    @DrawableRes val product: Int = R.drawable.ic_entry_product

    @DrawableRes val note: Int = R.drawable.ic_entry_note

    @DrawableRes val meal: Int = R.drawable.ic_entry_meal

    @DrawableRes val refeed: Int = R.drawable.ic_entry_refeed

    @DrawableRes val bloodPressure: Int = R.drawable.ic_entry_blood_pressure

    /** Forma distinta por nivel (triángulo aviso, octógono alerta), como el ▲/■ que sustituye. */
    @DrawableRes
    fun alert(isNotice: Boolean): Int = if (isNotice) R.drawable.ic_entry_notice else R.drawable.ic_entry_alert

    fun metric(type: MetricType): Int = metric(type.code, type.options.isNotEmpty())

    /** Medición por código, con su clase según el catálogo (si no está, numérica). */
    @DrawableRes
    fun metric(code: String, types: Map<String, MetricType>): Int = types[code]?.let { metric(it) } ?: metric(code, isScale = false)

    /** Lo que toca hacer (Hoy, Inicio): su medición, el producto o el paso de la vuelta a comer. */
    @DrawableRes
    fun pending(kind: PendingKind, code: String, types: Map<String, MetricType>): Int = when (kind) {
        PendingKind.METRIC -> metric(code, types)
        PendingKind.PRODUCT -> product
        PendingKind.REFEED -> refeed
    }

    /** Las de fábrica con icono propio; el resto, el genérico de su clase (escala si tiene palabras, si no numérica). */
    @DrawableRes
    fun metric(code: String, isScale: Boolean): Int = when (code) {
        "glucose" -> R.drawable.ic_entry_glucose
        MetricType.BLOOD_PRESSURE_CODE -> bloodPressure
        "weight" -> R.drawable.ic_entry_weight
        "energy" -> R.drawable.ic_entry_energy
        "mood" -> R.drawable.ic_entry_mood
        "hunger" -> R.drawable.ic_entry_hunger
        else -> if (isScale) R.drawable.ic_entry_scale else R.drawable.ic_entry_numeric
    }
}

/** Icono de tipo, decorativo: el texto de al lado ya lo nombra (GUIA §2.1). */
@Composable
fun EntryIcon(@DrawableRes icon: Int, size: Dp = Dimens.iconRow) {
    Icon(painterResource(icon), contentDescription = null, tint = AyunoThemeTokens.colors.onSurfaceVariant, modifier = Modifier.size(size))
}
