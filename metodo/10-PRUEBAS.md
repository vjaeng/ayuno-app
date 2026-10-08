# 10 — Pruebas

## Propósito

`specs/PRUEBAS.md` define la estrategia; las pruebas mismas viven en el código. La fuente de verdad de **qué** probar es doble: los **criterios de aceptación** (comportamiento) y la **matriz CRUD** (datos y operaciones).

## Derivación de casos

Por cada historia:

1. **Cada criterio de aceptación** → al menos un caso (feliz o no feliz según el criterio).
2. **Cada fila de la matriz CRUD** → verificación de la operación declarada: si dice C, existe una prueba que crea y verifica persistencia; si dice U, una que modifica y verifica el nuevo estado; si una operación NO está declarada, existe la expectativa de que **no** ocurra por esa vía.
3. **Casos negativos de seguridad**: por cada endpoint con auth/rol, una prueba de acceso sin permiso (espera 401/403); por cada campo Sensible/Crítico, verificación de que no se expone donde el contrato no lo declara.

## Estructura de `specs/PRUEBAS.md`

1. **Estrategia** — pirámide del proyecto (unitarias / integración / e2e), herramientas, umbral de cobertura si aplica.
2. **Convenciones** — naming, ubicación, datos de prueba (regla anti-fixture-fantasma: solo estados alcanzables por el flujo real).
3. **Matriz historia × cobertura** — tabla: historia, casos derivados, tipo (U/I/E2E), estado.
4. **Casos transversales** — seguridad, rendimiento si hay criterios cuantitativos.

## Reglas

1. Una historia sin sus casos derivados no se marca "entregada".
2. Las pruebas usan **nombres canónicos** — son la verificación ejecutable del Tejido.
3. Prueba que falla = detenerse y corregir (código o criterio, decidiendo con el humano si el criterio está mal), nunca marcarla como skip silencioso.
4. E2E: solo los flujos de las historias de mayor prioridad; el detalle de herramientas vive en `.claude/rules/pruebas-e2e.md`.
