package com.ayuno.domain.usecase

import com.ayuno.domain.derived.ExpectedIntake
import com.ayuno.domain.derived.HouseholdMeasures
import com.ayuno.domain.derived.IntakeNudge
import com.ayuno.domain.derived.StandingMoment
import com.ayuno.domain.fakes.DirectTransactions
import com.ayuno.domain.fakes.SeedCatalog
import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.EntryKind
import com.ayuno.domain.model.HouseholdUnit
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleDirection
import com.ayuno.domain.model.Settings
import com.ayuno.domain.model.StartWarning
import com.ayuno.domain.model.TrendOf
import com.ayuno.domain.model.WarningKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** EV-006 (H-050 a H-057 y criterios añadidos): derivados y casos de uso con el mundo de prueba. */
class Ev006UseCasesTest {

    private val at = LocalDateTime.of(2026, 10, 23, 9, 0)

    private fun units(type: String, vararg u: Triple<String, Double, Boolean>) =
        u.mapIndexed { i, (code, eq, imprecise) -> HouseholdUnit(Ids.newId(), code, type, code, eq, imprecise, i) }

    private val water = SeedCatalog.water.copy(quickUnitCode = "glass", units = units("water", Triple("glass", 250.0, false), Triple("cup", 240.0, false)))
    private val salt = SeedCatalog.salt.copy(
        quickUnitCode = "pinch",
        units = units("salt", Triple("teaspoon", 5.0, false), Triple("quarter_teaspoon", 1.25, false), Triple("pinch", 0.3, true)),
    )

    private suspend fun TestWorld.withMeasures() {
        catalog.saveIntakeType(water)
        catalog.saveIntakeType(salt)
        // Un tipo que rompe el ayuno nunca tiene botón, aunque lo traiga.
        catalog.saveIntakeType(SeedCatalog.broth.copy(quickUnitCode = "cup", units = units("broth", Triple("cup", 240.0, false))))
    }

    private fun TestWorld.quick() = RecordQuickIntake(catalog, recordIntake, clock)

    // ---------- H-050: medidas caseras ----------

    @Test
    fun `H-050 una pizca guarda lo tecleado y su equivalente en gramos`() = runTest {
        val w = TestWorld(); w.withMeasures(); val f = w.startedFast()
        w.recordIntake(RecordIntake.Input(f.id, "salt", at, 1.0, "pinch", null))
        val i = w.records.intakes.value.single()
        assertEquals(listOf(1.0, 0.3), listOf(i.amountEntered, i.amount))
        assertEquals(listOf("pinch", "g"), listOf(i.unitEntered, i.unit))
    }

