---
name: trama-tejer
description: Ejecuta el Tejido del Método TRAMA — la fase que cierra la trazabilidad bidireccional entre specs/HISTORIAS/ y specs/MODELO-DATOS.md, poblando la matriz CRUD de cada historia y el Mapa de Consumo de cada entidad, y reportando nudos N1-N5. Actúa como compuerta antes de Diseño. Úsalo (1) en Fundación paso 5, (2) en modo parcial dentro de una evolución (paso 5a), (3) en modo diagnóstico durante la Adopción (paso 11), o (4) ante deuda de trazabilidad acumulada. NO usarlo para cambios triviales que no tocan historias ni modelo.
---

# Tejer — trazabilidad bidireccional

Doctrina completa: `metodo/07-TEJIDO.md`. Convención asumida: artefactos en `specs/` (`HISTORIAS/`, `MODELO-DATOS.md`, `UX-UI/`, `CONTRATO-API/`).

## Paso 0 — Determinar el modo

Preguntar (o inferir del contexto de invocación) el modo:

- **completo** — todas las historias y entidades; compuerta estricta.
- **parcial** — solo el alcance indicado (lista de historias + entidades de una evolución); compuerta estricta sobre ese alcance.
- **diagnóstico** — todo, sin bloquear; el resultado alimenta `specs/DEUDA-INICIAL.md`.

## Paso 1 — Barrido historia → modelo

Por cada historia del alcance:
1. Extraer los **conceptos de dato** de sus criterios de aceptación.
2. Mapear cada concepto a `entidad.campo` canónico, consultando el glosario del modelo.
3. Determinar la operación implicada (C/R/U/D) por cada concepto.
4. Escribir la **matriz CRUD** en la historia (formato del doc 07, con fecha).
5. Concepto sin campo posible → registrar **nudo N1** (no inventar el campo: proponerlo aparte).

## Paso 2 — Barrido modelo → historias

Por cada entidad del alcance:
1. Recolectar las historias cuya matriz la referencia.
2. Escribir el **Mapa de Consumo** en la entidad (formato del doc 07, con fecha).
3. Campo de dominio sin historia → **nudo N2**. Estructuras de control: exentas.

## Paso 3 — Coherencia de operaciones

Sobre las matrices agregadas: todo campo con R/U/D debe tener C en alguna parte del sistema; las operaciones deben ser compatibles con las reglas de negocio del modelo. Incoherencia → **nudo N4**. Sinónimos sin reconciliar detectados en el mapeo → **nudo N3**.

## Paso 4 — Contraste con UX y API

Muestrear `specs/UX-UI/` y `specs/CONTRATO-API/` del alcance: nombres no canónicos o campos inexistentes → **nudo N5**.

## Paso 5 — Reporte y veredicto

Producir el reporte: tabla de nudos (id, tipo, ubicación, descripción, propuesta de resolución) y veredicto:

- Modo completo/parcial: **compuerta abierta** (sin N1/N4) o **compuerta cerrada** (listar bloqueantes). N2/N3/N5: proponer resolución o registrar excepción con responsable.
- Modo diagnóstico: catálogo completo sin veredicto de bloqueo, listo para clasificarse en DEUDA-INICIAL.

## Condiciones de parada

Detenerse y consultar al humano si: un concepto admite dos mapeos plausibles (ambigüedad de dominio), una resolución de nudo exige cambiar un criterio de aceptación, o el glosario contradice el uso real en las historias.
