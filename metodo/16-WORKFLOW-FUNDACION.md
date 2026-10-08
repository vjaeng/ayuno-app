# 16 — Workflow de Fundación (proyecto nuevo)

## Cuándo

Proyecto sin código previo: hay una idea o necesidad de negocio, todavía no hay entidades ni pantallas. La causalidad fluye en su dirección natural: **usuario → historias → modelo → código**. Se recorre **una sola vez**; al cerrar la primera entrega, todo cambio posterior es una Evolución (`14-WORKFLOW-EVOLUCION.md`). Si ya hay código, el workflow correcto es Adopción (`15-WORKFLOW-ADOPCION.md`).

El workflow completo se orquesta con `/trama-fundacion`, que ejecuta los pasos en orden pausando entre ellos para validación humana; cada paso puede también invocarse suelto con su skill.

## Los ocho pasos

### Paso 1 — Memoria (`CLAUDE.md`)
Completar la plantilla conversacionalmente (ver `02-MEMORIA-DEL-PROYECTO.md`): stack, convenciones, comandos, arquitectura prevista. **Sin dominio** — las entidades y requisitos van a `specs/`.
**Produce:** `CLAUDE.md` sin placeholders. **Referencia:** doc 02.

### Paso 2 — Historias (primera pasada)
Levantar las historias en lenguaje del dominio, con criterios verificables y matriz CRUD **vacía**. Se materializa `specs/HISTORIAS/`.
**Produce:** un archivo por historia + índice. **Referencia:** doc 03. **Skill:** `/trama-fundacion-historias`.

### Paso 3 — Modelo de datos
Derivar `specs/MODELO-DATOS.md` de las historias: entidades de dominio canónicas, estructuras de control separadas, clasificación de sensibilidad, glosario. Mapas de Consumo **vacíos**.
**Produce:** el modelo completo. **Referencia:** doc 04. **Skill:** `/trama-fundacion-modelo`.

### Paso 4 — UX-UI y Contrato de API (en paralelo)
Ambos consumen el modelo y usan nombres canónicos. Flujos con wireframes y validación Laws of UX; endpoints con auth explícita y errores enumerados.
**Produce:** `specs/UX-UI/` y `specs/CONTRATO-API/`. **Referencia:** docs 05 y 06. **Skill:** `/trama-fundacion-definicion`.

### Paso 5 — El Tejido ⭐ (COMPUERTA)
`/trama-tejer` en modo completo: matrices CRUD pobladas, Mapas de Consumo poblados, reporte de nudos. **No se avanza con N1 o N4 abiertos.** N2/N3/N5: resolver o excepción documentada.
**Produce:** trazabilidad bidireccional cerrada. **Referencia:** doc 07. **Skill:** `/trama-tejer`.

### Paso 6 — Diseño
ADRs (incluyendo `SEG-`) y `specs/ARQUITECTURA.md` (capas, patrones, checklist de features).
**Produce:** `specs/DISENO.md`, `specs/ARQUITECTURA.md`. **Referencia:** doc 08.

### Paso 7 — Construcción ⇄ Pruebas (iterativo)
Implementación historia por historia con la matriz CRUD como verdad; pruebas derivadas de criterios + matriz. Delegable al ciclo autónomo `/trama-construir` cuando los artefactos están completos.
**Produce:** código + pruebas por historia. **Referencia:** docs 09 y 10. **Skill:** `/trama-construir`.

### Paso 8 — Despliegue y Transacciones Clave
Runbook `specs/DESPLIEGUE.md` (el método documenta; el deploy lo ejecuta el pipeline/operaciones) y `specs/TRANSACCIONES-CLAVE/` para las operaciones que califiquen.
**Produce:** runbook + transacciones documentadas. **Referencia:** docs 11 y 12.

## Ejemplo condensado (una historia hasta la puerta de Construcción)

1. *Paso 2* — `H-001: reservar sala como empleado`; criterios: "solo salas del edificio del empleado", "máximo 2 horas por reserva". Matriz vacía.
2. *Paso 3* — nacen `sala.edificio`, `reserva.duracion`, `empleado.edificio` (dominio); `id/created_at` (control). Mapas vacíos.
3. *Paso 5* — `/trama-tejer` puebla la matriz: "sala del edificio" → `sala.edificio` (R), "duración máxima" → `reserva.duracion` (C,R); y el Mapa de Consumo inverso (SALA ← H-001). Sin nudos N1/N4 → compuerta abierta.
4. *Puerta de Construcción* — `/trama-construir` sabe exactamente qué implementar y qué probar; la ambigüedad se eliminó antes de la primera línea de código.

## Buenas prácticas

- Respetar la causalidad: si estás escribiendo entidades antes que historias, detente.
- La compuerta del Tejido no es formalidad: los nudos baratos aquí son caros después.
- Diseño ⇄ Construcción ⇄ Pruebas es ciclo, no cascada.
- La seguridad se pliega en cada paso (doc SEGURIDAD), no se deja para el final.
- Al cerrar la primera entrega: cambiar a Evolución. No se "vuelve a fundar".
