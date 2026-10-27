## H-057: Revisar lo que falta antes de empezar y empezar igual si quiero

**Como** persona que ayuna,
**quiero** que, al tocar «Empieza el ayuno ahora», la app me enseñe lo que mi plan tiene a medio hacer (reglas de ejemplo, metas sin cifra, lista previa sin marcar) y me deje empezar igual,
**para** no pasar un ayuno entero con reglas de relleno creyendo que tengo mis criterios de parada configurados.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Al tocar «Empieza el ayuno ahora», si el plan del ayuno tiene pendientes, se abre una pantalla que los lista agrupados: reglas de ejemplo sin tocar (activas o no), metas de ingesta sin cifra y elementos de la lista previa sin marcar.
- [ ] Cada pendiente tiene un acceso para arreglarlo en el plan de ese ayuno; al volver, la lista se actualiza.
- [ ] «Empezar de todos modos» empieza el ayuno igual que H-005 y deja guardada en el ayuno la lista de lo que faltaba en ese momento; «Volver» no empieza.
- [ ] Sin pendientes, el toque empieza el ayuno directamente, sin pantalla intermedia.
- [ ] Una regla es «de ejemplo» porque la app la trajo así, no por su texto; deja de serlo cuando cambio su nombre, su mensaje o su cifra.
- [ ] Con la plantilla de ejemplo sin tocar, la revisión lista sus reglas; tras «Empezar de todos modos», la exportación del ayuno lleva esas reglas en la lista de lo que faltaba, y el detalle del ayuno en Historial dice que empezó con pendientes.

*Los aparatos que faltan (M10 de la solicitud) se añadirán a la revisión con la evolución de la Prioridad 2.*

### Consideraciones de Seguridad
La lista guardada es registro de seguridad: se escribe en la misma transacción que empieza el ayuno y no se edita (S8). La revisión informa, no bloquea, y no aconseja.

### Matriz CRUD (tejida el 2026-10-26, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| pendientes del plan del ayuno | derivado «Revisión previa» (MODELO §2.5) |  | ✓ |  |  |
| reglas de ejemplo sin tocar (activas o no) | `rule.is_placeholder`, `rule.name`, `rule.active` |  | ✓ |  |  |
| metas sin cifra | `intake_goal.daily_amount` (nulo), `intake_goal.intake_type_code` |  | ✓ |  |  |
| lista previa sin marcar | `checklist_item.is_checked`, `checklist_item.text` |  | ✓ |  |  |
| arreglar en el plan de ese ayuno | `rule.*`, `intake_goal.daily_amount` (copia del ayuno); `checklist_item.is_checked` |  |  | ✓ |  |
| deja de ser de ejemplo al cambiar nombre, mensaje o cifra | `rule.is_placeholder` |  |  | ✓ |  |
| empezar igual que H-005 | `fast.status`, `fast.clock_start_actual`, `fast.tz_offset` (vía H-005) |  |  | ✓ |  |
| lista de lo que faltaba, guardada en el ayuno | `fast.started_with_warnings` | ✓ | ✓ |  |  |
| exportación con la lista | `fast.started_with_warnings` (vía H-036) |  | ✓ |  |  |
| Historial dice que empezó con pendientes | `fast.started_with_warnings` (vía H-033) |  | ✓ |  |  |
