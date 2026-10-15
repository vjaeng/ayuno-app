## H-018: Ver el día como una línea de tiempo

**Como** persona que ayuna,
**quiero** una vista «Hoy» con lo hecho, lo pendiente, lo atrasado y lo no hecho del día,
**para** ver de un vistazo qué me falta y retomar lo atrasado.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada elemento del día aparece como hecho, pendiente, atrasado o no hecho.
- [ ] Tocar un pendiente lo abre ya rellenado.
- [ ] Tocar una medición o presión hecha la abre con sus valores para verla o corregirla (H-048, H-034) (EV-004).
- [ ] La hora del ayuno («hora 33») aparece siempre junto a la fecha y hora reales.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-14, EV-004)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| hora programada de cada pendiente | `metric_schedule.time_of_day` |  | ✓ |  |  |
| hecho: medición anotada | `measurement.taken_at` |  | ✓ |  |  |
| pendiente al que responde | `measurement.scheduled_for` |  | ✓ |  |  |
| no hecho | `measurement.not_done_reason` |  | ✓ |  |  |
| hecho: presión anotada | `blood_pressure_reading.taken_at` |  | ✓ |  |  |
| pendiente al que responde (presión) | `blood_pressure_reading.scheduled_for` |  | ✓ |  |  |
| toma hecha o no hecha | `product_log.status` |  | ✓ |  |  |
| toma pendiente a la que responde | `product_log.scheduled_at` |  | ✓ |  |  |
| pasos de vuelta a comer | `refeed_check.done_at` |  | ✓ |  |  |
| hora del ayuno junto a la hora real | `fast.clock_start_actual` |  | ✓ |  |  |
| tocar un hecho lo abre (medición o presión, para H-048) | `measurement.id`, `blood_pressure_reading.id` |  | ✓ |  |  |
