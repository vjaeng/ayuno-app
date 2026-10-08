---
name: trama-evolucion
description: Orquestador del workflow de Evolución del Método TRAMA (cambios sobre sistema con specs/ poblado). Ejecuta en orden las 5 fases — plan (pasos 1-3), actualizar (paso 4), verificar (pasos 5a-5b), construcción (paso 6, delega en /trama-construir) y cierre (paso 7) — pausando entre fases para revisión humana. NO usarlo si specs/ no está poblado (usar Fundación o /trama-adopcion), ni para cambios triviales que no tocan historias ni modelo, ni para retomar una evolución existente (usar /trama-evolucion-retomar).
---

# Evolución — orquestador

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md`.

## Precondición

`specs/` poblado (HISTORIAS + MODELO-DATOS como mínimo). Si no existe: detenerse y redirigir a Fundación o `/trama-adopcion`. Si el humano describe algo ya registrado en `EVOLUCIONES-EN-CURSO.md`: detenerse y redirigir a `/trama-evolucion-retomar`.

## Secuencia

1. **FASE 1 — Planificación.** Ejecutar el procedimiento de `/trama-evolucion-plan` (intención, colisiones, reporte de impacto con citas, memoria EV-NNN, registro con estado `Análisis`). **PAUSA: presentar el reporte de impacto y esperar aprobación explícita.**
2. **FASE 2 — Actualización.** Con aprobación, ejecutar `/trama-evolucion-actualizar` (artefactos en orden historias → modelo → UX/API → transacciones; estado `En Diseño`). **PAUSA: presentar el diff de artefactos.**
3. **FASE 3 — Verificación.** Ejecutar `/trama-evolucion-verificar` (mini-Tejido + revisión cruzada). Si el veredicto es `Bloqueada`: presentar los nudos y detenerse hasta resolución. Si `En Construcción`: **PAUSA breve: confirmar arranque de construcción.**
4. **FASE 4 — Construcción.** Delegar en `/trama-construir` sobre el alcance de la evolución. Poblar el paso 6 de la memoria. **PAUSA: presentar resultado y pruebas.**
5. **FASE 5 — Cierre.** Ejecutar `/trama-evolucion-cerrar` (logs de decisiones, memoria cerrada, índice histórico, fila eliminada de EN-CURSO, CLAUDE.md si aplica).

## Reglas del orquestador

- Nunca saltar una pausa: la revisión humana entre fases es parte del método (P8), no cortesía.
- Si el humano abandona a mitad, dejar la memoria poblada hasta el último paso completado y el estado fiel en EN-CURSO — eso es lo que `/trama-evolucion-retomar` leerá.
- Si durante cualquier fase el alcance crece (aparecen artefactos no previstos en el impacto), volver a FASE 1 para re-analizar, no improvisar.
