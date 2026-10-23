# Historias de usuario — ayuno-app

Fuente: `docs/SOLICITUD_app_movil_ayuno.md` (versión base, app genérica). Actor único: **la persona que ayuna**.

Total: **49** historias · alta 34 · media 14 · baja 1. H-001 a H-043 son la versión base; H-044 y H-045 llegan con EV-001; H-046 con EV-002; H-047 con EV-003; H-048 con EV-004; H-049 con EV-005; *alta* marca el núcleo de fiabilidad y seguridad (objeto de E2E según `.claude/rules/pruebas-e2e.md`).

## Índice

| Id | Título | Módulo | Prioridad | Estado |
|---|---|---|---|---|
| H-001 | [Crear una plantilla con qué medir y cuándo](plantillas-001-crear-plantilla.md) | plantillas | alta | entregada |
| H-002 | [Completar la plantilla con metas, lista previa y avisos](plantillas-002-metas-lista-avisos.md) | plantillas | media | entregada |
| H-003 | [Duplicar, borrar y partir de una plantilla de ejemplo](plantillas-003-gestionar-plantillas.md) | plantillas | media | entregada |
| H-004 | [Crear un ayuno desde una plantilla con dos datos](ayuno-004-crear-ayuno.md) | ayuno | alta | entregada |
| H-005 | [Empezar el ayuno con un toque](reloj-005-empezar-ayuno.md) | reloj | alta | entregada |
| H-006 | [Ver de un vistazo dónde estoy](reloj-006-pantalla-inicio.md) | reloj | alta | en construcción |
| H-007 | [Ajustar el catálogo de mediciones a lo que uso](mediciones-007-catalogo-mediciones.md) | mediciones | alta | entregada |
| H-008 | [Anotar una medición en tres toques](mediciones-008-anotar-medicion.md) | mediciones | alta | entregada |
| H-009 | [Anotar la presión sentado y de pie](mediciones-009-presion-sentado-de-pie.md) | mediciones | alta | entregada |
| H-010 | [Marcar síntomas de mi propio catálogo](sintomas-010-sintomas.md) | sintomas | alta | entregada |
| H-011 | [Definir mis tipos de líquidos y electrolitos](liquidos-011-tipos-ingesta.md) | liquidos | alta | entregada |
| H-012 | [Anotar lo que bebo y ver cuánto llevo](liquidos-012-anotar-ingesta.md) | liquidos | alta | entregada |
| H-013 | [Mantener mi lista de suplementos y medicamentos](productos-013-lista-productos.md) | productos | alta | entregada |
| H-014 | [Decidir qué productos siguen y cuáles salen en el ayuno](productos-014-estado-durante-ayuno.md) | productos | alta | entregada |
| H-015 | [Marcar cada toma de un producto](productos-015-marcar-toma.md) | productos | alta | entregada |
| H-016 | [Que cada recordatorio suene a su hora pase lo que pase](recordatorios-016-recordatorio-fiable.md) | recordatorios | alta | en construcción |
| H-017 | [Resolver un pendiente desde la notificación](recordatorios-017-gestionar-pendiente.md) | recordatorios | alta | en construcción |
| H-018 | [Ver el día como una línea de tiempo](recordatorios-018-vista-hoy.md) | recordatorios | alta | entregada |
| H-019 | [Definir una regla de alerta por umbral](reglas-019-regla-umbral.md) | reglas | alta | entregada |
| H-020 | [Definir una regla de alerta sostenida en el tiempo](reglas-020-regla-sostenida.md) | reglas | alta | entregada |
| H-021 | [Definir una regla que salte con un síntoma de alarma](reglas-021-regla-sintoma-alarma.md) | reglas | alta | entregada |
| H-022 | [Definir una regla por ingesta bajo la meta a cierta hora](reglas-022-regla-ingesta-bajo-meta.md) | reglas | media | entregada |
| H-023 | [Pedir una alerta manual cuando algo no va bien](reglas-023-boton-algo-no-va-bien.md) | reglas | alta | entregada |
| H-024 | [Limitar una regla a ciertos momentos o contextos](reglas-024-filtros-regla.md) | reglas | media | tejida |
| H-025 | [Elegir el nivel y el mensaje de cada regla](reglas-025-nivel-y-mensaje.md) | reglas | alta | tejida |
| H-026 | [Que una regla programe otra medición u ofrezca terminar](reglas-026-accion-regla.md) | reglas | alta | entregada |
| H-027 | [Decidir yo ante cada alerta y que quede constancia](reglas-027-decision-y-registro.md) | reglas | alta | entregada |
| H-028 | [Tener mis reglas siempre a un toque](reglas-028-boton-seguridad.md) | reglas | alta | entregada |
| H-029 | [Llamar a mi contacto de emergencia con un toque](reglas-029-contacto-emergencia.md) | reglas | media | tejida |
| H-030 | [Terminar el ayuno con su hora y motivo](fin-030-terminar-ayuno.md) | fin | alta | entregada |
| H-031 | [Definir y seguir mi plan de vuelta a comer](vuelta-a-comer-031-plan-vuelta.md) | vuelta-a-comer | media | entregada |
| H-032 | [Anotar cada comida y cómo me cayó](vuelta-a-comer-032-registrar-comida.md) | vuelta-a-comer | media | entregada |
| H-033 | [Revisar mis ayunos pasados](historial-033-historial-ayunos.md) | historial | media | entregada |
| H-034 | [Corregir o borrar una entrada dejando rastro](historial-034-corregir-borrar.md) | historial | alta | en construcción (EV-004: mediciones y presión hechas) |
| H-035 | [Escribir una nota libre](notas-035-nota-libre.md) | notas | alta | entregada |
| H-036 | [Exportar un ayuno o todo a un archivo con formato fijo](exportacion-036-exportar.md) | exportacion | alta | entregada |
| H-037 | [Exportar también en tablas](exportacion-037-exportar-tablas.md) | exportacion | baja | entregada |
| H-038 | [Decidir si el archivo lleva mis datos personales](exportacion-038-datos-identidad.md) | exportacion | media | entregada |
| H-039 | [Importar plantillas y mi lista de productos](importacion-039-importar-plantillas-productos.md) | importacion | media | entregada |
| H-040 | [Restaurar todo desde un archivo exportado](importacion-040-restaurar.md) | importacion | alta | entregada |
| H-041 | [Usar la app entera sin internet ni cuenta](privacidad-041-sin-conexion-sin-cuenta.md) | privacidad | alta | tejida |
| H-042 | [Proteger mis datos en el teléfono y en copias](privacidad-042-proteger-datos.md) | privacidad | media | tejida |
| H-043 | [Elegir mis unidades por defecto](ajustes-043-unidades.md) | ajustes | media | tejida |
| H-044 | [Cambiar un ayuno que aún no ha empezado](ayuno-044-cambiar-ayuno-planificado.md) | ayuno | media | entregada (EV-001) |
| H-045 | [Borrar un ayuno para siempre con todos sus datos](historial-045-borrar-ayuno.md) | historial | alta | en construcción (EV-001) |
| H-046 | [Ver mis ayunos planificados y elegir uno](ayuno-046-ayunos-planificados.md) | ayuno | alta | entregada (EV-002) |
| H-047 | [Anotar mi peso al empezar y al terminar y ver cuánto cambió](mediciones-047-peso-inicio-fin.md) | mediciones | media | entregada (EV-003) |
| H-048 | [Ver y corregir las mediciones del ayuno en curso](mediciones-048-ver-corregir-mediciones.md) | mediciones | alta | entregada (EV-004) |
| H-049 | [Elegir con palabras en las escalas de cómo me siento](mediciones-049-escalas-con-palabras.md) | mediciones | alta | construida (EV-005; falta la verificación en el teléfono) |

