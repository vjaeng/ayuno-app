## H-047: Anotar mi peso al empezar y al terminar y ver cuánto cambió

**Como** persona que ayuna,
**quiero** que la app me pida el peso justo al empezar y al terminar el ayuno, y me muestre el peso inicial, el final y la diferencia,
**para** saber cuánto cambió mi peso en cada ayuno sin tener que acordarme de anotarlo ni de hacer la cuenta.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Justo después de tocar «Empieza el ayuno ahora» se ofrece anotar el peso, con la unidad por defecto del peso (kg o lb) y la otra a elegir; «Ahora no» lo salta. El ayuno ya ha empezado: empezar sigue siendo un solo toque.
- [ ] El formulario de «Terminar el ayuno» tiene un campo de peso opcional; si lo relleno, se guarda junto con el fin y con la hora de fin; si falla el guardado, no se guarda ni el fin ni el peso.
- [ ] Un peso fuera del rango válido pide la confirmación de siempre (H-008).
- [ ] Si la medición «Peso» está desactivada en el catálogo, no se pide.
- [ ] El peso inicial es el último anotado no después del inicio real del reloj o, si no hay, el primero del ayuno; el final es el último anotado no después del fin real y posterior al inicial o, si no hay, el primero después del fin.
- [ ] La pantalla de resultado al terminar y el detalle del ayuno muestran «inicial → final · diferencia» en la unidad por defecto del peso, sin colores de juicio; con un solo peso se muestra ese, sin diferencia; sin pesos, nada.
- [ ] Los pesos así anotados son mediciones como las demás: salen en la línea de tiempo, se corrigen o borran (H-034) y se exportan (H-036). El de empezar evalúa las reglas; el de terminar no.

### Consideraciones de Seguridad
El peso es un dato de salud (Crítico, como toda medición): cifrado en la base y nunca en registros de depuración (S4).

### Matriz CRUD (tejida el 2026-10-13, EV-003)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| peso anotado (al empezar o al terminar) | `measurement.metric_type_code` | ✓ | ✓ |  |  |
| hora del peso (ahora al empezar; hora de fin al terminar) | `measurement.taken_at` | ✓ | ✓ |  |  |
| valor tecleado | `measurement.value_entered` | ✓ |  |  |  |
| unidad tecleada (kg o lb) | `measurement.unit_entered` | ✓ |  |  |  |
| valor equivalente en kg (para comparar) | `measurement.value_num` | ✓ | ✓ |  |  |
| unidad por defecto y la otra a elegir | `metric_type.entry_unit` |  | ✓ |  |  |
| unidad del catálogo y alternativa | `metric_type.unit` |  | ✓ |  |  |
| unidad alternativa | `metric_type.alt_unit` |  | ✓ |  |  |
| rango válido (confirmación) | `metric_type.min_valid` |  | ✓ |  |  |
| rango válido (confirmación) | `metric_type.max_valid` |  | ✓ |  |  |
| no se pide si «Peso» está desactivado | `metric_type.active` |  | ✓ |  |  |
| inicio real del reloj (para el inicial) | `fast.clock_start_actual` |  | ✓ |  |  |
| fin real (para el final y la hora del peso) | `fast.end_actual` |  | ✓ |  |  |
