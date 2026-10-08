package com.ayuno.domain.usecase

import com.ayuno.domain.model.Alert
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.port.AlertRepository

/**
 * H-027: la persona decide ante cada alerta. Elegir «termino el ayuno» solo registra la decisión: el fin lo hace
 * la persona en «Terminar el ayuno» (H-030). Ninguna alerta cambia el ayuno por su cuenta.
 */
class DecideAlert(private val alerts: AlertRepository) {
    suspend operator fun invoke(alertId: String, choice: UserChoice, reason: String?): Outcome<Alert> {
        val alert = alerts.get(alertId) ?: return invalid(ValidationError.ALERT_NOT_FOUND)
        if (alert.userChoice != null) return invalid(ValidationError.ALERT_ALREADY_DECIDED)
        val trimmed = reason?.trim()?.ifEmpty { null }
        if (choice == UserChoice.CONTINUE && trimmed == null) return invalid(ValidationError.REASON_REQUIRED)
        val decided = alert.copy(userChoice = choice, choiceReason = trimmed.takeIf { choice == UserChoice.CONTINUE })
        alerts.update(decided)
        return Outcome.Ok(decided)
    }
}