## Épicas

Agrupaciones de 5+ historias con módulo y objetivo comunes (sin criterios propios). Tras la consolidación solo el motor de reglas, que se mantiene deliberadamente desglosado, llega a 5:

- **Motor de reglas de alerta (decidir parar con la regla propia a la vista):** H-019, H-020, H-021, H-022, H-023, H-024, H-025, H-026, H-027, H-028, H-029.

## Requisitos transversales (no son historias)

Aplican a todas las pantallas; se trasladan a `specs/UX-UI/`, `specs/GUIA-ESTILO.md` y `specs/ARQUITECTURA.md`:

- **Lenguaje llano:** español, frases completas, fechas con día de la semana, sin siglas ni jerga; textos en archivos de traducción.
- **Diseño:** tema claro y oscuro; texto ≥17 pt; botones ≥48×48; alto contraste; tamaño de letra del sistema y lector de pantalla; rojo reservado a las alertas; ninguna cifra se pinta de verde o rojo por sí sola.
- **Fiabilidad:** guardar antes de confirmar; ninguna pérdida al cerrar, quedarse sin batería o reiniciar; reloj en menos de 1 s.
- **Datos:** hora local al segundo con desfase aparte; vacío = «no lo anoté», nunca cero; se guarda lo tecleado y su equivalente; los derivados se calculan.
- **Todo configurable:** mediciones, síntomas, horarios, reglas y textos viven en datos editables, no en el código.
- **Mantenimiento:** pruebas automáticas del motor de reglas, del reloj y del formato de exportación; versión del formato que sube en cambios incompatibles.

