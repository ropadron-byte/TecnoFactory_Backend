# 05. Probar la API

Con la app arrancada, hay dos formas: **Swagger** (con botones) y **curl** (terminal).

## En Swagger

Abre `http://localhost:8080/swagger-ui/index.html`.

### Recorrido básico

| # | Paso | Esperado |
| --- | --- | --- |
| 1 | `POST /api/auth/login` con tu `ADMIN_EMAIL` y `ADMIN_PASSWORD` | 200 y un `token` |
| 2 | Botón **Authorize**: pega **solo** el token | Candado cerrado |
| 3 | `GET /api/usuarios/me` | 200 con tus datos |
| 4 | `GET /api/usuarios/1` | 200 (eres administrador) |
| 5 | `GET /api/usuarios/9999` | 404 |
| 6 | `GET /api/usuarios/abc` | 400 |
| 7 | Cierra la sesión (Authorize → Logout) y repite el 3 | 401 |

### Listados con paginación

En `GET /api/imagenes` y `GET /api/blogs`, *Try it out* muestra tres campos: `page` (desde 0), `size` y `sort` (por ejemplo `id,desc`). **Borra el `"string"` que Swagger pone de ejemplo en `sort`**: no es un campo real y devuelve 400.

### Formulario de contacto

| # | Paso | Esperado |
| --- | --- | --- |
| 1 | Sin Authorize: `POST /api/contactos` con `{"nombre":"Ana","correo":"ana@correo.cl","comentario":"Hola"}` | **201** con `id` y `Location` |
| 2 | Mismo `POST` con `"correo":"no-es-correo"` | 400 con `detalles.correo` |
| 3 | `GET /api/contactos` sin Authorize | 401 |
| 4 | `GET /api/contactos` con el token de un cliente | 403 |
| 5 | `GET /api/contactos` con el token del administrador | 200 con la fila del paso 1 |

### Probar un rol sin permiso

1. Registra un cliente: `POST /api/usuarios` (no pide token; clave de 8 a 72 caracteres, RUN con dígito verificador válido).
2. `POST /api/auth/login` con ese correo y úsalo en Authorize.
3. `GET /api/usuarios/1` → **403**.
4. `GET /api/usuarios/me` → **200**.

## Con curl

```bash
# Iniciar sesión y guardar el token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"correo\":\"$ADMIN_EMAIL\",\"clave\":\"$ADMIN_PASSWORD\"}" \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Usar el token
curl -i http://localhost:8080/api/usuarios/me -H "Authorization: Bearer $TOKEN"

# Sin token (espera 401)
curl -i http://localhost:8080/api/usuarios/me
```

> Los nombres exactos de los campos del login están en Swagger (esquema `LoginRequest`); si un campo no coincide, verás un 400 con el detalle.

## Qué comprobar en cada prueba

- El **código** HTTP (no solo que «responde»).
- Que el JSON de error tenga `mensaje` y `ruta`.
- Que **nunca** aparezcan `contrasenia` ni el hash en una respuesta.

## Reglas

1. Un 401 y un 403 no son lo mismo: el primero es «no sé quién eres»; el segundo, «no tienes permiso».
2. Si Swagger dice 401 con un token recién creado, revisa que no pegaste `Bearer` dos veces.
3. En producción Swagger está apagado.

## Para saber más

[Guía 19: contacto](../guias/19-formulario-de-contacto.md) · [Guía 11](../guias/11-seguridad-y-login.md) · [Referencia 21, sección 14](../referencia/21-seguridad-jwt-y-roles-explicada.md) · [Guía 10: errores](../guias/10-errores-de-la-api.md)
