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
| `GET /api/usuarios/me` (cualquier sesión) | Hecho |
| `GET /api/usuarios/{id}` (solo administrador) | Hecho |
| `GET /api/usuarios` listado (administrador y vendedor, lectura) | **Pendiente** |
| `PUT /api/usuarios/me` (editar el propio perfil) | **Pendiente** |
| Crear vendedores (solo administrador) | **Pendiente** |
| Productos, stock y ventas con reglas por rol | **Pendiente** (con cada entidad) |

## Cómo se aplican las reglas

En `SecurityConfig`, de arriba hacia abajo, **la primera que coincide gana**:

1. Rutas públicas (Swagger, `/actuator/health`, login, registro).
2. `GET /api/usuarios/me` → cualquiera con sesión.
3. `/api/usuarios/**` → `hasRole("ADMINISTRADOR")`.
4. `anyRequest().authenticated()` → todo lo demás pide sesión.

Para sumar al vendedor a una ruta de **lectura**:

```java
.requestMatchers(HttpMethod.GET, "/api/usuarios").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
```

Va **antes** de la línea general `/api/usuarios/**`.

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
4. Al agregar una ruta nueva, decide su rol **en ese momento**; si lo olvidas, queda como «cualquier sesión».

## Para saber más

[Guía 11](../guias/11-seguridad-y-login.md) · [Referencia 08](../referencia/08-seguridad.md) · [Referencia 21](../referencia/21-seguridad-jwt-y-roles-explicada.md)
