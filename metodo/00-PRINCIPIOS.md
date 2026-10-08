# 00 — Principios y Posiciones del Método TRAMA

Este documento explica **por qué** el método decide lo que decide. No es manual operativo: es el razonamiento fundacional. Cada principio incluye su posición frente a los enfoques predecesores de los que TRAMA aprende — y de los que se aparta.

---

## P1 — Memoria pura

**Posición:** `CLAUDE.md` no contiene requisitos, entidades ni reglas de negocio. Contiene la disciplina para procesarlos: stack, convenciones, comandos, punteros a `specs/` y reglas de operación del agente.

**Por qué.** La memoria del agente se carga en cada sesión. Si el dominio vive ahí, crece sin límite, se desactualiza en silencio y compite por contexto con la tarea real. El dominio vive en `specs/`, que se lee **por demanda** según la tarea. La memoria se mantiene en ~150 líneas.

**Síntoma de violación:** aparece una entidad, un campo o un criterio de aceptación dentro de `CLAUDE.md`. Corrección: moverlo a `specs/` y dejar un puntero.

## P2 — Los requisitos preceden al modelo

**Posición:** la causalidad fluye **usuario → historias → modelo → código**. El modelo de datos se *deriva* de las historias; cada campo de dominio existe porque una historia lo justifica.

**Frente a SDD.** El Spec-Driven Development (Spec Kit, Kiro, OpenSpec) acierta en que el agente necesita especificaciones antes que código, pero sus specs suelen nacer "modelo primero" o "diseño primero", con el usuario como derivado. TRAMA invierte eso: la historia — narrada en lenguaje del dominio, sin nombres técnicos — es el artefacto raíz. Lo que TRAMA sí toma de SDD: la persistencia de contexto (logs de decisiones que registran el *porqué* que git no captura) y el ciclo de ejecución autónoma (ver P7).

**Síntoma de violación:** escribir entidades antes de tener historias, o campos que ninguna historia consume.

## P3 — El Tejido: trazabilidad con nombre propio y compuerta

**Posición:** la trazabilidad bidireccional entre historias y modelo no es una aspiración difusa — es una **fase nombrada** (el Tejido) con entregables concretos (matriz CRUD por historia, Mapa de Consumo por entidad), una taxonomía de defectos (los **nudos** N1-N5) y una regla de compuerta: **no se entra a Diseño con nudos N1 o N4 abiertos**.

**Linaje.** La matriz CRUD viene del Information Engineering (matrices entidad × proceso, años 80); la trazabilidad requisito ⇄ implementación viene de la ingeniería de requisitos clásica (RTM). TRAMA las funde y las convierte en compuerta operada por el agente. Un nudo detectado en el Tejido cuesta minutos; en Construcción cuesta horas; en producción, reputación.

## P4 — Dominio y control separados en el modelo

**Posición:** el modelo distingue **campos de dominio** (justificados por historias, sujetos a trazabilidad) de **estructuras de control** (id, timestamps, soft-delete, claves foráneas técnicas — exentas de trazabilidad).

**Por qué.** Sin la distinción, la trazabilidad se vuelve burocracia: nadie escribe una historia para `created_at`. Con ella, la regla "todo campo de dominio tiene historia" es exigible al 100% sin excepciones incómodas.

## P5 — Lo plural en directorios, lo singular en archivos

**Posición:** artefactos con cardinalidad natural mayor a uno (historias, flujos de UX, contratos por servicio, transacciones, memorias de evolución) viven en **directorios con un archivo por unidad** y un `README.md` índice. Artefactos únicos (modelo, arquitectura, guía de estilo, estrategia de pruebas, runbook de despliegue) viven en **un solo archivo**.

**Por qué.** Un archivo por historia permite ediciones paralelas sin conflictos, diffs legibles y lectura por demanda. Un modelo fragmentado, en cambio, rompería la vista de conjunto que su función exige.

