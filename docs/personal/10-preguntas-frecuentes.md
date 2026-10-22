# 10. Preguntas frecuentes

Las dudas que han salido mientras se construía el proyecto, con respuesta corta.

## Base de datos

**¿Por qué la app no crea las tablas sola?**
Porque eso lo hace Flyway con migraciones versionadas. Hibernate solo **comprueba** (`validate`). Así los cambios a la base quedan escritos y revisables.

**¿Puedo editar `V1` para agregar una columna?**
No, si ya se aplicó: Flyway detecta el cambio (checksum) y la app no arranca. Crea una `V4`. En desarrollo puedes empezar de cero con `docker compose down -v`.

**¿Por qué no puedo borrar un usuario o un producto?**
Las claves foráneas con `RESTRICT` lo impiden si tienen boletas, blogs o carritos. Se usa la baja lógica (`activo = false`).

**¿Por qué el dinero es `BIGINT`?**
Porque el peso chileno no tiene decimales y así no hay errores de redondeo.

## Código

**¿Qué es `open-in-view`?**
Si está en `true`, Spring mantiene la conexión abierta hasta que termina de armar la respuesta, y puedes consultar la base desde el controller sin darte cuenta. En `false` la conexión se suelta al salir del service, así que **todo lo que necesites debe cargarse y convertirse a DTO dentro del service**.

**¿Por qué el service devuelve DTOs y no entidades?**
Para no exponer campos internos y porque las relaciones `LAZY` (como `Comuna.region`) solo se pueden leer dentro de la transacción.

**¿Por qué la clave se llama `clave` y no `contrasenia`?**
Para que el DTO no se confunda con la columna que guarda el hash. Así es difícil devolver el hash por error.

**¿Por qué el `Request` no trae el rol?**
Para que nadie pueda registrarse como administrador mandando `"rol": "ADMINISTRADOR"`. El service asigna `CLIENTE`.

**¿Qué hace `@Transactional`?**
Agrupa lo que hace el método: o se guarda todo, o nada. En lecturas se usa `readOnly = true`.

**¿Cuándo lanzo una excepción propia?**
`RecursoNoEncontradoException` si algo no existe (404). `ReglaNegocioException` si la petición es válida pero choca con una regla (409).

## Seguridad

**¿Qué diferencia hay entre 401 y 403?**
401: no sé quién eres. 403: sé quién eres, pero no tienes permiso.

**¿Por qué un correo inexistente y una clave incorrecta dan el mismo error?**
Para que nadie pueda descubrir qué correos están registrados.

**¿Puedo anular un token antes de que venza?**
No directamente. Pero si pones `activo = false` al usuario, el filtro lo rechaza en la siguiente petición, porque consulta la base cada vez.

**¿El token está cifrado?**
No, está **firmado**. Cualquiera puede leer su contenido; nadie puede modificarlo sin que se note. Por eso no lleva datos sensibles.

**¿Qué pasa si cambio `JWT_SECRET`?**
Todos los tokens emitidos dejan de ser válidos y los usuarios deben iniciar sesión de nuevo.

**¿Por qué la clave tiene un máximo de 72 caracteres?**
BCrypt solo usa los primeros 72 bytes.

**¿Es suficiente ocultar el botón en React?**
No. La API debe rechazar la acción por su cuenta.

## Configuración

**Cambié el YAML y no pasó nada.**
Spring ignora en silencio las propiedades mal escritas (así pasó con la zona horaria). Revisa el nombre exacto y la sangría.

**¿Por qué Spring no lee mi `.env`?**
Porque `.env` lo lee Docker Compose, no Spring. Tienes que cargar las variables en la terminal (`set -a; source .env; set +a`) o en IntelliJ.

**¿Qué significa `${DB_USER:root}`?**
Usa la variable `DB_USER`; si no existe, `root`. Sin los dos puntos, la variable es obligatoria.

**¿Dónde veo el log?**
En la consola y, en `local`, en `logs/tecnofactory.log`.

## Pruebas

**¿Por qué `contextLoads` falla en mi equipo?**
Porque arranca toda la aplicación y necesita MySQL. Usa `./mvnw test -Dtest="*Test"` para correr el resto, o levanta Docker.

**¿Qué es `UnnecessaryStubbingException`?**
Preparaste un `when(...)` que el test nunca usó. Bórralo.

## Trabajo con la IA

**¿Por qué dice que no pudo compilar?**
El entorno donde trabaja no puede descargar las dependencias de Maven. Lo escrito se revisa leyendo, pero **tú debes ejecutar** `./mvnw test`.

**¿Cómo pido ayuda con un error?**
Pega solo las líneas relevantes (`ERROR`, `Caused by`), di qué cambiaste justo antes y qué esperabas.

## Para saber más

[Guía 14: leer errores](../guias/14-leer-errores-y-problemas.md) · [Referencia 25: glosario](../referencia/25-glosario.md) · [Referencia 24: decisiones de diseño](../referencia/24-decisiones-de-diseno.md)
