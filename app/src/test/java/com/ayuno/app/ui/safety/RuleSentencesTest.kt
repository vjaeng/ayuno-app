package com.ayuno.app.ui.safety

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.TestAyunoApplication
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.TrendOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/** H-028 (EV-006): las condiciones nuevas se leen como frase completa en Seguridad. */
@RunWith(AndroidJUnit4::class)
class RuleSentencesTest {

    private val app: TestAyunoApplication = ApplicationProvider.getApplicationContext()
    private val types: List<MetricType> = runBlocking { app.container.catalog.metricTypes() }
    private val intakes: List<IntakeType> = runBlocking { app.container.catalog.intakeTypes() }

    private fun rule(kind: RuleConditionKind, direction: RuleDirection?, threshold: Double?, action: RuleAction = RuleAction.NONE, minutes: Int? = null) = Rule(
        "r", "Regla", true, kind, "blood_pressure", BpComponent.PULSE, threshold, null, null, null, null, emptyList(), false, null,
        RuleLevel.NOTICE, "Mensaje", action, minutes, direction = direction,
    )

    private fun describe(rule: Rule) = RuleSentences.describe(app, rule, types, emptyList(), intakes)

    @Test
    fun `frases de diferencia de pie, partida, tendencia y porcentaje de la meta`() {
        val sentences = listOf(
            describe(rule(RuleConditionKind.PAIR_DIFFERENCE, RuleDirection.ABOVE, 30.0, RuleAction.SCHEDULE_MEASUREMENT, 30)),
            describe(rule(RuleConditionKind.CHANGE_FROM_BASELINE, RuleDirection.ABOVE, 15.0).copy(filterPosition = Position.SITTING)),
            describe(rule(RuleConditionKind.TREND, RuleDirection.ABOVE, null).copy(trendCount = 3, trendOf = TrendOf.VALUE)),
            describe(
                rule(RuleConditionKind.INTAKE_BELOW_GOAL, null, null).copy(metricTypeCode = null, bpComponent = null, intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0), goalPercent = 50.0),
            ),
        )
        assertEquals(
            listOf(
                "Si tu pulso sube más de 30 al ponerte de pie: aviso amarillo y volver a medir en 30 minutos.",
                "Si tu pulso sentado queda más de 15 por encima de tu valor de partida: aviso amarillo.",
                "Si tu pulso sube en 3 lecturas seguidas: aviso amarillo.",
                "Si a las 14:00 llevas menos del 50 % de tu meta de sal: aviso amarillo.",
            ),
            sentences,
        )
    }
}
