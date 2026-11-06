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
| `datasource` | `${DB_URL:...localhost...}`, `${DB_USER:root}`, `${DB_PASSWORD}` | Conexión a MySQL. La clave **no tiene valor por defecto**: si falta, la app no arranca |
| `jpa.hibernate.ddl-auto` | `validate` | Hibernate solo comprueba; **nunca crea ni modifica tablas** |
| `jpa.open-in-view` | `false` | Evita consultas perezosas fuera de la capa de servicio (buena práctica). **Corregido el 20 oct** (antes estaba en `true`) |
| `jpa.properties.hibernate.jdbc.time_zone` | `America/Santiago` | Zona horaria con la que Hibernate guarda fechas y horas |
| `jackson.time-zone` | `America/Santiago` | Zona horaria de las fechas en el JSON |
| `data.web.pageable` | `default-page-size: 20`, `max-page-size: 100` | Tamaño de página por defecto y tope |
| `jwt.secret` y `jwt.expiration-ms` | `JWT_SECRET` y `3600000` (1 hora) | Firma y vida del token |
| `cors.allowed-origins` | `${CORS_ORIGINS:http://localhost:5173}` | Orígenes del frontend permitidos |
| `servlet.multipart` | `5MB` | Límite de subida de imágenes |
| `mail` | `localhost:1025` | Para Mailpit en desarrollo (aún sin uso) |
| `data.redis` / `cache` | Redis, TTL 10 min | **Por defecto el caché es Redis**, que hoy no existe; los perfiles lo apagan |
| `jwt.secret` | `${JWT_SECRET}` | **Sin valor por defecto** desde el 3 nov; el perfil `prod` tampoco lo trae |
| `jwt.expiration-ms` | `3600000` (1 hora) | Vida del token |
| `management.endpoints` | Solo `health` | Actuator mínimo |

## En palabras simples: cuatro ajustes de `application.yaml`

| Ajuste | Analogía | Qué pasaría sin él |
| --- | --- | --- |
| `open-in-view: false` | En una **biblioteca**, los libros se prestan solo mientras estás en el mostrador (el service). Con `true`, te dejan llevarte el libro a casa y seguir pidiendo hojas por teléfono (el controller) | Consultas escondidas a la base fuera del service; los errores de carga aparecen tarde |
| Zona horaria | Un **reloj de pared** para toda la oficina: si cada máquina usa su propia hora, la misma compra aparece a las 15:00 en tu PC y a las 18:00 en el servidor | Boletas con la hora corrida |
| Paginación (`max-page-size`) | Un **buscador** muestra 10 resultados por página, no los millones de una vez | Un listado largo congela la API y el navegador |
| `cors.allowed-origins` | El **portero de un edificio**: solo deja pasar a quienes están en la lista de visitas (tu frontend) | El navegador bloquea al frontend aunque la API funcione |

**Una trampa:** si escribes mal un nombre en el YAML (por ejemplo `Jackson:` con mayúscula o `jpa.time_zone`), Spring **no avisa**: lo ignora y los tests siguen pasando. Pasó el 20 oct con la zona horaria ([20, sección 4](20-configuracion-base-explicada.md#4-zona-horaria)). Cuando agregues una propiedad, comprueba su nombre exacto.

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

Por qué en prod los errores no muestran nada: un mensaje de excepción puede revelar nombres de tablas, rutas o versiones. El `GlobalExceptionHandler` (ya escrito, ver [Cambios recientes](../estado/cambios-recientes.md)) decide qué se muestra al cliente. Ojo: la propiedad `server.error.include-message: never` solo afecta a la respuesta de error **por defecto** de Spring. Las respuestas del handler propio llevan el `mensaje` que tú escribas, por eso las excepciones propias (`RecursoNoEncontradoException`, `ReglaNegocioException`) deben tener mensajes pensados para el usuario, y el error 500 devuelve siempre un texto genérico.

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
