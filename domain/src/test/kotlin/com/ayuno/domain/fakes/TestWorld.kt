package com.ayuno.domain.fakes

import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastWithPlan
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.Plan
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.TemplateWithPlan
import com.ayuno.domain.usecase.CorrectClockStart
import com.ayuno.domain.usecase.CreateFast
import com.ayuno.domain.usecase.DecideAlert
import com.ayuno.domain.usecase.EndFast
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.RecordBloodPressure
import com.ayuno.domain.usecase.RecordMeasurement
import com.ayuno.domain.usecase.RegisterLastMeal
import com.ayuno.domain.usecase.SaveMetricType
import com.ayuno.domain.usecase.SaveTemplate
import com.ayuno.domain.usecase.SetMetricTypeActive
import com.ayuno.domain.usecase.StartFast
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Mundo de prueba: todos los datos se crean por los casos de uso reales (regla anti-fixture-fantasma).
 * Reloj inicial: jueves 22-oct-2026 18:00 hora local (-05:00).
 */
class TestWorld {
    val clock = TestClock()
    val catalog = FakeCatalog()
    val templates = FakeTemplates()
    val fasts = FakeFasts()
    val records = FakeRecords()
    val alerts = FakeAlerts()
    val audit = FakeAudit()
    val refeed = FakeRefeed(fasts)

    val saveMetricType = SaveMetricType(catalog)
    val setMetricTypeActive = SetMetricTypeActive(catalog)
    val saveTemplate = SaveTemplate(templates)
    val duplicateTemplate = com.ayuno.domain.usecase.DuplicateTemplate(templates)
    val setTemplateDeleted = com.ayuno.domain.usecase.SetTemplateDeleted(templates, audit, DirectTransactions, clock)
    val createFast = CreateFast(templates, fasts, DirectTransactions, clock)
    val startFast = StartFast(fasts, DirectTransactions, clock)
    val registerLastMeal = RegisterLastMeal(fasts, audit, DirectTransactions, clock)
    val correctClockStart = CorrectClockStart(fasts, audit, DirectTransactions, clock)
    val endFast = EndFast(fasts, refeed, DirectTransactions, clock)
    val setRefeedCheck = com.ayuno.domain.usecase.SetRefeedCheck(refeed, audit, DirectTransactions, clock)
    val recordMeal = com.ayuno.domain.usecase.RecordMeal(fasts, refeed)
    val recordMeasurement = RecordMeasurement(catalog, fasts, records, alerts, DirectTransactions, clock)
    val recordBloodPressure = RecordBloodPressure(catalog, fasts, records, alerts, DirectTransactions, clock)
    val decideAlert = DecideAlert(alerts)
    val saveSymptomType = com.ayuno.domain.usecase.SaveSymptomType(catalog)
    val saveIntakeType = com.ayuno.domain.usecase.SaveIntakeType(catalog)
    val saveProduct = com.ayuno.domain.usecase.SaveProduct(catalog)
    val recordSymptom = com.ayuno.domain.usecase.RecordSymptom(catalog, fasts, records, alerts, DirectTransactions, clock)
    val recordIntake = com.ayuno.domain.usecase.RecordIntake(catalog, fasts, records, alerts, DirectTransactions, clock)
    val recordProductLog = com.ayuno.domain.usecase.RecordProductLog(catalog, fasts, records, DirectTransactions)
    val recordNote = com.ayuno.domain.usecase.RecordNote(fasts, records)
    val triggerManualAlert = com.ayuno.domain.usecase.TriggerManualAlert(fasts, alerts, DirectTransactions, clock)
    val evaluateRules = com.ayuno.domain.usecase.EvaluateRules(catalog, fasts, records, alerts, DirectTransactions, clock)
    val setChecklistItem = com.ayuno.domain.usecase.SetChecklistItem(fasts)
    val changePlannedFast = com.ayuno.domain.usecase.ChangePlannedFast(templates, fasts, audit, DirectTransactions, clock)
    val deleteFast = com.ayuno.domain.usecase.DeleteFast(fasts, DirectTransactions)

    fun glucoseBelow(threshold: Double, sustained: Int? = null, action: RuleAction = RuleAction.OFFER_END) = Rule(
        id = Ids.newId(), name = "Glucosa baja", active = true, conditionKind = RuleConditionKind.METRIC_BELOW,
        metricTypeCode = "glucose", bpComponent = null, threshold = threshold, sustainedMinutes = sustained,
        symptomTypeCode = null, intakeTypeCode = null, checkTime = null, filterStageKinds = emptyList(),
        filterOnWaking = false, filterPosition = null, level = RuleLevel.ALERT, message = "Rompe con caldo tibio con sal.",
        action = action, actionMinutes = null,
    )

    fun standingSystolicBelow(threshold: Double) = glucoseBelow(threshold).copy(
        name = "Presión de pie baja", metricTypeCode = "blood_pressure", bpComponent = BpComponent.SYSTOLIC,
        filterPosition = Position.STANDING,
    )

    fun plan57h(rules: List<Rule> = emptyList()) = Plan(
        stages = listOf(
            Stage(Ids.newId(), StageKind.PREPARATION, 3.0, DurationUnit.DAYS),
            Stage(Ids.newId(), StageKind.REFEED, 6.0, DurationUnit.HOURS),
            Stage(Ids.newId(), StageKind.POST, 4.0, DurationUnit.DAYS),
        ),
        metrics = listOf(
            PlanMetric(Ids.newId(), "glucose", listOf(7, 12, 18, 22).map { MetricSchedule(Ids.newId(), StageKind.FAST, LocalTime.of(it, 0)) }),
            PlanMetric(Ids.newId(), "weight", listOf(MetricSchedule(Ids.newId(), StageKind.POST, LocalTime.of(7, 0)))),
        ),
        rules = rules,
    )

    suspend fun template(plan: Plan = plan57h()): TemplateWithPlan {
        val result = saveTemplate(TemplateWithPlan(Template(Ids.newId(), "Ayuno de 57 horas", 57.0, listOf(12.0, 24.0, 36.0, 48.0)), plan))
        return (result as Outcome.Ok).value
    }

    /** Ayuno planificado: última comida jueves 22-oct 18:00, reloj previsto 22:00, objetivo 57 h. */
    /** Ayuno planificado con última comida el jueves 22 oct a las 18:00; [weeksLater] lo aleja para respetar D-17. */
    suspend fun plannedFast(plan: Plan = plan57h(), weeksLater: Long = 0): FastWithPlan {
        val template = template(plan)
        val lastMeal = LocalDateTime.of(2026, 10, 22, 18, 0).plusWeeks(weeksLater)
        val result = createFast(CreateFast.Input(template.template.id, lastMeal, lastMeal.plusHours(4), 57.0, null))
        return (result as Outcome.Ok).value
    }

    /** Ayuno en curso iniciado a las 22:10 del jueves. */
    suspend fun startedFast(plan: Plan = plan57h()): Fast {
        val planned = plannedFast(plan)
        clock.instant = java.time.Instant.parse("2026-10-23T03:10:00Z")
        return (startFast(planned.fast.id) as Outcome.Ok).value
    }
}
