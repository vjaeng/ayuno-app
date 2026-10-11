# Diseño — ADRs de ayuno-app

Registro de decisiones arquitectónicas (formato Nygard). Las decisiones con componente de seguridad llevan prefijo `SEG-` y citan los principios de `metodo/SEGURIDAD.md`. Prerrequisito cumplido: Tejido completo con compuerta abierta (2026-10-08).

**Índice**

| ADR | Título | Estado |
|---|---|---|
| ADR-001 | Kotlin + Jetpack Compose, una sola actividad | aceptada |
| ADR-002 | Tres módulos Gradle: `:app`, `:domain`, `:data` | aceptada |
| ADR-003 | Persistencia con Room sobre el modelo canónico | aceptada |
| ADR-004 | Hora local sin zona, reloj inyectable | aceptada |
| ADR-005 | Recordatorios con alarmas de despertador del sistema | aceptada |
| ADR-006 | Motor de reglas como función pura del dominio | aceptada |
| ADR-007 | Formato de intercambio: JSON canónico con JSON Schema | aceptada |
| ADR-008 | Datos precargados y textos fuera del código | aceptada |
| ADR-009 | Inyección de dependencias manual | aceptada |
| ADR-SEG-001 | Base de datos cifrada con SQLCipher y clave en Android Keystore | aceptada |
| ADR-SEG-002 | Bloqueo con la credencial del dispositivo | aceptada |
| ADR-SEG-003 | Exportación de datos Críticos iniciada solo por la persona | aceptada |
| ADR-SEG-004 | App sin permiso de red ni terceros | aceptada |
| ADR-SEG-005 | Copia de seguridad cifrada con contraseña | aceptada |
| ADR-SEG-006 | Importación validada, todo o nada | aceptada |
| ADR-SEG-007 | Borrado definitivo de un ayuno a petición de la persona | aceptada (EV-001) |

---

## ADR-001: Kotlin + Jetpack Compose, una sola actividad
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** La app es solo para Android (CLAUDE.md). Las historias exigen formularios rápidos (H-008: 3 toques, <30 s), pantallas a pantalla completa para alertas (H-025) y accesibilidad con el tamaño de letra del sistema y lector de pantalla (UX-UI README).

**Decisión.** Kotlin con Jetpack Compose y Material 3, una sola `Activity` con Navigation Compose; estado de pantalla con `ViewModel` + `StateFlow` (flujo unidireccional). `minSdk 29` (Android 10), `targetSdk 35` y `compileSdk 35` (Android 15: la plataforma y build-tools instaladas en el equipo de desarrollo). La app se ejecuta en el teléfono de la persona (Android 16) en modo de compatibilidad con API 35. Las versiones de Kotlin y del plugin de Android se fijan al crear el proyecto (las estables compatibles con API 35 y JDK 17) y se anotan en `ARQUITECTURA.md`.

**Consecuencias.**
- Gana: un solo lenguaje, accesibilidad nativa (semántica de Compose para el lector de pantalla), escalado de texto del sistema.
- Paga: Compose obliga a cuidar recomposiciones en la línea de tiempo larga (H-033); `minSdk 29` deja fuera teléfonos con Android 9 o anterior (decisión del humano: soporte desde Android 10).
- Difícil: portar a iPhone requerirá reescribir la UI (aceptado: fuera de alcance).
- Con `targetSdk 35` en un teléfono Android 16, los cambios de comportamiento exclusivos de API 36 no se activan; subir a 36 requiere instalar la plataforma y revisar esos cambios (evolución futura).

**Alternativas consideradas.** Flutter o React Native (multiplataforma, recomendados en la solicitud): descartados porque solo se apunta a Android y las alarmas exactas y la reprogramación tras reinicio (H-016) exigen código nativo igualmente. Vistas XML: descartadas por mayor código para formularios dinámicos (rejilla de mediciones activas).

## ADR-002: Tres módulos Gradle: `:app`, `:domain`, `:data`
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** La solicitud pide pruebas automáticas del motor de reglas, del reloj y del formato de exportación; deben ser rápidas y sin emulador. Las direcciones de dependencia deben poder verificarse (regla de arquitectura del método).

