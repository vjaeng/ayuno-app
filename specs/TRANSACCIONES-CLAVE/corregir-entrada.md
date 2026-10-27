# TRX: Corregir, borrar o restaurar una entrada con bitácora

**Historias:** H-034, H-048, H-049, H-051 («Deshacer»), H-055 (marcar de partida) · **Disparador:** «Guardar», «Borrar» o «Restaurar» en el Detalle de entrada; «Marcar como de partida» / «Quitar de partida» en su menú ⋮; «Deshacer» del Snackbar del BotonRapido
**Entidades tocadas:** `measurement` o `blood_pressure_reading` (U; borrar y restaurar: control `deleted_at`; «de partida»: `is_baseline`), `intake` (solo «Deshacer»: control `deleted_at`), `edit_log` (C, una por campo cambiado o una por borrado/restauración), `alert` (C, solo al corregir con el ayuno en curso)
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

**Medición con palabras (EV-005, H-049):** el valor nuevo llega como el `id` de una `metric_option` y se resuelve en el paso 2 igual que al anotar ([guardar-entrada.md](guardar-entrada.md)); `edit_log` guarda los números anterior y nuevo de `value_entered` y `value_num`, no las palabras.

**Lectura de pie con segundos (EV-006, H-053):** `seconds_standing` nunca se edita directamente. Si se corrige `taken_at` de una lectura de pie con segundos, en el paso 2 se desplaza en la misma diferencia, así se conserva la hora de «Ya estoy de pie». Un resultado negativo es un error de validación y no se escribe nada. Queda una fila de `edit_log` por cada uno de los dos campos.

**Ingesta en medida casera (EV-006, H-050; cuando se construya H-034 para ingestas):**
- Si se corrige `amount_entered` con la misma medida, `amount` se recalcula con la proporción que guarda la propia fila (`amount ÷ amount_entered`), no con el catálogo de hoy, para que cambiar una equivalencia no reescriba nada (D-22).
- Si se cambia `unit_entered` a otra medida, se resuelve con su equivalencia vigente (`MEASURE_NOT_FOUND` si se quitó).
- Una ingesta de «Agua con sal» se corrige o se borra por separado y conserva su `group_id`.

## Secuencia — borrar o restaurar
1. BEGIN · leer la fila. 2. Fijar (borrar) o vaciar (restaurar) `deleted_at` · insertar `edit_log` con `action = delete` o `restore`. 3. COMMIT · reprogramar recordatorios (un pendiente que esa medición cumplía vuelve a estar pendiente o atrasado, y al revés). No se evalúan reglas (D-20).

## Secuencia — «Deshacer» del botón rápido (EV-006, H-051)
Es el borrado de arriba aplicado a la `intake` recién creada (`edit_log` con `action = delete` y `entity_name = intake`). Las alertas que hubiera disparado se quedan (S8). Después se reprograma la pregunta de H-052, porque su ancla vuelve a la ingesta anterior.

## Secuencia — marcar o quitar «de partida» (EV-006, H-055)
1. BEGIN · leer la fila (no borrada, con valor) y su ayuno. 2. Si se marca: buscar la que tenga `is_baseline = true` en el mismo ayuno, el mismo `metric_type_code` (en presión, la misma `position`) y ponerla a false, con su `edit_log`. 3. Poner `is_baseline` a true (o a false si se quita) en la fila, con su `edit_log` (`field = is_baseline`). 4. Si el ayuno está `in_progress`, evaluar las reglas `change_from_baseline` con el valor de partida nuevo frente a las lecturas posteriores (puede crear `alert`; las ya registradas no cambian). 5. COMMIT. Marcar la que ya es de partida no escribe nada. Una «no se hizo» no se puede marcar (`FIELD_NOT_EDITABLE`).

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Validación o fuera de rango sin confirmar | Nada se escribe; el formulario conserva lo editado |
| 2 | Intento de poner valor a una «no se hizo» o de cambiar `scheduled_for` / `paired_reading_id` | `FIELD_NOT_EDITABLE`; nada se escribe |
| 2 | La palabra elegida se quitó en Catálogos | `OPTION_NOT_FOUND`; nada se escribe; el formulario pide elegir otra |
| 3 | La fila ya no existe o está borrada (p. ej. se borró el ayuno) | `ENTRY_NOT_FOUND`; nada se escribe; vuelve a la lista con mensaje |
| 5–7 | Fallo a mitad | Rollback: nunca queda una corrección sin su rastro (S8) ni sin sus alertas |
| 8 | La app se cierra antes de mostrar la alerta | La alerta queda sin decisión e Inicio la vuelve a mostrar (H-027) |

## Idempotencia
Guardar sin cambios no escribe nada. Repetir la misma corrección ya aplicada no encuentra diferencias. Borrar una entrada ya borrada o restaurar una visible no hace nada. Las alertas ya registradas nunca se modifican; las nuevas siguen la lógica de episodios (ADR-006), así que corregir dos veces al mismo valor no duplica la alerta.

Pruebas: `H048CorrectMeasurementsTest` (dominio), `H048CorrectEntryRoomTest` (Room: atomicidad, exportación con el mismo id y la borrada marcada), `H048MeasurementsE2eTest`.

*Log:* 2026-10-14, EV-004 — creada al construir H-034 para mediciones y presión: corregir una entrada de seguridad debe dejar rastro y tener el mismo efecto sobre las reglas que anotarla bien (D-20).

*Log:* 2026-10-26, EV-006 (mini-Tejido): corregir la hora de una lectura de pie desplaza sus segundos; corregir una ingesta en medida casera usa la proporción de la fila.

*Log:* 2026-10-26, EV-006: «Deshacer» del botón rápido (borrado blando de la ingesta) y marcar o quitar «de partida», que mueve la marca en la misma transacción y deja `edit_log` de las dos filas (D-22, D-24).
