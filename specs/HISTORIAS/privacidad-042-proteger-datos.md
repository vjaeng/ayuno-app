## H-042: Proteger mis datos en el teléfono y en copias

**Como** persona que ayuna,
**quiero** que los datos estén cifrados en el teléfono, poder activar un bloqueo con huella o PIN y guardar manualmente una copia de seguridad cifrada,
**para** que nadie con acceso a mi teléfono lea mis datos de salud y no perder mi registro si pierdo el teléfono.

**Prioridad:** media · **Estimación:** M

### Criterios de Aceptación
- [ ] Los datos guardados en el teléfono están cifrados.
- [ ] El bloqueo es opcional; activado, la app pide huella o PIN al abrirse, y los recordatorios y alertas siguen funcionando.
- [ ] La copia de seguridad se protege con una contraseña que elijo.
- [ ] Restaurar la copia sin la contraseña correcta falla sin revelar contenido.
- [ ] Puedo hacer capturas de pantalla de cualquier pantalla de la app, y la vista de aplicaciones recientes muestra su contenido (EV-005).

### Consideraciones de Seguridad
Datos de salud (S4); secretos fuera del código (S3); fallar cerrado y sin filtrar (S6). Riesgo aceptado por la persona (EV-005, ADR-SEG-002): una captura sale del cifrado de la app, queda en la galería y puede copiarse a la nube por el sistema; la vista de recientes muestra datos de salud a quien tenga el teléfono, también con el bloqueo activo.

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-23, EV-005)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| bloqueo opcional con huella o PIN | `settings.app_lock_enabled` | ✓ | ✓ | ✓ |  |

*Capturas de pantalla (EV-005):* criterio de comportamiento de la ventana, sin campo de datos: fijo en el código y sin ajuste (decisión del humano, ADR-SEG-002 enmendado).
