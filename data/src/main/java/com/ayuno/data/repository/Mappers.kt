package com.ayuno.data.repository

import com.ayuno.data.db.AlertEntity
import com.ayuno.data.db.BloodPressureReadingEntity
import com.ayuno.data.db.EditLogEntity
import com.ayuno.data.db.FastEntity
import com.ayuno.data.db.MeasurementEntity
import com.ayuno.data.db.MetricScheduleEntity
import com.ayuno.data.db.MetricTypeEntity
import com.ayuno.data.db.PlanMetricEntity
import com.ayuno.data.db.RuleEntity
import com.ayuno.data.db.StageEntity
import com.ayuno.data.db.TemplateEntity
import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.BloodPressureReading
import com.ayuno.domain.model.BpComponent
import com.ayuno.domain.model.DurationUnit
import com.ayuno.domain.model.EditAction
import com.ayuno.domain.model.EditLogEntry
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Fast
import com.ayuno.domain.model.FastStatus
import com.ayuno.domain.model.GlucoseSource
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricSchedule
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.PlanMetric
import com.ayuno.domain.model.PlanOwner
import com.ayuno.domain.model.Position
import com.ayuno.domain.model.Rule
import com.ayuno.domain.model.RuleAction
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.RuleLevel
import com.ayuno.domain.model.Stage
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.Template
import com.ayuno.domain.model.Trend
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.model.codeOf
import com.ayuno.domain.time.LocalTimes
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.LocalTime

// Mapeo explícito entidad Room ↔ modelo de dominio (ARQUITECTURA §1). Las listas se guardan como texto JSON (ADR-003).

private val json = Json
private val doubles = ListSerializer(Double.serializer())
private val strings = ListSerializer(String.serializer())

internal fun List<Double>.toJson(): String = json.encodeToString(doubles, this)
internal fun String.toDoubleList(): List<Double> = json.decodeFromString(doubles, this)
internal fun List<String>.toJsonStrings(): String = json.encodeToString(strings, this)
internal fun String.toStringList(): List<String> = json.decodeFromString(strings, this)

internal fun LocalDateTime.text(): String = LocalTimes.format(this)
internal fun String.localDateTime(): LocalDateTime = LocalTimes.parse(this)
internal fun LocalTime.text(): String = toString().take(5)
internal fun String.localTime(): LocalTime = LocalTime.parse(this)

internal fun MetricTypeEntity.toDomain() = MetricType(code, name, unit, altUnit, entryUnit, isNumeric, minValid, maxValid, active)

internal fun MetricType.toEntity(createdAt: String, updatedAt: String?) =
    MetricTypeEntity(code, name, unit, altUnit, entryUnit, isNumeric, minValid, maxValid, active, createdAt, updatedAt, null)

internal fun TemplateEntity.toDomain() = Template(id, name, targetHours, milestoneHours.toDoubleList())

internal fun StageEntity.toDomain() = Stage(id, codeOf<StageKind>(kind), durationValue, codeOf<DurationUnit>(durationUnit))

internal fun Stage.toEntity(owner: PlanOwner, now: String) = StageEntity(
    id, owner.templateId(), owner.fastId(), kind.code, durationValue, durationUnit.code, now, null, null,
)

internal fun PlanMetricEntity.toDomain(schedules: List<MetricScheduleEntity>) =
    PlanMetric(id, metricTypeCode, schedules.filter { it.planMetricId == id }.map { it.toDomain() })

internal fun MetricScheduleEntity.toDomain() = MetricSchedule(id, codeOf<StageKind>(stageKind), timeOfDay.localTime())

internal fun PlanMetric.toEntity(owner: PlanOwner, now: String) =
    PlanMetricEntity(id, owner.templateId(), owner.fastId(), metricTypeCode, now, null, null)

internal fun MetricSchedule.toEntity(planMetricId: String, now: String) =
    MetricScheduleEntity(id, planMetricId, stageKind.code, timeOfDay.text(), now, null, null)

internal fun RuleEntity.toDomain() = Rule(
    id = id,
    name = name,
    active = active,
    conditionKind = codeOf<RuleConditionKind>(conditionKind),
    metricTypeCode = metricTypeCode,
    bpComponent = bpComponent?.let { codeOf<BpComponent>(it) },
    threshold = threshold,
    sustainedMinutes = sustainedMinutes,
    symptomTypeCode = symptomTypeCode,
    intakeTypeCode = intakeTypeCode,
    checkTime = checkTime?.localTime(),
    filterStageKinds = filterStageKinds.toStringList().map { codeOf<StageKind>(it) },
    filterOnWaking = filterOnWaking,
    filterPosition = filterPosition?.let { codeOf<Position>(it) },
    level = codeOf<RuleLevel>(level),
    message = message,
    action = codeOf<RuleAction>(action),
    actionMinutes = actionMinutes,
)