**Decisión.** Tres módulos:
- `:domain` — Kotlin puro (JVM, sin Android): modelo canónico, casos de uso, motor de reglas, cálculo de derivados y pendientes, interfaces de repositorio (puertos).
- `:data` — Room, DAOs, implementación de repositorios, cifrado, serialización de intercambio.
- `:app` — UI Compose, ViewModels, recordatorios y receptores del sistema, raíz de composición.

Dependencias permitidas: `:app → :domain`, `:data → :domain`, y `:app → :data` **solo** desde el paquete de raíz de composición (`di/`). `:domain` no depende de nada del proyecto.

**Consecuencias.**
- Gana: el compilador impide que el dominio importe Android o Room; pruebas del dominio en la JVM en segundos.
- Paga: mapeo explícito entre entidades Room y modelo de dominio (código repetitivo); tres `build.gradle` que mantener.

**Alternativas consideradas.** Un solo módulo con paquetes: más simple, pero las direcciones de dependencia solo se vigilarían por revisión. Módulo por funcionalidad (`:feature:reglas`…): excesivo para una app de un solo usuario y ~13 flujos.

## ADR-003: Persistencia con Room sobre el modelo canónico
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** Modelo de 29 entidades con nombres canónicos en inglés (MODELO-DATOS D-01); todo debe guardarse antes de confirmarse en pantalla (H-008) y sobrevivir a cierres y reinicios.

**Decisión.**
- Room sobre SQLite. Tabla = entidad canónica, columna = campo canónico (`snake_case` idéntico al modelo y al archivo de intercambio).
- `id` UUID v4 en texto; catálogos con llave `code`. Borrado blando con `deleted_at` en todas las tablas salvo `edit_log`.
- Entidades de plan con dos FK anulables (`template_id`, `fast_id`) y restricción `CHECK` de exactamente una no nula (D-03).
- Listas (`milestone_hours`, `stage_kinds`, `filter_stage_kinds`, `trigger_entry_ids`) como texto JSON mediante convertidores.
- Escrituras de varias entidades dentro de `withTransaction` (crear ayuno, terminar, editar con bitácora, importar).
- Migraciones de esquema explícitas y probadas; nunca `fallbackToDestructiveMigration`.

**Consecuencias.**
- Gana: consultas tipadas, transacciones, migraciones verificables; el archivo exportado se arma columna a columna sin traducción.
- Paga: las columnas JSON no se pueden consultar con SQL (aceptable: se filtran en memoria, volúmenes pequeños); la restricción XOR de propietario requiere SQL a mano en la migración inicial.

**Alternativas consideradas.** SQLDelight: también válida; Room tiene mejor integración con el ecosistema Android y soporte directo de SQLCipher (ADR-SEG-001). DataStore o archivos JSON: sin transacciones ni consultas, descartados.

## ADR-004: Hora local sin zona, reloj inyectable
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** Las horas se guardan en hora local al segundo, sin zona, con el desfase aparte por ayuno (`fast.tz_offset`, P-05); el reloj del ayuno se calcula, no se cuenta (H-006); las pruebas del reloj y de las reglas necesitan controlar el tiempo.

**Decisión.**
- Tipos de dominio: `java.time.LocalDateTime` truncado a segundos; persistido como texto `AAAA-MM-DDTHH:MM:SS` (orden lexicográfico = orden temporal).
- Al iniciar el reloj se captura `tz_offset` del dispositivo. Las duraciones se calculan convirtiendo ambas horas a instantes con el `tz_offset` del ayuno.
- Toda lectura de «ahora» pasa por un `java.time.Clock` inyectado; en pruebas se usa un reloj fijo o manipulable.

**Consecuencias.**
- Gana: exportación sin conversiones (requisito del sistema receptor); reloj correcto tras reinicio; pruebas deterministas.
- Paga: si la persona cambia de zona a mitad de un ayuno, las horas nuevas quedan en la zona nueva y las duraciones pueden desviarse en la diferencia (riesgo aceptado en P-05).

