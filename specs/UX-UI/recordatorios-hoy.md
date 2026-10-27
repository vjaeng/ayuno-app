# Flujo: Recordatorios y Hoy

**Historias que cubre:** H-015, H-016, H-017, H-018, H-048 (tocar un hecho), H-052 (pregunta tras horas sin anotar)
**Entrada:** notificación del sistema, o pestaña Hoy · **Salida:** formulario de registro prerrellenado; o vuelta a Hoy

## Pantallas

### Pantalla: Notificación (sistema operativo)

```
┌──────────────────────────────────┐
│ Ayuno · 12:00                    │
│ Toca anotar la glucosa           │
│ [Anotar glucosa] [+10 min]       │
│ [+30 min] [No se hizo]           │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Título y hora | `metric_schedule.time_of_day` / `product_schedule.time_of_day` / `refeed_check.scheduled_at` / recordatorio de regla (`alert.fired_at` + `rule.action_minutes`) | lectura |
| Qué toca | `metric_type.name` / `product.name` / `refeed_step.description` | lectura |
| Acción directa «Anotar …» | — | acción → formulario con `*.scheduled_for` / `product_log.scheduled_at` ya puesto |
| +10 min / +30 min | — | acción (reprograma la notificación; no escribe datos) |
| No se hizo | `measurement.not_done_reason` / `blood_pressure_reading.not_done_reason` / `product_log.status = not_taken` + `product_log.reason` | escritura (abre la app para pedir el motivo) |

### Pantalla: Notificación «¿Has tomado …?» (H-052, EV-006)

```
┌──────────────────────────────────┐
│ Ayuno · 13:00                    │
│ ¿Has tomado sal desde las 07:00? │
│ [Sí, anotar]   [Todavía no]      │
└──────────────────────────────────┘
```

| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Momento de la notificación | derivado «Próxima pregunta por tipo» (`intake_goal.nudge_after_hours`, última `intake.taken_at` del tipo o `fast.clock_start_actual`, `settings.quiet_start`/`quiet_end`) | derivado |
| Tipo («sal») | `intake_goal.intake_type_code` → `intake_type.name` | lectura (nunca cantidades, S4) |
| «desde las 07:00» | `intake.taken_at` de la última ingesta del tipo, o `fast.clock_start_actual` | lectura |
| «Sí, anotar» | — | acción → formulario de líquido con ese tipo ([registro-anotar.md](registro-anotar.md)) |
| «Todavía no» | — | acción (cierra; no escribe datos ni vuelve a preguntar por ese hueco) |

No es una regla ni una alerta: no escribe `alert` ni usa rojo. Suena una vez por hueco; una ingesta nueva de ese tipo la retira si estaba visible y reprograma la siguiente. Como cae fuera del silencio nocturno por construcción, el silencio no la retrasa.

**Acciones:** como arriba. Las notificaciones se programan localmente, se reprograman solas tras reinicio y tras cambio de hora o de zona; suenan en modo avión. Durante el silencio nocturno (`settings.quiet_start`–`settings.quiet_end`) no suenan, **salvo** las programadas por una regla (`rule.action = schedule_measurement`).
**Estados:** vacío (n/a) · cargando (n/a) · error (si el sistema rechazó programar: franja en Inicio y Hoy «Hay recordatorios que el teléfono no aceptó») · sin permisos (notificaciones denegadas: franja persistente con botón a ajustes del teléfono; con bloqueo activo, la acción directa pide huella o PIN antes de abrir el formulario).

### Pantalla: Hoy

```
┌──────────────────────────────────┐
│ Hoy · viernes 23 de octubre      │
│ hora 9 a hora 33                 │
│──────────────────────────────────│
│ ✔ 07:00 Glucosa 72 mg/dL (07:02) │  hecho
│ ✔ 07:10 Presión 118/76 sentado   │  hecho
│ ✖ 08:30 Vitamina C — no tomado:  │  no hecho
│        «náusea»                  │
│ ! 11:00 Peso                     │  atrasado → toca para anotar
│ ○ 12:00 Glucosa                  │  pendiente
│ ○ 18:00 Glucosa                  │
│ ○ 20:00 Magnesio 400 mg          │
│ ◆ 12:20 Medir de nuevo (regla:   │  pendiente de regla
│        Glucosa baja)             │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha del día y rango de horas del ayuno | hora del ayuno | derivado |
| Lista de pendientes y su estado (hecho, pendiente, atrasado, no hecho) | pendientes del día y su estado | derivado |
| Hora programada | `metric_schedule.time_of_day`, `product_schedule.time_of_day`, `refeed_check.scheduled_at`, recordatorio de regla | lectura / derivado |
| Valor anotado (hechos) | `measurement.value_entered` + `measurement.unit_entered` (con palabras: derivado «Palabra de una medición», EV-005); `blood_pressure_reading.systolic`/`diastolic`/`position`; `product_log.amount` | lectura |
| Hora real (hechos) | `measurement.taken_at`, `blood_pressure_reading.taken_at`, `product_log.taken_at`, `refeed_check.done_at` | lectura |
| Motivo (no hechos) | `measurement.not_done_reason`, `blood_pressure_reading.not_done_reason`, `product_log.reason` | lectura |
| Productos que salen (sección gris al pie) | `plan_product.during_fast = stops`, `product.name` | lectura |
| Fila «? 13:00 ¿Has tomado sal desde las 07:00?» (desde que suena hasta que se anota una ingesta del tipo) | derivado «Próxima pregunta por tipo», `intake_type.name` | derivado → tocar abre el formulario de líquido |

