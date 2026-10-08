# 12 — Transacciones Clave

## Propósito

`specs/TRANSACCIONES-CLAVE/` documenta las operaciones cuyo fallo parcial deja el sistema en estado inconsistente. Un archivo por transacción + README índice.

## Qué califica

Una operación es transacción clave si cumple al menos una:

- Toca **más de una entidad** con escritura.
- Tiene **efectos secundarios externos** (correo, pago, webhook, archivo).
- Requiere **rollback explícito** si falla a medias.
- Está detrás de un botón cuyo "doble clic" duplicaría efectos.

CRUD simple sobre una entidad **no** califica.

## Plantilla

```markdown
# TRX: [nombre]

**Historias:** H-00X · **Disparador:** [endpoint/botón/evento]
**Entidades tocadas:** [lista con operaciones]

## Secuencia
[Diagrama Mermaid sequenceDiagram con pasos numerados]

## Manejo de fallos
| Falla en paso | Estado resultante | Acción |
|---|---|---|
| 2 | [qué quedó a medias] | [rollback/compensación/reintento] |

## Idempotencia
[Cómo se evita el efecto duplicado ante reintento o doble disparo]
```

## Reglas

1. Se documenta al construir la operación (Fundación paso 8 / durante la Evolución que la introduce), no después del primer incidente.
2. Cada fila de "Manejo de fallos" tiene su caso de prueba en `specs/PRUEBAS.md`.
3. Si la transacción involucra campos Críticos (pagos, credenciales), exige ADR `SEG-` en Diseño.
