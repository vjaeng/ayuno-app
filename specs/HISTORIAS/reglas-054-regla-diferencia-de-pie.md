## H-054: Definir una regla sobre la diferencia al ponerme de pie

**Como** persona que ayuna,
**quiero** crear reglas que se cumplan cuando mi pulso o mi presión cambian más de una cifra que yo elija entre la toma sentado y la de pie,
**para** que la app vea a tiempo lo que en mi ayuno fue el hallazgo importante: cuánto sube el pulso al levantarme, no la cifra suelta.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] La condición «diferencia al ponerme de pie» compara de pie − sentado del pulso, la de arriba o la de abajo, por encima o por debajo de una cifra que pongo yo; la regla nueva no trae cifra.
- [ ] Se evalúa al guardar cada lectura de pie enlazada a una sentada, con cada una por separado si hay varias (H-053).
- [ ] Con la regla «el pulso sube más de 30 al ponerme de pie», la pareja 83 → 126 la cumple y las parejas 60 → 80 y 61 → 86 no.
- [ ] Una lectura de pie sin sentada enlazada no la evalúa.
- [ ] Se combina con los filtros (H-024), el nivel y el mensaje (H-025) y las acciones (H-026): «si el pulso sube más de 30 → aviso y medir de nuevo en 30 minutos».

### Consideraciones de Seguridad
Regla crítica de seguridad: requiere pruebas automáticas del motor de reglas. Sin cifras clínicas precargadas (H-019).

### Matriz CRUD (pendiente del mini-Tejido, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
