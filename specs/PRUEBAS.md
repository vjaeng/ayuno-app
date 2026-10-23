# Pruebas — ayuno-app

Estrategia de pruebas. Las pruebas viven en el código; aquí se define qué se prueba, cómo y dónde. Fuente de verdad de los casos: **criterios de aceptación** (comportamiento) + **matriz CRUD** (datos y operaciones) de cada historia en `specs/HISTORIAS/`.

## 1. Estrategia

| Nivel | Qué cubre | Dónde | Herramientas | Comando |
|---|---|---|---|---|
| **Unitarias (U)** | Casos de uso, motor de reglas, derivados (reloj, hitos, totales, sodio, pendientes), validaciones, mapeo del formato de intercambio | `domain/src/test/` | JUnit 5, `kotlinx-coroutines-test`, `Clock` fijo | `./gradlew :domain:test` |
| **Integración (I)** | Repositorios y DAOs contra Room real (SQLite sin cifrar); migraciones; transacciones todo o nada; exportación validada contra el JSON Schema; carga inicial | `data/src/test/` | Robolectric, Room testing, validador de JSON Schema (solo en pruebas) | `./gradlew :data:testDebugUnitTest` |
| **E2E** | Flujos completos de las historias de prioridad **alta**, como los vería la persona | `app/src/test/` | Compose UI Test sobre Robolectric (JVM, sin dispositivo) | `./gradlew :app:testDebugUnitTest` |
| **Dispositivo (D)** | Lo que Robolectric no puede reproducir (ver §4.7) | `app/src/androidTest/`, `data/src/androidTest/` | AndroidX Test en el teléfono (Android 16) o emulador API 35 | `./gradlew connectedAndroidTest` |

**Sin dispositivo por ahora** (decisión del humano, 2026-10-08): integración y E2E corren en la JVM con Robolectric; lo que exige hardware real se agrupa en el nivel D y queda pendiente hasta conectar el teléfono (§4.7).

**Pirámide:** la mayor parte de los casos son unitarios (la lógica crítica vive en `:domain` puro, ADR-002); integración por cada operación de escritura de la matriz; E2E solo para el recorrido de cada historia de prioridad alta (`.claude/rules/pruebas-e2e.md`).

**Cobertura:** mínimo 90 % de líneas en `com.ayuno.domain.rules` y `com.ayuno.domain.derived` (comportamiento de seguridad); 80 % en el resto de `:domain`. Medido con Kover; el umbral rompe la construcción.

## 2. Convenciones

- **Nombres:** clase de prueba por historia u operación (`H020SustainedRuleTest`, `H005StartFastTest`); cada método nombra el criterio o la fila de la matriz que verifica: `` `dos lecturas bajo X separadas 20 minutos disparan la alerta` ``.
- **Nombres canónicos:** las pruebas usan los nombres del modelo (`fast.clock_start_actual` → `clockStartActual`); son la verificación ejecutable del Tejido.
- **Tiempo:** siempre con `Clock` fijo o avanzable; ninguna prueba depende de la hora real ni hace `sleep`.
- **Anti-fixture-fantasma:** los datos se crean por los casos de uso reales (o *builders* que llaman a esos casos de uso); prohibido insertar filas directamente en estados que la matriz declara imposibles (p. ej. una `alert` con `user_choice = continue` sin `choice_reason`, un `fast` `finished` sin `end_reason`).
- **Matriz CRUD → casos:** por cada fila con C, una prueba que crea y verifica persistencia; con U, una que modifica y verifica; con D, una que borra (blando) y verifica que deja de verse y se exporta marcado; una operación **no** declarada se verifica como no disponible por esa vía cuando sea relevante (p. ej. una alerta no se edita, H-034).
- **Rojo bloquea:** una prueba en rojo se corrige (código o criterio, decidiendo con el humano); nunca se marca *skip* sin registrarlo en §5.

