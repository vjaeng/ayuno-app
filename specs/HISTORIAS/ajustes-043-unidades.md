## H-043: Elegir mis unidades por defecto

**Como** persona que ayuna,
**quiero** elegir las unidades por defecto de glucosa (mg/dL o mmol/L), peso (kg o lb), cintura (cm o in) y temperatura (°C o °F),
**para** teclear en la unidad que leo en mis aparatos.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] Cambiar una unidad por defecto no altera las entradas ya guardadas.
- [ ] Cada entrada conserva la unidad con que se tecleó.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| unidad por defecto de glucosa, peso, cintura, temperatura | `metric_type.entry_unit` |  | ✓ | ✓ |  |
| opciones de unidad | `metric_type.alt_unit` |  | ✓ |  |  |
| cada entrada conserva su unidad | `measurement.unit_entered` |  | ✓ |  |  |
