---
name: trama-evolucion-retomar
description: Paso 0 del workflow de Evolución — reanudar una evolución en curso. Lee specs/EVOLUCIONES-EN-CURSO.md, confirma con el humano cuál entrada se retoma, lee su memoria EV-NNN, ejecuta git status y git diff sobre specs/ para detectar desalineación memoria ↔ árbol, y reporta estado actual, último paso poblado, próximo paso esperado, motivo de bloqueo si aplica y archivos sin commit. Solo diagnóstico: no modifica artefactos. Usarlo al retomar trabajo previo sobre una evolución registrada. NO usarlo para cambios nuevos (usar /trama-evolucion o /trama-evolucion-plan).
---

# Evolución — Paso 0: Retomar

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md`, paso 0. **Este skill no modifica nada.**

## Procedimiento

1. **Leer** `specs/EVOLUCIONES-EN-CURSO.md`. Sin entradas → informar y sugerir `/trama-evolucion-plan`. Varias entradas → preguntar cuál se retoma.
2. **Leer la memoria** `EV-NNN.md` completa: identificar el último paso poblado y el estado declarado.
3. **Detectar desalineación.** `git status` y `git diff` sobre `specs/` (y sobre el código si el estado es `En Construcción`): ¿hay modificaciones sin commit que la memoria no menciona? ¿la memoria declara archivos tocados que el árbol no refleja?
4. **Reportar:**
   - Estado actual y su coherencia con la memoria.
   - Último paso poblado / próximo paso esperado del workflow.
   - Motivo del bloqueo, si estado = `Bloqueada` (con los nudos pendientes).
   - Archivos modificados sin commit y si contradicen la memoria.
   - Recomendación: qué skill ejecutar a continuación (`/trama-evolucion-actualizar`, `/trama-evolucion-verificar`, `/trama-construir` o `/trama-evolucion-cerrar`).
5. Esperar decisión del humano. Si la desalineación es grave (árbol contradice memoria), recomendar reconciliar la memoria **antes** de continuar el workflow.
