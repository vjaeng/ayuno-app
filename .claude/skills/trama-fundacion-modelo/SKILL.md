---
name: trama-fundacion-modelo
description: Paso 3 de la Fundación (doc 16). Deriva specs/MODELO-DATOS.md desde las historias validadas — entidades de dominio con nombres canónicos y clasificación de sensibilidad, estructuras de control separadas, glosario de sinónimos, diagrama Mermaid y Mapas de Consumo vacíos — justificando cada entidad con las historias que la originan. Usarlo tras validar las historias de /trama-fundacion-historias. NO usarlo sin specs/HISTORIAS/ poblado ni sobre un modelo ya existente (usar /trama-evolucion).
---

# Fundación — Paso 3: Modelo de datos

Doctrina: `metodo/04-MODELO-DE-DATOS.md`. El modelo se **deriva** de las historias (P2): cada campo de dominio existe porque una historia lo justifica.

## Precondición

`specs/HISTORIAS/` poblado y validado por el humano. Si no hay historias: redirigir a `/trama-fundacion-historias`.

## Secuencia

1. **Leer todas las historias** y extraer cada concepto de dato de sus criterios.
2. **Nombrar canónicamente:** `entidad.campo`, snake_case, entidad en singular. Reconciliar los sinónimos detectados en el **Glosario** ("cliente" = "comprador" → `cliente`), dejando registro.
3. **Separar dominio de control** (P4): los campos justificados por historias van en su entidad; `id`, `created_at`, `updated_at`, `deleted_at` y FKs puramente estructurales van en la sección única de estructuras de control, exenta de trazabilidad. Ante la duda: si alguna historia lo menciona o lo necesita para un criterio, es dominio.
4. **Clasificar sensibilidad** de todo campo de dominio (Público / PII / Sensible / Crítico); ante la duda, la clase más restrictiva.
5. **Materializar** `specs/MODELO-DATOS.md` con la estructura del doc 04: diagrama ER Mermaid, entidades de dominio, estructuras de control, glosario y log de decisiones.

## Reglas duras

- **Los Mapas de Consumo nacen vacíos** — son entregable del Tejido; poblarlos ahora produce trazabilidad de una sola dirección.
- **Un campo sin historia no entra al modelo:** se anota como pregunta para el humano, nunca se incluye "por si acaso".

## Entregable y pausa

Presentar el modelo **justificando verbalmente cada entidad con las historias que la originan**, más la lista de preguntas (campos huérfanos, dudas dominio/control). Verificar las compuertas del doc 04 y **esperar validación del humano** — el modelo validado es el insumo de `/trama-fundacion-definicion`.
