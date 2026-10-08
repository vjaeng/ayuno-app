# Contrato de API — ayuno-app

**(no aplica)**

La app funciona sin servidor, sin cuentas y sin conexión (H-041): no expone ni consume ninguna API. Decisión del humano, 2026-10-08.

La única superficie programática es el **archivo de exportación e importación** (H-036 a H-040), que consume un sistema externo. Su formato no se documenta aquí: se especifica en el paso de Diseño (paso 6), con propuesta de ubicación en `specs/TRANSACCIONES-CLAVE/` (exportar y restaurar son operaciones críticas) más el JSON Schema entregable. Los nombres de sus claves son los canónicos de `specs/MODELO-DATOS.md` (decisión D-01).

Pendiente para Diseño: el archivo exporta por diseño campos **Crítico** (datos de salud, H-036), lo que contradice la regla general de no exponerlos; requiere ADR `SEG-` que lo justifique (exportación iniciada solo por la persona, vía hoja de compartir, datos personales opt-in según H-038).
