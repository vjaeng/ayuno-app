# Regla de Arquitectura

La descripción operacional completa vive en `specs/ARQUITECTURA.md` — leerla antes de crear módulos, servicios o capas nuevas.

Reglas fijas del método:

1. Respetar las **direcciones de dependencia permitidas** declaradas en `specs/ARQUITECTURA.md`. Una importación que las viola se corrige o se registra como deuda arquitectónica — nunca se silencia.
2. Toda feature nueva sigue el **checklist de features** de `specs/ARQUITECTURA.md`.
3. Las decisiones estructurales nuevas exigen ADR en `specs/DISENO.md` antes de implementarse (formato en `metodo/08-DISENO.md`); si tienen componente de seguridad, prefijo `SEG-`.
4. SOLID como vara de diseño de módulos; violaciones deliberadas → deuda documentada.
