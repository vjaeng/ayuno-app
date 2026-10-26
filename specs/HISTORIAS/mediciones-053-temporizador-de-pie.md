## H-053: Medir la presión de pie a su tiempo con un temporizador

**Como** persona que ayuna,
**quiero** que, tras tomarme la presión sentado, la app me cronometre desde que me pongo de pie y me pida las lecturas al minuto y a los 3 minutos,
**para** distinguir el salto normal de los primeros segundos de lo que de verdad cambia mi presión al estar de pie.

**Prioridad:** alta · **Estimación:** M

### Criterios de Aceptación
- [ ] Al guardar la toma sentado, la app ofrece «Ponte de pie ahora»; al tocar «Ya estoy de pie» empieza un temporizador visible.
- [ ] A cada tiempo del plan suena y pide una lectura de pie; de fábrica, a los 60 y a los 180 segundos. Los tiempos se configuran en la plantilla, en la medición de presión.
- [ ] Cada lectura de pie guarda los segundos que llevaba de pie, calculados desde «Ya estoy de pie» hasta la hora de la lectura, no tecleados, y queda enlazada a la toma sentado.
- [ ] Puedo saltarme el temporizador («Anotar sin esperar»): la lectura se guarda igual, sin segundos, y su ficha dice «tomada sin esperar».
- [ ] El resultado de la pareja muestra, por cada lectura de pie, cuánto cambiaron la de arriba, la de abajo y el pulso («a 1 min: −4 arriba · −2 abajo · +31 pulso»), sin colores de juicio.
- [ ] El temporizador suena aunque la pantalla se apague o cambie de app, y puedo cancelarlo; si lo cancelo, las lecturas ya guardadas se quedan.
- [ ] La exportación lleva los segundos de pie de cada lectura (vacío si se tomó sin esperar): una pareja hecha con el temporizador sale con dos lecturas de pie de 60 y 180 segundos que apuntan a la sentada.

*Solo sentado → de pie; medir acostado queda fuera (decisión del humano, EV-006).*

### Consideraciones de Seguridad
Lecturas y segundos de pie son datos de salud (Crítico, S4). El temporizador usa las alarmas que la app ya tiene; sin permisos nuevos.

### Matriz CRUD (pendiente del mini-Tejido, EV-006)
| Concepto en criterio | Campo canónico | C | R | U | D |
|---|---|---|---|---|---|
