## H-002: Completar la plantilla con metas, lista previa y avisos

**Como** persona que ayuna,
**quiero** añadir a la plantilla, si quiero, metas diarias por tipo de líquido o electrolito, una lista de comprobación previa y avisos de texto libre para ciertas etapas,
**para** llegar preparado al ayuno, saber cuánto me falta de cada cosa y tener presentes mis indicaciones cuando aplican.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada meta es opcional y se define por tipo de ingesta con su unidad; un tipo sin meta solo muestra el total del día.
- [ ] Los elementos de la lista previa son texto libre (p. ej. «comprar sal», «cargar el tensiómetro») y se pueden marcar.
- [ ] Dejar elementos de la lista sin marcar no impide empezar el ayuno; la app solo informa cuántos faltan.
- [ ] Cada aviso fijo se asocia a una o varias etapas y solo se muestra mientras dura alguna de ellas.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| meta diaria por tipo de ingesta | `intake_goal.intake_type_code` | ✓ | ✓ |  | ✓ |
| cantidad de la meta | `intake_goal.daily_amount` | ✓ | ✓ | ✓ |  |
| unidad de la meta | `intake_goal.unit` | ✓ | ✓ | ✓ |  |
| elemento de la lista previa | `checklist_item.text` | ✓ | ✓ | ✓ | ✓ |
| elemento marcado | `checklist_item.is_checked` | ✓ | ✓ | ✓ |  |
| aviso fijo de texto libre | `stage_notice.text` | ✓ | ✓ | ✓ | ✓ |
| etapas en que se muestra el aviso | `stage_notice.stage_kinds` | ✓ | ✓ | ✓ |  |
| etapa en curso (para mostrar el aviso) | `stage.kind` |  | ✓ |  |  |
