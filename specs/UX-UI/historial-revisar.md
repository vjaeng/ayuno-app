# Flujo: Historial

**Historias que cubre:** H-033, H-034, H-035 (notas en la línea de tiempo), H-045 (borrar un ayuno para siempre), H-048 (ver y corregir las mediciones del ayuno en curso)
**Entrada:** pestaña Historial; Inicio → último ayuno; Inicio en curso → «Mediciones de este ayuno»; Hoy → tocar un hecho · **Salida:** Exportar; formulario de corrección

## Pantallas

### Pantalla: Lista de ayunos

```
┌──────────────────────────────────┐
│ Historial                        │
│ ● En curso · desde jueves 22 oct │
│   33 h de 57 h                   │
│ domingo 25 oct · 57 h 3 min      │
│   objetivo 57 h · objetivo       │
│   cumplido                       │
│ sábado 12 sep · 30 h 40 min      │
│   objetivo 36 h · decisión       │
│   personal                       │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Estado (solo `in_progress` y `finished`; los planificados viven en [ayuno-planificados.md](ayuno-planificados.md), EV-002) | `fast.status` | lectura |
| Fecha de inicio | `fast.clock_start_actual` | lectura |
| Duración final | duración final | derivado |
| Objetivo | `fast.target_hours` | lectura |
| Motivo de fin | `fast.end_reason`, `fast.end_reason_detail`, `fast.end_rule_id` → `rule.name` | lectura |

**Acciones:** tocar → Detalle del ayuno.
**Estados:** vacío («Aún no has hecho ningún ayuno» + enlace a Planificados) · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Detalle del ayuno (línea de tiempo)

```
┌──────────────────────────────────┐
│ ← Ayuno · 22 al 25 oct [Exportar]⋮│
│ 57 h 3 min · objetivo cumplido   │
│ Peso: 71.4 kg → 68.9 kg · −2.5 kg│
│ Notas: «Semana tranquila»        │
│ [Todo ▾]  (filtro por tipo)      │
│──────────────────────────────────│
│ viernes 23 de octubre            │
│ Totales: Agua 2.1 L · Sal 4 g    │
│  (sodio 1.57 g)                  │
│ 07:02 hora 9  Glucosa 72 mg/dL   │
│ 07:10 hora 9  Presión 118/76     │
│               sentado            │
│ 12:01 hora 14 Glucosa 64 ▲ aviso │
│ 12:03 hora 14 Hambre 4/10        │
│ 12:10 hora 14 Nota «leve temblor»│
│ ...                              │
│ Borradas (1) ›                   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Cabecera: fechas, duración, motivo | `fast.clock_start_actual`, `fast.end_actual`, duración final, `fast.end_reason` | lectura / derivado |
| Línea de peso (H-047) | peso inicial, final y diferencia (derivado, D-19) | derivado; sin pesos no aparece; texto neutro, sin colores |
| Notas del ayuno | `fast.notes` | lectura / escritura (editar) |
| Totales por día | totales del día, sodio | derivado |
| Entrada: medición | `measurement.taken_at`, `metric_type.name`, `measurement.value_entered`, `measurement.unit_entered`, `measurement.not_done_reason` | lectura |
| Entrada: presión | `blood_pressure_reading.taken_at`, `.systolic`, `.diastolic`, `.pulse`, `.position` | lectura |
| Entrada: síntoma | `symptom.noted_at`, `symptom_type.name`, `symptom.severity` | lectura |
| Entrada: ingesta | `intake.taken_at`, `intake_type.name`, `intake.amount`, `intake.unit` | lectura |
| Entrada: toma de producto | `product_log.taken_at`, `product.name`, `product_log.status`, `product_log.amount` | lectura |
| Entrada: alerta | `alert.fired_at`, `rule.name`, `rule.level`, `alert.user_choice`, `alert.choice_reason` | lectura |
| Entrada: nota | `note.noted_at`, `note.text` | lectura |
| Entrada: paso de vuelta a comer | `refeed_check.done_at`, `refeed_step.description` | lectura |
| Entrada: comida | `meal.eaten_at`, `meal.description`, `meal.tolerance` | lectura |
| Hora del ayuno junto a cada hora | hora del ayuno | derivado |
| «Borradas» | entradas con `deleted_at` (control) | lectura |
| Botón «Exportar» | — | acción → [datos-exportar-importar.md](datos-exportar-importar.md) |
| Menú ⋮ → «Borrar ayuno» | `fast` y todo lo que cuelga de él (D-15) | acción irreversible (confirmación) (H-045) |

**Acciones:** tocar una entrada → Detalle de entrada · Exportar · filtro por tipo · «Borradas» → lista con «Restaurar» · menú ⋮ → «Borrar ayuno» («Cambiar» un planificado se hace desde su detalle en Planificados, EV-002).

**Confirmación de «Borrar ayuno»:**

