# Flujo: Plantillas

**Historias que cubre:** H-001, H-002, H-003, H-014, H-031 (definir el plan de vuelta a comer), H-052 (preguntar tras N horas), H-053 (tiempos de pie), H-057 (arreglar el plan de un ayuno desde la revisión previa)
**Entrada:** Más → Plantillas; Crear ayuno → «Crear una plantilla»; Revisión antes de empezar → «Arreglar ›» de una meta (plan del ayuno, solo la subpantalla Líquidos y metas) · **Salida:** lista de plantillas; editor de reglas; Revisión antes de empezar

## Pantallas

### Pantalla: Lista de plantillas

```
┌──────────────────────────────────┐
│ ← Plantillas            [ + ]    │
│ Ayuno de 57 horas · 57 h      ⋮  │
│ Ayuno de 36 horas · 36 h      ⋮  │
│ Ejemplo (sin valores clínicos)⋮  │
│──────────────────────────────────│
│ Borradas (2) ›                   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `template.name` | lectura |
| Duración objetivo | `template.target_hours` | lectura |
| Menú ⋮: Duplicar / Borrar | — | acción (duplicar copia todo el plan; borrar es blando) |
| «Borradas» → Restaurar | (control `deleted_at`) | acción |

**Acciones:** + → editor vacío · tocar → editor · Duplicar · Borrar (confirmación: «Los ayunos creados desde ella no cambian») · Restaurar.
**Estados:** vacío («Aún no tienes plantillas» + «Crear» + «Ver la de ejemplo» si fue borrada) · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Editor de plantilla

Secciones plegables; cada una se edita en su propia subpantalla.

```
┌──────────────────────────────────┐
│ ← Plantilla              Guardar │
│ Nombre [Ayuno de 57 horas    ]   │
│ Objetivo (horas) [ 57 ]          │
│ Hitos [12] [24] [36] [48] [+]    │
│──────────────────────────────────│
│ › Etapas            4 de 4       │
│ › Mediciones        6 activas    │
│ › Productos         3 siguen     │
│ › Líquidos y metas  2 metas      │
│ › Reglas de alerta  4 activas    │
│ › Vuelta a comer    3 pasos      │
│ › Lista previa      5 elementos  │
│ › Avisos fijos      2 avisos     │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `template.name` | escritura (obligatorio) |
| Objetivo | `template.target_hours` | escritura (> 0) |
| Hitos | `template.milestone_hours` | escritura |
| Resúmenes de sección | conteos de entidades del plan | derivado |

#### Subpantalla: Etapas
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor por etapa (preparación, ayuno, vuelta a comer, posterior) | `stage.kind` (existencia de la fila) | escritura |
| Duración | `stage.duration_value` | escritura (no aplica a «ayuno») |
| Unidad (días / horas) | `stage.duration_unit` | lectura (fija por etapa) |

#### Subpantalla: Mediciones
```
│ [✔] Glucosa                      │
│     Ayuno: 07:00 12:00 18:00 22:00 [+]
│     Posterior: 07:00          [+]│
│ [✔] Presión arterial             │
│     Ayuno: 07:00 21:00        [+]│
│     De pie a los: 60 s · 180 s [+]│
│ [ ] Cetonas                      │
```
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor por tipo | `plan_metric.metric_type_code` (existencia de la fila); lista de `metric_type.name` activos | escritura |
| Horarios por etapa | `metric_schedule.stage_kind`, `metric_schedule.time_of_day` | escritura |
| «De pie a los: …» (solo presión arterial) | `plan_metric.standing_check_seconds` (cada uno > 0, sin repetir; se ordenan solos; vacío = sin temporizador) | escritura |

#### Subpantalla: Productos
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Producto | `product.name`, `product_schedule.time_of_day`, `product_schedule.amount` | lectura |
| Sigue / Sale / Opcional | `plan_product.during_fast` | escritura |
| Activar opcional | `plan_product.optional_enabled` | escritura |
| Motivo | `plan_product.reason` | escritura (opcional) |

#### Subpantalla: Líquidos y metas
```
│ Agua    Meta [ 2000 ] mL         │
│         Preguntar tras [ 6 ] h   │
│         sin anotar               │
│ Sal     Meta [ ____ ] g  por llenar│
│         Preguntar tras [ 6 ] h   │
│ Potasio [+ Meta]                 │
```
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Tipo de ingesta | `intake_type.name` | lectura |
| Meta (existencia de la fila) | `intake_goal.intake_type_code` | escritura («+ Meta» / quitar) |
| Meta diaria | `intake_goal.daily_amount` | escritura (opcional: vacía = «por llenar», EV-006) |
| Unidad de la meta | `intake_goal.unit` | escritura |
| «por llenar» | `intake_goal.daily_amount` nulo | lectura |
| «Preguntar tras N h sin anotar» | `intake_goal.nudge_after_hours` (> 0; vacío = no pregunta) | escritura (opcional) |
| Franja «Estás cambiando el plan de este ayuno, no la plantilla» (solo al llegar desde la revisión previa) | — | lectura |

