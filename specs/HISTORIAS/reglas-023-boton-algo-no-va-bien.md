## H-023: Pedir una alerta manual cuando algo no va bien

**Como** persona que ayuna,
**quiero** un botón «algo no va bien» que dispare la regla que yo haya definido para ese caso,
**para** poder parar o pedir ayuda sin tener que justificarlo con un número.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] El botón está accesible desde la pantalla de seguridad.
- [ ] Tocarlo dispara la regla manual con su mensaje y su acción.
- [ ] Queda registrado como cualquier otra alerta.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| regla manual «algo no va bien» | `rule.condition_kind` | ✓ | ✓ |  |  |
| mensaje de la regla manual | `rule.message` |  | ✓ |  |  |
| acción de la regla manual | `rule.action` |  | ✓ |  |  |
| queda registrado como alerta | `alert.rule_id` | ✓ |  |  |  |
| hora | `alert.fired_at` | ✓ |  |  |  |
