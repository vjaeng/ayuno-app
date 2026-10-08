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

### Consideraciones de Seguridad
Datos de salud (S4); secretos fuera del código (S3); fallar cerrado y sin filtrar (S6).

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| bloqueo opcional con huella o PIN | `settings.app_lock_enabled` | ✓ | ✓ | ✓ |  |