#### Subpantalla: Reglas de alerta
Lista de reglas (`rule.name`, `rule.level`, `rule.active`) → [reglas-editar.md](reglas-editar.md).

#### Subpantalla: Vuelta a comer
```
│ Pasos tras el fin                │
│  Una taza de caldo · desde 0 h · │
│  cada 60 min durante 4 h      ✎  │
│  Comida blanda · a las 6 h    ✎  │
│ Reintroducción                   │
│  Día 1 · Vuelven los suplementos✎│
│  [+ Paso] [+ Reintroducción]     │
```
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Tipo de paso | `refeed_step.kind` | escritura |
| Descripción | `refeed_step.description` | escritura |
| A las N horas | `refeed_step.offset_minutes` | escritura (en horas y minutos) |
| Cada N minutos | `refeed_step.repeat_every_minutes` | escritura (opcional) |
| Durante N horas | `refeed_step.repeat_for_minutes` | escritura (obligatorio si repite) |
| Día N | `refeed_step.day_offset` | escritura (reintroducción) |

#### Subpantalla: Lista previa
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Texto | `checklist_item.text` | escritura |

#### Subpantalla: Avisos fijos
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Texto | `stage_notice.text` | escritura |
| Etapas en que se muestra | `stage_notice.stage_kinds` | escritura (al menos una) |

**Acciones:** Guardar (valida y guarda la plantilla completa; no afecta a ayunos ya creados) · Atrás con cambios sin guardar → «¿Descartar cambios?».
**Estados:** vacío (plantilla nueva: secciones con «Sin configurar») · cargando · error (nombre vacío, objetivo ≤ 0, repetición sin duración, aviso sin etapas, tiempo de pie ≤ 0 o repetido, horas de pregunta ≤ 0: mensaje en el campo; fallo al guardar: se conserva lo editado) · sin permisos (bloqueo activo).

**Plan de un ayuno (desde la revisión previa, EV-006):** «Arreglar ›» abre la misma subpantalla o el mismo editor de regla sobre la copia del plan del ayuno planificado (D-03), con la FranjaInformativa «Estás cambiando el plan de este ayuno, no la plantilla». Guardar escribe solo en el plan del ayuno y vuelve a la revisión. La plantilla no cambia.

## Validación Laws of UX
- **Hick:** 1 acción principal (Guardar); la complejidad se reparte en 8 secciones de una sola tarea cada una. ✔
- **Jakob:** lista de ajustes con subpantallas, patrón de «Ajustes» del sistema. ✔
- **Fitts:** filas completas tocables; Guardar arriba (patrón de edición) — se acepta porque no es acción frecuente en ayunas. ✔
- **Miller:** 8 secciones (dentro de 5-9). ✔

## Decisiones de producto

*Implementación (H-003, 2026-10-10):* Duplicar y Borrar en el menú ⋮ de cada fila; «Borradas (n)» despliega las borradas con «Restaurar» (también visible si la lista queda vacía, para recuperar la de ejemplo).

*Implementación (tanda 3, 2026-10-10):* la subpantalla «Vuelta a comer» se construye como sección del mismo editor (como el resto de secciones), visible al activar la etapa. Cada paso se valida al añadirlo.

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿El editor de plantilla sirve también para editar el plan copiado de un ayuno? Ligado a la pregunta de [ayuno-crear.md](ayuno-crear.md). Propuesta: sí, mismo editor con un aviso «Estás editando el plan de este ayuno, no la plantilla».
- Duplicar: ¿nombre por defecto «Copia de …»? Propuesta: sí.

*Cambio EV-006 (2026-10-26):* las metas de ingesta pueden quedar **sin cifra** («por llenar») y tener «Preguntar tras N h sin anotar» (H-002, H-052); la presión arterial tiene sus tiempos de pie (60 s y 180 s de fábrica, H-053). La plantilla de ejemplo trae metas de agua y sal sin cifra que preguntan tras 6 h, sus reglas marcadas «de ejemplo» y la regla desactivada «Sal por debajo de ___ % de la meta a las ___» (H-003). Por primera vez se edita el **plan de un ayuno** (no la plantilla), solo desde la revisión antes de empezar y solo en las partes que esta señala (reglas y metas); con ello se aplica la propuesta aceptada el 2026-10-08 («mismo editor con un aviso»), que no se había construido.
