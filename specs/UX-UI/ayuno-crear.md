# Flujo: Crear o cambiar un ayuno

**Historias que cubre:** H-004, H-044 (modo «Cambiar ayuno»)
**Entrada:** Planificados → «Planificar ayuno»; Inicio vacío → «Planificar ayuno»; Detalle del ayuno planificado → ⋮ «Cambiar» ([ayuno-planificados.md](ayuno-planificados.md), EV-002) · **Salida:** Planificados (al crear) o el Detalle desde el que se entró (al cambiar)

## Pantallas

### Pantalla: Elegir plantilla

```
┌──────────────────────────────────┐
│ ← Planificar ayuno               │
│ ¿Desde qué plantilla?            │
│  ● Ayuno de 57 horas   57 h      │  ← propuesta: la primera plantilla
│  ○ Ayuno de 36 horas   36 h      │
│  ○ Empezar desde cero            │
│                                  │
│           [ Siguiente ]          │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre de plantilla | `template.name` | lectura |
| Duración objetivo | `template.target_hours` | lectura |
| Opción elegida | `fast.template_id` | escritura (vacío = desde cero; al crear viene marcada la primera plantilla) |

**Acciones:** Siguiente → Datos del ayuno. Atrás → pantalla de origen.
**Estados:** vacío (sin plantillas: solo «Empezar desde cero» + enlace «Crear una plantilla») · cargando · error (no se pudo leer: reintentar) · sin permisos (bloqueo activo).

### Pantalla: Datos del ayuno

```
┌──────────────────────────────────┐
│ ← Ayuno de 57 horas              │
│ Última comida                    │
│  [jueves 22 oct]  [18:00]        │
│ Inicio previsto del reloj        │
│  [jueves 22 oct]  [22:00]        │
│ Fin previsto                     │
│  [domingo 25 oct]  [07:00]       │  ← propuesto: inicio + objetivo
│ Objetivo: 57 horas (calculado)   │
│ Notas del ayuno                  │
│  [_____________________]         │
│──────────────────────────────────│
│ Fin previsto: domingo 25 oct,    │
│ 07:00                            │
│ Etapas: preparación desde lunes  │
│ 19 oct · vuelta a comer 6 h ·    │
│ posterior 4 días                 │
│──────────────────────────────────│
│ Lista previa: 2 de 5 marcadas    │
│        [ Crear ayuno ]           │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Última comida (fecha y hora) | `fast.last_meal_planned` | escritura (obligatorio) |
| Inicio previsto del reloj | `fast.clock_start_planned` | escritura (obligatorio; ≥ última comida) |
| Fin previsto (fecha y hora) | fin previsto | escritura (propuesto: inicio + `template.target_hours`; debe ser posterior al inicio) |
| Objetivo (calculado) | `fast.target_hours` | escritura derivada: horas entre inicio y fin previstos |
| Notas del ayuno | `fast.notes` | escritura (opcional) |
| Fin previsto | fin previsto | derivado |
| Resumen de etapas | `stage.kind`, `stage.duration_value`, `stage.duration_unit` + fechas de etapas | lectura / derivado |
| Lista previa (resumen y casillas al tocar) | `checklist_item.text`, `checklist_item.is_checked` | lectura / escritura |

**Acciones:** «Crear ayuno» → copia el plan de la plantilla (etapas, mediciones y horarios, metas, lista previa, avisos, productos, reglas, vuelta a comer) al ayuno con `status = planned`, programa recordatorios → Planificados. Se puede crear aunque haya otro ayuno en curso (EV-002). Desde cero: mismas entradas más etapas y objetivo; el resto del plan queda vacío (sin recordatorios ni reglas), aunque en Anotar siguen todas las mediciones activas del catálogo (D-18).
**Estados:** vacío (n/a) · cargando (al crear: botón deshabilitado con indicador hasta confirmar guardado) · error (validación: inicio previsto anterior a la última comida → mensaje en el campo; menos de una semana con otro ayuno (D-17) → mensaje bajo el campo de última comida: «Tiene que pasar una semana desde el ayuno que termina el sábado 24 oct, 07:00. Puedes planificarlo desde el sábado 31 oct, 07:00.» o, si choca con uno posterior, «Este ayuno tiene que terminar una semana antes del que empieza el lunes 2 nov, 20:00.»; nada se guarda; fallo al guardar → nada se crea, mensaje y reintento) · sin permisos (notificaciones denegadas → se crea igual y se advierte «No podremos recordarte las mediciones»).

