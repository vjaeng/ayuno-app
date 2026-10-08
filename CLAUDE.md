# CLAUDE.md — ayuno-app

<!--
AL AGENTE (primera sesión tras la instalación):
Este CLAUDE.md viene de la plantilla del Método TRAMA. Las secciones con
[p.ej. ...] son placeholders dirigidos: el ejemplo sugiere QUÉ tipo de valor
va, no el valor exacto. Recorre las secciones una a una, conversacionalmente,
preguntando al adoptante. Si una sección no aplica, escribe "(no aplica)" —
constancia explícita, no borrar. Al terminar, elimina este bloque completo.
-->

## Adopción del Método

- **Workflow de origen:** fundacion <!-- fundacion | adopcion -->
- **Versión del método:** 0.1.0
- **Configuración operativa:** `.trama-config.yaml` (fuente de verdad post-instalación)

La seguridad es hilo transversal del método: consultar `metodo/SEGURIDAD.md` al escribir o revisar código.

## Descripción del Proyecto

[p.ej. Sistema de reservas de salas para oficinas multi-edificio. Permite a empleados reservar, modificar y liberar salas con reglas por edificio. 3-5 líneas máximo.]

## Stack Tecnológico

- **Lenguaje:** [p.ej. TypeScript 5 | Python 3.12 | Go 1.22]
- **Frontend:** [p.ej. React + Vite | ninguno (backend puro)]
- **Backend:** [p.ej. FastAPI | NestJS | ninguno]
- **Base de datos:** [p.ej. PostgreSQL 16 | SQLite]
- **Autenticación:** [p.ej. OIDC corporativo | JWT propio | ninguna aún]
- **Infraestructura:** [p.ej. Cloud Run | Docker local]
- **CI/CD:** [p.ej. GitHub Actions | ninguno aún]
- **Testing:** [p.ej. Vitest + Playwright | Pytest]

## Estructura del Proyecto

```
<raíz>/
├── metodo/     ← metodología vendoreada (no editar)
├── specs/      ← artefactos vivos
└── .claude/    ← rules + skills /trama-*
```
[p.ej. agregar las carpetas reales: src/, tests/, infra/ — top-level, sin recursión]

## Comandos

- **Dev:** [p.ej. `npm run dev`]
- **Build:** [p.ej. `npm run build`]
- **Lint:** [p.ej. `npm run lint`]
- **Test:** [p.ej. `npm test`]
- **Deploy:** [p.ej. según `specs/DESPLIEGUE.md` | n/a]

## Convenciones de Código (resumen)

[p.ej. 5-10 líneas con lo no-obvio: naming, imports, manejo de errores, idioma de comentarios. Detalle en `.claude/rules/convenciones-codigo.md`.]

## Principios de Arquitectura (resumen)

[p.ej. 5-10 líneas: patrón principal, capas, direcciones de dependencia. Detalle operacional en `specs/ARQUITECTURA.md`.]

## Guías de UX

[p.ej. Laws of UX como referencia (Hick, Jakob, Fitts, Miller) — o "(no aplica — sin UI directa)".]

## Artefactos de Referencia
*Artefactos vivos en `specs/`; metodología en `metodo/` (inmutable).*

- `specs/HISTORIAS/` — historias con matriz CRUD (índice en su README)
- `specs/MODELO-DATOS.md` — entidades (dominio + control) con Mapas de Consumo
- `specs/UX-UI/` — flujos y pantallas con campos canónicos
- `specs/CONTRATO-API/` — contratos por servicio
- `specs/DISENO.md` — ADRs (incluye SEG-*)
- `specs/ARQUITECTURA.md` — capas, patrones, checklist de features, deuda
- `specs/PRUEBAS.md` — estrategia y matriz historia × cobertura
- `specs/DESPLIEGUE.md` — runbook
- `specs/TRANSACCIONES-CLAVE/` — transacciones críticas
- `specs/GUIA-ESTILO.md` — tokens y componentes visuales
- `specs/EVOLUCIONES-EN-CURSO.md` — coordinación (consultar antes de tocar artefactos)
- `specs/EVOLUCIONES-HISTORICAS/` — memoria por evolución
- `metodo/SEGURIDAD.md` — principios S1-S8 de codificación segura

## Reglas de Operación del Agente

1. Leer este archivo antes de cualquier tarea.
2. No generar código sin historia correspondiente en `specs/HISTORIAS/`.
3. Las historias narran en lenguaje del dominio; su **matriz CRUD** mapea a `entidad.campo`. Al implementar, usar los nombres canónicos de la matriz, no los conceptos genéricos.
4. Al modificar un campo del modelo, consultar su **Mapa de Consumo** y actualizar la matriz CRUD de cada historia afectada.
5. Al crear pruebas, la matriz CRUD es la fuente de verdad de qué campos verificar y con qué operación.
6. Antes de tocar artefactos, consultar `specs/EVOLUCIONES-EN-CURSO.md`; para retomar una evolución registrada, `/trama-evolucion-retomar`.
7. Todo componente de UI sigue `specs/GUIA-ESTILO.md`; las operaciones complejas se documentan en `specs/TRANSACCIONES-CLAVE/`.
8. `metodo/` es referencia inmutable: no editar.
9. [p.ej. reglas propias del proyecto, una por línea — o "(sin reglas adicionales)"]

## Estado Actual del Proyecto

[p.ej. fase del método (fundación: "historias en redacción" / adopción: "Fase 3 — extracción"); módulos completos vs. en construcción. Sin detalle día a día.]

## Decisiones Técnicas

| Fecha | Decisión | Razón |
|---|---|---|
| [AAAA-MM-DD] | [qué se decidió] | [por qué] |
