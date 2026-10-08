package com.ayuno.domain.model

import java.time.LocalDateTime

data class EditLogEntry(
    val id: String,
    val entityName: String,
    val rowId: String,
    val action: EditAction,
    val editedAt: LocalDateTime,
    val field: String?,
    val oldValue: String?,
    val newValue: String?,
    val reason: String?,
)
