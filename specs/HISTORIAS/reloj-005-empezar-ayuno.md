## H-005: Empezar el ayuno con un toque

**Como** persona que ayuna,
**quiero** tocar un botón grande «Empieza el ayuno ahora» que fije la hora real de inicio del reloj,
**para** que la hora de inicio sea exacta y no dependa de mi memoria.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] Al tocarlo se guarda la hora real con segundos y el ayuno pasa a en curso.
- [ ] Si empiezo más tarde de lo previsto, el fin previsto, los hitos y los recordatorios se recalculan desde la hora real.
- [ ] Puedo corregir después la hora de inicio indicando un motivo; queda constancia del valor anterior.
- [ ] Solo puede haber un ayuno en curso a la vez.
- [ ] Puedo registrar la hora real de la última comida (por defecto, la planificada) y corregirla después.

### Consideraciones de Seguridad
Lo crítico deja rastro (S8): la corrección de la hora de inicio queda en la bitácora de cambios.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| hora real de inicio, con segundos | `fast.clock_start_actual` | ✓ |  | ✓ |  |
| desfase horario de la hora local | `fast.tz_offset` | ✓ |  |  |  |
| ayuno pasa a en curso; solo uno en curso | `fast.status` |  | ✓ | ✓ |  |
| inicio previsto (para recalcular) | `fast.clock_start_planned` |  | ✓ |  |  |
| objetivo (fin previsto recalculado) | `fast.target_hours` |  | ✓ |  |  |
| hitos recalculados | `fast.milestone_hours` |  | ✓ |  |  |
| hora real de la última comida (default la planificada) | `fast.last_meal_actual` | ✓ |  | ✓ |  |
| última comida planificada (valor por defecto) | `fast.last_meal_planned` |  | ✓ |  |  |
| motivo de la corrección | `edit_log.reason` | ✓ |  |  |  |
| constancia del valor anterior | `edit_log.old_value` | ✓ |  |  |  |
| valor nuevo | `edit_log.new_value` | ✓ |  |  |  |
| campo corregido | `edit_log.field` | ✓ |  |  |  |
| fila corregida | `edit_log.row_id` | ✓ |  |  |  |
| entidad corregida | `edit_log.entity_name` | ✓ |  |  |  |
| tipo de cambio | `edit_log.action` | ✓ |  |  |  |
| hora de la corrección | `edit_log.edited_at` | ✓ |  |  |  |
