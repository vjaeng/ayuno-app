package com.ayuno.domain.model

/** Valores de los ENUM del modelo; `code` es el valor canónico persistido y exportado. */
interface Coded {
    val code: String
}

inline fun <reified T> codeOf(code: String): T where T : Enum<T>, T : Coded =
    enumValues<T>().firstOrNull { it.code == code } ?: error("Código desconocido para ${T::class.simpleName}: $code")

enum class StageKind(override val code: String) : Coded { PREPARATION("preparation"), FAST("fast"), REFEED("refeed"), POST("post") }

enum class DurationUnit(override val code: String) : Coded { DAYS("days"), HOURS("hours") }

enum class FastStatus(override val code: String) : Coded { PLANNED("planned"), IN_PROGRESS("in_progress"), FINISHED("finished") }

enum class EndReason(override val code: String) : Coded {
    GOAL_MET("goal_met"), ALERT_RULE("alert_rule"), PERSONAL_DECISION("personal_decision"), OTHER("other"),
}

enum class GlucoseSource(override val code: String) : Coded { CGM("cgm"), GLUCOMETER("glucometer") }

enum class Trend(override val code: String) : Coded {
    RISING_FAST("rising_fast"), RISING("rising"), STEADY("steady"), FALLING("falling"), FALLING_FAST("falling_fast"),
}

enum class Position(override val code: String) : Coded { SITTING("sitting"), STANDING("standing"), LYING("lying") }

enum class BpComponent(override val code: String) : Coded { SYSTOLIC("systolic"), DIASTOLIC("diastolic"), PULSE("pulse") }

enum class RuleConditionKind(override val code: String) : Coded {
    METRIC_BELOW("metric_below"), METRIC_ABOVE("metric_above"), ALARM_SYMPTOM("alarm_symptom"),
    INTAKE_BELOW_GOAL("intake_below_goal"), MANUAL("manual"),
}

enum class RuleLevel(override val code: String) : Coded { NOTICE("notice"), ALERT("alert") }

enum class RuleAction(override val code: String) : Coded { NONE("none"), SCHEDULE_MEASUREMENT("schedule_measurement"), OFFER_END("offer_end") }

enum class UserChoice(override val code: String) : Coded { END_FAST("end_fast"), CONTINUE("continue") }

enum class EditAction(override val code: String) : Coded { EDIT("edit"), DELETE("delete"), RESTORE("restore") }

enum class ProductKind(override val code: String) : Coded { SUPPLEMENT("supplement"), MEDICATION("medication") }

enum class DuringFast(override val code: String) : Coded { CONTINUES("continues"), STOPS("stops"), OPTIONAL("optional") }

enum class ProductLogStatus(override val code: String) : Coded { TAKEN("taken"), NOT_TAKEN("not_taken"), DOSE_CHANGED("dose_changed") }

enum class RefeedStepKind(override val code: String) : Coded { TIMED("timed"), REINTRODUCTION("reintroduction") }

enum class MealTolerance(override val code: String) : Coded {
    FINE("fine"), HEAVINESS("heaviness"), NAUSEA("nausea"), DIARRHEA("diarrhea"), OTHER("other"),
}
