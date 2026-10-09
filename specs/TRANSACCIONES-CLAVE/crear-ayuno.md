# TRX: Crear ayuno desde plantilla

**Historias:** H-004 · **Disparador:** botón «Crear ayuno»
**Entidades tocadas:** `fast` (C), `stage` (C, copia), `plan_metric` (C, copia), `metric_schedule` (C, copia), `rule` (C, copia)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as CreateFastScreen
    participant UC as CreateFast
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UC: 1. plantilla, última comida, inicio previsto, objetivo, notas
    UC->>DB: 2. leer plantilla y su plan
    UC->>UC: 3. validar y copiar el plan con ids nuevos
    UC->>DB: 4. BEGIN · insertar fast · insertar copia del plan · COMMIT
    UI->>R: 5. reprogramar recordatorios
```

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Plantilla inexistente | `TEMPLATE_NOT_FOUND`; nada se escribe |
| 3 | Validación (inicio antes de la última comida, objetivo ≤ 0) | Errores tipados; nada se escribe |
| 4 | Fallo a mitad de la escritura | Rollback: no queda ayuno sin plan ni plan sin ayuno |
| 5 | La reprogramación falla | El ayuno existe; las alarmas se recalculan al siguiente arranque |

## Idempotencia
Cada pulsación crea un ayuno distinto (no es idempotente por diseño: se pueden planificar varios). El botón queda deshabilitado mientras guarda y la pantalla se cierra al confirmar, para evitar el doble toque.

Pruebas: `TemplateAndFastUseCasesTest` (H-004), `RepositoriesTest` (copia en Room), `RoomTransactionRunnerTest` (rollback), `FastLifecycleE2eTest`.
