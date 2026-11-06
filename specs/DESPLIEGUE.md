# Despliegue — ayuno-app

Runbook de la app Android. El método documenta el despliegue; lo ejecuta la persona (`metodo/11-DESPLIEGUE.md`). Escrito para quien tenga menos contexto: sin pasos implícitos.

## 1. Topología

| Entorno | Dónde | Paquete | Nombre en el lanzador | Firma | Datos |
|---|---|---|---|---|---|
| **Producción** | Teléfono de la persona (Android 16) | `com.ayuno` | Ayuno | Clave release propia (§3) | Los reales |
| **Prueba** | Emulador o el mismo teléfono | `com.ayuno.debug` | Ayuno (prueba) | Clave de depuración del PC (`~/.android/debug.keystore`) | Propios, separados de los reales |

- No hay servidor, tienda ni red: la app no pide `INTERNET` (ADR-SEG-004). Se distribuye como APK firmado (ADR-005: `USE_EXACT_ALARM` impide publicarla en Google Play sin revisar ese ADR).
- Las dos variantes conviven en el teléfono sin tocarse: cada una tiene su paquete, su base cifrada y su clave de base en el Keystore (ADR-SEG-001).
- La base de cada paquete solo la abre ese paquete en ese teléfono. Desinstalar borra los datos y no hay copia del sistema (`allowBackup="false"`, ADR-SEG-001); la única vía para mover datos es exportar todo y restaurar (H-034, H-040).

## 2. Pipeline

Sin CI/CD. Todo se ejecuta a mano en el PC de desarrollo:

| Paso | Comando | Resultado |
|---|---|---|
| Pruebas | `./gradlew test` | Dominio, datos (Room, migraciones) y E2E con Robolectric |
| Lint | `./gradlew lint` | Sin errores |
| APK de prueba | `./gradlew assembleDebug` | `app/build/outputs/apk/debug/ayuno-<versión>-debug.apk` |
| APK oficial | `./gradlew assembleRelease` | `app/build/outputs/apk/release/ayuno-<versión>-release.apk`, firmado |
| Publicar el oficial | copiar a `dist/ayuno-<versión>.apk` | Sin sufijo; `dist/` y los `*.apk` no van al repositorio |

La firma se configura en `app/build.gradle.kts` con las propiedades de §3. Si faltan, `assembleRelease` genera el APK **sin firmar** (no se puede instalar); eso indica que la clave no está configurada en esa máquina.

## 3. Variables y secretos

Nombres y ubicaciones; los valores nunca se escriben aquí ni en el repositorio (`.gitignore` excluye `*.jks`, `*.keystore` y `*.apk`).

| Nombre | Dónde vive | Qué es |
|---|---|---|
| `AYUNO_RELEASE_STORE_FILE` | `~/.gradle/gradle.properties` | Ruta del almacén de claves: `~/keystores/ayuno-release.jks` (PKCS12) |
| `AYUNO_RELEASE_STORE_PASSWORD` | `~/.gradle/gradle.properties` | Contraseña del almacén |
| `AYUNO_RELEASE_KEY_ALIAS` | `~/.gradle/gradle.properties` | Alias de la clave: `ayuno` |
| `AYUNO_RELEASE_KEY_PASSWORD` | `~/.gradle/gradle.properties` | Contraseña de la clave (igual que la del almacén) |

- **Huella del certificado (SHA-256), pública:** `4627fdc13c0cb93483a27a691d359a6d7ab303c89b877a508dda7ab2e5dfb0b2`. Sirve para comprobar que un APK se firmó con la clave correcta.
- **Copia de la clave:** `ayuno-firma-backup-2026-11-06.zip` (AES-256) en Google Drive de la persona, con el `.jks`, las cuatro propiedades y un `LEEME.txt`. Se abre con la misma contraseña del almacén, guardada en el gestor de contraseñas de la persona (fuera de Drive). Ese zip se abre con 7-Zip o `7z x`; el descompresor de Windows no abre zips con AES-256.
- **Perder la clave** impide actualizar la app instalada: habría que exportar todo, desinstalar, instalar una app firmada con otra clave y restaurar.

### Restaurar la clave en otra máquina
1. Descargar el zip de Drive y abrirlo con `7z x ayuno-firma-backup-<fecha>.zip` y la contraseña.
2. `mkdir -p ~/keystores && chmod 700 ~/keystores`; mover `ayuno-release.jks` allí con `chmod 600`.
3. Añadir las líneas de `firma-ayuno.properties` a `~/.gradle/gradle.properties` (con `chmod 600`), ajustando `AYUNO_RELEASE_STORE_FILE` a la ruta real.
4. `./gradlew assembleRelease` y comprobar la huella (§5, paso 1).

## 4. Procedimiento de despliegue

### 4.1 Actualización normal (lo habitual)

Se instala **encima** de la versión oficial; los datos se conservan. **Nunca desinstalar.**

1. Pruebas y lint en verde (`./gradlew test lint`). Si la versión de la base cambia, `MigrationTest` cubre la migración desde la versión instalada en el teléfono, con un ayuno en curso (`specs/PRUEBAS.md`).
2. Subir `versionCode` (siempre mayor que el instalado) y `versionName` en `app/build.gradle.kts`.
3. `./gradlew assembleRelease` y copiar el APK a `dist/ayuno-<versión>.apk`.
4. Comprobar la firma (§5, paso 1).
5. En el teléfono, **antes de instalar**: Más → Exportar todo, y copiar el `.json` fuera del teléfono (PC o Drive). Es la red de seguridad del rollback (§6).
6. Instalar encima:
   - con cable: `adb install -r dist/ayuno-<versión>.apk`;
   - sin cable: copiar el APK al teléfono, abrirlo y permitir «instalar apps desconocidas».