## Vocabulario a reconciliar en el glosario del modelo

Sinónimos detectados en la solicitud (no resueltos aquí; candidatos a nudo N3 en el Tejido):

- **Usuario / persona**: la solicitud usa ambos; las historias usan «persona que ayuna».
- **Inicio del ayuno / inicio del reloj**: el botón dice «Empieza el ayuno ahora» pero fija el *inicio del reloj*, distinto de la *última comida*.
- **Fin / terminar / objetivo cumplido**: «fin previsto» es el hito planificado; «terminar» es la acción con motivo.
- **Líquidos y electrolitos / ingesta**: el módulo habla de líquidos; el concepto general es *ingesta* (incluye caldo y comida).
- **Suplemento / medicamento / producto**: *producto* es el concepto que engloba ambos.
- **Regla / alerta / aviso**: *regla* es la condición; *alerta* es el registro de que se cumplió; *aviso* (amarillo) y *alerta* (rojo) son además los dos niveles. Doble uso de «alerta» a resolver.
- **Pendiente / recordatorio**: el pendiente es lo que toca hacer; el recordatorio es la notificación.
- **Botón manual**: «algo no va bien» (tabla de reglas) — no confundir con el botón de seguridad.
- **Posterior / etapa posterior**: misma etapa.

## Fuera de esta versión

La sección *Después de la versión base* de la solicitud no genera historias todavía (entrarán como Evolución): gráficas y comparación entre ayunos, widget, importar lecturas del sensor continuo, Apple Salud / Health Connect, nota de voz, ventana diaria de comidas, envío a un servidor propio.

## Preguntas abiertas que afectan a las historias

- **Contacto de emergencia** en la versión base o después: H-029 queda en prioridad media hasta decidirlo.
- **Unidades por defecto** (mg/dL o mmol/L, kg o lb, °C o °F): la historia de unidades las deja configurables; falta el valor inicial.
- **Plataforma** (Android, iPhone o ambos): no cambia las historias, pero condiciona la viabilidad de los recordatorios fiables.
- ¿Entra ya algún punto de *Después de la versión base*? Si sí, se añade como historia nueva.

## Tejido (completo, 2026-10-08)

Matriz CRUD poblada en las 43 historias y Mapa de Consumo en las 29 entidades de `specs/MODELO-DATOS.md`.

| Nudo | Tipo | Ubicación | Descripción | Resolución |
|---|---|---|---|---|
| T-01 | N4 | H-004 · `fast.last_meal_actual` | Se mostraba la hora real de la última comida pero ninguna historia la registraba | Criterio nuevo en H-005; botón «Registrar última comida» en Inicio (D-13) |
| T-02 | N4 | H-024 · `measurement.on_waking`, `blood_pressure_reading.on_waking` | El filtro «solo al despertar» leía una marca que nada escribía | Criterio nuevo en H-008 (D-13) |
| T-03 | N2 | `checklist_item.position` | Campo sin historia | Eliminado del modelo y de UX-UI (D-12) |
| T-04 | N2 | `alert.decided_at` | Campo sin historia | Eliminado del modelo y de UX-UI (D-12) |
| T-05 | N5 | UX-UI `reglas-editar.md` | Borrar reglas: operación sin historia | Quitada; las reglas se desactivan (H-019) |

Sin N3 (el glosario del modelo cubre los sinónimos detectados). Sin N5 de nombres: todo campo citado en UX-UI existe en el modelo.

Convención: las historias de exportación, importación, corrección y borrado (H-034, H-036, H-037, H-039, H-040, H-045) usan comodines `entidad.*`; no cuentan para detectar campos huérfanos ni como origen de datos.

