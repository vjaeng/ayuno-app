# Flujo: Terminar el ayuno

**Historias que cubre:** H-030, H-012, H-027
**Entrada:** Seguridad → «Terminar el ayuno»; Alerta → «Termino el ayuno»; Anotar una ingesta que rompe el ayuno → «Sí, terminar»; Inicio → reloj → «Terminar el ayuno» · **Salida:** Vuelta a comer (si la plantilla la usa) o Inicio

## Pantallas

### Pantalla: Terminar el ayuno

```
┌──────────────────────────────────┐
│ ← Terminar el ayuno              │
│ Hora de fin                      │
│  [domingo 25 oct] [07:03:12]     │  ← default ahora, editable
│ Duración: 57 h 3 min             │
│ ¿Por qué terminas?               │
│  ○ Objetivo cumplido             │
│  ○ Regla de alerta [Glucosa ▾]   │
│  ○ Decisión personal             │
│  ○ Otro: [______________]        │
│                                  │
│ Un ayuno terminado no se puede   │
│ reanudar.                        │
│       [ Terminar el ayuno ]      │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Hora de fin | `fast.end_actual` | escritura (default ahora; ≥ `fast.clock_start_actual`) |
| Duración | duración final | derivado |
| Motivo | `fast.end_reason` | escritura (obligatorio) |
| Regla (si motivo = regla de alerta) | `fast.end_rule_id` → `rule.name` | escritura (prellenada si viene de una alerta) |
| Texto (si motivo = otro) | `fast.end_reason_detail` | escritura (obligatorio con «otro») |
| Estado resultante | `fast.status = finished` | escritura |

**Acciones:** «Terminar el ayuno» → guarda y pasa a [vuelta-a-comer.md](vuelta-a-comer.md) si el ayuno tiene etapa `refeed`; si no, a Inicio. Si se llega desde una ingesta que rompe el ayuno, la hora de fin se propone igual a `intake.taken_at`. Si se llega desde una alerta, el motivo «regla de alerta» y la regla vienen prellenados.
**Estados:** vacío (n/a) · cargando (botón deshabilitado hasta confirmar el guardado) · error (motivo sin elegir o «otro» sin texto: mensaje en el campo; hora de fin anterior al inicio: mensaje; fallo al guardar: el ayuno sigue en curso y se ofrece reintentar) · sin permisos (bloqueo activo).

### Pantalla: Resultado

```
┌──────────────────────────────────┐
│ Ayuno terminado                  │
│ domingo 25 de octubre, 07:03     │
│ 57 horas y 3 minutos             │
│ Motivo: objetivo cumplido        │
│      [ Empezar vuelta a comer ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Hora de fin | `fast.end_actual` | lectura |
| Duración | duración final | derivado |
| Motivo | `fast.end_reason`, `fast.end_reason_detail`, `fast.end_rule_id` | lectura |

**Acciones:** «Empezar vuelta a comer» (o «Volver al inicio» si no hay etapa de vuelta a comer). Mismo diseño neutro sea cual sea el motivo: sin colores de juicio, sin «fracaso» ni «éxito».
**Estados:** como la pantalla anterior.

## Validación Laws of UX
- **Hick:** 1 acción principal; 4 motivos en lista única. ✔
- **Jakob:** formulario de confirmación con selección única. ✔
- **Fitts:** botón principal al pie, ancho completo. ✔
- **Miller:** 3 bloques (hora, duración, motivo). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Se pide confirmación adicional («¿Seguro?») antes de terminar? Propuesta: no; el formulario ya es la confirmación, y cualquier fricción extra juega en contra de parar a tiempo.
