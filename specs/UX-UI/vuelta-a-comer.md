# Flujo: Vuelta a comer

**Historias que cubre:** H-031 (seguir el plan), H-032
**Entrada:** Resultado de Terminar el ayuno; Inicio mientras dure la etapa de vuelta a comer; notificación de un paso · **Salida:** Inicio

## Pantallas

### Pantalla: Vuelta a comer

```
┌──────────────────────────────────┐
│ Vuelta a comer                   │
│ domingo 25 oct · desde las 07:03 │
│──────────────────────────────────│
│ Pasos de hoy                     │
│ ☑ 07:03 Una taza de caldo (07:05)│
│ ☑ 08:03 Una taza de caldo (08:10)│
│ ☐ 09:03 Una taza de caldo        │
│ ☐ 13:00 Comida blanda            │
│──────────────────────────────────│
│ Comidas anotadas                 │
│  09:40 Puré de calabaza · bien   │
│     [ + Anotar comida ]          │
│──────────────────────────────────│
│ Reintroducción                   │
│ ☐ lunes 26 oct · Vuelven los     │
│   suplementos                    │
│ ☐ martes 27 oct · Vuelven las    │
│   legumbres                      │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Inicio de la vuelta a comer | `fast.end_actual` | lectura |
| Paso: descripción | `refeed_step.description` | lectura |
| Paso: hora calculada | `refeed_check.scheduled_at` | lectura |
| Casilla del paso | `refeed_check.done_at` | escritura (guarda la hora real) |
| Reintroducción: fecha | `refeed_step.day_offset` + `fast.end_actual` → fecha | derivado |
| Reintroducción: casilla | `refeed_check.done_at` | escritura |
| Lista de comidas | `meal.eaten_at`, `meal.description`, `meal.tolerance` | lectura |
| Botón «Anotar comida» | — | acción → Anotar comida |

**Acciones:** marcar casilla (un toque; desmarcar pide confirmación y queda en `edit_log`) · Anotar comida.
**Estados:** vacío (plan sin pasos: «Tu plantilla no tiene plan de vuelta a comer» + «Anotar comida» sigue disponible) · cargando · error · sin permisos (bloqueo activo; notificaciones denegadas: franja).

### Pantalla: Anotar comida

```
┌──────────────────────────────────┐
│ ← Comida        domingo 09:40 ✎  │
│ ¿Qué comiste? [______________]   │
│ Cantidad aproximada [________]   │
│ ¿Cómo te cayó?                   │
│ [Bien][Pesadez][Náusea][Diarrea] │
│ [Otro: ________]                 │
│                     [ Guardar ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha y hora | `meal.eaten_at` | escritura (default ahora) |
| Descripción | `meal.description` | escritura (obligatorio) |
| Cantidad aproximada | `meal.approx_amount` | escritura (opcional) |
| Cómo cayó | `meal.tolerance` | escritura (obligatorio) |
| Texto de «Otro» | `meal.tolerance_detail` | escritura (obligatorio con «otro») |

**Acciones:** Guardar → vuelve a Vuelta a comer.
**Estados:** error (descripción o «cómo cayó» vacíos: mensaje en el campo); resto como los demás.

## Validación Laws of UX
- **Hick:** 2 acciones (marcar, anotar comida). ✔
- **Jakob:** lista de tareas con casillas. ✔
- **Fitts:** casilla = fila completa tocable. ✔
- **Miller:** 3 secciones; los pasos repetidos se agrupan por bloque horario. ✔

## Decisiones de producto

*Implementación (tanda 3, 2026-10-10):* Inicio muestra esta pantalla durante la etapa `refeed` (con «Anotar» para seguir registrando mediciones); después, la reintroducción sin marcar aparece en Inicio como recordatorio. La notificación de un paso ofrece abrir y posponer (sin «no se hizo»: la casilla se marca aquí). «¿Cómo te cayó?» usa lista de opciones: cinco botones segmentados no caben con texto ≥ 17 pt. Hoy sigue mostrando el ayuno terminado mientras dure su vuelta a comer, reintroducción o etapa posterior.

*Propuestas aceptadas por el humano el 2026-10-08.*
- Durante la vuelta a comer, ¿Inicio muestra esta pantalla en lugar del reloj? Propuesta: sí, mientras dure la etapa `refeed`; después vuelve el Inicio sin ayuno con la reintroducción pendiente como recordatorio.
- ¿Se pueden seguir anotando mediciones durante la vuelta a comer y la etapa posterior? Propuesta: sí, con la misma rejilla Anotar (las entradas siguen colgando del ayuno).
