# 07. Roles y permisos

## Los tres roles

| Rol | Qué puede hacer | Panel |
| --- | --- | --- |
| **ADMINISTRADOR** | Todo | Panel de administración completo |
| **VENDEDOR** | Agregar, editar y eliminar productos y stock; **ver** usuarios (solo lectura); ver ventas; editar **solo su propio perfil** | Panel reducido |
| **CLIENTE** | Comprar y gestionar lo suyo | **Sin** panel ni botón |

## Qué está implementado y qué no

| Función | Estado |
| --- | --- |
| Registro público (siempre como `CLIENTE`) | Hecho |
| Login con JWT | Hecho |
| `GET /api/v1/usuarios/me` (cualquier sesión) | Hecho |
| `GET /api/v1/usuarios/{id}` (solo administrador) | Hecho |
| `POST /api/v1/contactos` (cualquier persona, sin sesión) | Hecho |
| `GET /api/v1/contactos` y `/{id}` (solo administrador) | Hecho |
| `GET /api/v1/usuarios` listado (administrador y vendedor, lectura) | Hecho (20 nov) |
| `PUT /api/v1/usuarios/me` (editar el propio perfil) | Hecho |
| `POST /api/v1/usuarios/vendedores` (crear vendedores, solo administrador) | Hecho |
| `PATCH /api/v1/usuarios/me/desactivar` (solo cliente) | Hecho |
| Productos y categorías: lectura pública, escritura solo de `ADMINISTRADOR` | Hecho |
| Carrito y boletas: cualquier sesión; `GET /api/v1/boletas` y anular (`PATCH /.../anular`) son de personal/administrador | Hecho |
| Gestión de blogs e imágenes (`ADMINISTRADOR` o `VENDEDOR`) | Hecho |

## Cómo se aplican las reglas

En `SecurityConfig`, de arriba hacia abajo, **la primera que coincide gana**:

1. Rutas públicas (Swagger, `/actuator/health`, `/error`, `OPTIONS`, login, registro, `POST /api/v1/contactos`, lectura de blogs, `GET /api/v1/productos`, `/api/v1/productos/ofertas`, `/api/v1/productos/{id}`, `/api/v1/categorias` y `/{id}`, y `/uploads/**`).
2. `GET` y `PUT /api/v1/usuarios/me` → cualquiera con sesión; `PATCH /api/v1/usuarios/me/desactivar` → solo `CLIENTE`.
3. `GET /api/v1/usuarios` (raíz, sin id) → `hasAnyRole("ADMINISTRADOR", "VENDEDOR")`, solo lectura; va antes de la regla siguiente.
4. `/api/v1/usuarios/**` (incluye `/vendedores`) → `hasRole("ADMINISTRADOR")`.
5. Blogs de gestión, imágenes y sus escrituras (`POST`/`PUT`/`DELETE`) → `ADMINISTRADOR` o `VENDEDOR`; escrituras de productos y categorías → solo `ADMINISTRADOR`.
6. Carrito → cualquier sesión. Boletas: comprar, ver las propias y el detalle → cualquier sesión; `GET /api/v1/boletas` → personal; `PATCH /api/v1/boletas/{id}/anular` → `ADMINISTRADOR`.
7. `anyRequest().denyAll()` → **todo lo demás queda cerrado**, incluso con token.

Para sumar al vendedor a una ruta de **lectura**:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/usuarios").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
```

Va **antes** de la línea general `/api/v1/usuarios/**`.

## Reglas más finas (no caben en una ruta)

«El vendedor edita **solo su perfil**» no se puede expresar con una ruta: se resuelve en el service, usando el usuario autenticado y **nunca** un id que mande el cliente. Por eso el endpoint es `/me`, no `/{id}`.

## Lo que el frontend sí y no hace

| El frontend... | Es |
| --- | --- |
| Oculta el botón del panel a un cliente | **Comodidad**, no seguridad |
| Rechaza una acción en el servidor con 401/403 | **La seguridad real** |

Cualquiera puede llamar a la API sin pasar por tu pantalla; por eso cada ruta se protege aquí.

## Reglas

1. El rol sale de la **base de datos** en cada petición, no del token.
2. Desactivar a un usuario (`activo = false`) le quita el acceso al instante.
3. El registro público **nunca** acepta un rol del cliente (el `Request` no tiene ese campo).
4. Al agregar una ruta nueva, decide su rol **en ese momento**; si lo olvidas, queda **denegada** (403) incluso con token.

## Para saber más

[Guía 11](../guias/11-seguridad-y-login.md) · [Referencia 08](../referencia/08-seguridad.md) · [Referencia 21](../referencia/21-seguridad-jwt-y-roles-explicada.md)
