## H-026: Que una regla programe otra medición u ofrezca terminar

**Como** persona que ayuna,
**quiero** elegir para cada regla una acción: ninguna, programar una nueva medición en N minutos u ofrecer terminar el ayuno,
**para** que la app me ayude a confirmar o a parar según lo que yo decidí de antemano.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Una regla con acción «medir de nuevo en 15 minutos» programa ese recordatorio.
- [ ] Una regla con acción «ofrecer terminar» muestra el botón para terminar el ayuno junto a la alerta.
- [ ] El recordatorio programado por una regla suena aunque haya silencio nocturno.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| acción: ninguna, medir de nuevo, ofrecer terminar | `rule.action` | ✓ | ✓ | ✓ |  |
| en N minutos | `rule.action_minutes` | ✓ | ✓ | ✓ |  |
| hora de la alerta (base del recordatorio) | `alert.fired_at` |  | ✓ |  |  |
