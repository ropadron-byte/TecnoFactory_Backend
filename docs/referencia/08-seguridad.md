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
| Formato uniforme de errores de la API (`ErrorResponse`); el error 500 no revela detalles internos | `exception/GlobalExceptionHandler` | **Probado** con controller (20 oct): 400, 401, 403, 404, 405, 409 y 415; faltan 500 y el 409 de la base (ver [Cambios recientes](../estado/cambios-recientes.md#cobertura-de-los-errores-de-spring)) |
| Secretos fuera del código, en `.env` | `.env` | Hecho (verificar `.gitignore`) |
| Contraseñas guardadas con BCrypt (`PasswordEncoder`): al registrar y al crear al administrador | `security/SecurityConfig`, `UsuarioService` | **En uso** (20 oct); el login compara contra el hash |
| CSRF desactivado y sin sesiones (`STATELESS`), como corresponde a una API con tokens | `security/SecurityConfig` | Escrito |
| Rutas explícitas por método y rol; **cualquier ruta sin regla queda denegada** (`anyRequest().denyAll()`) | `security/SecurityConfig` | Escrito y probado (6 nov) |
| Validación de claves al arrancar: `ADMIN_PASSWORD` de 8 a 72 caracteres / 72 bytes UTF-8; placeholders de `.env.example` rechazados | `ValidadorClaves`, `ConfiguracionValidator`, `AdministradorInicialRunner` | Escrito y probado (6 nov) |
| El detalle público de un blog **no expone** datos del autor; el correo solo aparece en `/api/blogs/gestion/{id}` | `BlogPublicoResponse`, `BlogService` | Escrito y probado (6 nov) |
| Los metadatos de imágenes (`GET /api/imagenes/**`) exigen personal; `/uploads/**` sigue público para los `<img>` | `security/SecurityConfig` | Escrito y probado (6 nov) |
| `/uploads/**` responde con `X-Content-Type-Options: nosniff` y `Content-Security-Policy: default-src 'none'` | `CabecerasUploadsFilter` | Escrito y probado (6 nov) |
| Login con JWT: correo y clave contra la base, token firmado de 1 hora | `AuthService`, `JwtService` | Escrito (20 oct); `JwtService` confirmado |
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
| **Confirmar la seguridad nueva**: los 56 tests, el arranque y el flujo de login en Swagger ([21, sección 14](21-seguridad-jwt-y-roles-explicada.md#14-cómo-probarlo-a-mano)) | Reglas escritas pero sin comprobar |
| **Reglas por rol del resto de la API**: lista de usuarios (administrador y vendedor), editar el propio perfil, crear vendedores, catálogo, órdenes | Hoy solo hay dos niveles: administrador y «cualquiera con sesión» |
| **Límite de intentos de login** | Fuerza bruta contra cuentas |
| **Token no revocable** (sin *refresh token* ni lista de bloqueo) | Un token robado sirve hasta que expira (1 hora); desactivar al usuario sí lo bloquea de inmediato |
| **Confirmar el administrador inicial**: falta un segundo arranque sin duplicar (la fila y la clave fuera del log ya están verificadas) | Si algo falla, nadie podría gestionar el catálogo. Comandos en [17, sección 8](17-administrador-inicial-explicado.md#8-comandos-importantes) |
| **HTTPS** | Las claves y tokens viajarían en texto plano |
| **Dónde guarda el frontend el token** | En `localStorage` lo podría leer un script malicioso (XSS) |
| **Validación de subida de archivos** | Subida de archivos maliciosos |

## Diseño previsto del control de acceso

**Hoy en `SecurityConfig` (6 nov):**

| Ruta | Acceso |
| --- | --- |
| Swagger, `/v3/api-docs/**` (solo `local`), `/actuator/health`, `/error`, `OPTIONS /**` | Público |
| `POST /api/auth/login`, `POST /api/usuarios` (registro) y `POST /api/contactos` | Público |
| `GET /api/contactos` y `GET /api/contactos/{id}` | Solo `ADMINISTRADOR` |
| `GET /api/usuarios/me` | Cualquiera con sesión |
| Resto de `/api/usuarios/**` | Solo `ADMINISTRADOR` |
| `GET /api/blogs/gestion/**` | `ADMINISTRADOR` o `VENDEDOR` |
| `GET /api/blogs`, `/api/blogs/{id}`, `/api/blogs/{id}/imagen`, `/api/blogs/{id}/imagenes` | Público |
| `POST/PUT/PATCH/DELETE /api/blogs/**` | `ADMINISTRADOR` o `VENDEDOR` |
| `GET /api/imagenes/**` | `ADMINISTRADOR` o `VENDEDOR` |
| `GET /uploads/**` | Público |
| `POST/PUT/DELETE /api/imagenes/**` | `ADMINISTRADOR` o `VENDEDOR` |
| Cualquier otra ruta | **Denegada** (`denyAll`) |

**Previsto al llegar cada entidad:**

| Ruta | Acceso |
| --- | --- |
| `GET /api/productos/**`, `GET /api/categorias/**` | Público (la tienda se ve sin sesión) |
| `POST/PUT/DELETE` de productos y stock | `VENDEDOR` y `ADMINISTRADOR` |
| `POST/PUT/DELETE` de categorías | `ADMINISTRADOR` |
| Órdenes (ver ventas) | `VENDEDOR` y `ADMINISTRADOR` |
| `GET /api/usuarios` (lista) | `ADMINISTRADOR` y `VENDEDOR` (solo lectura) |
| Editar o desactivar a otros usuarios, crear vendedores, reportes | `ADMINISTRADOR` |
| Carrito y boletas propias | Usuario autenticado, solo lo suyo |

Los roles vienen de la tabla `tipo_usuario` (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`). `UserDetailsServiceImpl` agrega el prefijo `ROLE_` al convertirlos. Los tokens JWT se firman con `JWT_SECRET` (mínimo 32 bytes) y duran 1 hora (`jwt.expiration-ms`).

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
- **El RUN del administrador** no se valida en la base. El dígito verificador solo se comprueba en el servicio para los registros nuevos (fase 4). `11111111-1` es un RUN con dígito válido, pero conviene usar el tuyo o uno claramente de prueba.
- **No hay carrito** para el administrador todavía; se creará cuando exista el servicio de carrito (fase 5).
- La comprobación de que se puede **iniciar sesión** con esa clave solo es posible cuando exista el login (fase 4).
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
