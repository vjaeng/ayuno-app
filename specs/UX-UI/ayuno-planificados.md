# Flujo: Ayunos planificados

**Historias que cubre:** H-046, H-005 (empezar y registrar la última comida desde el detalle), H-044 (acceso a «Cambiar»), H-045 (acceso a «Borrar ayuno»), H-004 (acceso a «Planificar ayuno»)
**Entrada:** pestaña Planificados; Inicio → «Ver y preparar este ayuno» del próximo · **Salida:** Crear ayuno, Cambiar ayuno, Inicio (al empezar)

## Pantallas

### Pantalla: Planificados

```
┌──────────────────────────────────┐
│ Planificados                     │
│ jueves 22 oct, 18:00          ›  │
│   57 horas · Ayuno de 57 horas   │
│ lunes 2 nov, 20:00            ›  │
│   36 horas · Ayuno de 36 horas   │
│ sábado 14 nov, 19:00          ›  │
│   24 horas · desde cero          │
│──────────────────────────────────│
│      [ + Planificar ayuno ]      │  ← BotonPrincipal tras la lista
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Lista (solo planificados, orden ascendente) | `fast.status` = `planned`, ordenados por `fast.last_meal_planned` | lectura |
| Fecha y hora de la fila | `fast.last_meal_planned` | lectura |
| Objetivo | `fast.target_hours` | lectura |
| Plantilla de origen | `fast.template_id` → `template.name` («desde cero» si nulo; «plantilla borrada» si ya no existe) | lectura |
| Botón «Planificar ayuno» | — | acción → [ayuno-crear.md](ayuno-crear.md) |

**Acciones:** tocar una fila → Detalle del ayuno planificado · «Planificar ayuno» → Crear ayuno; al guardar, vuelve aquí. Disponible también con un ayuno en curso.
**Estados:** vacío («No tienes ayunos planificados. Planifica uno eligiendo una plantilla y la hora de tu última comida.» + «Planificar ayuno») · cargando (esqueleto de filas) · error (no se pudo leer: mensaje llano y «Reintentar») · sin permisos (bloqueo activo → huella o PIN).

### Pantalla: Detalle del ayuno planificado

```
┌──────────────────────────────────┐
│ ← Ayuno del jueves 22 oct      ⋮ │  ← ⋮: Cambiar · Borrar ayuno
│ Última comida: jueves 22 oct,    │
│   18:00                          │
│ Inicio del reloj: jueves 22 oct, │
│   22:00                          │
│ Fin previsto: domingo 25 oct,    │
│   07:00 · objetivo 57 horas      │
│ Notas: «Semana tranquila»        │
│──────────────────────────────────│
│ Lista previa                     │
│  ☑ Avisar en el trabajo          │
│  ☐ Comprar sal y agua            │
│──────────────────────────────────│
│ [ Registrar última comida ]      │  ← BotonSecundario
│   [ Empieza el ayuno ahora ]     │  ← BotonPrincipal al pie
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Última comida prevista (y real, si ya se registró: «previsto 18:00 · real 18:10») | `fast.last_meal_planned`, `fast.last_meal_actual` | lectura |
| Inicio previsto del reloj | `fast.clock_start_planned` | lectura |
| Fin previsto | fin previsto | derivado |
| Objetivo | `fast.target_hours` | lectura |
| Notas | `fast.notes` | lectura |
| Lista previa | `checklist_item.text`, `checklist_item.is_checked` | lectura / escritura |
| Botón «Registrar última comida» / «Corregir última comida» | `fast.last_meal_actual` | escritura (default `fast.last_meal_planned`, editable) |
| Botón «Empieza el ayuno ahora» | `fast.clock_start_actual`, `fast.status` | acción → escritura (H-005) |
| Menú ⋮ → «Cambiar» | — | acción → [ayuno-crear.md](ayuno-crear.md), modo cambio (H-044) |
| Menú ⋮ → «Borrar ayuno» | `fast` (D) + recuento de registros del ayuno | acción → confirmación de [historial-revisar.md](historial-revisar.md) (H-045) |

**Acciones:** «Empieza el ayuno ahora» → fija `clock_start_actual` con segundos, `status = in_progress`, reprograma recordatorios y lleva a Inicio con el reloj (sin confirmación, igual que en Inicio), donde se abre la hoja «Peso al empezar» ([ayuno-inicio.md](ayuno-inicio.md), H-047). Si ya hay otro ayuno en curso, el botón no aparece y en su lugar una FranjaInformativa dice «Termina el ayuno en curso para poder empezar este.» «Cambiar» → modo cambio y vuelta a este detalle. «Borrar ayuno» → confirmación «Borrar para siempre»; al confirmar, vuelve a Planificados.
**Estados:** vacío (n/a) · cargando (esqueleto; al empezar, botón deshabilitado con indicador) · error (fallo al leer → mensaje y «Reintentar»; el ayuno ya no está planificado — se empezó desde Inicio o una notificación, o se borró → «Este ayuno ya no está planificado.» y vuelta a Planificados) · sin permisos (bloqueo activo; notificaciones denegadas → FranjaInformativa como en Inicio).

## Validación Laws of UX
- **Hick:** lista: 2 acciones (tocar una fila, Planificar ayuno). Detalle: 1 acción principal (Empieza el ayuno ahora) + 1 secundaria (Registrar última comida); Cambiar y Borrar, raras, en el menú ⋮. ✔
- **Jakob:** lista → detalle con acciones en la barra superior, patrón estándar de Android. ✔
- **Fitts:** «Planificar ayuno» y «Empieza el ayuno ahora» al pie, ancho completo; filas FilaLista de 56 dp. ✔
- **Miller:** detalle en 3 bloques (horas, lista previa, acciones); la lista rara vez pasa de 5-9 filas y se desplaza. ✔
