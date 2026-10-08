## H-006: Ver de un vistazo dónde estoy

**Como** persona que ayuna,
**quiero** una pantalla de inicio que, durante el ayuno, muestre el tiempo transcurrido, el restante al objetivo, la hora prevista de fin, el próximo pendiente, los hitos, los totales del día frente a las metas y la última medición; y que, sin ayuno en curso, muestre el próximo ayuno planificado, el último y un botón «Nuevo ayuno»,
**para** saber en todo momento dónde estoy sin hacer cuentas.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Durante el ayuno muestra transcurrido, restante, fin previsto con fecha y próximo pendiente con su hora.
- [ ] Muestra los hitos de la plantilla, cada uno con su fecha y hora reales.
- [ ] Tras forzar el cierre de la app y reiniciar el teléfono, el tiempo transcurrido es el correcto (se calcula desde la hora de inicio guardada).
- [ ] La pantalla aparece en menos de 1 segundo al abrir la app.
- [ ] Durante el ayuno, los botones «Anotar» y de seguridad están siempre visibles.
- [ ] Sin ayuno en curso se muestran el próximo planificado y el último terminado (si existen) y «Nuevo ayuno» lleva a crear uno.
- [ ] Sin ningún ayuno, la pantalla explica cómo empezar.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| tiempo transcurrido desde la hora de inicio guardada | `fast.clock_start_actual` |  | ✓ |  |  |
| tiempo restante y fin previsto | `fast.target_hours` |  | ✓ |  |  |
| hitos con fecha y hora | `fast.milestone_hours` |  | ✓ |  |  |
| ayuno en curso / planificado / terminado | `fast.status` |  | ✓ |  |  |
| próximo ayuno planificado | `fast.clock_start_planned` |  | ✓ |  |  |
| último ayuno terminado | `fast.end_actual` |  | ✓ |  |  |
| motivo del último ayuno | `fast.end_reason` |  | ✓ |  |  |
| próximo pendiente | `metric_schedule.time_of_day` |  | ✓ |  |  |
| totales del día | `intake.amount` |  | ✓ |  |  |
| metas del día | `intake_goal.daily_amount` |  | ✓ |  |  |
| última medición: tipo | `measurement.metric_type_code` |  | ✓ |  |  |
| última medición: valor | `measurement.value_entered` |  | ✓ |  |  |
| última medición: unidad | `measurement.unit_entered` |  | ✓ |  |  |
| última medición: hora | `measurement.taken_at` |  | ✓ |  |  |
