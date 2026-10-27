# Flujo: Reglas de alerta

**Historias que cubre:** H-019, H-020, H-021, H-022, H-023, H-024, H-025, H-026, H-054, H-055, H-056 (condiciones entre lecturas), H-003 y H-057 (reglas de ejemplo)
**Entrada:** Editor de plantilla → Reglas de alerta; Revisión antes de empezar → «Arreglar ›» de una regla de ejemplo (plan del ayuno) · **Salida:** Editor de plantilla, o Revisión antes de empezar

El editor construye la regla como una frase que se lee de arriba abajo («Si … durante … solo cuando … entonces …»); la misma frase es la que se muestra en la pantalla de Seguridad.

## Pantallas

### Pantalla: Lista de reglas

```
┌──────────────────────────────────┐
│ ← Reglas de alerta      [ + ]    │
│ [●] Glucosa baja sostenida  ROJA │
│ [●] Presión de pie baja     ROJA │
│ [○] Sodio bajo a las 14:00  AVISO│
│ [●] Algo no va bien         ROJA │
│     de ejemplo                   │  ← insignia: la app la trajo así
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor | `rule.active` | escritura |
| Nombre | `rule.name` | lectura |
| Nivel | `rule.level` | lectura |
| Insignia «de ejemplo» | `rule.is_placeholder` | lectura |

**Acciones:** + → editor vacío · tocar → editor. Las reglas no se borran: se desactivan con el interruptor (H-019).
**Estados:** vacío («Esta plantilla no tiene reglas. La app no trae umbrales: tú decides cuándo avisar.» + «Crear regla») · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Editor de regla

```
┌──────────────────────────────────┐
│ ← Regla                  Guardar │
│ Nombre [Glucosa baja sostenida]  │
│──────────────────────────────────│
│ SI                               │
│ (●) Una medición                 │
│ (○) Cambio al ponerme de pie     │
│ (○) Cambio desde mi valor de     │
│     partida                      │
│ (○) Tendencia                    │
│ (○) Síntoma de alarma            │
│ (○) Total del día bajo la meta   │
│ (○) Botón «algo no va bien»      │
│  [Glucosa ▾] [por debajo de ▾]   │
│  [ 55 ] mg/dL                    │
│  [✔] durante al menos [20] min   │
│──────────────────────────────────│
│ SOLO CUANDO (opcional)           │
│  Etapas: [Ayuno] [Vuelta a comer]│
│  [ ] Solo al despertar           │
│  Posición: [cualquiera ▾]        │
│──────────────────────────────────│
│ ENTONCES                         │
│  Nivel: (○) Aviso  (●) Alerta    │
│  Mensaje [Rompe con caldo tibio…]│
│  Acción: (○) Ninguna             │
│   (○) Medir de nuevo en [15] min │
│   (●) Ofrecer terminar el ayuno  │
│──────────────────────────────────│
│ Se leerá así: «Si la glucosa     │
│ queda por debajo de 55 durante   │
│ 20 minutos en el ayuno: alerta   │
│ roja y ofrecer terminar.»        │
└──────────────────────────────────┘
```

Bloque «SI» de las condiciones nuevas (EV-006); «Solo cuando» y «Entonces» no cambian:

```
── Cambio al ponerme de pie (H-054) ──
│  [Pulso ▾] de pie − sentado      │  ← Pulso · La de arriba · La de abajo
│  [sube más de ▾] [ 30 ]          │  ← sube más de · baja más de
│ Se leerá así: «Si el pulso sube  │
│ más de 30 al ponerme de pie:     │
│ aviso y medir de nuevo en 30 min»│
── Cambio desde mi valor de partida (H-055) ──
│  [Presión ▾] [Pulso ▾]           │
│  [más de ▾] [ 15 ] [por encima ▾]│
│  del valor de partida            │
│  ⓘ El de partida es la primera   │
│  lectura del ayuno, o la que     │
│  marques como «de partida».      │
── Tendencia (H-056) ──
│  [Presión ▾] [Pulso ▾]           │
│  ( ) la lectura (●) el cambio al │
│      ponerme de pie              │
│  [ 3 ] lecturas seguidas que     │
│  [suben ▾] cada vez              │
── Total del día bajo la meta (H-022) ──
│  [Sal ▾] por debajo del [ 50 ] % │
│  de la meta a las [14:00]        │
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `rule.name` | escritura (obligatorio) |
| Tipo de condición | `rule.condition_kind` | escritura |
| Medición | `rule.metric_type_code` | escritura (obligatorio en condición de medición) |
| Valor de la presión (si la medición es presión) | `rule.bp_component` | escritura |
| Por debajo / por encima | `rule.condition_kind` (`metric_below` / `metric_above`) | escritura |
| Umbral y unidad | `rule.threshold` (en `metric_type.unit`) | escritura (obligatorio para activar) |
| «Cambio al ponerme de pie»: cifra comparada (pulso, la de arriba, la de abajo) | `rule.condition_kind = pair_difference`, `rule.metric_type_code` = presión arterial (implícito), `rule.bp_component` | escritura |
| «sube más de / baja más de» + cifra | `rule.direction` (`above` / `below`), `rule.threshold` (de pie − sentado; «baja más de 20» se guarda como `below` y −20) | escritura (obligatorio para activar) |
| «Cambio desde mi valor de partida»: medición (y cifra de la presión) | `rule.condition_kind = change_from_baseline`, `rule.metric_type_code`, `rule.bp_component` | escritura |
| «más de N por encima / por debajo» | `rule.threshold` (> 0), `rule.direction` | escritura (obligatorio para activar) |
| «Tendencia»: medición (y cifra de la presión) | `rule.condition_kind = trend`, `rule.metric_type_code`, `rule.bp_component` | escritura |
| «la lectura / el cambio al ponerme de pie» (solo con presión) | `rule.trend_of` (`value` / `pair_difference`) | escritura |
| «N lecturas seguidas» | `rule.trend_count` (≥ 3, 3 de fábrica) | escritura |
| «suben / bajan cada vez» | `rule.direction` (`above` / `below`) | escritura |
| «por debajo del N % de la meta» | `rule.goal_percent` (1-100; vacío = la meta entera) | escritura (opcional) |
| Insignia «de ejemplo» bajo el nombre | `rule.is_placeholder` | lectura (desaparece al guardar un cambio de nombre, mensaje o cifra) |
| «Durante al menos N min» | `rule.sustained_minutes` | escritura (opcional) |
| Síntoma (o «cualquier síntoma de alarma») | `rule.symptom_type_code` (solo `symptom_type.is_alarm`) | escritura |
| Tipo de ingesta | `rule.intake_type_code` | escritura |
| A la hora | `rule.check_time` | escritura |
| Etapas | `rule.filter_stage_kinds` | escritura (opcional) |
| Solo al despertar | `rule.filter_on_waking` | escritura |
| Posición | `rule.filter_position` | escritura (solo con presión) |
| Nivel | `rule.level` | escritura |
| Mensaje | `rule.message` | escritura (obligatorio; se mostrará tal cual) |
| Acción | `rule.action` | escritura |
| Minutos para volver a medir | `rule.action_minutes` | escritura (obligatorio con «medir de nuevo») |
| Franja «Estás cambiando el plan de este ayuno, no la plantilla» (solo al llegar desde la revisión previa) | — | lectura |
| Vista previa «Se leerá así» | frase compuesta de los campos anteriores | derivado |

