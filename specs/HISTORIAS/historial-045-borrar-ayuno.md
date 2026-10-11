## H-045: Borrar un ayuno para siempre con todos sus datos

**Como** persona que ayuna,
**quiero** borrar un ayuno con todo lo que anoté en él, de forma definitiva,
**para** quitar de mi teléfono un ayuno creado por error o unos datos de salud que no quiero conservar.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Puedo borrar un ayuno planificado, en curso o terminado.
- [ ] Antes de borrar, la app me pide confirmación y me dice qué se va a borrar (el ayuno y cuántos registros tiene), que no se puede deshacer y que los archivos que ya exporté no cambian.
- [ ] Al confirmar se borran el ayuno, todos sus registros (mediciones, presión, síntomas, ingestas, tomas, alertas, notas y comidas), su plan, las casillas de la vuelta a comer y los cambios guardados en la bitácora sobre todo ello; no queda constancia del ayuno en el teléfono.
- [ ] Si el ayuno estaba en curso, sus recordatorios y notificaciones se cancelan y deja de haber un ayuno en curso.
- [ ] La plantilla de la que salió, los catálogos y los demás ayunos no cambian.
- [ ] El ayuno borrado deja de verse en Inicio, Historial y Hoy, y una exportación posterior ya no lo contiene.
- [ ] Si algo falla a mitad del borrado, no se borra nada y el ayuno queda como estaba.

### Consideraciones de Seguridad
Excepción deliberada a S8 (lo crítico deja rastro): ADR-SEG-007. Acción destructiva irreversible con confirmación explícita; borrado todo o nada; nada del contenido en los registros de la app (S4).

### Matriz CRUD (tejida el 2026-10-10, EV-001)
*Usa comodines `entidad.*` (convención del README): el borrado alcanza todas las columnas.*

| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| ayuno en cualquier estado; en curso → cancelar recordatorios | `fast.status` |  | ✓ |  |  |
| fechas del ayuno en la confirmación | `fast.clock_start_planned`, `fast.clock_start_actual`, `fast.end_actual` |  | ✓ |  |  |
| el ayuno | `fast.*` |  |  |  | ✓ |
| registros: mediciones (y recuento) | `measurement.*` |  | ✓ |  | ✓ |
| registros: presión | `blood_pressure_reading.*` |  | ✓ |  | ✓ |
| registros: síntomas | `symptom.*` |  | ✓ |  | ✓ |
| registros: ingestas | `intake.*` |  | ✓ |  | ✓ |
| registros: tomas | `product_log.*` |  | ✓ |  | ✓ |
| registros: alertas | `alert.*` |  | ✓ |  | ✓ |
| registros: notas | `note.*` |  | ✓ |  | ✓ |
| registros: comidas | `meal.*` |  | ✓ |  | ✓ |
| su plan: etapas | `stage.*` |  |  |  | ✓ |
| su plan: mediciones | `plan_metric.*` |  |  |  | ✓ |
| su plan: horarios | `metric_schedule.*` |  |  |  | ✓ |
| su plan: metas | `intake_goal.*` |  |  |  | ✓ |
| su plan: lista previa | `checklist_item.*` |  |  |  | ✓ |
| su plan: avisos | `stage_notice.*` |  |  |  | ✓ |
| su plan: productos | `plan_product.*` |  |  |  | ✓ |
| su plan: reglas | `rule.*` |  |  |  | ✓ |
| su plan: vuelta a comer | `refeed_step.*` |  |  |  | ✓ |
| casillas de la vuelta a comer | `refeed_check.*` |  |  |  | ✓ |
| cambios guardados en la bitácora sobre todo ello | `edit_log.*` |  |  |  | ✓ |
