# 05 — UX-UI

## Propósito

`specs/UX-UI/` define cómo el usuario interactúa con el sistema: flujos, pantallas y wireframes textuales, validados contra las **Laws of UX** (Yablonski) para decidir con evidencia de comportamiento humano, no con gusto estético. Si el proyecto no tiene UI directa (backend puro, agente), el directorio contiene solo un README con "(no aplica)".

## Forma

Directorio con **un archivo por flujo** (`[modulo]-[slug].md`) y un `README.md` índice con el **mapa de navegación** global (Mermaid).

## Plantilla de flujo

```markdown
# Flujo: [nombre]

**Historias que cubre:** H-001, H-004
**Entrada:** [desde dónde llega el usuario] · **Salida:** [a dónde va]

## Pantallas

### Pantalla: [nombre]
[Wireframe textual: cajas ASCII o lista jerárquica de componentes]

**Campos:** usa nombres canónicos del modelo —
| Elemento UI | Campo canónico | Modo |
|---|---|---|
| input "Correo" | `usuario.email` | escritura |

**Acciones:** [botones/gestos y a qué pantalla o endpoint llevan]
**Estados:** vacío · cargando · error · éxito

## Validación Laws of UX
- Hick: ¿≤3 acciones principales visibles?
- Jakob: ¿el patrón es familiar (formulario/lista/detalle estándar)?
- Fitts: ¿los objetivos frecuentes son grandes y cercanos?
- Miller: ¿la información se agrupa en bloques de 5-9 elementos?
```

## Reglas

1. **Nombres canónicos obligatorios.** Cada campo mostrado o capturado se mapea a `entidad.campo`. Un elemento de UI sin campo canónico es un nudo N5 en el Tejido.
2. **Todo flujo cita sus historias.** Un flujo sin historia es diseño especulativo.
3. **Estados no felices siempre**: vacío, cargando, error, sin permisos.
4. Consistencia visual: los componentes referencian `specs/GUIA-ESTILO.md`, no estilos ad-hoc.

## Prompt sugerido

> Lee las historias de `specs/HISTORIAS/` y el modelo. Diseña `specs/UX-UI/` según `metodo/05-UX-UI.md`: un archivo por flujo con wireframes textuales, tabla de mapeo elemento → campo canónico, estados no felices y validación contra Laws of UX. Crea el README con el mapa de navegación. Pregúntame las decisiones de producto que las historias no resuelvan.
