# Historias de usuario — ayuno-app

Fuente: `docs/SOLICITUD_app_movil_ayuno.md` (versión base, app genérica). Actor único: **la persona que ayuna**.

Total: **43** historias · alta 30 · media 12 · baja 1. Todas pertenecen a la versión base; *alta* marca el núcleo de fiabilidad y seguridad (objeto de E2E según `.claude/rules/pruebas-e2e.md`).

## Índice

| Id | Título | Módulo | Prioridad | Estado |
|---|---|---|---|---|
| H-001 | [Crear una plantilla con qué medir y cuándo](plantillas-001-crear-plantilla.md) | plantillas | alta | borrador |
| H-002 | [Completar la plantilla con metas, lista previa y avisos](plantillas-002-metas-lista-avisos.md) | plantillas | media | borrador |
| H-003 | [Duplicar, borrar y partir de una plantilla de ejemplo](plantillas-003-gestionar-plantillas.md) | plantillas | media | borrador |
| H-004 | [Crear un ayuno desde una plantilla con dos datos](ayuno-004-crear-ayuno.md) | ayuno | alta | borrador |
| H-005 | [Empezar el ayuno con un toque](reloj-005-empezar-ayuno.md) | reloj | alta | borrador |
| H-006 | [Ver de un vistazo dónde estoy](reloj-006-pantalla-inicio.md) | reloj | alta | borrador |
| H-007 | [Ajustar el catálogo de mediciones a lo que uso](mediciones-007-catalogo-mediciones.md) | mediciones | alta | borrador |
| H-008 | [Anotar una medición en tres toques](mediciones-008-anotar-medicion.md) | mediciones | alta | borrador |
| H-009 | [Anotar la presión sentado y de pie](mediciones-009-presion-sentado-de-pie.md) | mediciones | alta | borrador |
| H-010 | [Marcar síntomas de mi propio catálogo](sintomas-010-sintomas.md) | sintomas | alta | borrador |
| H-011 | [Definir mis tipos de líquidos y electrolitos](liquidos-011-tipos-ingesta.md) | liquidos | alta | borrador |
| H-012 | [Anotar lo que bebo y ver cuánto llevo](liquidos-012-anotar-ingesta.md) | liquidos | alta | borrador |
| H-013 | [Mantener mi lista de suplementos y medicamentos](productos-013-lista-productos.md) | productos | alta | borrador |
| H-014 | [Decidir qué productos siguen y cuáles salen en el ayuno](productos-014-estado-durante-ayuno.md) | productos | alta | borrador |
| H-015 | [Marcar cada toma de un producto](productos-015-marcar-toma.md) | productos | alta | borrador |
| H-016 | [Que cada recordatorio suene a su hora pase lo que pase](recordatorios-016-recordatorio-fiable.md) | recordatorios | alta | borrador |
| H-017 | [Resolver un pendiente desde la notificación](recordatorios-017-gestionar-pendiente.md) | recordatorios | alta | borrador |
| H-018 | [Ver el día como una línea de tiempo](recordatorios-018-vista-hoy.md) | recordatorios | alta | borrador |
| H-019 | [Definir una regla de alerta por umbral](reglas-019-regla-umbral.md) | reglas | alta | borrador |
| H-020 | [Definir una regla de alerta sostenida en el tiempo](reglas-020-regla-sostenida.md) | reglas | alta | borrador |
| H-021 | [Definir una regla que salte con un síntoma de alarma](reglas-021-regla-sintoma-alarma.md) | reglas | alta | borrador |
| H-022 | [Definir una regla por ingesta bajo la meta a cierta hora](reglas-022-regla-ingesta-bajo-meta.md) | reglas | media | borrador |
| H-023 | [Pedir una alerta manual cuando algo no va bien](reglas-023-boton-algo-no-va-bien.md) | reglas | alta | borrador |
| H-024 | [Limitar una regla a ciertos momentos o contextos](reglas-024-filtros-regla.md) | reglas | media | borrador |
| H-025 | [Elegir el nivel y el mensaje de cada regla](reglas-025-nivel-y-mensaje.md) | reglas | alta | borrador |
| H-026 | [Que una regla programe otra medición u ofrezca terminar](reglas-026-accion-regla.md) | reglas | alta | borrador |
| H-027 | [Decidir yo ante cada alerta y que quede constancia](reglas-027-decision-y-registro.md) | reglas | alta | borrador |
| H-028 | [Tener mis reglas siempre a un toque](reglas-028-boton-seguridad.md) | reglas | alta | borrador |
| H-029 | [Llamar a mi contacto de emergencia con un toque](reglas-029-contacto-emergencia.md) | reglas | media | borrador |
| H-030 | [Terminar el ayuno con su hora y motivo](fin-030-terminar-ayuno.md) | fin | alta | borrador |
| H-031 | [Definir y seguir mi plan de vuelta a comer](vuelta-a-comer-031-plan-vuelta.md) | vuelta-a-comer | media | borrador |
| H-032 | [Anotar cada comida y cómo me cayó](vuelta-a-comer-032-registrar-comida.md) | vuelta-a-comer | media | borrador |
| H-033 | [Revisar mis ayunos pasados](historial-033-historial-ayunos.md) | historial | media | borrador |
| H-034 | [Corregir o borrar una entrada dejando rastro](historial-034-corregir-borrar.md) | historial | alta | borrador |
| H-035 | [Escribir una nota libre](notas-035-nota-libre.md) | notas | alta | borrador |
| H-036 | [Exportar un ayuno o todo a un archivo con formato fijo](exportacion-036-exportar.md) | exportacion | alta | borrador |
| H-037 | [Exportar también en tablas](exportacion-037-exportar-tablas.md) | exportacion | baja | borrador |
| H-038 | [Decidir si el archivo lleva mis datos personales](exportacion-038-datos-identidad.md) | exportacion | media | borrador |
| H-039 | [Importar plantillas y mi lista de productos](importacion-039-importar-plantillas-productos.md) | importacion | media | borrador |
| H-040 | [Restaurar todo desde un archivo exportado](importacion-040-restaurar.md) | importacion | alta | borrador |
| H-041 | [Usar la app entera sin internet ni cuenta](privacidad-041-sin-conexion-sin-cuenta.md) | privacidad | alta | borrador |
| H-042 | [Proteger mis datos en el teléfono y en copias](privacidad-042-proteger-datos.md) | privacidad | media | borrador |
| H-043 | [Elegir mis unidades por defecto](ajustes-043-unidades.md) | ajustes | media | borrador |

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
