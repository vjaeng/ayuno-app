# Guía de Estilo — ayuno-app

Vocabulario visual de la app (Jetpack Compose + Material 3, ADR-001). Todo color, tamaño y espaciado del código sale de estos tokens (`com.ayuno.app.ui.theme`); un componente nuevo se añade aquí antes de usarlo.

Reglas de producto que condicionan la guía (`specs/UX-UI/README.md`):

- **El rojo es solo para alertas** (`rule.level = alert`). Ni los errores de formulario ni las cifras usan rojo.
- **Ninguna cifra se colorea por su valor** (sin verde «bien» ni rojo «mal»): eso sería un juicio clínico.
- Personas en ayunas, cansadas y con una mano: texto grande, objetivos grandes, poca densidad.

## 1. Tokens

### 1.1 Color

Roles de Material 3 más dos roles propios (`notice`, `alert`). Sin verde en la paleta.

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `background` | `#FAFAF7` | `#121412` | Fondo de pantalla |
| `surface` | `#FFFFFF` | `#1B1D1B` | Tarjetas, hojas, diálogos |
| `surface_variant` | `#ECEDE6` | `#2A2D29` | Fondo de filas alternas, rejilla Anotar |
| `on_surface` | `#1B1C1A` | `#E4E3DE` | Texto principal |
| `on_surface_variant` | `#45473F` | `#C5C7BD` | Texto secundario, horas |
| `outline` | `#75786F` | `#8F9288` | Bordes de campos y divisores |
| `outline_strong` | `#1B1C1A` | `#E4E3DE` | Borde de campo con error (2 dp) |
| `primary` | `#1F5F8B` | `#9CCBF0` | Acción principal, interruptores activos, hitos alcanzados |
| `on_primary` | `#FFFFFF` | `#003352` | Texto sobre `primary` |
| `primary_container` | `#D2E4F3` | `#0E4A70` | Reloj, elemento seleccionado |
| `on_primary_container` | `#0B2E47` | `#D2E4F3` | Texto sobre `primary_container` |
| `disabled` | `#1B1C1A` al 38 % | `#E4E3DE` al 38 % | Controles inactivos; productos que salen («en gris») |
| `notice_container` | `#FFF4CC` | `#3D3000` | Fondo del aviso amarillo |
| `on_notice_container` | `#4A3800` | `#FFE08A` | Texto del aviso amarillo |
| `notice_accent` | `#B58100` | `#E8B634` | Icono y borde del aviso |
| `alert` | `#B3261E` | `#8C1D18` | **Solo** alerta roja a pantalla completa y marca de síntoma de alarma |
| `on_alert` | `#FFFFFF` | `#FFFFFF` | Texto sobre `alert` |
| `focus` | `#1F5F8B` | `#9CCBF0` | Anillo de foco (3 dp) |

Todos los pares texto/fondo de la tabla cumplen contraste AA (≥4.5:1; los pares principales superan 7:1). Cualquier par nuevo se verifica antes de añadirse.

### 1.2 Tipografía

Fuente del sistema (Roboto); tamaños en `sp` para respetar el tamaño de letra del sistema. **Mínimo 17 sp** en todo texto legible.

| Token | Tamaño / altura de línea | Peso | Uso |
|---|---|---|---|
| `display_clock` | 56 / 64 sp | Medium | Reloj del ayuno |
| `headline` | 24 / 32 sp | Medium | Título de pantalla, mensaje de alerta |
| `title` | 20 / 28 sp | Medium | Encabezado de sección, nombre en tarjeta |
| `body` | 17 / 24 sp | Regular | Texto general, filas de lista |
| `body_strong` | 17 / 24 sp | Medium | Valores anotados, horas |
| `label` | 17 / 24 sp | Medium | Botones, etiquetas de campo |
| `numeric_input` | 32 / 40 sp | Medium | Valor dentro del campo numérico |

Las cifras usan dígitos tabulares (`fontFeatureSettings = "tnum"`) para que el reloj y los valores no «bailen».

### 1.3 Espaciado, forma y tamaño

| Token | Valor | Uso |
|---|---|---|
| `space_xs` / `s` / `m` / `l` / `xl` / `xxl` | 4 / 8 / 12 / 16 / 24 / 32 dp | Escala única de márgenes y separaciones |
| `screen_padding` | 16 dp | Margen lateral de pantalla |
| `radius_s` / `m` / `l` | 8 / 12 / 16 dp | Campos / tarjetas y botones / hojas y diálogos |
| `touch_min` | 48 dp | Objetivo táctil mínimo de cualquier control |
| `button_height` | 56 dp | Botones principales y secundarios |
| `tile_size` | 96 dp | Botones de la rejilla Anotar |
| `row_min_height` | 56 dp | Filas de lista tocables |
| `elevation` | 0 dp (tarjetas con borde) · 3 dp (hojas y diálogos) | Profundidad mínima |

## 2. Componentes canónicos