```
┌──────────────────────────────────┐
│ ¿Borrar este ayuno para siempre? │
│ Se borrarán el ayuno del 22 al   │
│ 25 oct y sus 48 registros, con   │
│ su plan y sus cambios. No se     │
│ puede deshacer.                  │
│ Los archivos que ya exportaste   │
│ no cambian.                      │
│ (en curso:) Sus recordatorios    │
│ dejarán de sonar.                │
│  [Cancelar]  [Borrar para siempre]│
└──────────────────────────────────┘
```

Recuento: derivado «recuento de registros del ayuno» (MODELO-DATOS §2.5). «Cancelar» es la opción por defecto y «Borrar para siempre» usa el estilo de botón secundario, sin rojo (`GUIA-ESTILO.md`: el rojo es solo para alertas). Al confirmar → borrado definitivo en una transacción (D-15, ADR-SEG-007) → Historial; si estaba en curso, se retiran sus notificaciones y se reprograman los recordatorios. Error al borrar → nada se borra, mensaje («No se pudo borrar el ayuno. No se ha borrado nada.») y reintento.

### Pantalla: Mediciones de este ayuno (H-048, EV-004)

Se abre desde Inicio en curso. Muestra solo mediciones y presiones del ayuno; la línea de tiempo completa sigue en el Detalle del ayuno.

```
┌──────────────────────────────────┐
│ ← Mediciones de este ayuno       │
│   En curso desde jueves 22 oct   │
│ [✓ Todas][Glucosa][Presión][Peso]│ → FiltroTipos (desplazable)
│──────────────────────────────────│
│ viernes 23 de octubre            │
│ 12:01  Glucosa 64 mg/dL        › │
│ hora 14 Sensor · subiendo ·      │
│         ✎ corregida              │
│ 11:02  Peso 70,2 kg            › │
│ hora 13                          │
│ 07:10  Presión 118/76 · pulso 62›│
│ hora 9  Sentado · al despertar   │
│ jueves 22 de octubre             │
│ 23:00  Cetonas · no se hizo    › │
│ hora 1  «No me quedaban tiras»   │
│ Borradas (1)                   › │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Subtítulo «En curso desde …» | `fast.status`, `fast.clock_start_actual` | lectura |
| Filtro por tipo (FiltroTipos: «Todas» + un chip por tipo con mediciones en el ayuno; «Presión» si hay tomas) | `measurement.metric_type_code` → `metric_type.name`; `blood_pressure_reading` | lectura |
| Encabezado de día | `measurement.taken_at` / `blood_pressure_reading.taken_at` (fecha con día de la semana) | lectura |
| Fila: hora real y hora del ayuno | `measurement.taken_at`, hora del ayuno | lectura / derivado |
| Fila medición: tipo, valor y unidad | `metric_type.name`, `measurement.value_entered`, `measurement.unit_entered`, `measurement.value_text` | lectura |
| Fila medición: detalle | `measurement.source`, `measurement.trend`, `measurement.on_waking` | lectura |
| Fila medición no hecha | `measurement.not_done_reason` | lectura |
| Fila presión | `blood_pressure_reading.taken_at`, `.systolic`, `.diastolic`, `.pulse`, `.position`, `.on_waking`, `.not_done_reason` | lectura |
| Marca «corregida» | `edit_log.row_id`, `edit_log.action = edit` | lectura |
| Lista y orden | derivado «Mediciones del ayuno» (MODELO-DATOS §2.5) | derivado |
| «Borradas (n) ›» | entradas con `deleted_at` (control) | lectura → lista con «Restaurar» |

**Acciones:** tocar una fila → Detalle de entrada (corregir o borrar) · elegir un filtro · «Borradas» → lista con «Restaurar».
**Estados:** vacío («Aún no has anotado ninguna medición en este ayuno» + BotonSecundario «Anotar» → [registro-anotar.md](registro-anotar.md); con un filtro sin resultados: «No hay mediciones de este tipo» + «Ver todas») · cargando (EstadoCargando con esqueleto de filas) · error (EstadoError «No se pudieron leer las mediciones. Reintentar.») · sin permisos (bloqueo activo).

### Pantalla: Detalle de entrada (corregir o borrar)

```
┌──────────────────────────────────┐
│ ← Glucosa                      ⋮ │
│ (formulario de registro con los  │
│  valores actuales, editable)     │
│ Motivo del cambio [__________]   │
│ ⓘ Si el valor nuevo cumple una   │
│   de tus reglas, verás su aviso. │
│ [          Guardar           ]   │  (Borrar: en el menú ⋮)
│──────────────────────────────────│
│ Cambios                          │
│ viernes 13:05 · valor 46 → 64 ·  │
│ «error de dedo»                  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Campos de la entrada | los del formulario correspondiente en [registro-anotar.md](registro-anotar.md) | escritura |
| Motivo del cambio | `edit_log.reason` | escritura (opcional; obligatorio para la hora de inicio del ayuno) |
| Historial de cambios | `edit_log.edited_at`, `edit_log.field`, `edit_log.old_value`, `edit_log.new_value`, `edit_log.reason`, `edit_log.action` | lectura |
| Menú ⋮ → «Borrar» (MenuAcciones) | `edit_log.action = delete` (+ control `deleted_at`) | acción (DialogoConfirmacion) |
| Texto «Si el valor nuevo cumple una de tus reglas, verás su aviso» (solo con el ayuno en curso) | — | lectura |
| Botón «Restaurar» (si borrada) | `edit_log.action = restore` | acción |

