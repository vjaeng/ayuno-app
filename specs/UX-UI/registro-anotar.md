# Flujo: Anotar

**Historias que cubre:** H-008, H-009, H-010, H-012, H-013, H-015, H-035, H-049, H-050 (medidas caseras), H-051 (agua con sal; formulario desde la pulsación larga), H-053 (temporizador de pie)
**Entrada:** Inicio → «Anotar», o pulsación larga en un BotonRapido de Inicio, o acción directa de una notificación (también «Sí, anotar» de la pregunta de H-052), o un pendiente de Hoy · **Salida:** pantalla de origen; o Aviso/Alerta si una regla se cumple; o Terminar el ayuno si se anota algo que lo rompe

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
   ↓ al guardar sentado → Temporizador de pie (abajo)
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

**Acciones:** Guardar (sentado) → abre el **Temporizador de pie** (EV-006; sustituye a la antigua pregunta «¿Ahora de pie?»). Una toma de pie o acostado anotada directamente desde aquí se guarda sin vínculo ni segundos. Reglas evaluadas tras cada guardado.
**Estados:** como el formulario genérico; error adicional «La diastólica debe ser menor que la sistólica».

### Pantalla: Temporizador de pie (H-053, EV-006)

Solo sentado → de pie; medir acostado queda fuera. Los tiempos salen de `plan_metric.standing_check_seconds` del ayuno (de fábrica 60 y 180 s).

