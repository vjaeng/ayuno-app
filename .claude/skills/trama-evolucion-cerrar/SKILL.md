---
name: trama-evolucion-cerrar
description: FASE 5 del workflow de Evolución (paso 7 del doc 14). Actualiza los logs de decisiones de los artefactos modificados, cierra la memoria EV-NNN con metadatos y Resultado final, agrega la fila al índice histórico (EVOLUCIONES-HISTORICAS/README.md), elimina la fila operativa de EVOLUCIONES-EN-CURSO.md y registra en CLAUDE.md las decisiones técnicas de alcance de proyecto. Usarlo al terminar construcción y pruebas (FASE 4). NO usarlo si la evolución no completó la construcción con pruebas en verde.
---

# Evolución — FASE 5: Cierre

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md`, paso 7.

## Precondición

Memoria EV-NNN con "Paso 6 — Construcción y pruebas" poblado y suite en verde; estado `En Construcción`.

## Procedimiento

1. **Logs de decisiones.** Por cada artefacto modificado en la evolución, agregar al log del artefacto la entrada con fecha, referencia `EV-NNN` y el **porqué** del cambio (git ya registra el qué).
2. **Cerrar la memoria.** Completar metadatos (fecha de cierre, estado final) y redactar "Resultado final": qué quedó entregado, desviaciones respecto al plan, deuda registrada, bloqueos habidos.
3. **Índice histórico.** Agregar la fila (id, nombre, fechas, resumen de una línea) a `specs/EVOLUCIONES-HISTORICAS/README.md`.
4. **Coordinación.** Eliminar la fila de `specs/EVOLUCIONES-EN-CURSO.md`.
5. **Memoria del proyecto.** Si la evolución tomó decisiones técnicas de alcance de proyecto (nueva dependencia estructural, patrón nuevo, cambio de convención): agregarlas a la tabla "Decisiones Técnicas" de `CLAUDE.md` y actualizar "Estado Actual" si cambió de fase/módulo.
6. Presentar resumen de cierre al humano.

## Regla

El cierre es atómico: o se completan los 5 puntos, o no se cierra (una memoria cerrada con fila zombie en EN-CURSO, o viceversa, rompe la coordinación del método).
