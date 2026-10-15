# TRX: Corregir, borrar o restaurar una entrada con bitácora

**Historias:** H-034, H-048 · **Disparador:** «Guardar», «Borrar» o «Restaurar» en el Detalle de entrada
**Entidades tocadas:** `measurement` o `blood_pressure_reading` (U; borrar y restaurar: control `deleted_at`), `edit_log` (C, una por campo cambiado o una por borrado/restauración), `alert` (C, solo al corregir con el ayuno en curso)
**Alcance (EV-004):** mediciones y presión; el resto de entradas de H-034 seguirá esta misma secuencia cuando se construya.

## Secuencia — corregir
```mermaid
sequenceDiagram
    participant UI as Detalle de entrada
    participant UC as CorrectMeasurement / CorrectBloodPressure
    participant DB as Room (transacción)
    participant E as RuleEngine (puro)
    UI->>UC: 1. valores nuevos y motivo opcional
    UC->>UC: 2. validar como al anotar (S1), convertir unidad, comprobar rango (pide confirmación si hace falta); en una «no se hizo» solo hora y motivo; nunca scheduled_for ni paired_reading_id
    UC->>DB: 3. BEGIN · leer la fila actual (no borrada) y su ayuno
    UC->>UC: 4. comparar campo a campo; sin diferencias → fin, nada se escribe
    UC->>DB: 5. actualizar la fila (mismo id; value_num recalculado) · insertar una fila de edit_log por campo cambiado
    UC->>E: 6. si el ayuno está in_progress: evaluar reglas con la entrada corregida
    UC->>DB: 7. insertar alertas nuevas · COMMIT
    UI->>UI: 8. mostrar la alerta (si hay) o volver a la lista; reprogramar recordatorios
```

## Secuencia — borrar o restaurar
1. BEGIN · leer la fila. 2. Fijar (borrar) o vaciar (restaurar) `deleted_at` · insertar `edit_log` con `action = delete` o `restore`. 3. COMMIT · reprogramar recordatorios (un pendiente que esa medición cumplía vuelve a estar pendiente o atrasado, y al revés). No se evalúan reglas (D-20).

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Validación o fuera de rango sin confirmar | Nada se escribe; el formulario conserva lo editado |
| 2 | Intento de poner valor a una «no se hizo» o de cambiar `scheduled_for` / `paired_reading_id` | `FIELD_NOT_EDITABLE`; nada se escribe |
| 3 | La fila ya no existe o está borrada (p. ej. se borró el ayuno) | `ENTRY_NOT_FOUND`; nada se escribe; vuelve a la lista con mensaje |
| 5–7 | Fallo a mitad | Rollback: nunca queda una corrección sin su rastro (S8) ni sin sus alertas |
| 8 | La app se cierra antes de mostrar la alerta | La alerta queda sin decisión e Inicio la vuelve a mostrar (H-027) |

## Idempotencia
Guardar sin cambios no escribe nada. Repetir la misma corrección ya aplicada no encuentra diferencias. Borrar una entrada ya borrada o restaurar una visible no hace nada. Las alertas ya registradas nunca se modifican; las nuevas siguen la lógica de episodios (ADR-006), así que corregir dos veces al mismo valor no duplica la alerta.

Pruebas: `H048CorrectMeasurementsTest` (dominio), `H048CorrectEntryRoomTest` (Room: atomicidad, exportación con el mismo id y la borrada marcada), `H048MeasurementsE2eTest`.

*Log:* 2026-10-14, EV-004 — creada al construir H-034 para mediciones y presión: corregir una entrada de seguridad debe dejar rastro y tener el mismo efecto sobre las reglas que anotarla bien (D-20).
