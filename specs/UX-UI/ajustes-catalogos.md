# Flujo: Catálogos

**Historias que cubre:** H-007, H-010, H-011, H-013, H-049
**Entrada:** Más → Catálogos; enlaces desde estados vacíos de Anotar · **Salida:** Más

## Pantallas

### Pantalla: Catálogos

```
┌──────────────────────────────────┐
│ ← Catálogos                      │
│ › Mediciones          9 activas  │
│ › Síntomas           14 activos  │
│ › Líquidos y electrolitos   7    │
│ › Productos           12 activos │
└──────────────────────────────────┘
```

**Campos:** conteos (derivado). **Acciones:** abrir cada catálogo. **Estados:** cargando · error · sin permisos (bloqueo activo); vacío no aplica (siempre hay precargados, aunque estén desactivados).

### Pantalla: Mediciones (lista y editor)

```
│ [●] Glucosa · mg/dL (o mmol/L)  ›│
│ [●] Peso · kg (o lb)            ›│
│ [○] Cetonas · mmol/L            ›│
│ [+ Medición propia]              │
──── editor ────
│ Nombre [Perímetro de cuello]     │
│ Unidad [cm]  ¿Numérica? [✔]      │
│ Rango válido [20] a [60]         │
──── editor de una medición con palabras (EV-005) ────
│ ← Hambre                         │
│ [●] Activa                       │
│ Rango válido          0 a 10     │
│ Palabras para elegir             │
│ Al anotar, tocas una palabra y   │
│ se guarda su número. Cambiarlas  │
│ no altera lo ya anotado.         │
│ ⋮⋮ [Nada      ] [ 0] ✕           │
│ ⋮⋮ [Poca      ] [ 3] ✕           │
│ ⋮⋮ [Moderada  ] [ 5] ✕           │
│ ⋮⋮ [Mucha     ] [ 7] ✕           │
│ ⋮⋮ [Intensa   ] [10] ✕           │
│ [+ Palabra]                      │
│ [          Guardar           ]   │
```

| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor | `metric_type.active` | escritura |
| Nombre | `metric_type.name` | escritura |
| Unidad | `metric_type.unit` | escritura (fija en precargados) |
| Unidad alternativa | `metric_type.alt_unit` | lectura (solo precargados) |
| ¿Numérica? | `metric_type.is_numeric` | escritura (solo propias) |
| Rango válido | `metric_type.min_valid`, `metric_type.max_valid` | escritura |
| Sección «Palabras para elegir» (solo si `metric_type.is_numeric`) | `metric_option` sin `deleted_at` del tipo, en orden de `position` | lectura |
| Texto de cada palabra | `metric_option.label` | escritura |
| Número de cada palabra | `metric_option.value` | escritura (teclado numérico) |
| Asa ⋮⋮ (arrastrar para ordenar) | `metric_option.position` | escritura |
| ✕ Quitar palabra | control `deleted_at` de `metric_option` | acción (sin confirmación: no altera mediciones; se aplica al pulsar «Guardar» del editor, así que salir sin guardar la deshace) |
| «+ Palabra» | alta de `metric_option` | acción (oculto con 9 palabras) |

**Errores:** nombre vacío; mínimo ≥ máximo. Palabras: texto vacío («Escribe la palabra»); número fuera del rango («El número debe estar entre 0 y 10»); texto o número repetido («Ya hay una palabra con ese texto» / «… con ese número»); cambiar el rango dejando palabras fuera («Hay palabras fuera del rango nuevo: cámbialas antes»).
**Estados de la sección:** vacío («Sin palabras: se anota tecleando el número» + «+ Palabra») · cargando · error (fallo al guardar conserva lo editado) · sin permisos (bloqueo activo).

### Pantalla: Síntomas (lista y editor)

| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor | `symptom_type.active` | escritura |
| Nombre | `symptom_type.name` | escritura |
| Grupo | `symptom_type.group_name` | escritura (texto libre con sugerencias de grupos existentes) |
| «De alarma» | `symptom_type.is_alarm` | escritura |

**Errores:** nombre vacío o repetido.

### Pantalla: Líquidos y electrolitos (lista y editor)

| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `intake_type.name` | escritura |
| Unidad | `intake_type.unit` | escritura |
| «Rompe el ayuno» | `intake_type.breaks_fast` | escritura |
| Factor de sodio | `intake_type.sodium_factor` | escritura (opcional; la sal trae 0.393) |

**Errores:** nombre vacío; factor ≤ 0.

### Pantalla: Productos (lista y editor)

```
│ Vitamina C · suplemento · 1 g    │
│   08:30 · 20:00                 ›│
│ Ibuprofeno · medicamento ⚠ evitar›│
│ [+ Producto]                     │
──── editor ────
│ Nombre [Vitamina C]              │
│ Código [vit_c] (no se puede      │
│   cambiar después)               │
│ Tipo (●)Suplemento (○)Medicamento│
│ Dosis [1] [g]                    │
│ Horarios [08:30 · 1 g] [20:00 ·  │
│   1 g] [+]                       │
│ [ ] A evitar   [●] Activo        │
```

| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `product.name` | escritura |
| Código | `product.code` | escritura solo al crear; después lectura |
| Tipo | `product.kind` | escritura |
| Dosis habitual | `product.default_amount` | escritura |
| Unidad | `product.unit` | escritura |
| Horarios y dosis por toma | `product_schedule.time_of_day`, `product_schedule.amount` | escritura |
| A evitar | `product.avoid` | escritura |
| Activo | `product.active` | escritura |

**Errores:** código vacío, repetido o con caracteres distintos de minúsculas, números y `_`; dosis ≤ 0.

**Acciones (todas las listas):** tocar → editor · + → editor vacío · Guardar · desactivar (nunca borra lo ya anotado).
**Estados (todos):** vacío (lista sin elementos propios: solo los precargados) · cargando · error (validaciones indicadas; fallo al guardar) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** 1 acción principal por pantalla (abrir / guardar). ✔
- **Jakob:** listas de ajustes con interruptor y detalle. ✔
- **Fitts:** interruptores y filas ≥ 48 pt. ✔
- **Miller:** 4 catálogos; listas largas con búsqueda y agrupación (síntomas por grupo). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Código del producto:** ¿lo escribe la persona o lo genera la app a partir del nombre? Propuesta: se genera (`vitamina_c`) y es editable solo al crear.
- **Mediciones precargadas:** ¿se puede cambiar su nombre? Propuesta: sí, su código no cambia.

*Cambio EV-005 (2026-10-23):* el editor de una medición numérica añade «Palabras para elegir» (H-049): texto y número de cada palabra, orden, alta y baja, hasta 9. Las de fábrica de Hambre, Energía, Ánimo y Claridad mental se editan igual. Mockup: https://claude.ai/artifact/ERYg3yGCNPWG7x6tdGkjCq

*Implementación (EV-005, 2026-10-23):* el orden se cambia con el botón «Subir» de cada palabra en lugar de arrastrar por el asa ⋮⋮ (mismo resultado con un objetivo de 48 pt, sin gesto de arrastre). Los errores de rango usan un único mensaje («El número de cada palabra debe estar dentro del rango válido. Si cambias el rango, cambia antes las palabras que quedan fuera.»), y un número sin escribir da «Escribe el número de cada palabra».
