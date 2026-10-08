## H-009: Anotar la presión sentado y de pie

**Como** persona que ayuna,
**quiero** anotar sistólica, diastólica, pulso y posición (sentado, de pie o acostado) y que, tras la toma sentado, la app me ofrezca la de pie y muestre la diferencia,
**para** ver cuánto cambia mi presión al ponerme de pie.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada toma registra sistólica, diastólica, pulso y posición.
- [ ] Al guardar la toma sentado, la app ofrece anotar la de pie.
- [ ] La toma sentado y la de pie quedan vinculadas y la app muestra la diferencia calculada, no tecleada.
- [ ] Puedo guardar solo la toma sentado.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| sistólica | `blood_pressure_reading.systolic` | ✓ | ✓ |  |  |
| diastólica | `blood_pressure_reading.diastolic` | ✓ | ✓ |  |  |
| pulso | `blood_pressure_reading.pulse` | ✓ | ✓ |  |  |
| posición (sentado, de pie, acostado) | `blood_pressure_reading.position` | ✓ | ✓ |  |  |
| hora de la toma | `blood_pressure_reading.taken_at` | ✓ |  |  |  |
| toma de pie vinculada a la sentada | `blood_pressure_reading.paired_reading_id` | ✓ | ✓ |  |  |