**Veredicto: compuerta abierta** — sin N1 ni N4. Resoluciones aprobadas por el humano.

## Construcción — tanda 1 (2026-10-08)

Recorrido mínimo: plantillas, crear y empezar el ayuno, reloj, anotar mediciones y presión, recordatorios, reglas por umbral y sostenidas, alertas con decisión, Seguridad y fin del ayuno. 102 pruebas en verde (dominio, integración con Room y E2E con Robolectric); lint sin errores.

**Entregadas en la tanda 1:** H-001, H-005, H-007, H-009, H-020.

**En construcción** — construidas, con criterios que dependen de otras historias o de verificación en dispositivo:

| Historia | Pendiente |
|---|---|
| H-006 | Reloj en < 1 s → verificación en dispositivo (PRUEBAS §4.7). Los totales del día ya funcionan. |
| H-017 | Acciones de la notificación con la app cerrada → verificación en dispositivo (PRUEBAS §4.7). «No se hizo» ya viaja en la exportación. |
| H-016 | Verificación en dispositivo (alarma real con la app cerrada, reinicio real, 3 días). Ya incluye tomas de productos y pasos de la vuelta a comer. |
| H-045 | Retirar las notificaciones visibles de un ayuno borrado → verificación en dispositivo (PRUEBAS §4.7). El borrado, la confirmación y el recálculo de recordatorios ya funcionan (EV-001). |

## Construcción — tanda 2a (2026-10-08)

Registro completo durante el ayuno: síntomas, líquidos y sal (con totales y sodio), productos con sus tomas, notas; metas, lista previa y avisos de la plantilla; reglas por síntoma de alarma, por ingesta bajo la meta (evaluadas también al llegar cada hora programada) y botón «algo no va bien». 136 pruebas en verde; lint sin errores.

**Entregadas en la tanda 2a:** H-002, H-010, H-011, H-012, H-013, H-014, H-021, H-022, H-023; y pasan a entregadas H-019 (evaluación a la hora programada) y H-028 («algo no va bien»). La tabla de pendientes de arriba refleja el estado actual.

## Construcción — tanda 2b (2026-10-10)

Recordatorios completos y vista del día: pestaña «Hoy» con lo hecho, pendiente, atrasado y no hecho, agrupado por franjas y con la hora del ayuno; acciones en la notificación (anotar, posponer 10 o 30 minutos, «no se hizo» con motivo); horario de silencio nocturno configurable en Ajustes; reglas con «medir de nuevo en N minutos», cuyo recordatorio suena aunque sea silencio; propuesta de «al despertar» en la primera lectura tras el silencio. 222 pruebas en verde; lint sin errores.

**Entregadas en la tanda 2b:** H-026; y pasan a entregadas H-008 (propuesta al despertar) y H-015 (tomas en la vista del día). H-017 y H-018 quedan en construcción (pendientes en la tabla de arriba).

## Construcción — tanda 3 (2026-10-10)

Vuelta a comer e historial: pasos de la vuelta a comer en la plantilla (por horas, con repetición, o por días), copiados al ayuno; al terminar se calculan sus casillas desde el fin real en la misma transacción; pantalla de vuelta a comer (también en Inicio durante la etapa) con casillas que guardan la hora real, comidas con cómo cayeron y reintroducción con fecha; los pasos en Hoy y en los recordatorios; pestaña Historial con la lista de ayunos y la línea de tiempo por días con totales. Base de datos en versión 3 (migración 2→3 probada). 244 pruebas en verde; lint sin errores.

**Entregadas en la tanda 3:** H-031, H-032; y pasan a entregadas H-004 (copia de la vuelta a comer), H-018 (pasos en el día), H-030 (la vuelta a comer empieza al terminar) y H-035 (la nota en la línea de tiempo). H-033 queda en construcción (exportar → H-036). Reglas de cálculo de las casillas en MODELO-DATOS D-14.

## Construcción — tanda 4 (2026-10-10)

Exportar e importar: archivo `fasting-log` 1.0 con nombres canónicos, `null` para lo no anotado, horas locales con segundos y desfase en la cabecera; esquema JSON publicado en `schema/` (generado desde la descripción del formato y comprobado en cada prueba) con ejemplo y registro de cambios; exportar un ayuno desde su detalle o todo desde Más, con tablas CSV opcionales en un comprimido, por la hoja de compartir; datos personales en Ajustes, que solo viajan si se activan; importar con resumen previo (nuevos, ya estaban, campos ignorados), validación completa y escritura todo o nada, sin duplicar. Base de datos en versión 4 (`person_profile`, migración 3→4 probada). 272 pruebas en verde; lint sin errores.

