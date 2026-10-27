package com.ayuno.domain.rules

import com.ayuno.domain.fakes.SeedCatalog
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.TrendOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

/** Tabla de `specs/PRUEBAS.md` §4.1, filas EV-006 (H-022, H-054 a H-056): el motor puro con contextos construidos. */
class Ev006RuleEngineTest {

    private val t0 = LocalDateTime.of(2026, 10, 23, 8, 0)

    private fun bp(
        id: String, minutes: Long, systolic: Int, position: Position = Position.SITTING, paired: String? = null, seconds: Int? = null,
        pulse: Int = 70, baseline: Boolean = false,
    ) = BloodPressureReading(id, "f", t0.plusMinutes(minutes), null, systolic, 60, pulse, position, false, paired, null, null, seconds, baseline)

    /** Pareja sentada → de pie: dos lecturas con la de pie enlazada y sus segundos. */
    private fun pair(n: Int, minutes: Long, sitting: Int, standing: Int, seconds: Int = 60) = listOf(
        bp("s$n", minutes, sitting),
        bp("p$n", minutes + 2, standing, Position.STANDING, paired = "s$n", seconds = seconds),
    )

    private fun rule(
        kind: RuleConditionKind, direction: RuleDirection?, threshold: Double?, component: BpComponent? = BpComponent.SYSTOLIC,
        metric: String? = "blood_pressure", trendCount: Int? = null, trendOf: TrendOf? = null, position: Position? = null,
    ) = Rule(
        "r", "Regla", true, kind, metric, component, threshold, null, null, null, null, emptyList(), false, position,
        RuleLevel.ALERT, "Mensaje", RuleAction.NONE, null, direction = direction, trendCount = trendCount, trendOf = trendOf,
    )

    private fun hit(rule: Rule, readings: List<BloodPressureReading>, alerts: List<Alert> = emptyList(), measurements: List<Measurement> = emptyList()) =
        RuleEngine.evaluate(RuleContext(listOf(rule), measurements, readings, alerts, emptyList(), standingCheckSeconds = listOf(60, 180))).singleOrNull()

    private val pairAbove30 = rule(RuleConditionKind.PAIR_DIFFERENCE, RuleDirection.ABOVE, 30.0)

    // ---------- Diferencia de pareja (H-054) ----------

    @Test
    fun `pareja 83 a 126 con diferencia mayor de 30 dispara con la sentada y la de pie`() {
        assertEquals(listOf("s1", "p1"), hit(pairAbove30, pair(1, 0, 83, 126))!!.triggerEntryIds)
    }

    @Test
    fun `parejas 60 a 80 y 61 a 86 no disparan`() {
        assertNull(hit(pairAbove30, pair(1, 0, 60, 80) + pair(2, 30, 61, 86)))
    }

    @Test
    fun `dos lecturas de pie de una misma sentada se evaluan cada una con la sentada`() {
        val sitting = bp("s", 0, 100)
        val at60 = bp("p60", 2, 120, Position.STANDING, paired = "s", seconds = 60)
        val at180 = bp("p180", 4, 135, Position.STANDING, paired = "s", seconds = 180)
        assertNull(hit(pairAbove30, listOf(sitting, at60)))
        assertEquals(listOf("s", "p180"), hit(pairAbove30, listOf(sitting, at60, at180))!!.triggerEntryIds)
    }

    @Test
    fun `una lectura de pie sin sentada enlazada no se evalua`() {
        assertNull(hit(pairAbove30, listOf(bp("s", 0, 80), bp("p", 2, 130, Position.STANDING, seconds = 60))))
    }

    @Test
    fun `una pareja que sigue cumpliendo no duplica la alerta y una normal la rearma`() {
        val first = pair(1, 0, 80, 120)
        val alert = Alert("a", "f", "r", t0.plusMinutes(3), listOf("s1", "p1"), null, null)
        assertNull(hit(pairAbove30, first + pair(2, 30, 80, 121), listOf(alert)))
        assertEquals(listOf("s3", "p3"), hit(pairAbove30, first + pair(2, 30, 80, 90) + pair(3, 60, 80, 125), listOf(alert))!!.triggerEntryIds)
    }

    // ---------- Tendencia (H-056) ----------

    private val pairTrendUp = rule(RuleConditionKind.TREND, RuleDirection.ABOVE, null, trendCount = 3, trendOf = TrendOf.PAIR_DIFFERENCE)

    @Test
    fun `diferencias 20, 25 y 43 con tendencia de 3 subiendo disparan en la tercera`() {
        val two = pair(1, 0, 100, 120) + pair(2, 30, 100, 125)
        assertNull(hit(pairTrendUp, two))
        assertEquals(listOf("s1", "p1", "s2", "p2", "s3", "p3"), hit(pairTrendUp, two + pair(3, 60, 100, 143))!!.triggerEntryIds)
    }

    @Test
    fun `una diferencia igual en medio corta la racha`() {
        assertNull(hit(pairTrendUp, pair(1, 0, 100, 120) + pair(2, 30, 100, 120) + pair(3, 60, 100, 125)))
    }

