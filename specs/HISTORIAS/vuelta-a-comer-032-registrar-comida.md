## H-032: Anotar cada comida y cómo me cayó

**Como** persona que ayuna,
**quiero** anotar cada comida con hora, descripción, cantidad aproximada y cómo me cayó (bien, pesadez, náusea, diarrea u otro),
**para** saber qué toleré al volver a comer.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] Cada comida guarda hora, descripción, cantidad aproximada y cómo cayó.
- [ ] «Otro» permite texto.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| hora de la comida | `meal.eaten_at` | ✓ |  |  |  |
| descripción | `meal.description` | ✓ |  |  |  |
| cantidad aproximada | `meal.approx_amount` | ✓ |  |  |  |
| cómo cayó | `meal.tolerance` | ✓ |  |  |  |
| texto de «otro» | `meal.tolerance_detail` | ✓ |  |  |  |
