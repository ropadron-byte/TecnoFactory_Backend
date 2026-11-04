# Guía 19. Formulario de contacto

> **En una frase:** cualquier persona, **sin iniciar sesión**, puede enviar su nombre, correo y un comentario con `POST /api/contactos`; solo el `ADMINISTRADOR` puede leer lo que llegó.

## La idea: el buzón de la tienda

Una tienda tiene un **buzón en la puerta**: cualquiera que pasa puede meter una carta, no hace falta ser cliente ni mostrar credencial. Pero el buzón **solo lo abre el dueño**: quien dejó la carta no puede abrir el buzón para leer las de los demás.

| En la tienda | En TecnoFactory |
| --- | --- |
| La **ranura** del buzón, abierta a todos | `POST /api/contactos`, público |
| La **carta** | Una fila de la tabla `contacto` (nombre, correo, comentario) |
| El **dueño** que abre el buzón | `ADMINISTRADOR`: `GET /api/contactos` y `GET /api/contactos/{id}` |
| El letrero «máximo una hoja» | Los límites de largo (100, 250 y 500 caracteres) |

## El recorrido

```
ENVIAR: React (formulario) -> POST /api/contactos  (sin token)
        -> SecurityConfig (POST /api/contactos es público)
        -> ContactoController (@Valid) -> ContactoService (quita espacios, guarda)
        -> 201 { id, nombre, correo, comentario } + Location: /api/contactos/{id}

LEER:   Panel de administración -> GET /api/contactos  (con token)
        -> JwtAuthFilter -> SecurityConfig (¿ADMINISTRADOR?)
        -> ContactoController -> ContactoService -> PageResponse
```

Sin token en la lectura → **401**; con token de `VENDEDOR` o `CLIENTE` → **403**.

## Las piezas

| Pieza | Dónde | Qué hace |
| --- | --- | --- |
| `V6__contacto.sql` | `db/migration/` | Crea la tabla `contacto` |
| `Contacto` | `entity/` | La fila de la tabla como objeto Java |
| `ContactoRepository` | `repository/` | Guarda y busca (`JpaRepository`) |
| `ContactoRequest` | `dto/` | Lo que **entra**: nombre, correo y comentario, con sus validaciones |
| `ContactoResponse` | `dto/` | Lo que **sale**: `id`, nombre, correo y comentario |
| `ContactoMapper` | `mapper/` | Convierte entre `Contacto` y los dos DTO (MapStruct) |
| `ContactoService` | `service/` | Quita los espacios sobrantes, guarda y lista |
| `ContactoController` | `controller/` | Las tres rutas y los códigos HTTP |
| `SecurityConfig` | `security/` | Quién puede entrar a cada ruta |

## Las reglas de hoy

| Ruta | Quién entra | Éxito |
| --- | --- | --- |
| `POST /api/contactos` | **Todos**, sin sesión | 201 |
| `GET /api/contactos` | Solo `ADMINISTRADOR` | 200 (paginado) |
| `GET /api/contactos/{id}` | Solo `ADMINISTRADOR` | 200 |

## Validaciones del formulario

| Campo | Regla |
| --- | --- |
| `nombre` | Obligatorio, máx. **100** |
| `correo` | Obligatorio, formato de correo, máx. **250** |
| `comentario` | Obligatorio, máx. **500** |

Los máximos son los de las columnas de la tabla: lo que pasa la validación **siempre cabe** en la base.

## Probarlo

```bash
# Enviar un contacto (sin token)
curl -X POST http://localhost:8080/api/contactos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ana","correo":"ana@correo.cl","comentario":"Hola, quiero más información"}'

# Leerlos (con el token del administrador)
curl http://localhost:8080/api/contactos -H "Authorization: Bearer $TOKEN"
```

En Swagger: grupo **Contacto**. El `POST` no muestra candado; los `GET` sí (usa **Authorize** con el token del administrador).

## Qué no hace (todavía)

| Falta | Por qué importa |
| --- | --- |
| **Límite de envíos por IP o captcha** | La ruta es pública: cualquiera (o un programa) puede llenar la tabla |
| **Fecha de envío** | La tabla no tiene `fecha_creacion`; no se sabe cuándo llegó cada mensaje |
| **Marcar como leído / borrar** | Hoy solo se crea y se lee |
| **Aviso por correo al administrador** | Hoy hay que entrar a mirar el listado |

## Errores que verás

| Síntoma | Causa | Arreglo |
| --- | --- | --- |
| 400 «Datos inválidos» con `detalles` | Falta un campo, el correo no tiene formato o se pasó del máximo | Revisa el campo que indica `detalles` |
| 401 al **enviar** | Falta la regla `POST /api/contactos` con `permitAll()` en `SecurityConfig`, o quedó después de `anyRequest()` | Ponla antes de `anyRequest()` |
| 403 al **leer** | La sesión no es de administrador | Entra con el administrador |
| `Schema-validation: missing table [contacto]` al arrancar | No se aplicó `V6` | Revisa que `V6__contacto.sql` esté en `db/migration/` y mira `flyway_schema_history` |
| `Schema-validation: wrong column type ... id_contacto` | La tabla no es la de `V6` (por ejemplo, quedó `NUMERIC`) | La entidad refleja a la base: corrige la migración (ver [guía 05](05-base-de-datos-y-flyway.md)) |

## Para saber más

[Referencia 28: contacto, línea por línea](../referencia/28-contacto-explicado.md) · [Catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md) · [Guía 11: seguridad y login](11-seguridad-y-login.md) · [Guía 09: service y controller](09-service-y-controller.md)
