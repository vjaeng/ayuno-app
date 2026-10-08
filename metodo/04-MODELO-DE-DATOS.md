# 04 — Modelo de Datos (la urdimbre)

## Propósito

`specs/MODELO-DATOS.md` define las entidades del sistema **derivadas de las historias** (principio P2). Cada campo de dominio existe porque una historia lo justifica; si no hay historia, el campo espera o se elimina.

## Estructura del archivo

1. **Diagrama** — ER en Mermaid con entidades y relaciones.
2. **Entidades de dominio** — una sección por entidad.
3. **Estructuras de control** — sección única, exenta de trazabilidad.
4. **Glosario** — reconciliación de sinónimos del dominio.
5. **Log de decisiones** — el *porqué* de los cambios (git registra el *qué*).

## Plantilla de entidad

```markdown
### USUARIO (dominio)
| Campo | Tipo | Restricciones | Clasificación | Regla de negocio |
|---|---|---|---|---|
| email | VARCHAR(255) | único, no nulo | PII | solo dominios autorizados |
| rol | VARCHAR(50) | enum(member, admin) | Sensible | default "member" |

**Mapa de Consumo**
*Pendiente — se puebla en el Tejido.*
| Historia | Operaciones | Campos |
|---|---|---|
```

## Dominio vs. control (principio P4)

**Dominio:** campos que una historia justifica (email, rol, fecha_limite). Sujetos a trazabilidad total.

**Control:** infraestructura técnica — `id`, `created_at`, `updated_at`, `deleted_at`, FKs puramente estructurales. Se declaran una vez en su propia sección y quedan **exentos** del Tejido. Ante la duda ("¿`estado` es dominio o control?"): si alguna historia lo menciona o lo necesita para un criterio, es dominio.

## Clasificación de sensibilidad

Todo campo de dominio se clasifica; la clasificación informa cifrado, logging, retención y control de acceso (ver `metodo/SEGURIDAD.md`):

| Clase | Criterio | Ejemplo |
|---|---|---|
| **Público** | Sin riesgo si se expone | nombre de producto |
| **PII** | Identifica a una persona | email, teléfono |
| **Sensible** | Habilita decisiones o acceso | rol, saldo |
| **Crítico** | Compromiso = daño legal/reputacional | credenciales, tokens, datos de pago |

Ante la duda entre dos clases, elegir la **más restrictiva**.

## Reglas de derivación

1. Recorrer las historias y extraer cada **concepto de dato** de sus criterios.
2. Nombrar canónicamente: `entidad.campo`, en snake_case, singular la entidad.
3. Reconciliar sinónimos en el **Glosario** ("cliente" = "comprador" → `cliente`), dejando registro.
4. **No poblar el Mapa de Consumo ahora** — es entregable del Tejido. Poblarlo al crear el modelo invierte el proceso y produce trazabilidad de una sola dirección.
5. Antes de **modificar** un campo existente, consultar su Mapa de Consumo y actualizar la matriz CRUD de cada historia afectada (regla 4 de la memoria).

## Prompt sugerido (derivar el modelo en Fundación)

> Lee todas las historias de `specs/HISTORIAS/`. Extrae los conceptos de dato de sus criterios y deriva `specs/MODELO-DATOS.md` con la estructura de `metodo/04-MODELO-DE-DATOS.md`: entidades de dominio con nombres canónicos y clasificación de sensibilidad, estructuras de control separadas, glosario de sinónimos y diagrama Mermaid. Deja los Mapas de Consumo vacíos. Justifícame verbalmente cada entidad con las historias que la originan; si un campo no tiene historia, no lo incluyas — anótalo como pregunta.

## Compuertas de calidad

- [ ] Cada campo de dominio nace de al menos una historia (verificación informal aquí; formal en el Tejido).
- [ ] Estructuras de control separadas y completas.
- [ ] Todo campo clasificado.
- [ ] Glosario cubre los sinónimos detectados en las historias.
