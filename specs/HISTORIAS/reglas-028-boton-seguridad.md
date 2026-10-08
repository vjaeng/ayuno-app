## H-028: Tener mis reglas siempre a un toque

**Como** persona que ayuna,
**quiero** un botón de seguridad siempre visible que abra mis reglas activas en lenguaje llano y me permita terminar el ayuno,
**para** decidir con la regla escrita a la vista.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] El botón de seguridad está visible en la pantalla principal durante todo el ayuno.
- [ ] La pantalla lista cada regla activa como una frase completa (p. ej. «Si la glucosa queda bajo 55 durante 20 minutos: alerta roja»), sin siglas.
- [ ] Desde ella puedo terminar el ayuno y tocar «algo no va bien».

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| reglas activas | `rule.active` |  | ✓ |  |  |
| nombre | `rule.name` |  | ✓ |  |  |
| condición | `rule.condition_kind` |  | ✓ |  |  |
| medición | `rule.metric_type_code` |  | ✓ |  |  |
| valor de la presión | `rule.bp_component` |  | ✓ |  |  |
| umbral | `rule.threshold` |  | ✓ |  |  |
| minutos sostenida | `rule.sustained_minutes` |  | ✓ |  |  |
| síntoma | `rule.symptom_type_code` |  | ✓ |  |  |
| ingesta | `rule.intake_type_code` |  | ✓ |  |  |
| hora de comprobación | `rule.check_time` |  | ✓ |  |  |
| filtros | `rule.filter_stage_kinds` |  | ✓ |  |  |
| filtro al despertar | `rule.filter_on_waking` |  | ✓ |  |  |
| filtro de posición | `rule.filter_position` |  | ✓ |  |  |
| nivel (alerta roja) | `rule.level` |  | ✓ |  |  |
| acción | `rule.action` |  | ✓ |  |  |
