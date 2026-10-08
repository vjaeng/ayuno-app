# Flujo: Inicio y reloj

**Historias que cubre:** H-005, H-006, H-041
**Entrada:** apertura de la app, pestaña Inicio · **Salida:** Crear ayuno, Anotar, Seguridad, Terminar el ayuno

## Pantallas

### Pantalla: Inicio — sin ayuno en curso

```
┌──────────────────────────────────┐
│ Próximo ayuno                    │
│  Última comida: jueves 22 oct,   │
│  18:00 · Reloj: 22:00            │
│  Objetivo: 57 horas              │
│  [ Empieza el ayuno ahora ]      │  ← solo si hay uno planificado
│──────────────────────────────────│
│ Último ayuno                     │
│  domingo 25 oct · 57 h 10 min    │
│  Objetivo cumplido               │
│──────────────────────────────────│
│        [ + Nuevo ayuno ]         │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Última comida prevista | `fast.last_meal_planned` | lectura |
| Inicio previsto del reloj | `fast.clock_start_planned` | lectura |
| Objetivo | `fast.target_hours` | lectura |
| Botón «Empieza el ayuno ahora» | `fast.clock_start_actual`, `fast.status` | acción → escritura |
| Fecha del último ayuno | `fast.end_actual` | lectura |
| Duración del último ayuno | duración final | derivado |
| Motivo de fin del último | `fast.end_reason`, `fast.end_reason_detail` | lectura |
| Botón «Nuevo ayuno» | — | acción → Crear ayuno |

**Acciones:** «Empieza el ayuno ahora» (fija `clock_start_actual` con segundos, `status = in_progress`, recalcula derivados y reprograma recordatorios; rechazado si ya hay un ayuno `in_progress`). «Nuevo ayuno» → [ayuno-crear.md](ayuno-crear.md). Tocar el último ayuno → detalle en [historial-revisar.md](historial-revisar.md).
**Estados:** vacío (sin ningún ayuno: texto «Crea tu primer ayuno: elige una plantilla y la hora de tu última comida» + «Nuevo ayuno») · cargando (esqueleto; el reloj debe estar en <1 s) · error (no se pudo leer el almacenamiento: mensaje llano y «Reintentar»; nunca se muestra un reloj inventado) · sin permisos (bloqueo activo → huella o PIN; notificaciones denegadas → franja persistente «Los recordatorios están desactivados en el teléfono» con botón a ajustes).

### Pantalla: Inicio — ayuno en curso

```
┌──────────────────────────────────┐
│ [Seguridad]              hora 33 │
│                                  │
│          33 h 12 min             │  ← reloj grande
│   faltan 23 h 48 min             │
│   fin previsto: domingo 25 oct,  │
│   07:00                          │
│ ─○──●──●──○──○── hitos           │
│  12  24  36  48  57              │
│──────────────────────────────────│
│ Próximo: Glucosa · 12:00         │
│──────────────────────────────────│
│ Hoy: Agua 1.2 de 2 L · Sodio     │
│ 0.8 de 1.5 g                     │
│ Última: Glucosa 64 mg/dL · 11:58 │
│──────────────────────────────────│
│            [ Anotar ]            │  ← botón grande inferior
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Reloj (transcurrido) | tiempo transcurrido desde `fast.clock_start_actual` | derivado |
| Tiempo restante | fin previsto − ahora | derivado |
| Fin previsto | fin previsto (`clock_start_actual` + `target_hours`) | derivado |
| Hitos | `fast.milestone_hours` + `fast.clock_start_actual` | derivado |
| Hora del ayuno («hora 33») | hora del ayuno | derivado |
| Inicio real y previsto (detalle al tocar el reloj) | `fast.clock_start_actual`, `fast.clock_start_planned` | lectura |
| Botón «Corregir hora de inicio» (detalle) | `fast.clock_start_actual`, `edit_log.reason` | escritura (motivo obligatorio) |
| Próximo pendiente | pendientes del día | derivado |
| Totales frente a metas | totales del día, `intake_goal.daily_amount`, `intake_goal.unit` | derivado / lectura |
| Última medición | `measurement.metric_type_code`, `measurement.value_entered`, `measurement.unit_entered`, `measurement.taken_at` | lectura |
| Avisos fijos de la etapa | `stage_notice.text` (si la etapa actual está en `stage_notice.stage_kinds`) | lectura |
| Botón «Seguridad» | — | acción → [seguridad-alertas.md](seguridad-alertas.md) |
| Botón «Anotar» | — | acción → [registro-anotar.md](registro-anotar.md) |

**Acciones:** Anotar · Seguridad · tocar el reloj → detalle de horas planificadas y reales con «Corregir hora de inicio» · tocar «Próximo» → abre el formulario prerrellenado.
**Estados:** vacío (no aplica: hay ayuno) · cargando (reloj calculado en <1 s desde la hora guardada, aun tras reinicio) · error (fallo al leer: mensaje y reintento; el botón de seguridad sigue visible) · sin permisos (como arriba; además, si las notificaciones están denegadas, franja roja **no**: franja informativa persistente, porque no es una alerta de regla).

## Validación Laws of UX
- **Hick:** sin ayuno, 1 acción principal (Nuevo ayuno; o Empieza el ayuno si hay planificado). En curso, 2 principales (Anotar, Seguridad). ✔
- **Jakob:** pantalla de estado con contador grande y botón de acción inferior, patrón de apps de ayuno y temporizadores. ✔
- **Fitts:** «Anotar» ocupa el ancho inferior al alcance del pulgar; Seguridad arriba pero siempre visible y ≥48 pt. ✔
- **Miller:** 4 bloques en curso (reloj, próximo, hoy, última). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿«Empieza el ayuno ahora» en Inicio sin ayuno también si el planificado es para otro día? Propuesta: sí, con confirmación si faltan más de 2 horas para el inicio previsto.
