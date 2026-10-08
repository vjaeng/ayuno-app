# Flujo: Ajustes generales

**Historias que cubre:** H-017 (silencio nocturno), H-029, H-038 (datos personales), H-042 (bloqueo), H-043
**Entrada:** Más → Ajustes; Seguridad → «Configura un contacto de emergencia» · **Salida:** Más

## Pantallas

### Pantalla: Ajustes

```
┌──────────────────────────────────┐
│ ← Ajustes                        │
│ RECORDATORIOS                    │
│  Silencio nocturno  [23:00]–[06:30]│
│  (los recordatorios de tus reglas│
│   suenan igual)                  │
│ UNIDADES                         │
│  Glucosa     (●)mg/dL (○)mmol/L  │
│  Peso        (●)kg    (○)lb      │
│  Cintura     (●)cm    (○)in      │
│  Temperatura (●)°C    (○)°F      │
│ CONTACTO DE EMERGENCIA           │
│  Nombre [Ana]  Teléfono [+57 …]  │
│ PRIVACIDAD                       │
│  Bloquear con huella o PIN [○]   │
│  Datos personales ›              │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Inicio del silencio | `settings.quiet_start` | escritura (opcional) |
| Fin del silencio | `settings.quiet_end` | escritura (obligatorio si hay inicio) |
| Unidad por defecto de cada medición con alternativa | `metric_type.entry_unit` (opciones `metric_type.unit` / `metric_type.alt_unit`) | escritura |
| Nombre del contacto | `emergency_contact.name` | escritura |
| Teléfono del contacto | `emergency_contact.phone` | escritura (formato telefónico) |
| Bloquear con huella o PIN | `settings.app_lock_enabled` | escritura (al activar, el sistema pide autenticarse) |
| Datos personales › | — | acción → Datos personales |

**Acciones:** cada cambio se guarda al instante, con confirmación breve. Cambiar una unidad no altera entradas guardadas (texto de ayuda bajo el bloque).
**Estados:** vacío (sin contacto: campos vacíos con «Opcional») · cargando · error (teléfono inválido; fin de silencio sin inicio; fallo al guardar) · sin permisos (activar el bloqueo sin huella ni PIN configurados en el teléfono → «Configura primero un bloqueo en el teléfono» con botón a ajustes del sistema; elegir contacto desde la agenda con permiso denegado → se escribe a mano).

### Pantalla: Datos personales

```
┌──────────────────────────────────┐
│ ← Datos personales               │
│ Incluir en el archivo exportado  │
│                            [○]   │
│ Por defecto el archivo no lleva  │
│ tu identidad.                    │
│ Nombre completo [__________]     │
│ Documento de identidad [_____]   │
│ Fecha de nacimiento [________]   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Incluir en el archivo | `settings.export_person_enabled` | escritura (default no) |
| Nombre completo | `person_profile.full_name` | escritura |
| Documento de identidad | `person_profile.id_number` | escritura (se muestra enmascarado salvo al editar) |
| Fecha de nacimiento | `person_profile.birth_date` | escritura |

**Acciones:** guardado al instante. Activar la inclusión con todos los campos vacíos → aviso «No hay datos que incluir».
**Estados:** vacío (todos los campos vacíos, interruptor apagado) · cargando · error · sin permisos (esta pantalla **siempre** pide huella o PIN si el bloqueo está activo, incluso con la app ya desbloqueada hace poco — campo Crítico).

## Validación Laws of UX
- **Hick:** sin acción principal única: ajustes independientes, cada uno de un toque. ✔
- **Jakob:** pantalla de ajustes agrupada por secciones, patrón del sistema. ✔
- **Fitts:** controles ≥ 48 pt. ✔
- **Miller:** 4 secciones; la mayor tiene 4 elementos. ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Unidades:** solo se listan las mediciones con unidad alternativa (glucosa, peso, cintura, temperatura). ¿Se añaden más precargadas con alternativa? Propuesta: no por ahora.
- **Contacto desde la agenda:** ¿se permite elegirlo de los contactos del teléfono? Propuesta: sí, opcional; implica pedir permiso de agenda (solo lectura del contacto elegido).