    @Test
    fun `la tendencia de la diferencia compara cada momento de pie con su igual`() {
        // A los 60 s: 20, 25, 30 (sube); a los 180 s: 40 y 10 intercalados no rompen la de 60 s.
        val mixed = pair(1, 0, 100, 120, 60) + pair(2, 10, 100, 140, 180) + pair(3, 30, 100, 125, 60) + pair(4, 40, 100, 110, 180) + pair(5, 60, 100, 130, 60)
        assertEquals(listOf("s1", "p1", "s3", "p3", "s5", "p5"), hit(pairTrendUp, mixed)!!.triggerEntryIds)
        // En orden de hora suben 20, 25, 30, pero son de momentos distintos: no hay racha de 3 en ninguno.
        val interleaved = pair(1, 0, 100, 120, 60) + pair(2, 10, 100, 125, 180) + pair(3, 30, 100, 130, 60)
        assertNull(hit(pairTrendUp, interleaved))
    }

    @Test
    fun `la tendencia del valor mira la serie de la medicion`() {
        val trend = rule(RuleConditionKind.TREND, RuleDirection.BELOW, null, component = null, metric = "glucose", trendCount = 3, trendOf = TrendOf.VALUE)
        val m = { id: String, minutes: Long, v: Double -> Measurement(id, "f", "glucose", t0.plusMinutes(minutes), null, v, null, v, "mg/dL", null, null, false, null, null) }
        assertNull(hit(trend, emptyList(), measurements = listOf(m("a", 0, 80.0), m("b", 30, 75.0))))
        assertEquals(listOf("a", "b", "c"), hit(trend, emptyList(), measurements = listOf(m("a", 0, 80.0), m("b", 30, 75.0), m("c", 60, 70.0)))!!.triggerEntryIds)
    }

    // ---------- Cambio desde la partida (H-055) ----------

    private val pulseUp15 = rule(RuleConditionKind.CHANGE_FROM_BASELINE, RuleDirection.ABOVE, 15.0, component = BpComponent.PULSE, position = Position.SITTING)

    @Test
    fun `pulso sentado mas de 15 sobre la partida dispara con 86 y no con 84`() {
        val base = bp("b", 0, 110, pulse = 70)
        assertEquals(listOf("x"), hit(pulseUp15, listOf(base, bp("x", 60, 110, pulse = 86)))!!.triggerEntryIds)
        assertNull(hit(pulseUp15, listOf(base, bp("x", 60, 110, pulse = 84))))
    }

    @Test
    fun `sin lecturas con valor o solo con la de partida no dispara`() {
        assertNull(hit(pulseUp15, emptyList()))
        assertNull(hit(pulseUp15, listOf(bp("b", 0, 110, pulse = 99, baseline = true))))
    }

    @Test
    fun `la lectura marcada de partida sustituye a la primera`() {
        val readings = listOf(bp("a", 0, 110, pulse = 70), bp("b", 30, 110, pulse = 80), bp("c", 60, 110, pulse = 90))
        assertEquals(listOf("c"), hit(pulseUp15, readings)!!.triggerEntryIds)
        assertNull(hit(pulseUp15, readings.map { if (it.id == "b") it.copy(isBaseline = true) else it }))
    }

    @Test
    fun `is_placeholder no cambia la evaluacion de una regla activa`() {
        assertEquals(hit(pairAbove30, pair(1, 0, 83, 126)), hit(pairAbove30.copy(isPlaceholder = true), pair(1, 0, 83, 126))?.copy(rule = pairAbove30))
    }

    // ---------- Ingesta bajo un porcentaje de la meta (H-022) ----------

    private fun saltRule(percent: Double?) = rule(RuleConditionKind.INTAKE_BELOW_GOAL, null, null, component = null, metric = null)
        .copy(intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0), goalPercent = percent)

    private fun saltHit(percent: Double?, grams: Double, goal: Double?) = RuleEngine.evaluate(
        RuleContext(
            rules = listOf(saltRule(percent)), measurements = emptyList(), bloodPressure = emptyList(), alerts = emptyList(), stageWindows = emptyList(),
            intakes = listOf(Intake("i", "f", "salt", t0, grams, "g", null)), intakeTypes = listOf(SeedCatalog.salt),
            goals = listOf(IntakeGoal("g", "salt", goal, "g")), now = t0.withHour(14),
        ),
    ).singleOrNull()

    @Test
    fun `sal por debajo del 50 por ciento de la meta a las 14 con 0_6 de 2_5 dispara`() {
        assertTrue(saltHit(50.0, 0.6, 2.5) != null)
        assertNull(saltHit(50.0, 1.3, 2.5))
    }

    @Test
    fun `sin porcentaje la regla es la de siempre, al 100 por ciento`() {
        assertTrue(saltHit(null, 2.0, 2.5) != null)
        assertNull(saltHit(null, 2.5, 2.5))
    }

    @Test
    fun `con la meta sin cifra la regla no se evalua`() {
        assertNull(saltHit(50.0, 0.0, null))
    }
}
