## H-024: Limitar una regla a ciertos momentos o contextos

**Como** persona que ayuna,
**quiero** añadir a una regla filtros opcionales: solo en ciertas etapas, solo al despertar o solo con cierto contexto (p. ej. de pie),
**para** que la regla salte solo cuando de verdad aplica.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Una regla filtrada por etapa no salta fuera de esas etapas.
- [ ] Una regla «solo al despertar» solo se evalúa sobre lecturas marcadas al despertar.
- [ ] Una regla «solo de pie» solo se evalúa sobre mediciones con esa posición.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| solo en ciertas etapas | `rule.filter_stage_kinds` | ✓ | ✓ | ✓ |  |
| solo al despertar | `rule.filter_on_waking` | ✓ | ✓ | ✓ |  |
| solo con cierta posición | `rule.filter_position` | ✓ | ✓ | ✓ |  |
| etapa en curso | `stage.kind` |  | ✓ |  |  |
| lecturas marcadas al despertar | `measurement.on_waking` |  | ✓ |  |  |
| lecturas de presión marcadas al despertar | `blood_pressure_reading.on_waking` |  | ✓ |  |  |
| mediciones con esa posición | `blood_pressure_reading.position` |  | ✓ |  |  |
