## H-011: Definir mis tipos de líquidos y electrolitos

**Como** persona que ayuna,
**quiero** editar los tipos de ingesta (agua, infusiones, sal, potasio, magnesio, otros) con su unidad, si rompen el ayuno y, para la sal, su aporte de sodio,
**para** registrar exactamente lo que tomo y que la app sepa qué cosas rompen el ayuno.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] Cada tipo tiene nombre y unidad, y puedo añadir tipos propios.
- [ ] Puedo marcar un tipo como «rompe el ayuno» (p. ej. caldo, comida).
- [ ] La sal trae su factor de sodio (0.393) y es editable.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre del tipo de ingesta | `intake_type.name` | ✓ | ✓ | ✓ |  |
| unidad del tipo | `intake_type.unit` | ✓ | ✓ | ✓ |  |
| «rompe el ayuno» | `intake_type.breaks_fast` | ✓ | ✓ | ✓ |  |
| factor de sodio de la sal (0.393, editable) | `intake_type.sodium_factor` | ✓ | ✓ | ✓ |  |
