# 29. Login con Google explicado

> **En una frase:** React obtiene un **ID token** de Google Identity Services, la API lo verifica y lo cambia por el **mismo JWT propio** que usa el login con contraseña.

## 1. Por qué este flujo y no la redirección OAuth2

Spring Boot trae `spring-boot-starter-oauth2-client`, que normalmente redirige el navegador a Google y crea una sesión con cookies. Aquí **no se usa así**: la API es *stateless* y el frontend ya maneja el JWT.

| Alternativa | Por qué se descartó |
| --- | --- |
| Redirección OAuth2 con sesión | Rompe el modelo `STATELESS`, mezcla cookies con `Bearer` y obliga a CORS con credenciales |
| Pedir correo y clave a Google | El usuario tendría que entregar su clave a TecnoFactory |
| **ID token (elegida)** | El navegador obtiene el token, la API verifica la firma y emite su propio JWT |

El starter se usa **solo** por sus clases de verificación de JWT (`NimbusJwtDecoder`, `JwtValidators`), no por el login OAuth2.

## 2. Piezas

| Archivo | Qué hace |
| --- | --- |
| `security/GoogleTokenVerifier` | Verifica el ID token con las claves públicas (JWKS) de Google |
| `security/JwtService` | Emite el JWT propio y el **token temporal de registro** |
| `service/GoogleAuthService` | Decide entre entrar, pedir perfil o rechazar |
| `controller/AuthController` | Expone `/api/v1/auth/google`, `/completar` y `/vincular` |
| `dto/GoogleLoginRequest` | `{ "credential": "..." }` |
| `dto/GooglePerfilPendienteResponse` | Respuesta 202 con `registroToken` y datos de Google |
| `dto/GoogleCompletarRequest` | RUN, DV, fecha, dirección, teléfono y comuna |
| `db/migration/V8__google_auth.sql` | `google_id` único y `contrasenia` nullable |
| `config/ConfiguracionValidator` | Exige `GOOGLE_CLIENT_ID` al arrancar |

## 3. El recorrido completo

```
React                        API
  │  Google Identity Services
  │  ── ID token (JWT de Google) ──▶ POST /api/v1/auth/google { credential }
  │                                   GoogleTokenVerifier
  │                                     ├ firma con JWKS de Google
  │                                     ├ iss = accounts.google.com
  │                                     ├ aud contiene GOOGLE_CLIENT_ID
  │                                     ├ exp vigente
  │                                     └ email_verified = true
  │                                   GoogleAuthService
  │                                     ├ google_id ya existe ──▶ 200 + JWT propio
  │                                     ├ correo con contraseña ─▶ 409
  │                                     ├ correo solo-Google ────▶ vincula + 200
  │                                     └ correo nuevo ──────────▶ 202 + registroToken
  │
  │  ◀── 202 { registroToken, correo, nombre, apellidos }
  │  POST /api/v1/auth/google/completar { registroToken, run, runDv, ... }
  │  ◀── 201 + JWT propio (cuenta CLIENTE, contrasenia = NULL)
```

## 4. `GoogleTokenVerifier`

```java
NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
        "https://www.googleapis.com/oauth2/v3/certs").build();

OAuth2TokenValidator<Jwt> emisor = new JwtClaimValidator<>(JwtClaimNames.ISS,
        iss -> "https://accounts.google.com".equals(iss)
                || "accounts.google.com".equals(iss));

OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<>(JwtClaimNames.AUD,
        aud -> aud != null && aud.contains(clientId));

decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefault(), emisor, audiencia));
```

