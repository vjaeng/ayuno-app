## H-056: Definir una regla de tendencia

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando varias lecturas seguidas suben (o bajan) cada vez,
**para** ver que algo empeora poco a poco aunque ninguna lectura sola cruce un límite.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] La condición «tendencia» se cumple cuando N lecturas seguidas suben cada vez (o bajan cada vez); N es 3 o más, 3 de fábrica.
- [ ] Sirve para una medición numérica, para una cifra de la presión y para la diferencia al ponerme de pie (H-054).
- [ ] Con la diferencia de pulso al ponerme de pie +20, +25 y +43 en tres parejas seguidas, se cumple en la tercera.
- [ ] Una lectura igual a la anterior corta la racha.
- [ ] Con varias lecturas de pie por sentada (H-053), cada momento se compara con su igual: la de 1 minuto con las de 1 minuto, la de 3 minutos con las de 3 minutos y las «sin esperar» entre sí.
- [ ] Mientras la racha siga, la regla no vuelve a saltar con cada lectura nueva (como el resto de reglas).

*Decisión del reporte de impacto (EV-006, 3): cada momento de pie se compara con su igual.*

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas.

### Matriz CRUD (pendiente del mini-Tejido, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
