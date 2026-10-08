## H-007: Ajustar el catálogo de mediciones a lo que uso

**Como** persona que ayuna,
**quiero** un catálogo de mediciones precargado (glucosa, cetonas, presión arterial, peso, cintura, temperatura y escalas 0-10 de hambre, energía, ánimo y claridad mental) que pueda desactivar, editar y ampliar,
**para** medir solo lo que me interesa, con mis propias unidades y mediciones.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Todo tipo precargado se puede desactivar.
- [ ] Puedo añadir una medición propia con nombre, unidad, si es numérica y rango válido.
- [ ] Cada tipo define su unidad principal y, si aplica, una alternativa (mg/dL o mmol/L, kg o lb, cm o in, °C o °F).
- [ ] Desactivar un tipo no borra las mediciones ya anotadas.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre de la medición (precargada o propia) | `metric_type.name` | ✓ | ✓ | ✓ |  |
| unidad principal | `metric_type.unit` | ✓ | ✓ |  |  |
| unidad alternativa | `metric_type.alt_unit` | ✓ | ✓ |  |  |
| unidad por defecto al teclear | `metric_type.entry_unit` | ✓ |  |  |  |
| si es numérica | `metric_type.is_numeric` | ✓ | ✓ |  |  |
| rango válido (mínimo) | `metric_type.min_valid` | ✓ | ✓ | ✓ |  |
| rango válido (máximo) | `metric_type.max_valid` | ✓ | ✓ | ✓ |  |
| activar o desactivar el tipo | `metric_type.active` | ✓ | ✓ | ✓ |  |
| mediciones ya anotadas no se borran | `measurement.metric_type_code` |  | ✓ |  |  |
