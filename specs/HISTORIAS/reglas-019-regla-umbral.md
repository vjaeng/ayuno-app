## H-019: Definir una regla de alerta por umbral

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando una medición quede por debajo o por encima de un valor que yo elija,
**para** que la app aplique mis propios criterios de alerta sin tener que recordarlos.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Una regla tiene nombre, condición, nivel, mensaje y acción, y se puede activar o desactivar.
- [ ] La condición «medición por debajo de X» o «por encima de X» se cumple con una sola lectura.
- [ ] Las reglas se evalúan al guardar cada entrada y al llegar cada hora programada.
- [ ] Las reglas pertenecen a la plantilla y se copian al ayuno.
- [ ] La app no trae umbrales clínicos precargados.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
