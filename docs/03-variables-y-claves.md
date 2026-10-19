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
| `DB_URL` | Por defecto `jdbc:mysql://localhost:3306/tecnofactory` | **Obligatoria** | En Docker: `jdbc:mysql://db:3306/tecnofactory` |
| `DB_USER` | Por defecto `root` | **Obligatoria** (`tecnofactory_app`) | |
| `DB_PASSWORD` | **Obligatoria** | **Obligatoria** | |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | No se usan (Flyway usa la conexión de la app) | **Obligatorias** | Solo en `application-prod.yaml` |
| `JWT_SECRET` | **Obligatoria** | **Obligatoria** | Mínimo 32 caracteres |
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

`${DB_URL:jdbc:mysql://localhost:3306/tecnofactory}` significa "usa la variable si existe; si no, este valor".

En producción **no se ponen valores por defecto a propósito**: prefieres que la app falle con un error claro antes que conectarse por accidente a `localhost` o usar una clave de ejemplo.

> **Ojo:** si una variable sin valor por defecto no está definida, Spring puede dejar el texto literal `${DB_URL}` como si fuera el valor. Por eso aparecen errores como `'url' must start with "jdbc"` o `Access denied for user '${DB_USER}'`. **Cuando veas `${...}` dentro de un mensaje de error, esa variable no está definida.**

## Reglas de seguridad para los secretos

- El `.env` **nunca** va a Git. Comprueba: `git ls-files | grep -i env` no debe devolver nada, y `.gitignore` debe contener `.env`.
- Si un `.env` ya se subió: `git rm --cached .env`, sube el cambio, y **cambia todas las claves**: quedaron en el historial de Git aunque borres el archivo.
- Protégelo: `chmod 600 .env`.
- **Las claves compartidas en chats, capturas o repositorios ya no son secretas.** Úsalas solo en desarrollo y genera otras para producción.
- Guarda una copia del `.env` de producción en un gestor de contraseñas: si se pierde, no hay forma de recuperar las claves.
- Los yaml van dentro del JAR y a Git: nunca contienen secretos reales.
