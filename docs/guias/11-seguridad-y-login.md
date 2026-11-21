# Guía 11. Seguridad y login

> **En una frase:** el usuario inicia sesión con correo y clave, recibe un **token** (un JWT), y lo envía en cada petición; la API lee el token, sabe quién es y su **rol**, y decide qué puede hacer.

## La idea: un hotel

| En el hotel | En TecnoFactory |
| --- | --- | 
| Muestras tu documento en recepción y te dan una **tarjeta** | `POST /api/v1/auth/login` y recibes un **token** |
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
| `AuthService` + `AuthController` | `POST /api/v1/auth/login` |
| `ManejadorErroresSeguridad` | Entrega el 401 y el 403 con el JSON de siempre |
| `SecurityConfig` | Las reglas por ruta |

## Las reglas de hoy

| Ruta | Quién entra |
| --- | --- |
| Swagger, `/actuator/health`, `POST /api/v1/auth/login`, `POST /api/v1/auth/google`, `POST /api/v1/auth/google/completar`, `POST /api/v1/usuarios` (registro), `POST /api/v1/contactos` (formulario de contacto) | Todos |
| `GET /api/v1/contactos` y `GET /api/v1/contactos/{id}` | Solo `ADMINISTRADOR` |
| `GET /api/v1/usuarios/me` | Cualquiera con sesión |
| `GET /api/v1/usuarios` (raíz) | `ADMINISTRADOR` o `VENDEDOR` (solo lectura) |
| `POST /api/v1/auth/google/vincular` | Cualquiera con sesión (la abre con contraseña) |
| Resto de `/api/v1/usuarios/**` | Solo `ADMINISTRADOR` |
| Todo lo demás | **Nadie**: `anyRequest().denyAll()` cierra lo que no tenga regla explícita |

Spring evalúa **de arriba hacia abajo y se detiene en la primera que coincide**: lo específico (`/me`) va antes que lo general (`/**`), y `anyRequest()` va al final.

## Login con Google

El frontend usa **Google Identity Services** y obtiene un **ID token** (un JWT firmado por
Google). No hay redirección OAuth2 ni Client Secret: el navegador envía ese token a la API.

```
React -> Google Identity Services -> ID token
      -> POST /api/v1/auth/google {credential}
         -> GoogleTokenVerifier: firma (JWKS), iss, aud = GOOGLE_CLIENT_ID, exp, email_verified
         -> ¿google_id ya vinculado?  -> 200 + JWT propio
         -> ¿correo existe con clave? -> 409: iniciar sesión y vincular
         -> ¿correo existe solo con Google? -> se vincula y 200 + JWT
         -> cuenta nueva -> 202 + registroToken (10 min, no sirve como Bearer)
              -> POST /api/v1/auth/google/completar (RUN, DV, fecha, dirección, teléfono, comuna)
              -> 201 + JWT
```

**Cuenta existente con contraseña:** no se vincula sola, porque cualquiera podría crear una
cuenta de Google con el correo de otra persona y entrar. El flujo correcto es:

1. `POST /api/v1/auth/login` con correo y contraseña.
2. `POST /api/v1/auth/google/vincular` con el `credential` de Google y el `Bearer` del paso 1.
3. El correo de Google debe coincidir con el de la cuenta; si no, `409`.

**Cuentas solo-Google:** se guardan con `contrasenia = NULL` y `google_id` único. El login
con contraseña devuelve `401`, igual que una clave incorrecta.

**Reglas que no cambian:** `GOOGLE_CLIENT_ID` es público (puede ir en el frontend) pero es
**obligatorio** para que la app arranque; el JWT propio sigue firmándose con `JWT_SECRET`.
El `registroToken` lleva el claim `proposito=registro_google`, y `JwtService.esValido()` lo
rechaza como token de acceso.

La guía de React, con el botón de Google y el formulario de completar perfil, está en la
[guía 17](17-integracion-con-react.md#login-con-google). El detalle línea por línea, en la
[referencia 29](../referencia/29-login-con-google-explicado.md).

## Qué es un JWT

Tres partes separadas por puntos: cabecera, datos (`sub` = correo, `rol`, `iat`, `exp`) y **firma**. La firma se calcula con `JWT_SECRET`; si alguien cambia una letra de los datos, no coincide y el token se rechaza. **No cifra**: cualquiera puede leer los datos, por eso no llevan contraseñas.

## Cómo probarlo en Swagger

1. `POST /api/v1/auth/login` con `ADMIN_EMAIL` y `ADMIN_PASSWORD` → copia el `token`.
2. Botón **Authorize**: pega **solo** el token (Swagger agrega `Bearer`).
3. `GET /api/v1/usuarios/me` → 200.
4. Sin token → 401. Con el token de un cliente en `/api/v1/usuarios/1` → 403.

## Reglas que no hay que olvidar

1. **Lo no permitido explícitamente queda cerrado** (`anyRequest().denyAll()`).
2. **Ocultar un botón en el frontend no protege nada**: la regla real está aquí.
3. **Correo inexistente, clave incorrecta y usuario desactivado dan el mismo 401**: nadie averigua qué correos existen.
4. **El rol que decide es el de la base**, no el del token; cambiarlo o desactivar al usuario surte efecto de inmediato.
5. **El token no se puede revocar** antes de expirar; vida corta (1 hora).
6. **`JWT_SECRET` de 32 o más bytes**, secreto y distinto en producción.
7. **`JwtAuthFilter` no lleva `@Component`**: `SecurityConfig` lo crea (si no, `@WebMvcTest` lo cargaría y fallaría).

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| 401 con un token recién emitido | `Bearer` escrito dos veces, o el token venció | Pega solo el token; inicia sesión otra vez |
| 403 a un cliente en `/api/v1/usuarios/7` | Es lo esperado | Usa `/api/v1/usuarios/me` |
| `/me` da 403 | La regla está después de `/api/v1/usuarios/**` | Reordena |
| La app no arranca: `WeakKeyException` | `JWT_SECRET` corto | Alárgalo |
| La app no arranca: `GOOGLE_CLIENT_ID es obligatoria` | Falta la variable o quedó el placeholder | Copia el Client ID de Google Cloud en `.env` |
| Google devuelve 401 | El `credential` no es para este Client ID, expiró o el correo no está verificado | Revisa el Client ID y el origen autorizado en Google Cloud |

## Para saber más

[Referencia 29: login con Google](../referencia/29-login-con-google-explicado.md) · [Referencia 21: JWT y roles, línea por línea](../referencia/21-seguridad-jwt-y-roles-explicada.md) · [Referencia 08: modelo de seguridad](../referencia/08-seguridad.md) · [Referencia 15: BCrypt y la primera `SecurityConfig`](../referencia/15-seguridad-codigo-explicado.md) · [Personal 07: roles y permisos](../personal/07-roles-y-permisos.md)
