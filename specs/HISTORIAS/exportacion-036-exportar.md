## H-036: Exportar un ayuno o todo a un archivo con formato fijo

**Como** persona que ayuna,
**quiero** exportar un ayuno, o todos mis ayunos y plantillas de una vez, a un archivo con el formato acordado y entregarlo por la hoja de compartir del teléfono,
**para** analizar mis datos en otro sistema sin pasarlos a mano y tener una copia completa de mi registro.

**Prioridad:** alta · **Estimación:** L

### Criterios de Aceptación
- [ ] El archivo de un ayuno se nombra con la fecha del inicio real del reloj y los primeros 8 caracteres del identificador del ayuno.
- [ ] «Exportar todo» incluye todos los ayunos, plantillas y catálogos con el mismo formato.
- [ ] Las horas van en hora local con segundos y sin zona; el desfase horario va una sola vez en la cabecera; nada se convierte a hora universal.
- [ ] Un dato no anotado sale vacío, nunca como cero ni como texto vacío.
- [ ] Los números salen como números, con punto decimal.
- [ ] Cada entrada lleva un identificador estable: reexportar el mismo ayuno da los mismos identificadores.
- [ ] Los catálogos y la plantilla usada viajan dentro del archivo.
- [ ] El archivo valida contra el esquema publicado.
- [ ] La app no sube nada por su cuenta.

### Consideraciones de Seguridad
Datos de salud (S4): la exportación es la única vía de salida y la decide el usuario.

### Matriz CRUD
*Pendiente — se puebla en el Tejido.*
