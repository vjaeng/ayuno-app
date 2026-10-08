package com.ayuno.domain.id

import java.util.UUID

/** Identificadores estables UUID v4: no cambian al editar (base de la importación sin duplicados). */
object Ids {
    fun newId(): String = UUID.randomUUID().toString()
}
