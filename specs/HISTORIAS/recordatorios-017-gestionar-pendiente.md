## H-017: Resolver un pendiente desde la notificación

**Como** persona que ayuna,
**quiero** que la notificación me deje anotar directamente, posponer 10 o 30 minutos o marcar «no se hizo» con motivo, y poder configurar un silencio nocturno,
**para** gestionar los recordatorios sin navegar por la app y dormir sin interrupciones innecesarias.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] La acción directa (p. ej. «Anotar glucosa») abre el formulario con la hora programada ya puesta y editable.
- [ ] Posponer vuelve a avisar a los 10 o 30 minutos.
- [ ] «No se hizo» exige motivo y queda registrado como tal en la vista del día y en la exportación; un dato no anotado, en cambio, queda vacío, nunca como cero.
- [ ] El horario de silencio nocturno es configurable.
- [ ] Los recordatorios programados por una regla de alerta suenan aunque sea horario de silencio.

### Consideraciones de Seguridad
Las acciones de seguridad no se silencian (S6: fallar cerrado).

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| formulario con la hora programada | `measurement.scheduled_for` | ✓ |  |  |  |
| formulario con la hora programada (presión) | `blood_pressure_reading.scheduled_for` | ✓ |  |  |  |
| «no se hizo» con motivo | `measurement.not_done_reason` | ✓ | ✓ |  |  |
| «no se hizo» con motivo (presión) | `blood_pressure_reading.not_done_reason` | ✓ | ✓ |  |  |
| «no se hizo» de una toma | `product_log.status` | ✓ |  |  |  |
| motivo de la toma no hecha | `product_log.reason` | ✓ |  |  |  |
| inicio del silencio nocturno | `settings.quiet_start` | ✓ | ✓ | ✓ |  |
| fin del silencio nocturno | `settings.quiet_end` | ✓ | ✓ | ✓ |  |
| recordatorios de reglas suenan en silencio | `rule.action` |  | ✓ |  |  |
