---
name: trama-fundacion
description: Orquestador del workflow de Fundación del Método TRAMA (proyecto nuevo, sin código). Ejecuta en orden los 8 pasos del doc 16 — memoria, historias, modelo, definición de superficie (UX-UI + API), Tejido como compuerta, diseño, construcción ⇄ pruebas y despliegue — pausando entre pasos para validación humana. Al cerrar la primera entrega, el proyecto opera con /trama-evolucion. NO usarlo si ya existe código (usar /trama-adopcion) ni si specs/ ya está poblado y alineado (usar /trama-evolucion).
---

# Fundación — orquestador

Doctrina: `metodo/16-WORKFLOW-FUNDACION.md`. Se recorre **una sola vez**; la causalidad fluye usuario → historias → modelo → código.

## Precondición

Proyecto sin código previo y sin `specs/`. Si hay código: redirigir a `/trama-adopcion`. Si `specs/` ya opera: redirigir a `/trama-evolucion`.

## Secuencia

1. **Paso 1 — Memoria.** Completar `CLAUDE.md` conversacionalmente desde la plantilla (doc 02): stack, convenciones, comandos, arquitectura prevista. **Sin dominio.** **PAUSA: el humano confirma la memoria sin placeholders.**
2. **Paso 2 — Historias.** Ejecutar `/trama-fundacion-historias`. **PAUSA: el humano valida las historias** — son el artefacto raíz.
3. **Paso 3 — Modelo.** Ejecutar `/trama-fundacion-modelo`. **PAUSA: el humano valida entidades, glosario y preguntas abiertas.**
4. **Paso 4 — Superficie.** Ejecutar `/trama-fundacion-definicion` (UX-UI + Contrato de API en paralelo). **PAUSA: el humano valida flujos, endpoints y decisiones de producto.**
5. **Paso 5 — Tejido ⭐ (COMPUERTA).** Ejecutar `/trama-tejer` en modo completo. **No se avanza con nudos N1 o N4 abiertos**; N2/N3/N5 se resuelven o se documenta la excepción.
6. **Paso 6 — Diseño.** ADRs (incluyendo `SEG-`) y `specs/ARQUITECTURA.md` según doc 08. **PAUSA: el humano aprueba las decisiones estructurales.**
7. **Paso 7 — Construcción ⇄ Pruebas.** Delegar en `/trama-construir` historia por historia (los artefactos son el proposal, P7); pruebas derivadas de criterios + matriz (docs 09-10).
8. **Paso 8 — Despliegue y Transacciones Clave.** Runbook `specs/DESPLIEGUE.md` y `specs/TRANSACCIONES-CLAVE/` para las operaciones que califiquen (docs 11-12).

## Reglas del orquestador

- **Respetar la causalidad:** si se están escribiendo entidades antes que historias, detenerse y volver al paso que corresponde.
- La compuerta del paso 5 no es formalidad: los nudos baratos aquí son caros después.
- Diseño ⇄ Construcción ⇄ Pruebas es ciclo, no cascada; la seguridad se pliega en cada paso (doc SEGURIDAD).
- Si el humano abandona a mitad, registrar en `CLAUDE.md` (Estado Actual) el paso alcanzado — la Fundación es reanudable por paso.
- Al cerrar la primera entrega: cambiar a `/trama-evolucion`. **No se "vuelve a fundar".**
