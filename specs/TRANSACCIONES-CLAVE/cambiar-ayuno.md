# TRX: Cambiar un ayuno planificado

**Historias:** H-044 · **Disparador:** «Guardar cambios» en el modo «Cambiar ayuno»
**Entidades tocadas:** `fast` (U: `last_meal_planned`, `clock_start_planned`, `target_hours`, `notes`, `template_id`, `milestone_hours`), `edit_log` (C); si cambia la plantilla: entidades *(plan)* del ayuno (D, blando, de la copia anterior; C de la nueva)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as CreateFastScreen (modo cambio)
    participant UC as ChangePlannedFast
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UC: 1. ayuno, plantilla, última comida, inicio y fin previstos, notas
    UC->>DB: 2. BEGIN · leer el ayuno, los demás ayunos y (si cambia) la plantilla nueva con su plan
    UC->>UC: 3. validar: status = planned, horas como al crear; si cambian las horas, semana de separación sin contar este ayuno (D-17)
    UC->>DB: 4. actualizar fast · si cambia la plantilla: marcar deleted_at en la copia anterior del plan e insertar la nueva con ids nuevos · una fila de edit_log por campo cambiado · COMMIT
    UI->>R: 5. reprogramar recordatorios
```

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Ayuno inexistente o borrado | `FAST_NOT_FOUND`; nada cambia |
| 2 | Plantilla nueva inexistente | `TEMPLATE_NOT_FOUND`; nada cambia |
| 3 | El ayuno ya no está planificado (empezó desde otra pantalla o una notificación) | `FAST_NOT_PLANNED`; nada cambia |
| 3 | Validación (inicio antes de la última comida, fin no posterior al inicio) | Errores tipados; nada cambia |
| 3 | Las horas nuevas quedan a menos de 7 días de otro ayuno (EV-002) | `FAST_TOO_CLOSE` con el ayuno con el que choca; nada cambia. Cambiar solo notas o plantilla no lo comprueba |
| 4 | Fallo a mitad de la escritura | Rollback: nunca queda un ayuno sin plan, con dos planes, ni un cambio sin su rastro (S8) |
| 5 | La reprogramación falla | El cambio está guardado; las alarmas se recalculan al siguiente arranque |

## Alertas ya registradas (nudo EV1-01)
Si el ayuno planificado ya tiene alertas, cambiar la plantilla se permite: las reglas que las dispararon quedan en la copia anterior (con `deleted_at`) y se siguen leyendo para mostrar la alerta y sus recordatorios pendientes de «medir de nuevo» (MODELO-DATOS D-16). Las reglas nuevas solo se evalúan a partir del cambio.

## Idempotencia
Guardar sin diferencias no escribe nada (ni `fast` ni `edit_log`). Repetir el mismo cambio tras confirmarlo no encuentra diferencias. Un cambio de plantilla repetido a la misma plantilla no vuelve a copiar el plan.

Pruebas: `H044ChangePlannedFastTest` (dominio), `ExchangeTest` «H-044 cambiar la plantilla…» (Room: copia anterior en blando, regla de la alerta, archivo válido e importable), `H045DeleteFastE2eTest` (acceso desde el detalle).
