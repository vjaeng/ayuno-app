# Regla de Diseño de UI

Aplica solo si el proyecto tiene interfaz de usuario.

1. Todo componente nuevo usa los **tokens y componentes canónicos** de `specs/GUIA-ESTILO.md`; nada de colores/tamaños hardcodeados. Un componente que no existe en la guía se agrega primero a la guía.
2. Toda pantalla nueva pertenece a un flujo de `specs/UX-UI/` y usa los **campos canónicos** de su tabla de mapeo.
3. Validar contra Laws of UX antes de dar por cerrada una pantalla: Hick (≤3 acciones principales), Jakob (patrones familiares), Fitts (objetivos frecuentes grandes/cercanos), Miller (agrupar en 5-9).
4. Estados no felices obligatorios: vacío, cargando, error, sin permisos.
5. Accesibilidad mínima: contraste AA, foco visible, targets ≥44px.