## 3. Matriz historia × cobertura

Casos mínimos derivados = criterios de aceptación + filas de la matriz CRUD (algunas filas se cubren con el mismo caso que un criterio). Tipos: U = unitarias, I = integración, E2E = extremo a extremo.

| Historia | Título | Prioridad | Criterios | Filas CRUD | Tipos | Estado |
|---|---|---|---|---|---|---|
| H-001 | Crear una plantilla con qué medir y cuándo | alta | 7 | 10 | U + I + E2E | en verde |
| H-002 | Completar la plantilla con metas, lista previa y avisos | media | 4 | 8 | U + I | en verde |
| H-003 | Duplicar, borrar y partir de una plantilla de ejemplo | media | 4 | 8 | U + I | en verde |
| H-004 | Crear un ayuno desde una plantilla con dos datos | alta | 12 | 24 | U + I + E2E | en verde |
| H-005 | Empezar el ayuno con un toque | alta | 5 | 16 | U + I + E2E | en verde |
| H-006 | Ver de un vistazo dónde estoy | alta | 9 | 17 | U + E2E | en verde (parcial) |
| H-007 | Ajustar el catálogo de mediciones a lo que uso | alta | 5 | 10 | U + I + E2E | en verde |
| H-008 | Anotar una medición en tres toques | alta | 8 | 22 | U + I + E2E | en verde |
| H-009 | Anotar la presión sentado y de pie | alta | 4 | 6 | U + I + E2E | en verde |
| H-010 | Marcar síntomas de mi propio catálogo | alta | 5 | 8 | U + I + E2E | en verde |
| H-011 | Definir mis tipos de líquidos y electrolitos | alta | 3 | 4 | U + I + E2E | en verde |
| H-012 | Anotar lo que bebo y ver cuánto llevo | alta | 5 | 9 | U + I + E2E | en verde |
| H-013 | Mantener mi lista de suplementos y medicamentos | alta | 3 | 11 | U + I + E2E | en verde |
| H-014 | Decidir qué productos siguen y cuáles salen en el ayuno | alta | 3 | 6 | U + I + E2E | en verde |
| H-015 | Marcar cada toma de un producto | alta | 3 | 10 | U + I + E2E | en verde |
| H-016 | Que cada recordatorio suene a su hora pase lo que pase | alta | 4 | 4 | U + E2E | en verde (parcial) |
| H-017 | Resolver un pendiente desde la notificación | alta | 5 | 9 | U + I + E2E | en verde (parcial) |
| H-018 | Ver el día como una línea de tiempo | alta | 4 | 11 | U + E2E | en verde |
| H-019 | Definir una regla de alerta por umbral | alta | 5 | 18 | U + I + E2E | en verde |
| H-020 | Definir una regla de alerta sostenida en el tiempo | alta | 3 | 5 | U + I + E2E | en verde |
| H-021 | Definir una regla que salte con un síntoma de alarma | alta | 2 | 5 | U + I + E2E | en verde |
| H-022 | Definir una regla por ingesta bajo la meta a cierta hora | media | 2 | 6 | U + I | en verde |
| H-023 | Pedir una alerta manual cuando algo no va bien | alta | 3 | 5 | U + I + E2E | en verde |
| H-024 | Limitar una regla a ciertos momentos o contextos | media | 3 | 7 | U + I | pendiente |
| H-025 | Elegir el nivel y el mensaje de cada regla | alta | 3 | 2 | U + I + E2E | pendiente |
| H-026 | Que una regla programe otra medición u ofrezca terminar | alta | 3 | 3 | U + I + E2E | en verde |
| H-027 | Decidir yo ante cada alerta y que quede constancia | alta | 4 | 7 | U + I + E2E | en verde |
| H-028 | Tener mis reglas siempre a un toque | alta | 3 | 15 | U + E2E | en verde |
| H-029 | Llamar a mi contacto de emergencia con un toque | media | 3 | 2 | U + I | pendiente |
| H-030 | Terminar el ayuno con su hora y motivo | alta | 6 | 11 | U + I + E2E | en verde |
| H-031 | Definir y seguir mi plan de vuelta a comer | media | 4 | 10 | U + I | en verde |
| H-032 | Anotar cada comida y cómo me cayó | media | 2 | 5 | U + I | en verde |
| H-033 | Revisar mis ayunos pasados | media | 5 | 17 | U + E2E | en verde |
| H-034 | Corregir o borrar una entrada dejando rastro | alta | 7 | 19 | U + I + E2E | en verde (parcial: mediciones y presión, EV-004) |
| H-035 | Escribir una nota libre | alta | 3 | 2 | U + I + E2E | en verde |
| H-036 | Exportar un ayuno o todo a un archivo con formato fijo | alta | 9 | 29 | U + E2E | en verde |
| H-037 | Exportar también en tablas | baja | 2 | 2 | U | en verde |
| H-038 | Decidir si el archivo lleva mis datos personales | media | 2 | 4 | U + I | en verde |
| H-039 | Importar plantillas y mi lista de productos | media | 3 | 3 | U + I | en verde |
| H-040 | Restaurar todo desde un archivo exportado | alta | 4 | 3 | U + I + E2E | en verde |
| H-041 | Usar la app entera sin internet ni cuenta | alta | 3 | 7 | U + I + E2E | pendiente |
| H-042 | Proteger mis datos en el teléfono y en copias | media | 5 | 1 | U + I + E2E | pendiente |
| H-043 | Elegir mis unidades por defecto | media | 2 | 3 | U + I | pendiente |
| H-044 | Cambiar un ayuno que aún no ha empezado | media | 9 | 27 | U + I (+ E2E de acceso) | en verde |
| H-045 | Borrar un ayuno para siempre con todos sus datos | alta | 7 | 22 | U + I + E2E | en verde (parcial: caso D) |
| H-046 | Ver mis ayunos planificados y elegir uno | alta | 8 | 12 | U + E2E | en verde |
| H-047 | Anotar mi peso al empezar y al terminar y ver cuánto cambió | media | 7 | 13 | U + I + E2E | en verde |
| H-048 | Ver y corregir las mediciones del ayuno en curso | alta | 7 | 23 | U + I + E2E | en verde |
| H-049 | Elegir con palabras en las escalas de cómo me siento | alta | 9 | 16 | U + I + E2E | en verde; falta el caso D de actualizar sobre la instalada |

