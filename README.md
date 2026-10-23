# ayuno-app

App Android personal para planificar y registrar ayunos de cualquier duración. **Registra, recuerda y avisa; no aconseja**: la app no trae umbrales clínicos, las reglas de alerta las define la propia persona.

Funciona sin internet y sin cuentas (no pide el permiso `INTERNET`), guarda los datos cifrados en el teléfono y exporta el registro a un archivo con formato fijo.

## Qué hace

- **Plantillas** reutilizables: objetivo, hitos, etapas (preparación, ayuno, vuelta a comer, posterior), mediciones con horarios, metas de líquidos, lista previa, avisos fijos, productos y reglas.
- **Reloj del ayuno** con hitos, fin previsto y próximo pendiente.
- **Registro** en pocos toques: mediciones (glucosa, peso, presión sentado y de pie…), síntomas, líquidos y sal (con totales y sodio), suplementos y medicamentos, y notas.
- **Recordatorios locales** a la hora exacta, que se reprograman tras reiniciar o cambiar la hora, con silencio nocturno y acciones desde la notificación.
- **Reglas de alerta propias** (umbral, sostenida, síntoma de alarma, ingesta bajo la meta, «algo no va bien»), con registro de cada decisión.
- **Vista Hoy:** hecho, pendiente, atrasado y no hecho, con la hora del ayuno.
- **Vuelta a comer:** pasos con hora calculada desde el fin real, comidas y reintroducción.
- **Historial** con la línea de tiempo de cada ayuno.
- **Exportar e importar** el formato `fasting-log` (JSON con esquema publicado y tablas CSV opcionales); restaurar sin duplicar.

## Stack

- Kotlin, Jetpack Compose y Material 3 — `minSdk 29` (Android 10), `targetSdk`/`compileSdk 35`.
- Room sobre SQLite cifrado con SQLCipher (clave en Android Keystore).
- Alarmas exactas con `AlarmManager` y notificaciones locales.
- kotlinx-serialization para el formato de intercambio.
- Pruebas: JUnit 5 (dominio), y Robolectric con Compose UI Test (integración y E2E en la JVM).

## Estructura

```
app/      UI Compose, ViewModels, recordatorios y raíz de composición (di/)
domain/   Kotlin puro: modelo, casos de uso, motor de reglas, derivados, puertos
data/     Room + SQLCipher, repositorios, migraciones e intercambio (exchange/)
schema/   Esquema JSON del formato de exportación, ejemplo y registro de cambios
specs/    Especificación viva: historias, modelo de datos, UX, diseño (ADR), pruebas
docs/     Solicitud de origen
metodo/   Metodología de trabajo (referencia, no se edita)
```

Dependencias permitidas: `:app → :domain`, `:data → :domain`; `:app` solo conoce `:data` desde `di/`.

## Compilar e instalar

Requisitos: JDK 17 y Android SDK con la plataforma 35 (`local.properties` con `sdk.dir`).

```bash
./gradlew assembleDebug      # APK en app/build/outputs/apk/debug/
./gradlew installDebug       # instala en un teléfono conectado o emulador
```

La distribución es por APK (sin tienda). Al actualizar, la base de datos se migra conservando los datos.

## Pruebas

```bash
./gradlew test               # dominio, integración con Room y E2E (Robolectric)
./gradlew lint
./gradlew connectedAndroidTest   # verificación en dispositivo
```

El esquema vigente `schema/fasting-log-1.1.schema.json` (el 1.0 queda publicado como referencia) se genera desde la descripción del formato, y una prueba valida cada exportación contra él. Si el formato cambia, se regenera con:

```bash
./gradlew :data:testDebugUnitTest -PregenerateSchema=true --tests '*ExchangeTest*'
```

## Formato de exportación

`fasting-log` 1.0: UTF-8 sin BOM, claves en inglés con los nombres canónicos del modelo, horas locales con segundos y sin zona (el desfase va una vez en la cabecera), `null` = no anotado e identificadores estables. Ver `schema/CHANGELOG.md` y `specs/DISENO.md` (ADR-007).

## Estado

En construcción. Las historias entregadas y las pendientes están en `specs/HISTORIAS/README.md`.

## Privacidad

Sin red, sin analítica ni terceros. Los datos solo salen del teléfono cuando la persona exporta y elige dónde enviarlos; los datos personales solo se incluyen si los activa en Ajustes.
