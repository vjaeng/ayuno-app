# Convenciones de Código

- **Naming:** Kotlin estándar — `camelCase` para funciones y propiedades, `PascalCase` para tipos, `UPPER_SNAKE_CASE` para constantes; tablas y columnas Room en `snake_case` idéntico al nombre canónico del modelo. Paquete raíz `com.ayuno` (`.app`, `.domain`, `.data`).
- **Imports:** sin comodines; `:domain` nunca importa `android.*`, `androidx.room.*` ni `kotlinx.serialization.*`; fuera de `com.ayuno.app.di` nadie en `:app` importa `com.ayuno.data` (ver `specs/ARQUITECTURA.md`).
- **Errores:** el dominio devuelve resultados tipados (`sealed interface`) con códigos; la UI los traduce a mensajes llanos (qué pasó + qué hacer). Nunca capturar y silenciar. Nada Crítico ni PII en logs (S4, S6).
- **Async:** corrutinas y `Flow`; `viewModelScope` en ViewModels; prohibido `runBlocking` y `GlobalScope` fuera de pruebas.
- **Tiempo:** siempre a través del `Clock` inyectado; `LocalDateTime` truncado a segundos (ADR-004).
- **Textos:** toda cadena visible en `strings.xml` (español); comentarios en español, solo para restricciones no evidentes en el código.
- **Formato:** ktlint (estilo oficial de Kotlin); sin debates de estilo en revisión.

Regla fija del método: los identificadores que representan campos del modelo usan el **nombre canónico** (`entidad.campo`) adaptado a la convención del lenguaje — nunca sinónimos del dominio.
