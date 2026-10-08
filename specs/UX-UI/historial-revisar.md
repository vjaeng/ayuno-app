# Flujo: Historial

**Historias que cubre:** H-033, H-034, H-035 (notas en la línea de tiempo)
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
│ ○ Planificado · jueves 5 nov     │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Estado | `fast.status` | lectura |
| Fecha de inicio | `fast.clock_start_actual` (o `fast.clock_start_planned` si `planned`) | lectura |
| Duración final | duración final | derivado |
| Objetivo | `fast.target_hours` | lectura |
| Motivo de fin | `fast.end_reason`, `fast.end_reason_detail`, `fast.end_rule_id` → `rule.name` | lectura |

**Acciones:** tocar → Detalle del ayuno.
**Estados:** vacío («Aún no hay ayunos» + enlace a Nuevo ayuno) · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Detalle del ayuno (línea de tiempo)

```
┌──────────────────────────────────┐
│ ← Ayuno · 22 al 25 oct  [Exportar]│
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

**Acciones:** tocar una entrada → Detalle de entrada · Exportar · filtro por tipo · «Borradas» → lista con «Restaurar».
**Estados:** vacío (ayuno planificado o sin entradas: «Todavía no hay registros en este ayuno») · cargando (paginado por día) · error · sin permisos (bloqueo activo).

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
- **Hick:** lista: 1 acción. Detalle: Exportar + tocar entrada. Edición: Guardar / Borrar. ✔
- **Jakob:** lista → detalle → edición; línea de tiempo cronológica por días. ✔
- **Fitts:** filas completas tocables; Guardar en zona del pulgar. ✔
- **Miller:** la línea de tiempo se agrupa por día y se puede filtrar por tipo (9 tipos de entrada). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Alertas:** ¿se permite borrar o editar una alerta registrada? Propuesta: no (S8: registro de seguridad).
- **Editar un ayuno terminado:** ¿se permite corregir entradas de ayunos ya terminados? Propuesta: sí, siempre con rastro.
