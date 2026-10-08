# Flujo: Crear un ayuno

**Historias que cubre:** H-004
**Entrada:** Inicio → «Nuevo ayuno» · **Salida:** Inicio (ayuno planificado)

## Pantallas

### Pantalla: Elegir plantilla

```
┌──────────────────────────────────┐
│ ← Nuevo ayuno                    │
│ ¿Desde qué plantilla?            │
│  ○ Ayuno de 57 horas   57 h      │
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
| Opción elegida | `fast.template_id` | escritura (vacío = desde cero) |

**Acciones:** Siguiente → Datos del ayuno. Atrás → Inicio.
**Estados:** vacío (sin plantillas: solo «Empezar desde cero» + enlace «Crear una plantilla») · cargando · error (no se pudo leer: reintentar) · sin permisos (bloqueo activo).

### Pantalla: Datos del ayuno

```
┌──────────────────────────────────┐
│ ← Ayuno de 57 horas              │
│ Última comida                    │
│  [jueves 22 oct]  [18:00]        │
│ Inicio previsto del reloj        │
│  [jueves 22 oct]  [22:00]        │
│ Objetivo (horas)   [ 57 ]        │  ← editable; precargado
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
| Objetivo | `fast.target_hours` | escritura (copiado de `template.target_hours`) |
| Notas del ayuno | `fast.notes` | escritura (opcional) |
| Fin previsto | fin previsto | derivado |
| Resumen de etapas | `stage.kind`, `stage.duration_value`, `stage.duration_unit` + fechas de etapas | lectura / derivado |
| Lista previa (resumen y casillas al tocar) | `checklist_item.text`, `checklist_item.is_checked` | lectura / escritura |

**Acciones:** «Crear ayuno» → copia el plan de la plantilla (etapas, mediciones y horarios, metas, lista previa, avisos, productos, reglas, vuelta a comer) al ayuno con `status = planned`, programa recordatorios → Inicio. Desde cero: mismas entradas más etapas y objetivo; el resto del plan queda vacío y se edita después.
**Estados:** vacío (n/a) · cargando (al crear: botón deshabilitado con indicador hasta confirmar guardado) · error (validación: inicio previsto anterior a la última comida → mensaje en el campo; fallo al guardar → nada se crea, mensaje y reintento) · sin permisos (notificaciones denegadas → se crea igual y se advierte «No podremos recordarte las mediciones»).

## Validación Laws of UX
- **Hick:** 1 acción principal por pantalla. ✔
- **Jakob:** asistente de dos pasos con selectores nativos de fecha y hora. ✔
- **Fitts:** botón principal al pie, ancho completo. ✔
- **Miller:** 4 entradas + 3 bloques de resumen. ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Se puede editar el plan copiado de un ayuno (horarios, reglas, productos) antes y durante el ayuno? Ninguna historia lo pide ni lo prohíbe. Propuesta: sí mientras esté `planned`; durante `in_progress` solo horarios y reglas, con rastro en `edit_log`.
- Avisar si ya hay otro ayuno planificado que se solapa. Propuesta: aviso, no bloqueo.
