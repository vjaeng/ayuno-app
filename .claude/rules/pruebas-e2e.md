# Regla de Pruebas E2E

- **Herramienta:** Compose UI Test (instrumentado, en emulador Android)
- **Comando:** `./gradlew connectedAndroidTest`
- **Ubicación:** `app/src/androidTest/` (escenarios nombrados por historia: `H005EmpezarAyunoTest.kt`)

Reglas fijas del método:

1. E2E solo para los flujos de historias de **prioridad alta**; el resto se cubre con unitarias/integración (pirámide en `specs/PRUEBAS.md`).
2. Los escenarios se nombran por historia (`h001-registro.spec`), y verifican los criterios de aceptación tal como están escritos.
3. **Anti-fixture-fantasma:** los datos de prueba se crean por el flujo real (o factories que lo respetan); prohibido sembrar estados que la matriz CRUD declara imposibles.
4. Un E2E rojo bloquea la entrega de su historia; no se marca skip sin registro en PRUEBAS.md.