**Alternativas consideradas.** Guardar en UTC y convertir al exportar: viola la regla de la solicitud («no convertir a UTC») y complica el cruce con el sensor. Desfase por entrada: más fiel, pero rechazado en P-05.

## ADR-005: Recordatorios con alarmas de despertador del sistema
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** H-016: los recordatorios deben sonar a la hora exacta con la app cerrada, tras reiniciar y tras cambiar la hora o la zona; «un recordatorio perdido es un fallo de seguridad». Android limita las alarmas en reposo profundo (Doze) y exige permisos para alarmas exactas desde Android 12.

**Decisión.**
- Cada recordatorio se programa con `AlarmManager.setAlarmClock()`, el tipo de alarma que Android no difiere en reposo profundo.
- Permiso `USE_EXACT_ALARM` (Android 13+), concedido al instalar; `SCHEDULE_EXACT_ALARM` para Android 12; `POST_NOTIFICATIONS` pedido al crear el primer ayuno.
- Un `ReminderScheduler` recalcula los pendientes de las próximas 48 horas (derivados de horarios, productos, vuelta a comer y reglas) y mantiene programada **la siguiente** alarma; al sonar, notifica lo que vence y programa la siguiente. Se reejecuta al guardar cualquier cambio de plan o registro, al arrancar la app y ante `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` y el cambio de permiso de alarmas exactas.
- *Enmienda de construcción (2026-10-08):* se programa solo la siguiente alarma en lugar de todas las de 48 h. Una sola alarma con un `PendingIntent` fijo no necesita guardar ni cancelar listas de alarmas, y reprogramar es idempotente. Riesgo: si la cadena se rompe, se recupera en el siguiente arranque, reinicio o cambio de hora.
- El silencio nocturno se aplica al programar: los pendientes que caen en él no se programan, salvo los de reglas.
- Notificaciones con acciones directas (anotar, posponer 10/30, no se hizo); canal de alta importancia para recordatorios y otro para alertas.

**Consecuencias.**
- Gana: la vía más fiable que ofrece Android sin servidor.
- Paga: el icono de alarma aparece en la barra de estado mientras haya alarmas programadas; `USE_EXACT_ALARM` está reservado en Google Play a apps de alarma o calendario, así que **publicar en Play requeriría revisar esta decisión** (hoy la distribución es por APK). Algunos fabricantes cierran apps en segundo plano de forma agresiva: el runbook de despliegue incluirá desactivar la optimización de batería para la app.
- Difícil: probar la reprogramación tras reinicio exige pruebas instrumentadas en emulador.

**Alternativas consideradas.** `setExactAndAllowWhileIdle`: sujeta a límites de frecuencia en reposo profundo (dos recordatorios cercanos podrían retrasarse). WorkManager: no garantiza hora exacta. Notificaciones remotas: requieren servidor, fuera de alcance.

## ADR-006: Motor de reglas como función pura del dominio
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** H-019 a H-027: reglas definidas por la persona, evaluadas al guardar cada entrada y al llegar cada hora programada; la app nunca termina el ayuno por su cuenta; requiere pruebas automáticas.

**Decisión.**
- `RuleEngine.evaluate(reglas, contexto, momento) → List<RuleHit>` en `:domain`, sin efectos: recibe las reglas activas del ayuno y las entradas relevantes, devuelve qué reglas se cumplen y con qué entradas.
- Semántica de «sostenida N minutos»: se cumple si hay al menos dos lecturas fuera del umbral cuya separación es ≥ N minutos y **no hay ninguna lectura dentro del umbral entre ellas** (cumple H-020: una sola lectura o dos separadas < N no bastan).
- **Episodios:** una regla no vuelve a dispararse mientras su condición siga cumpliéndose desde el último disparo; el episodio termina con una lectura dentro del umbral (o, para ingesta bajo meta, al cambiar de día). Evita alertas duplicadas por cada lectura nueva.
- El caso de uso `RecordEntry` guarda la entrada, evalúa y crea las `alert` en la **misma transacción**; los efectos (mostrar alerta, programar recordatorio) los ejecuta `:app` después de confirmar.
- Las reglas son datos (`rule`); no hay umbrales en el código (H-019).

