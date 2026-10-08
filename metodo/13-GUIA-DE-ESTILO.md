# 13 — Guía de Estilo Visual

## Propósito

`specs/GUIA-ESTILO.md` fija el vocabulario visual del proyecto para que cada pantalla nueva sea consistente sin discusión. Si no hay UI: "(no aplica)".

## Estructura

1. **Tokens** — paleta (con roles: primario, superficie, error, éxito…), tipografía (familias, escala), espaciado (escala base), radios y sombras. Valores concretos, no adjetivos.
2. **Componentes canónicos** — botones (variantes y estados), inputs (con estados de error), tarjetas, tablas, modales, toasts: para cada uno, cuándo usarlo y cuándo no.
3. **Modo claro/oscuro** — si aplica, mapeo de tokens por tema.
4. **Accesibilidad** — contraste mínimo AA, foco visible, targets táctiles ≥44px, textos alternativos.
5. **Voz y tono** — cómo se escribe en la UI (idioma, mayúsculas, mensajes de error útiles: qué pasó + qué hacer).

## Reglas

1. Los flujos de `specs/UX-UI/` referencian componentes de esta guía; un componente nuevo se agrega primero aquí.
2. Ningún color/tamaño hardcodeado en código: siempre tokens.
3. Los cambios de la guía son evoluciones (afectan artefactos), no ajustes silenciosos.
