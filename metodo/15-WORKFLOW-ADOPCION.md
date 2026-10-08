# 15 — Workflow de Adopción (código existente)

## Cuándo

Hay código en producción o avanzado, sin `specs/` alineado al método. La causalidad se recorre **a la inversa**: código → modelo → historias. Al cerrar el bootstrap, el proyecto opera con Evolución (doc 14) — la Adopción no se repite.

Orquestador: **`/trama-adopcion`** (pausa entre fases). Sub-skills por fase para control granular.

## Principios de la Adopción

1. **El código es la verdad inicial** sobre el *qué*; el humano es el árbitro del *para qué* (P8). El agente extrae; el humano confirma significado.
2. **Iterativo, no big-bang.** Cada fase deja artefactos usables aunque las siguientes tarden.
3. **El primer Tejido es diagnóstico, no compuerta.** Con código previo, los nudos son inevitables: se catalogan, no bloquean.
4. **Deuda explícita es un activo** (P9): lo no resuelto queda en `specs/DEUDA-INICIAL.md` con categoría y plan.
5. **La documentación dispersa se absorbe**, no se referencia: lo útil de READMEs/wikis viejos se incorpora al artefacto que corresponde y la fuente vieja se marca obsoleta.

## FASE 1 — Inventario · `/trama-adopcion-inventario` (paso 1)

**Paso 1.** Recorrer el repositorio y producir el inventario: stack real, estructura de carpetas, módulos y su estado aparente, esquema de datos (migraciones/ORM/DDL), superficie de API (rutas), pantallas (frontend), pipeline de CI/CD, pruebas existentes, documentación dispersa encontrada. Sin modificar nada. **Pausa: el humano valida y corrige el inventario.**

## FASE 2 — Andamiaje · `/trama-adopcion-andamiaje` (pasos 2-3)

**Paso 2 — Memoria y reglas.** Generar `CLAUDE.md` (desde la plantilla, poblado con el inventario validado — no con placeholders) y las rules de `.claude/rules/` ajustadas a las convenciones reales observadas en el código.

**Paso 3 — Esqueleto de `specs/`.** Materializar la estructura completa por **copia mecánica de los assets del skill** (`assets/base/`): directorios con README índice placeholder y archivos únicos con esqueleto de secciones. El contenido de cada esqueleto vive como archivo físico en el skill, no en el prompt — evita deriva entre lo declarado y lo generado.

## FASE 3 — Extracción mecánica · `/trama-adopcion-extraccion` (pasos 4-9)

Extraer desde el código, artefacto por artefacto — el humano revisa cada entrega:

- **Paso 4 — Modelo.** `MODELO-DATOS.md` desde migraciones/ORM: entidades, dominio vs control, clasificación de sensibilidad propuesta (el humano la confirma), glosario inicial. Mapas de Consumo vacíos.
- **Paso 5 — Contrato de API.** Desde rutas y controladores: endpoints reales, auth observada, errores.
- **Paso 6 — UX-UI.** Desde el frontend: flujos y pantallas reales con sus campos.
- **Paso 7 — Transacciones Clave.** Detectar operaciones multi-entidad/con efectos externos y documentarlas.
- **Paso 8 — Estilo, despliegue y ADRs.** `GUIA-ESTILO.md` desde los estilos reales; `DESPLIEGUE.md` desde el pipeline real; ADRs iniciales (incluyendo `SEG-`) para las decisiones estructurales ya tomadas que se puedan inferir — marcadas "reconstruida, validar".
- **Paso 9 — Arquitectura y pruebas.** `ARQUITECTURA.md` por arqueología (capas reales, patrones observados, violaciones → deuda arquitectónica) y `PRUEBAS.md` con el estado real de cobertura.

## FASE 4 — Reconstrucción y cierre · `/trama-adopcion-cierre` (pasos 10-12)

**Paso 10 — Historias por arqueología inversa.** A partir de pantallas, endpoints y modelo, reconstruir las historias que el código implica — narradas en lenguaje del dominio, matriz CRUD vacía. **El humano arbitra cada "para qué"**: el agente propone la narrativa, el humano confirma o corrige el beneficio. Dos modos a elección: iterativo (historia por historia) o bulk (lote completo y revisión al final).

**Paso 11 — Tejido diagnóstico.** `/trama-tejer` en modo diagnóstico: matrices y Mapas poblados donde se pueda, catálogo completo de nudos **sin bloquear**.

**Paso 12 — Deuda inicial y entrada al ciclo.** Crear `specs/DEUDA-INICIAL.md` clasificando cada nudo:

| Categoría | Criterio | Destino |
|---|---|---|
| **1 — Resolver ahora** | Bloquea operar con seguridad (N4 en flujo crítico, campo Crítico sin clasificar) | Se resuelve antes de cerrar la Adopción |
| **2 — Deuda incremental** | Molesta pero no bloquea | Cada Evolución que toque el área resuelve su parte |
| **3 — Permanente aceptada** | Costo de resolver > beneficio | Documentada con razón; no se vuelve a discutir |

Con la categoría 1 resuelta: crear `EVOLUCIONES-EN-CURSO.md` vacío, actualizar el estado en `CLAUDE.md`, y **declarar cerrado el bootstrap**. Desde aquí, todo pasa por `/trama-evolucion`.

## Regla de oro

La Adopción termina cuando `specs/` refleja el sistema **tal como es** — no como debería ser. Las mejoras deseadas son evoluciones futuras, no requisitos del bootstrap.
