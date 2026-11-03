# 26. Configuración YAML explicada propiedad por propiedad

Qué hace cada línea de `application.yaml`, `application-local.yaml` y `application-prod.yaml`. Recuerda: **Spring ignora en silencio las propiedades mal escritas**; un nombre incorrecto no da error, simplemente no hace nada.

## Cómo se combinan

Spring lee `application.yaml` (base) y encima el del perfil activo (`local` o `prod`). Si una clave aparece en ambos, **gana el del perfil**.

## `application.yaml` (base, común)

| Propiedad | Valor | Qué hace |
| --- | --- | --- |
| `spring.application.name` | `TecnoFactory` | Nombre de la aplicación |
| `spring.data.web.pageable.default-page-size` | `20` | Tamaño de página si el cliente no pide `size` |
| `spring.data.web.pageable.max-page-size` | `100` | Tope: `?size=5000` se recorta a 100 |
| `spring.data.redis.host/port` | `${REDIS_HOST:localhost}` / `6379` | Dónde estará Redis (aún no se usa) |
| `spring.datasource.url/username/password` | `${DB_URL:...}`, `${DB_USER:root}`, `${DB_PASSWORD:root}` | Conexión a MySQL; lo que va tras `:` es el **valor por defecto** |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate solo comprueba, nunca modifica |
| `spring.jpa.open-in-view` | `false` | La conexión se suelta al salir del service |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `America/Santiago` | Zona horaria con que Hibernate lee y escribe fechas |
| `spring.jackson.time-zone` | `America/Santiago` | Zona horaria de las fechas en el JSON |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `5MB` | Límite de subida de archivos (imágenes futuras) |
| `spring.mail.*` | `localhost:1025` | Servidor de correo; apunta a un SMTP de pruebas (Mailpit) que aún no existe |
| `spring.cache.type` | `redis` | Caché en Redis (**lo sobrescriben `local` y `prod` con `none`**) |
| `spring.cache.redis.time-to-live` | `10m` | Cuánto vive un dato en caché |
| `jwt.secret` | `${JWT_SECRET}` | Clave que firma los tokens. **Mínimo 32 caracteres. Sin valor por defecto desde el 3 nov**: antes traía una clave escrita en el código y, si se arrancaba sin la variable ni perfil, cualquiera podía firmar un token de administrador |
| `jwt.expiration-ms` | `3600000` | Vida del token: 1 hora |
| `cors.allowed-origins` | `${CORS_ORIGINS:http://localhost:5173}` | Orígenes del frontend permitidos (lista separada por comas) |
| `management.endpoints.web.exposure.include` | `health` | Único endpoint de Actuator publicado |

### La sintaxis `${VARIABLE:valor}`

- `${DB_USER:root}` → usa la variable `DB_USER`; si no existe, `root`.
- `${DB_PASSWORD}` (sin `:`) → **obligatoria**: si falta, la app no arranca (`Could not resolve placeholder`).

> Observación: la base trae valores por defecto (`root`/`root`, y una `jwt.secret` conocida) pensados solo para desarrollo. El perfil `prod` los **quita** a propósito.

## `application-local.yaml` (desarrollo)

| Propiedad | Valor | Efecto |
| --- | --- | --- |
| `spring.jpa.show-sql` | `true` | Imprime cada consulta SQL |
| `spring.jpa.properties.hibernate.format_sql` | `true` | SQL en varias líneas |
| `spring.flyway.enabled` | `true` | Aplica las migraciones al arrancar |
| `spring.cache.type` | `none` | Sin caché hasta que exista Redis |
| `springdoc.api-docs.enabled` / `swagger-ui.enabled` | `true` | Swagger disponible |
| `logging.level.root` | `INFO` | Librerías: eventos normales |
| `logging.level.com.factory` | `DEBUG` | Tu código: todo el detalle |
| `logging.file.name` | `logs/tecnofactory.log` | Guarda el log en un archivo |
| `logging.logback.rollingpolicy` | `10MB`, 7 archivos | Rotación |
| `management.endpoint.health.show-details` | `always` | `/actuator/health` muestra cada componente |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | Más endpoints de Actuator |
| `management.health.redis/mail.enabled` | `false` | No los comprueba (no existen aún) |
| `server.error.include-message` | `always` | El error por defecto trae su mensaje |
| `server.error.include-stacktrace` | `on_param` | Rastro solo con `?trace=true` |
| `server.error.include-binding-errors` | `always` | Indica qué campo falló |

## `application-prod.yaml` (producción)

| Propiedad | Valor | Efecto |
| --- | --- | --- |
| `spring.datasource.url/username/password` | `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}` | **Sin valor por defecto**: obligatorias |
| `spring.flyway.user/password` | `${FLYWAY_USER}`, `${FLYWAY_PASSWORD}` | Usuario con permisos de estructura (`tecnofactory_migrator`) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Igual que en local |
| `spring.jpa.show-sql` | `false` | No imprime consultas |
| `spring.cache.type` | `none` | Cambiará a `redis` cuando exista |
| `jwt.secret` | `${JWT_SECRET}` | **Sin valor por defecto**: falta → no arranca |
| `springdoc.*.enabled` | `false` | Sin Swagger ni descripción de la API |
| `logging.level.root` / `com.factory` | `WARN` / `INFO` | Mínimo ruido |
| `management.endpoint.health.show-details` | `never` | Solo `UP` o `DOWN` |
| `management.endpoints.web.exposure.include` | `health` | Único endpoint publicado |
| `server.error.include-message/stacktrace/binding-errors` | `never` | Los errores no revelan nada interno |

## Observaciones útiles

1. **No hay `CORS_ORIGINS` en `prod`**: cae al valor de la base (`http://localhost:5173`). Al desplegar **hay que definir la variable** con el dominio real, o el frontend en producción será bloqueado.
2. **`server.error.*` en `prod` y tu `GlobalExceptionHandler`:** tus errores propios siguen saliendo con su `mensaje`; las opciones `never` solo afectan a la página de error por defecto de Spring.
3. **Los logs a archivo solo están en `local`.** En producción la salida va a la consola del contenedor (`docker logs`).
4. **Orden de prioridad de una propiedad:** variable de entorno > archivo del perfil > `application.yaml`.

## Para saber más

[Referencia 04: perfiles](04-perfiles-spring.md) · [Referencia 03: variables y claves](03-variables-y-claves.md) · [Referencia 20: configuración base](20-configuracion-base-explicada.md)
