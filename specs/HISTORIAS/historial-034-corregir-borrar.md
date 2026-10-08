## H-034: Corregir o borrar una entrada dejando rastro

**Como** persona que ayuna,
**quiero** editar o borrar cualquier entrada ya guardada y poder restaurar una borrada,
**para** corregir errores sin perder lo que había antes.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Al editar, la bitácora guarda el valor anterior, el nuevo, la hora y el motivo.
- [ ] La entrada editada conserva su identificador: en la exportación sustituye a la anterior, no se duplica.
- [ ] Una entrada borrada deja de verse en la línea de tiempo, queda en la bitácora y se puede restaurar.
- [ ] Las entradas borradas se exportan marcadas como borradas, no se omiten.

### Consideraciones de Seguridad
Lo crítico deja rastro (S8): nada se sobrescribe ni se borra sin rastro.

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
