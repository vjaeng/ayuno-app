# Flujo: Reglas de alerta

**Historias que cubre:** H-019, H-020, H-021, H-022, H-023, H-024, H-025, H-026
**Entrada:** Editor de plantilla → Reglas de alerta · **Salida:** Editor de plantilla

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
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Interruptor | `rule.active` | escritura |
| Nombre | `rule.name` | lectura |
| Nivel | `rule.level` | lectura |

**Acciones:** + → editor vacío · tocar → editor. Las reglas no se borran: se desactivan con el interruptor (H-019).
**Estados:** vacío («Esta plantilla no tiene reglas. La app no trae umbrales: tú decides cuándo avisar.» + «Crear regla») · cargando · error · sin permisos (bloqueo activo).

### Pantalla: Editor de regla

```
┌──────────────────────────────────┐
│ ← Regla                  Guardar │
│ Nombre [Glucosa baja sostenida]  │
│──────────────────────────────────│
│ SI                               │
│ (●) Una medición  (○) Síntoma de │
│ alarma  (○) Total del día bajo   │
│ la meta  (○) Botón «algo no va   │
│ bien»                            │
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

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre | `rule.name` | escritura (obligatorio) |
| Tipo de condición | `rule.condition_kind` | escritura |
| Medición | `rule.metric_type_code` | escritura (obligatorio en condición de medición) |
| Valor de la presión (si la medición es presión) | `rule.bp_component` | escritura |
| Por debajo / por encima | `rule.condition_kind` (`metric_below` / `metric_above`) | escritura |
| Umbral y unidad | `rule.threshold` (en `metric_type.unit`) | escritura (obligatorio) |
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
| Vista previa «Se leerá así» | frase compuesta de los campos anteriores | derivado |

**Acciones:** Guardar (valida según el tipo de condición) · Atrás con cambios → «¿Descartar?». Los campos irrelevantes para el tipo de condición elegido se ocultan (p. ej. «posición» solo con presión; «a la hora» solo con total del día).
**Estados:** vacío (regla nueva: tipo «Una medición» preseleccionado, campos vacíos; **sin valores sugeridos** de umbral) · cargando · error (campos obligatorios vacíos; umbral fuera del rango válido de la medición → aviso, no bloqueo; meta inexistente para el tipo de ingesta elegido → «Este líquido no tiene meta en la plantilla»; más de una regla «algo no va bien» → «Ya existe una regla para el botón»; fallo al guardar) · sin permisos (bloqueo activo).

## Validación Laws of UX
- **Hick:** 1 acción principal; las opciones se revelan por pasos (Si → Solo cuando → Entonces) y se ocultan las que no aplican. ✔
- **Jakob:** constructor de reglas «si … entonces …» (patrón de automatizaciones en móviles). ✔
- **Fitts:** controles ≥ 48 pt; selección por botones de opción grandes. ✔
- **Miller:** 3 bloques con ≤ 5 controles visibles cada uno. ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- ¿Se limita a una sola regla de tipo «algo no va bien» por plantilla? Propuesta: sí (el botón es uno).
- La condición «sostenida» con umbral «por encima» (p. ej. presión alta sostenida): el modelo lo permite. ¿Se ofrece? Propuesta: sí, simétrico a «por debajo».
