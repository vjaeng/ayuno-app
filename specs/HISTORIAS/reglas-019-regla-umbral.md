## H-019: Definir una regla de alerta por umbral

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando una medición quede por debajo o por encima de un valor que yo elija,
**para** que la app aplique mis propios criterios de alerta sin tener que recordarlos.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Una regla tiene nombre, condición, nivel, mensaje y acción, y se puede activar o desactivar.
- [ ] La condición «medición por debajo de X» o «por encima de X» se cumple con una sola lectura.
- [ ] Las reglas se evalúan al guardar cada entrada y al llegar cada hora programada.
- [ ] Las reglas pertenecen a la plantilla y se copian al ayuno.
- [ ] La app no trae umbrales clínicos precargados.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre de la regla | `rule.name` | ✓ | ✓ | ✓ |  |
| condición | `rule.condition_kind` | ✓ | ✓ | ✓ |  |
| nivel | `rule.level` | ✓ |  |  |  |
| mensaje | `rule.message` | ✓ |  |  |  |
| acción | `rule.action` | ✓ |  |  |  |
| activar o desactivar | `rule.active` | ✓ | ✓ | ✓ |  |
| medición de la condición | `rule.metric_type_code` | ✓ | ✓ | ✓ |  |
| valor de la presión que se compara | `rule.bp_component` | ✓ | ✓ | ✓ |  |
| valor elegido (por debajo / por encima de X) | `rule.threshold` | ✓ | ✓ | ✓ |  |
| una sola lectura (valor) | `measurement.value_num` |  | ✓ |  |  |
| una sola lectura (tipo) | `measurement.metric_type_code` |  | ✓ |  |  |
| lectura de presión | `blood_pressure_reading.systolic` |  | ✓ |  |  |
| lectura de presión (diastólica) | `blood_pressure_reading.diastolic` |  | ✓ |  |  |
| lectura de presión (pulso) | `blood_pressure_reading.pulse` |  | ✓ |  |  |
| se evalúan al llegar cada hora programada | `metric_schedule.time_of_day` |  | ✓ |  |  |
| registro de la regla cumplida | `alert.rule_id` | ✓ |  |  |  |
| hora en que se cumple | `alert.fired_at` | ✓ |  |  |  |
| lecturas que la disparan | `alert.trigger_entry_ids` | ✓ |  |  |  |