```
┌──────────────────────────────────┐
│ ← Presión de pie                 │
│ Sentado: 118/76 · pulso 83 (07:10)│
│ Ponte de pie ahora. Te avisaré   │
│ al minuto y a los 3 minutos.     │
│ [      Ya estoy de pie       ]   │  ← BotonPrincipal
│ [    Anotar sin esperar      ]   │  ← BotonSecundario
│ [         Ahora no           ]   │  ← BotonSecundario
└──────────────────────────────────┘
   ↓ «Ya estoy de pie»
┌──────────────────────────────────┐
│ De pie desde las 07:11:02        │
│             0:42                 │  ← Temporizador
│ Lecturas: a 1 min ○ · a 3 min ○  │
│ [ Anotar sin esperar ] [Cancelar]│
└──────────────────────────────────┘
   ↓ a los 60 s: suena y vibra (también con la pantalla apagada)
┌──────────────────────────────────┐
│ Toma la presión ahora · 1 min    │
│ 07:12:02 [✎] · de pie 60 s       │
│ Sistólica  [ 114 ] mmHg          │
│ Diastólica [  74 ] mmHg          │
│ Pulso      [ 114 ] por minuto    │
│ Nota: [____]        [ Guardar ]  │
│ (el temporizador sigue: 1:05)    │
└──────────────────────────────────┘
   ↓ tras la última lectura
┌──────────────────────────────────┐
│ Al ponerte de pie                │
│ a 1 min: −4 arriba · −2 abajo ·  │
│   +31 pulso                      │
│ a 3 min: −6 arriba · 0 abajo ·   │
│   +43 pulso                      │
│ [          Hecho             ]   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Lectura sentado de referencia | `blood_pressure_reading.systolic`, `.diastolic`, `.pulse`, `.taken_at` (la sentada recién guardada) | lectura |
| Tiempos de aviso («al minuto y a los 3 minutos») | `plan_metric.standing_check_seconds` | lectura |
| «Ya estoy de pie» | — (hora de inicio del temporizador; no se guarda como campo: vive en la alarma programada y en el estado de la pantalla) | acción |
| Temporizador | segundos desde «Ya estoy de pie» | derivado |
| Lecturas hechas y pendientes (○ / ✔) | lecturas de pie con `paired_reading_id` de la sentada | derivado |
| Hora de la lectura de pie | `blood_pressure_reading.taken_at` (default: hora del aviso; editable) | escritura |
| «de pie 60 s» | `blood_pressure_reading.seconds_standing` = `taken_at` − inicio del temporizador | escritura implícita (calculado, nunca tecleado) |
| Sistólica, diastólica, pulso, nota | `blood_pressure_reading.systolic`, `.diastolic`, `.pulse`, `.note` | escritura |
| Posición y vínculo | `blood_pressure_reading.position = standing`, `.paired_reading_id` = la sentada | escritura implícita |
| «Anotar sin esperar» | `blood_pressure_reading.seconds_standing` = nulo | escritura (la lectura dice «tomada sin esperar») |
| Resultado «a 1 min: −4 arriba · −2 abajo · +31 pulso» | derivado «Diferencia sentado / de pie» por lectura, rotulada con el derivado «Momento de pie» («sin esperar» si no tiene segundos) | derivado |

**Acciones:** «Ya estoy de pie» → arranca el temporizador y programa una alarma exacta por cada tiempo del plan (como los recordatorios, ADR-005; sin permisos nuevos). En cada tiempo suena, vibra y abre (o notifica, con la pantalla apagada) el mini-formulario de esa lectura; Guardar → guarda la lectura enlazada y evalúa las reglas (incluidas las de diferencia, partida y tendencia, H-054-H-056; si alguna se cumple → [seguridad-alertas.md](seguridad-alertas.md) y el temporizador sigue en segundo plano). Si una lectura no se guarda antes del siguiente tiempo, el siguiente aviso se muestra igual; la anterior queda sin hacer. «Anotar sin esperar» → mini-formulario sin segundos. «Cancelar» → retira las alarmas pendientes; las lecturas ya guardadas se quedan. «Ahora no» → termina sin lectura de pie. Tras la última lectura → resultado con las diferencias, sin colores.
**Estados:** vacío (el plan no tiene tiempos de pie → solo «Anotar sin esperar» y «Ahora no», con el texto «Tu plan no tiene tiempos para medir de pie.») · cargando (Guardar deshabilitado hasta confirmar escritura) · error (validaciones del formulario de presión; fallo al guardar conserva lo tecleado y el temporizador sigue; el teléfono no aceptó la alarma → FranjaInformativa «El aviso puede no sonar con la pantalla apagada: mantén la app abierta») · sin permisos (bloqueo activo: el aviso llega como notificación y el formulario se abre tras desbloquear, sin perder los segundos; notificaciones denegadas → FranjaInformativa con botón a ajustes).

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
│ Cantidad [ 1 ] [pizca ▾]         │  ← g · pizca · ¼ de cucharadita ·
│                                  │    cucharadita rasa
│ Una pizca son unos 0.3 g. Tu meta│  ← solo con medida imprecisa
│ de hoy equivale a unas 8 pizcas  │
│ o media cucharadita.             │
│ = 0.3 g · sodio 0.12 g           │  ← sodio solo si el tipo tiene factor
│ Hoy: Sal 0.9 de 2.5 g            │
│ Nota: [______________]           │
│                     [ Guardar ]  │
└──────────────────────────────────┘
   con Agua elegida:
│ Cantidad [ 1 ] [vaso ▾] = 250 mL │
│ [✔] Lleva sal disuelta           │
│   Sal [ 1 ] [¼ de cucharadita ▾] │
│   = 1.25 g · sodio 0.49 g        │
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Tipos | `intake_type.name`, `intake_type.unit` | lectura |
| Tipo elegido | `intake.intake_type_code` | escritura |
| Fecha y hora | `intake.taken_at` | escritura |
| Cantidad tecleada | `intake.amount_entered` | escritura |
| Selector de medida (unidad del tipo + sus medidas caseras sin `deleted_at`, en orden de `position`) | `intake.unit_entered` (`intake_type.unit` o `household_unit.code`); opciones `household_unit.name`; default: la del BotonRapido si se llegó por pulsación larga, si no `intake_type.unit` | escritura |
| Equivalente «= 0.3 g» (solo con una medida) | `intake.amount` = `amount_entered` × `household_unit.equivalent`, `intake.unit` = `intake_type.unit` | derivado al guardar y almacenado (D-22) |
| Línea de la medida imprecisa (fija mientras está elegida) | `household_unit.imprecise`, `household_unit.equivalent`; con meta: `intake_goal.daily_amount` ÷ `equivalent` («unas 8 pizcas») y en la medida no imprecisa de mayor equivalencia del tipo, redondeada a cuartos («media cucharadita») | derivado |
| Sodio | sodio (`intake.amount` × `intake_type.sodium_factor`) | derivado |
| Casilla «Lleva sal disuelta» (solo con un tipo sin factor de sodio y si existe un tipo con factor; nombra ese tipo) | — | acción (muestra la segunda fila) |
| Segunda fila: tipo, cantidad y medida de lo disuelto | segunda `intake`: `intake_type_code`, `amount_entered`, `unit_entered`, `amount`, `unit`; las dos con el mismo `intake.group_id` y `taken_at` | escritura (H-051) |
| Total del día frente a meta | totales del día, `intake_goal.daily_amount` | derivado / lectura |
| Nota | `intake.note` | escritura |

**Acciones:** Guardar → guarda la ingesta (o las dos de «Lleva sal disuelta», todo o nada, con una sola evaluación de reglas) y vuelve al origen con Snackbar («Agua con sal guardada»). Cambiar la medida recalcula el equivalente al momento; cambiar la equivalencia en Catálogos no toca lo ya anotado. Si `intake_type.breaks_fast` y el ayuno está `in_progress`: diálogo «Anotaste caldo. ¿Estás terminando el ayuno?» → «Sí, terminar» ([fin-terminar.md](fin-terminar.md), con la ingesta ya guardada) / «No, sigo» (se guarda y el ayuno sigue).
**Estados:** como los demás; sin colores de juicio en el total. Error adicional: la medida elegida se quitó en Catálogos con el formulario abierto → «Esa medida ya no existe. Elige otra.» (lo tecleado se conserva).

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
- **Hick (EV-006):** el formulario de líquido sigue con 1 acción (Guardar); el selector de medida es un desplegable junto a la cantidad y «Lleva sal disuelta» se pliega. Temporizador de pie: 1 principal por paso (Ya estoy de pie → Guardar → Hecho) + secundarias. ✔
- **Fitts (EV-006):** el Temporizador ocupa el centro con cifras grandes y el formulario de cada lectura se abre solo al sonar: de pie, con el manguito puesto, no hay que buscar nada. ✔
- **Miller:** la rejilla puede superar 9 botones si se activan muchas mediciones → se agrupan en «Mediciones» y «Otros registros» (bloques ≤ 9). ✔ con condición.

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Escalas 0-10** (hambre, energía, ánimo, claridad): ¿botones sueltos en la rejilla o agrupadas en un único formulario «Cómo me siento» que guarda varias mediciones a la vez? Propuesta: formulario agrupado, para no saturar la rejilla.
- **«Al despertar»:** ¿se marca solo a mano o se propone marcado si es la primera lectura tras el fin del silencio nocturno? Propuesta: propuesto automáticamente, editable.

*Cambio EV-005 (2026-10-23):* las mediciones con palabras (`metric_option`; de fábrica Hambre, Energía, Ánimo y Claridad mental) se anotan tocando una palabra que guarda su número, en lugar de teclearlo (H-049). La decisión de 2026-10-08 de agrupar las escalas en un formulario «Cómo me siento» no se construyó (cada escala tiene su botón en la rejilla, D-18) y no cambia aquí. Motivo: el humano quiere elegir cómo se siente con palabras. Mockup: https://claude.ai/artifact/ERYg3yGCNPWG7x6tdGkjCq

*Cambio EV-006 (2026-10-26):* el formulario de líquido permite anotar en la unidad del tipo o en una medida casera (vaso, taza, cucharadita rasa, ¼ de cucharadita, pizca), muestra el equivalente y, con una medida imprecisa, una línea fija que la traduce sin juicio (H-050, decisión 5 del reporte). Con un líquido sin sodio, «Lleva sal disuelta» anota las dos ingestas enlazadas en una sola entrada (H-051). Tras guardar la presión sentado, la antigua pregunta «¿Ahora de pie?» se sustituye por el **Temporizador de pie**: lecturas a los tiempos del plan con sus segundos de pie calculados, «Anotar sin esperar» y el resultado con las diferencias de las tres cifras de cada lectura (H-009, H-053). Motivo: en el primer ayuno real no se anotaron agua ni sal y la presión de pie se tomó a los pocos segundos de levantarse. La casilla nombra el tipo con factor de sodio («sal»); si hay varios, la segunda fila deja elegir entre ellos.

*Cambio EV-006, construcción (2026-10-26):* si la toma sentado dispara una alerta, se muestra primero la alerta y no se abre el Temporizador (la decisión de seguridad va antes). Si la app se cerró, el aviso de cada tiempo reabre el Temporizador con su hora de inicio. Una medida cuyo nombre empieza por cifra no lleva un 1 delante («¼ de cucharadita»; con otra cantidad, «2 × ¼ de cucharadita»), para no leerse como 1,25.
