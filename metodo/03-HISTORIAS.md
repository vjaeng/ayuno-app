# 03 — Historias (los hilos)

## Propósito

Las historias son el artefacto raíz del método (principio P2): capturan **valor para el usuario en lenguaje del dominio**. De ellas se deriva todo lo demás. Una historia = un valor entregado; si no puedes explicar el beneficio, es una tarea técnica, no una historia.

## Forma

Directorio `specs/HISTORIAS/` con **un archivo por historia** y un `README.md` índice.

- Nombre de archivo: `[modulo]-[NNN]-[slug].md` (p.ej. `auth-001-registro.md`).
- Identificador interno: `H-[NNN]` global e inmutable.
- El índice lista: id, título, módulo, prioridad, estado (borrador / tejida / en construcción / entregada).

## Plantilla de historia

```markdown
## H-001: [Título orientado a valor]

**Como** [tipo de usuario],
**quiero** [acción en lenguaje del dominio],
**para** [beneficio].

**Prioridad:** alta | media | baja · **Estimación:** S | M | L

### Criterios de Aceptación
- [ ] [Criterio verificable, en lenguaje del dominio — sin `entidad.campo`]
- [ ] ...

### Consideraciones de Seguridad
[Solo si son evidentes desde los criterios: datos sensibles, permisos, abuso. Si no: "(ninguna evidente)".]

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
```

## Reglas de redacción

1. **Lenguaje del dominio en la narrativa.** Los criterios hablan de "correo del miembro", "rol", "fecha límite" — nunca de `user.email`. El mapeo concepto → campo canónico es tarea del Tejido, no de la redacción.
2. **Vocabulario consistente.** Si dos historias llaman distinto al mismo concepto ("cliente" / "comprador"), anotarlo: el glosario del modelo lo reconciliará y el Tejido lo detectará como nudo N3.
3. **Criterios verificables.** Cada criterio debe poder convertirse en al menos un caso de prueba. "El sistema es rápido" no es criterio; "la búsqueda responde en <2s con 10k registros" sí.
4. **Épicas (opcional).** Si 5+ historias comparten módulo y objetivo, agruparlas con un archivo de épica que solo lista sus historias — la épica no tiene criterios propios.
5. **La matriz CRUD nace vacía.** Poblarla al redactar invierte el proceso: el modelo aún no existe.

## Prompt sugerido (levantar historias en Fundación)

> Vamos a levantar las historias de usuario del proyecto descrito en CLAUDE.md. Entrevístame por módulos: para cada capacidad que yo describa, redacta la historia con la plantilla de `metodo/03-HISTORIAS.md`, en lenguaje del dominio, con criterios verificables. Señálame sinónimos entre historias y sugiere prioridad. Crea `specs/HISTORIAS/` con un archivo por historia y el README índice. No inventes campos técnicos: eso llega después.

## Compuertas de calidad

Antes de pasar al modelo (Fundación, paso 3):

- [ ] Toda historia tiene beneficio explícito ("para...").
- [ ] Ningún criterio menciona nombres técnicos `entidad.campo`.
- [ ] El índice está completo y sin ids duplicados.
- [ ] Las consideraciones de seguridad evidentes están anotadas.
