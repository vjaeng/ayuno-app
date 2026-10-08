## H-031: Definir y seguir mi plan de vuelta a comer

**Como** persona que ayuna,
**quiero** definir en la plantilla la vuelta a comer como pasos con hora relativa al fin del ayuno o intervalo de repetición, más pasos de reintroducción por día, y seguirlos con recordatorio y casilla,
**para** volver a comer y reintroducir en el orden que decidí, sin hacer cuentas.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Un paso puede tener hora relativa al fin (p. ej. «a las 6 horas») o repetirse (p. ej. «una taza de caldo cada 60 minutos durante 4 horas»).
- [ ] Las horas reales de los pasos se calculan desde la hora real de fin del ayuno.
- [ ] Cada paso tiene recordatorio a su hora calculada y casilla; marcarlo guarda la hora real.
- [ ] Un paso de reintroducción se define como «día N tras el fin» y muestra su fecha con día de la semana y casilla.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| tipo de paso | `refeed_step.kind` | ✓ | ✓ |  |  |
| descripción del paso | `refeed_step.description` | ✓ | ✓ | ✓ |  |
| hora relativa al fin | `refeed_step.offset_minutes` | ✓ | ✓ | ✓ |  |
| intervalo de repetición | `refeed_step.repeat_every_minutes` | ✓ | ✓ | ✓ |  |
| durante cuánto se repite | `refeed_step.repeat_for_minutes` | ✓ | ✓ | ✓ |  |
| día N tras el fin | `refeed_step.day_offset` | ✓ | ✓ | ✓ |  |
| hora real de fin del ayuno | `fast.end_actual` |  | ✓ |  |  |
| ocurrencia de cada paso | `refeed_check.refeed_step_id` | ✓ |  |  |  |
| hora calculada del paso | `refeed_check.scheduled_at` | ✓ | ✓ |  |  |
| casilla marcada guarda la hora real | `refeed_check.done_at` | ✓ | ✓ | ✓ |  |
