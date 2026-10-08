## H-027: Decidir yo ante cada alerta y que quede constancia

**Como** persona que ayuna,
**quiero** que ante cada alerta la app me pida elegir «termino el ayuno» o «sigo» (con motivo) y registre todo,
**para** conservar el control de la decisión y el historial de lo que pasó.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] Ninguna alerta termina el ayuno sin mi confirmación.
- [ ] Si elijo seguir, el motivo es obligatorio.
- [ ] Cada alerta queda registrada con hora, regla, lecturas que la dispararon y mi decisión, incluso si sigo.
- [ ] Las alertas aparecen en la línea de tiempo y en la exportación.

### Consideraciones de Seguridad
Lo crítico deja rastro (S8): el registro de alertas no se pierde ni se edita sin rastro.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| decisión: termino o sigo | `alert.user_choice` | ✓ | ✓ |  |  |
| motivo obligatorio si sigo | `alert.choice_reason` | ✓ | ✓ |  |  |
| hora | `alert.fired_at` |  | ✓ |  |  |
| regla | `alert.rule_id` |  | ✓ |  |  |
| lecturas que la dispararon | `alert.trigger_entry_ids` |  | ✓ |  |  |
| nombre de la regla mostrada | `rule.name` |  | ✓ |  |  |
| mensaje mostrado | `rule.message` |  | ✓ |  |  |
