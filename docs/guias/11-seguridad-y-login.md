# Guía 11. Seguridad y login

> **En una frase:** el usuario inicia sesión con correo y clave, recibe un **token** (un JWT), y lo envía en cada petición; la API lee el token, sabe quién es y su **rol**, y decide qué puede hacer.

## La idea: un hotel

| En el hotel | En TecnoFactory |
| --- | --- | 
| Muestras tu documento en recepción y te dan una **tarjeta** | `POST /api/auth/login` y recibes un **token** |
| La tarjeta lleva tu nombre, tu nivel y un **vencimiento** | El token lleva tu correo, tu rol y expira en 1 hora |
| Cada puerta tiene un **lector** | `JwtAuthFilter` lee el token en cada petición |
| El **reglamento** dice qué tarjetas abren qué puertas | `SecurityConfig` |
| Tarjeta falsa o vencida | **401** |
| Tarjeta válida, pero de huésped en la bodega | **403** |
| Si desactivan tu habitación, la tarjeta deja de servir | El filtro consulta la base: un usuario desactivado pierde el acceso al instante |

**Autenticación** = quién eres. **Autorización** = qué puedes hacer.

## El recorrido

```
LOGIN:   correo+clave -> AuthService -> AuthenticationManager -> UserDetailsServiceImpl (base) + BCrypt
                                      -> JwtService.generarToken -> {token, tipo:"Bearer", correo, rol}
PETICIÓN: Authorization: Bearer <token> -> JwtAuthFilter (¿válido? ¿usuario activo?) -> reglas de SecurityConfig
          sin sesión -> 401        sin el rol -> 403        todo bien -> controller
```

## Las piezas

| Pieza | Qué hace |
| --- | --- |
| `JwtService` | Crea y comprueba tokens firmados con `JWT_SECRET` |
| `UserDetailsServiceImpl` | Busca al usuario por correo y le pone el rol (`ROLE_CLIENTE`...) |
| `JwtAuthFilter` | Lee el token y deja autenticada a la persona. **Nunca rechaza**: solo identifica |
| `AuthService` + `AuthController` | `POST /api/auth/login` |
| `ManejadorErroresSeguridad` | Entrega el 401 y el 403 con el JSON de siempre |
| `SecurityConfig` | Las reglas por ruta |

## Las reglas de hoy

| Ruta | Quién entra |
| --- | --- |
| Swagger, `/actuator/health`, `POST /api/auth/login`, `POST /api/usuarios` (registro), `POST /api/contactos` (formulario de contacto) | Todos |
| `GET /api/contactos` y `GET /api/contactos/{id}` | Solo `ADMINISTRADOR` |
| `GET /api/usuarios/me` | Cualquiera con sesión |
| Resto de `/api/usuarios/**` | Solo `ADMINISTRADOR` |
| Todo lo demás | Cualquiera con token válido |

Spring evalúa **de arriba hacia abajo y se detiene en la primera que coincide**: lo específico (`/me`) va antes que lo general (`/**`), y `anyRequest()` va al final.

## Qué es un JWT

Tres partes separadas por puntos: cabecera, datos (`sub` = correo, `rol`, `iat`, `exp`) y **firma**. La firma se calcula con `JWT_SECRET`; si alguien cambia una letra de los datos, no coincide y el token se rechaza. **No cifra**: cualquiera puede leer los datos, por eso no llevan contraseñas.

## Cómo probarlo en Swagger

1. `POST /api/auth/login` con `ADMIN_EMAIL` y `ADMIN_PASSWORD` → copia el `token`.
2. Botón **Authorize**: pega **solo** el token (Swagger agrega `Bearer`).
3. `GET /api/usuarios/me` → 200.
4. Sin token → 401. Con el token de un cliente en `/api/usuarios/1` → 403.

## Reglas que no hay que olvidar

1. **Lo no permitido explícitamente queda protegido** (`anyRequest().authenticated()`).
2. **Ocultar un botón en el frontend no protege nada**: la regla real está aquí.
3. **Correo inexistente, clave incorrecta y usuario desactivado dan el mismo 401**: nadie averigua qué correos existen.
4. **El rol que decide es el de la base**, no el del token; cambiarlo o desactivar al usuario surte efecto de inmediato.
5. **El token no se puede revocar** antes de expirar; vida corta (1 hora).
6. **`JWT_SECRET` de 32 o más caracteres**, secreto y distinto en producción.
7. **`JwtAuthFilter` no lleva `@Component`**: `SecurityConfig` lo crea (si no, `@WebMvcTest` lo cargaría y fallaría).

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| 401 con un token recién emitido | `Bearer` escrito dos veces, o el token venció | Pega solo el token; inicia sesión otra vez |
| 403 a un cliente en `/api/usuarios/7` | Es lo esperado | Usa `/api/usuarios/me` |
| `/me` da 403 | La regla está después de `/api/usuarios/**` | Reordena |
| La app no arranca: `WeakKeyException` | `JWT_SECRET` corto | Alárgalo |

## Para saber más

[Referencia 21: JWT y roles, línea por línea](../referencia/21-seguridad-jwt-y-roles-explicada.md) · [Referencia 08: modelo de seguridad](../referencia/08-seguridad.md) · [Referencia 15: BCrypt y la primera `SecurityConfig`](../referencia/15-seguridad-codigo-explicado.md) · [Personal 07: roles y permisos](../personal/07-roles-y-permisos.md)
