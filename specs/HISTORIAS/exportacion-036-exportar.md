## H-036: Exportar un ayuno o todo a un archivo con formato fijo

**Como** persona que ayuna,
**quiero** exportar un ayuno, o todos mis ayunos y plantillas de una vez, a un archivo con el formato acordado y entregarlo por la hoja de compartir del teléfono,
**para** analizar mis datos en otro sistema sin pasarlos a mano y tener una copia completa de mi registro.

**Prioridad:** alta · **Estimación:** L

### Criterios de Aceptación
- [ ] El archivo de un ayuno se nombra con la fecha del inicio real del reloj y los primeros 8 caracteres del identificador del ayuno.
- [ ] «Exportar todo» incluye todos los ayunos, plantillas y catálogos con el mismo formato.
- [ ] Las horas van en hora local con segundos y sin zona; el desfase horario va una sola vez en la cabecera; nada se convierte a hora universal.
- [ ] Un dato no anotado sale vacío, nunca como cero ni como texto vacío.
- [ ] Los números salen como números, con punto decimal.
- [ ] Cada entrada lleva un identificador estable: reexportar el mismo ayuno da los mismos identificadores.
- [ ] Los catálogos y la plantilla usada viajan dentro del archivo.
- [ ] El archivo valida contra el esquema publicado.
- [ ] La app no sube nada por su cuenta.

### Consideraciones de Seguridad
Datos de salud (S4): la exportación es la única vía de salida y la decide el usuario.

### Matriz CRUD (tejida el 2026-10-08; re-tejida el 2026-10-23, EV-005)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| fecha del inicio real del reloj (nombre del archivo) | `fast.clock_start_actual` |  | ✓ |  |  |
| desfase horario en la cabecera | `fast.tz_offset` |  | ✓ |  |  |
| plantilla usada | `fast.template_id` |  | ✓ |  |  |
| ayuno | `fast.*` |  | ✓ |  |  |
| plantillas | `template.*` |  | ✓ |  |  |
| plan: etapas | `stage.*` |  | ✓ |  |  |
| plan: mediciones activas | `plan_metric.*` |  | ✓ |  |  |
| plan: horarios | `metric_schedule.*` |  | ✓ |  |  |
| plan: metas | `intake_goal.*` |  | ✓ |  |  |
| plan: lista previa | `checklist_item.*` |  | ✓ |  |  |
| plan: avisos | `stage_notice.*` |  | ✓ |  |  |
| plan: productos | `plan_product.*` |  | ✓ |  |  |
| plan: reglas | `rule.*` |  | ✓ |  |  |
| plan: vuelta a comer | `refeed_step.*` |  | ✓ |  |  |
| catálogo de mediciones | `metric_type.*` |  | ✓ |  |  |
| palabras de las mediciones (EV-005, formato 1.1) | `metric_option.*` |  | ✓ |  |  |
| catálogo de síntomas | `symptom_type.*` |  | ✓ |  |  |
| catálogo de ingestas | `intake_type.*` |  | ✓ |  |  |
| catálogo de productos | `product.*` |  | ✓ |  |  |
| horarios de productos | `product_schedule.*` |  | ✓ |  |  |
| mediciones | `measurement.*` |  | ✓ |  |  |
| presión | `blood_pressure_reading.*` |  | ✓ |  |  |
| síntomas | `symptom.*` |  | ✓ |  |  |
| ingestas | `intake.*` |  | ✓ |  |  |
| tomas | `product_log.*` |  | ✓ |  |  |
| alertas | `alert.*` |  | ✓ |  |  |
| pasos de vuelta a comer | `refeed_check.*` |  | ✓ |  |  |
| comidas | `meal.*` |  | ✓ |  |  |
| notas | `note.*` |  | ✓ |  |  |
| bitácora | `edit_log.*` |  | ✓ |  |  |