**Acciones:** Guardar (valida según el tipo de condición) · Atrás con cambios → «¿Descartar?». Los campos irrelevantes para el tipo de condición elegido se ocultan (p. ej. «posición» solo con presión; «a la hora» solo con total del día).
**Estados:** vacío (regla nueva: tipo «Una medición» preseleccionado, campos vacíos; **sin valores sugeridos** de umbral, de cambio ni de porcentaje; solo `trend_count` trae 3, que no es una cifra clínica) · cargando · error (campos obligatorios vacíos; umbral fuera del rango válido de la medición → aviso, no bloqueo; meta inexistente para el tipo de ingesta elegido → «Este líquido no tiene meta en la plantilla»; meta sin cifra → aviso «La meta de este líquido aún no tiene cifra: la regla no se evaluará hasta que la pongas» (no bloquea); porcentaje fuera de 1-100 → «El porcentaje va de 1 a 100»; tendencia con menos de 3 lecturas → «Hacen falta al menos 3 lecturas»; activar una regla sin cifra → «Pon la cifra para poder activarla»; más de una regla «algo no va bien» → «Ya existe una regla para el botón»; fallo al guardar) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** 1 acción principal; las opciones se revelan por pasos (Si → Solo cuando → Entonces) y se ocultan las que no aplican. ✔
- **Jakob:** constructor de reglas «si … entonces …» (patrón de automatizaciones en móviles). ✔
- **Fitts:** controles ≥ 48 pt; selección por botones de opción grandes. ✔
- **Miller:** 3 bloques con ≤ 5 controles visibles cada uno. ✔
- **Hick / Miller (EV-006):** el tipo de condición pasa de 4 a 7 opciones en una lista vertical (dentro de 5-9); al elegir una, solo se muestran sus 2-4 controles. ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Se limita a una sola regla de tipo «algo no va bien» por plantilla? Propuesta: sí (el botón es uno).
- La condición «sostenida» con umbral «por encima» (p. ej. presión alta sostenida): el modelo lo permite. ¿Se ofrece? Propuesta: sí, simétrico a «por debajo».

*Cambio EV-006 (2026-10-26):* tres tipos de condición nuevos en «SI»: **cambio al ponerme de pie** (de pie − sentado de una cifra de la presión, H-054), **cambio desde mi valor de partida** (H-055) y **tendencia** (N lecturas seguidas que suben o bajan, de una medición o del cambio al ponerse de pie, H-056); todos sin cifra sugerida y con su frase «Se leerá así». «Total del día bajo la meta» admite un porcentaje (H-022). Las reglas que trajo la app muestran la insignia «de ejemplo» hasta que se cambia su nombre, su mensaje o una cifra (H-003, H-057). La lista de tipos de condición pasa a radios en vertical para que quepan los 7.
