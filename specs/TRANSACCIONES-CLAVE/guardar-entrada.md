# TRX: Guardar una entrada y evaluar reglas

**Historias:** H-008, H-009, H-010, H-012, H-019, H-020, H-021, H-022, H-027 · **Disparador:** «Guardar» en un formulario de Anotar
**Entidades tocadas:** `measurement`, `blood_pressure_reading`, `symptom` o `intake` (C), `alert` (C, una por regla cumplida)

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

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | Validación o valor fuera de rango sin confirmar | Nada se escribe; el formulario conserva lo tecleado |
| 3–6 | Fallo a mitad | Rollback: no queda una lectura sin su alerta (ni al revés) |
| 7 | La app se cierra antes de mostrar la alerta | La alerta queda sin decisión y Inicio la vuelve a mostrar al abrir (H-027) |

## Idempotencia
Cada guardado crea una entrada nueva (identificador UUID propio). La evaluación usa **episodios** (ADR-006, Q-01): una regla no vuelve a dispararse mientras su racha siga abierta, así que guardar otra lectura igual no duplica la alerta.

Pruebas: `RecordAndAlertUseCasesTest`, `RuleEngineTest` (tabla §4.1 de PRUEBAS), `RepositoriesTest`, `RecordAndAlertE2eTest`.
