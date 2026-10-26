## H-009: Anotar la presión sentado y de pie

**Como** persona que ayuna,
**quiero** anotar sistólica, diastólica, pulso y posición (sentado, de pie o acostado) y que, tras la toma sentado, la app me ofrezca la de pie y muestre la diferencia,
**para** ver cuánto cambia mi presión al ponerme de pie.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Cada toma registra sistólica, diastólica, pulso y posición.
- [ ] Al guardar la toma sentado, la app ofrece anotar la de pie, con el temporizador de H-053 o sin esperar (EV-006).
- [ ] La toma sentado y la de pie quedan vinculadas y la app muestra la diferencia calculada, no tecleada, de las tres cifras (arriba, abajo y pulso) (EV-006).
- [ ] Una toma sentado puede tener varias de pie vinculadas (por ejemplo, a 1 y a 3 minutos), cada una con su diferencia (EV-006).
- [ ] Puedo guardar solo la toma sentado.

### Consideraciones de Seguridad
(ninguna evidente)

### Matriz CRUD (tejida el 2026-10-08; **re-tejer: EV-006**)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| sistólica | `blood_pressure_reading.systolic` | ✓ | ✓ |  |  |
| diastólica | `blood_pressure_reading.diastolic` | ✓ | ✓ |  |  |
| pulso | `blood_pressure_reading.pulse` | ✓ | ✓ |  |  |
| posición (sentado, de pie, acostado) | `blood_pressure_reading.position` | ✓ | ✓ |  |  |
| hora de la toma | `blood_pressure_reading.taken_at` | ✓ |  |  |  |
| toma de pie vinculada a la sentada | `blood_pressure_reading.paired_reading_id` | ✓ | ✓ |  |  |
