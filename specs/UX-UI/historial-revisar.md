# Flujo: Historial

**Historias que cubre:** H-033, H-034, H-035 (notas en la línea de tiempo), H-045 (borrar un ayuno para siempre)
**Entrada:** pestaña Historial; Inicio → último ayuno; Hoy → tocar un hecho · **Salida:** Exportar; formulario de corrección

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

### Pantalla: Detalle de entrada (corregir o borrar)

```
┌──────────────────────────────────┐
│ ← Glucosa                        │
│ (formulario de registro con los  │
│  valores actuales, editable)     │
│ Motivo del cambio [__________]   │
│ [Borrar]           [ Guardar ]   │
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
| Botón «Borrar» | `edit_log.action = delete` (+ control `deleted_at`) | acción (confirmación) |
| Botón «Restaurar» (si borrada) | `edit_log.action = restore` | acción |

**Acciones:** Guardar → cada campo cambiado escribe una fila en `edit_log`; la entrada conserva su identificador. Borrar → borrado blando y fila en `edit_log`. Restaurar → igual. Las alertas no se pueden borrar ni editar su decisión (registro de seguridad, ver decisión abierta).
**Estados:** vacío (sin cambios: no se muestra la sección «Cambios») · cargando · error (mismas validaciones del formulario; fallo al guardar conserva lo editado) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** lista: 1 acción. Detalle: Exportar + tocar entrada; «Cambiar» y «Borrar ayuno» van en el menú ⋮ (acciones raras, lejos del pulgar). Edición: Guardar / Borrar. ✔
- **Jakob:** lista → detalle → edición; línea de tiempo cronológica por días. ✔
- **Fitts:** filas completas tocables; Guardar en zona del pulgar. ✔
- **Miller:** la línea de tiempo se agrupa por día y se puede filtrar por tipo (9 tipos de entrada). ✔

## Decisiones de producto

*Implementación (EV-001, 2026-10-10):* «Cambiar» y «Borrar ayuno» en un MenuAcciones (⋮) de la barra superior del detalle (componente añadido a `GUIA-ESTILO.md`). Los avisos visibles de un ayuno borrado se retiran porque cada notificación lleva su `fast_id`.

*Implementación (tanda 3, 2026-10-10):* construidas la lista y la línea de tiempo por días con totales de ingesta. Quedan para su historia: «Exportar» (H-036), «Borradas» y tocar una entrada para corregirla (H-034). El filtro por tipo queda pendiente: con la agrupación por día la lista se mantiene legible y ningún criterio lo exige; se añadirá con H-034.

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Alertas:** ¿se permite borrar o editar una alerta registrada? Propuesta: no (S8: registro de seguridad). *EV-001:* al borrar el ayuno entero, sus alertas se borran con él (ADR-SEG-007); una alerta suelta sigue sin poder borrarse.
- **Editar un ayuno terminado:** ¿se permite corregir entradas de ayunos ya terminados? Propuesta: sí, siempre con rastro.