**Consecuencias.**
- Gana: el comportamiento crítico se prueba con tablas de casos en la JVM; la UI no decide nada de seguridad.
- Paga: la semántica de episodio es una decisión nueva que las historias no fijan (resuelta en Q-01); el contexto a cargar por evaluación crece con el número de lecturas (acotado al ayuno en curso).

**Alternativas consideradas.** Evaluar en la UI al guardar: duplicaría lógica y no cubriría las horas programadas. Un motor de reglas genérico (biblioteca externa): superficie innecesaria (S7) para cinco tipos de condición.

## ADR-007: Formato de intercambio: JSON canónico con JSON Schema
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** H-036 a H-040 y la solicitud fijan un archivo JSON con estructura y reglas concretas (sin BOM, hora local sin zona, `null` = no anotado, ids estables, catálogos y plantilla dentro). `specs/CONTRATO-API/` no aplica; este ADR es el contrato.

**Decisión.**
- Estructura de la solicitud (`schema: "fasting-log"`, `schema_version: "1.0"`, cabecera con `tz_offset` y `person`, catálogos, una lista por entidad). Claves = nombres canónicos del modelo y control (`id`, `fast_id`, `created_at`, `updated_at`, `deleted_at`); `catalogs` incluye `metric_types`, `symptom_types`, `intake_types`, `products` y `product_schedules`.
- `blood_pressure_readings` es una lista propia (D-09) y `refeed_checks`, `meals` también (D-04): **diferencia con la estructura de ejemplo de la solicitud**, que se documenta en el changelog del formato.
- Serialización con `kotlinx.serialization`; `null` explícito siempre (nunca se omiten claves).
- JSON Schema draft 2020-12 en `schema/fasting-log-1.0.schema.json`, un archivo de ejemplo con un ayuno simulado completo y `schema/CHANGELOG.md`; una prueba valida cada exportación generada contra el esquema.
- Versionado: un cambio incompatible sube la versión mayor; la app rechaza importar una versión mayor que la suya.

**Consecuencias.**
- Gana: contrato verificable automáticamente; el receptor sustituye por `id` en vez de duplicar.
- Paga: el esquema es un artefacto más que mantener sincronizado con el modelo (cada evolución del modelo toca el esquema y su versión).

**Alternativas consideradas.** Seguir literalmente la estructura de ejemplo (sin listas para presión, comidas ni pasos): obligaría a desnormalizar entidades del modelo validado. CSV como formato principal: pierde la cabecera y los catálogos (queda como opción de H-037).

**Implementación (tanda 4, 2026-10-10).** Esquema publicado en `schema/fasting-log-1.0.schema.json` con ejemplo y `schema/CHANGELOG.md` (diferencias con la solicitud: listas y campos con nombres canónicos, `person` con los campos de `person_profile`). Ruta del esquema: `schema/` en la raíz del repositorio.

**Aclaración (EV-001, 2026-10-10).** Un plan reemplazado al cambiar la plantilla de un ayuno planificado viaja con `deleted_at` (MODELO-DATOS D-16); un ayuno borrado no viaja (D-15, borrado definitivo). Compatible: `schema_version` sigue en 1.0; registrado en `schema/CHANGELOG.md`.

## ADR-008: Datos precargados y textos fuera del código
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** Principio de la solicitud «todo configurable, nada fijo en el código»; catálogos precargados (H-007, H-010, H-011), plantilla de ejemplo sin valores clínicos (H-003), rangos de presión en configuración (P-11), textos traducibles.

**Decisión.**
- Datos iniciales en `:data/src/main/assets/seed/*.json` (mismo formato que el intercambio), cargados en la primera apertura dentro de una transacción.
- Rangos válidos de sistólica, diastólica y pulso en `assets/config/blood_pressure_ranges.json` (P-11).
- Factores de conversión de unidades precargadas como constantes de `:domain` (P-09): son física, no configuración.
- Todos los textos de interfaz en `strings.xml` (español); ninguna cadena visible en el código Compose.

**Consecuencias.**
- Gana: cambiar un catálogo inicial o un texto no requiere tocar lógica.
- Paga: la carga inicial es una operación más que probar; los textos de recursos no están disponibles en `:domain` (los mensajes de dominio se devuelven como códigos y la UI los traduce).

