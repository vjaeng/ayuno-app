## H-001: Crear una plantilla con qué medir y cuándo

**Como** persona que ayuna,
**quiero** crear y editar una plantilla con su nombre, duración objetivo en horas, las etapas que usa (preparación, ayuno, vuelta a comer, posterior) con su duración, los hitos que quiero ver y las mediciones activas con sus horarios de recordatorio por etapa,
**para** preparar una vez el plan de un tipo de ayuno y reutilizarlo cada vez que lo repita.

**Prioridad:** alta · **Estimación:** L

### Criterios de Aceptación
- [ ] Puedo crear una plantilla indicando nombre y duración objetivo en horas.
- [ ] Elijo qué etapas usa; preparación y posterior se miden en días, vuelta a comer en horas.
- [ ] Defino los hitos que se mostrarán en el reloj (p. ej. 12, 24, 36 y 48 horas).
- [ ] Activo o desactivo cada tipo de medición del catálogo y defino sus horarios de recordatorio, distintos por etapa.
- [ ] Una medición desactivada en la plantilla no genera recordatorios; se puede anotar igual mientras esté activa en el catálogo (cambio 2026-10-13).
- [ ] Editar una plantilla no cambia los ayunos ya creados desde ella.
- [ ] Todo funciona en modo avión.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre de la plantilla | `template.name` | ✓ | ✓ | ✓ |  |
| duración objetivo en horas | `template.target_hours` | ✓ | ✓ | ✓ |  |
| hitos del reloj | `template.milestone_hours` | ✓ | ✓ | ✓ |  |
| etapas que usa la plantilla | `stage.kind` | ✓ | ✓ |  | ✓ |
| duración de cada etapa | `stage.duration_value` | ✓ | ✓ | ✓ |  |
| unidad de la duración (días u horas) | `stage.duration_unit` | ✓ | ✓ |  |  |
| medición activa en la plantilla | `plan_metric.metric_type_code` | ✓ | ✓ |  | ✓ |
| tipos del catálogo de mediciones | `metric_type.name` |  | ✓ |  |  |
| horarios de recordatorio por etapa | `metric_schedule.time_of_day` | ✓ | ✓ | ✓ | ✓ |
| etapa del horario | `metric_schedule.stage_kind` | ✓ | ✓ | ✓ |  |
