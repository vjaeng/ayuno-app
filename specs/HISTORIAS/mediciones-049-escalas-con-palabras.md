## H-049: Elegir con palabras en las escalas de cómo me siento

**Como** persona que ayuna,
**quiero** anotar mi hambre, energía, ánimo y claridad mental tocando una palabra (por ejemplo «Mucha» o «Baja») en lugar de teclear un número, y poder cambiar esas palabras,
**para** registrar cómo me siento en un toque y con mis propias palabras, sin perder el número que usan las reglas y el historial.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Hambre, Energía, Ánimo y Claridad mental traen de fábrica cinco palabras cada una, con los números 0, 3, 5, 7 y 10:
  - Hambre: Nada · Poca · Moderada · Mucha · Intensa
  - Energía: Muy baja · Baja · Normal · Alta · Muy alta
  - Ánimo: Muy bajo · Bajo · Normal · Bueno · Muy bueno
  - Claridad mental: Nublada · Poca · Normal · Buena · Muy clara
- [ ] Al anotar una medición que tiene palabras, el formulario las muestra como botones grandes con su número, en lugar del teclado; un toque elige una y «Guardar» guarda su número. Sin palabra elegida, «Guardar» pide elegir una o marcar «No se hizo».
- [ ] Hora, «Al despertar», nota y «No se hizo» funcionan igual que en cualquier medición.
- [ ] En Más › Catálogos puedo cambiar el texto y el número de cada palabra, añadir, quitar y ordenar palabras (hasta 9), y dar palabras a una medición numérica propia; el número debe estar dentro del rango válido de la medición y no se repiten textos ni números.
- [ ] Donde se ve una medición con palabras (Inicio, Hoy, Mediciones de este ayuno, Historial, alerta) aparece la palabra con su número («Mucha · 7»); si el número guardado no coincide con ninguna palabra (por ejemplo, uno tecleado antes de este cambio), se ve el número con su unidad, como hasta ahora («4 de 0 a 10»).
- [ ] Cambiar o quitar una palabra no altera ninguna medición ya anotada.
- [ ] Corregir una medición con palabras (H-034, H-048) usa los mismos botones.
- [ ] Las reglas siguen comparando el número: una regla «Hambre por encima de 6» salta al elegir «Mucha» (7).
- [ ] Al actualizar la app, las mediciones ya anotadas (también las de un ayuno en curso) quedan intactas y las palabras de fábrica aparecen solas.

### Consideraciones de Seguridad
Las palabras son catálogo (Público); lo anotado sigue siendo dato de salud (Crítico, S4). Textos y números de las palabras se validan al guardar y al importar (S1). La actualización de la base no borra ni cambia filas existentes y, si falla, deja la base como estaba (S6).

### Matriz CRUD (tejida el 2026-10-23, EV-005)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| medición a la que pertenece cada palabra | `metric_option.metric_type_code` | ✓ | ✓ |  |  |
| texto de la palabra (de fábrica, editable, sin repetir) | `metric_option.label` | ✓ | ✓ | ✓ |  |
| número de la palabra (dentro del rango, sin repetir) | `metric_option.value` | ✓ | ✓ | ✓ |  |
| orden de las palabras | `metric_option.position` | ✓ | ✓ | ✓ |  |
| quitar una palabra (hasta 9 visibles) | `metric_option.deleted_at` (control) |  |  |  | ✓ |
| solo mediciones numéricas pueden tener palabras | `metric_type.is_numeric` |  | ✓ |  |  |
| rango válido de la medición | `metric_type.min_valid`, `metric_type.max_valid` |  | ✓ |  |  |
| nombre de la medición (título del formulario y de Catálogos) | `metric_type.name` |  | ✓ |  |  |
| unidad con que se guarda el número | `metric_type.entry_unit` |  | ✓ |  |  |
| tipo de la medición anotada | `measurement.metric_type_code` | ✓ | ✓ |  |  |
| número de la palabra elegida, guardado | `measurement.value_entered`, `measurement.value_num` | ✓ | ✓ | ✓ |  |
| unidad guardada | `measurement.unit_entered` | ✓ | ✓ | ✓ |  |
| hora, «Al despertar», nota y «No se hizo» como en H-008 | `measurement.taken_at`, `measurement.on_waking`, `measurement.note`, `measurement.not_done_reason` | ✓ |  |  |  |
| palabra mostrada al leer (o número y unidad si no coincide) | derivado «Palabra de una medición» (MODELO §2.5) |  | ✓ |  |  |
| corregir con los mismos botones (H-034) | `measurement.value_entered`, `measurement.value_num` + `edit_log.*` (vía H-034) |  |  | ✓ |  |
| reglas comparan el número | `rule.threshold` (vía H-019) sobre `measurement.value_num` |  | ✓ |  |  |
