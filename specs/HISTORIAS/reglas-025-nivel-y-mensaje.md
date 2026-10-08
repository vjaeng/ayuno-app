## H-025: Elegir el nivel y el mensaje de cada regla

**Como** persona que ayuna,
**quiero** elegir si cada regla es un aviso (amarillo) o una alerta (rojo) y escribir el mensaje que mostrará,
**para** recibir mis propias instrucciones, con mis palabras, en el momento en que las necesito.

**Prioridad:** alta · **Estimación:** S

### Criterios de Aceptación
- [ ] El aviso se muestra en amarillo y la alerta en rojo a pantalla completa.
- [ ] El mensaje se muestra tal cual lo escribí.
- [ ] El rojo solo se usa para alertas.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| aviso (amarillo) o alerta (rojo) | `rule.level` | ✓ | ✓ | ✓ |  |
| mensaje mostrado tal cual | `rule.message` | ✓ | ✓ | ✓ |  |
