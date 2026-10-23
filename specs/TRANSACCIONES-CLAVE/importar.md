# TRX: Importar o restaurar

**Historias:** H-039, H-040 · **Disparador:** «Importar archivo…» (Más → Exportar e importar) → resumen → «Importar»
**Entidades tocadas:** creación o actualización de cualquier entidad del formato por su clave (`id`, o `code` en los catálogos)

## Secuencia
```mermaid
sequenceDiagram
    participant UI as Exportar e importar
    participant UC as PreviewImport / ImportData
    participant X as JsonDataExchange
    participant DB as Room (transacción)
    participant R as ReminderScheduler
    UI->>UI: 1. leer el archivo elegido hasta 50 MB (más = rechazo sin leerlo entero)
    UI->>UC: 2. resumen previo
    UC->>X: 3. validar todo: formato, versión, tipos, nulos, enumerados, horas, propietario del plan y referencias
    X-->>UI: 4. recuentos (nuevos / ya estaban) y campos ignorados — nada escrito
    UI->>UC: 5. «Importar»
    UC->>X: 6. validar otra vez y escribir
    X->>DB: 7. BEGIN · por cada fila: nueva → insertar; existente → gana `updated_at` más reciente · COMMIT
    UI->>R: 8. reprogramar recordatorios
```

**Palabras de las mediciones (EV-005):** `metric_options` viaja en `catalogs` desde el formato 1.1 y se importa como cualquier catálogo por su `id` (gana el `updated_at` más reciente). Se valida que su `metric_type_code` exista en el archivo o en el teléfono y sea numérico, y que `value` quede dentro del rango del tipo. Un archivo 1.0 no trae la lista: las palabras del teléfono se conservan sin cambios. Las 20 de fábrica tienen identificadores fijos (como la plantilla de ejemplo), así que restaurar en otro teléfono no las duplica; si aun así la importación dejaría en un tipo dos palabras no borradas con el mismo texto o número, se rechaza con la primera fila en conflicto y nada se modifica.

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 1 | Archivo de más de 50 MB | «Demasiado grande»; nada se modifica |
| 3 / 6 | No es de la app, versión mayor, vacío o fila inválida | Mensaje con la primera fila inválida; nada se modifica |
| 7 | Fallo a mitad de escritura | Rollback: ninguna importación parcial (ADR-SEG-006) |
| 8 | La reprogramación falla | Alarmas recalculadas al siguiente arranque |

## Idempotencia
Importar el mismo archivo dos veces no crea nada nuevo: las filas existentes con la misma marca de tiempo se conservan.
Los triggers de propietario de la base rechazan además cualquier fila de plan sin exactamente un propietario.

Pruebas: `ExchangeTest` (restaurar en un teléfono nuevo, dos veces, campos desconocidos, versión mayor, filas inválidas, plantillas y productos), `Batch4E2eTest` (H-040).
