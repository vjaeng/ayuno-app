## H-039: Importar plantillas y mi lista de productos

**Como** persona que ayuna,
**quiero** importar plantillas y listas de productos desde un archivo,
**para** compartir plantillas entre teléfonos y no teclear producto a producto.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] Las plantillas importadas aparecen en la lista de plantillas; importar la misma dos veces no la duplica.
- [ ] Los códigos de producto se conservan exactamente; un producto con el mismo código se actualiza, no se duplica.
- [ ] Un archivo inválido se rechaza con un mensaje claro y no modifica nada.

### Consideraciones de Seguridad
Toda entrada es hostil hasta validarse (S1).

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| plantillas importadas | `template.*` | ✓ | ✓ |  |  |
| códigos de producto conservados | `product.code` | ✓ | ✓ |  |  |
| producto con el mismo código se actualiza | `product.*` | ✓ |  | ✓ |  |
