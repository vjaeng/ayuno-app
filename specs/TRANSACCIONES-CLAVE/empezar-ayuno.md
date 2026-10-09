# TRX: Empezar el ayuno

**Historias:** H-005 · **Disparador:** botón «Empieza el ayuno ahora»
**Entidades tocadas:** `fast` (U: `status`, `clock_start_actual`, `tz_offset`)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as Inicio
    participant UC as StartFast
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UC: 1. id del ayuno
    UC->>DB: 2. BEGIN · leer ayuno · comprobar que ninguno está en curso
    UC->>DB: 3. actualizar estado, hora real con segundos y desfase · COMMIT
    UI->>R: 4. reprogramar recordatorios desde la hora real
```

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Ya hay otro ayuno en curso, o este no está planificado | `ANOTHER_FAST_IN_PROGRESS` / `FAST_NOT_PLANNED`; nada cambia |
| 3 | Fallo al escribir | Rollback: el ayuno sigue planificado |
| 4 | La reprogramación falla | El ayuno está en curso; alarmas al siguiente arranque o reinicio |

## Idempotencia
La comprobación «solo uno en curso» y el cambio de estado van en la **misma transacción**: un doble toque deja el ayuno en curso una sola vez; el segundo intento devuelve `FAST_NOT_PLANNED` sin cambiar la hora real.

Pruebas: `TemplateAndFastUseCasesTest` (H-005), `RepositoriesTest`, `FastLifecycleE2eTest`.
