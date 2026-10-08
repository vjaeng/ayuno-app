# SEGURIDAD — Principios de Codificación Segura

Guía agnóstica de stack que el agente consulta **al escribir o revisar código**. Es disciplina interna, no checklist de auditoría: OWASP (ASVS, Top 10) y STRIDE quedan como referencia informativa para razonar amenazas, nunca como verificación literal numerada (principio P6).

## Cómo se pliega la seguridad en cada fase

| Fase | Pliegue |
|---|---|
| Historias | Sección "Consideraciones de Seguridad" cuando el criterio las hace evidentes |
| Modelo | Clasificación Público / PII / Sensible / Crítico por campo |
| Contrato API | Auth y rol explícitos por endpoint; errores 401/403 enumerados |
| Diseño | ADRs `SEG-` para decisiones con componente de seguridad |
| Construcción | Estos principios, aplicados en el código |
| Pruebas | Casos negativos: acceso sin permiso, datos que no deben exponerse |
| Despliegue | Secretos por entorno, menor privilegio en permisos de deploy |

## Los ocho principios

### S1 — Toda entrada es hostil hasta validarse
Validar en el servidor tipo, rango, formato y longitud de **toda** entrada externa (body, query, headers, archivos), con listas de lo permitido antes que listas de lo prohibido. La validación de cliente es UX, no seguridad.
**Verificar:** ¿cada endpoint valida su payload contra el contrato antes de tocar lógica?

### S2 — La autorización vive en el servidor y se decide en cada request
Cada operación verifica que **este** usuario puede hacer **esta** acción sobre **este** recurso, contra estado actual — antes de tocar el recurso. El cliente puede ocultar botones; nunca es fuente de verdad.
**Verificar:** ¿la verificación ocurre antes del acceso al dato? ¿hay pruebas negativas por endpoint?

### S3 — Los secretos no viven en el código
Credenciales, tokens y claves van en gestores de secretos o variables de entorno; jamás en el repositorio, en logs ni en respuestas de error. 
**Verificar:** ¿un `grep` de claves sobre el repo sale limpio? ¿los mensajes de error filtran internals?

### S4 — Los datos se tratan según su clasificación
La clase del campo (modelo) dicta el trato: **Crítico** → cifrado en reposo, nunca en logs ni respuestas no contractuales; **Sensible** → acceso justificado y logueado; **PII** → minimización y retención definida.
**Verificar:** ¿el logging excluye Crítico y enmascara PII? ¿las respuestas exponen solo lo contratado?

### S5 — Menor privilegio en todo actor
Usuarios, servicios, tokens y pipelines reciben el mínimo permiso que su función exige, verificado (no más de lo declarado).
**Verificar:** ¿el rol de la BD del servicio puede hacer DROP? ¿por qué?

### S6 — Fallar cerrado y sin filtrar
Ante error o duda, denegar. Los errores al usuario dicen qué hacer, no cómo está construido el sistema (sin stack traces, sin SQL, sin rutas internas).

### S7 — Dependencias son superficie de ataque
Actualizaciones regulares, lockfiles versionados, alerta de vulnerabilidades activa. Agregar una dependencia es una decisión (¿mantenida? ¿necesaria?), no un reflejo.

### S8 — Lo crítico deja rastro
Autenticación, cambios de permisos, operaciones sobre datos Críticos y transacciones clave se registran con quién/qué/cuándo — sin volcar los datos mismos (S4).

## Regla de uso

Al construir o revisar, el agente contrasta el cambio contra S1-S8 y menciona explícitamente los que apliquen. Si un principio se viola a sabiendas, se registra como ADR `SEG-` con la razón — nunca en silencio.
