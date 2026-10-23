# Formato `fasting-log` — registro de cambios

El esquema vigente `fasting-log-1.1.schema.json` (JSON Schema draft 2020-12) se **genera** desde la descripción del
formato en `data/src/main/java/com/ayuno/data/exchange/ExchangeFormat.kt`; una prueba (`ExchangeTest`) falla si el
archivo publicado no coincide. Para regenerarlo: `./gradlew :data:testDebugUnitTest -PregenerateSchema=true --tests '*ExchangeTest*'`
(reescribe también `fasting-log-1.1.example.json`, un ayuno simulado completo). Un cambio incompatible sube la
versión mayor; la app rechaza importar una versión mayor que la suya (ADR-007). Los archivos de versiones anteriores
(`fasting-log-1.0.*`) se conservan como referencia.

## 1.1 — 2026-10-23 (EV-005, app 0.13.0)

- **Palabras de las mediciones** (MODELO-DATOS D-21, H-049): `catalogs.metric_options`, una fila por palabra con
  `metric_type_code`, `label`, `value`, `position` y las columnas de control. Las 20 de fábrica (hambre, energía, ánimo y
  claridad mental) tienen identificadores fijos, así que restaurar en otro teléfono no las duplica.
- Cambio **aditivo**: la app sigue importando archivos 1.0 (sin la lista se conservan las palabras del teléfono).
  Al importar se rechaza el archivo entero si alguna palabra es de una medición no numérica, queda fuera de su rango o
  repite texto o número en su medición.
- Las mediciones no cambian: una medición anotada con palabra guarda su número en `value_entered` y `value_num`.

## 1.0 — aclaración 2026-10-10 (EV-001, sin cambio de versión)

- **Ayuno borrado** (MODELO-DATOS D-15, ADR-SEG-007): borrar un ayuno es definitivo; un archivo exportado después ya no
  contiene ninguna de sus filas. Un archivo exportado antes sí, e importarlo lo devuelve.
- **Plan reemplazado** (D-16): al cambiar la plantilla de un ayuno planificado, la copia anterior del plan viaja con
  `deleted_at` y la nueva con identificadores nuevos.

## 1.0 — 2026-10-10 (app 0.5.0)

Primera versión publicada. Diferencias con la estructura de ejemplo de la solicitud (`docs/SOLICITUD_app_movil_ayuno.md`):

- **Claves canónicas del modelo** (MODELO-DATOS D-01) en listas y campos: cada lista se llama como su entidad en plural
  (`measurements`, `blood_pressure_readings`, `product_logs`, `plan_products`, `edit_logs`…) y cada fila lleva todas las
  columnas canónicas de la entidad y de control (`id`, `created_at`, `updated_at`, `deleted_at`), con `null` explícito.
  En la medición: `metric_type_code` (no `metric_code`); no hay `unit`, `context` ni `group_id` (no están en el modelo:
  la unidad del valor convertido es la del catálogo).
- **Listas propias** para la presión (`blood_pressure_readings`, D-09), los pasos y casillas de la vuelta a comer
  (`refeed_steps`, `refeed_checks`) y las comidas (`meals`) (D-04).
- **Plan estructurado** (D-02): `stages`, `plan_metrics`, `metric_schedules`, `rules`, `intake_goals`, `checklist_items`,
  `stage_notices`, `plan_products`, `refeed_steps`, cada fila con su propietario (`template_id` o `fast_id`, exactamente uno).
- `catalogs` incluye también `product_schedules` (D-05).
- `person` usa los campos canónicos de `person_profile`: `full_name`, `id_number`, `birth_date` (la solicitud decía `name`).
  Va con `null` salvo que la persona active su envío (H-038). El contacto de emergencia nunca se exporta (ADR-SEG-003).
- Los derivados (fin previsto, duración, totales) no viajan: se calculan (D-06).
