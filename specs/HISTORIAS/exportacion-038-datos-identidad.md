## H-038: Decidir si el archivo lleva mis datos personales

**Como** persona que ayuna,
**quiero** activar en ajustes si el archivo exportado incluye mi nombre, documento de identidad y fecha de nacimiento,
**para** que quien reciba el archivo pueda verificar que los datos son míos solo si yo lo autorizo.

**Prioridad:** media · **Estimación:** S

### Criterios de Aceptación
- [ ] Por defecto el bloque de datos personales sale vacío.
- [ ] Al activarlo, se incluyen los datos que haya guardado en ajustes.

### Consideraciones de Seguridad
Minimización de datos personales (S4): opt-in explícito.

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| incluir datos personales en el archivo | `settings.export_person_enabled` | ✓ | ✓ | ✓ |  |
| nombre | `person_profile.full_name` | ✓ | ✓ | ✓ |  |
| documento de identidad | `person_profile.id_number` | ✓ | ✓ | ✓ |  |
| fecha de nacimiento | `person_profile.birth_date` | ✓ | ✓ | ✓ |  |