## 4. Casos transversales

### 4.1 Motor de reglas (H-019 a H-027) — tabla de casos obligatoria

| Caso | Esperado |
|---|---|
| Una lectura bajo X con regla «bajo X» | Dispara |
| Una lectura bajo X con regla «bajo X sostenida 20 min» | No dispara |
| Dos lecturas bajo X separadas 19 min | No dispara |
| Dos lecturas bajo X separadas 20 min | Dispara, `trigger_entry_ids` = ambas |
| Dos lecturas bajo X separadas 25 min con una lectura ≥ X entre ellas | No dispara (ADR-006) |
| Tercera lectura bajo X tras el disparo, sin lectura normal entre medias | No vuelve a disparar (episodio, Q-01) |
| Lectura normal y luego dos lecturas bajo X separadas ≥ 20 min | Vuelve a disparar |
| Síntoma de alarma marcado con mediciones normales | Dispara la regla de síntoma de alarma |
| Regla «ingesta bajo meta a las 14:00» con meta alcanzada a las 13:00 | No dispara |
| Regla filtrada por etapa fuera de esa etapa | No dispara |
| Regla «solo al despertar» con lectura sin marca | No dispara |
| Regla con acción «medir de nuevo en 15 min» | Programa un recordatorio a `fired_at` + 15 min que ignora el silencio nocturno |
| Ninguna regla (de ninguna acción) | Nunca cambia `fast.status` |

