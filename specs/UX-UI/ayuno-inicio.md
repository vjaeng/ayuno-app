# Flujo: Inicio y reloj

**Historias que cubre:** H-005, H-006, H-041 (H-005 incluye registrar la última comida real, ahora en el detalle del planificado), H-046 (acceso a «Ver ›»), H-047 (peso al empezar)
**Entrada:** apertura de la app, pestaña Inicio · **Salida:** Detalle del ayuno planificado, Planificados, Anotar, Seguridad, Terminar el ayuno

## Pantallas

### Pantalla: Inicio — sin ayuno en curso

```
┌──────────────────────────────────┐
│ Próximo ayuno                    │
│  Última comida: jueves 22 oct,   │
│  18:00                           │
│  Reloj: 22:00 · Objetivo: 57 h   │
│ [ Ver y preparar este ayuno ]    │  ← BotonSecundario al detalle
│   [ Empieza el ayuno ahora ]     │  ← única acción principal
│──────────────────────────────────│
│ Último ayuno                     │
│  domingo 25 oct · 57 h 10 min    │
│  Objetivo cumplido               │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Próximo ayuno (el planificado con la `fast.last_meal_planned` más cercana) | `fast.status`, `fast.last_meal_planned` | lectura |
| Última comida prevista | `fast.last_meal_planned` | lectura |
| Inicio previsto del reloj | `fast.clock_start_planned` | lectura |
| Objetivo | `fast.target_hours` | lectura |
| Botón «Ver y preparar este ayuno» (BotonSecundario) | — | acción → Detalle del ayuno planificado ([ayuno-planificados.md](ayuno-planificados.md)) |
| Botón «Empieza el ayuno ahora» | `fast.clock_start_actual`, `fast.status` | acción → escritura |
| Fecha del último ayuno | `fast.end_actual` | lectura |
| Duración del último ayuno | duración final | derivado |
| Motivo de fin del último | `fast.end_reason`, `fast.end_reason_detail` | lectura |

**Acciones:** «Empieza el ayuno ahora» (fija `clock_start_actual` con segundos, `status = in_progress`, recalcula derivados y reprograma recordatorios; rechazado si ya hay un ayuno `in_progress`; después, Inicio en curso abre la hoja «Peso al empezar», H-047). «Ver y preparar este ayuno» → detalle del próximo, donde están la lista previa, «Registrar última comida», «Cambiar» y «Borrar» (EV-002). Tocar el último ayuno → detalle en [historial-revisar.md](historial-revisar.md). Planificar más ayunos → pestaña Planificados.
**Estados:** vacío (sin planificados ni terminados: texto «Crea tu primer ayuno: elige una plantilla y la hora de tu última comida» + «Planificar ayuno»; sin planificados pero con un terminado: el último ayuno y el BotonSecundario «Planificar el siguiente ayuno», que abre la pestaña Planificados) · cargando (esqueleto; el reloj debe estar en <1 s) · error (no se pudo leer el almacenamiento: mensaje llano y «Reintentar»; nunca se muestra un reloj inventado) · sin permisos (bloqueo activo → huella o PIN; notificaciones denegadas → franja persistente «Los recordatorios están desactivados en el teléfono» con botón a ajustes).

La sección de vuelta a comer (H-031) y las reintroducciones pendientes, cuando existen, siguen arriba de «Próximo ayuno», sin cambios.

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

### Hoja: Peso al empezar (H-047, EV-003)

Diálogo de Material 3 (sin hoja inferior en la guía). Se abre una sola vez sobre Inicio en curso, justo después de «Empieza el ayuno ahora» (desde Inicio o desde el detalle del planificado). No se abre si `metric_type.active` es falso para el peso.

```
┌──────────────────────────────────┐
│ ¿Cuánto pesas al empezar?        │
│  [ 71.4        ] [kg ▾]          │
│  El ayuno ya ha empezado.        │
│  [ Ahora no ]      [ Guardar ]   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Valor | `measurement.value_entered` (y `value_num` convertido) | escritura |
| Unidad (kg / lb) | `measurement.unit_entered`; por defecto `metric_type.entry_unit`, alternativa `metric_type.alt_unit` | escritura |
| Tipo y hora | `measurement.metric_type_code = weight`, `measurement.taken_at` = ahora | escritura (implícito) |

**Acciones:** «Guardar» → guarda la medición por la vía de siempre (evalúa reglas; si alguna se cumple → [seguridad-alertas.md](seguridad-alertas.md)) y cierra la hoja. «Ahora no» o cerrar → no guarda nada; no se vuelve a preguntar (tampoco si la app se cerró sin responder; el peso se anota igual desde Anotar).
**Estados:** vacío (campo vacío: «Guardar» deshabilitado) · cargando (Guardar deshabilitado hasta confirmar escritura) · error (fuera de rango: DialogoConfirmacion «¿Seguro?» de H-008; fallo al guardar: conserva lo tecleado y ofrece reintentar) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** hoja de peso: 1 principal (Guardar) + 1 secundaria (Ahora no). Sin ayuno, 1 acción principal (Empieza el ayuno ahora; o Planificar ayuno si no hay planificado) y 1 secundaria (Ver y preparar este ayuno). Preparar, cambiar y planificar viven en la pestaña Planificados (EV-002). En curso, 2 principales (Anotar, Seguridad). ✔
- **Jakob:** pantalla de estado con contador grande y botón de acción inferior, patrón de apps de ayuno y temporizadores. ✔
- **Fitts:** «Anotar» ocupa el ancho inferior al alcance del pulgar; Seguridad arriba pero siempre visible y ≥48 pt. ✔
- **Miller:** 4 bloques en curso (reloj, próximo, hoy, última). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿«Empieza el ayuno ahora» en Inicio sin ayuno también si el planificado es para otro día? Propuesta: sí, con confirmación si faltan más de 2 horas para el inicio previsto.

*Cambio EV-002 (2026-10-11):* la lista previa, «Registrar última comida», «Cambiar este ayuno» y «Nuevo ayuno» salen de Inicio hacia el detalle del planificado y la pestaña Planificados ([ayuno-planificados.md](ayuno-planificados.md)). Motivo: el humano encontró Inicio confuso con todas las opciones juntas. *Implementación:* «Ver ›» se construyó como BotonSecundario «Ver y preparar este ayuno», porque la guía no tiene un componente de enlace de texto y el botón mide 48 dp.

*Cambio EV-003 (2026-10-13):* tras «Empieza el ayuno ahora» se ofrece una vez «¿Cuánto pesas al empezar?». Motivo: el humano quiere comparar el peso de antes y después del ayuno; se pregunta *después* de empezar para que empezar siga siendo un toque (H-005). *Implementación:* diálogo de Material 3, porque la guía no tiene hoja inferior.
