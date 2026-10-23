# Flujo: Anotar

**Historias que cubre:** H-008, H-009, H-010, H-012, H-013, H-015, H-035, H-049
**Entrada:** Inicio → «Anotar», o acción directa de una notificación, o un pendiente de Hoy · **Salida:** pantalla de origen; o Aviso/Alerta si una regla se cumple; o Terminar el ayuno si se anota algo que lo rompe

Objetivo de diseño: **3 toques o menos y menos de 30 segundos** desde Inicio hasta guardar (Anotar → tipo → Guardar).

## Pantallas

### Pantalla: Rejilla Anotar

```
┌──────────────────────────────────┐
│ Anotar                       ✕   │
│ ┌────────┐┌────────┐┌────────┐   │
│ │Glucosa ││Presión ││ Peso   │   │
│ └────────┘└────────┘└────────┘   │
│ ┌────────┐┌────────┐┌────────┐   │
│ │Temper. ││Hambre  ││Energía │   │  ← activas del catálogo, las del plan primero
│ └────────┘└────────┘└────────┘   │
│ ┌────────┐┌────────┐┌────────┐   │
│ │Síntoma ││Líquido ││Producto│   │
│ └────────┘└────────┘└────────┘   │
│ ┌────────┐                       │
│ │ Nota   │                       │
│ └────────┘                       │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Botón por medición activa | `metric_type.name` (todas con `metric_type.active`; primero las de `plan_metric.metric_type_code`) | lectura |
| Botones fijos Síntoma, Líquido, Producto, Nota | — | acción |

**Acciones:** cada botón abre su formulario de una sola pantalla.
**Estados:** vacío (catálogo sin mediciones activas: solo los botones fijos + «Actívalas en Más, Catálogos») · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Formulario de medición (genérico; glucosa como ejemplo)

```
┌──────────────────────────────────┐
│ ← Glucosa                        │
│ viernes 23 oct, 12:01:40  [✎]    │
│        [   64   ] mg/dL ▾        │  ← teclado numérico abierto
│ Origen: (●Sensor) (○Glucómetro)  │
│ Tendencia: ⇈ ↑ → ↓ ⇊  (opcional) │
│ [ ] Al despertar                 │
│ Nota: [______________]           │
│ [No se hizo]        [ Guardar ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha y hora | `measurement.taken_at` | escritura (default ahora con segundos) |
| Hora programada (si viene de un pendiente) | `measurement.scheduled_for` | escritura implícita |
| Valor | `measurement.value_entered` | escritura |
| Selector de unidad | `measurement.unit_entered` (default `metric_type.entry_unit`; opciones `metric_type.unit`, `metric_type.alt_unit`) | escritura |
| Valor convertido (bajo el campo, si la unidad difiere) | `measurement.value_num` | derivado al guardar y almacenado |
| Origen (solo glucosa) | `measurement.source` | escritura |
| Tendencia (solo glucosa) | `measurement.trend` | escritura (opcional) |
| Casilla «Al despertar» | `measurement.on_waking` | escritura |
| Texto (tipos no numéricos) | `measurement.value_text` | escritura |
| Nota | `measurement.note` | escritura |
| Botón «No se hizo» | `measurement.not_done_reason` | escritura (motivo obligatorio) |
| Confirmación de rango | `metric_type.min_valid`, `metric_type.max_valid` | lectura |

**Acciones:** Guardar → si el valor está fuera de rango, diálogo «¿Seguro? 640 está fuera de lo habitual (20 a 600)» con «Guardar tal cual» / «Corregir»; se guarda sin redondear. Tras guardar se evalúan las reglas → [seguridad-alertas.md](seguridad-alertas.md) si alguna se cumple; si no, vuelve al origen con confirmación breve. Un peso tecleado en lb muestra «157.4 lb · 71.4 kg».
**Estados:** vacío (n/a) · cargando (Guardar deshabilitado hasta confirmar escritura) · error (campo vacío: «Escribe el valor o marca No se hizo»; fallo al guardar: el formulario conserva lo tecleado y ofrece reintentar) · sin permisos (bloqueo activo).

### Pantalla: Formulario de medición con palabras (H-049, EV-005; hambre como ejemplo)

Se usa en lugar del formulario genérico cuando la medición tiene `metric_option` sin borrar (de fábrica: Hambre, Energía, Ánimo y Claridad mental).

```
┌──────────────────────────────────┐
│ ← Hambre                         │
│ viernes 23 oct, 12:01:40  [✎]    │
│ ┌──────────────────────────────┐ │
│ │ ○ Nada                     0 │ │
│ ├──────────────────────────────┤ │
│ │ ○ Poca                     3 │ │
│ ├──────────────────────────────┤ │
│ │ ○ Moderada                 5 │ │
│ ├──────────────────────────────┤ │
│ │ ● Mucha                    7 │ │  ← elegida (primary_container)
│ ├──────────────────────────────┤ │
│ │ ○ Intensa                 10 │ │
│ └──────────────────────────────┘ │
│ [ ] Al despertar                 │
│ Nota: [______________]           │
│ [No se hizo]        [ Guardar ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha y hora | `measurement.taken_at` | escritura (default ahora con segundos) |
| Hora programada (si viene de un pendiente) | `measurement.scheduled_for` | escritura implícita |
| Botón por palabra (SelectorPalabras), en orden de `position` | `metric_option.label`, `metric_option.value` (sin `deleted_at`, del `metric_type_code` del formulario) | lectura |
| Palabra elegida | `measurement.value_entered` = `measurement.value_num` = `metric_option.value` | escritura |
| Unidad | `measurement.unit_entered` = `metric_type.entry_unit` | escritura implícita (sin selector) |
| Casilla «Al despertar» | `measurement.on_waking` | escritura |
| Nota | `measurement.note` | escritura |
| Botón «No se hizo» | `measurement.not_done_reason` | escritura (motivo obligatorio) |

**Acciones:** tocar una palabra la elige (una sola; tocar otra cambia la elección). Guardar → igual que el genérico: se evalúan las reglas con el número (una regla «Hambre ≥ 7» salta con «Mucha») → [seguridad-alertas.md](seguridad-alertas.md) si alguna se cumple; si no, vuelve al origen con confirmación breve («Hambre: Mucha guardada»). No hay confirmación de rango: los números de las palabras ya están dentro del rango.
**Estados:** vacío (la medición no tiene palabras → formulario genérico con teclado) · cargando (Guardar deshabilitado hasta confirmar escritura) · error (sin palabra elegida: «Elige una opción o marca No se hizo»; la palabra elegida se quitó en Catálogos mientras el formulario estaba abierto: «Esa opción ya no existe. Elige otra.»; fallo al guardar: conserva la elección y ofrece reintentar) · sin permisos (bloqueo activo).

### Pantalla: Formulario de presión arterial

```
┌──────────────────────────────────┐
│ ← Presión arterial               │
│ viernes 23 oct, 07:10:05  [✎]    │
│ Posición: (●Sentado)(○De pie)    │
│           (○Acostado)            │
│ Sistólica  [ 118 ] mmHg          │
│ Diastólica [  76 ] mmHg          │
│ Pulso      [  64 ] por minuto    │
│ [ ] Al despertar   Nota: [____]  │
│ [No se hizo]        [ Guardar ]  │
└──────────────────────────────────┘
   ↓ al guardar sentado
┌──────────────────────────────────┐
│ ¿Ahora de pie?                   │
│ [ Anotar de pie ]   [ Ahora no ] │
└──────────────────────────────────┘
   ↓ tras guardar de pie
│ Al ponerte de pie: −14 sistólica │
│ · −6 diastólica · +12 pulso      │
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha y hora | `blood_pressure_reading.taken_at` | escritura |
| Hora programada | `blood_pressure_reading.scheduled_for` | escritura implícita |
| Posición | `blood_pressure_reading.position` | escritura |
| Sistólica | `blood_pressure_reading.systolic` | escritura |
| Diastólica | `blood_pressure_reading.diastolic` | escritura |
| Pulso | `blood_pressure_reading.pulse` | escritura (opcional) |
| Al despertar | `blood_pressure_reading.on_waking` | escritura |
| Nota | `blood_pressure_reading.note` | escritura |
| No se hizo | `blood_pressure_reading.not_done_reason` | escritura |
| Vínculo con la sentada (toma de pie) | `blood_pressure_reading.paired_reading_id` | escritura implícita |
| Diferencia al ponerse de pie | diferencia sentado / de pie | derivado |

**Acciones:** Guardar (sentado) → pregunta «¿Ahora de pie?»; «Anotar de pie» abre el mismo formulario con posición de pie y vínculo; «Ahora no» termina. Reglas evaluadas tras cada guardado.
**Estados:** como el formulario genérico; error adicional «La diastólica debe ser menor que la sistólica».

### Pantalla: Síntoma

```
┌──────────────────────────────────┐
│ ← Síntoma        viernes 12:03 ✎ │
│ De alarma                        │
│  [Mareo al ponerse de pie]  ...  │  ← marcados como alarma, primero
│ Otros (por grupo)                │
│  [Hambre] [Dolor de cabeza]      │
│  [Frío]   [Náusea]  [Insomnio]   │
│ Intensidad  0 ───●────── 10  (4) │
│ Nota: [______________]           │
│                     [ Guardar ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Lista de síntomas | `symptom_type.name`, `symptom_type.group_name`, `symptom_type.is_alarm` (solo `active`) | lectura |
| Síntoma elegido | `symptom.symptom_type_code` | escritura |
| Fecha y hora | `symptom.noted_at` | escritura |
| Intensidad | `symptom.severity` | escritura (0-10) |
| Nota | `symptom.note` | escritura |

**Acciones:** Guardar → evalúa reglas (`alarm_symptom` salta al instante).
**Estados:** vacío (catálogo sin síntomas activos: enlace a Catálogos) · cargando · error · sin permisos.

### Pantalla: Líquido o electrolito

```
┌──────────────────────────────────┐
│ ← Líquido       viernes 12:05 ✎  │
│ [Agua] [Infusión] [Sal] [Potasio]│
│ [Magnesio] [Caldo] [Otro]        │
│ Cantidad [ 5 ] g                 │
│ Sodio: 1.97 g                    │  ← solo si el tipo tiene factor
│ Hoy: Sal 5 de 4 g                │
│ Nota: [______________]           │
│                     [ Guardar ]  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Tipos | `intake_type.name`, `intake_type.unit` | lectura |
| Tipo elegido | `intake.intake_type_code` | escritura |
| Fecha y hora | `intake.taken_at` | escritura |
| Cantidad | `intake.amount` | escritura |
| Unidad | `intake.unit` (default `intake_type.unit`) | escritura |
| Sodio | sodio (`amount` × `intake_type.sodium_factor`) | derivado |
| Total del día frente a meta | totales del día, `intake_goal.daily_amount` | derivado / lectura |
| Nota | `intake.note` | escritura |

**Acciones:** Guardar. Si `intake_type.breaks_fast` y el ayuno está `in_progress`: diálogo «Anotaste caldo. ¿Estás terminando el ayuno?» → «Sí, terminar» ([fin-terminar.md](fin-terminar.md), con la ingesta ya guardada) / «No, sigo» (se guarda y el ayuno sigue).
**Estados:** como los demás; sin colores de juicio en el total.

### Pantalla: Producto (toma)

```
┌──────────────────────────────────┐
│ ← Producto      viernes 08:30 ✎  │
│ Pendientes ahora                 │
│  Vitamina C 1 g · 08:30          │
│   [Tomado] [No tomado] [Otra dosis]│
│ Otros productos                  │
│  [Magnesio] [Ibuprofeno ⚠] ...   │
│  (en gris: los que salen)        │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Producto | `product.name`, `product.kind` | lectura |
| Estado en el ayuno (gris si sale) | `plan_product.during_fast` | lectura |
| Marca «a evitar» (⚠) | `product.avoid` | lectura |
| Toma pendiente: hora y dosis | `product_schedule.time_of_day`, `product_schedule.amount`, `product.unit` | lectura |
| Hora programada | `product_log.scheduled_at` | escritura implícita |
| Dosis planificada | `product_log.planned_amount` | escritura implícita |
| Tomado / No tomado / Otra dosis | `product_log.status` | escritura |
| Hora real | `product_log.taken_at` | escritura (default ahora) |
| Dosis tomada | `product_log.amount`, `product_log.unit` | escritura |
| Motivo | `product_log.reason` | escritura (obligatorio en no tomado y otra dosis) |

**Acciones:** Tomado (un toque, guarda con hora actual y dosis planificada) · No tomado (pide motivo) · Otra dosis (pide dosis y motivo). Elegir un producto con `avoid = true` muestra «Marcaste este producto como a evitar» con «Anotar igual» / «Cancelar»; si se anota, se guarda.
**Estados:** vacío (sin productos: enlace a Catálogos) · cargando · error · sin permisos.

### Pantalla: Nota

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Fecha y hora | `note.noted_at` | escritura (default ahora) |
| Texto | `note.text` | escritura (obligatorio) |

**Acciones:** Guardar. **Estados:** error «Escribe algo o cierra» si está vacía; resto como los demás.

## Validación Laws of UX
- **Hick:** cada formulario tiene 1 acción principal (Guardar) y 1 secundaria (No se hizo); la rejilla es una elección única. ✔
- **Jakob:** rejilla de accesos + formulario de una pantalla, patrón de apps de salud. ✔
- **Fitts:** botones de rejilla ≥ 96 pt; Guardar al alcance del pulgar; teclado numérico abierto de entrada; en las escalas, palabras a ancho completo de 56 pt (EV-005). ✔
- **Hick / Miller (escalas, EV-005):** una elección única entre 5 palabras de fábrica, como mucho 9. ✔
- **Miller:** la rejilla puede superar 9 botones si se activan muchas mediciones → se agrupan en «Mediciones» y «Otros registros» (bloques ≤ 9). ✔ con condición.

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Escalas 0-10** (hambre, energía, ánimo, claridad): ¿botones sueltos en la rejilla o agrupadas en un único formulario «Cómo me siento» que guarda varias mediciones a la vez? Propuesta: formulario agrupado, para no saturar la rejilla.
- **«Al despertar»:** ¿se marca solo a mano o se propone marcado si es la primera lectura tras el fin del silencio nocturno? Propuesta: propuesto automáticamente, editable.

*Cambio EV-005 (2026-10-23):* las mediciones con palabras (`metric_option`; de fábrica Hambre, Energía, Ánimo y Claridad mental) se anotan tocando una palabra que guarda su número, en lugar de teclearlo (H-049). La decisión de 2026-10-08 de agrupar las escalas en un formulario «Cómo me siento» no se construyó (cada escala tiene su botón en la rejilla, D-18) y no cambia aquí. Motivo: el humano quiere elegir cómo se siente con palabras. Mockup: https://claude.ai/artifact/ERYg3yGCNPWG7x6tdGkjCq
