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

App móvil Android, personal y genérica, para planificar y registrar ayunos de cualquier duración: plantillas reutilizables, reloj del ayuno, mediciones, síntomas, líquidos y productos, recordatorios locales y reglas de alerta definidas por la propia persona. Registra, recuerda y avisa; no aconseja. Funciona sin internet ni cuentas y exporta el registro a un archivo con formato fijo. Fuente: `docs/SOLICITUD_app_movil_ayuno.md`.

## Stack Tecnológico

- **Plataforma:** app nativa Android (solo Android; sin iPhone en esta versión)
- **Lenguaje:** Kotlin (versión exacta, `minSdk` y `targetSdk` se fijan por ADR en Diseño)
- **Frontend:** Jetpack Compose + Material 3
- **Backend:** ninguno (app sin servidor; `specs/CONTRATO-API/` no aplica)
- **Base de datos:** SQLite local vía Room, cifrada en el dispositivo (H-042; librería de cifrado por ADR `SEG-` en Diseño)
- **Autenticación:** ninguna (sin cuentas); bloqueo opcional con la credencial del dispositivo (BiometricPrompt)
- **Recordatorios:** notificaciones locales con alarmas exactas (AlarmManager) y reprogramación tras reinicio y cambio de hora (H-016)
- **Infraestructura:** solo el teléfono; distribución por APK firmado (sin tienda por ahora)
- **CI/CD:** ninguno aún
- **Testing:** JUnit + pruebas de Room; E2E con Compose UI Test en emulador

## Estructura del Proyecto

```
<raíz>/
├── metodo/     ← metodología vendoreada (no editar)
├── specs/      ← artefactos vivos
└── .claude/    ← rules + skills /trama-*
```
Previstas (se crean en Construcción): `app/` (módulo Android: código y pruebas), `docs/` (solicitud de origen).

## Comandos

- **Dev:** `./gradlew installDebug` (emulador o teléfono conectado)
- **Build:** `./gradlew assembleRelease`
- **Lint:** `./gradlew lint`
- **Test:** `./gradlew test`
- **E2E:** `./gradlew connectedAndroidTest`
- **Deploy:** según `specs/DESPLIEGUE.md` (APK firmado instalado en el teléfono)

## Convenciones de Código (resumen)

[p.ej. 5-10 líneas con lo no-obvio: naming, imports, manejo de errores, idioma de comentarios. Detalle en `.claude/rules/convenciones-codigo.md`.]

## Principios de Arquitectura (resumen)

[p.ej. 5-10 líneas: patrón principal, capas, direcciones de dependencia. Detalle operacional en `specs/ARQUITECTURA.md`.]

## Guías de UX

Laws of UX (Hick, Jakob, Fitts, Miller) en cada pantalla; lenguaje llano sin siglas; texto ≥17 pt y objetivos ≥48 pt; rojo reservado a las alertas. Detalle en `specs/UX-UI/README.md`.

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
9. (sin reglas adicionales)

## Estado Actual del Proyecto

Fundación: pasos 2-5 cerrados (historias, modelo, UX-UI, Tejido con compuerta abierta). Siguiente: Diseño (paso 6). Sin código aún.

## Decisiones Técnicas

| Fecha | Decisión | Razón |
|---|---|---|
| 2026-10-08 | Nombres canónicos en inglés y `snake_case` | Coinciden con las claves del archivo de exportación (MODELO-DATOS D-01) |
| 2026-10-08 | Sin API: `specs/CONTRATO-API/` no aplica | App sin servidor; el formato del archivo se especifica en Diseño |
| 2026-10-08 | App nativa Android con Kotlin y Jetpack Compose | Solo Android; control directo de alarmas exactas y reinicio, críticos para que ningún recordatorio se pierda (H-016) |