7. Verificar (§5) y anotar la fila en el registro (§8).

Si `adb install` responde `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, el APK no está firmado con la clave oficial: **no desinstalar**; revisar §3.

### 4.2 Migrar datos entre paquetes o claves

Para pasar datos de un paquete a otro (p. ej. de la prueba a la oficial) o tras perder la clave:

1. En la app de origen: Más → Exportar todo. Si se rellenaron datos personales y se quieren conservar, activar antes en Ajustes → Datos personales «Incluir en el archivo exportado». La casilla «Incluir también tablas» no hace falta: restaurar solo lee el `.json`.
2. Copiar el `.json` al PC y comprobarlo contra `schema/fasting-log-<versión>.schema.json`: debe validar y contener los ayunos esperados en `fasts`.
3. Si el paquete de destino es el mismo que el de origen, desinstalar el origen ahora (borra sus datos).
4. Instalar el APK de destino y abrir Más → Restaurar con ese `.json`. El resumen previo debe mostrar las mismas cifras; al confirmar, los recordatorios se reprograman solos.
5. Volver a conceder lo que no viaja en el archivo (§4.3).

### 4.3 Tras una primera instalación

- Permitir notificaciones (la app las pide al crear el primer ayuno).
- Alarmas exactas: `USE_EXACT_ALARM` se concede al instalar en Android 13+; en Android 12, permitirlas en los ajustes de la app.
- Ajustes de Android → Apps → Ayuno → Batería → **Sin restricciones**, para que el fabricante no cierre la app en segundo plano (ADR-005).
- Bloqueo con la credencial del teléfono, si se usaba (Ajustes de la app).

## 5. Comprobaciones de salud

1. **Firma:** `$ANDROID_HOME/build-tools/<versión>/apksigner verify --print-certs dist/ayuno-<versión>.apk` debe mostrar la huella SHA-256 de §3.
2. **Instalado:** `adb shell dumpsys package com.ayuno | grep versionName` muestra la versión nueva.
3. **En la app:** abre sin error; Historial con todos los ayunos; si hay ayuno en curso, el reloj marca la hora correcta y Hoy muestra sus mediciones.
4. **Recordatorios:** el icono de alarma aparece en la barra de estado si hay un ayuno en curso o planificado, y el siguiente recordatorio llega a su hora.
5. **Pendiente de una vez (H-016):** 3 días seguidos con la app cerrada sin perder ningún recordatorio, con un reinicio en medio (`specs/PRUEBAS.md`).

## 6. Rollback

Android no deja instalar una versión **anterior** de la oficial encima de la nueva, y Room no abre una base de versión mayor que la que conoce. No hay vuelta atrás en el sitio:

1. Si la versión nueva abre: Más → Exportar todo y copiar el `.json` fuera. Si no abre, usar el `.json` exportado en §4.1, paso 5 (se pierde lo anotado después).
2. Desinstalar la versión nueva.
3. Instalar el APK anterior desde `dist/` (por eso se conservan los APK publicados).
4. Más → Restaurar con el `.json`. **Límite:** si la versión nueva subió el formato del archivo (`schema_version`), la anterior lo rechaza (H-040). En ese caso, restaurar el `.json` del paso 5 de §4.1 o, mejor, corregir hacia delante con una versión nueva.

## 7. Permisos

| Quién | Qué puede |
|---|---|
| La persona (dueña del teléfono y de la clave) | Generar el APK oficial, instalarlo y actualizarlo |
| Quien tenga solo el código | Generar la variante de prueba; un APK firmado con otra clave no se instala encima de la oficial |
| Nadie más | La clave no sale del PC de la persona ni del zip cifrado en su Drive |

## 8. Registro de despliegues

| Fecha | Versión (versionCode) | Tipo | Resultado |
|---|---|---|---|
| 2026-11-06 | 0.14.0 (16) | Primera versión oficial firmada; migración desde la debug `com.ayuno` (§4.2) | Correcto (confirmado por la persona): exportado (formato 1.2, válido contra el esquema), desinstalada la debug, instalada `dist/ayuno-0.14.0.apk`, restaurado (1 ayuno, 38 mediciones, 7 presiones, 5 síntomas, 2 comidas, 1 plantilla) |

## Log de decisiones

| Fecha | Decisión | Razón |
|---|---|---|
| 2026-11-06 | Clave release propia fuera del repo (`~/keystores/`, propiedades en `~/.gradle/gradle.properties`), con copia cifrada en Drive | Firmar la versión oficial sin depender de la clave de depuración del PC |
| 2026-11-06 | La variante de prueba se instala como `com.ayuno.debug` («Ayuno (prueba)») | Probar sin desinstalar ni tocar los datos reales de la oficial |
| 2026-11-06 | El APK oficial se publica en `dist/ayuno-<versión>.apk` y se conservan los anteriores | Nombre limpio para instalar; los anteriores hacen posible el rollback (§6) |
