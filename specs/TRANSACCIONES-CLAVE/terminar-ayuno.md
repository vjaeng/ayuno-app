# TRX: Terminar el ayuno

**Historias:** H-030, H-031 · **Disparador:** «Terminar el ayuno» (Inicio → reloj, Seguridad o alerta)
**Entidades tocadas:** `fast` (U: `status`, `end_actual`, `end_reason`, `end_reason_detail`, `end_rule_id`) · `refeed_check` (C: `refeed_step_id`, `scheduled_at`)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as EndFastScreen
    participant UC as EndFast
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UC: 1. hora de fin, motivo, detalle o regla
    UC->>DB: 2. BEGIN · leer ayuno y su plan
    UC->>UC: 3. validar (en curso, fin ≥ inicio y ≤ ahora, motivo completo)
    UC->>DB: 4. actualizar el ayuno · crear las casillas de la vuelta a comer desde el fin real · COMMIT
    UI->>R: 5. reprogramar (los recordatorios del ayuno cambian de etapa)
```

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 3 | Validación | Errores tipados; el ayuno sigue en curso |
| 4 | Fallo al escribir (ayuno o casillas) | Rollback: el ayuno sigue en curso y sin casillas; nunca un ayuno terminado sin su vuelta a comer |
| 5 | La reprogramación falla | Alarmas recalculadas al siguiente arranque |

## Idempotencia
Un segundo intento sobre un ayuno ya terminado devuelve `FAST_NOT_IN_PROGRESS`; no se puede reanudar ni terminar dos veces.

**Vuelta a comer (H-031):** si el plan copiado tiene pasos, sus casillas (`refeed_check`, una por repetición) se calculan desde `fast.end_actual` y se insertan en la misma transacción. Las casillas no se recalculan después: el fin real ya no cambia.

Pruebas: `TemplateAndFastUseCasesTest` (H-030), `Batch3Test` (casillas en la misma operación), `RepositoriesTest`, `Repositories3Test`, `FastLifecycleE2eTest`, `Batch3E2eTest`.