### Modo «Cambiar ayuno» (H-044)

Mismas dos pantallas, solo para un ayuno con `fast.status = planned`. Diferencias:

```
┌──────────────────────────────────┐
│ ← Cambiar ayuno                  │
│ ¿Desde qué plantilla?            │
│  ● Ayuno de 57 horas   57 h      │  ← preseleccionada: fast.template_id
│  ○ Ayuno de 36 horas   36 h      │
│  ○ Empezar desde cero            │
│           [ Siguiente ]          │
└──────────────────────────────────┘
        (si cambia la plantilla)
┌──────────────────────────────────┐
│ ¿Cambiar de plantilla?           │
│ Se reemplazará el plan de este   │
│ ayuno por el de «Ayuno de 36     │
│ horas». Las marcas de la lista   │
│ previa se perderán. Lo que ya    │
│ anotaste no cambia.              │
│   [Cancelar]   [Cambiar plantilla]│
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Opción preseleccionada | `fast.template_id` | lectura / escritura |
| Confirmación de cambio de plantilla | `fast.template_id` + copia del plan | acción (confirmación) |
| Última comida, inicio previsto | `fast.last_meal_planned`, `fast.clock_start_planned` | lectura / escritura (valores actuales; mismas validaciones) |
| Fin previsto | fin previsto | escritura (actual: `fast.clock_start_planned` + `fast.target_hours`; si cambió la plantilla, se propone de nuevo desde `template.target_hours`) |
| Objetivo (calculado) | `fast.target_hours` | escritura derivada |
| Notas del ayuno | `fast.notes` | lectura / escritura |
| Botón «Guardar cambios» | `edit_log.*` (una fila por campo cambiado) | acción |

**Acciones:** «Guardar cambios» → si cambiaron las horas, comprueba la semana de separación sin contar este ayuno (mismo mensaje que al crear); actualiza `fast` (y, si cambió la plantilla, sustituye la copia del plan, D-16), escribe la bitácora y reprograma recordatorios → vuelve a la pantalla de origen. Si no cambió nada, el botón está deshabilitado.
**Estados:** vacío (n/a: siempre hay un ayuno) · cargando (leer el ayuno; al guardar, botón deshabilitado con indicador) · error (validaciones de creación en su campo; el ayuno ya no está planificado — empezó en otra pantalla o una notificación — → «Este ayuno ya empezó; sus horas reales se corrigen desde el reloj» y vuelta atrás; fallo al guardar → nada cambia, se conserva lo tecleado y se ofrece reintentar) · sin permisos (bloqueo activo; notificaciones denegadas → se guarda igual y se advierte como al crear).

## Validación Laws of UX
- **Hick:** 1 acción principal por pantalla. ✔
- **Jakob:** asistente de dos pasos con selectores nativos de fecha y hora. ✔
- **Fitts:** botón principal al pie, ancho completo. ✔
- **Miller:** 4 entradas + 3 bloques de resumen. ✔
- **Modo cambio:** mismo asistente que al crear (Jakob); el reemplazo del plan se confirma en un diálogo con 2 opciones (Hick). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Se puede editar el plan copiado de un ayuno (horarios, reglas, productos) antes y durante el ayuno? Ninguna historia lo pide ni lo prohíbe. Propuesta: sí mientras esté `planned`; durante `in_progress` solo horarios y reglas, con rastro en `edit_log`.
- ~~Avisar si ya hay otro ayuno planificado que se solapa. Propuesta: aviso, no bloqueo.~~ Sustituida en EV-002 (D-17): se **bloquea** crear o cambiar las horas si no hay al menos una semana con cualquier otro ayuno.
