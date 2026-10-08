# Convenciones de Código

<!-- Completar en la primera sesión (Fundación) o desde el código real (Adopción, paso 2). -->

- **Naming:** [p.ej. camelCase para funciones/variables, PascalCase para tipos, snake_case en BD]
- **Imports:** [p.ej. absolutos desde `src/`, agrupados: stdlib / terceros / propios]
- **Errores:** [p.ej. errores tipados por capa; nunca capturar y silenciar; mensajes al usuario según S6]
- **Async:** [p.ej. async/await siempre; prohibido mezclar con .then()]
- **Comentarios:** [p.ej. en español, solo para restricciones no evidentes en el código]
- **Formato:** [p.ej. formateador automático del stack; sin debates de estilo en PR]

Regla fija del método: los identificadores que representan campos del modelo usan el **nombre canónico** (`entidad.campo`) adaptado a la convención del lenguaje — nunca sinónimos del dominio.
