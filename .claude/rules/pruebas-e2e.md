# Regla de Pruebas E2E

- **Herramienta:** Compose UI Test sobre Robolectric (JVM, sin dispositivo); lo que exige hardware real va en `androidTest/` (nivel D de `specs/PRUEBAS.md`)
- **Comando:** `./gradlew :app:testDebugUnitTest` (E2E) · `./gradlew connectedAndroidTest` (nivel D, con dispositivo)
- **Ubicación:** `app/src/test/.../e2e/` (escenarios nombrados por historia: `H005StartFastE2eTest.kt`)

Reglas fijas del método:

1. E2E solo para los flujos de historias de **prioridad alta**; el resto se cubre con unitarias/integración (pirámide en `specs/PRUEBAS.md`).
2. Los escenarios se nombran por historia (`h001-registro.spec`), y verifican los criterios de aceptación tal como están escritos.
3. **Anti-fixture-fantasma:** los datos de prueba se crean por el flujo real (o factories que lo respetan); prohibido sembrar estados que la matriz CRUD declara imposibles.
4. Un E2E rojo bloquea la entrega de su historia; no se marca skip sin registro en PRUEBAS.md.
