# 07 — El Tejido ⭐ (compuerta de trazabilidad)

## Propósito

El Tejido es la fase que **cruza los hilos con la urdimbre**: verifica y materializa la trazabilidad bidireccional entre `specs/HISTORIAS/` y `specs/MODELO-DATOS.md`. Es el principio P3 en acción y la compuerta previa a Diseño. Skill canónico: **`/trama-tejer`**.

Produce dos entregables simétricos:

- **Matriz CRUD** en cada historia: concepto del criterio → `entidad.campo` canónico → operaciones C/R/U/D.
- **Mapa de Consumo** en cada entidad: qué historias consumen cada campo y con qué operación.

Y un **reporte de nudos**: los defectos de trazabilidad encontrados, clasificados.

## Los cinco nudos

| Nudo | Nombre | Definición | Severidad |
|---|---|---|---|
| **N1** | Hilo suelto | Un concepto de una historia no tiene campo en el modelo | **Bloqueante** |
| **N2** | Campo huérfano | Un campo de dominio no es consumido por ninguna historia | Resolver o documentar |
| **N3** | Sinónimo sin reconciliar | Dos historias nombran distinto el mismo concepto y el glosario no lo resuelve | Resolver |
| **N4** | Operación incoherente | La matriz declara operaciones imposibles (p.ej. se actualiza un campo que nada crea; se lee algo que nunca se escribe) | **Bloqueante** |
| **N5** | Artefacto desfasado | UX-UI o CONTRATO-API usan nombres no canónicos o campos inexistentes | Resolver o excepción documentada |

**Regla de compuerta: no se entra a Diseño con N1 o N4 abiertos.** N2/N3/N5 se resuelven o se registran como excepción explícita con responsable y plazo.

## Procedimiento

1. **Barrido historia → modelo.** Por cada historia: extraer los conceptos de sus criterios, mapear cada uno a `entidad.campo` (consultando el glosario), determinar la operación (C/R/U/D) que el criterio implica, y poblar la matriz CRUD. Concepto sin campo → nudo N1.
2. **Barrido modelo → historias.** Por cada campo de dominio: recolectar las historias que lo referencian en sus matrices y poblar el Mapa de Consumo. Campo sin historia → nudo N2. (Estructuras de control: exentas.)
3. **Verificación de coherencia.** Sobre las matrices agregadas: ¿todo campo con U/R/D tiene algún C en el sistema? ¿Las operaciones son consistentes con las reglas de negocio? Incoherencia → nudo N4.
4. **Contraste con UX y API.** Muestrear los flujos y contratos: ¿usan solo nombres canónicos y campos existentes? Desfase → nudo N5.
5. **Reporte de nudos** y veredicto de compuerta: **abierta** (sin N1/N4) o **cerrada** (con lista de nudos a resolver).

## Formato de la matriz CRUD (en la historia)

```markdown
### Matriz CRUD (tejida el AAAA-MM-DD)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
| correo del miembro | `usuario.email` | ✓ | ✓ | | |
| rol asignado automáticamente | `usuario.rol` | ✓ | ✓ | | |
```

## Formato del Mapa de Consumo (en la entidad)

```markdown
**Mapa de Consumo** (tejido el AAAA-MM-DD)
| Historia | Operaciones | Campos |
|---|---|---|
| H-001 | C, R | email, rol |
```

## Los tres modos

| Modo | Alcance | Compuerta | Cuándo |
|---|---|---|---|
| **Completo** | Todas las historias y entidades | Estricta (N1/N4 bloquean) | Fundación, paso 5; deuda de trazabilidad acumulada |
| **Parcial (mini-Tejido)** | Solo historias/entidades tocadas por una evolución | Estricta sobre el alcance | Evolución, paso 5a |
| **Diagnóstico** | Todo, pero **sin bloquear** | No aplica — produce catálogo de nudos para `DEUDA-INICIAL.md` | Adopción, paso 11 |

## Anti-patrones

- **Tejer al redactar.** Poblar matrices mientras se escriben historias o modelo produce trazabilidad de una sola pasada, sin verificación cruzada. El Tejido es una fase posterior deliberada.
- **Compuerta decorativa.** Entrar a Diseño "mientras se resuelven los nudos" anula el método: los N1/N4 descubiertos en Construcción cuestan horas y en producción, reputación.
- **Tejer lo trivial.** Cambios que no tocan historias ni modelo no requieren Tejido.
