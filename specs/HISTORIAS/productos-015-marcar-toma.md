## H-015: Marcar cada toma de un producto

**Como** persona que ayuna,
**quiero** marcar cada toma como tomada con su hora real, no tomada con motivo, o con la dosis cambiada,
**para** saber exactamente qué tomé y cuándo.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] «Tomado» guarda la hora real; «no tomado» exige motivo.
- [ ] «Dosis cambiada» guarda la nueva dosis y el motivo sin borrar la planificada.
- [ ] Las tomas pendientes aparecen en la vista del día.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| estado de la toma (tomado, no tomado, dosis cambiada) | `product_log.status` | ✓ |  |  |  |
| hora real | `product_log.taken_at` | ✓ |  |  |  |
| motivo (no tomado, dosis cambiada) | `product_log.reason` | ✓ |  |  |  |
| nueva dosis | `product_log.amount` | ✓ |  |  |  |
| unidad de la dosis | `product_log.unit` | ✓ |  |  |  |
| dosis planificada conservada | `product_log.planned_amount` | ✓ | ✓ |  |  |
| toma pendiente a la que responde | `product_log.scheduled_at` | ✓ | ✓ |  |  |
| producto | `product_log.product_code` | ✓ |  |  |  |
| tomas pendientes del día | `product_schedule.time_of_day` |  | ✓ |  |  |
| dosis planificada de origen | `product_schedule.amount` |  | ✓ |  |  |
