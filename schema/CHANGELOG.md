# Formato `fasting-log` — registro de cambios

El esquema vigente `fasting-log-1.2.schema.json` (JSON Schema draft 2020-12) se **genera** desde la descripción del
formato en `data/src/main/java/com/ayuno/data/exchange/ExchangeFormat.kt`; una prueba (`ExchangeTest`) falla si el
archivo publicado no coincide. Para regenerarlo: `./gradlew :data:testDebugUnitTest -PregenerateSchema=true --tests '*ExchangeTest*'`
(reescribe también `fasting-log-1.2.example.json`, un ayuno simulado completo). Un cambio incompatible sube la
versión mayor; la app rechaza importar una versión mayor que la suya (ADR-007). Los archivos de versiones anteriores
(`fasting-log-1.0.*`, `fasting-log-1.1.*`) se conservan como referencia.

## 1.2 — 2026-10-26 (EV-006, app 0.14.0)

- **Medidas caseras** (MODELO-DATOS D-22, H-050): `catalogs.household_units`, una fila por medida con `intake_type_code`,
  `code`, `name`, `equivalent` (en la unidad del tipo), `imprecise` y `position`. Las 7 de fábrica (vaso y taza de agua e
  infusión; cucharadita rasa, ¼ de cucharadita y pizca de sal) tienen identificadores fijos. `intake_types` suma
  `quick_unit_code` (botón rápido, H-051).
- **Ingestas**: `amount_entered` y `unit_entered` (lo tecleado, en la unidad del tipo o una medida suya) y `group_id`
  («agua con sal» en una sola anotación). `amount` y `unit` siguen siendo el equivalente en la unidad del tipo.
- **Metas**: `intake_goals.daily_amount` puede ser `null` (meta sin cifra) y suma `nudge_after_hours` (H-052).
- **Presión entre lecturas** (D-24, H-053 a H-056): `blood_pressure_readings.seconds_standing` e `is_baseline`;
  `measurements.is_baseline`; `plan_metrics.standing_check_seconds` (lista de enteros, solo en presión); `rules` suma los
  tipos `pair_difference`, `change_from_baseline` y `trend` con `direction`, `trend_count`, `trend_of`, y `goal_percent`
  para `intake_below_goal`.
- **Revisión antes de empezar** (D-25, H-057): `rules.is_placeholder` (regla «de ejemplo») y
  `fasts.started_with_warnings`, lista de `{kind, ref_id, text}` con lo que quedaba pendiente al empezar «de todos modos».
- Cambio **aditivo**: la app sigue importando archivos 1.0 y 1.1. Lo que no traen toma su valor por defecto: lo tecleado
  de cada ingesta es su cantidad y unidad, las marcas son falsas, la presión de un plan toma los tiempos de pie 60 y 180 s
  y el resto queda vacío. Al importar se rechaza el archivo entero si una medida no tiene equivalencia positiva o repite
  `code` en su tipo, lo tecleado no está en la unidad del tipo ni en una de sus medidas, el botón rápido apunta a una
  medida ajena o a un tipo que rompe el ayuno, una regla nueva no trae dirección o su racha es menor de 3, hay segundos de
  pie en una lectura que no es de pie enlazada a una sentada del mismo ayuno, o hay dos «de partida» de lo mismo.

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
