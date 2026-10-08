## H-029: Llamar a mi contacto de emergencia con un toque

**Como** persona que ayuna,
**quiero** configurar, si quiero, un contacto de emergencia y llamarlo con un toque desde la pantalla de seguridad,
**para** pedir ayuda rápido aunque esté confuso.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] El contacto es opcional; guardo nombre y teléfono.
- [ ] Un toque inicia la llamada desde la pantalla de seguridad.
- [ ] Sin contacto configurado, el botón invita a configurarlo.

### Consideraciones de Seguridad
Dato personal de un tercero (S4): solo en el teléfono, no se exporta.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| nombre del contacto | `emergency_contact.name` | ✓ | ✓ | ✓ | ✓ |
| teléfono | `emergency_contact.phone` | ✓ | ✓ | ✓ | ✓ |
