## H-034: Corregir o borrar una entrada dejando rastro

**Como** persona que ayuna,
**quiero** editar o borrar cualquier entrada ya guardada y poder restaurar una borrada,
**para** corregir errores sin perder lo que había antes.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Al editar, la bitácora guarda el valor anterior, el nuevo, la hora y el motivo.
- [ ] La entrada editada conserva su identificador: en la exportación sustituye a la anterior, no se duplica.
- [ ] Una entrada borrada deja de verse en la línea de tiempo, queda en la bitácora y se puede restaurar.
- [ ] Las entradas borradas se exportan marcadas como borradas, no se omiten.
- [ ] Corregir una entrada de un ayuno en curso vuelve a evaluar mis reglas con el valor nuevo (puede saltar un aviso o una alerta); las alertas ya registradas no cambian. Borrar o restaurar no evalúa reglas (EV-004).
- [ ] Lo corregido, borrado o restaurado se refleja al momento en Inicio (última medición, peso), en Hoy y en los totales (EV-004).
- [ ] Guardar sin haber cambiado nada no escribe nada ni deja rastro (EV-004).

*Alcance por entregas (EV-004):* primero mediciones y presión arterial (con H-048); síntomas, ingestas, tomas, notas y comidas quedan pendientes dentro de esta historia.

### Consideraciones de Seguridad
Lo crítico deja rastro (S8): nada se sobrescribe ni se borra sin rastro.

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-14, EV-004)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| editar cualquier entrada: mediciones | `measurement.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: presión | `blood_pressure_reading.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: síntomas | `symptom.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: ingestas | `intake.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: tomas | `product_log.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: notas | `note.*` |  |  | ✓ | ✓ |
| editar cualquier entrada: comidas | `meal.*` |  |  | ✓ | ✓ |
| valor anterior | `edit_log.old_value` | ✓ | ✓ |  |  |
| valor nuevo | `edit_log.new_value` | ✓ | ✓ |  |  |
| hora del cambio | `edit_log.edited_at` | ✓ | ✓ |  |  |
| motivo | `edit_log.reason` | ✓ | ✓ |  |  |
| campo cambiado | `edit_log.field` | ✓ | ✓ |  |  |
| entrada afectada | `edit_log.row_id` | ✓ |  |  |  |
| entidad afectada | `edit_log.entity_name` | ✓ |  |  |  |
| edición, borrado o restauración | `edit_log.action` | ✓ | ✓ |  |  |
| reglas del ayuno evaluadas al corregir en curso | `rule.*` (las vigentes del ayuno) |  | ✓ |  |  |
| ayuno en curso (decide si se evalúan reglas) | `fast.status` |  | ✓ |  |  |
| alerta nueva si la corrección cumple una regla | `alert.rule_id`, `alert.fired_at`, `alert.trigger_entry_ids` | ✓ |  |  |  |
| valor equivalente recalculado | `measurement.value_num` |  |  | ✓ |  |
