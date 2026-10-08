## H-021: Definir una regla que salte con un síntoma de alarma

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan al marcar un síntoma de alarma,
**para** que los síntomas manden sobre los números.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] Una regla sobre un síntoma de alarma salta al marcarlo, con cualquier valor de las mediciones.
- [ ] Puedo hacer la regla para un síntoma concreto o para cualquier síntoma de alarma.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| síntoma concreto o cualquiera de alarma | `rule.symptom_type_code` | ✓ | ✓ | ✓ |  |
| síntoma de alarma | `symptom_type.is_alarm` |  | ✓ |  |  |
| síntoma marcado | `symptom.symptom_type_code` |  | ✓ |  |  |
| registro de la regla cumplida | `alert.rule_id` | ✓ |  |  |  |
| síntoma que la dispara | `alert.trigger_entry_ids` | ✓ |  |  |  |