**Acciones:** tocar un pendiente o atrasado → formulario prerrellenado con la hora programada · deslizar → «No se hizo» con motivo · tocar una medición o presión hecha → Detalle de entrada con sus valores, para verla o corregirla ([historial-revisar.md](historial-revisar.md), H-048; las tomas de producto y los pasos de vuelta a comer, cuando se complete H-034).
**Estados:** vacío (sin ayuno en curso: «No hay nada programado hoy» + enlace a Inicio; con ayuno pero sin pendientes: «Hoy no hay mediciones programadas») · cargando · error (no se pudo leer: reintentar) · sin permisos (bloqueo activo; notificaciones denegadas: franja).

## Validación Laws of UX
- **Hick:** notificación con 4 acciones (límite del sistema), 1 principal (Anotar). Hoy: 1 acción principal (tocar pendiente). ✔
- **Jakob:** notificación con acciones nativas; lista cronológica tipo agenda. ✔
- **Fitts:** cada fila ≥ 56 pt de alto, toda la fila es tocable. ✔
- **Miller:** la lista puede superar 9 filas → se agrupa por franjas (mañana, tarde, noche). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Hoy muestra solo el día natural o la franja del ayuno de ese día (de medianoche a medianoche)? Propuesta: día natural, con la hora del ayuno junto a cada hora.
- ¿Cuándo pasa un pendiente a «atrasado»? Propuesta: 15 minutos después de su hora (configurable en un futuro).

*Cambio EV-004 (2026-10-14):* tocar una medición o presión hecha abre su Detalle de entrada (antes no hacía nada). Motivo: corregir desde donde se ve el hecho (H-018, H-048).

*Cambio EV-005 (2026-10-23):* el valor de un hecho de una escala con palabras se muestra con su palabra y número («Mucha · 7») (H-049).

*Cambio EV-006 (2026-10-26):* notificación nueva «¿Has tomado sal desde las 07:00?» con «Sí, anotar» / «Todavía no», cuando una meta con «preguntar tras N h» lleva N horas despierto sin ingestas de su tipo (H-052); también aparece como fila en Hoy hasta que se anota. Usa las mismas alarmas exactas y se reprograma tras reiniciar (H-016, ADR-005).
