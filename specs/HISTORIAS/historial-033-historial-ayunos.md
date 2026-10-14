## H-033: Revisar mis ayunos pasados

**Como** persona que ayuna,
**quiero** una lista de ayunos con fecha, duración, objetivo y motivo de fin, y dentro de cada uno su línea de tiempo con todas las entradas y los totales de líquidos por día,
**para** tener a mano mis ayunos y entender cómo fue cada uno de principio a fin.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada ayuno de la lista muestra fecha de inicio, duración final, duración objetivo y motivo de fin; desde ahí puedo abrirlo y exportarlo.
- [ ] La línea de tiempo incluye mediciones, síntomas, ingestas, tomas, alertas, notas y pasos de vuelta a comer, en orden de hora.
- [ ] Se muestran los totales de cada tipo de ingesta por día.
- [ ] Las fechas llevan día de la semana.
- [ ] El detalle de cada ayuno muestra el peso inicial, el final y la diferencia, si hay pesos anotados (H-047, EV-003).

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-13, EV-003)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| fecha de inicio | `fast.clock_start_actual` |  | ✓ |  |  |
| duración final | `fast.end_actual` |  | ✓ |  |  |
| duración objetivo | `fast.target_hours` |  | ✓ |  |  |
| motivo de fin | `fast.end_reason` |  | ✓ |  |  |
| detalle del motivo | `fast.end_reason_detail` |  | ✓ |  |  |
| regla del fin | `fast.end_rule_id` |  | ✓ |  |  |
| línea de tiempo: mediciones | `measurement.*` |  | ✓ |  |  |
| línea de tiempo: presión | `blood_pressure_reading.*` |  | ✓ |  |  |
| línea de tiempo: síntomas | `symptom.*` |  | ✓ |  |  |
| línea de tiempo: ingestas | `intake.*` |  | ✓ |  |  |
| línea de tiempo: tomas | `product_log.*` |  | ✓ |  |  |
| línea de tiempo: alertas | `alert.*` |  | ✓ |  |  |
| línea de tiempo: notas | `note.*` |  | ✓ |  |  |
| línea de tiempo: pasos de vuelta a comer | `refeed_check.*` |  | ✓ |  |  |
| totales de ingesta por día | `intake.amount` |  | ✓ |  |  |
| peso inicial, final y diferencia | `measurement.value_num` |  | ✓ |  |  |
| unidad en que se muestra el peso | `metric_type.entry_unit` |  | ✓ |  |  |