**Alternativas consideradas.** Datos iniciales en una migración SQL: más difícil de leer y de mantener alineada con el formato de intercambio.

## ADR-009: Inyección de dependencias manual
**Fecha:** 2026-10-08 · **Estado:** aceptada

**Contexto.** App pequeña, tres módulos, pocas dependencias por pantalla; S7 pide minimizar dependencias.

**Decisión.** Un `AppContainer` en `:app/di/` crea base de datos, repositorios, casos de uso y planificador; los ViewModels reciben sus dependencias por constructor mediante fábricas.

**Consecuencias.**
- Gana: sin procesador de anotaciones ni biblioteca extra; grafo de dependencias legible en un archivo.
- Paga: código de fábricas a mano; si la app crece mucho, migrar a Hilt.

**Alternativas consideradas.** Hilt: estándar y cómodo, pero añade generación de código y una dependencia grande para ~15 ViewModels. Koin: resolución en tiempo de ejecución, errores tardíos.

---

## ADR-SEG-001: Base de datos cifrada con SQLCipher y clave en Android Keystore
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S3, S4

**Contexto.** H-042: «los datos guardados en el teléfono están cifrados». La mayoría de campos de registro son **Crítico** (D-07). Además, las alarmas y alertas deben funcionar con el bloqueo de la app activo (H-042), así que la base debe poder abrirse en segundo plano sin intervención de la persona.

**Decisión.**
- Room sobre SQLCipher for Android (AES-256).
- Clave aleatoria de 256 bits generada en el primer arranque, guardada cifrada (envuelta) con una clave AES de Android Keystore **no ligada a autenticación del usuario** (para que los recordatorios abran la base con la app bloqueada).
- `android:allowBackup="false"` y reglas de extracción vacías: la base no sale por la copia de seguridad del sistema.
- Ningún campo Crítico ni PII se escribe en logs (S4); los logs de error registran solo identificadores y códigos.

**Consecuencias.**
- Gana: un archivo de base copiado del teléfono no es legible sin la clave del Keystore del propio dispositivo.
- Paga: tamaño del APK (+ ~varios MB por la librería nativa) y algo de coste de rendimiento; perder el Keystore (restablecer el teléfono) hace la base irrecuperable — por eso existen la exportación y la copia cifrada (ADR-SEG-005).
- No protege frente a un teléfono desbloqueado y con la app abierta (eso es tarea de ADR-SEG-002).

**Alternativas consideradas.** Confiar solo en el cifrado del sistema de archivos de Android: no cumple el criterio de H-042 frente a extracciones con el teléfono encendido. Clave ligada a biometría: impediría que los recordatorios y alertas funcionen con la app bloqueada.

## ADR-SEG-002: Bloqueo con la credencial del dispositivo
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S3, S4, S6

**Contexto.** H-042 (bloqueo opcional con huella o PIN), P-08 (sin PIN propio), decisión de UX: Seguridad y llamada de emergencia accesibles sin desbloquear; Datos personales siempre piden autenticación.

