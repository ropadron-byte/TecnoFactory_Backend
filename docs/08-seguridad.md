# 08. Seguridad

Este archivo describe **qué medidas hay y cuáles faltan**. El código de `SecurityConfig` y el concepto de BCrypt están explicados línea por línea en [15-seguridad-codigo-explicado.md](15-seguridad-codigo-explicado.md).

## Lo que ya está resuelto

| Medida | Dónde | Estado |
| --- | --- | --- |
| La app usa un usuario de MySQL sin DDL en producción | `tecnofactory_app` | **Probado** |
| Flyway usa un usuario separado | `tecnofactory_migrator` | **Probado** |
| Sin valores por defecto en prod | `application-prod.yaml` | Hecho |
| Swagger y `api-docs` apagados en prod | `application-prod.yaml` | Hecho |
| Errores sin mensaje, rastro ni detalle de validación en prod | `application-prod.yaml` | Hecho |
| Actuator publica solo `health`, sin detalles | `application-prod.yaml` | Hecho |
| Formato uniforme de errores de la API (`ErrorResponse`); el error 500 no revela detalles internos | `exception/GlobalExceptionHandler` | Escrito, con cobertura de errores de Spring (400, 404, 405, 403, 409, 500); **falta compilarlo y probarlo** con un controller (ver [Cambios recientes](cambios-recientes.md#cobertura-de-los-errores-de-spring)) |
| Secretos fuera del código, en `.env` | `.env` | Hecho (verificar `.gitignore`) |
| Codificador de contraseñas BCrypt disponible como bean (`PasswordEncoder`) | `security/SecurityConfig` | Escrito (18 oct); aún sin usar |
| CSRF desactivado y sin sesiones (`STATELESS`), como corresponde a una API con tokens | `security/SecurityConfig` | Escrito; Swagger carga (confirmado) |
| Swagger y `/actuator/health` como rutas públicas explícitas | `security/SecurityConfig` | Escrito |
| Administrador inicial creado por código, con la clave guardada como hash BCrypt (nunca en Git) | `UsuarioService` y `AdministradorInicialRunner` | **Verificado en el log** (18 oct); faltan 3 comprobaciones ([17, sección 9](17-administrador-inicial-explicado.md#9-qué-se-verificó-y-qué-no)) |

## Lo que falta

| Pendiente | Riesgo mientras no exista |
| --- | --- |
| **`SecurityConfig` definitivo.** Hoy está escrito pero **abierto**: `.anyRequest().permitAll()` | Cualquiera puede llamar a cualquier ruta. Aceptable solo en desarrollo local; **no se despliega** así. Pasa a `.authenticated()` en la fase 4 |
| **JWT** (`JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl`) | No hay login real |
| **Usar BCrypt** en el registro de usuarios y el login (ya se usa para el administrador inicial) | Nunca guardar texto plano en `usuario.contrasenia` |
| **CORS** (`CorsConfig`) | El navegador bloquea las peticiones de React |
| **Resolver los 401 y 403 en `SecurityConfig`** (token ausente o inválido; rutas por rol) | El `GlobalExceptionHandler` solo cubre el 403 de la seguridad por métodos; el resto lo decide Spring Security antes de llegar a los controllers |
| **Confirmar el administrador inicial**: fila en la base, segundo arranque sin duplicar y clave ausente del log | Si algo falla, nadie podría gestionar el catálogo. Comandos en [17, sección 8](17-administrador-inicial-explicado.md#8-comandos-importantes) |
| **HTTPS** | Las claves y tokens viajarían en texto plano |
| **Límite de intentos de login** | Fuerza bruta contra cuentas |
| **Validación de subida de archivos** | Subida de archivos maliciosos |

## Diseño previsto del control de acceso

| Ruta | Acceso |
| --- | --- |
| `POST /api/auth/**` (registro, login) | Público |
| `GET /api/productos/**`, `GET /api/categorias/**` | Público |
| `POST/PUT/DELETE` de catálogo | Administrador |
| Carrito y boletas propias | Usuario autenticado |
| Gestión de usuarios y roles | Administrador |
| `/actuator/health` | Público (solo `UP`/`DOWN`) |
| Swagger | Solo en `local` |

Los roles vienen de la tabla `tipo_usuario` (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`). Spring Security exige el prefijo `ROLE_` en las autoridades (`ROLE_ADMINISTRADOR`); `UserDetailsServiceImpl` debe añadirlo al convertir `nombre_tipo_usuario`.

Los tokens JWT se firman con `JWT_SECRET` (mínimo 32 caracteres) y duran 1 hora (`jwt.expiration-ms`).

## Administrador inicial

> **Actualización 18 oct 2026:** se decidió seguir la **opción B** (crearlo por código al arrancar). Ya existen `Usuario`, `UsuarioRepository`, `TipoUsuarioRepository` y el `PasswordEncoder`; **ya están escritos** `UsuarioService` ("crear administrador si no existe") y `AdministradorInicialRunner`, y el primer arranque lo creó (log del 18 oct). Explicado línea por línea en [17](17-administrador-inicial-explicado.md). La plantilla `V4` y su procedimiento (opción A) quedan documentados abajo como alternativa, pero **no se va a aplicar**: ya no está en `docs/`.
>
> Cómo pasar las variables: Spring no lee el `.env` por sí solo. Con tu forma de arrancar (`set -a; source .env; set +a`) basta agregar `ADMIN_EMAIL` y `ADMIN_PASSWORD` al `.env`, y quedan disponibles para `./mvnw spring-boot:run`. Reglas: la clave **nunca** va a un `log` ni a un mensaje de error; el administrador se crea **una sola vez** (si ya existe uno, el código no hace nada, de modo que cambiar la variable después **no** cambia la clave); y la clave se guarda con `passwordEncoder.encode(...)`.
>
> Datos del administrador: además del correo y la clave (variables de entorno), el runner usa **valores de relleno fijos** para `run` (`11111111-1`), nombre, apellidos, fecha, dirección, teléfono y comuna (`13101`). Se eligió así por defecto y se puede cambiar después.

El usuario administrador **no está en `V2`** a propósito: necesita un hash BCrypt, y la clave real no debe pasar por chats ni escribirse en archivos del proyecto. Hay dos formas de crearlo:

| | A. Migración `V4` con el hash | B. Creación al arrancar desde variables de entorno |
| --- | --- | --- |
| Cómo funciona | Un `INSERT` en `V4__administrador_inicial.sql` con el hash ya calculado | Código que, al iniciar, crea el administrador si no existe, leyendo `ADMIN_EMAIL` y `ADMIN_PASSWORD` del entorno |
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
- No dejar `ports: "3306:3306"` en un servidor.
- No guardar números de tarjeta en el pago simulado.
- No devolver entidades JPA por la API (filtrarían el hash de la contraseña).
- No poner secretos en los yaml: van dentro del JAR y a Git.

## Lista de verificación antes de un despliegue real

- [ ] `.env` de producción con claves **nuevas** (`openssl rand -hex 32`), `chmod 600`.
- [ ] `git ls-files | grep -i env` no devuelve nada y `.gitignore` contiene `.env`.
- [ ] `SecurityConfig` escrita: rutas públicas y protegidas probadas.
- [ ] **`.anyRequest().permitAll()` reemplazado por `.anyRequest().authenticated()`** (hoy está abierto a propósito, solo para desarrollo).
- [ ] Contraseñas hasheadas con BCrypt (incluido el administrador inicial).
- [ ] CORS limitado al dominio real del frontend.
- [ ] Puerto 3306 sin publicar.
- [ ] HTTPS con Nginx o Caddy.
- [ ] Swagger apagado y errores sin detalle (probado como en el Escenario 2).
- [ ] Respaldos automáticos guardados fuera del servidor y **restauración probada**.
- [ ] Firewall: solo 80/443 (y SSH restringido).
- [ ] Imágenes subidas en un volumen.
