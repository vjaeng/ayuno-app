# Flujo: Inicio y reloj

**Historias que cubre:** H-005, H-006, H-041 (H-005 incluye registrar la última comida real, ahora en el detalle del planificado), H-046 (acceso a «Ver ›»), H-047 (peso al empezar), H-048 (acceso a «Mediciones de este ayuno»), H-051 (botones rápidos), H-057 (revisión antes de empezar)
**Entrada:** apertura de la app, pestaña Inicio · **Salida:** Detalle del ayuno planificado, Planificados, Anotar, Seguridad, Terminar el ayuno, Mediciones de este ayuno, Revisión antes de empezar

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

**Acciones:** «Empieza el ayuno ahora» (si la revisión previa tiene pendientes, abre antes la pantalla Revisión antes de empezar, H-057; sin pendientes, empieza directamente: fija `clock_start_actual` con segundos, `status = in_progress`, recalcula derivados y reprograma recordatorios; rechazado si ya hay un ayuno `in_progress`; después, Inicio en curso abre la hoja «Peso al empezar», H-047). «Ver y preparar este ayuno» → detalle del próximo, donde están la lista previa, «Registrar última comida», «Cambiar» y «Borrar» (EV-002). Tocar el último ayuno → detalle en [historial-revisar.md](historial-revisar.md). Planificar más ayunos → pestaña Planificados.
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
│ Hoy                              │
│  Agua: llevas 1.2 de 2 L         │
│   a esta hora: 1.1 L             │  ← solo con silencio nocturno
│  Sal: llevas 0.6 de 2.5 g        │
│   a esta hora: 1.2 g             │
│   sodio: 0.24 de 0.98 g          │
│  Potasio: 0.4 g                  │  ← sin meta: solo el total
│ Última: Glucosa 64 mg/dL · 11:58 │
│ ┌──────────────────────────────┐ │
│ │ Mediciones de este ayuno   › │ │  ← FilaLista (EV-004)
│ │ 14 anotadas                  │ │
│ └──────────────────────────────┘ │
│──────────────────────────────────│
│ [+1 vaso de agua][+1 pizca de sal]│ ← BotonRapido (EV-006)
│ Mantén pulsado para cambiar      │
│ la cantidad                      │
│            [ Anotar ]            │  ← botón grande inferior
└──────────────────────────────────┘
   ↓ tras un toque en «+1 pizca de sal»
