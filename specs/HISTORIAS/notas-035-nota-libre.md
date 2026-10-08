## H-035: Escribir una nota libre

**Como** persona que ayuna,
**quiero** escribir una nota libre con hora en cualquier momento,
**para** registrar lo que no cabe en una medición ni en un síntoma.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] «Nota» está en la rejilla de «Anotar».
- [ ] La nota guarda fecha y hora (por defecto ahora, editable).
- [ ] La nota aparece en la línea de tiempo.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| fecha y hora de la nota | `note.noted_at` | ✓ |  | ✓ |  |
| texto libre | `note.text` | ✓ |  |  |  |
