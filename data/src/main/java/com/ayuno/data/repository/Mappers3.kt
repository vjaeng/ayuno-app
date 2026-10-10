package com.ayuno.data.repository

import com.ayuno.data.db.MealEntity
import com.ayuno.data.db.RefeedCheckEntity
import com.ayuno.data.db.RefeedStepEntity
import com.ayuno.domain.model.Meal
import com.ayuno.domain.model.MealTolerance
import com.ayuno.domain.model.PlanOwner
import com.ayuno.domain.model.RefeedCheck
import com.ayuno.domain.model.RefeedStep
import com.ayuno.domain.model.RefeedStepKind
import com.ayuno.domain.model.codeOf

internal fun RefeedStepEntity.toDomain() =
    RefeedStep(id, codeOf<RefeedStepKind>(kind), description, offsetMinutes, repeatEveryMinutes, repeatForMinutes, dayOffset)
internal fun RefeedStep.toEntity(owner: PlanOwner, now: String) = RefeedStepEntity(
    id, (owner as? PlanOwner.OfTemplate)?.id, (owner as? PlanOwner.OfFast)?.id, kind.code, description,
    offsetMinutes, repeatEveryMinutes, repeatForMinutes, dayOffset, now, null, null,
)

internal fun RefeedCheckEntity.toDomain() = RefeedCheck(id, refeedStepId, scheduledAt.localDateTime(), doneAt?.localDateTime())
internal fun RefeedCheck.toEntity(createdAt: String, updatedAt: String?) =
    RefeedCheckEntity(id, refeedStepId, scheduledAt.text(), doneAt?.text(), createdAt, updatedAt, null)

internal fun MealEntity.toDomain() = Meal(id, fastId, eatenAt.localDateTime(), description, approxAmount, codeOf<MealTolerance>(tolerance), toleranceDetail)
internal fun Meal.toEntity(now: String) =
    MealEntity(id, fastId, eatenAt.text(), description, approxAmount, tolerance.code, toleranceDetail, now, null, null)
