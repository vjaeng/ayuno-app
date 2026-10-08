## H-037: Exportar también en tablas

**Como** persona que ayuna,
**quiero** exportar opcionalmente un archivo comprimido con una tabla por tipo de dato,
**para** abrir mis datos en una hoja de cálculo.

**Prioridad:** baja · **Estimación:** S

### Criterios de Aceptación
- [ ] Cada tabla lleva las mismas columnas que su equivalente en el archivo principal.
- [ ] Los datos no anotados salen vacíos, nunca como cero.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| mismas columnas que el archivo principal | `fast.*` |  | ✓ |  |  |
| tablas de registros | `measurement.*` |  | ✓ |  |  |
