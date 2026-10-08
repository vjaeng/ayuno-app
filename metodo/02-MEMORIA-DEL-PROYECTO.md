# 02 — Memoria del Proyecto (`CLAUDE.md`)

## Doctrina

`CLAUDE.md` es **memoria pura** (principio P1): contiene la disciplina para procesar el dominio, nunca el dominio. Presupuesto: **~150 líneas**. Si crece más, algo que debía vivir en `specs/` se está colando.

**Qué SÍ va:** descripción del proyecto en 3-5 líneas, stack, estructura de carpetas top-level, comandos, resumen de convenciones y arquitectura (el detalle vive en `.claude/rules/` y `specs/ARQUITECTURA.md`), punteros a los artefactos de `specs/`, reglas de operación del agente, estado actual de alto nivel, log de decisiones técnicas.

**Qué NO va:** entidades, campos, criterios de aceptación, endpoints, reglas de negocio, wireframes, casos de prueba. Todo eso tiene su artefacto en `specs/`.

## Plantilla

La plantilla canónica vive en `plantillas/CLAUDE-PLANTILLA.md` del repositorio del método y es la que el instalador copia al proyecto. Sus secciones:

1. **Adopción del Método** — tipo de proyecto (fundación/adopción), versión del método, puntero a `.trama-config.yaml`.
2. **Descripción del Proyecto** — 3-5 líneas.
3. **Stack Tecnológico** — lenguaje, frameworks, BD, auth, infra, CI/CD, testing.
4. **Estructura del Proyecto** — carpetas top-level reales.
5. **Comandos** — dev, build, lint, test, deploy.
6. **Convenciones de Código (resumen)** — 5-10 líneas; detalle en `.claude/rules/convenciones-codigo.md`.
7. **Principios de Arquitectura (resumen)** — 5-10 líneas; detalle en `specs/ARQUITECTURA.md`.
8. **Guías de UX** — referencia a Laws of UX o "(no aplica)" si no hay UI.
9. **Artefactos de Referencia** — la tabla de punteros a `specs/` y `metodo/`.
10. **Reglas de Operación del Agente** — ver abajo.
11. **Estado Actual** — fase del método, módulos completos/en curso.
12. **Decisiones Técnicas** — tabla fecha/decisión/razón.

## Reglas de operación del agente (incluidas en la plantilla)

1. Leer `CLAUDE.md` antes de cualquier tarea.
2. No generar código sin historia correspondiente en `specs/HISTORIAS/`.
3. Las historias narran en lenguaje del dominio; su **matriz CRUD** mapea a `entidad.campo` canónico. Al implementar, usar siempre los nombres canónicos de la matriz.
4. Al modificar un campo del modelo, consultar su **Mapa de Consumo** y actualizar la matriz CRUD de cada historia afectada.
5. Al crear pruebas, la matriz CRUD de la historia es la fuente de verdad de qué campos verificar y con qué operación.
6. Antes de tocar artefactos, consultar `specs/EVOLUCIONES-EN-CURSO.md`. Para retomar una evolución registrada, ejecutar `/trama-evolucion-retomar`.
7. Consultar `metodo/SEGURIDAD.md` al escribir o revisar código.
8. `metodo/` es referencia inmutable: no editar.

## Primera sesión tras la instalación

La plantilla llega con placeholders dirigidos `[p.ej. ...]`. En la primera sesión, el agente los recorre **uno por uno, conversacionalmente**, preguntando al adoptante qué corresponde a su proyecto. Si una sección no aplica, se escribe "(no aplica)" — constancia explícita, nunca borrar la sección. Al terminar, se elimina el bloque de instrucciones inicial.
