---
name: trama-fundacion-definicion
description: Paso 4 de la Fundación (doc 16). Define en paralelo la superficie del sistema sobre el modelo validado — specs/UX-UI/ (un archivo por flujo con wireframes textuales, mapeo elemento → campo canónico, estados no felices y validación Laws of UX) y specs/CONTRATO-API/ (un archivo por servicio, convenciones globales, endpoints con mapeo canónico, auth explícita y errores enumerados). Usarlo tras validar el modelo de /trama-fundacion-modelo. NO usarlo sin modelo poblado ni sobre superficies ya definidas (usar /trama-evolucion).
---

# Fundación — Paso 4: Definición de superficie (UX-UI + Contrato de API)

Doctrina: `metodo/05-UX-UI.md` y `metodo/06-CONTRATO-API.md`. Ambos artefactos consumen el modelo y usan **nombres canónicos** — un elemento de UI o campo de payload sin campo canónico será nudo N5 en el Tejido.

## Precondición

`specs/MODELO-DATOS.md` poblado y validado. Si falta: redirigir a `/trama-fundacion-modelo`. Si el proyecto no tiene UI directa o no expone API, el directorio correspondiente lleva solo un README con "(no aplica)" — se decide con el humano, no se asume.

## UX-UI (`specs/UX-UI/`)

1. Un archivo por flujo (`[modulo]-[slug].md`) con la plantilla del doc 05: historias que cubre, entrada/salida, pantallas con wireframe textual, tabla elemento UI → campo canónico → modo, acciones y estados.
2. **Estados no felices siempre:** vacío, cargando, error, sin permisos.
3. **Validación Laws of UX por pantalla:** Hick (≤3 acciones principales), Jakob (patrón familiar), Fitts (objetivos frecuentes grandes/cercanos), Miller (agrupar en 5-9).
4. `README.md` índice con el mapa de navegación global (Mermaid).
5. Todo flujo cita sus historias — un flujo sin historia es diseño especulativo y no se crea.

## Contrato de API (`specs/CONTRATO-API/`)

1. Un archivo por servicio con la plantilla del doc 06; `README.md` índice con las convenciones globales (base URL, versionado, formato de errores, paginación, auth por defecto).
2. **Auth explícita por endpoint** (pública / autenticada / rol) y **errores enumerados**, incluidos 401/403 — serán casos negativos en PRUEBAS.
3. Payloads mapeados a canónico; nunca exponer campos `Crítico` en respuestas, `Sensible` solo con justificación en el propio contrato.
4. Cada endpoint cita sus historias — señalar al humano cualquier endpoint que no se pueda justificar con una, en vez de crearlo.

## Entregable y pausa

Presentar ambas superficies junto con las **decisiones de producto que las historias no resuelven** (preguntas, no suposiciones) y **esperar validación del humano**. Con ambas validadas, el siguiente paso es la compuerta: `/trama-tejer` en modo completo (paso 5).