│ Sal: +1 pizca (0.3 g)  [Deshacer]│  ← Snackbar
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
| Totales frente a metas («llevas X de Y»; sin cifra en la meta o sin meta, solo el total) | totales del día (suma de `intake.amount`), `intake_goal.daily_amount`, `intake_goal.unit` | derivado / lectura |
| «a esta hora: Z» bajo cada meta con cifra (solo con silencio nocturno configurado) | derivado «Avance esperado a esta hora» (`intake_goal.daily_amount`, `settings.quiet_start`, `settings.quiet_end`) | derivado |
| «sodio: X de Y» bajo la meta de un tipo con factor | sodio (`intake.amount` × `intake_type.sodium_factor`), derivado «Sodio de la meta» | derivado |
| BotonRapido por tipo con medida de botón («+1 vaso de agua»; solo tipos sin `breaks_fast`) | `intake_type.quick_unit_code` → `household_unit.name`, `intake_type.name` | lectura |
| Toque en un BotonRapido | `intake.intake_type_code`, `intake.taken_at` (ahora), `intake.amount_entered` = 1, `intake.unit_entered` = `household_unit.code`, `intake.amount` = `household_unit.equivalent`, `intake.unit` = `intake_type.unit` | escritura (H-051) |
| Pulsación larga en un BotonRapido | — | acción → formulario de líquido con tipo y medida elegidos ([registro-anotar.md](registro-anotar.md)) |
| Snackbar «Sal: +1 pizca (0.3 g)» con «Deshacer» | `intake.amount_entered`, `household_unit.name`, `intake.amount`, `intake.unit` | lectura (solo tras confirmar la escritura) |
| «Deshacer» | control `deleted_at` de `intake` + `edit_log.action = delete` | escritura (borrado blando, D-22) |
| Última medición | `measurement.metric_type_code`, `measurement.value_entered`, `measurement.unit_entered`, `measurement.taken_at`; con palabras, derivado «Palabra de una medición» («Hambre: Mucha · 7», EV-005) | lectura |
| Fila «Mediciones de este ayuno» con su número (FilaLista) | derivado «Mediciones del ayuno» (número) | acción → [historial-revisar.md](historial-revisar.md#pantalla-mediciones-de-este-ayuno) (H-048) |
| Avisos fijos de la etapa | `stage_notice.text` (si la etapa actual está en `stage_notice.stage_kinds`) | lectura |
| Botón «Seguridad» | — | acción → [seguridad-alertas.md](seguridad-alertas.md) |
| Botón «Anotar» | — | acción → [registro-anotar.md](registro-anotar.md) |

**Acciones:** Anotar · Seguridad · BotonRapido: un toque guarda 1 de su medida con la hora actual y evalúa las reglas como cualquier ingesta (si alguna se cumple → [seguridad-alertas.md](seguridad-alertas.md)); el total del día y el Snackbar aparecen solo después de guardar; mantener pulsado → formulario · «Deshacer» del Snackbar → borra en blando esa ingesta con bitácora (las alertas que hubiera disparado se quedan, S8) · «Mediciones de este ayuno» → lista para ver y corregir (H-048) · tocar el reloj → detalle de horas planificadas y reales con «Corregir hora de inicio» · tocar «Próximo» → abre el formulario prerrellenado.
**Estados:** vacío (no aplica: hay ayuno; sin metas ni ingestas, el bloque «Hoy» no aparece; ningún tipo con medida de botón → sin fila de BotonRapido) · cargando (reloj calculado en <1 s desde la hora guardada, aun tras reinicio; BotonRapido deshabilitado con indicador mientras guarda, para no duplicar con dos toques) · error (fallo al leer: mensaje y reintento; el botón de seguridad sigue visible) · sin permisos (como arriba; además, si las notificaciones están denegadas, franja roja **no**: franja informativa persistente, porque no es una alerta de regla).

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

### Pantalla: Revisión antes de empezar (H-057, EV-006)

Se abre al tocar «Empieza el ayuno ahora» (en Inicio o en el detalle del planificado) **solo si** el derivado «Revisión previa» tiene pendientes. Sin pendientes no aparece y el toque empieza el ayuno, como siempre (H-005).

```
┌──────────────────────────────────┐
│ ← Antes de empezar               │
│ A tu plan le falta esto. Puedes  │
│ arreglarlo ahora o empezar igual.│
│ Reglas de ejemplo sin tocar (2)  │
│  Glucosa baja (pon tu umbral)  › │  ← FilaLista · insignia «de ejemplo»
│  Algo no va bien · de ejemplo  › │
│ Metas sin cifra (2)              │
│  Agua                          › │
│  Sal                           › │
│ Lista previa sin marcar (1)      │
│  ☐ Comprar sal y agua            │  ← se marca aquí mismo
│──────────────────────────────────│
│ [         Volver           ]     │  ← BotonSecundario
│ [ Empezar de todos modos   ]     │  ← BotonPrincipal
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Lista agrupada de pendientes | derivado «Revisión previa» (MODELO-DATOS §2.5) sobre el plan del ayuno | derivado |
| Regla de ejemplo (activa o no) | `rule.name`, `rule.active`, `rule.is_placeholder` | lectura → acción «Arreglar» → editor de regla del plan del ayuno ([reglas-editar.md](reglas-editar.md)) |
| Meta sin cifra | `intake_goal.intake_type_code` → `intake_type.name`, `intake_goal.daily_amount` (nulo) | lectura → acción «Arreglar» → Líquidos y metas del plan del ayuno ([plantillas-editar.md](plantillas-editar.md)) |
| Elemento de la lista previa sin marcar | `checklist_item.text`, `checklist_item.is_checked` | lectura / escritura (marcar aquí) |
| «Empezar de todos modos» | `fast.clock_start_actual`, `fast.status`, `fast.tz_offset`, `fast.started_with_warnings` (`kind`, `ref_id`, `text` de cada pendiente en ese momento) | escritura (H-005 + H-057) |
| «Volver» | — | acción (no empieza) |

**Acciones:** «Arreglar ›» abre el editor del **plan de este ayuno** (no de la plantilla), con la FranjaInformativa «Estás cambiando el plan de este ayuno, no la plantilla»; al volver, la lista se recalcula y, si queda vacía, el botón principal pasa a «Empieza el ayuno ahora» y ya no guarda pendientes. Marcar un elemento lo quita de la lista. «Empezar de todos modos» → vuelve a calcular la revisión dentro de la transacción de empezar, guarda lo que falte en `fast.started_with_warnings` y sigue como «Empieza el ayuno ahora» (Inicio en curso con la hoja «Peso al empezar», H-047). «Volver» → pantalla de origen sin cambios.
**Estados:** vacío (no se muestra: sin pendientes se empieza directamente) · cargando (esqueleto; los dos botones deshabilitados hasta tener la lista; al empezar, botón deshabilitado con indicador) · error (no se pudo leer el plan: EstadoError con «Reintentar» y «Volver»; **no** se ofrece empezar a ciegas; fallo al empezar: nada cambia, mensaje y reintento; ya hay otro ayuno en curso: «Termina el ayuno en curso para poder empezar este.») · sin permisos (bloqueo activo → huella o PIN).

## Validación Laws of UX
- **Hick:** hoja de peso: 1 principal (Guardar) + 1 secundaria (Ahora no). Sin ayuno, 1 acción principal (Empieza el ayuno ahora; o Planificar ayuno si no hay planificado) y 1 secundaria (Ver y preparar este ayuno). Preparar, cambiar y planificar viven en la pestaña Planificados (EV-002). En curso, 2 principales (Anotar, Seguridad). ✔
- **Jakob:** pantalla de estado con contador grande y botón de acción inferior, patrón de apps de ayuno y temporizadores. ✔
- **Fitts:** «Anotar» ocupa el ancho inferior al alcance del pulgar; Seguridad arriba pero siempre visible y ≥48 pt. ✔
- **Miller:** 4 bloques en curso (reloj, próximo, hoy, última con su acceso «Mediciones de este ayuno»). ✔
- **Hick / Fitts (EV-006):** los BotonRapido son secundarios y como mucho dos o tres por fila (uno por líquido con medida de botón); «Anotar» sigue siendo la acción principal, ancha y al pie. Revisión antes de empezar: 1 principal (Empezar de todos modos) + 1 secundaria (Volver); los «Arreglar ›» son filas. Pendientes en 3 grupos (Miller). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿«Empieza el ayuno ahora» en Inicio sin ayuno también si el planificado es para otro día? Propuesta: sí, con confirmación si faltan más de 2 horas para el inicio previsto.

*Cambio EV-002 (2026-10-11):* la lista previa, «Registrar última comida», «Cambiar este ayuno» y «Nuevo ayuno» salen de Inicio hacia el detalle del planificado y la pestaña Planificados ([ayuno-planificados.md](ayuno-planificados.md)). Motivo: el humano encontró Inicio confuso con todas las opciones juntas. *Implementación:* «Ver ›» se construyó como BotonSecundario «Ver y preparar este ayuno», porque la guía no tiene un componente de enlace de texto y el botón mide 48 dp.

*Cambio EV-003 (2026-10-13):* tras «Empieza el ayuno ahora» se ofrece una vez «¿Cuánto pesas al empezar?». Motivo: el humano quiere comparar el peso de antes y después del ayuno; se pregunta *después* de empezar para que empezar siga siendo un toque (H-005). *Implementación:* diálogo de Material 3, porque la guía no tiene hoja inferior.

*Cambio EV-004 (2026-10-14):* bajo la última medición, la FilaLista «Mediciones de este ayuno · N anotadas ›» abre la lista del ayuno en curso para verlas y corregirlas ([historial-revisar.md](historial-revisar.md)). Motivo: el humano necesita repasar y corregir las mediciones sin esperar a que termine el ayuno (H-048). Es acceso secundario: no compite con «Anotar» (Hick).

*Implementación (2026-10-14):* los hitos se construyeron como lista vertical con fecha y hora y ocupaban demasiado; se rehacen como **BarraHitos** horizontal, como en el wireframe (puntos rellenos alcanzados, huecos pendientes, horas debajo). Debajo de la barra, una línea con la fecha y hora del hito elegido: por defecto el siguiente; tocar un punto muestra el suyo. El lector de pantalla anuncia cada hito con su fecha y hora. Petición del humano tras verlo en el teléfono.

*Cambio EV-005 (2026-10-23):* la última medición de una escala con palabras se muestra con su palabra y número («Mucha · 7»); un número sin palabra, como hasta ahora («4 de 0 a 10») (H-049).

*Cambio EV-006 (2026-10-26):* en curso, el bloque «Hoy» muestra por cada meta «llevas X de Y», el avance esperado «a esta hora» (reparto lineal entre el fin y el inicio del silencio nocturno; sin silencio no aparece) y el sodio frente al sodio de la meta; una meta sin cifra muestra solo el total. Sobre «Anotar», un BotonRapido por líquido con medida de botón (de fábrica «+1 vaso de agua» y «+1 pizca de sal»): un toque guarda sin formulario y muestra un Snackbar con «Deshacer»; mantener pulsado abre el formulario (H-006, H-051). «Empieza el ayuno ahora» pasa por la pantalla nueva **Revisión antes de empezar** solo si hay pendientes (H-057). Motivo: el primer ayuno real dejó sin anotar agua y sal y empezó con reglas de relleno («si un dato lo pide el plan y no se anota, la app lo tiene que notar antes que el usuario»).
