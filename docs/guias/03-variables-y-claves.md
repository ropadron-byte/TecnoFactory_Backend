# Guía 03. Variables de entorno y claves

> **En una frase:** las claves no se escriben en el código ni en Git: viven en el archivo `.env` y se le pasan a Spring como variables de entorno.

## La idea

Una **variable de entorno** es un valor que el sistema le entrega a un programa al arrancar, como una nota pegada en la puerta. El código dice «lee la nota `JWT_SECRET`», y **tú** decides qué dice la nota en cada computador.

**La trampa más común:** Spring **no lee el `.env`**. Lo lee Docker Compose. A Spring hay que pasarle las variables al arrancar.

## Las variables

| Variable | Para qué | Local | Prod |
| --- | --- | --- | --- |
| `DB_PASSWORD` | Clave de la base para la app | Obligatoria (clave de root) | Obligatoria (la de `tecnofactory_app`) |
| `MIGRATOR_PASSWORD` | Clave de Flyway | Solo Docker | Obligatoria (`FLYWAY_PASSWORD`) |
| `APP_DB_PASSWORD` | Clave del usuario limitado | Solo Docker | Obligatoria |
| `JWT_SECRET` | Firma los tokens | Obligatoria (mínimo 32 bytes) | Obligatoria |
| `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Crean al administrador inicial | Opcionales | Opcionales |
| `ADMIN_RUN`, `ADMIN_RUN_DV`, `ADMIN_NOMBRE`, `ADMIN_APELLIDOS`, `ADMIN_FECHA_NACIMIENTO`, `ADMIN_DIRECCION`, `ADMIN_TELEFONO`, `ADMIN_COMUNA` | Datos del administrador inicial; si faltan se usan valores de relleno con `WARN` | Opcionales | Recomendadas |
| `CORS_ORIGINS` | Orígenes del frontend permitidos | Por defecto `http://localhost:5173` | Recomendada |
| `SPRING_PROFILES_ACTIVE` | Elige `local` o `prod` | `local` | `prod` |
| `DB_URL`, `DB_USER` | Dónde y con quién conectarse | Por defecto | Obligatorias |

`DB_PASSWORD` significa **cosas distintas** según el perfil: clave de `root` en local, clave de `tecnofactory_app` en prod.

## Cómo se usa

Cargar el `.env` en tu terminal y arrancar. El lanzador `./scripts/dev.sh` hace esto por ti (y en Windows `.\scripts\dev.ps1`):

```bash
set -a
source .env
set +a

SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3307/tecnofactory \
DB_USER=root \
./mvnw spring-boot:run
```

> MySQL se publica en el **3307** del host (dentro del contenedor sigue en el 3306), para no chocar con otro MySQL instalado en el computador.

Comprobar que una variable está cargada **sin mostrarla**: `echo "${#JWT_SECRET}"` imprime su largo (no el valor).

## Reglas que no hay que olvidar

1. **`JWT_SECRET` mide al menos 32 caracteres.** Si no, la app no arranca (`WeakKeyException`). **Tampoco tiene valor por defecto**: si falta, falla con `Could not resolve placeholder 'JWT_SECRET'` (así nadie puede olvidarla y quedar con una clave pública).
2. **Las claves compartidas fuera de tu equipo ya no son secretas.** No las reutilices en producción.
3. **No subas el `.env` ni `target/` al compartir un ZIP.**
4. **Genera claves con** `openssl rand -hex 16` (solo letras y números, sin caracteres problemáticos).
5. **Las variables viven en esa terminal.** Si abres otra, repite `set -a; source .env; set +a`.

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| `Could not resolve placeholder 'X'` | Falta la variable `X` | Cárgala; recuerda que Spring no lee el `.env` |
| `WeakKeyException` al arrancar | `JWT_SECRET` muy corto | Alárgalo a 32 o más |
| La app arranca pero no hay administrador | Faltan `ADMIN_EMAIL` o `ADMIN_PASSWORD` | Aparece un `WARN`; cárgalas y reinicia |

## Para saber más

[Referencia 03](../referencia/03-variables-y-claves.md) · [Guía 12: administrador inicial](12-administrador-inicial.md)