| Componente | Cuándo usarlo | Cuándo no | Especificación |
|---|---|---|---|
| **BotonPrincipal** | La acción principal de la pantalla («Anotar», «Guardar», «Terminar el ayuno», «Crear ayuno») | Más de uno por pantalla | `primary` / `on_primary`, alto 56 dp, ancho completo al pie, `label` |
| **BotonSecundario** | Acción alternativa («No se hizo», «Ahora no», «Cancelar») | Acciones destructivas sin confirmación | Borde `outline`, texto `primary`, alto 56 dp |
| **BotonSeguridad** | Acceso a la pantalla de Seguridad, siempre visible durante el ayuno | Cualquier otro uso | Icono de escudo + «Seguridad», `primary_container`, 48×48 dp mínimo, esquina superior |
| **TileAnotar** | Cada tipo en la rejilla Anotar | Listas largas | 96×96 dp, `surface_variant`, icono + nombre en `label`, sin color por tipo |
| **CampoNumerico** | Valores de medición, cantidades | Texto libre | Teclado numérico, `numeric_input`, unidad a la derecha con selector si hay alternativa; error con borde `outline_strong` 2 dp + icono ⓘ + mensaje bajo el campo en `on_surface` |
| **CampoTexto** | Notas, motivos, nombres | Valores numéricos | `body`, borde `outline`, error como CampoNumerico |
| **SelectorFechaHora** | Hora de cualquier entrada | — | Muestra «viernes 23 oct, 12:01:40» + icono ✎; abre selectores nativos |
| **SelectorOpciones** | 2-5 opciones excluyentes (origen, posición, nivel, motivo) | Más de 5 opciones (usar lista) | Botones segmentados o radio en fila, 48 dp de alto |
| **Interruptor** | Activar o desactivar (medición en plantilla, regla, bloqueo) | Acciones con efecto inmediato irreversible | Switch de Material 3 con etiqueta tocable en toda la fila |
| **TarjetaReloj** | Reloj del ayuno en Inicio | — | `primary_container`, `display_clock`, restante y fin previsto en `body` |
| **BarraHitos** | Hitos del ayuno | — | Puntos rellenos (`primary`) alcanzados y huecos pendientes, con hora y fecha legibles por el lector de pantalla |
| **FilaPendiente** | Vista Hoy | — | Estado con icono **y** texto (✔ hecho · ○ pendiente · ! atrasado · ✖ no hecho), nunca solo con color; fila de 56 dp |
| **ItemLineaTiempo** | Historial y detalle del ayuno | — | Hora + hora del ayuno + tipo + valor (`body_strong`); alertas con icono de nivel |
| **FranjaInformativa** | Avisos persistentes no clínicos (permiso de notificaciones denegado, recordatorios no aceptados) | Alertas de reglas | `surface_variant`, icono ⓘ, texto + botón de texto |
| **AvisoAmarillo** | Regla con `rule.level = notice` | Errores de formulario | Hoja inferior `notice_container`, borde izquierdo `notice_accent` 4 dp, icono ▲, mensaje en `headline` |
| **AlertaRoja** | Regla con `rule.level = alert` | Cualquier otra cosa | Pantalla completa `alert`/`on_alert`, mensaje en `headline`, botones de 56 dp en blanco con texto `alert`; no se cierra sin decidir |
| **DialogoConfirmacion** | Confirmar fuera de rango, borrar, descartar cambios, «¿estás terminando el ayuno?» | Confirmaciones triviales | Diálogo Material 3, dos botones, la acción segura a la derecha |
| **Snackbar** | Confirmar un guardado («Glucosa guardada») | Errores que exigen acción | 4 s, `body`, sin acciones salvo «Deshacer» si aplica |
| **EstadoVacio** | Listas sin elementos | — | Texto en `body` que dice qué hacer + un BotonSecundario |
| **EstadoCargando** | Lectura en curso | Escrituras (usar botón deshabilitado con indicador) | Esqueleto de la estructura; nunca valores inventados |
| **EstadoError** | Fallo de lectura o escritura | Validación de campo (va en el campo) | Texto «qué pasó + qué hacer» + «Reintentar» |
| **EstadoSinPermisos** | Bloqueo activo; permiso del sistema denegado | — | Pantalla de desbloqueo (BiometricPrompt) o FranjaInformativa con botón a ajustes del teléfono |
| **BarraNavegacion** | Pestañas Inicio, Hoy, Historial, Más | Dentro de flujos modales | Navigation bar de Material 3, 4 destinos, icono + etiqueta |

## 3. Modo claro y oscuro

- Ambos temas con los valores de §1.1; se sigue la preferencia del sistema (el oscuro se activa de noche si el sistema lo hace).
- El tema dinámico de Android (Material You) **no** se usa: alteraría el rojo reservado y el contraste verificado.

## 4. Accesibilidad

- Contraste AA mínimo en todo par texto/fondo (ver §1.1).
- Foco visible: anillo `focus` de 3 dp en todo control enfocable.
- Objetivos táctiles ≥ 48 dp (`touch_min`); los frecuentes, más grandes (`tile_size`, `button_height`).
- Texto escalable en `sp`; las pantallas se prueban con el tamaño de letra del sistema al 200 % sin texto cortado (desplazamiento vertical permitido).
- Lector de pantalla: cada control con `contentDescription` o semántica propia; el reloj se anuncia como «33 horas y 12 minutos de ayuno»; los estados de la vista Hoy se leen en texto.
- Ningún significado transmitido solo por color (iconos + texto siempre).
- Las alertas rojas se anuncian como región urgente (`liveRegion = Assertive`) y vibran.

## 5. Voz y tono

- **Idioma:** español, tuteo, frases completas.
- **Sin siglas ni jerga:** «presión arterial», «sensor de glucosa», «vuelta a comer», «antiinflamatorios».
- **Fechas:** con día de la semana («viernes 23 de octubre, 07:02»); la hora del ayuno («hora 33») siempre junto a la fecha y hora reales.
- **Mayúsculas:** solo la primera letra de cada frase y de los nombres propios; botones con verbo en infinitivo o imperativo («Guardar», «Empieza el ayuno ahora»).
- **Errores:** qué pasó + qué hacer («No se pudo guardar. Inténtalo de nuevo; lo que escribiste sigue aquí.»). Nunca culpar.
- **Neutralidad:** sin juicios ni ánimos sobre las cifras o el resultado («Ayuno terminado · 30 horas y 40 minutos · decisión personal»); nunca «fracaso», «éxito», «¡bien hecho!».
- **Mensajes de reglas:** se muestran exactamente como los escribió la persona.
