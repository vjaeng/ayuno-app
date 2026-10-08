## H-013: Mantener mi lista de suplementos y medicamentos

**Como** persona que ayuna,
**quiero** una lista de productos (suplementos y medicamentos) con nombre, código, dosis, unidad y horarios, en la que pueda marcar algunos como «a evitar»,
**para** tener en un solo sitio todo lo que tomo y no tomar sin darme cuenta algo que decidí evitar.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada producto tiene nombre, código interno, tipo (suplemento o medicamento), dosis, unidad y horarios.
- [ ] Puedo añadir, editar y desactivar productos; el código no cambia al editarlo.
- [ ] Anotar un producto marcado «a evitar» muestra un aviso y la toma se registra igual.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre del producto | `product.name` | ✓ | ✓ | ✓ |  |
| código interno, inmutable | `product.code` | ✓ | ✓ |  |  |
| tipo (suplemento o medicamento) | `product.kind` | ✓ | ✓ | ✓ |  |
| dosis | `product.default_amount` | ✓ | ✓ | ✓ |  |
| unidad | `product.unit` | ✓ | ✓ | ✓ |  |
| horarios | `product_schedule.time_of_day` | ✓ | ✓ | ✓ | ✓ |
| producto del horario | `product_schedule.product_code` | ✓ |  |  |  |
| dosis de cada toma | `product_schedule.amount` | ✓ | ✓ | ✓ |  |
| desactivar productos | `product.active` | ✓ | ✓ | ✓ |  |
| marca «a evitar» | `product.avoid` | ✓ | ✓ | ✓ |  |
| la toma se registra igual | `product_log.product_code` | ✓ |  |  |  |
