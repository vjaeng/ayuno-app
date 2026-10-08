## H-008: Anotar una medición en tres toques

**Como** persona que ayuna,
**quiero** abrir desde «Anotar» una rejilla de botones grandes con mis mediciones activas y guardar cada una en un formulario de una sola pantalla, con la unidad en que la leo,
**para** registrar aunque esté en ayunas, cansado y con una mano, sin perder la lectura original de mi aparato.

**Prioridad:** alta · **Estimación:** L

### Criterios de Aceptación
- [ ] Desde la pantalla principal llego a guardar una medición en 3 toques o menos y en menos de 30 segundos.
- [ ] El formulario usa teclado numérico para valores numéricos.
- [ ] La fecha y hora es «ahora» por defecto, con segundos, y se puede editar; cada medición admite nota opcional.
- [ ] La glucosa indica su origen (sensor continuo o glucómetro), admite flecha de tendencia opcional y se puede anotar en mg/dL o mmol/L.
- [ ] Un peso tecleado en lb se guarda en lb y también en kg; se muestran los dos y el tecleado nunca se sustituye por el convertido.
- [ ] Un valor fuera del rango válido pide confirmación; si confirmo, se guarda tal cual, sin redondear ni corregir.
- [ ] La entrada queda guardada en el teléfono antes de confirmarse en pantalla.

### Consideraciones de Seguridad
Datos de salud (S4): se guardan solo en el teléfono y no salen de él salvo exportación explícita. Toda entrada es hostil hasta validarse (S1).

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
