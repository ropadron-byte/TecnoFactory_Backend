# 08. Seguridad

Este archivo describe **qué medidas hay y cuáles faltan**. El código de `SecurityConfig` y el concepto de BCrypt están explicados línea por línea en [15-seguridad-codigo-explicado.md](15-seguridad-codigo-explicado.md).

## En palabras simples

La seguridad de la API responde dos preguntas **en cada petición**:

1. **¿Quién eres?** (autenticación). Lo resuelve el **token**, como la tarjeta magnética de un hotel: la recibes al iniciar sesión y la muestras en cada puerta.
2. **¿Qué puedes hacer?** (autorización). Lo resuelve el **rol**: un cliente entra a la tienda, un vendedor además a productos y ventas, el administrador a todo.

| Si ocurre esto | La API responde |
| --- | --- |
| No muestras tarjeta, o está vencida o falsificada | **401** («no sé quién eres») |
| Tu tarjeta es válida, pero no abre esa puerta | **403** («sé quién eres, pero no puedes») |
| Todo en orden | La respuesta normal |

Una regla de oro: **lo que no está permitido explícitamente, queda protegido**. Y otra: **ocultar un botón en el frontend no protege nada**; la regla de verdad vive aquí, en el backend. Detalle completo, con la analogía del hotel, en [21](21-seguridad-jwt-y-roles-explicada.md#1-en-palabras-simples).


## Lo que ya está resuelto

| Medida | Dónde | Estado |
| --- | --- | --- |
| La app usa un usuario de MySQL sin DDL en producción | `tecnofactory_app` | **Probado** |
| Flyway usa un usuario separado | `tecnofactory_migrator` | **Probado** |
| Sin valores por defecto en prod | `application-prod.yaml` | Hecho |
| Swagger y `api-docs` apagados en prod | `application-prod.yaml` | Hecho |
| Errores sin mensaje, rastro ni detalle de validación en prod | `application-prod.yaml` | Hecho |
| Actuator publica solo `health`, sin detalles | `application-prod.yaml` | Hecho |
| Formato uniforme de errores de la API (`ErrorResponse`); el error 500 no revela detalles internos | `exception/GlobalExceptionHandler` | **Probado** con controller (20 oct): 400, 401, 403, 404, 405, 409 y 415; `GlobalExceptionHandlerTest` cubre el mensaje del 413. El 500 genérico, el 409 por `UNIQUE` de la base y el 400 por parámetro faltante tienen handler, pero **no test directo** |
| Secretos fuera del código, en `.env` | `.env` | Hecho (verificar `.gitignore`) |
| Contraseñas guardadas con BCrypt (`PasswordEncoder`): al registrar y al crear al administrador | `security/SecurityConfig`, `UsuarioService` | **En uso** (20 oct); el login compara contra el hash |
| CSRF desactivado y sin sesiones (`STATELESS`), como corresponde a una API con tokens | `security/SecurityConfig` | Escrito |
| Rutas explícitas por método y rol; **cualquier ruta sin regla queda denegada** (`anyRequest().denyAll()`) | `security/SecurityConfig` | Escrito y probado (6 nov) |
| Validación de claves al arrancar: `ADMIN_PASSWORD` de 8 a 72 caracteres / 72 bytes UTF-8; placeholders de `.env.example` rechazados | `ValidadorClaves`, `ConfiguracionValidator`, `AdministradorInicialRunner` | Escrito y probado (6 nov) |
| El detalle público de un blog **no expone** datos del autor; el correo solo aparece en `/api/v1/blogs/gestion/{id}` | `BlogPublicoResponse`, `BlogService` | Escrito y probado (6 nov) |
| Los metadatos de imágenes (`GET /api/v1/imagenes/**`) exigen personal; `/uploads/**` sigue público para los `<img>` | `security/SecurityConfig` | Escrito y probado (6 nov) |
| `/uploads/**` responde con `X-Content-Type-Options: nosniff` y `Content-Security-Policy: default-src 'none'` | `CabecerasUploadsFilter` | Escrito y probado (6 nov) |
| Login con JWT: correo y clave contra la base, token firmado de 1 hora | `AuthService`, `JwtService` | Escrito (20 oct); `JwtService` confirmado |
| Login con Google: ID token verificado (firma JWKS, `iss`, `aud`, `exp`, `email_verified`); cuentas nuevas completan perfil; cuentas con contraseña se vinculan solo con sesión | `GoogleTokenVerifier`, `GoogleAuthService`, `AuthController` | Escrito (11 nov); 269 tests en verde al 20 nov |
| `google_id` único y `contrasenia` opcional para cuentas solo-Google | `V8__google_auth.sql`, `Usuario` | Escrito (11 nov) |
| Filtro que lee `Authorization: Bearer ...` y consulta la base en cada petición (un usuario desactivado pierde el acceso al instante) | `security/JwtAuthFilter` | Escrito (20 oct) |
| 401 y 403 con el mismo JSON que los demás errores | `ManejadorErroresSeguridad` + `GlobalExceptionHandler` | Escrito (20 oct) |
| Los fallos de login no revelan qué correos existen (mismo 401 para los tres casos) | `AuthService` | Escrito (20 oct) |
| CORS: solo los orígenes de `CORS_ORIGINS` | `config/CorsConfig` | Escrito y la app arranca (20 oct) |
| `JWT_SECRET` de al menos 32 caracteres, o la app no arranca; **sin valor por defecto** en ningún perfil (3 nov) | `JwtService` y `application.yaml` | Escrito |
| Subida de imágenes: el contenido (primeros bytes) debe coincidir con el `Content-Type`, no solo el header que escribe el cliente (3 nov) | `AlmacenamientoService` | Escrito; probado con 10 tests nuevos |
| Un blog oculto lo ven solo el administrador y su autor; para el resto responde 404 (3 nov) | `BlogAutorizacion`, `BlogService`, `ImagenService` | Escrito |
| Administrador inicial creado por código, con la clave guardada como hash BCrypt (nunca en Git) | `UsuarioService` y `AdministradorInicialRunner` | **Verificado en el log** (18 oct); faltan 2 comprobaciones ([17, sección 9](17-administrador-inicial-explicado.md#9-qué-se-verificó-y-qué-no)) |

## Lo que falta

| Pendiente | Riesgo mientras no exista |
| --- | --- |
| **Confirmar el flujo de login en Swagger**: el arranque con MySQL ya está probado y los 269 tests pasan (20 nov) ([21, sección 14](21-seguridad-jwt-y-roles-explicada.md#14-cómo-probarlo-a-mano)) | Reglas escritas pero sin comprobar a mano |
| **Comprobar a mano las reglas ya escritas** del resto de la API (catálogo, carrito, boletas, perfil y creación de vendedores) con MySQL real | Están implementadas y cubiertas por tests; falta la prueba manual en Swagger |
| **Límite de intentos de login** | Fuerza bruta contra cuentas |
| **Token no revocable** (sin *refresh token* ni lista de bloqueo) | Un token robado sirve hasta que expira (1 hora); desactivar al usuario sí lo bloquea de inmediato |
| **Confirmar el administrador inicial**: falta un segundo arranque sin duplicar (la fila y la clave fuera del log ya están verificadas) | Si algo falla, nadie podría gestionar el catálogo. Comandos en [17, sección 8](17-administrador-inicial-explicado.md#8-comandos-importantes) |
| **HTTPS** | Las claves y tokens viajarían en texto plano |
| **Dónde guarda el frontend el token** | En `localStorage` lo podría leer un script malicioso (XSS) |
| **Validación de subida de archivos** | Subida de archivos maliciosos |

## Control de acceso por rol (estado actual)

Reglas vigentes en `SecurityConfig` (completadas el 6 nov y ampliadas con catálogo, carrito y boletas):

| Ruta | Acceso |
| --- | --- |
| Swagger, `/v3/api-docs/**` (solo `local`), `/actuator/health`, `/error`, `OPTIONS /**` | Público |
| `POST /api/v1/auth/login`, `POST /api/v1/auth/google`, `POST /api/v1/auth/google/completar`, `POST /api/v1/usuarios` (registro) y `POST /api/v1/contactos` | Público |
| `GET /api/v1/blogs`, `/api/v1/blogs/{id}`, `/api/v1/blogs/{id}/imagen`, `/api/v1/blogs/{id}/imagenes` | Público |
| `GET /api/v1/productos`, `/api/v1/productos/ofertas`, `/api/v1/productos/{id}`, `GET /api/v1/categorias`, `/api/v1/categorias/{id}` | Público (la tienda se ve sin sesión) |
| `GET /uploads/**` | Público (para los `<img>`; los metadatos no) |
| `POST /api/v1/auth/google/vincular`; `GET` y `PUT /api/v1/usuarios/me`; `/api/v1/carrito/**`; comprar, ver las propias y ver el detalle de `/api/v1/boletas/**` | Cualquiera con sesión |
| `PATCH /api/v1/usuarios/me/desactivar` | Solo `CLIENTE` (baja de su propia cuenta) |
| `GET /api/v1/contactos/**`, resto de `/api/v1/usuarios/**` (incluye lista, crear vendedores y borrar usuarios) | Solo `ADMINISTRADOR` |
| `GET /api/v1/boletas` (todas) y `PATCH /api/v1/boletas/{id}/anular` | Solo `ADMINISTRADOR` |
| Gestión de blogs (`/api/v1/blogs/gestion/**` y escrituras), gestión de imágenes (`/api/v1/imagenes/**`), `GET /api/v1/productos/admin` y `/api/v1/productos/criticos` | `ADMINISTRADOR` o `VENDEDOR` |
| Escrituras de `/api/v1/productos/**` y `/api/v1/categorias/**` | Solo `ADMINISTRADOR` |
| Cualquier otra ruta | **Denegada** (`denyAll`) |

Los roles vienen de la tabla `tipo_usuario` (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`). `UserDetailsServiceImpl` agrega el prefijo `ROLE_` al convertirlos. Los tokens JWT se firman con `JWT_SECRET` (mínimo 32 bytes) y duran 1 hora (`jwt.expiration-ms`). El login con Google añade una segunda forma de autenticarse, pero **emite el mismo JWT propio**: los roles y las reglas de `SecurityConfig` no cambian. Ver [29](29-login-con-google-explicado.md).

## Administrador inicial

> **Actualización 18 oct 2026:** se decidió seguir la **opción B** (crearlo por código al arrancar). Ya existen `Usuario`, `UsuarioRepository`, `TipoUsuarioRepository` y el `PasswordEncoder`; **ya están escritos** `UsuarioService` ("crear administrador si no existe") y `AdministradorInicialRunner`, y el primer arranque lo creó (log del 18 oct). Explicado línea por línea en [17](17-administrador-inicial-explicado.md). La plantilla `V4` y su procedimiento (opción A) quedan documentados abajo como alternativa, pero **no se va a aplicar**: ya no está en `docs/`.
>
> Cómo pasar las variables: Spring no lee el `.env` por sí solo. Con tu forma de arrancar (`set -a; source .env; set +a`) basta agregar `ADMIN_EMAIL`, `ADMIN_PASSWORD` y los `ADMIN_*` opcionales al `.env`, y quedan disponibles para `./mvnw spring-boot:run`. Reglas: la clave **nunca** va a un `log` ni a un mensaje de error; el administrador se crea **una sola vez** (si ya existe uno, el código no hace nada, de modo que cambiar las variables después **no** cambia los datos guardados); la clave se guarda con `passwordEncoder.encode(...)`; y se valida al arrancar (8 a 72 caracteres, máximo 72 bytes UTF-8, sin placeholders).
>
> Datos del administrador: todos sus campos (`ADMIN_RUN`, `ADMIN_NOMBRE`, `ADMIN_APELLIDOS`, `ADMIN_FECHA_NACIMIENTO`, `ADMIN_DIRECCION`, `ADMIN_TELEFONO`, `ADMIN_COMUNA`) se leen del entorno. Si faltan, se usan valores de relleno (`11111111-1`, `Por definir`, comuna `13101`) y el log escribe un `WARN`. Un dato inválido impide el arranque.

El usuario administrador **no está en `V2`** a propósito: necesita un hash BCrypt, y la clave real no debe pasar por chats ni escribirse en archivos del proyecto. Hay dos formas de crearlo:

| | A. Migración `V4` con el hash | B. Creación al arrancar desde variables de entorno |
| --- | --- | --- |
| Cómo funciona | Un `INSERT` en `V4__administrador_inicial.sql` con el hash ya calculado | Código que, al iniciar, crea el administrador si no existe, leyendo **todas** las variables `ADMIN_*` del entorno |
| ¿Se puede hacer hoy? | Sí, pero **descartada** | **Hecho** (18 oct): `UsuarioService` y `AdministradorInicialRunner` |
| ¿Qué queda en Git? | El **hash** (no la clave) | Nada |
| Riesgo | Si la clave es débil, el hash se puede adivinar con fuerza bruta | Ninguno en el repositorio |
| Cambiar la clave después | Con la API o un `UPDATE`; **no** editando `V4` | Igual |

**Decisión (18 oct):** se sigue la opción B, que no deja nada en Git y sirve igual en desarrollo y producción. La opción A se mantiene documentada: con una clave aleatoria de 24 caracteres el hash no es un riesgo práctico, pero queda para siempre en el historial de Git.

### A. Pasos con la migración `V4` (alternativa descartada)

**1. Genera una clave aleatoria** y guárdala en tu gestor de contraseñas. No es la clave del `.env`.

```bash
openssl rand -base64 24
```

**2. Genera su hash BCrypt.** Dos formas (elige una):

```bash
# Con Docker (no instala nada); escribe la clave sin que se muestre
read -s -p "Clave: " CLAVE; echo
docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 "" "$CLAVE" | tr -d ':\n'; echo
unset CLAVE
```

```bash
# Con Python (necesita: pip install bcrypt)
python3 -c "import bcrypt,getpass; print(bcrypt.hashpw(getpass.getpass('Clave: ').encode(), bcrypt.gensalt(10)).decode())"
```

El resultado es un texto de unos 60 caracteres que empieza con `$2y$10$` o `$2b$10$`. **Spring Security acepta ambos prefijos.** Cópialo entero.

**3. Edita `V4__administrador_inicial.sql`:** pega el hash en `contrasenia` y cambia los valores marcados con `CAMBIAR` (RUN con su dígito verificador, nombre, correo, teléfono, comuna). El correo es con el que iniciarás sesión.

**4. Recrea la base y arranca la app:**

```bash
docker compose down -v
docker compose up -d
```

**5. Comprueba:**

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory -e "
SELECT u.id_usuario, u.correo, t.nombre_tipo_usuario AS rol, LEFT(u.contrasenia, 7) AS prefijo, u.activo
FROM usuario u JOIN tipo_usuario t ON t.id_tipo_usuario = u.tipo_usuario_id_tipo_usuario;"
```

Debe mostrar tu correo, el rol `ADMINISTRADOR`, un prefijo `$2y$10$` o `$2b$10$` y `activo = 1`. Si el prefijo dice `REEMPLAZ`, no pegaste el hash.

### Detalles a tener en cuenta

- **Cuidado con el símbolo `$`** del hash en la terminal: va siempre entre comillas simples o dentro de un archivo. No lo pegues dentro de un comando con comillas dobles, porque el shell lo interpreta como variable.
- **No edites `V4` una vez aplicada** (Flyway fallará con `checksum mismatch`). Si te equivocaste y aún es desarrollo, recrea la base con `down -v`.
- **El RUN del administrador** no se valida en la base; se crea por código, fuera del `UsuarioRequest`. Para los registros nuevos el dígito verificador sí se comprueba con `@RunValido` (módulo 11, [guía 19](19-validacion-run-service-y-controller-explicados.md)). `11111111-1` es un RUN con dígito válido, pero conviene usar el tuyo o uno claramente de prueba.
- El carrito ya existe: cualquier usuario con sesión, incluido el administrador, usa el suyo en `/api/v1/carrito/**`.
- El login ya existe y compara la clave con el hash BCrypt. Queda confirmarlo a mano en Swagger contra MySQL real.
- La migración se probó en una base de pruebas (MariaDB 10.11) con un hash generado con Python; el comando de Docker/`htpasswd` no se ejecutó en esa prueba.

---

## Qué NO hacer

- No subir el `.env` a Git ni compartir sus valores por chat.
- No usar `root` en la app en producción.
- No dejar el mapeo `ports: "3307:3306"` en un servidor.
- No guardar números de tarjeta en el pago simulado.
- No devolver entidades JPA por la API (filtrarían el hash de la contraseña).
- No poner secretos en los yaml: van dentro del JAR y a Git.

## Lista de verificación antes de un despliegue real

- [ ] `.env` de producción con claves **nuevas** (`openssl rand -hex 32`), `chmod 600`.
- [ ] `git ls-files | grep -i env` no devuelve nada y `.gitignore` contiene `.env`.
- [ ] `SecurityConfig` escrita: rutas públicas y protegidas probadas.
- [ ] **`SecurityConfig` con `anyRequest().denyAll()`** y una regla explícita por ruta nueva. *(Ya escrito y probado el 6 nov.)*
- [ ] Contraseñas hasheadas con BCrypt (incluido el administrador inicial).
- [ ] CORS limitado al dominio real del frontend.
- [ ] MySQL sin publicar en el host (o solo en el 3307 para administrarlo).
- [ ] HTTPS con Nginx o Caddy.
- [ ] Swagger apagado y errores sin detalle (probado como en el Escenario 2).
- [ ] Respaldos automáticos guardados fuera del servidor y **restauración probada**.
- [ ] Firewall: solo 80/443 (y SSH restringido).
- [ ] Imágenes subidas en un volumen.
