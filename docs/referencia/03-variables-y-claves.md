# 03. Variables de entorno y claves

## Dos mundos que no se hablan

| Programa | Qué lee | Cuándo |
| --- | --- | --- |
| **Docker Compose** | El archivo `.env` (automáticamente) | Al ejecutar `docker compose ...` |
| **Spring Boot** | Las **variables de entorno del proceso Java** | Al arrancar la app |

Spring **no lee el `.env`**. Alguien tiene que pasarle las variables:

| Cómo corre la app | Quién le da las variables |
| --- | --- |
| Terminal | Tú: `set -a; source .env; set +a` y las del comando |
| IntelliJ | *Run, Edit Configurations, Environment variables* |
| JAR a mano | Tú, en la línea de comando |
| Contenedor `backend` | El bloque `environment:` del compose (que a su vez toma valores del `.env`) |

## Las cuatro claves del `.env`

Copia `.env.example` a `.env` la primera vez:

```bash
cp .env.example .env
chmod 600 .env
```

```env
DB_PASSWORD=...          # clave de root
MIGRATOR_PASSWORD=...    # clave de tecnofactory_migrator
APP_DB_PASSWORD=...      # clave de tecnofactory_app
JWT_SECRET=...           # firma los tokens (mínimo 32 caracteres)
```

Usa **claves distintas** para cada una.

### Cómo generarlas

```bash
openssl rand -hex 32
```

Entrega 64 caracteres aleatorios, solo letras y números. Se usa `-hex` porque `$`, `#`, `'`, `@` o `"` dan problemas en `.env`, en Compose y en el script de MySQL.

## Traducción: del `.env` a Spring

| En el `.env` | Compose lo usa para | Variable que recibe Spring |
| --- | --- | --- |
| `DB_PASSWORD` | Clave de `root` | `DB_PASSWORD` **en local** (la app entra como root) |
| `MIGRATOR_PASSWORD` | Clave de `tecnofactory_migrator` | `FLYWAY_PASSWORD` |
| `APP_DB_PASSWORD` | Clave de `tecnofactory_app` | `DB_PASSWORD` **en prod** |
| `JWT_SECRET` | No lo usa | `JWT_SECRET` |

`DB_PASSWORD` significa **cosas distintas** según el perfil. Por eso en los comandos de prod se escribe `DB_PASSWORD="$APP_DB_PASSWORD"`: esa asignación, en la misma línea del comando, tiene prioridad sobre el `DB_PASSWORD` (clave de root) que cargó `source .env`.

## Todas las variables que lee Spring

