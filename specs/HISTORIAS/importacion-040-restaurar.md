## H-040: Restaurar todo desde un archivo exportado

**Como** persona que ayuna,
**quiero** importar un archivo exportado por la propia app para reconstruir mis ayunos y plantillas,
**para** recuperar todo tras cambiar de teléfono o reinstalar.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Exportar, desinstalar, reinstalar e importar restaura todos los ayunos y plantillas con las horas al segundo.
- [ ] Importar el mismo archivo dos veces no duplica entradas.
- [ ] Un archivo con campos desconocidos se importa igual y la app lista los que ignoró.
- [ ] Un archivo con una versión de formato mayor que la que la app entiende se rechaza con un mensaje claro.

### Consideraciones de Seguridad
Toda entrada es hostil hasta validarse (S1); fallar cerrado (S6) ante versión incompatible.

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-23, EV-005)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| ayunos restaurados | `fast.*` | ✓ |  |  |  |
| plantillas restauradas | `template.*` | ✓ |  |  |  |
| entradas restauradas con las horas al segundo | `measurement.*` | ✓ |  |  |  |
| palabras de las mediciones restauradas (EV-005; un archivo 1.0 no las trae) | `metric_option.*` | ✓ |  | ✓ |  |
