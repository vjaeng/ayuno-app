## H-046: Ver mis ayunos planificados y elegir uno

**Como** persona que ayuna,
**quiero** ver en un solo lugar todos los ayunos que tengo planificados y abrir el que quiera para prepararlo, cambiarlo, borrarlo o empezarlo,
**para** planificar varios ayunos por adelantado y saber siempre sobre cuál estoy actuando.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Una pestaña «Planificados» lista todos los ayunos planificados, del más próximo al más lejano, cada uno con la fecha y hora de la última comida prevista (con día de la semana), el objetivo en horas y la plantilla de origen (o «desde cero»).
- [ ] Sin ayunos planificados, la pestaña explica cómo planificar uno y ofrece «Planificar ayuno».
- [ ] «Planificar ayuno» lleva a crear un ayuno (H-004) y, al guardarlo, vuelvo a la lista con el nuevo en su lugar.
- [ ] Puedo planificar ayunos aunque haya uno en curso.
- [ ] Al tocar un ayuno de la lista se abre su detalle con la última comida, el inicio y el fin previstos, el objetivo, las notas y la lista previa para marcar.
- [ ] Desde el detalle puedo registrar la última comida real (H-005), cambiar el ayuno (H-044), borrarlo (H-045) y empezarlo (H-005).
- [ ] Si hay otro ayuno en curso, el detalle no ofrece «Empieza el ayuno ahora» y explica que primero hay que terminar el que está en curso.
- [ ] Un ayuno que empiezo o borro desaparece de la lista.

### Consideraciones de Seguridad
Horas previstas y notas son datos Críticos (S4): se muestran solo en pantalla, nunca en los registros de la app.

### Matriz CRUD (tejida el 2026-10-11, EV-002)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| solo los ayunos planificados; otro en curso oculta «Empieza el ayuno ahora» | `fast.status` |  | ✓ |  |  |
| fecha y hora de la última comida prevista; orden de la lista | `fast.last_meal_planned` |  | ✓ |  |  |
| inicio previsto | `fast.clock_start_planned` |  | ✓ |  |  |
| objetivo en horas; fin previsto | `fast.target_hours` |  | ✓ |  |  |
| plantilla de origen (o «desde cero») | `fast.template_id` |  | ✓ |  |  |
| nombre de la plantilla | `template.name` |  | ✓ |  |  |
| notas | `fast.notes` |  | ✓ |  |  |
| lista previa: texto | `checklist_item.text` |  | ✓ |  |  |
| lista previa: marcar | `checklist_item.is_checked` |  | ✓ | ✓ |  |
| última comida real (acción de H-005) | `fast.last_meal_actual` |  | ✓ | ✓ |  |
| empezar el ayuno (acción de H-005) | `fast.clock_start_actual` |  |  | ✓ |  |
| borrar (acción de H-045) | `fast` (todo el ayuno) |  |  |  | ✓ |
