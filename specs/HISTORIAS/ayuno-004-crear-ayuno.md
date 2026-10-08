## H-004: Crear un ayuno desde una plantilla con dos datos

**Como** persona que ayuna,
**quiero** crear un ayuno eligiendo una plantilla e indicando solo la fecha y hora de la última comida y la hora prevista de inicio del reloj, que cada hito guarde por separado su hora planificada y su hora real, y poder añadirle notas generales,
**para** planificar un ayuno en segundos y saber después cuánto me desvié del plan.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Con plantilla, última comida e inicio del reloj previstos, la app calcula fin previsto, fechas de las etapas y recordatorios.
- [ ] También puedo crear un ayuno desde cero, sin plantilla.
- [ ] El ayuno guarda una copia del plan de la plantilla: cambios posteriores a la plantilla no lo alteran.
- [ ] Un ayuno recién creado queda como planificado, no en curso.
- [ ] Cada hito (última comida, inicio del reloj, fin) guarda hora planificada y hora real por separado; registrar la real no borra la planificada.
- [ ] Si difieren, se muestran las dos juntas (p. ej. «previsto 22:00 · real 22:10»); un hito que no ha ocurrido muestra solo la planificada.
- [ ] Puedo escribir y editar unas notas generales del ayuno, sin hora, distintas de las notas libres con hora de la línea de tiempo.

### Consideraciones de Seguridad
Datos de salud (S4): se guardan solo en el teléfono y no salen de él salvo exportación explícita.

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