internal fun Rule.toEntity(owner: PlanOwner, now: String) = RuleEntity(
    id = id,
    templateId = owner.templateId(),
    fastId = owner.fastId(),
    name = name,
    active = active,
    conditionKind = conditionKind.code,
    metricTypeCode = metricTypeCode,
    bpComponent = bpComponent?.code,
    threshold = threshold,
    sustainedMinutes = sustainedMinutes,
    symptomTypeCode = symptomTypeCode,
    intakeTypeCode = intakeTypeCode,
    checkTime = checkTime?.text(),
    filterStageKinds = filterStageKinds.map { it.code }.toJsonStrings(),
    filterOnWaking = filterOnWaking,
    filterPosition = filterPosition?.code,
    level = level.code,
    message = message,
    action = action.code,
    actionMinutes = actionMinutes,
    createdAt = now,
    updatedAt = null,
    deletedAt = null,
)

internal fun FastEntity.toDomain() = Fast(
    id = id,
    templateId = templateId,
    status = codeOf<FastStatus>(status),
    targetHours = targetHours,
    milestoneHours = milestoneHours.toDoubleList(),
    lastMealPlanned = lastMealPlanned.localDateTime(),
    lastMealActual = lastMealActual?.localDateTime(),
    clockStartPlanned = clockStartPlanned.localDateTime(),
    clockStartActual = clockStartActual?.localDateTime(),
    endActual = endActual?.localDateTime(),
    endReason = endReason?.let { codeOf<EndReason>(it) },
    endReasonDetail = endReasonDetail,
    endRuleId = endRuleId,
    tzOffset = tzOffset,
    notes = notes,
)

internal fun Fast.toEntity(createdAt: String, updatedAt: String?) = FastEntity(
    id = id,
    templateId = templateId,
    status = status.code,
    targetHours = targetHours,
    milestoneHours = milestoneHours.toJson(),
    lastMealPlanned = lastMealPlanned.text(),
    lastMealActual = lastMealActual?.text(),
    clockStartPlanned = clockStartPlanned.text(),
    clockStartActual = clockStartActual?.text(),
    endActual = endActual?.text(),
    endReason = endReason?.code,
    endReasonDetail = endReasonDetail,
    endRuleId = endRuleId,
    tzOffset = tzOffset,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = null,
)

internal fun MeasurementEntity.toDomain() = Measurement(
    id, fastId, metricTypeCode, takenAt.localDateTime(), scheduledFor?.localDateTime(), valueNum, valueText, valueEntered,
    unitEntered, source?.let { codeOf<GlucoseSource>(it) }, trend?.let { codeOf<Trend>(it) }, onWaking, notDoneReason, note,
)

internal fun Measurement.toEntity(now: String) = MeasurementEntity(
    id, fastId, metricTypeCode, takenAt.text(), scheduledFor?.text(), valueNum, valueText, valueEntered, unitEntered,
    source?.code, trend?.code, onWaking, notDoneReason, note, now, null, null,
)

internal fun BloodPressureReadingEntity.toDomain() = BloodPressureReading(
    id, fastId, takenAt.localDateTime(), scheduledFor?.localDateTime(), systolic, diastolic, pulse, codeOf<Position>(position),
    onWaking, pairedReadingId, notDoneReason, note,
)

internal fun BloodPressureReading.toEntity(now: String) = BloodPressureReadingEntity(
    id, fastId, takenAt.text(), scheduledFor?.text(), systolic, diastolic, pulse, position.code, onWaking, pairedReadingId,
    notDoneReason, note, now, null, null,
)

internal fun AlertEntity.toDomain() = Alert(
    id, fastId, ruleId, firedAt.localDateTime(), triggerEntryIds.toStringList(), userChoice?.let { codeOf<UserChoice>(it) }, choiceReason,
)

internal fun Alert.toEntity(createdAt: String, updatedAt: String?) = AlertEntity(
    id, fastId, ruleId, firedAt.text(), triggerEntryIds.toJsonStrings(), userChoice?.code, choiceReason, createdAt, updatedAt, null,
)

internal fun EditLogEntity.toDomain() = EditLogEntry(id, entityName, rowId, codeOf<EditAction>(action), editedAt.localDateTime(), field, oldValue, newValue, reason)

internal fun EditLogEntry.toEntity(now: String) =
    EditLogEntity(id, entityName, rowId, action.code, editedAt.text(), field, oldValue, newValue, reason, now, null)

private fun PlanOwner.templateId(): String? = (this as? PlanOwner.OfTemplate)?.id
private fun PlanOwner.fastId(): String? = (this as? PlanOwner.OfFast)?.id
