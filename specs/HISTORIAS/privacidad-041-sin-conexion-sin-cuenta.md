## H-041: Usar la app entera sin internet ni cuenta

**Como** persona que ayuna,
**quiero** usar todas las funciones sin conexión y sin crear cuenta,
**para** que mis datos de salud no salgan del teléfono salvo cuando yo exporte.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] En modo avión puedo crear una plantilla, crear un ayuno desde ella, iniciar el reloj y anotar cada tipo de medición, síntoma, líquido y suplemento sin error.
- [ ] La app no pide crear cuenta ni iniciar sesión.
- [ ] La app no incluye analítica, publicidad ni bibliotecas que envíen datos a terceros.

### Consideraciones de Seguridad
Datos de salud (S4); dependencias como superficie de ataque (S7).

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| crear una plantilla | `template.name` | ✓ |  |  |  |
| crear un ayuno desde ella | `fast.template_id` | ✓ |  |  |  |
| iniciar el reloj | `fast.clock_start_actual` | ✓ |  |  |  |
| anotar mediciones | `measurement.value_entered` | ✓ |  |  |  |
| anotar síntomas | `symptom.symptom_type_code` | ✓ |  |  |  |
| anotar líquidos | `intake.amount` | ✓ |  |  |  |
| anotar suplementos | `product_log.status` | ✓ |  |  |  |