    @Test
    fun `H-050 una unidad que no es del tipo ni convertible se rechaza y una antigua convertible se acepta`() = runTest {
        val w = TestWorld(); w.withMeasures(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.MEASURE_NOT_FOUND), (w.recordIntake(RecordIntake.Input(f.id, "salt", at, 1.0, "cubo", null)) as Outcome.Invalid).errors)
        w.recordIntake(RecordIntake.Input(f.id, "water", at, 0.5, "L", null))
        assertEquals(500.0, w.records.intakes.value.single().amount, 1e-9)
    }

    @Test
    fun `H-050 la medida imprecisa dice a cuantas equivale la meta y a que fraccion de la exacta mayor`() {
        val line = HouseholdMeasures.impreciseLine(salt, salt.unit("pinch")!!, IntakeGoal("g", "salt", 2.5, "g"))!!
        assertEquals(8, line.goalInUnits)
        assertEquals("teaspoon", line.goalInLargest!!.first.code)
        assertEquals(0.5, line.goalInLargest!!.second, 1e-9)
        assertNull(HouseholdMeasures.impreciseLine(salt, salt.unit("teaspoon")!!, null))
        assertNull(HouseholdMeasures.impreciseLine(salt, salt.unit("pinch")!!, IntakeGoal("g", "salt", null, "g"))!!.goalInUnits)
    }

    // ---------- H-051: botón rápido y agua con sal ----------

    @Test
    fun `H-051 un toque guarda una de su medida a la hora actual y Deshacer la borra con su traza`() = runTest {
        val w = TestWorld(); w.withMeasures(); val f = w.startedFast()
        val saved = ((w.quick()(f.id, "water") as Outcome.Ok).value as RecordResult.Saved).entry
        assertEquals(250.0 to "glass", saved.amount to saved.unitEntered)
        assertEquals(java.time.LocalDateTime.ofInstant(w.clock.instant, w.clock.zone).withNano(0), saved.takenAt)
        assertTrue(w.setEntryDeleted(EntryKind.INTAKE, saved.id, true) is Outcome.Ok)
        assertTrue(w.records.intakes(f.id).isEmpty())
        assertEquals(listOf("intake"), w.audit.entriesFor(saved.id).map { it.entityName })
    }

    @Test
    fun `H-051 un tipo que rompe el ayuno o sin medida rapida no tiene boton`() = runTest {
        val w = TestWorld(); w.withMeasures(); val f = w.startedFast()
        assertEquals(listOf(ValidationError.QUICK_UNIT_NOT_SET), (w.quick()(f.id, "broth") as Outcome.Invalid).errors)
        w.catalog.saveIntakeType(salt.copy(quickUnitCode = null))
        assertEquals(listOf(ValidationError.QUICK_UNIT_NOT_SET), (w.quick()(f.id, "salt") as Outcome.Invalid).errors)
    }

    @Test
    fun `H-051 agua con sal guarda dos ingestas juntas y evalua una sola vez viendo la sal`() = runTest {
        val w = TestWorld(); w.withMeasures()
        val saltRule = w.glucoseBelow(0.0).copy(
            conditionKind = RuleConditionKind.INTAKE_BELOW_GOAL, metricTypeCode = null, threshold = null, intakeTypeCode = "salt", checkTime = LocalTime.of(0, 0),
        )
        val f = w.startedFast(w.plan57h(listOf(saltRule)).copy(goals = listOf(IntakeGoal(Ids.newId(), "salt", 1.0, "g"))))
        w.clock.instant = java.time.Instant.parse("2026-10-23T14:00:00Z")
        val result = w.recordIntake(RecordIntake.Input(f.id, "water", at, 1.0, "glass", null, alongside = RecordIntake.Part("salt", 4.0, "pinch")))
        assertTrue(((result as Outcome.Ok).value as RecordResult.Saved).alerts.isEmpty())
        val (a, b) = w.records.intakes.value
        assertTrue(a.groupId != null && a.groupId == b.groupId)
        assertEquals(1.2, b.amount, 1e-9)
    }

    // ---------- H-052: pregunta tras horas sin anotar ----------

    private val quiet = Settings(quietStart = LocalTime.of(23, 0), quietEnd = LocalTime.of(6, 30))

    @Test
    fun `H-052 la pregunta cae tras 6 h despierto saltando el silencio nocturno`() {
        val from = LocalDateTime.of(2026, 10, 23, 21, 0)
        assertEquals(LocalDateTime.of(2026, 10, 24, 10, 30), IntakeNudge.addAwake(from, Duration.ofHours(6), quiet))
        assertEquals(LocalDateTime.of(2026, 10, 24, 3, 0), IntakeNudge.addAwake(from, Duration.ofHours(6), Settings()))
        // Empezando dentro del silencio, cuenta desde que acaba.
        assertEquals(LocalDateTime.of(2026, 10, 24, 8, 30), IntakeNudge.addAwake(LocalDateTime.of(2026, 10, 24, 2, 0), Duration.ofHours(2), quiet))
    }

    @Test
    fun `H-052 una ingesta del tipo mueve el ancla y una meta sin horas no pregunta`() = runTest {
        val w = TestWorld(); w.withMeasures()
        val goals = listOf(IntakeGoal(Ids.newId(), "salt", null, "g", nudgeAfterHours = 6.0), IntakeGoal(Ids.newId(), "water", 2000.0, "mL"))
        val f = w.startedFast(w.plan57h().copy(goals = goals))
        val source = w.fasts.get(f.id)!!
        val first = IntakeNudge.items(source, emptyList(), quiet).single()
        assertEquals("salt" to f.clockStartActual, first.intakeTypeCode to first.anchor)
        w.recordIntake(RecordIntake.Input(f.id, "salt", at, 1.0, "pinch", null))
        w.recordIntake(RecordIntake.Input(f.id, "water", at.plusHours(1), 1.0, "glass", null))
        val moved = IntakeNudge.items(source, w.records.intakes.value, quiet).single()
        assertEquals(at, moved.anchor)
        assertEquals(at.plusHours(6), moved.at)
    }

    @Test
    fun `H-006 el avance esperado reparte la meta en el dia despierto`() {
        val goal = IntakeGoal("g", "water", 2000.0, "mL")
        val settings = Settings(quietStart = LocalTime.of(23, 0), quietEnd = LocalTime.of(7, 0))
        assertEquals(1000.0, ExpectedIntake.at(goal, settings, LocalDateTime.of(2026, 10, 23, 15, 0))!!, 1e-9)
        assertEquals(0.0, ExpectedIntake.at(goal, settings, LocalDateTime.of(2026, 10, 23, 6, 0))!!, 1e-9)
        assertNull(ExpectedIntake.at(goal.copy(dailyAmount = null), settings, LocalDateTime.of(2026, 10, 23, 15, 0)))
        assertNull(ExpectedIntake.at(goal, Settings(), LocalDateTime.of(2026, 10, 23, 15, 0)))
    }

    // ---------- H-053: temporizador de pie ----------

    @Test
    fun `H-053 la lectura de pie guarda sus segundos y el momento mas cercano del plan`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val sitting = ((w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 110, diastolic = 70, pulse = 60, position = Position.SITTING)) as Outcome.Ok).value as RecordResult.Saved).entry
        val standing = ((w.recordBloodPressure(
            RecordBloodPressure.Input(f.id, at.plusSeconds(160), systolic = 100, diastolic = 70, pulse = 80, position = Position.STANDING, pairedReadingId = sitting.id, standingSince = at.plusSeconds(65)),
        ) as Outcome.Ok).value as RecordResult.Saved).entry
        assertEquals(95, standing.secondsStanding)
        assertEquals(60, StandingMoment.of(standing, listOf(60, 180)))
        assertEquals(60, StandingMoment.of(standing.copy(secondsStanding = 120), listOf(60, 180)))
        assertNull(StandingMoment.of(standing.copy(secondsStanding = null), listOf(60, 180)))
        assertEquals(-10, StandingMoment.difference(standing, sitting, BpComponent.SYSTOLIC))
        assertEquals(
            listOf(ValidationError.SECONDS_STANDING_NEGATIVE),
            (w.recordBloodPressure(
                RecordBloodPressure.Input(f.id, at, systolic = 100, diastolic = 70, position = Position.STANDING, pairedReadingId = sitting.id, standingSince = at.plusMinutes(1)),
            ) as Outcome.Invalid).errors,
        )
    }

    @Test
    fun `H-053 corregir la hora de una lectura de pie desplaza sus segundos con traza`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val sitting = ((w.recordBloodPressure(RecordBloodPressure.Input(f.id, at, systolic = 110, diastolic = 70, position = Position.SITTING)) as Outcome.Ok).value as RecordResult.Saved).entry
        val standing = ((w.recordBloodPressure(
            RecordBloodPressure.Input(f.id, at.plusMinutes(2), systolic = 100, diastolic = 70, position = Position.STANDING, pairedReadingId = sitting.id, standingSince = at.plusMinutes(1)),
        ) as Outcome.Ok).value as RecordResult.Saved).entry
        w.correctBloodPressure(CorrectBloodPressure.Input(standing.id, at.plusMinutes(4), systolic = 100, diastolic = 70, position = Position.STANDING))
        assertEquals(180, w.records.bloodPressureReading(standing.id)!!.secondsStanding)
        assertTrue(w.audit.entriesFor(standing.id).any { it.field == "seconds_standing" && it.oldValue == "60" && it.newValue == "180" })
    }

    // ---------- H-054 a H-056: reglas nuevas en el editor ----------

    private fun TestWorld.betweenRule(kind: RuleConditionKind) = glucoseBelow(0.0).copy(
        conditionKind = kind, metricTypeCode = "blood_pressure", bpComponent = BpComponent.SYSTOLIC, threshold = 30.0, direction = RuleDirection.ABOVE,
    )

    @Test
    fun `H-054 H-056 las reglas nuevas exigen direccion, racha de 3 y presion para la tendencia de la diferencia`() {
        val w = TestWorld()
        assertTrue(RuleValidator.validate(w.betweenRule(RuleConditionKind.PAIR_DIFFERENCE)).isEmpty())
        assertTrue(ValidationError.RULE_DIRECTION_REQUIRED in RuleValidator.validate(w.betweenRule(RuleConditionKind.PAIR_DIFFERENCE).copy(direction = null)))
        val trend = w.betweenRule(RuleConditionKind.TREND).copy(threshold = null, trendCount = 2, trendOf = TrendOf.PAIR_DIFFERENCE)
        assertTrue(ValidationError.RULE_TREND_COUNT_TOO_LOW in RuleValidator.validate(trend))
        assertTrue(ValidationError.RULE_TREND_NEEDS_BLOOD_PRESSURE in RuleValidator.validate(trend.copy(trendCount = 3, metricTypeCode = "glucose", bpComponent = null)))
        val percent = w.glucoseBelow(0.0).copy(conditionKind = RuleConditionKind.INTAKE_BELOW_GOAL, metricTypeCode = null, threshold = null, intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0), goalPercent = 120.0)
        assertTrue(ValidationError.RULE_GOAL_PERCENT_OUT_OF_RANGE in RuleValidator.validate(percent))
    }

    @Test
    fun `H-055 marcar de partida quita la anterior, deja traza y reevalua sin tocar las alertas pasadas`() = runTest {
        val w = TestWorld()
        val rule = w.betweenRule(RuleConditionKind.CHANGE_FROM_BASELINE).copy(bpComponent = BpComponent.PULSE, threshold = 15.0, filterPosition = Position.SITTING)
        val f = w.startedFast(w.plan57h(listOf(rule)))
        suspend fun sitting(minutes: Long, pulse: Int) = ((w.recordBloodPressure(
            RecordBloodPressure.Input(f.id, at.plusMinutes(minutes), systolic = 110, diastolic = 70, pulse = pulse, position = Position.SITTING),
        ) as Outcome.Ok).value as RecordResult.Saved)
        val first = sitting(0, 70).entry
        val second = sitting(30, 60).entry
        val third = sitting(60, 80)
        // Partida = primera (70): 80 no pasa de 85.
        assertTrue(third.alerts.isEmpty())
        val mark = MarkBaseline(w.catalog, w.fasts, w.records, w.alerts, w.audit, DirectTransactions, w.clock)
        mark(EntryKind.BLOOD_PRESSURE, first.id, true)
        val alerts = (mark(EntryKind.BLOOD_PRESSURE, second.id, true) as Outcome.Ok).value
        // Partida = 60: 80 pasa de 75 → salta ahora.
        assertEquals(listOf(third.entry.id), alerts.single().triggerEntryIds)
        assertFalse(w.records.bloodPressureReading(first.id)!!.isBaseline)
        assertTrue(w.audit.entriesFor(first.id).map { it.newValue } == listOf("true", "false"))
        // Volver a la primera: la alerta ya registrada sigue ahí.
        mark(EntryKind.BLOOD_PRESSURE, first.id, true)
        assertEquals(1, w.alerts.items.value.size)
    }

    // ---------- H-057 y H-003: revisión antes de empezar ----------

    @Test
    fun `H-057 la revision lista reglas de ejemplo, metas sin cifra y casillas sin marcar, y empezar igual las guarda`() = runTest {
        val w = TestWorld()
        val placeholder = w.glucoseBelow(55.0).copy(name = "Glucosa baja (ejemplo)", isPlaceholder = true)
        val goal = IntakeGoal(Ids.newId(), "salt", null, "g")
        val item = ChecklistItem(Ids.newId(), "Comprar sal", false)
        val planned = w.plannedFast(w.plan57h(listOf(placeholder)).copy(goals = listOf(goal), checklist = listOf(item)))
        w.clock.instant = java.time.Instant.parse("2026-10-23T03:10:00Z")
        assertEquals(listOf(ValidationError.PRE_START_REVIEW_PENDING), (w.startFast(planned.fast.id) as Outcome.Invalid).errors)
        val started = (w.startFast(planned.fast.id, anyway = true) as Outcome.Ok).value
        assertEquals(
            listOf(
                StartWarning(WarningKind.PLACEHOLDER_RULE, planned.plan.rules.single().id, "Glucosa baja (ejemplo)"),
                StartWarning(WarningKind.GOAL_WITHOUT_AMOUNT, planned.plan.goals.single().id, "Sal"),
                StartWarning(WarningKind.UNCHECKED_ITEM, planned.plan.checklist.single().id, "Comprar sal"),
            ),
            started.startedWithWarnings,
        )
    }

    @Test
    fun `H-057 arreglar el plan del ayuno quita la marca de ejemplo al cambiar la cifra y solo mientras esta planificado`() = runTest {
        val w = TestWorld()
        val placeholder = w.glucoseBelow(55.0).copy(isPlaceholder = true)
        val planned = w.plannedFast(w.plan57h(listOf(placeholder)))
        val save = SaveFastPlan(w.fasts)
        val rule = planned.plan.rules.single()
        // Guardar sin cambios la deja «de ejemplo»; cambiar la cifra la convierte en propia.
        assertTrue((save(planned.fast.id, listOf(rule), emptyList()) as Outcome.Ok).value.rules.single().isPlaceholder)
        val fixed = (save(planned.fast.id, listOf(rule.copy(threshold = 60.0)), listOf(IntakeGoal(Ids.newId(), "salt", 2.5, "g"))) as Outcome.Ok).value
        assertFalse(fixed.rules.single().isPlaceholder)
        assertTrue(w.startFast(planned.fast.id) is Outcome.Ok)
        assertEquals(listOf(ValidationError.FAST_NOT_PLANNED), (save(planned.fast.id, listOf(rule), emptyList()) as Outcome.Invalid).errors)
    }

    @Test
    fun `H-003 una plantilla guardada sin tocar sus reglas de ejemplo las conserva y editar el mensaje las hace propias`() = runTest {
        val w = TestWorld()
        val template = w.template(w.plan57h(listOf(w.glucoseBelow(55.0).copy(isPlaceholder = true))))
        val same = (w.saveTemplate(template) as Outcome.Ok).value
        assertTrue(same.plan.rules.single().isPlaceholder)
        val edited = (w.saveTemplate(same.copy(plan = same.plan.copy(rules = same.plan.rules.map { it.copy(message = "Mío") }))) as Outcome.Ok).value
        assertFalse(edited.plan.rules.single().isPlaceholder)
    }

    @Test
    fun `H-009 los tiempos de pie del plan son crecientes, distintos, positivos y solo de la presion`() {
        val w = TestWorld()
        fun errors(code: String, seconds: List<Int>?) = PlanValidator.validate(w.plan57h().copy(metrics = listOf(PlanMetric(Ids.newId(), code, emptyList(), seconds))))
        assertTrue(errors("blood_pressure", listOf(60, 180)).isEmpty())
        assertTrue(ValidationError.STANDING_SECONDS_INVALID in errors("blood_pressure", listOf(180, 60)))
        assertTrue(ValidationError.STANDING_SECONDS_INVALID in errors("blood_pressure", listOf(0, 60)))
        assertTrue(ValidationError.STANDING_SECONDS_INVALID in errors("glucose", listOf(60)))
    }

    // ---------- Medidas caseras y botón rápido en el catálogo (H-050, H-051) ----------

    private fun draft(name: String, equivalent: Double, imprecise: Boolean = false, id: String? = null, quick: Boolean = false) =
        SaveIntakeType.UnitDraft(id, name, equivalent, imprecise, quick)

    @Test
    fun `H-050 editar un liquido sin tocar sus medidas las conserva con su boton rapido`() = runTest {
        val w = TestWorld()
        val withUnits = (w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, listOf(draft("pizca", 0.3, true, quick = true)))) as Outcome.Ok).value
        assertEquals("pizca", withUnits.quickUnitCode)
        val renamed = (w.saveIntakeType(SaveIntakeType.Input("salt", "Sal fina", "g", false, 0.393)) as Outcome.Ok).value
        assertEquals(withUnits.units, renamed.units)
        assertEquals("pizca", renamed.quickUnitCode)
    }

    @Test
    fun `H-050 una medida nueva recibe codigo de su nombre y la existente conserva el suyo`() = runTest {
        val w = TestWorld()
        val first = (w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, listOf(draft("Pizca", 0.3, true)))) as Outcome.Ok).value
        val pinch = first.units.single()
        val second = (
            w.saveIntakeType(
                SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, listOf(draft("Cucharadita rasa", 5.0), draft("Pellizco", 0.3, true, id = pinch.id))),
            ) as Outcome.Ok
            ).value
        assertEquals(listOf("cucharadita_rasa", pinch.code), second.units.map { it.code })
        assertEquals(listOf(0, 1), second.units.map { it.position })
        assertEquals("Pellizco", second.units[1].name)
    }

    @Test
    fun `H-051 quitar la medida del boton o marcar que rompe el ayuno deja el boton en ninguno`() = runTest {
        val w = TestWorld()
        val saved = (w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, listOf(draft("pizca", 0.3, quick = true)))) as Outcome.Ok).value
        assertEquals("pizca", saved.quickUnitCode)
        assertNull((w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, emptyList())) as Outcome.Ok).value.quickUnitCode)
        assertNull((w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", true, 0.393, listOf(draft("pizca", 0.3, quick = true)))) as Outcome.Ok).value.quickUnitCode)
    }

    @Test
    fun `H-050 medidas sin nombre, repetidas o con equivalencia vacia o cero no se guardan`() = runTest {
        val w = TestWorld()
        suspend fun errors(vararg units: SaveIntakeType.UnitDraft) =
            (w.saveIntakeType(SaveIntakeType.Input("salt", "Sal", "g", false, 0.393, units.toList())) as Outcome.Invalid).errors
        assertEquals(listOf(ValidationError.MEASURE_NAME_REQUIRED), errors(draft(" ", 1.0)))
        assertEquals(listOf(ValidationError.MEASURE_NAME_DUPLICATED), errors(draft("Pizca", 0.3), draft("pizca", 0.4)))
        assertEquals(listOf(ValidationError.MEASURE_EQUIVALENT_NOT_POSITIVE), errors(draft("Pizca", Double.NaN)))
        assertEquals(listOf(ValidationError.MEASURE_EQUIVALENT_NOT_POSITIVE), errors(draft("Pizca", 0.0)))
    }
}
