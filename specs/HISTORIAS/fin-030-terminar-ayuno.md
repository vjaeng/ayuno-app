## H-030: Terminar el ayuno con su hora y motivo

**Como** persona que ayuna,
**quiero** tocar «Terminar el ayuno», que guarde la hora real y me pida el motivo (objetivo cumplido, regla de alerta, decisión personal u otro con texto),
**para** que la hora y el motivo del final queden como el resultado del ayuno, no como éxito o fracaso.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] El motivo es obligatorio; «otro» exige texto y «regla de alerta» indica cuál.
- [ ] La duración final se calcula desde el inicio real del reloj hasta el fin real.
- [ ] Un ayuno terminado antes se muestra con su hora y motivo, sin lenguaje de fracaso ni colores de juicio.
- [ ] Un ayuno terminado no se puede reanudar; para seguir se crea otro.
- [ ] Al terminar empieza la vuelta a comer si la plantilla la usa.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| hora real de fin | `fast.end_actual` | ✓ | ✓ |  |  |
| motivo obligatorio | `fast.end_reason` | ✓ | ✓ |  |  |
| texto de «otro» | `fast.end_reason_detail` | ✓ | ✓ |  |  |
| qué regla de alerta | `fast.end_rule_id` | ✓ | ✓ |  |  |
| duración desde el inicio real | `fast.clock_start_actual` |  | ✓ |  |  |
| terminado, no se reanuda | `fast.status` |  | ✓ | ✓ |  |
| la plantilla usa vuelta a comer | `stage.kind` |  | ✓ |  |  |