- **JWKS**: Google publica sus claves en `oauth2/v3/certs`; `NimbusJwtDecoder` las descarga y las cachea. Si la firma no coincide, lanza `JwtException`.
- **`createDefault()`** valida `exp` y `nbf`.
- **`iss`**: Google usa las dos formas (`https://accounts.google.com` y `accounts.google.com`); se aceptan ambas.
- **`aud`**: debe contener el Client ID de **esta** aplicación. Sin esto, un token emitido para otra app de Google serviría.
- **`email_verified`**: si es `false` o falta, se rechaza. Se acepta `true` booleano o la cadena `"true"`.
- **`sub`** (identificador de la cuenta) y **`email`** son obligatorios.

Cualquier fallo se traduce a `TokenGoogleInvalidoException`, que `GlobalExceptionHandler` convierte en **401** con el mensaje «El token de Google no es válido o expiró».

## 5. El token temporal de registro

`JwtService.generarTokenRegistroGoogle(...)` crea un JWT firmado con el mismo `JWT_SECRET`, con:

| Claim | Valor |
| --- | --- |
| `sub` | `google_id` |
| `correo`, `nombre`, `apellidos` | Datos que devolvió Google |
| `proposito` | `registro_google` |
| `exp` | 10 minutos |

`JwtService.esValido()` —el que usa `JwtAuthFilter`— devuelve `false` si el claim `proposito` existe. Así **el `registroToken` no puede usarse como `Bearer`** aunque sea un JWT válido. `leerRegistroGoogle()` exige ese propósito y lanza `JwtException` si el token es de acceso.

## 6. `GoogleAuthService`

### `iniciarSesion`

1. Verifica el ID token.
2. Busca por `google_id`: si existe y está activo, devuelve JWT (**200**).
3. Busca por correo:
   - con contraseña → **409** («inicia sesión y vincula Google»);
   - con `google_id` de otra cuenta → **409**;
   - sin contraseña ni `google_id` (cuenta solo-Google que perdió el vínculo) → lo vuelve a vincular y devuelve JWT.
4. Si no existe, emite el `registroToken` y devuelve **202**. **No crea la cuenta todavía.**

### `completarPerfil`

1. Lee el `registroToken`; si es inválido o expiró → **401**.
2. Comprueba que el correo, el `google_id` y el RUN no existan → **409**.
3. Busca la comuna (**404** si no existe) y el rol `CLIENTE`.
4. Crea el usuario con `contrasenia = null`, el `google_id` del token y los datos del formulario.
5. Devuelve **201** con el JWT propio.

El correo **nunca** se recibe en el cuerpo: sale del token firmado por Google, así que el cliente no puede cambiarlo.

### `vincular`

1. Exige sesión (`SecurityConfig` pide `authenticated()`).
2. Verifica el ID token de Google.
3. Compara el correo de Google con el de la cuenta autenticada; si no coincide → **409**.
4. Comprueba que esa cuenta de Google no pertenezca a otro usuario → **409**.
5. Guarda `google_id` si aún no estaba. Responde **204**.

**Por qué no se vincula automáticamente al hacer login con Google:** cualquiera puede crear una cuenta de Google con el correo de otra persona. Si se vinculara sola, esa persona entraría a la cuenta ajena. Exigir la contraseña primero demuestra que es el dueño.

## 7. Respuestas

| Endpoint | Código | Significado |
| --- | --- | --- |
| `POST /api/v1/auth/google` | 200 | Cuenta vinculada: devuelve JWT |
| | 202 | Falta completar perfil: devuelve `registroToken` |
| | 401 | ID token inválido, expirado, de otro Client ID, correo no verificado o cuenta desactivada |
| | 409 | Correo con contraseña o cuenta de Google ya vinculada |
| `POST /api/v1/auth/google/completar` | 201 | Cuenta creada: devuelve JWT |
| | 401 | `registroToken` inválido o expirado |
| | 404 | Comuna inexistente |
| | 409 | Correo, RUN o cuenta de Google ya registrados |
| `POST /api/v1/auth/google/vincular` | 204 | Vinculado |
| | 401 | Sin sesión o ID token inválido |
| | 409 | Correo distinto o cuenta de Google ya vinculada |

