---
name: trama-evolucion-verificar
description: FASE 3 del workflow de Evolución (pasos 5a-5b del doc 14). Paso 5a delega en /trama-tejer modo parcial para el mini-Tejido sobre el alcance del cambio (historias tocadas + entidades tocadas), poblando matrices CRUD y Mapas de Consumo; paso 5b ejecuta la revisión cruzada de consistencia entre artefactos. Transiciona el estado de En Diseño a En Construcción (sin nudos N1/N4) o a Bloqueada (con ellos). Usarlo después de /trama-evolucion-actualizar. NO usarlo si la memoria no tiene el paso 4 poblado, ni para cambios triviales.
---

# Evolución — FASE 3: Verificación

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md` pasos 5a-5b; `metodo/07-TEJIDO.md`.

## Precondición

Memoria EV-NNN con "Paso 4 — Artefactos actualizados" poblado; estado `En Diseño`.

## Paso 5a — Mini-Tejido

Invocar el procedimiento de `/trama-tejer` en **modo parcial** con alcance = historias nuevas/editadas + entidades tocadas (según la lista del paso 4). Resultado: matrices CRUD pobladas (incluidas las marcadas "re-tejer"), Mapas de Consumo actualizados, reporte de nudos del alcance.

## Paso 5b — Revisión cruzada de consistencia

Sobre el alcance, verificar los cruces que el Tejido no cubre:

- ¿Cada pantalla afectada muestra/captura los campos que el contrato expone y la matriz declara?
- ¿Cada endpoint nuevo tiene flujo UX (si es de cara al usuario) y viceversa?
- ¿Las transacciones clave afectadas reflejan la secuencia nueva?
- ¿`PRUEBAS.md` contempla los casos que las historias nuevas exigirán (incluidos negativos de seguridad)?
- ¿El plan de migración (si hubo cambio incompatible) cubre los datos existentes?

Inconsistencias residuales se agregan al reporte como nudos N5.

## Veredicto y cierre de fase

1. Poblar "Paso 5 — Verificación" en la memoria: nudos encontrados, resoluciones, veredicto.
2. **Sin N1/N4:** estado → `En Construcción`; N2/N3/N5 restantes: resolver ahora o excepción documentada con responsable.
3. **Con N1/N4:** estado → `Bloqueada`, con la lista de nudos y propuestas. Presentar al humano; la fase se repite tras resolver.

La construcción es FASE 4 (delega en `/trama-construir`).