### 4.2 Reloj y tiempo (H-005, H-006, H-018)

- Transcurrido = ahora − `clock_start_actual` con `Clock` fijo; tras «reiniciar» (recrear todo el grafo con el mismo `Clock`) el valor es idéntico.
- Inicio real 10 min después del previsto → fin previsto e hitos recalculados desde la hora real.
- Horas truncadas a segundos y persistidas como `AAAA-MM-DDTHH:MM:SS`.

### 4.3 Recordatorios (H-016, H-017)

- **U:** el cálculo de pendientes produce las horas correctas por etapa, solo productos que siguen u opcionales activados, respeta el silencio nocturno salvo recordatorios de reglas.
- **I (Robolectric):** el `ReminderScheduler` registra en `AlarmManager` (sombra de Robolectric) exactamente las alarmas esperadas; los receptores de `BOOT_COMPLETED`, `TIME_SET` y `TIMEZONE_CHANGED` las vuelven a programar.
- **D:** con la app cerrada, una alarma programada a 1 min dispara la notificación real; tras reiniciar el dispositivo las alarmas siguen programadas.
- **Manual en el teléfono real (Android 16), antes del primer ayuno:** 3 días seguidos con la app cerrada sin perder ningún recordatorio de 07:00, 12:00, 18:00 y 22:00, incluido un reinicio. Se registra en `specs/DESPLIEGUE.md`.

### 4.4 Intercambio (H-036 a H-040)

- Cada exportación generada en pruebas valida contra el esquema vigente (`schema/fasting-log-1.1.schema.json` desde EV-005).
- Un campo no anotado se exporta como `null` (nunca `0` ni `""`); números como números con punto decimal; sin BOM.
- Reexportar el mismo ayuno produce los mismos `id`; editar una entrada conserva su `id` y deja la fila en `edit_log`.
- Exportar → borrar datos → importar restaura todo con horas al segundo; importar dos veces no duplica.
- Archivo con campos desconocidos: se importa y se listan; `schema_version` mayor: se rechaza sin modificar nada; fallo a mitad: no queda nada importado.
- EV-005: la exportación 1.1 lleva `catalogs.metric_options`; un archivo 1.0 sin ella se importa y conserva las palabras del teléfono; restaurar en un teléfono nuevo no duplica las 20 de fábrica; palabras con tipo inexistente, no numérico, número fuera de rango o duplicadas se rechazan sin modificar nada.

### 4.4b Actualizar la base sin perder datos (EV-005, H-049)

Restricción del humano: la app instalada tiene un ayuno en curso. Casos de integración (`MigrationTest`), obligatorios antes de generar el APK:

- Base en versión 7 con un ayuno `in_progress`, plantillas, mediciones de las cuatro escalas (con números que coinciden y que no coinciden con las palabras de fábrica), presión, síntomas, ingestas, reglas, alertas con y sin decisión, `edit_log` y borradas en blando → migración 7→8 → cada fila de cada tabla existente es idéntica (comparación columna a columna), el ayuno sigue `in_progress` con su `clock_start_actual`, y `metric_option` tiene las 20 palabras de fábrica.
- La migración no siembra palabras de un tipo que no exista o que la persona haya hecho no numérico, y no duplica si se ejecuta sobre una base que ya las tiene.
- La cadena completa 1→8 sigue pasando; `ReleasedSchemasTest` registra la versión 8; Room valida el esquema 8 exportado (`8.json`).
- Un fallo dentro de la migración deja la base en la versión 7 sin cambios (transacción de Room).

### 4.5 Seguridad (S1-S8)

