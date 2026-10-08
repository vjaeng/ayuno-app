---
name: trama-evolucion-actualizar
description: FASE 2 del workflow de Evolución (paso 4 del doc 14). Aplica las modificaciones aprobadas del reporte de impacto a los artefactos de specs/, en orden estricto historias → modelo → UX/API → transacciones; anexa la lista de archivos modificados a la memoria EV-NNN y transiciona el estado de Análisis a En Diseño. Usarlo después de /trama-evolucion-plan con el reporte aprobado. NO usarlo si el reporte dejó colisiones sin resolver o si no hay entrada activa en EVOLUCIONES-EN-CURSO.md.
---

# Evolución — FASE 2: Actualización de artefactos

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md`, paso 4.

## Precondiciones

1. Entrada activa en `EVOLUCIONES-EN-CURSO.md` en estado `Análisis`.
2. Memoria EV-NNN con pasos 1-2 poblados y reporte de impacto **aprobado por el humano**.

## Procedimiento — orden estricto

1. **HISTORIAS.** Crear/editar las historias según el bloque Modificación: narrativa y criterios en lenguaje del dominio; matriz CRUD de las historias nuevas queda vacía (la poblará el mini-Tejido); las matrices de historias editadas se marcan "re-tejer". Actualizar el índice.
2. **MODELO-DATOS.** Aplicar cambios de entidades/campos con nombres canónicos, clasificación de sensibilidad y glosario. Cambios incompatibles: redactar el plan de migración junto al cambio. No tocar Mapas de Consumo (mini-Tejido).
3. **UX-UI y CONTRATO-API** (paralelizable). Reflejar los campos/flujos/endpoints nuevos con nombres canónicos.
4. **TRANSACCIONES-CLAVE.** Si el cambio crea o altera una operación que califica (doc 12), crear/editar su archivo.

`PRUEBAS.md` y `ARQUITECTURA.md` se tocan aquí solo si el impacto lo declaró (estrategia/estructura); los casos concretos llegan con la construcción.

## Cierre de la fase

1. Anexar a la memoria EV-NNN la sección "Paso 4 — Artefactos actualizados" con la **lista consolidada de archivos modificados**.
2. Estado en `EVOLUCIONES-EN-CURSO.md`: `Análisis` → `En Diseño`.
3. Presentar el diff de artefactos al humano. La verificación es FASE 3 (`/trama-evolucion-verificar`).
