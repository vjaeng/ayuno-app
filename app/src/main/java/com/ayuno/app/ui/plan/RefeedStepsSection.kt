package com.ayuno.app.ui.plan

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ayuno.app.R
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.usecase.ValidationError
import kotlin.math.roundToInt

/** «a las 6 h» · «desde 0 h · cada 60 min durante 4 h» · «Día 1». */
@Composable
fun refeedStepSummary(step: RefeedStep): String = when (step.kind) {
    RefeedStepKind.REINTRODUCTION -> stringResource(R.string.refeed_day_n, step.dayOffset ?: 1)
    RefeedStepKind.TIMED -> {
        val at = Formats.number((step.offsetMinutes ?: 0) / 60.0, 2)
        val every = step.repeatEveryMinutes
        if (every == null) {
            stringResource(R.string.refeed_at_hours, at)
        } else {
            stringResource(R.string.refeed_repeat, at, every, Formats.number((step.repeatForMinutes ?: 0) / 60.0, 2))
        }
    }
}

/** H-031: pasos tras el fin y reintroducción de la plantilla; se añaden ya validados. */
@Composable
fun RefeedStepsSection(steps: List<RefeedStep>, onAdd: (RefeedStep) -> List<ValidationError>, onRemove: (String) -> Unit) {
    SectionTitle(stringResource(R.string.template_refeed))
    listOf(RefeedStepKind.TIMED to R.string.refeed_timed_title, RefeedStepKind.REINTRODUCTION to R.string.refeed_reintroduction_title).forEach { (kind, title) ->
        val ofKind = steps.filter { it.kind == kind }
        if (ofKind.isNotEmpty()) {
            Text(stringResource(title))
            ofKind.forEach { step ->
                ListRow(step.description, refeedStepSummary(step), onClick = {}) {
                    TextButton(onClick = { onRemove(step.id) }) { Text(stringResource(R.string.action_remove)) }
                }
            }
        }
    }

    var kind by remember { mutableStateOf(RefeedStepKind.TIMED) }
    var description by remember { mutableStateOf("") }
    var atHours by remember { mutableStateOf("") }
    var every by remember { mutableStateOf("") }
    var forHours by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }

    OptionSelector(
        listOf(RefeedStepKind.TIMED to stringResource(R.string.refeed_kind_timed), RefeedStepKind.REINTRODUCTION to stringResource(R.string.refeed_kind_reintroduction)),
        kind,
        { kind = it },
    )
    AyunoTextField(stringResource(R.string.field_refeed_description), description, { description = it })
    if (kind == RefeedStepKind.TIMED) {
        AyunoTextField(stringResource(R.string.field_refeed_at_hours), atHours, { atHours = it }, numeric = true, suffix = stringResource(R.string.unit_hours))
        AyunoTextField(stringResource(R.string.field_refeed_every), every, { every = it }, numeric = true, suffix = stringResource(R.string.unit_minutes))
        if (every.isNotBlank()) {
            AyunoTextField(stringResource(R.string.field_refeed_for_hours), forHours, { forHours = it }, numeric = true, suffix = stringResource(R.string.unit_hours))
        }
    } else {
        AyunoTextField(stringResource(R.string.field_refeed_day), day, { day = it }, numeric = true)
    }
    errors.forEach { FieldError(stringResource(it.message())) }
    SecondaryButton(stringResource(R.string.refeed_add_step), onClick = {
        fun minutes(hours: String) = Formats.parseNumber(hours)?.let { (it * 60).roundToInt() }
        val step = RefeedStep(
            id = Ids.newId(),
            kind = kind,
            description = description,
            offsetMinutes = minutes(atHours).takeIf { kind == RefeedStepKind.TIMED },
            repeatEveryMinutes = Formats.parseNumber(every)?.roundToInt().takeIf { kind == RefeedStepKind.TIMED },
            repeatForMinutes = minutes(forHours).takeIf { kind == RefeedStepKind.TIMED && every.isNotBlank() },
            dayOffset = Formats.parseNumber(day)?.roundToInt().takeIf { kind == RefeedStepKind.REINTRODUCTION },
        )
        errors = onAdd(step)
        if (errors.isEmpty()) { description = ""; atHours = ""; every = ""; forHours = ""; day = "" }
    })
}
