## H-012: Anotar lo que bebo y ver cuánto llevo

**Como** persona que ayuna,
**quiero** anotar cada ingesta con su cantidad y unidad, ver el total del día frente a la meta y que la app me pregunte si estoy terminando cuando anoto algo que rompe el ayuno,
**para** llevar la cuenta sin hacer cuentas ni sentirme juzgado, y no dejar un ayuno abierto por descuido.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada ingesta guarda hora, tipo, cantidad, unidad y nota opcional.
- [ ] 5 g de sal muestran 1.97 g de sodio, calculado y no tecleado.
- [ ] Con meta se muestra «llevas X de Y»; sin meta, solo el total; sin colores de juicio.
- [ ] Anotar durante el ayuno un tipo marcado «rompe el ayuno» pregunta si lo estoy terminando.
- [ ] Si respondo que sí, paso a terminar el ayuno; si respondo que no, la ingesta se guarda igual y el ayuno sigue.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| hora de la ingesta | `intake.taken_at` | ✓ |  |  |  |
| tipo de ingesta | `intake.intake_type_code` | ✓ | ✓ |  |  |
| cantidad | `intake.amount` | ✓ | ✓ |  |  |
| unidad | `intake.unit` | ✓ |  |  |  |
| nota opcional | `intake.note` | ✓ |  |  |  |
| sodio calculado | `intake_type.sodium_factor` |  | ✓ |  |  |
| meta del día («llevas X de Y») | `intake_goal.daily_amount` |  | ✓ |  |  |
| tipo marcado «rompe el ayuno» | `intake_type.breaks_fast` |  | ✓ |  |  |
| durante el ayuno | `fast.status` |  | ✓ |  |  |