**Entrada «no se hizo»:** solo se corrigen su hora y su motivo; para anotar el valor se usa «Anotar» (una entrada nueva). Una presión de pie conserva su vínculo con la sentada (`blood_pressure_reading.paired_reading_id` no se edita) y la diferencia se recalcula (EV-004).

**Acciones:** Guardar (deshabilitado mientras no haya cambios) → cada campo cambiado escribe una fila en `edit_log`; la entrada conserva su identificador; con el ayuno en curso se evalúan las reglas con el valor nuevo y, si alguna se cumple → [seguridad-alertas.md](seguridad-alertas.md) (D-20); después vuelve a la lista con el Snackbar «Glucosa corregida». Borrar → confirmación «¿Borrar esta medición? Glucosa 64 mg/dL del viernes 23 oct, 12:01. Dejará de verse, pero queda en «Borradas» y la puedes restaurar.» [Borrar] [Cancelar] (Cancelar a la derecha, sin rojo) → borrado blando y fila en `edit_log`; no evalúa reglas. Restaurar → igual. Las alertas no se pueden borrar ni editar su decisión (registro de seguridad, ver decisión abierta).
**Estados:** vacío (sin cambios: no se muestra la sección «Cambios») · cargando · error (mismas validaciones del formulario; fallo al guardar conserva lo editado) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** lista: 1 acción. Mediciones de este ayuno: 1 acción (tocar una fila) + filtro. Detalle: Exportar + tocar entrada; «Cambiar» y «Borrar ayuno» van en el menú ⋮ (acciones raras, lejos del pulgar). Edición: 1 principal (Guardar); Borrar en el menú ⋮ (EV-004). ✔
- **Jakob:** lista → detalle → edición; línea de tiempo cronológica por días. ✔
- **Fitts:** filas completas tocables; Guardar en zona del pulgar. ✔
- **Miller:** la línea de tiempo se agrupa por día y se puede filtrar por tipo (9 tipos de entrada); la lista de mediciones, igual, por día y por tipo. ✔

## Decisiones de producto

*Implementación (EV-001, 2026-10-10):* «Cambiar» y «Borrar ayuno» en un MenuAcciones (⋮) de la barra superior del detalle (componente añadido a `GUIA-ESTILO.md`). Los avisos visibles de un ayuno borrado se retiran porque cada notificación lleva su `fast_id`.

*Implementación (tanda 3, 2026-10-10):* construidas la lista y la línea de tiempo por días con totales de ingesta. Quedan para su historia: «Exportar» (H-036), «Borradas» y tocar una entrada para corregirla (H-034). El filtro por tipo queda pendiente: con la agrupación por día la lista se mantiene legible y ningún criterio lo exige; se añadirá con H-034.

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Alertas:** ¿se permite borrar o editar una alerta registrada? Propuesta: no (S8: registro de seguridad). *EV-001:* al borrar el ayuno entero, sus alertas se borran con él (ADR-SEG-007); una alerta suelta sigue sin poder borrarse.
- **Editar un ayuno terminado:** ¿se permite corregir entradas de ayunos ya terminados? Propuesta: sí, siempre con rastro.

*Cambio EV-003 (2026-10-13):* la cabecera del detalle muestra el peso inicial, el final y la diferencia. Motivo: ver cuánto cambió el peso en cada ayuno sin hacer la cuenta (H-047); solo en el detalle, no en la lista (decisión del humano).

*Cambio EV-004 (2026-10-14):* pantalla nueva «Mediciones de este ayuno» (desde Inicio en curso) con filtro por tipo y marca «corregida»; en el Detalle de entrada, «Borrar» pasa al menú ⋮ (una sola acción principal) y, con el ayuno en curso, corregir vuelve a evaluar las reglas (D-20). Alcance de esta entrega: mediciones y presión; el resto de entradas sigue sin poder corregirse hasta completar H-034. Motivo: el humano necesita ver y corregir las mediciones del ayuno en curso (H-048). Mockup: https://claude.ai/artifact/YPvGTb3NZyPPo7S6Bvn3qx

*Implementación (EV-004, 2026-10-14):* «Borradas (n)» de las mediciones se despliega en la misma lista (como en Plantillas) en vez de abrir otra pantalla. Tras guardar se vuelve a la lista sin Snackbar: la app no tiene ese componente construido y la marca «corregida» ya confirma el cambio. El filtro solo ofrece los tipos que tienen mediciones y vuelve a «Todas» si un tipo se queda vacío, así que el estado «No hay mediciones de este tipo» no puede darse y no se construyó. El detalle del ayuno añade la fila «Mediciones de este ayuno ›» (también para ayunos terminados) y sus mediciones y presiones de la línea de tiempo se pueden tocar para corregirlas; el resto de entradas, cuando se complete H-034.
