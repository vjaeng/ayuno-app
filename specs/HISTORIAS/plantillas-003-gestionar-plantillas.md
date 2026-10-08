## H-003: Duplicar, borrar y partir de una plantilla de ejemplo

**Como** persona que ayuna,
**quiero** duplicar una plantilla para hacer una variante, borrar las que ya no use y contar con una plantilla de ejemplo sin valores clínicos,
**para** probar variaciones de mi plan sin rehacerlo y entender la estructura sin que la app me imponga umbrales ni dosis.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] Duplicar crea una plantilla independiente con todo el contenido de la original.
- [ ] Borrar una plantilla no borra los ayunos creados desde ella, que conservan el plan con que se crearon; la plantilla borrada se puede restaurar.
- [ ] Al instalar existe una plantilla de ejemplo con etapas, mediciones y estructura de reglas, sin umbrales de alerta ni dosis de productos.
- [ ] La plantilla de ejemplo se puede duplicar, editar o borrar como cualquier otra.

### Consideraciones de Seguridad
La app no aconseja: ningún valor clínico precargado.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| duplicar la plantilla con todo su contenido | `template.name` | ✓ | ✓ |  |  |
| duración de la copia | `template.target_hours` | ✓ |  |  |  |
| hitos de la copia | `template.milestone_hours` | ✓ |  |  |  |
| borrar y restaurar la plantilla | `template.name` |  |  |  | ✓ |
| plantilla de ejemplo con etapas | `stage.kind` | ✓ |  |  |  |
| plantilla de ejemplo con mediciones | `plan_metric.metric_type_code` | ✓ |  |  |  |
| plantilla de ejemplo con estructura de reglas, sin umbrales | `rule.condition_kind` | ✓ |  |  |  |
| ayunos creados desde la plantilla no cambian | `fast.template_id` |  | ✓ |  |  |
