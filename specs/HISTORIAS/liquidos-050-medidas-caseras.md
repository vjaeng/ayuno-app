## H-050: Anotar lo que tomo en mis medidas caseras

**Como** persona que ayuna,
**quiero** anotar el agua y la sal en las medidas que uso en casa (un vaso, una taza, una cucharadita, una pizca), cada una con la equivalencia que yo le declare,
**para** registrar lo que tomo tal como lo mido, sin convertir a mililitros ni gramos de cabeza, y que la app sume bien.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada tipo de ingesta puede tener medidas caseras propias, cada una con nombre, equivalencia en la unidad del tipo y marca «imprecisa»; las cambio, añado, quito y ordeno en Más › Catálogos, en el editor de cada líquido.
- [ ] De fábrica: «vaso» = 250 mL y «taza» = 240 mL (agua e infusión); «cucharadita rasa» = 5 g, «¼ de cucharadita» = 1.25 g y «pizca» = 0.3 g, imprecisa (sal). Son equivalencias de cocina, editables, no dosis.
- [ ] Al anotar una ingesta elijo la unidad del tipo o una de sus medidas caseras; se guarda lo que tecleé con su medida («1 pizca») **y** su equivalente en la unidad del tipo (0.3 g).
- [ ] Totales del día, sodio, metas y reglas usan el equivalente: «1 pizca» de sal sube el total del día 0.3 g y el sodio 0.12 g.
- [ ] Con una medida imprecisa elegida, bajo la cantidad aparece una línea en texto llano que la traduce, sin juzgar: «Una pizca son unos 0.3 g. Tu meta de hoy equivale a unas 8 pizcas o media cucharadita.» (la segunda frase solo si el tipo tiene meta).
- [ ] Cambiar la equivalencia de una medida, o quitarla, no cambia ninguna ingesta ya anotada.
- [ ] La exportación lleva lo tecleado, su medida y el equivalente (`1`, `pinch`, `0.3`, `g`) y el catálogo de medidas.

*Decisiones del reporte de impacto (EV-006): la línea de la medida imprecisa es fija mientras está elegida, no un aviso de una vez (5); el código de la medida va en inglés (`pinch`) y el nombre visible en español (6).*

### Consideraciones de Seguridad
Las medidas son catálogo (Público); lo anotado es dato de salud (Crítico, S4). Equivalencias > 0, validadas al guardar y al importar (S1). Ninguna equivalencia de fábrica es una dosis ni un umbral clínico.

### Matriz CRUD (tejida el 2026-10-26, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| medidas caseras de cada líquido | `household_unit.intake_type_code` | ✓ | ✓ |  |  |
| nombre de la medida («pizca», «vaso») | `household_unit.name` | ✓ | ✓ | ✓ |  |
| equivalencia en la unidad del tipo (> 0) | `household_unit.equivalent` | ✓ | ✓ | ✓ |  |
| marca «imprecisa» | `household_unit.imprecise` | ✓ | ✓ | ✓ |  |
| orden de las medidas | `household_unit.position` | ✓ | ✓ | ✓ |  |
| quitar una medida | `household_unit.deleted_at` (control) |  |  |  | ✓ |
| medidas de fábrica (código en inglés: `pinch`, `glass`) | `household_unit.code` (control) | ✓ | ✓ |  |  |
| unidad del tipo | `intake_type.unit` |  | ✓ |  |  |
| lo tecleado y su medida («1 pizca») | `intake.amount_entered`, `intake.unit_entered` | ✓ | ✓ |  |  |
| equivalente guardado (0.3 g) | `intake.amount`, `intake.unit` | ✓ | ✓ |  |  |
| sodio del equivalente (0.12 g) | `intake_type.sodium_factor` |  | ✓ |  |  |
| totales, metas y reglas usan el equivalente | derivado «Totales del día» sobre `intake.amount`; `intake_goal.daily_amount` |  | ✓ |  |  |
| línea de la medida imprecisa («unas 8 pizcas») | derivado «Equivalente casero» (MODELO §2.5) con `intake_goal.daily_amount` |  | ✓ |  |  |
| cambiar o quitar la medida no toca lo anotado | `intake.amount` (no se reescribe, D-22) |  | ✓ |  |  |
| exportación con lo tecleado, la medida y el catálogo | `intake.*`, `household_unit.*` (vía H-036) |  | ✓ |  |  |