**Entregadas en la tanda 4:** H-036, H-037, H-038, H-039, H-040; y pasan a entregadas H-027 (las alertas viajan en la exportación) y H-033 (exportar desde el ayuno). H-017 sigue pendiente solo de la verificación en dispositivo.

## Construcción — H-003 (2026-10-10)

Duplicar una plantilla (copia independiente con identificadores nuevos, «Copia de …»), borrarla en blando con confirmación y constancia en la bitácora (los ayunos creados desde ella no cambian) y restaurarla desde «Borradas». Al instalar existe una plantilla de ejemplo (etapas, mediciones y reglas sin umbrales ni dosis), con identificadores fijos; las instalaciones existentes la reciben con la migración 4→5 (sin cambios de esquema). Una regla inactiva puede quedar sin umbral; al activarla lo exige. 281 pruebas en verde; lint sin errores.

## Cambios aprobados durante la construcción

| Fecha | Historia | Cambio | Origen |
|---|---|---|---|
| 2026-10-08 | H-004 | El objetivo se calcula entre el inicio y el fin previstos (antes se tecleaba); nuevo criterio de fin posterior al inicio. Sin cambios de modelo: `fast.target_hours` se sigue guardando | Petición del humano |
| 2026-10-11 | EV-002: H-046 nueva (pestaña Planificados con lista y detalle); H-006 deja en Inicio solo el próximo ayuno con una acción principal; H-004 y H-044 exigen una semana de separación entre ayunos (D-17). Motivo: Inicio era confuso con todas las opciones juntas y no había dónde elegir qué ayuno planificado cambiar | Petición del humano (EV-002) |
| 2026-10-13 | H-003 | La plantilla de ejemplo incluye todas las mediciones del catálogo (antes solo glucosa, peso y presión); solo esas tres llevan horario, las demás se anotan sin recordatorio. Las instalaciones existentes las reciben con la migración 5→6 (sin cambios de esquema). Los ayunos ya creados conservan su plan | Petición del humano |
| 2026-10-13 | H-001, H-008, H-004 | Anotar muestra todas las mediciones activas del catálogo (las del plan primero): el plan decide los recordatorios, no qué se puede anotar. Al planificar viene propuesta la primera plantilla en vez de «desde cero». Motivo: un ayuno desde cero quedaba sin mediciones y no había forma de añadirlas | Petición del humano |
| 2026-10-13 | EV-003: H-047 nueva; H-030 y H-033 con un criterio de peso; nota en H-005 | Peso al empezar (después del toque, se puede saltar) y al terminar (campo opcional), con inicial, final y diferencia en el resultado y en el detalle. Motivo: el humano quiere saber cuánto cambió su peso en cada ayuno | Petición del humano (EV-003) |
| 2026-10-14 | EV-004: H-048 nueva (lista de mediciones del ayuno desde Inicio, con corrección y borrado); H-034 con reevaluación de reglas al corregir en curso y alcance por entregas (primero mediciones y presión); H-006 con el acceso desde Inicio; H-018 tocar un hecho lo abre | El humano necesita ver y corregir las mediciones de un ayuno en curso | Petición del humano (EV-004) |
| 2026-10-14 | H-006 | Los hitos de Inicio pasan de lista vertical a la barra horizontal del wireframe; la fecha y hora de cada hito se ve bajo la barra (el siguiente por defecto, cualquiera al tocarlo). Sin cambio de criterios | Petición del humano |
| 2026-10-14 | H-003 | La plantilla de ejemplo pasa de los hitos 12 y 24 a 24, 36 y 48 (con objetivo 36 h, el 48 queda después del objetivo). Las instalaciones existentes la reciben con la migración 6→7 (solo datos), salvo que la persona ya hubiera cambiado los hitos; los ayunos ya creados conservan los suyos | Petición del humano |
| 2026-10-23 | EV-005: H-049 nueva (escalas con palabras que guardan un número, editables en Catálogos); H-007 y H-008 con el criterio de palabras; H-042 permite capturas de pantalla | El humano quiere elegir cómo se siente con palabras y poder hacer capturas de la app | Petición del humano (EV-005) |
