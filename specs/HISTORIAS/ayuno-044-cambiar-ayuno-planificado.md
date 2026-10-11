## H-044: Cambiar un ayuno que aún no ha empezado

**Como** persona que ayuna,
**quiero** cambiar las horas previstas, las notas y la plantilla de un ayuno mientras todavía no lo he empezado,
**para** ajustar el plan cuando cambian mis circunstancias sin tener que borrarlo y crearlo de nuevo.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Solo un ayuno planificado ofrece «Cambiar»; uno en curso o terminado no (sus horas reales siguen corrigiéndose con motivo, H-005).
- [ ] Puedo cambiar la última comida prevista, el inicio previsto y el fin previsto; el objetivo se vuelve a calcular entre el inicio y el fin previstos, con las mismas validaciones que al crear (H-004).
- [ ] Si cambio las horas, se exige la misma semana de separación con los demás ayunos que al crear (H-004), sin contar este; cambiar solo las notas o la plantilla no la comprueba (EV-002).
- [ ] Puedo cambiar las notas generales del ayuno.
- [ ] Puedo cambiar la plantilla, o pasar a «desde cero»: el plan del ayuno se sustituye entero por una copia de la nueva plantilla (o queda vacío), y el fin previsto se propone de nuevo a partir de su duración objetivo.
- [ ] Antes de cambiar la plantilla, la app me avisa de que se reemplaza el plan de este ayuno y de que se pierden las marcas de la lista previa.
- [ ] Al guardar, las fechas de las etapas y los recordatorios se recalculan con los datos nuevos.
- [ ] Cada dato cambiado queda en la bitácora con su valor anterior, el nuevo y la hora; el motivo es opcional.
- [ ] Lo ya anotado en el ayuno (p. ej. mediciones de la preparación) no cambia al cambiar la plantilla.

### Consideraciones de Seguridad
Horas previstas y notas son datos Críticos: los cambios dejan rastro (S8) sin volcar los valores en los registros de la app (S4).

### Matriz CRUD (tejida el 2026-10-10, EV-001; re-tejida el 2026-10-11, EV-002)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| solo un ayuno planificado ofrece «Cambiar»; otros ayunos para la semana de separación | `fast.status` |  | ✓ |  |  |
| franja de los demás ayunos (D-17) | `fast.last_meal_actual`, `fast.clock_start_actual`, `fast.end_actual` |  | ✓ |  |  |
| última comida prevista | `fast.last_meal_planned` |  | ✓ | ✓ |  |
| inicio previsto | `fast.clock_start_planned` |  | ✓ | ✓ |  |
| fin previsto → objetivo recalculado | `fast.target_hours` |  | ✓ | ✓ |  |
| notas generales | `fast.notes` |  | ✓ | ✓ |  |
| cambiar la plantilla o pasar a «desde cero» | `fast.template_id` |  | ✓ | ✓ |  |
| hitos de la nueva plantilla | `fast.milestone_hours` |  |  | ✓ |  |
| fin propuesto desde la duración de la nueva plantilla | `template.target_hours` |  | ✓ |  |  |
| hitos de origen | `template.milestone_hours` |  | ✓ |  |  |
| plan sustituido: etapas | `stage.kind` | ✓ |  |  | ✓ |
| plan sustituido: duración de etapas | `stage.duration_value` | ✓ |  |  | ✓ |
| plan sustituido: mediciones | `plan_metric.metric_type_code` | ✓ |  |  | ✓ |
| plan sustituido: horarios | `metric_schedule.time_of_day` | ✓ |  |  | ✓ |
| plan sustituido: metas | `intake_goal.daily_amount` | ✓ |  |  | ✓ |
| plan sustituido: lista previa | `checklist_item.text` | ✓ |  |  | ✓ |
| se pierden las marcas de la lista previa | `checklist_item.is_checked` |  |  |  | ✓ |
| plan sustituido: avisos | `stage_notice.text` | ✓ |  |  | ✓ |
| plan sustituido: estado de productos | `plan_product.during_fast` | ✓ |  |  | ✓ |
| plan sustituido: reglas | `rule.condition_kind` | ✓ |  |  | ✓ |
| plan sustituido: vuelta a comer | `refeed_step.description` | ✓ |  |  | ✓ |
| bitácora: dato cambiado | `edit_log.field` | ✓ |  |  |  |
| bitácora: valor anterior | `edit_log.old_value` | ✓ |  |  |  |
| bitácora: valor nuevo | `edit_log.new_value` | ✓ |  |  |  |
| bitácora: hora | `edit_log.edited_at` | ✓ |  |  |  |
| bitácora: motivo opcional | `edit_log.reason` | ✓ |  |  |  |
| bitácora: entidad, fila y acción | `edit_log.entity_name`, `edit_log.row_id`, `edit_log.action` | ✓ |  |  |  |
