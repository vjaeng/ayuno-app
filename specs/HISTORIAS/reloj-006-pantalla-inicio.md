## H-006: Ver de un vistazo dónde estoy

**Como** persona que ayuna,
**quiero** una pantalla de inicio que, durante el ayuno, muestre el tiempo transcurrido, el restante al objetivo, la hora prevista de fin, el próximo pendiente, los hitos, los totales del día frente a las metas y la última medición; y que, sin ayuno en curso, muestre solo el próximo ayuno planificado, con un único botón para empezarlo, y el último terminado,
**para** saber en todo momento dónde estoy sin hacer cuentas.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Durante el ayuno muestra transcurrido, restante, fin previsto con fecha y próximo pendiente con su hora.
- [ ] Muestra los hitos de la plantilla, cada uno con su fecha y hora reales.
- [ ] Tras forzar el cierre de la app y reiniciar el teléfono, el tiempo transcurrido es el correcto (se calcula desde la hora de inicio guardada).
- [ ] La pantalla aparece en menos de 1 segundo al abrir la app.
- [ ] Durante el ayuno, los botones «Anotar» y de seguridad están siempre visibles.
- [ ] Durante el ayuno, por cada meta de ingesta con cifra, «llevas X de Y» y, si el tipo tiene factor de sodio, su sodio frente al de la meta («Sal: llevas 0.6 de 2.5 g · sodio 0.24 de 1 g»), sin colores de juicio (EV-006).
- [ ] Junto a cada meta, cuánto tocaría llevar a esta hora si la meta se reparte por igual entre el fin y el inicio del silencio nocturno («a esta hora: 1.2 g»); antes de levantarse es 0 y tras dormirse, la meta entera; sin silencio configurado no se muestra. Es una cuenta, no un consejo (EV-006).
- [ ] Durante el ayuno, los botones rápidos de H-051 están en Inicio, sin quitar protagonismo a «Anotar» (EV-006).
- [ ] Durante el ayuno, bajo la última medición, «Mediciones de este ayuno» con su número lleva a la lista de H-048 (EV-004).
- [ ] Sin ayuno en curso se muestran el próximo planificado y el último terminado (si existen). Del próximo se ven la fecha y hora de la última comida, el inicio previsto y el objetivo, con una única acción principal «Empieza el ayuno ahora» y un acceso secundario «Ver y preparar este ayuno» a su detalle (H-046) (EV-002).
- [ ] Preparar, cambiar, borrar y planificar ayunos no están en Inicio, sino en la pestaña Planificados (H-046) (EV-002).
- [ ] Sin ningún ayuno, la pantalla explica cómo empezar y ofrece «Planificar ayuno».

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-11, EV-002; re-tejida el 2026-10-14, EV-004; re-tejida el 2026-10-26, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| tiempo transcurrido desde la hora de inicio guardada | `fast.clock_start_actual` |  | ✓ |  |  |
| tiempo restante y fin previsto | `fast.target_hours` |  | ✓ |  |  |
| hitos con fecha y hora | `fast.milestone_hours` |  | ✓ |  |  |
| ayuno en curso / planificado / terminado | `fast.status` |  | ✓ |  |  |
| próximo ayuno planificado: el de última comida prevista más cercana | `fast.last_meal_planned` |  | ✓ |  |  |
| próximo ayuno planificado: inicio previsto | `fast.clock_start_planned` |  | ✓ |  |  |
| «Empieza el ayuno ahora» del próximo (acción de H-005) | `fast.clock_start_actual`, `fast.status` |  |  | ✓ |  |
| último ayuno terminado | `fast.end_actual` |  | ✓ |  |  |
| motivo del último ayuno | `fast.end_reason` |  | ✓ |  |  |
| próximo pendiente | `metric_schedule.time_of_day` |  | ✓ |  |  |
| totales del día | `intake.amount` |  | ✓ |  |  |
| metas del día | `intake_goal.daily_amount` |  | ✓ |  |  |
| última medición: tipo | `measurement.metric_type_code` |  | ✓ |  |  |
| última medición: valor | `measurement.value_entered` |  | ✓ |  |  |
| última medición: unidad | `measurement.unit_entered` |  | ✓ |  |  |
| última medición: hora | `measurement.taken_at` |  | ✓ |  |  |
| número de mediciones del ayuno (acceso a H-048) | `measurement.fast_id`, `blood_pressure_reading.fast_id` (control) |  | ✓ |  |  |
| «llevas X de Y» por meta con cifra (EV-006) | `intake_goal.daily_amount`, `intake_goal.unit`, `intake.amount` |  | ✓ |  |  |
| sodio frente al de la meta (EV-006) | `intake_type.sodium_factor` |  | ✓ |  |  |
| «a esta hora» repartido entre fin e inicio del silencio (EV-006) | `settings.quiet_start`, `settings.quiet_end` → derivado «Avance esperado a esta hora» |  | ✓ |  |  |
| botones rápidos (EV-006) | `intake_type.quick_unit_code`, `household_unit.name` (vía H-051) |  | ✓ |  |  |
