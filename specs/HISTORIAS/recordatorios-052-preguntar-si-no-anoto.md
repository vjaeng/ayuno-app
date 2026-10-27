## H-052: Que la app me pregunte si llevo horas sin anotar lo que pide mi plan

**Como** persona que ayuna,
**quiero** que, si mi plan tiene una meta de sal o de agua y llevo varias horas despierto sin anotar nada de ese tipo, la app me pregunte una vez si lo he tomado,
**para** que un registro vacío no signifique «nadie preguntó» y yo me dé cuenta a tiempo de que voy corto.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada meta de ingesta puede tener «preguntar tras N horas sin anotar»; vacío = no pregunta. Las metas de la plantilla de ejemplo traen 6 horas.
- [ ] Con el ayuno en curso, si pasan N horas despierto sin ninguna ingesta de ese tipo, llega una notificación: «¿Has tomado sal desde las 07:00?» con «Sí, anotar» y «Todavía no». La hora es la de la última ingesta de ese tipo o, si no hay, la del inicio del reloj.
- [ ] Las horas del silencio nocturno no cuentan: con 6 horas y silencio de 23:00 a 06:30, una última ingesta a las 21:00 pregunta a las 10:30.
- [ ] «Sí, anotar» abre el formulario de ese tipo; «Todavía no» cierra la pregunta.
- [ ] Pregunta una sola vez por cada hueco: no vuelve a preguntar hasta que haya una ingesta nueva de ese tipo y pasen otra vez N horas despierto.
- [ ] La pregunta suena aunque la app esté cerrada y tras reiniciar el teléfono, como cualquier recordatorio (H-016), y aparece también en Hoy.
- [ ] Sin silencio nocturno configurado, cuentan las 24 horas.

*Decisiones del reporte de impacto (EV-006): las horas de sueño son las del silencio nocturno de Ajustes (1); las horas son configurables por meta (2).*

### Consideraciones de Seguridad
La notificación nombra el tipo («sal»), nunca cantidades ni lecturas (S4). No aconseja: pregunta y ofrece anotar.

### Matriz CRUD (tejida el 2026-10-26, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| «preguntar tras N horas sin anotar» de la meta | `intake_goal.nudge_after_hours` |  | ✓ |  |  |
| tipo de la meta («sal») | `intake_goal.intake_type_code`, `intake_type.name` |  | ✓ |  |  |
| hora de la última ingesta de ese tipo | `intake.taken_at`, `intake.intake_type_code` (no borradas) |  | ✓ |  |  |
| o, si no hay, la del inicio del reloj | `fast.clock_start_actual` |  | ✓ |  |  |
| con el ayuno en curso | `fast.status` |  | ✓ |  |  |
| horas del silencio nocturno no cuentan | `settings.quiet_start`, `settings.quiet_end` |  | ✓ |  |  |
| cuándo pregunta y que no repite el hueco | derivado «Próxima pregunta por tipo» (MODELO §2.5, D-23) |  | ✓ |  |  |
| «Sí, anotar» abre el formulario | `intake.*` (vía H-012) | ✓ |  |  |  |
| fila en Hoy | derivado «Pendientes del día» |  | ✓ |  |  |
