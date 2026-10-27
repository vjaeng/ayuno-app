# TRX: Empezar el ayuno

**Historias:** H-005, H-057 · **Disparador:** botón «Empieza el ayuno ahora» (Inicio o detalle del planificado) o «Empezar de todos modos» (Revisión antes de empezar)
**Entidades tocadas:** `fast` (U: `status`, `clock_start_actual`, `tz_offset`; con «de todos modos»: `started_with_warnings`); lee `rule`, `intake_goal` y `checklist_item` del plan del ayuno

## Secuencia
```mermaid
sequenceDiagram
    participant UI as Inicio
    participant UC as StartFast
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UC: 1. id del ayuno y si se eligió «de todos modos»
    UC->>DB: 2. BEGIN · leer ayuno · comprobar que ninguno está en curso
    UC->>DB: 2b. leer el plan del ayuno y calcular la revisión previa
    UC->>UC: 2c. con pendientes y sin «de todos modos» → PRE_START_REVIEW_PENDING (con la lista) · ROLLBACK
    UC->>DB: 3. actualizar estado, hora real con segundos y desfase; con «de todos modos» y pendientes, started_with_warnings = esa lista · COMMIT
    UI->>R: 4. reprogramar recordatorios desde la hora real (y las preguntas de H-052)
```

**Revisión antes de empezar (EV-006, H-057):** el primer toque en «Empieza el ayuno ahora» llama a `StartFast` sin «de todos modos». Si no hay pendientes, empieza (como antes de EV-006). Si los hay, devuelve `PRE_START_REVIEW_PENDING` con la lista sin escribir nada, y la UI abre la revisión. «Empezar de todos modos» vuelve a llamar con el indicador. La revisión se **recalcula en el paso 2b**, dentro de la misma transacción: lo que se guarda en `started_with_warnings` es lo que faltaba en el instante de empezar, aunque la persona arreglara algo entre medias. Si al recalcular ya no queda nada, se empieza sin lista (`started_with_warnings` nulo). Cada elemento guarda `kind`, `ref_id` y una copia de `text` (nombre de la regla, tipo de la meta o texto de la lista previa), para que el registro siga legible aunque luego se cambie el plan. Es registro de seguridad: nada lo edita después (S8, D-25).

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Ya hay otro ayuno en curso, o este no está planificado | `ANOTHER_FAST_IN_PROGRESS` / `FAST_NOT_PLANNED`; nada cambia |
| 2c | Hay pendientes y no se eligió «de todos modos» | `PRE_START_REVIEW_PENDING` con la lista; nada cambia; la UI abre la revisión (no es un error que se muestre) |
| 2b | No se pudo leer el plan | Rollback: el ayuno sigue planificado; la revisión muestra el error y no ofrece empezar a ciegas |
| 3 | Fallo al escribir | Rollback: el ayuno sigue planificado |
| 4 | La reprogramación falla | El ayuno está en curso; alarmas al siguiente arranque o reinicio |

## Idempotencia
La comprobación «solo uno en curso», la revisión previa y el cambio de estado van en la **misma transacción**: un doble toque deja el ayuno en curso una sola vez; el segundo intento devuelve `FAST_NOT_PLANNED` sin cambiar la hora real.

Pruebas: `TemplateAndFastUseCasesTest` (H-005), `RepositoriesTest`, `FastLifecycleE2eTest`.

*Log:* 2026-10-26, EV-006: revisión previa dentro de la transacción y `started_with_warnings` con «Empezar de todos modos» (H-057, D-25).
