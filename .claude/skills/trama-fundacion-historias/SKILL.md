---
name: trama-fundacion-historias
description: Paso 2 de la Fundación (doc 16). Levanta las historias de usuario del proyecto por entrevista guiada por módulos — narrativa Como/quiero/para en lenguaje del dominio, criterios verificables, consideraciones de seguridad evidentes y matriz CRUD vacía — y materializa specs/HISTORIAS/ con un archivo por historia y README índice. Usarlo tras completar CLAUDE.md (paso 1) en un proyecto sin código. NO usarlo si ya existe código (usar /trama-adopcion) ni para añadir historias a un specs/ ya operativo (usar /trama-evolucion).
---

# Fundación — Paso 2: Historias

Doctrina: `metodo/03-HISTORIAS.md`. Es la primera materialización de `specs/` — las historias son el artefacto raíz (P2).

## Precondición

`CLAUDE.md` completado sin placeholders (paso 1) y sin código de dominio previo. Si `specs/HISTORIAS/` ya existe poblado: detenerse y redirigir a `/trama-evolucion`.

## Secuencia

1. **Entrevistar por módulos.** Partir de la descripción del proyecto en `CLAUDE.md`; para cada capacidad que el humano describa, redactar la historia con la plantilla del doc 03: narrativa `Como/quiero/para`, prioridad y estimación sugeridas, criterios verificables, consideraciones de seguridad evidentes (o "(ninguna evidente)").
2. **Vigilar el vocabulario.** Señalar sinónimos entre historias en el momento ("aquí dices 'cliente', antes dijiste 'comprador'") — el glosario del modelo los reconciliará; no resolverlos aquí en silencio.
3. **Materializar.** Crear `specs/HISTORIAS/` con un archivo por historia (`[modulo]-[NNN]-[slug].md`, id `H-[NNN]` global e inmutable) y el `README.md` índice (id, título, módulo, prioridad, estado `borrador`). Épicas solo si 5+ historias comparten módulo y objetivo.

## Reglas duras

- **Lenguaje del dominio:** ningún criterio menciona `entidad.campo` ni nombres técnicos — el mapeo es tarea del Tejido.
- **La matriz CRUD nace vacía** (sección "*Pendiente — se puebla en el Tejido.*"): poblarla ahora invierte el proceso.
- No inventar campos, entidades ni pantallas: eso llega en los pasos 3-4.

## Entregable y pausa

Verificar las compuertas de calidad del doc 03 (beneficio explícito, cero nombres técnicos, índice completo sin ids duplicados, seguridad evidente anotada) y **esperar validación del humano** — las historias validadas son el insumo de `/trama-fundacion-modelo`.
