# Flujo: Seguridad y alertas

**Historias que cubre:** H-023, H-025, H-026, H-027, H-028, H-029
**Entrada:** botón de seguridad (Inicio, Hoy) o una regla que se cumple al guardar una entrada o al llegar una hora programada · **Salida:** Terminar el ayuno, o vuelta a la pantalla de origen

## Pantallas

### Pantalla: Seguridad

```
┌──────────────────────────────────┐
│ ← Seguridad                      │
│ Tus reglas activas               │
│ • Si la glucosa queda por debajo │
│   de 55 durante 20 minutos:      │
│   alerta roja. Ofrece terminar.  │
│ • Si la sistólica de pie baja de │
│   90: alerta roja.               │
│ • Si a las 14:00 llevas menos    │
│   sodio que tu meta: aviso.      │
│──────────────────────────────────│
│ [ Algo no va bien ]              │
│ [ Llamar a Ana  ☎ ]              │
│ [ Terminar el ayuno ]            │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Frase de cada regla | `rule.name`, `rule.condition_kind`, `rule.metric_type_code`, `rule.bp_component`, `rule.threshold`, `rule.sustained_minutes`, `rule.symptom_type_code`, `rule.intake_type_code`, `rule.check_time`, `rule.filter_stage_kinds`, `rule.filter_on_waking`, `rule.filter_position`, `rule.level`, `rule.action` (solo `rule.active`) | lectura (frase compuesta en lenguaje llano) |
| Botón «Algo no va bien» | regla con `rule.condition_kind = manual` | acción → dispara Alerta |
| Botón «Llamar a …» | `emergency_contact.name`, `emergency_contact.phone` | lectura → acción (llamada del sistema) |
| Botón «Terminar el ayuno» | — | acción → [fin-terminar.md](fin-terminar.md) |

**Acciones:** como arriba.
**Estados:** vacío (sin reglas activas: «No tienes reglas activas en este ayuno. Puedes añadirlas en la plantilla.» — los tres botones siguen); sin regla manual definida: «Algo no va bien» igual dispara una alerta roja genérica con el texto «Detente y valora si terminar el ayuno» (ver decisión abierta) · cargando · error (si no se pueden leer las reglas: mensaje y los botones siguen operativos) · sin permisos (llamadas no permitidas o sin contacto: el botón dice «Configura un contacto de emergencia» y lleva a [ajustes-generales.md](ajustes-generales.md); el bloqueo de la app **no** se pide para esta pantalla, ver decisión abierta).

### Pantalla: Aviso (amarillo)

```
┌──────────────────────────────────┐
│ ▲ Aviso · viernes 23 oct, 12:01  │
│ «Toma sal en agua y vuelve a     │
│ mirar el sensor en 20 minutos.»  │
│ Por: Glucosa 64 mg/dL (12:01)    │
│ Te recordaremos a las 12:21.     │
│ [ Sigo ]   [ Termino el ayuno ]  │
│ Motivo: [______________]         │
└──────────────────────────────────┘
```

### Pantalla: Alerta (roja, pantalla completa)

```
┌──────────────────────────────────┐
│█ ALERTA · sábado 24 oct, 07:22  █│
│█                                █│
│█ «Rompe con caldo tibio con sal █│
│█ y pasa a la vuelta a comer.»   █│
│█                                █│
│█ Por: Glucosa 52 (07:02) y 53   █│
│█ (07:22), bajo 55 durante 20 min█│
│█                                █│
│█ [   Termino el ayuno   ]       █│
│█ [ Sigo — motivo: ______ ]      █│
│█ [ Llamar a Ana ☎ ]             █│
└──────────────────────────────────┘
```

**Campos (aviso y alerta):**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nivel (amarillo / rojo pantalla completa) | `rule.level` | lectura |
| Hora | `alert.fired_at` | escritura al dispararse / lectura |
| Mensaje tal cual | `rule.message` | lectura |
| «Por:» lecturas que la dispararon | `alert.trigger_entry_ids` → `measurement.*`, `blood_pressure_reading.*`, `symptom.*`, `intake.*` | lectura |
| Regla | `alert.rule_id` → `rule.name` | lectura |
| Aviso de recordatorio programado | `rule.action = schedule_measurement`, `rule.action_minutes` | lectura / derivado |
| Botón «Termino el ayuno» | `alert.user_choice = end_fast`, `alert.decided_at` | escritura → [fin-terminar.md](fin-terminar.md) con motivo «regla de alerta» prellenado |
| Botón «Sigo» + motivo | `alert.user_choice = continue`, `alert.choice_reason`, `alert.decided_at` | escritura (motivo obligatorio) |
| Botón «Llamar a …» (solo alerta roja) | `emergency_contact.phone` | acción |

**Acciones:** la app **nunca** termina el ayuno sola. El aviso o la alerta no se cierran sin elegir; si la persona sale de la app, la alerta queda registrada sin decisión (`user_choice` vacío) y vuelve a mostrarse al abrir. «Termino el ayuno» solo aparece como botón principal si `rule.action = offer_end`; si no, aparece como secundario. Con `rule.action = schedule_measurement` se programa el recordatorio, que ignora el silencio nocturno.
**Estados:** vacío (n/a) · cargando (n/a: se muestra al instante) · error (si falla el guardado de la decisión, la alerta sigue en pantalla con «No se pudo guardar tu decisión. Inténtalo de nuevo»; nunca se pierde el registro de la alerta) · sin permisos (con bloqueo activo, la alerta disparada por una hora programada se muestra como notificación; la pantalla completa aparece tras desbloquear).

## Validación Laws of UX
- **Hick:** Seguridad: 3 acciones. Alerta: 2 decisiones + llamada. ✔
- **Jakob:** pantalla de emergencia tipo SOS; diálogo modal de decisión. ✔
- **Fitts:** «Termino el ayuno» es el objetivo más grande en la alerta roja; botones ≥ 56 pt. ✔
- **Miller:** reglas activas en lista; si hay más de 9, se agrupan por nivel (alertas primero). ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Bloqueo y seguridad:** con el bloqueo de la app activo, ¿la pantalla de Seguridad y la llamada de emergencia deben abrirse sin huella ni PIN? Propuesta: sí (solo muestran reglas y el contacto, no registros de salud).
- **«Algo no va bien» sin regla manual definida:** propuesta: alerta roja genérica con un texto fijo de la app. Alternativa: el botón no aparece hasta definir la regla. (La app no aconseja, por eso el texto propuesto es neutro.)
- **Alerta sin decisión:** ¿se repite la notificación cada N minutos hasta que se decida? Propuesta: sí, cada 5 minutos durante 30 minutos para alertas rojas.
