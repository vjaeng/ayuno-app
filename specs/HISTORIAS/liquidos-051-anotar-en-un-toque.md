## H-051: Anotar agua y sal con un toque desde el reloj

**Como** persona que ayuna,
**quiero** sumar un vaso de agua o una pizca de sal tocando un botón en la pantalla del reloj, y anotar de una vez el agua con la sal que lleva disuelta,
**para** que anotar lo que tomo cueste menos que no anotarlo, incluso con dolor de cabeza y desde el teléfono.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Durante el ayuno, Inicio muestra un botón rápido por cada tipo de ingesta que tenga «medida del botón rápido»; de fábrica, «+1 vaso de agua» y «+1 pizca de sal».
- [ ] Un toque guarda, sin formulario, una ingesta de 1 de esa medida con la hora actual; el total del día sube al instante y aparece una confirmación breve con «Deshacer».
- [ ] «Deshacer» quita esa ingesta dejando constancia en la bitácora, como cualquier borrado (H-034).
- [ ] Mantener pulsado el botón abre el formulario completo con ese tipo y esa medida, para cambiar cantidad u hora; la pantalla dice que se puede mantener pulsado.
- [ ] En el formulario, «Agua con sal» permite anotar en una sola entrada «1 vaso con ¼ de cucharadita de sal»: se guardan dos ingestas, agua y sal, con la misma hora y enlazadas como un mismo grupo; o se guardan las dos o ninguna.
- [ ] Elijo en Catálogos qué medida usa el botón rápido de cada líquido, o que no tenga botón.
- [ ] Las reglas se evalúan igual que con cualquier ingesta (por ejemplo, la de sal bajo la meta, H-022); un tipo «rompe el ayuno» no tiene botón rápido (necesita la pregunta de H-012).

*Decisión del reporte de impacto (EV-006, 4): el botón de sal suma una pizca de fábrica.*

### Consideraciones de Seguridad
Guardar antes de confirmar (requisito transversal de fiabilidad): la confirmación solo aparece tras escribir. «Deshacer» deja rastro (S8). El grupo agua + sal se escribe todo o nada.

### Matriz CRUD (tejida el 2026-10-26, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| medida del botón rápido de cada líquido (o ninguna) | `intake_type.quick_unit_code` | ✓ | ✓ | ✓ |  |
| un tipo que rompe el ayuno no tiene botón | `intake_type.breaks_fast` |  | ✓ |  |  |
| texto del botón («+1 pizca de sal») | `household_unit.name`, `intake_type.name` |  | ✓ |  |  |
| ingesta de 1 de esa medida con la hora actual | `intake.intake_type_code`, `intake.taken_at`, `intake.amount_entered`, `intake.unit_entered`, `intake.amount`, `intake.unit` | ✓ |  |  |  |
| equivalente de la medida | `household_unit.equivalent` |  | ✓ |  |  |
| total del día sube al instante | derivado «Totales del día» |  | ✓ |  |  |
| solo durante el ayuno | `fast.status` |  | ✓ |  |  |
| «Deshacer» quita la ingesta | `intake.deleted_at` (control) |  |  |  | ✓ |
| constancia en la bitácora | `edit_log.entity_name`, `edit_log.row_id`, `edit_log.action`, `edit_log.edited_at` | ✓ |  |  |  |
| agua con sal: dos ingestas con la misma hora, enlazadas | `intake.group_id`, `intake.taken_at` | ✓ |  |  |  |
| reglas evaluadas como con cualquier ingesta | `rule.*` (vía H-022) → `alert.*` |  | ✓ |  |  |
