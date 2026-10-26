## H-055: Definir una regla sobre el cambio respecto a mi valor de partida

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando una medición se aleja más de una cifra que yo elija del valor con que empecé el ayuno,
**para** vigilar cuánto cambio yo, y no solo si cruzo un límite fijo.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] La condición «cambio desde el valor de partida» sirve para cualquier medición numérica y para cada cifra de la presión, por encima o por debajo de una cifra que pongo yo; la regla nueva no trae cifra.
- [ ] El valor de partida es la primera lectura del ayuno de esa medición (y de esa posición, si la regla filtra por posición, H-024), salvo que yo marque otra lectura como «de partida».
- [ ] Puedo marcar como «de partida» una lectura de cada medición (y posición) por ayuno; marcar otra cambia la marca, y queda constancia en la bitácora.
- [ ] Con la regla «pulso sentado más de 15 por encima del de partida», partida 70 y una lectura sentado de 86, se cumple; con 84, no.
- [ ] Sin valor de partida, la regla no se cumple; la propia lectura de partida nunca la cumple.

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas. Cambiar la marca de partida altera qué se compara: deja rastro (S8).

### Matriz CRUD (pendiente del mini-Tejido, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
