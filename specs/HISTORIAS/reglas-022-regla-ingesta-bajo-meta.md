## H-022: Definir una regla por ingesta bajo la meta a cierta hora

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan si a una hora dada el total del día de un tipo de ingesta está por debajo de su meta,
**para** recordar a tiempo que me falta, por ejemplo, sal.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] La regla se evalúa a la hora indicada.
- [ ] Si el total del día alcanza la meta antes de esa hora, no salta.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| tipo de ingesta | `rule.intake_type_code` | ✓ | ✓ | ✓ |  |
| hora dada | `rule.check_time` | ✓ | ✓ | ✓ |  |
| total del día | `intake.amount` |  | ✓ |  |  |
| meta | `intake_goal.daily_amount` |  | ✓ |  |  |
| registro de la regla cumplida | `alert.rule_id` | ✓ |  |  |  |
| hora en que se cumple | `alert.fired_at` | ✓ |  |  |  |
