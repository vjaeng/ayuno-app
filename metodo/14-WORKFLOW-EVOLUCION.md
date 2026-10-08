# 14 — Workflow de Evolución (cambios sobre sistema vivo)

## Cuándo

`specs/` está poblado y alineado (por Fundación o por Adopción cerrada) y hay que cambiar algo: feature nueva, regla de negocio, módulo, corrección estructural. **No aplica** a cambios triviales que no tocan historias ni modelo (typo, estilo, bug puntual sin efecto en artefactos): esos se hacen directo, con el bloque de comprensión del doc 09 si tocan código no trivial.

Orquestador: **`/trama-evolucion`** (pausa entre fases para revisión humana). Control granular: invocar los sub-skills fase por fase.

## Archivos de coordinación

### `specs/EVOLUCIONES-EN-CURSO.md`
Tabla de evoluciones activas — se consulta **antes de tocar cualquier artefacto** (regla 6 de la memoria):

```markdown
| Id | Evolución | Estado | Artefactos afectados | Memoria | Inicio |
|---|---|---|---|---|---|
| EV-012 | Reservas recurrentes | En Diseño | H-004, MODELO(reserva), API(reservas) | EVOLUCIONES-HISTORICAS/EV-012.md | 2026-07-01 |
```

**Estados:** `Análisis` → `En Diseño` → `En Construcción` → cierre (fila se elimina). `Bloqueada` en cualquier punto, con motivo.

### `specs/EVOLUCIONES-HISTORICAS/EV-NNN.md` (memoria de la evolución)
Un archivo por evolución, que se va poblando paso a paso:

```markdown
# EV-NNN: [nombre]
**Estado:** ... · **Inicio:** ... · **Cierre:** ...

## Paso 1 — Intención
## Paso 2 — Reporte de impacto
## Paso 4 — Artefactos actualizados
## Paso 5 — Verificación (nudos y veredicto)
## Paso 6 — Construcción y pruebas
## Bloqueos (si hubo)
## Resultado final
```

El README del directorio mantiene el índice histórico de evoluciones cerradas.

## Los pasos

### Paso 0 — Retomar (solo si la evolución ya existe) — `/trama-evolucion-retomar`
Diagnóstico sin modificar nada: leer EVOLUCIONES-EN-CURSO, confirmar cuál se retoma, leer su memoria, `git status`/`git diff` sobre `specs/` para detectar desalineación memoria ↔ árbol. Reporta: estado, último paso poblado, próximo paso, bloqueos, archivos sin commit.

### FASE 1 — Planificación — `/trama-evolucion-plan` (pasos 1-3)

**Paso 1 — Intención.** Qué se quiere cambiar y por qué, en lenguaje del dominio. Detección de colisiones: ¿alguna evolución activa toca los mismos artefactos? Si sí, pausar y resolver con el humano.

**Paso 2 — Reporte de impacto.** Sobre los 8 frentes (HISTORIAS, MODELO, UX-UI, CONTRATO-API, TRANSACCIONES, PRUEBAS, ARQUITECTURA, seguridad), dos bloques por frente:
- **Comprensión** — qué dice HOY el artefacto, con **citas literales**. Obliga a verificar el estado real antes de proponerlo cambiar.
- **Modificación** — qué cambiará exactamente.

**Paso 3 — Registro.** Crear la memoria `EV-NNN.md` con pasos 1-2 poblados; agregar la fila en EVOLUCIONES-EN-CURSO con estado `Análisis`. **Pausa para revisión humana.**

### FASE 2 — Actualización — `/trama-evolucion-actualizar` (paso 4)

**Paso 4 — Actualizar artefactos** según el reporte de impacto, en orden estricto: **historias → modelo → UX / API → transacciones**. Anexar la lista consolidada de archivos modificados a la memoria. Estado → `En Diseño`. **Pausa.**

### FASE 3 — Verificación — `/trama-evolucion-verificar` (pasos 5a-5b)

**Paso 5a — Mini-Tejido.** `/trama-tejer` en modo parcial sobre el alcance (historias tocadas + entidades tocadas): matrices y Mapas actualizados, nudos reportados.

**Paso 5b — Revisión cruzada.** Consistencia residual entre artefactos del alcance (¿la pantalla usa el campo nuevo? ¿el contrato lo expone? ¿la TRX cambió?).

Veredicto: sin N1/N4 → estado `En Construcción`; con nudos bloqueantes → `Bloqueada` con lista. **Pausa.**

### FASE 4 — Construcción + Pruebas (paso 6) — delega en `/trama-construir`

Implementación según doc 09 con ciclo hilar → aplicar → verificar; pruebas según doc 10. Verificación arquitectónica: el código respeta capas y direcciones de `ARQUITECTURA.md`; desviaciones → corregir o registrar deuda. Poblar el paso 6 de la memoria. **Pausa.**

### FASE 5 — Cierre — `/trama-evolucion-cerrar` (paso 7)

1. Actualizar los **logs de decisiones** de los artefactos tocados (el porqué, no el qué).
2. Cerrar la memoria: metadatos finales + "Resultado final".
3. Agregar la fila al índice histórico; **eliminar** la fila de EVOLUCIONES-EN-CURSO.
4. Si hubo decisiones técnicas de alcance de proyecto: registrarlas en la tabla de `CLAUDE.md`.

## Reglas

- Una evolución que se abandona se cierra con estado y motivo — nunca se deja fila zombie.
- Dos evoluciones sobre los mismos artefactos no avanzan en paralelo: se secuencian o se fusionan.
- El reporte de impacto con citas literales (Comprensión) no es opcional: es lo que impide cambiar sobre suposiciones.
