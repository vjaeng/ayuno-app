## H-020: Definir una regla de alerta sostenida en el tiempo

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando una medición se mantiene fuera de rango durante N minutos,
**para** no alarmarme por una lectura aislada y sí actuar cuando el problema persiste.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Una regla «glucosa bajo X sostenida 20 minutos» se cumple con dos lecturas bajo X separadas 20 minutos o más.
- [ ] Con una sola lectura bajo X no se cumple.
- [ ] Con dos lecturas bajo X separadas menos de 20 minutos no se cumple.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| durante N minutos | `rule.sustained_minutes` | ✓ | ✓ | ✓ |  |
| lecturas bajo X | `measurement.value_num` |  | ✓ |  |  |
| separadas 20 minutos o más | `measurement.taken_at` |  | ✓ |  |  |
| registro de la regla cumplida | `alert.rule_id` | ✓ |  |  |  |
| lecturas que la disparan | `alert.trigger_entry_ids` | ✓ |  |  |  |
