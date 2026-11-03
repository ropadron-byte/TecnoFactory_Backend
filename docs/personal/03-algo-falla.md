# 03. Algo falla

## Método de 4 pasos

1. **Lee el final**: ¿`BUILD FAILURE`? El motivo está más arriba.
2. **Busca el primer `ERROR`/`WARN`** con una `Exception`.
3. **Baja a `Caused by:`** y lee el que nombra tu código.
4. **Ignora** las líneas `at org.springframework...`.

Filtro útil: `./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by"`.

Antes de buscar la solución, **escribe tu propia interpretación** de lo que dice. Así se aprende.

## Por síntoma

| Veo... | Probable causa | Solución |
| --- | --- | --- |
| La app no arranca: `Could not resolve placeholder 'X'` | Falta una variable | Defínela (el `.env` no se lee solo) |
| `Communications link failure` | MySQL apagado | `docker compose up -d` |
| `Access denied for user` | Usuario o clave de MySQL | Revisa `DB_USER` y `DB_PASSWORD` |
| `Schema-validation: missing ...` | La entidad no calza con `V1` | Compara nombre de tabla/columna y tipo |
| `No property 'x' found for type` | Método de repository con nombre de columna | Usa el atributo Java |
| `Migration checksum mismatch` | Editaste una migración ya aplicada | Nueva `V` o `down -v` (solo desarrollo) |
| `WeakKeyException` | `JWT_SECRET` corto | 32+ caracteres |
| `Port 8080 was already in use` | Otra copia en ejecución | Deténla |
| Un test: `No qualifying bean ...` | Falta `@MockitoBean` | Agrégalo |
| Un test: `UnnecessaryStubbingException` | `when(...)` sin uso | Quítalo |
| API: **400** | Datos inválidos | Mira `detalles` en el JSON |
| API: **401** | Sin token, vencido o clave mala | Inicia sesión de nuevo |
| API: **403** | Tienes sesión, pero no el rol | Revisa [07](07-roles-y-permisos.md) |
| API: **404** | Recurso o ruta inexistente | Revisa el id y la URL |
| API: **409** | Duplicado o regla de negocio | Lee el `mensaje` |
| API: **500** | Falló algo del servidor | Lee el log (`ERROR` y `Caused by`) |
| Un error de cliente sale como 500 | Falta un handler | Agrégalo en `GlobalExceptionHandler` |
| `?sort=algo` o un archivo sin `archivo` / de más de 5 MB | Antes salían como 500; ahora 400, 400 y 413 | Si vuelve a salir 500, revisa que el handler siga en `GlobalExceptionHandler` |
| Swagger: el cuadro `pageable` con `{ "page": 0, ... }` falla o no ordena | Falta `@ParameterObject` en el `Pageable` del controller | Agrégala (ver [23](../referencia/23-catalogo-de-endpoints.md)) |
| El test `contextLoads` falla con `Could not resolve placeholder 'jwt.secret'` | `jwt.secret` ya no tiene valor por defecto | El test define el suyo; en la app usa `./scripts/dev.sh` |
| El frontend da error de CORS | Origen no permitido | `CORS_ORIGINS` |
| Cambié el YAML y no hace nada | Propiedad mal escrita (Spring la ignora) | Revisa el nombre exacto |

## Si sigue sin salir

1. ¿Qué cambié justo antes? Deshazlo y comprueba.
2. ¿Falla la app o solo un test? Aísla.
3. Pega **solo las líneas relevantes** (no el log completo) al pedir ayuda, y di qué ya intentaste.

## Para saber más

[Guía 14](../guias/14-leer-errores-y-problemas.md) · [Referencia 16](../referencia/16-leer-errores-de-spring.md) · [Referencia 09](../referencia/09-errores-comunes.md)