## 8. Base de datos (`V8`)

```sql
ALTER TABLE usuario
    ADD COLUMN google_id VARCHAR(255) NULL AFTER contrasenia,
    MODIFY COLUMN contrasenia VARCHAR(255) NULL,
    ADD CONSTRAINT usuario_google_id_unique UNIQUE (google_id);
```

- `google_id` es **nullable y único**: las cuentas con contraseña no lo tienen; MySQL permite muchos `NULL` en un `UNIQUE`.
- `contrasenia` pasa a `NULL` para las cuentas creadas solo con Google.
- `UserDetailsServiceImpl` convierte `NULL` en `""` para que el login con contraseña falle con **401** en lugar de lanzar una excepción.

## 9. Configuración de Google Cloud

1. Crear un **OAuth 2.0 Client ID** de tipo *Web application*.
2. Añadir como **Authorized JavaScript origins** el origen del frontend:
   - `http://localhost:5173` en desarrollo;
   - el dominio real en producción.
3. **No hace falta Client Secret** ni usuarios de prueba para los scopes básicos (`openid`, `email`, `profile`).
4. Copiar el Client ID a `.env`:

```env
GOOGLE_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

`ConfiguracionValidator` rechaza el placeholder y detiene el arranque si falta. El Client ID es **público**: puede ir en el frontend (`VITE_GOOGLE_CLIENT_ID`).

## 10. Cómo probarlo

### Tests automáticos

```bash
./mvnw test -Dtest="*Test"
```

Cubren, entre otros:

- `JwtServiceTest`: el `registroToken` no sirve como token de acceso y un token de acceso no sirve como registro.
- `GoogleTokenVerifierTest` (5): acepta un token válido y `email_verified` como texto, y rechaza correo ausente, correo no verificado o un token que el decoder no puede validar.
- `GoogleAuthServiceTest` (11): cuenta vinculada, cuenta nueva, cuenta con contraseña, re-vinculación, cuenta desactivada, completar perfil, RUN duplicado, token inválido y reglas de vinculación.
- `AuthControllerTest` (11): 200, 202, 401, 409, 201 y 204 de los endpoints nuevos.
- `UserDetailsServiceImplTest`: cuenta solo-Google con contraseña `""`.

### A mano

```bash
# 1. Token de Google: cópialo desde el navegador (consola) o desde el botón de prueba
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"credential":"<ID_TOKEN>"}'
```

- Cuenta nueva → **202** con `registroToken`.
- Completar perfil con ese token → **201** con JWT.
- Repetir el login con Google → **200**.
- Probar con una cuenta que ya tenga contraseña → **409**; hacer login normal, tomar el `Bearer` y llamar a `/vincular` → **204**.

## 11. Límites y decisiones

1. **El ID token se verifica en el backend**, no en el frontend. Un token manipulado no crea cuentas.
2. **El `registroToken` dura 10 minutos** y solo sirve para completar el perfil.
3. **No se guarda el Client Secret** porque este flujo no lo necesita.
4. **No se guarda la foto de Google**: el `GoogleUser` la expone por si más adelante se usa, pero la tabla `usuario` no tiene columna para ella.
5. **Una cuenta solo-Google no puede iniciar sesión con contraseña** hasta que se le asigne una (función que aún no existe).
6. **El correo de Google debe estar verificado** (`email_verified = true`); si no, no se acepta.
7. **La sesión sigue siendo un JWT propio de 1 hora**: revocar Google no revoca el token ya emitido; desactivar al usuario sí lo bloquea en la siguiente petición.

## Para saber más

[Guía 11: seguridad y login](../guias/11-seguridad-y-login.md) · [Guía 17: React](../guias/17-integracion-con-react.md#login-con-google) · [Referencia 21: JWT y roles](21-seguridad-jwt-y-roles-explicada.md) · [Catálogo de endpoints](23-catalogo-de-endpoints.md)