**Decisión.**
- `BiometricPrompt` con `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (huella o PIN/patrón del teléfono). La app no guarda ningún PIN.
- Con el bloqueo activo: se pide al abrir en frío y al volver tras 5 minutos en segundo plano. Excepciones sin desbloqueo: pantalla de Seguridad (reglas y llamada) y la alerta roja (su mensaje y decisión), porque detenerse a autenticarse juega contra la seguridad física.
- Datos personales, exportar e importar piden autenticación siempre.
- `FLAG_SECURE` en todas las pantallas con datos de salud (oculta el contenido en multitarea y capturas).
- Notificaciones con versión pública genérica en la pantalla de bloqueo («Tienes un recordatorio del ayuno»), sin valores.
- Si el teléfono no tiene credencial configurada, el interruptor no se puede activar (falla cerrado, S6).

**Consecuencias.**
- Gana: nada que custodiar en la app; experiencia familiar.
- Paga: `FLAG_SECURE` impide que la persona haga capturas de pantalla de sus datos; la excepción de la alerta roja muestra el mensaje de la regla a quien tenga el teléfono en la mano.

**Alternativas consideradas.** PIN propio: exige guardar un hash Crítico y gestionar intentos (P-08 lo descartó). Bloquear también Seguridad y alertas: más privado, pero puede retrasar una decisión de seguridad física.

## ADR-SEG-003: Exportación de datos Críticos iniciada solo por la persona
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S4, S6, S8

**Contexto.** La regla del método es no exponer campos Crítico en respuestas; H-036 exige exportar por diseño todos los datos de salud a otro sistema. Excepción deliberada que debe quedar justificada.

**Decisión.**
- La exportación existe **solo** como acción explícita de la persona, tras autenticación si el bloqueo está activo; nunca automática ni en segundo plano.
- El archivo se genera en el directorio de caché privado de la app y se entrega con `FileProvider` y la hoja de compartir; se borra la caché de exportaciones al terminar el envío y en cada arranque.
- Datos personales (`person_profile`) solo si `settings.export_person_enabled` (H-038); `emergency_contact` **nunca** se exporta.
- La app no registra en logs el contenido exportado.

**Consecuencias.**
- Gana: la salida de datos de salud queda limitada a un único camino controlado por la persona.
- Paga: una vez compartido, el archivo queda fuera del control de la app (en el destino que elija la persona); se acepta como decisión de la persona.

**Alternativas consideradas.** Cifrar también el archivo exportado: rompería el contrato con el sistema receptor, que lo carga directamente; la copia cifrada (ADR-SEG-005) cubre el caso de respaldo.

## ADR-SEG-004: App sin permiso de red ni terceros
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S4, S7

**Contexto.** H-041: sin internet, sin cuentas, sin analítica, publicidad ni bibliotecas que envíen datos a terceros.

**Decisión.**
- El manifiesto **no declara** el permiso `INTERNET`: el sistema operativo impide cualquier tráfico de red de la app.
- Dependencias permitidas solo de AndroidX, Kotlin y SQLCipher; cualquier dependencia nueva requiere justificación en el log de `ARQUITECTURA.md` y revisión de que no incluya telemetría.
- Una prueba de construcción verifica que el manifiesto final (tras fusionar dependencias) no contiene `INTERNET`.

**Consecuencias.**
- Gana: garantía verificable, no una promesa: ni un error ni una biblioteca pueden filtrar datos por red.
- Paga: cualquier función futura con red (envío a un servidor propio, ítem 7 de *Después de la versión base*) exigirá un ADR que reemplace este.

**Alternativas consideradas.** Declarar `INTERNET` y confiar en el código: no verificable.

## ADR-SEG-005: Copia de seguridad cifrada con contraseña
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S3, S4, S6

**Contexto.** H-042: copia de seguridad manual protegida con contraseña; restaurar con contraseña incorrecta falla sin revelar contenido.

**Decisión.**
- Contenido: el mismo documento de «exportar todo» (ADR-007), incluido `person_profile` siempre (es una copia propia, no un envío).
- Cifrado AES-256-GCM; clave derivada de la contraseña con PBKDF2-HMAC-SHA256, sal aleatoria de 16 bytes y 600 000 iteraciones; cabecera con versión, sal y vector de inicialización.
- Contraseña incorrecta o archivo alterado → fallo de autenticación de GCM → mensaje único «Contraseña incorrecta o archivo dañado» (S6). La contraseña no se guarda.
- Se guarda con el selector de documentos del sistema (`ACTION_CREATE_DOCUMENT`) en la ubicación que elija la persona.

**Consecuencias.**
- Gana: respaldo que sobrevive a perder el teléfono o el Keystore.
- Paga: si la persona olvida la contraseña, la copia es irrecuperable (se advierte al crearla); PBKDF2 es más débil que Argon2 frente a hardware especializado.

**Alternativas consideradas.** Argon2id: más robusto, pero requiere biblioteca nativa adicional (S7). Copia sin cifrar: contradice H-042.

## ADR-SEG-006: Importación validada, todo o nada
**Fecha:** 2026-10-08 · **Estado:** aceptada · **Principios:** S1, S6

**Contexto.** H-039 y H-040 importan archivos externos (entrada no confiable); un archivo inválido no debe modificar nada; una versión mayor se rechaza; campos desconocidos se listan.

**Decisión.**
- Límite de tamaño (50 MB) antes de leer; parseo estricto de tipos; validación de cada fila contra las restricciones del modelo (enums, rangos, obligatorios, XOR de propietario, referencias existentes).
- Toda la importación ocurre en una sola transacción: cualquier error revierte todo.
- Fusión por `id` (y por `code` en productos); ante conflicto gana el `updated_at` más reciente (decisión de UX aceptada) y el resumen lo muestra.
- Los textos importados se tratan como datos, nunca se interpretan (no hay HTML ni código en la app).

**Consecuencias.**
- Gana: un archivo malicioso o dañado no deja la base a medias.
- Paga: un archivo grande con un solo error se rechaza entero (se informa la primera fila inválida).

**Alternativas consideradas.** Importación parcial «lo que se pueda»: deja estados inconsistentes y dificulta saber qué entró.

## ADR-SEG-007: Borrado definitivo de un ayuno a petición de la persona
**Fecha:** 2026-10-10 · **Estado:** aceptada (EV-001) · **Principios:** S8 (excepción deliberada), S4, S6

**Contexto.** H-045 pide borrar un ayuno con todos sus datos. El resto del modelo usa borrado blando con rastro en `edit_log` (S8: «operaciones sobre datos Críticos […] se registran con quién/qué/cuándo»). El humano decidió que el borrado del ayuno sea definitivo y **sin traza**, aun contradiciendo S8 y la decisión de que las alertas no se borran.

**Decisión.**
- «Borrar ayuno» elimina físicamente, en una sola transacción, el `fast`, toda fila con su `fast_id` (registros, alertas y plan copiado, también las ya borradas en blando), los `metric_schedule` y `refeed_check` que cuelgan de su plan y las filas de `edit_log` cuyo `row_id` sea cualquiera de ellas (MODELO-DATOS D-15). No se escribe ninguna fila de bitácora del borrado.
- Confirmación explícita que dice qué se borra, cuántos registros, que no se puede deshacer y que los archivos ya exportados no cambian; «Cancelar» por defecto.
- Todo o nada: un fallo a mitad revierte el borrado entero.
- Después de confirmar, se retiran las notificaciones del ayuno y se reprograman los recordatorios; los receptores de notificación ignoran un ayuno inexistente.
- Nada del contenido borrado se escribe en logs (S4).

**Consecuencias.**
- Gana: la persona puede eliminar de su teléfono datos de salud que no quiere conservar.
- Paga: no hay forma de deshacer ni de saber después que el ayuno existió; una alerta que motivó el fin de un ayuno se pierde con él. Las páginas liberadas en la base quedan cifradas (ADR-SEG-001). Los archivos exportados antes y las copias cifradas (ADR-SEG-005) conservan el ayuno, e importarlos (H-040) lo devuelve.

**Alternativas consideradas.** Borrado blando restaurable (coherente con S8 y H-003/H-034): descartado por el humano. Borrado definitivo con traza mínima sin datos (`edit_log` con solo `row_id`, acción y hora): descartado por el humano, que prefiere no dejar constancia.

---

## Preguntas resueltas al aprobar el diseño

*Aprobado por el humano el 2026-10-08 con las propuestas por defecto.*


- **Q-01 — Episodios de alerta (ADR-006).** Propuesta: una regla no se repite mientras su condición siga cumpliéndose; se rearma con una lectura dentro del umbral. **Aceptado.**
- **Q-02 — Icono de alarma (ADR-005).** Usar alarmas de despertador muestra el icono de alarma en la barra de estado durante el ayuno. **Aceptado.**
- **Q-03 — Excepción del bloqueo (ADR-SEG-002).** La alerta roja y la pantalla de Seguridad se muestran sin desbloquear. **Aceptado.**
- **Q-04 — Capturas de pantalla (ADR-SEG-002).** `FLAG_SECURE` impide hacer capturas de los datos. **Se aplica siempre** en pantallas con datos de salud.
