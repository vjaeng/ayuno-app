# 09 — Construcción

## Propósito

Implementar historia por historia usando la **matriz CRUD como fuente de verdad** de qué campos tocar y con qué operación. Construcción y Pruebas (`10-PRUEBAS.md`) se retroalimentan: no es secuencia, es ciclo.

## Prerrequisitos

- Tejido cerrado (compuerta abierta) para las historias a construir.
- `specs/DISENO.md` y `specs/ARQUITECTURA.md` existentes.
- La historia en estado "tejida" en el índice.

## El ciclo por historia

1. **Leer** la historia completa: narrativa, criterios, matriz CRUD, consideraciones de seguridad.
2. **Bloque de comprensión** (solo si se modifica código existente — ver abajo).
3. **Implementar** siguiendo el checklist de `ARQUITECTURA.md`, con los nombres canónicos de la matriz — nunca los conceptos genéricos de la narrativa.
4. **Probar** según la matriz: cada campo/operación de la matriz tiene verificación (ver `10-PRUEBAS.md`).
5. **Actualizar** el índice de historias (estado → "en construcción" → "entregada") y el log de decisiones si hubo decisiones de implementación no triviales.

## Bloque de comprensión (antes de modificar código existente)

Antes de cambiar código ya escrito, el agente redacta dos bloques y los presenta:

- **Bloque A — Intención:** qué se va a cambiar y qué criterio/historia lo pide.
- **Bloque B — Estado actual:** qué hace hoy el código afectado, **con citas literales** (archivo:línea). Este bloque obliga a verificar la comprensión contra el código real; sin él, los cambios entran con suposiciones que el código contradice en silencio.

Omitirlo solo en cambios triviales (typo, estilo). Para cambios que tocan más de una entidad o artefacto, es obligatorio. En una Evolución formal, el Bloque B ya quedó resuelto en el reporte de impacto (paso 2) — aquí solo se re-verifica que las citas sigan vigentes.

## Ciclo autónomo: hilar → aplicar → verificar (skill `/trama-construir`)

Cuando los artefactos están completos, la construcción puede delegarse al ciclo autónomo (principios A de autonomía controlada y P7 — los artefactos son el proposal):

1. **Hilar** — derivar de la historia una lista ordenada de tareas concretas (cada tarea: archivos, campos canónicos, prueba asociada).
2. **Aplicar** — ejecutar la siguiente tarea.
3. **Verificar** — correr pruebas y lint; contrastar contra la matriz CRUD y los criterios. Si falla, corregir antes de avanzar.
4. Repetir hasta agotar tareas; reporte final por historia.

**Condiciones de parada** (el agente se detiene y consulta): decisión de producto no resuelta por los artefactos, contradicción entre artefactos, nudo descubierto en vuelo, o prueba que exige cambiar un criterio.

**Paralelización.** Historias que no comparten entidades (verificable en los Mapas de Consumo) pueden construirse en paralelo con subagentes, uno por historia.

## Reglas

1. Nombres canónicos siempre; si la matriz y el código discrepan, detenerse: es un nudo, no una errata.
2. Sin pruebas no hay historia entregada.
3. Toda desviación del checklist de `ARQUITECTURA.md` se registra como deuda o se corrige — nunca se silencia.
4. Consultar `metodo/SEGURIDAD.md`: validación de entradas, autorización en servidor, secretos fuera del código, logging sin datos Críticos.
5. **Regla anti-fixture-fantasma:** las pruebas usan datos que el propio flujo puede crear; prohibido inventar fixtures que asumen estados imposibles según la matriz CRUD.
