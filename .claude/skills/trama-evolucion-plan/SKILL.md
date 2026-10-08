---
name: trama-evolucion-plan
description: FASE 1 del workflow de Evolución (pasos 1-3 del doc 14). Levanta la intención del cambio, detecta colisiones con evoluciones activas, genera el reporte de impacto con bloques de Comprensión (citas literales) y Modificación sobre los 8 frentes (historias, modelo, UX-UI, contrato API, transacciones, pruebas, arquitectura, seguridad), crea la memoria EV-NNN en specs/EVOLUCIONES-HISTORICAS/ y registra la entrada en specs/EVOLUCIONES-EN-CURSO.md con estado Análisis. Primer paso de toda evolución. NO usarlo si ya existe entrada activa para este cambio (usar /trama-evolucion-retomar) ni si specs/ no está poblado.
---

# Evolución — FASE 1: Planificación

Doctrina: `metodo/14-WORKFLOW-EVOLUCION.md`, pasos 1-3.

## Paso 1 — Intención y colisiones

1. Levantar con el humano la **intención**: qué cambia y por qué, en lenguaje del dominio (sin nombres técnicos todavía). Reformularla y confirmarla.
2. Leer `specs/EVOLUCIONES-EN-CURSO.md`. Si alguna evolución activa toca artefactos que este cambio previsiblemente tocará: **pausar** y resolver con el humano (esperar, fusionar o secuenciar). No avanzar con colisión abierta.

## Paso 2 — Reporte de impacto

Para cada uno de los 8 frentes — HISTORIAS, MODELO-DATOS, UX-UI, CONTRATO-API, TRANSACCIONES-CLAVE, PRUEBAS, ARQUITECTURA, seguridad (S1-S8 aplicables) — producir dos bloques:

- **Comprensión:** qué dice HOY el artefacto sobre el área afectada, con **citas literales** (`archivo:línea` o fragmento textual). Si el frente no se ve afectado, declararlo con una línea de justificación.
- **Modificación:** qué cambiará exactamente (historias nuevas/editadas, campos, endpoints, pantallas, casos de prueba).

Clasificar cada modificación de modelo como compatible (aditiva) o incompatible (rename/tipo/borrado — exigirá plan de migración en el paso 4).

## Paso 3 — Registro

1. Asignar `EV-NNN` (siguiente número libre del índice histórico).
2. Crear `specs/EVOLUCIONES-HISTORICAS/EV-NNN.md` con la plantilla del doc 14 y los pasos 1-2 poblados.
3. Agregar la fila a `specs/EVOLUCIONES-EN-CURSO.md` con estado `Análisis`, artefactos afectados y puntero a la memoria.
4. Presentar el reporte de impacto al humano. **El skill termina aquí** — la actualización de artefactos es FASE 2 (`/trama-evolucion-actualizar`) y requiere aprobación del reporte.