| Caso | Principio | Nivel |
|---|---|---|
| El manifiesto fusionado no contiene `android.permission.INTERNET` | S7, ADR-SEG-004 | construcción |
| `allowBackup="false"` y reglas de extracción vacías | S4, ADR-SEG-001 | construcción |
| El archivo de base no se abre sin la clave (abrir con SQLite plano falla) | S4, ADR-SEG-001 | D |
| Ningún log contiene valores de campos Crítico o PII (se capturan los logs de Robolectric durante las E2E y se buscan valores sembrados) | S4 | E2E |
| Exportar sin opt-in: `person` vacío; con opt-in: incluido; `emergency_contact` nunca | S4, ADR-SEG-003 | I |
| Con bloqueo activo: Historial, Anotar, exportar e importar piden autenticación; Seguridad y la alerta roja no | ADR-SEG-002 | E2E |
| Activar bloqueo sin credencial en el teléfono: no se activa | S6 | U + E2E |
| La ventana **no** lleva `FLAG_SECURE`: capturas permitidas (bandera ausente: E2E; captura posible: D). Riesgo aceptado, EV-005 | S4, ADR-SEG-002 | E2E + D |
| Copia cifrada con contraseña incorrecta o archivo alterado: mensaje único, nada se restaura | S6, ADR-SEG-005 | I |
| Importación de archivo malicioso (tipos erróneos, referencias rotas, 60 MB, XOR de propietario violado): rechazo sin cambios | S1, ADR-SEG-006 | I |
| Alertas no editables ni borrables; toda corrección deja fila en `edit_log` | S8 | U + I |

### 4.6 Rendimiento y usabilidad con criterio cuantitativo

- Inicio muestra el reloj en < 1 s en arranque en frío (H-006): prueba **D** con medición de tiempo hasta el primer fotograma con reloj, en el teléfono.
- Anotar una medición en ≤ 3 toques desde Inicio (H-008): E2E que cuenta las interacciones.

### 4.7 Verificación en dispositivo (nivel D) — pendiente

Robolectric no ejecuta código nativo ni el sistema real de alarmas, notificaciones o ventanas. Estos casos solo se verifican en el teléfono (o emulador) y quedan **pendientes** mientras no haya dispositivo conectado:

| Caso | Historias / ADR |
|---|---|
| Cifrado real con SQLCipher y clave en Android Keystore | H-042, ADR-SEG-001 |
| Alarmas suenan con la app cerrada; se reprograman tras reinicio real y cambio de hora | H-016, ADR-005 |
| Notificaciones reales con acciones directas (anotar, posponer, no se hizo) | H-017 |
| Autenticación real con huella o PIN del teléfono | H-042, ADR-SEG-002 |
| Captura de pantalla posible y vista de recientes con contenido (EV-005) | H-042, ADR-SEG-002 |
| Reloj visible en < 1 s en arranque en frío | H-006 |
| Prueba manual de 3 días de recordatorios | H-016 |
| Las notificaciones visibles de un ayuno borrado desaparecen | H-045 |
| Actualizar sobre la versión instalada (sin desinstalar) con un ayuno en curso: el ayuno, su reloj, sus mediciones y sus recordatorios siguen igual; las escalas muestran sus palabras (EV-005) | H-049, ADR-003 |

**Verificaciones manuales registradas:**

| Fecha | Dispositivo | Qué se verificó | Resultado |
|---|---|---|---|
| 2026-10-08 | Teléfono de la persona, Android 16 (APK `ayuno-0.1.0-debug`) | La app abre (base cifrada con SQLCipher y clave en Keystore) y permite registrar un ayuno | Correcto. Sin verificar aún: alarmas con la app cerrada, reinicio real, 3 días, `FLAG_SECURE`, arranque < 1 s |

**Regla de entrega:** una historia con casos D pendientes puede quedar «construida», pero **no se marca «entregada»** hasta pasar sus casos D. Afecta a H-006, H-016, H-017, H-042, H-045 y H-049.

## 5. Excepciones registradas

*(Ninguna.)* Toda prueba omitida se registra aquí con historia, motivo, responsable y fecha límite.
