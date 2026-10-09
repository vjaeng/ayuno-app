package com.ayuno.data.repository

import com.ayuno.data.db.ChecklistItemEntity
import com.ayuno.data.db.IntakeEntity
import com.ayuno.data.db.IntakeGoalEntity
import com.ayuno.data.db.IntakeTypeEntity
import com.ayuno.data.db.NoteEntity
import com.ayuno.data.db.PlanProductEntity
import com.ayuno.data.db.ProductEntity
import com.ayuno.data.db.ProductLogEntity
import com.ayuno.data.db.ProductScheduleEntity
import com.ayuno.data.db.StageNoticeEntity
import com.ayuno.data.db.SymptomEntity
import com.ayuno.data.db.SymptomTypeEntity
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.Intake
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Note
import com.ayuno.domain.model.PlanOwner
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLog
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.ProductSchedule
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.StageNotice
import com.ayuno.domain.model.Symptom
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.model.codeOf

private fun PlanOwner.tId(): String? = (this as? PlanOwner.OfTemplate)?.id
private fun PlanOwner.fId(): String? = (this as? PlanOwner.OfFast)?.id

internal fun SymptomTypeEntity.toDomain() = SymptomType(code, name, groupName, isAlarm, active)
internal fun SymptomType.toEntity(createdAt: String, updatedAt: String?) = SymptomTypeEntity(code, name, groupName, isAlarm, active, createdAt, updatedAt, null)

internal fun IntakeTypeEntity.toDomain() = IntakeType(code, name, unit, breaksFast, sodiumFactor)
internal fun IntakeType.toEntity(createdAt: String, updatedAt: String?) = IntakeTypeEntity(code, name, unit, breaksFast, sodiumFactor, createdAt, updatedAt, null)

internal fun ProductEntity.toDomain(schedules: List<ProductScheduleEntity>) = Product(
    code, name, codeOf<ProductKind>(kind), defaultAmount, unit, avoid, active,
    schedules.filter { it.productCode == code && it.deletedAt == null }.map { ProductSchedule(it.id, it.timeOfDay.localTime(), it.amount) },
)
internal fun Product.toEntity(createdAt: String, updatedAt: String?) = ProductEntity(code, name, kind.code, defaultAmount, unit, avoid, active, createdAt, updatedAt, null)

internal fun SymptomEntity.toDomain() = Symptom(id, fastId, symptomTypeCode, notedAt.localDateTime(), severity, note)
internal fun Symptom.toEntity(now: String) = SymptomEntity(id, fastId, symptomTypeCode, takenAt.text(), severity, note, now, null, null)

internal fun IntakeEntity.toDomain() = Intake(id, fastId, intakeTypeCode, takenAt.localDateTime(), amount, unit, note)
internal fun Intake.toEntity(now: String) = IntakeEntity(id, fastId, intakeTypeCode, takenAt.text(), amount, unit, note, now, null, null)

internal fun ProductLogEntity.toDomain() = ProductLog(
    id, fastId, productCode, scheduledAt?.localDateTime(), codeOf<ProductLogStatus>(status), takenAt?.localDateTime(), plannedAmount, amount, unit, reason,
)
internal fun ProductLog.toEntity(now: String) = ProductLogEntity(
    id, fastId, productCode, scheduledAt?.text(), status.code, takenAt?.text(), plannedAmount, amount, unit, reason, now, null, null,
)

internal fun NoteEntity.toDomain() = Note(id, fastId, notedAt.localDateTime(), text)
internal fun Note.toEntity(now: String) = NoteEntity(id, fastId, notedAt.text(), text, now, null, null)

internal fun IntakeGoalEntity.toDomain() = IntakeGoal(id, intakeTypeCode, dailyAmount, unit)
internal fun IntakeGoal.toEntity(owner: PlanOwner, now: String) = IntakeGoalEntity(id, owner.tId(), owner.fId(), intakeTypeCode, dailyAmount, unit, now, null, null)

internal fun ChecklistItemEntity.toDomain() = ChecklistItem(id, text, isChecked)
internal fun ChecklistItem.toEntity(owner: PlanOwner, now: String) = ChecklistItemEntity(id, owner.tId(), owner.fId(), text, isChecked, now, null, null)

internal fun StageNoticeEntity.toDomain() = StageNotice(id, text, stageKinds.toStringList().map { codeOf<StageKind>(it) })
internal fun StageNotice.toEntity(owner: PlanOwner, now: String) =
    StageNoticeEntity(id, owner.tId(), owner.fId(), text, stageKinds.map { it.code }.toJsonStrings(), now, null, null)

internal fun PlanProductEntity.toDomain() = PlanProduct(id, productCode, codeOf<DuringFast>(duringFast), optionalEnabled, reason)
internal fun PlanProduct.toEntity(owner: PlanOwner, now: String) =
    PlanProductEntity(id, owner.tId(), owner.fId(), productCode, duringFast.code, optionalEnabled, reason, now, null, null)
