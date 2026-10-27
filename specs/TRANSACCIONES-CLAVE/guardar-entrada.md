# TRX: Guardar una entrada y evaluar reglas

**Historias:** H-008, H-009, H-010, H-012, H-019, H-020, H-021, H-022, H-027, H-049, H-050, H-051, H-053, H-054, H-055, H-056 · **Disparador:** «Guardar» en un formulario de Anotar o en el Temporizador de pie; toque en un BotonRapido de Inicio
**Entidades tocadas:** `measurement`, `blood_pressure_reading`, `symptom` o `intake` (C; «agua con sal»: dos `intake`), `alert` (C, una por regla cumplida)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as Formulario
    participant UC as RecordMeasurement / RecordBloodPressure
    participant DB as Room (transacción)
    participant E as RuleEngine (puro)
    UI->>UC: 1. valor tecleado, unidad, hora…
    UC->>UC: 2. validar, convertir unidad, comprobar rango (pide confirmación si hace falta)
    UC->>DB: 3. BEGIN · insertar la entrada
    UC->>DB: 4. leer reglas del ayuno, entradas y alertas previas
    UC->>E: 5. evaluar
    UC->>DB: 6. insertar alertas · COMMIT
    UI->>UI: 7. mostrar la alerta (si hay) o volver a Inicio; reprogramar recordatorios
```

**Medición con palabras (EV-005, H-049):** en el paso 1 llega el `id` de la `metric_option` elegida en lugar de un valor tecleado. En el paso 2 se comprueba que la opción existe, no está borrada y es del mismo `metric_type_code`; su `value` pasa a `value_entered` y `value_num`, y `unit_entered = metric_type.entry_unit`. El resto de la secuencia (reglas incluidas) es idéntico. No se guarda el `id` de la opción (D-21).

**Ingesta en medida casera (EV-006, H-050):** en el paso 1 llegan `amount_entered` y `unit_entered`. En el paso 2, si `unit_entered` no es `intake_type.unit`, se busca la `household_unit` con ese `code` en el mismo `intake_type_code`, sin `deleted_at`; si no existe, `MEASURE_NOT_FOUND` y nada se escribe. Se calcula `amount = amount_entered × equivalent` y `unit = intake_type.unit`. Con la unidad del tipo, `amount = amount_entered`. Solo `amount` entra en totales, sodio y reglas (D-22).

**Botón rápido (EV-006, H-051):** la misma secuencia sin formulario. En el paso 1 llega solo el `intake_type_code`; el paso 2 lee `intake_type.quick_unit_code` (si es nulo o el tipo tiene `breaks_fast`, `QUICK_UNIT_NOT_SET` y nada se escribe), pone `amount_entered = 1`, `unit_entered` = esa medida y `taken_at` = ahora (`Clock`). El Snackbar con «Deshacer» se muestra en el paso 7, nunca antes del COMMIT. «Deshacer» es un borrado blando con `edit_log` ([corregir-entrada.md](corregir-entrada.md)); no toca las alertas que la ingesta hubiera disparado.

**Agua con sal (EV-006, H-051):** en el paso 1 llegan dos ingestas. El paso 2 valida y resuelve las dos; si una falla, no se escribe ninguna. En el paso 3 se insertan ambas con el mismo `group_id` (UUID nuevo) y el mismo `taken_at`. Los pasos 4-5 evalúan las reglas **una sola vez** con las dos ya insertadas, así que una regla de sal ve el total con la sal disuelta.

**Lectura de pie y condiciones entre lecturas (EV-006, H-053 a H-056):** una `blood_pressure_reading` de pie del temporizador llega con `paired_reading_id` y `seconds_standing`. En el paso 2 se comprueba que la sentada existe, no está borrada, es `sitting` y es del mismo ayuno (`PAIRED_READING_NOT_FOUND`), y que `seconds_standing ≥ 0`. En el paso 4, además de las reglas y las entradas previas, se cargan para el motor: la sentada enlazada (`pair_difference`), el valor de partida de cada medición y posición (`change_from_baseline`, derivado de `is_baseline` o la primera lectura) y la serie previa del mismo tipo, componente y momento de pie (`trend`; derivado «Momento de pie»). El motor sigue siendo puro (ADR-006) y aplica a estas condiciones los mismos episodios: una racha o una pareja que sigue cumpliendo no duplica la alerta.

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Validación o valor fuera de rango sin confirmar | Nada se escribe; el formulario conserva lo tecleado |
| 2 | La palabra elegida se quitó (o no es de esa medición) | `OPTION_NOT_FOUND`; nada se escribe; el formulario pide elegir otra |
| 2 | La medida casera se quitó o no es de ese líquido | `MEASURE_NOT_FOUND`; nada se escribe; el formulario conserva lo tecleado (EV-006) |
| 2 | Botón rápido sin medida o de un líquido que rompe el ayuno | `QUICK_UNIT_NOT_SET`; nada se escribe; Inicio quita el botón (EV-006) |
| 2 | La sentada enlazada no existe, está borrada o es de otro ayuno | `PAIRED_READING_NOT_FOUND`; nada se escribe (EV-006) |
| 3–6 | Fallo a mitad con «agua con sal» | Rollback de las dos ingestas: nunca queda el agua sin su sal (EV-006) |
| 3–6 | Fallo a mitad | Rollback: no queda una lectura sin su alerta (ni al revés) |
| 7 | La app se cierra antes de mostrar la alerta | La alerta queda sin decisión y Inicio la vuelve a mostrar al abrir (H-027) |

## Idempotencia
Cada guardado crea una entrada nueva (identificador UUID propio). El BotonRapido se deshabilita mientras guarda, así que un doble toque no duplica la ingesta; dos toques deliberados son dos ingestas. La evaluación usa **episodios** (ADR-006, Q-01): una regla no vuelve a dispararse mientras su racha siga abierta, así que guardar otra lectura igual no duplica la alerta.

Pruebas: `RecordAndAlertUseCasesTest`, `RuleEngineTest` (tabla §4.1 de PRUEBAS), `RepositoriesTest`, `RecordAndAlertE2eTest`.

*Log:* 2026-10-26, EV-006: se añaden la ingesta en medida casera, el botón rápido, «agua con sal» (dos filas, una sola evaluación) y la lectura de pie del temporizador con las condiciones `pair_difference`, `change_from_baseline` y `trend` (D-22, D-24).
