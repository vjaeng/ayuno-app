## H-004: Crear un ayuno desde una plantilla con dos datos

**Como** persona que ayuna,
**quiero** crear un ayuno eligiendo una plantilla e indicando la fecha y hora de la última comida y las horas previstas de inicio y de fin del ayuno, que cada hito guarde por separado su hora planificada y su hora real, y poder añadirle notas generales,
**para** planificar un ayuno en segundos y saber después cuánto me desvié del plan.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] El objetivo en horas se calcula solo, como la diferencia entre el inicio y el fin previstos; con plantilla, el fin se propone a partir de su duración objetivo y se puede cambiar.
- [ ] Si el fin previsto no es posterior al inicio, no se puede crear el ayuno.
- [ ] Con la plantilla, la app calcula las fechas de las etapas y los recordatorios.
- [ ] También puedo crear un ayuno desde cero, sin plantilla.
- [ ] Al planificar viene propuesta la primera plantilla de la lista, no «desde cero»; puedo elegir otra o empezar desde cero (cambio 2026-10-13).
- [ ] El ayuno guarda una copia del plan de la plantilla: cambios posteriores a la plantilla no lo alteran.
- [ ] Un ayuno recién creado queda como planificado, no en curso.
- [ ] Puedo crear un ayuno aunque haya otro en curso (EV-002).
- [ ] Entre el fin de cualquier otro ayuno (planificado, en curso o terminado) y la última comida prevista del nuevo, y entre el fin del nuevo y la última comida prevista del siguiente, deben pasar al menos 7 días; si no, la app dice con qué ayuno choca y desde cuándo puedo planificarlo, y no lo guarda (EV-002).
- [ ] Cada hito (última comida, inicio del reloj, fin) guarda hora planificada y hora real por separado; registrar la real no borra la planificada.
- [ ] Si difieren, se muestran las dos juntas (p. ej. «previsto 22:00 · real 22:10»); un hito que no ha ocurrido muestra solo la planificada.
- [ ] Puedo escribir y editar unas notas generales del ayuno, sin hora, distintas de las notas libres con hora de la línea de tiempo.

*Cambiar un ayuno planificado (horas, notas y plantilla) → H-044 (EV-001). Ver y elegir entre los planificados → H-046 (EV-002).*

### Consideraciones de Seguridad
Datos de salud (S4): se guardan solo en el teléfono y no salen de él salvo exportación explícita.

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-11, EV-002)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| plantilla elegida (o desde cero; propuesta la primera) | `fast.template_id` | ✓ |  |  |  |
| fecha y hora de la última comida (planificada) | `fast.last_meal_planned` | ✓ | ✓ |  |  |
| hora prevista de inicio del reloj | `fast.clock_start_planned` | ✓ | ✓ |  |  |
| hora real de la última comida (también franja de otros ayunos, D-17) | `fast.last_meal_actual` |  | ✓ |  |  |
| hora real de inicio del reloj (también franja del ayuno en curso) | `fast.clock_start_actual` |  | ✓ |  |  |
| hora real de fin (franja de un terminado, D-17) | `fast.end_actual` |  | ✓ |  |  |
| otros ayunos planificados, en curso o terminados (semana de separación) | `fast.status` |  | ✓ |  |  |
| objetivo calculado entre inicio y fin previstos | `fast.target_hours` | ✓ | ✓ |  |  |
| hitos copiados | `fast.milestone_hours` | ✓ |  |  |  |
| ayuno recién creado queda planificado | `fast.status` | ✓ |  |  |  |
| notas generales del ayuno | `fast.notes` | ✓ | ✓ | ✓ |  |
| plantilla: duración y hitos de origen | `template.target_hours` |  | ✓ |  |  |
| copia del plan: etapas | `stage.kind` | ✓ | ✓ |  |  |
| copia del plan: duración de etapas | `stage.duration_value` | ✓ | ✓ |  |  |
| copia del plan: unidad de etapas | `stage.duration_unit` | ✓ |  |  |  |
| copia del plan: mediciones activas | `plan_metric.metric_type_code` | ✓ |  |  |  |
| copia del plan: horarios | `metric_schedule.time_of_day` | ✓ |  |  |  |
| copia del plan: etapa de horarios | `metric_schedule.stage_kind` | ✓ |  |  |  |
| copia del plan: metas | `intake_goal.daily_amount` | ✓ |  |  |  |
| copia del plan: lista previa | `checklist_item.text` | ✓ |  |  |  |
| copia del plan: avisos | `stage_notice.text` | ✓ |  |  |  |
| copia del plan: estado de productos | `plan_product.during_fast` | ✓ |  |  |  |
| copia del plan: reglas | `rule.condition_kind` | ✓ |  |  |  |
| copia del plan: vuelta a comer | `refeed_step.description` | ✓ |  |  |  |
