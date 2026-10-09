# Transacciones clave — ayuno-app

Operaciones cuyo fallo parcial dejaría datos inconsistentes (doc 12). Todas se ejecutan dentro de `TransactionRunner` (Room `withTransaction`, ADR-003): o se confirma todo, o nada. Los efectos externos (alarmas, notificaciones) ocurren **después** de confirmar.

| Transacción | Archivo | Historias | Estado |
|---|---|---|---|
| Crear ayuno desde plantilla | [crear-ayuno.md](crear-ayuno.md) | H-004 | construida |
| Empezar el ayuno | [empezar-ayuno.md](empezar-ayuno.md) | H-005 | construida |
| Guardar una entrada y evaluar reglas | [guardar-entrada.md](guardar-entrada.md) | H-008, H-009, H-019, H-020, H-027 | construida |
| Corregir una hora del ayuno con bitácora | [corregir-hora.md](corregir-hora.md) | H-005 | construida |
| Terminar el ayuno | [terminar-ayuno.md](terminar-ayuno.md) | H-030 | construida (sin vuelta a comer: H-031) |

Previstas, aún sin construir (ARQUITECTURA §4): duplicar plantilla, corregir o borrar entradas (H-034), exportar, importar o restaurar, copia cifrada.

**Efecto externo común — reprogramar recordatorios.** Tras confirmar cualquiera de estas transacciones, la app llama a `ReminderScheduler.reschedule()`. Es idempotente (recalcula la siguiente alarma desde los datos y la reemplaza con el mismo `PendingIntent`); si falla o la app muere antes, la reprogramación ocurre al siguiente arranque, reinicio o cambio de hora (H-016).
