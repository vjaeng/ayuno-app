## H-048: Ver y corregir las mediciones del ayuno en curso

**Como** persona que ayuna,
**quiero** ver desde Inicio, en un toque, todas las mediciones que llevo anotadas en el ayuno en curso y abrir cualquiera para corregirla o borrarla,
**para** repasar lo que he medido y arreglar un error de dedo sin esperar a que termine el ayuno.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Durante el ayuno, Inicio muestra «Mediciones de este ayuno» con el número de mediciones anotadas; un toque abre la lista.
- [ ] La lista muestra todas las mediciones y presiones del ayuno, agrupadas por día (fecha con día de la semana), de la más reciente a la más antigua; cada fila con su hora real, la hora del ayuno, el tipo y el valor tecleado con su unidad.
- [ ] Las marcadas «no se hizo» aparecen con su motivo; las corregidas llevan la marca «corregida».
- [ ] Puedo filtrar la lista por tipo de medición.
- [ ] Tocar una fila abre su formulario con los valores actuales, donde puedo corregirla o borrarla (H-034); las borradas se ven en «Borradas» y se pueden restaurar.
- [ ] Sin mediciones, la lista lo dice y ofrece «Anotar».
- [ ] Desde el detalle de un ayuno terminado (Historial) se corrige igual, con las mismas pantallas.

### Consideraciones de Seguridad
Mediciones y presión son datos de salud (Crítico): toda corrección y borrado deja rastro (S8) y pasa por las mismas validaciones que al anotar (S1); ni valores ni motivos en registros de depuración (S4).

### Matriz CRUD (tejida el 2026-10-14, EV-004)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| ayuno en curso y desde cuándo | `fast.status`, `fast.clock_start_actual` |  | ✓ |  |  |
| mediciones del ayuno (lista y número) | `measurement.fast_id` (control) |  | ✓ |  |  |
| tipo de medición (fila y filtro) | `measurement.metric_type_code` |  | ✓ |  |  |
| nombre del tipo | `metric_type.name` |  | ✓ |  |  |
| hora real; agrupación por día; hora del ayuno | `measurement.taken_at` |  | ✓ | ✓ |  |
| valor tecleado | `measurement.value_entered` |  | ✓ | ✓ |  |
| unidad tecleada | `measurement.unit_entered` |  | ✓ | ✓ |  |
| valor equivalente (recalculado al corregir) | `measurement.value_num` |  |  | ✓ |  |
| valor de tipos no numéricos | `measurement.value_text` |  | ✓ | ✓ |  |
| origen y tendencia de la glucosa | `measurement.source`, `measurement.trend` |  | ✓ | ✓ |  |
| al despertar | `measurement.on_waking` |  | ✓ | ✓ |  |
| nota | `measurement.note` |  |  | ✓ |  |
| no se hizo con su motivo | `measurement.not_done_reason` |  | ✓ | ✓ |  |
| rango válido al corregir | `metric_type.min_valid`, `metric_type.max_valid` |  | ✓ |  |  |
| unidades ofrecidas al corregir | `metric_type.unit`, `metric_type.alt_unit` |  | ✓ |  |  |
| presión del ayuno: hora | `blood_pressure_reading.taken_at` |  | ✓ | ✓ |  |
| presión: valores | `blood_pressure_reading.systolic`, `.diastolic`, `.pulse` |  | ✓ | ✓ |  |
| presión: posición y al despertar | `blood_pressure_reading.position`, `.on_waking` |  | ✓ | ✓ |  |
| presión: nota | `blood_pressure_reading.note` |  |  | ✓ |  |
| presión no hecha | `blood_pressure_reading.not_done_reason` |  | ✓ | ✓ |  |
| borrar (y verla en «Borradas») | `measurement.deleted_at`, `blood_pressure_reading.deleted_at` (control) |  | ✓ |  | ✓ |
| marca «corregida» | `edit_log.row_id`, `edit_log.action` |  | ✓ |  |  |
| rastro de corrección y borrado (vía H-034) | `edit_log.*` | ✓ |  |  |  |

*Escrituras U/D y `edit_log` las ejecuta la operación de H-034 (`corregir-entrada.md`); se listan aquí porque la pantalla de H-048 las dispara.*
