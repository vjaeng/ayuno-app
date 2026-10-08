# Flujo: Exportar e importar

**Historias que cubre:** H-036, H-037, H-038, H-039, H-040
**Entrada:** Detalle del ayuno → «Exportar»; Más → Exportar e importar · **Salida:** hoja de compartir del sistema; o resumen de importación

El formato del archivo se especifica en Diseño (ver `specs/CONTRATO-API/README.md`); aquí solo la interacción.

## Pantallas

### Pantalla: Exportar e importar

```
┌──────────────────────────────────┐
│ ← Exportar e importar            │
│ EXPORTAR                         │
│  [ Exportar todo ]               │
│  [ ] Incluir también tablas      │
│      (archivo comprimido)        │
│  Datos personales: no incluidos ›│
│ IMPORTAR                         │
│  [ Importar archivo… ]           │
│  Plantillas, lista de productos o│
│  restaurar una copia exportada.  │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Exportar todo | todos los `fast`, `template`, catálogos y entidades dependientes | acción (lectura de todo) |
| Incluir tablas | — | acción (opción de la exportación, no se guarda) |
| Estado de datos personales | `settings.export_person_enabled` | lectura → [ajustes-generales.md](ajustes-generales.md) |
| Importar archivo | — | acción → selector de archivos del sistema |

### Pantalla: Exportar un ayuno (desde el detalle)

```
┌──────────────────────────────────┐
│ Exportar ayuno del 22 al 25 oct  │
│ Archivo: ayuno_2026-10-22_       │
│   b1e0c7d4.json                  │
│ [ ] Incluir también tablas       │
│ Datos personales: no incluidos   │
│         [ Exportar ]             │
└──────────────────────────────────┘
          ↓
   (hoja de compartir del sistema)
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Nombre del archivo | `fast.clock_start_actual` (fecha) + identificador del ayuno (control `id`, 8 primeros caracteres) | derivado |
| Contenido | `fast` y todas sus entradas y su plan, catálogos, `fast.tz_offset` en cabecera; `person_profile.*` solo si `settings.export_person_enabled` | lectura |
| Datos personales | `settings.export_person_enabled` | lectura |

**Acciones:** Exportar → genera el archivo en el teléfono y abre la hoja de compartir; la app no envía nada por su cuenta. Ayuno `planned` (sin inicio real): el nombre usa la fecha del inicio previsto (ver decisión abierta).
**Estados:** vacío (sin ayunos: «Exportar todo» deshabilitado con «No hay nada que exportar») · cargando (generando: indicador con progreso; no bloquea el resto de la app) · error (no se pudo generar o falta espacio: mensaje llano, nada se comparte) · sin permisos (bloqueo activo: se pide huella o PIN antes de exportar, siempre).

### Pantalla: Resumen de importación

```
┌──────────────────────────────────┐
│ Importar ayuno_2026-10-22_…json  │
│ Contiene: 1 ayuno, 2 plantillas, │
│ 12 productos                     │
│ Nuevos: 1 plantilla · Ya estaban:│
│ 1 ayuno, 1 plantilla (se         │
│ actualizarán, sin duplicar)      │
│ Campos ignorados (2):            │
│   fasts.weather, notes.mood      │
│ [Cancelar]        [ Importar ]   │
└──────────────────────────────────┘
```

**Campos:**
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| Recuento por tipo | entidades contenidas en el archivo | derivado |
| Nuevos / ya existentes | comparación por identificador (control `id`) y por `product.code` | derivado |
| Campos ignorados | claves desconocidas del archivo | derivado |

**Acciones:** Importar → escribe todo o nada (si algo falla, no queda ninguna importación parcial) · Cancelar. Tras importar: «Importado: 1 ayuno, 2 plantillas…».
**Estados:** vacío (archivo sin contenido reconocible: «Este archivo no contiene datos de la app») · cargando (validando y luego importando) · error (archivo inválido: «No es un archivo de la app o está dañado», nada se modifica; versión de formato mayor que la soportada: «Este archivo es de una versión más nueva de la app. Actualiza la app para importarlo.»; fallo a mitad: se revierte todo) · sin permisos (acceso a archivos denegado: aviso con botón a ajustes; bloqueo activo: huella o PIN antes de importar).

## Validación Laws of UX
- **Hick:** 2 acciones principales (Exportar todo, Importar). ✔
- **Jakob:** hoja de compartir y selector de archivos nativos. ✔
- **Fitts:** botones de ancho completo. ✔
- **Miller:** 2 bloques; el resumen de importación ≤ 5 líneas. ✔

## Decisiones de producto

*Propuestas aceptadas por el humano el 2026-10-08.*
- **Nombre del archivo de un ayuno sin inicio real** (planificado o desde cero sin empezar): H-036 usa la fecha del inicio real. Propuesta: usar la del inicio previsto.
- **Conflicto al restaurar:** si una entrada del archivo existe con otro contenido (editada en el teléfono después de exportar), ¿gana el archivo o el teléfono? Propuesta: gana la de `updated_at` más reciente y el resumen lo indica.
- **Exportar todo — nombre del archivo:** H-036 solo define el de un ayuno. Propuesta: `ayuno_todo_<AAAA-MM-DD de hoy>.json`.
