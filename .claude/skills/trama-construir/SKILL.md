---
name: trama-construir
description: Ejecuta la construcción autónoma de una o más historias del Método TRAMA con el ciclo hilar → aplicar → verificar. Requiere Tejido cerrado (compuerta abierta) y Diseño existente; los artefactos de specs/ son la única fuente de verdad — no se redacta proposal adicional. Úsalo para delegar la implementación completa de historias ya diseñadas sin supervisión paso a paso. NO usarlo si las historias no están en estado "tejida" o si hay nudos N1/N4 abiertos sobre su alcance.
---

# Construir — ciclo autónomo hilar → aplicar → verificar

Doctrina: `metodo/09-CONSTRUCCION.md` y `metodo/10-PRUEBAS.md`.

## Paso 0 — Verificación de prerrequisitos (abortar si falla)

1. Las historias objetivo existen, con **matriz CRUD poblada** (estado "tejida" en el índice).
2. `specs/DISENO.md` y `specs/ARQUITECTURA.md` existen y cubren el área.
3. Sin nudos N1/N4 abiertos sobre el alcance (revisar el último reporte de Tejido; ante la duda, correr `/trama-tejer` parcial).
4. `specs/EVOLUCIONES-EN-CURSO.md` no tiene otra evolución activa sobre los mismos artefactos.

Si algo falla: reportar qué falta y detenerse — no construir sobre compuerta cerrada.

## Paso 1 — Hilar

Por cada historia, derivar la lista ordenada de tareas concretas. Cada tarea declara: archivos a tocar, campos canónicos involucrados (de la matriz), operación (C/R/U/D), y la prueba que la verificará. Presentar el hilo de tareas como plan interno (no requiere aprobación si los prerrequisitos pasaron).

## Paso 2 — Aplicar

Ejecutar la siguiente tarea siguiendo el checklist de features de `ARQUITECTURA.md`, las rules de `.claude/rules/` y los principios S1-S8 de `metodo/SEGURIDAD.md`. Si la tarea modifica código existente no trivial: bloque de comprensión (doc 09) con citas archivo:línea antes de editar.

## Paso 3 — Verificar

Tras cada tarea: correr pruebas y lint del proyecto; contrastar el resultado contra la matriz CRUD (¿la operación declarada ocurre y persiste?) y el criterio de aceptación. Rojo → corregir antes de avanzar. Repetir 2-3 hasta agotar el hilo.

## Paso 4 — Cierre por historia

1. Suite completa de la historia en verde (incluye casos negativos de seguridad si el contrato declara auth).
2. Actualizar el índice de HISTORIAS (estado → "entregada").
3. Si hubo decisiones de implementación no triviales: anotarlas en el log del artefacto correspondiente.
4. Si la operación construida califica como transacción clave (doc 12): documentarla en `specs/TRANSACCIONES-CLAVE/`.
5. Reporte final: tareas ejecutadas, pruebas agregadas, desviaciones registradas.

## Paralelización

Historias sin entidades compartidas (verificar en los Mapas de Consumo) pueden construirse en paralelo con subagentes, cada uno con su propia instancia de este ciclo.

## Condiciones de parada (consultar al humano)

- Decisión de producto que los artefactos no resuelven.
- Contradicción entre artefactos (p.ej. contrato vs. matriz).
- Nudo descubierto en vuelo.
- Una prueba exige cambiar un criterio de aceptación.
- Violación arquitectónica inevitable sin decisión nueva.
