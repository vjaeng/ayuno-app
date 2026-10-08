## H-014: Decidir qué productos siguen y cuáles salen en el ayuno

**Como** persona que ayuna,
**quiero** indicar en cada plantilla si cada producto sigue, sale o es opcional durante el ayuno, con un motivo opcional,
**para** no tomar por costumbre algo que no quiero durante el ayuno.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] Cada producto tiene en la plantilla un estado: sigue, sale u opcional, y un motivo opcional.
- [ ] Solo se recuerdan los que siguen (y los opcionales que active).
- [ ] Los que salen se muestran en gris como recordatorio de lo que no se toma.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| producto en la plantilla | `plan_product.product_code` | ✓ | ✓ |  |  |
| estado: sigue, sale u opcional | `plan_product.during_fast` | ✓ | ✓ | ✓ |  |
| opcionales que active | `plan_product.optional_enabled` | ✓ | ✓ | ✓ |  |
| motivo opcional | `plan_product.reason` | ✓ | ✓ | ✓ |  |
| horarios a recordar | `product_schedule.time_of_day` |  | ✓ |  |  |
| nombre del producto (en gris si sale) | `product.name` |  | ✓ |  |  |
