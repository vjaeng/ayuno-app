# 06 — Contrato de API

## Propósito

`specs/CONTRATO-API/` define la superficie programática del sistema con **nombres canónicos del modelo**. Se refina en paralelo con UX-UI (ambos consumen el modelo). Si no hay API, README con "(no aplica)".

## Forma

Directorio con **un archivo por servicio** (`[servicio].md`) y un `README.md` índice con las **convenciones globales**: base URL, versionado, formato de errores, paginación, autenticación por defecto.

## Plantilla de endpoint

```markdown
### POST /usuarios
**Historias:** H-001 · **Auth:** pública · **Rol requerido:** n/a

**Request**
| Campo | Canónico | Tipo | Reglas |
|---|---|---|---|
| email | `usuario.email` | string | dominio autorizado |

**Response 201**
| Campo | Canónico |
|---|---|
| id | control |
| email | `usuario.email` |

**Errores:** 400 email inválido · 409 email duplicado · 422 dominio no autorizado
```

## Reglas

1. **Payloads mapeados a canónico.** Campo de payload sin campo canónico (o viceversa cuando la historia lo pide) = nudo N5.
2. **Cada endpoint cita sus historias.** Endpoint sin historia es superficie especulativa — y superficie de ataque gratuita.
3. **Auth explícita por endpoint**: pública / autenticada / rol. La autorización se valida en servidor, en cada request (ver `metodo/SEGURIDAD.md`).
4. **Errores enumerados**, incluidos los de permiso (401/403) — serán casos negativos en PRUEBAS.
5. Nunca exponer campos `Crítico` en respuestas; los `Sensible` solo con justificación en el propio contrato.

## Prompt sugerido

> Lee historias y modelo. Deriva `specs/CONTRATO-API/` según `metodo/06-CONTRATO-API.md`: un archivo por servicio, convenciones globales en el README, endpoints con mapeo canónico, auth explícita y errores enumerados. Señálame cualquier endpoint que no puedas justificar con una historia.
