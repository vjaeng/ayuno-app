# Transacciones clave — ayuno-app

Operaciones cuyo fallo parcial dejaría datos inconsistentes (doc 12). Todas se ejecutan dentro de `TransactionRunner` (Room `withTransaction`, ADR-003): o se confirma todo, o nada. Los efectos externos (alarmas, notificaciones) ocurren **después** de confirmar.

| Transacción | Archivo | Historias | Estado |
|---|---|---|---|
| Crear ayuno desde plantilla | [crear-ayuno.md](crear-ayuno.md) | H-004 | construida |
| Empezar el ayuno | [empezar-ayuno.md](empezar-ayuno.md) | H-005, H-057 | construida (revisión previa y `started_with_warnings`, EV-006) |
| Guardar una entrada y evaluar reglas | [guardar-entrada.md](guardar-entrada.md) | H-008, H-009, H-010, H-012, H-019–H-022, H-027, H-049, H-050, H-051, H-053–H-056 | construida (palabra elegida, EV-005; medida casera, botón rápido, agua con sal, lectura de pie y condiciones entre lecturas, EV-006) |
| «Algo no va bien» | [algo-no-va-bien.md](algo-no-va-bien.md) | H-023, H-027 | construida |
| Corregir una hora del ayuno con bitácora | [corregir-hora.md](corregir-hora.md) | H-005 | construida |
| Terminar el ayuno | [terminar-ayuno.md](terminar-ayuno.md) | H-030, H-031 | construida |
| Exportar | [exportar.md](exportar.md) | H-036, H-037, H-038 | construida |
| Importar o restaurar | [importar.md](importar.md) | H-039, H-040 | construida (formato 1.2, EV-006) |
| Cambiar un ayuno planificado | [cambiar-ayuno.md](cambiar-ayuno.md) | H-044 | construida |
| Borrar un ayuno para siempre | [borrar-ayuno.md](borrar-ayuno.md) | H-045 | construida |
| Corregir, borrar o restaurar una entrada | [corregir-entrada.md](corregir-entrada.md) | H-034, H-048, H-049, H-051, H-055 | construida (mediciones y presión, EV-004; con palabra, EV-005; «Deshacer» de ingesta y «de partida», EV-006); corregir ingestas pendiente de H-034 |

Previstas, aún sin construir (ARQUITECTURA §4): duplicar plantilla, copia cifrada.

**Efecto externo común — reprogramar recordatorios.** Tras confirmar cualquiera de estas transacciones, la app llama a `ReminderScheduler.reschedule()`. Es idempotente (recalcula la siguiente alarma desde los datos y la reemplaza con el mismo `PendingIntent`); si falla o la app muere antes, la reprogramación ocurre al siguiente arranque, reinicio o cambio de hora (H-016).