| Variable | Local | Prod | Notas |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | `prod` | Elige el perfil |
| `DB_URL` | Por defecto `jdbc:mysql://localhost:3307/tecnofactory` | **Obligatoria** | En Docker: `jdbc:mysql://db:3306/tecnofactory` |
| `DB_USER` | Por defecto `root` | **Obligatoria** (`tecnofactory_app`) | |
| `DB_PASSWORD` | **Obligatoria** | **Obligatoria** | |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | No se usan (Flyway usa la conexión de la app) | **Obligatorias** | Solo en `application-prod.yaml` |
| `JWT_SECRET` | **Obligatoria** | **Obligatoria** | Mínimo 32 caracteres. **Si mide menos, la app no arranca** (`WeakKeyException`). Con ella se firman los tokens: quien la conozca puede falsificar una sesión. Ver [21](21-seguridad-jwt-y-roles-explicada.md#3-qué-es-un-jwt) |
| `CORS_ORIGINS` | Por defecto `http://localhost:5173` | **Recomendada** | Orígenes del frontend permitidos por CORS, separados por comas. En prod, la URL real, **nunca `*`**. Ver [20](20-configuracion-base-explicada.md#6-cors) |
| `REDIS_HOST` | Por defecto `localhost` | — | Sin uso mientras `cache.type: none` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` | Por defecto `localhost` / `1025` | — | Sin uso hasta implementar correos |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Opcionales | Opcionales | Las lee `AdministradorInicialRunner`. Si faltan, la app arranca y avisa con un `WARN`. Ver [Administrador inicial](#variables-del-administrador-inicial) |

## Variables del administrador inicial

El administrador se crea por código al arrancar (decisión del 18 oct; ver [08](08-seguridad.md#administrador-inicial)). Su correo y su clave vienen de dos variables de entorno, para que **nunca** queden escritas en el repositorio:

```env
ADMIN_EMAIL=...          # correo con el que iniciará sesión
ADMIN_PASSWORD=...       # su clave en texto plano; el código la convierte en hash BCrypt
```

| Detalle | Explicación |
| --- | --- |
| Dónde van | En el mismo `.env`. Con `set -a; source .env; set +a` quedan disponibles para `./mvnw spring-boot:run`. En IntelliJ, en *Environment variables* |
| Docker Compose | Ignora las variables del `.env` que el compose no usa: no molestan |
| Cómo generar la clave | `openssl rand -hex 16`. Mismo criterio que arriba: evita `$`, `#`, `'`, `@` y `"` |
| Cuándo se usan | **Solo la primera vez**: si ya existe un administrador, el código no hace nada. Cambiar la variable después **no** cambia la clave guardada |
| Nunca | Escribirlas en un `log`, en un mensaje de error ni en los yaml |

Estado: las lee `AdministradorInicialRunner` (paso 7), explicado en [17](17-administrador-inicial-explicado.md). Son **opcionales**: sin ellas la app arranca igual y escribe un `WARN` en el log, pero no se crea el administrador.

## Sintaxis `${VARIABLE:valor}`

`${DB_URL:jdbc:mysql://localhost:3307/tecnofactory}` significa "usa la variable si existe; si no, este valor".

En producción **no se ponen valores por defecto a propósito**: prefieres que la app falle con un error claro antes que conectarse por accidente a `localhost` o usar una clave de ejemplo.

> **Ojo:** si una variable sin valor por defecto no está definida, Spring puede dejar el texto literal `${DB_URL}` como si fuera el valor. Por eso aparecen errores como `'url' must start with "jdbc"` o `Access denied for user '${DB_USER}'`. **Cuando veas `${...}` dentro de un mensaje de error, esa variable no está definida.**

## Reglas de seguridad para los secretos

- El `.env` **nunca** va a Git. Comprueba: `git ls-files | grep -i env` no debe devolver nada, y `.gitignore` debe contener `.env`.
- Si un `.env` ya se subió: `git rm --cached .env`, sube el cambio, y **cambia todas las claves**: quedaron en el historial de Git aunque borres el archivo.
- Protégelo: `chmod 600 .env`.
- **Las claves compartidas en chats, capturas o repositorios ya no son secretas.** Úsalas solo en desarrollo y genera otras para producción.
- Guarda una copia del `.env` de producción en un gestor de contraseñas: si se pierde, no hay forma de recuperar las claves.
- Los yaml van dentro del JAR y a Git: nunca contienen secretos reales.

## Rotar claves que ya se filtraron

Si el `.env` estuvo en Git, en un chat o en una captura, cambia las claves. El volumen de MySQL ya existe, así que `MYSQL_*` **no** se reaplica: hay que cambiarlas con `ALTER USER`.

```bash
# 1. Genera valores nuevos
openssl rand -hex 32   # DB_PASSWORD, MIGRATOR_PASSWORD, APP_DB_PASSWORD, JWT_SECRET
openssl rand -hex 16   # ADMIN_PASSWORD

# 2. Cámbialas en MySQL (pide la clave actual de root)
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" tecnofactory-db mysql -uroot <<'SQL'
ALTER USER 'root'@'%' IDENTIFIED BY 'NUEVA_ROOT';
ALTER USER 'root'@'localhost' IDENTIFIED BY 'NUEVA_ROOT';
ALTER USER 'tecnofactory_migrator'@'%' IDENTIFIED BY 'NUEVA_MIGRATOR';
ALTER USER 'tecnofactory_app'@'%' IDENTIFIED BY 'NUEVA_APP';
FLUSH PRIVILEGES;
SQL

# 3. Escribe las claves nuevas en .env y recrea el contenedor
docker compose up -d --force-recreate --wait db
```

Detalles importantes:

- **`--force-recreate` es necesario**: el healthcheck del contenedor usa `DB_PASSWORD`; si no lo recreas, sigue probando con la clave vieja.
- **`JWT_SECRET` nuevo invalida los tokens** ya emitidos: los usuarios deben volver a iniciar sesión.
- **`ADMIN_PASSWORD` nuevo no cambia el hash guardado.** El administrador solo se crea si no existe. Para cambiarlo, actualiza `usuario.contrasenia` con un hash BCrypt nuevo (o borra ese usuario para que se recree al arrancar).
- El orden importa: primero `ALTER USER`, después `.env` y por último recrear. Si lo haces al revés, la app no podrá autenticarse.
