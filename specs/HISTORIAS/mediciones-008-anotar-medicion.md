## H-008: Anotar una medición en tres toques

**Como** persona que ayuna,
**quiero** abrir desde «Anotar» una rejilla de botones grandes con mis mediciones activas y guardar cada una en un formulario de una sola pantalla, con la unidad en que la leo,
**para** registrar aunque esté en ayunas, cansado y con una mano, sin perder la lectura original de mi aparato.

**Prioridad:** alta · **Estimación:** L

### Criterios de Aceptación
- [ ] Desde la pantalla principal llego a guardar una medición en 3 toques o menos y en menos de 30 segundos.
- [ ] El formulario usa teclado numérico para valores numéricos.
- [ ] La fecha y hora es «ahora» por defecto, con segundos, y se puede editar; cada medición admite nota opcional.
- [ ] La glucosa indica su origen (sensor continuo o glucómetro), admite flecha de tendencia opcional y se puede anotar en mg/dL o mmol/L.
- [ ] Un peso tecleado en lb se guarda en lb y también en kg; se muestran los dos y el tecleado nunca se sustituye por el convertido.
- [ ] Un valor fuera del rango válido pide confirmación; si confirmo, se guarda tal cual, sin redondear ni corregir.
- [ ] Puedo marcar una medición (incluida la presión) como tomada al despertar; la app lo propone en la primera lectura tras el fin del silencio nocturno.
- [ ] La entrada queda guardada en el teléfono antes de confirmarse en pantalla.

### Consideraciones de Seguridad
Datos de salud (S4): se guardan solo en el teléfono y no salen de él salvo exportación explícita. Toda entrada es hostil hasta validarse (S1).

### Matriz CRUD (tejida el 2026-10-08)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| mediciones activas en la rejilla | `metric_type.active` |  | ✓ |  |  |
| orden de la rejilla (las del plan primero) | `plan_metric.metric_type_code` |  | ✓ |  |  |
| tipo de medición | `measurement.metric_type_code` | ✓ |  |  |  |
| teclado numérico para valores numéricos | `metric_type.is_numeric` |  | ✓ |  |  |
| fecha y hora, por defecto ahora, con segundos | `measurement.taken_at` | ✓ |  | ✓ |  |
| valor tecleado | `measurement.value_entered` | ✓ |  |  |  |
| unidad en que se tecleó (mg/dL o mmol/L, lb…) | `measurement.unit_entered` | ✓ |  |  |  |
| valor equivalente en la unidad del catálogo | `measurement.value_num` | ✓ |  |  |  |
| valor de tipos no numéricos | `measurement.value_text` | ✓ |  |  |  |
| unidad del catálogo y alternativa | `metric_type.unit` |  | ✓ |  |  |
| unidad alternativa ofrecida | `metric_type.alt_unit` |  | ✓ |  |  |
| unidad por defecto del formulario | `metric_type.entry_unit` |  | ✓ |  |  |
| origen de la glucosa | `measurement.source` | ✓ |  |  |  |
| flecha de tendencia opcional | `measurement.trend` | ✓ |  |  |  |
| nota opcional | `measurement.note` | ✓ |  |  |  |
| nota opcional (presión) | `blood_pressure_reading.note` | ✓ |  |  |  |
| medición tomada al despertar | `measurement.on_waking` | ✓ |  |  |  |
| presión tomada al despertar | `blood_pressure_reading.on_waking` | ✓ |  |  |  |
| propuesta tras el fin del silencio nocturno | `settings.quiet_end` |  | ✓ |  |  |
| rango válido (confirmación) | `metric_type.min_valid` |  | ✓ |  |  |
| rango válido (confirmación) | `metric_type.max_valid` |  | ✓ |  |  |