## P6 — La seguridad es un hilo transversal, no un anexo

**Posición:** la seguridad se pliega dentro de cada fase — consideraciones en las historias, clasificación de sensibilidad en el modelo, controles en el contrato de API, ADRs `SEG-` en diseño, casos negativos en pruebas — en lugar de existir como fase o checklist final.

**Frente a los marcos formales.** OWASP (ASVS, Top 10) y STRIDE son referencia informativa para razonar amenazas, no listas de verificación literal. El detalle operativo vive en `metodo/SEGURIDAD.md`.

## P7 — Los artefactos vivos son el proposal

**Posición:** cuando el Tejido está cerrado y el Diseño aprobado, los artefactos de `specs/` contienen todo lo que un agente necesita para construir de forma autónoma. No se redacta un "proposal" o "spec de implementación" adicional: **los artefactos son el proposal**. El ciclo autónomo de construcción (hilar → aplicar → verificar, skill `/trama-construir`) opera directamente sobre ellos.

**Frente a SDD.** Los proposals de SDD duplican información que ya vive (o debería vivir) en los artefactos, y esa duplicación deriva en desincronización. TRAMA conserva el ciclo de ejecución y elimina el artefacto intermedio. El precio: los artefactos deben estar completos de verdad — por eso la compuerta del Tejido es estricta.

## P8 — El humano es el árbitro del porqué

**Posición:** el agente extrae, propone, verifica y construye; el humano decide intención, prioridad y significado de negocio. En Adopción, el código es la verdad inicial sobre el *qué*, pero solo el humano confirma el *para qué* (las historias reconstruidas). En Evolución, cada fase pausa para revisión humana antes de continuar.

## P9 — La deuda explícita es un activo

**Posición:** lo que no se puede resolver ahora se registra con nombre, categoría y plan — nunca se oculta. En Adopción, `specs/DEUDA-INICIAL.md` cataloga los nudos del primer Tejido diagnóstico en tres categorías: resolver ahora / resolver incremental / aceptar como permanente. Linaje directo del concepto de deuda técnica de Ward Cunningham.

---

## Tabla de linaje

| Elemento de TRAMA | Predecesor | Qué se toma | Qué se cambia |
|---|---|---|---|
| Historias (`Como/quiero/para`) | Plantilla Connextra, XP/Scrum | Formato, criterios de aceptación | Segunda pasada obligatoria (matriz CRUD) en el Tejido |
| Matriz CRUD | Information Engineering | Matriz entidad × operación | Se ancla a cada historia y mapea concepto → campo canónico |
| Mapa de Consumo | Matriz de trazabilidad (RTM) | Índice inverso requisito ⇄ dato | Se vuelve compuerta operada por agente, con taxonomía de nudos |
| ADRs | M. Nygard | Contexto / decisión / consecuencias / alternativas | Prefijo `SEG-` para decisiones de seguridad |
| Laws of UX | J. Yablonski | Leyes como criterio de validación de flujos | Checklist integrado en la fase UX-UI |
| SOLID | R. C. Martin | Principios de diseño de código | Referencia en Diseño y Construcción |
| Ciclo autónomo | Spec-Driven Development | Loop hilar → aplicar → verificar; logs de decisiones | Sin proposals: los artefactos vivos son el proposal (P7) |
| Seguridad transversal | OWASP / STRIDE | Razonamiento de amenazas y controles | Informativo, plegado por fase, nunca checklist literal |
| Memoria y skills | Convenciones de Claude Code / Agent Skills | `CLAUDE.md`, `.claude/rules`, SKILL.md | Doctrina de memoria pura (P1) |
| Deuda explícita | W. Cunningham | Concepto de deuda técnica | Catálogo formal con 3 categorías en Adopción |

---

**La regla mental de TRAMA:** *la memoria no contiene el dominio; las historias preceden al modelo; el Tejido cierra la compuerta; los artefactos son el proposal; el humano arbitra el porqué.*
