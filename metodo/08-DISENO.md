# 08 — Diseño

## Prerrequisito

Compuerta del Tejido **abierta** (sin nudos N1/N4). Diseñar sobre trazabilidad rota es diseñar sobre arena.

## Entregables

### `specs/DISENO.md` — ADRs

Registro de decisiones arquitectónicas en formato Nygard:

```markdown
## ADR-003: [Título de la decisión]
**Fecha:** AAAA-MM-DD · **Estado:** propuesta | aceptada | reemplazada por ADR-NNN

**Contexto.** [Qué fuerza la decisión: requisito, restricción, historia.]
**Decisión.** [Qué se decide, en presente.]
**Consecuencias.** [Qué se gana, qué se paga, qué se vuelve difícil.]
**Alternativas consideradas.** [Qué se descartó y por qué.]
```

Las decisiones con componente de seguridad usan prefijo **`SEG-`** (`ADR-SEG-001`) y citan el principio de `metodo/SEGURIDAD.md` que aplican. La seguridad es hilo transversal (P6), no capítulo aparte.

### `specs/ARQUITECTURA.md` — descripción operacional

1. **Capas y responsabilidades** — qué capa hace qué, y las **direcciones de dependencia permitidas** (p.ej. Controller → Service → Repository, nunca al revés).
2. **Patrones aplicados** — con nombre (Repository, Service Layer, Hexagonal, Atomic Design…) y dónde.
3. **Checklist para nuevas features** — pasos concretos para agregar una feature sin violar la arquitectura (dónde va el handler, dónde la lógica, dónde el acceso a datos, qué pruebas mínimas).
4. **Deuda arquitectónica conocida** — violaciones aceptadas, con razón y plan.
5. **Log de decisiones** — punteros a los ADRs relevantes.

## Criterios

- **SOLID como vara** para el diseño de módulos y clases; las violaciones deliberadas se documentan como deuda, no se ocultan.
- Decidir es **elegir renuncias**: un ADR sin consecuencias negativas declaradas está incompleto.
- El diseño consume la matriz CRUD: los agregados, servicios y módulos se organizan alrededor de las entidades y operaciones que las historias exigen — no de abstracciones especulativas.

## Prompt sugerido

> Con el Tejido cerrado, propón el diseño: ADRs para las decisiones estructurales (stack de persistencia, auth, organización de módulos, decisiones SEG-*) y `specs/ARQUITECTURA.md` con capas, direcciones de dependencia, patrones y checklist de features, según `metodo/08-DISENO.md`. Preséntame cada ADR con sus alternativas antes de fijarlo.
