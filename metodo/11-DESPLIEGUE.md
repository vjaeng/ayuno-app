# 11 — Despliegue

## Alcance

El método **documenta** el despliegue; la **ejecución** es externa (pipeline CI/CD, operaciones). Ningún skill corre el deploy: el agente produce y mantiene el runbook `specs/DESPLIEGUE.md`.

## Estructura de `specs/DESPLIEGUE.md`

1. **Topología** — entornos (dev/staging/prod), qué corre dónde.
2. **Pipeline** — pasos del CI/CD, qué dispara cada uno, dónde vive la definición.
3. **Variables y secretos** — inventario por entorno (nombres, no valores; los secretos nunca se documentan en claro — ver `metodo/SEGURIDAD.md`).
4. **Procedimiento de despliegue** — pasos numerados, incluyendo migraciones de datos.
5. **Health checks** — cómo verificar que el despliegue quedó sano (endpoints, métricas, humo).
6. **Rollback** — procedimiento explícito y probado; un rollback no documentado es un incidente futuro.
7. **Permisos** — matriz mínima de quién/qué puede desplegar, derivada del principio de menor privilegio.

## Reglas

1. Todo cambio de pipeline o de variables se refleja en el runbook en la misma evolución que lo introduce.
2. Las migraciones destructivas (drop, rename) exigen ADR y plan de rollback antes de ejecutarse.
3. El runbook se escribe para el operador con menos contexto: sin pasos implícitos.
