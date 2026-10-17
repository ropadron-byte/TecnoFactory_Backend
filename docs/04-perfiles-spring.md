# 04. Perfiles de Spring

## Cómo se elige el perfil

Con la variable `SPRING_PROFILES_ACTIVE` al arrancar:

- `application.yaml` se carga **siempre**.
- `application-local.yaml` se aplica encima solo con el perfil `local`.
- `application-prod.yaml` se aplica encima solo con el perfil `prod`.
- Lo que el perfil escribe **reemplaza** al base; lo que no escribe se mantiene.

Se comprueba en el log de arranque: `The following 1 profile is active: "prod"`.

Si no defines ningún perfil, corre solo `application.yaml` (perfil `default`): usa Redis como caché y credenciales por defecto, así que **no sirve para trabajar**. Arranca siempre con `local` o `prod`.

## `application.yaml` (base)

| Bloque | Valor | Comentario |
| --- | --- | --- |
| `datasource` | `${DB_URL:...localhost...}`, `${DB_USER:root}`, `${DB_PASSWORD:root}` | Valores por defecto de desarrollo |
| `jpa.hibernate.ddl-auto` | `validate` | Hibernate solo comprueba; **nunca crea ni modifica tablas** |
| `jpa.open-in-view` | `false` | Evita consultas perezosas fuera de la capa de servicio (buena práctica) |
| `servlet.multipart` | `5MB` | Límite de subida de imágenes |
| `mail` | `localhost:1025` | Para Mailpit en desarrollo (aún sin uso) |
| `data.redis` / `cache` | Redis, TTL 10 min | **Por defecto el caché es Redis**, que hoy no existe; los perfiles lo apagan |
| `jwt.secret` | Valor por defecto de ejemplo | Solo para que arranque sin configurar; prod lo reemplaza sin valor por defecto |
| `jwt.expiration-ms` | `3600000` (1 hora) | Vida del token |
| `management.endpoints` | Solo `health` | Actuator mínimo |

## Comparación `local` frente a `prod`

| Aspecto | `local` | `prod` |
| --- | --- | --- |
| Usuario de la app | `${DB_USER:root}` | `${DB_USER}` (sin defecto) → `tecnofactory_app` |
| Usuario de Flyway | El mismo de la app (`root`) | `${FLYWAY_USER}` → `tecnofactory_migrator` |
| `DB_URL` | Con valor por defecto | **Sin valor por defecto** |
| `JWT_SECRET` | Obligatoria (sin defecto) | Obligatoria (sin defecto) |
| Hibernate | `validate` | `validate` |
| Flyway | Activo | Activo |
| SQL en logs | Visible y formateado | Oculto |
| Swagger / `api-docs` | Activos | **Apagados** |
| Logs | `root: INFO`, `com.factory: DEBUG` | `root: WARN`, `com.factory: INFO` |
| Actuator | `health, info, metrics` con detalles | Solo `health`, sin detalles |
| Errores de la API | Con mensaje, rastro con `?trace=true` y errores de validación | **Sin mensaje, sin rastro, sin detalle de campos** |
| Caché | `none` | `none` por ahora |
| Health de Redis y correo | Desactivados | Desactivados |

Detalle de niveles, dónde ver el log y cómo guardarlo en un archivo: [12](12-logs.md).

Por qué en prod los errores no muestran nada: un mensaje de excepción puede revelar nombres de tablas, rutas o versiones. El `GlobalExceptionHandler` (ya escrito, ver [Cambios recientes](cambios-recientes.md)) decide qué se muestra al cliente. Ojo: la propiedad `server.error.include-message: never` solo afecta a la respuesta de error **por defecto** de Spring. Las respuestas del handler propio llevan el `mensaje` que tú escribas, por eso las excepciones propias (`RecursoNoEncontradoException`, `ReglaNegocioException`) deben tener mensajes pensados para el usuario, y el error 500 devuelve siempre un texto genérico.

## Qué hace Flyway con cada perfil

Flyway usa **la conexión del datasource** salvo que se indique `spring.flyway.user` y `spring.flyway.password`:

| Perfil | Quién ejecuta las migraciones |
| --- | --- |
| `local` | `root` (la misma conexión de la app) |
| `prod` | `tecnofactory_migrator` (por `FLYWAY_USER` / `FLYWAY_PASSWORD`) |

Flyway abre su propia conexión, la usa al arrancar y la cierra; después la app trabaja solo con `tecnofactory_app`.

## Indentación de `jpa:` en `application-local.yaml` (corregido)

Este error ya está **corregido** en el proyecto. Se deja documentado porque es fácil volver a cometerlo: si `hibernate:` queda al mismo nivel que `jpa:`, o `properties:` cuelga de `hibernate:`, Spring lo ignora sin avisar (`show-sql` y `format_sql` no se aplican y `ddl-auto` cae al valor base). El bloque correcto es:

```yaml
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
```

`show-sql` va bajo `jpa:`, y `properties:` también bajo `jpa:` (nunca dentro de `hibernate:`). Se puede comprobar con un parser YAML: `spring.jpa` debe contener `show-sql`, `hibernate` y `properties` como claves hermanas.

## Cómo comprobar qué quedó activo

| Qué comprobar | Cómo |
| --- | --- |
| Perfil | Línea `The following 1 profile is active` del log |
| Que Flyway corrió | `SELECT * FROM flyway_schema_history;` |
| Que `show-sql` funciona (local) | Aparecen consultas SQL en el log al llamar a un endpoint |
| Que Swagger está apagado (prod) | `/swagger-ui/index.html` responde error |

## Opcional

Cuando el perfil `prod` se use en un servidor real conviene añadir `application-prod.yaml`: `server.shutdown: graceful`, límites de pool de Hikari y `spring.jpa.open-in-view: false` (ya está en el base).
