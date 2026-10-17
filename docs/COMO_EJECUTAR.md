# Cómo ejecutar TecnoFactory

Guía única para arrancar el backend en tres escenarios:

1. **Local**: desarrollo diario.
2. **Prod de prueba**: simula producción en tu máquina para probar el script de migración (`V1__esquema_inicial.sql`) y los permisos.
3. **Prod real**: cómo debe quedar cuando el proyecto esté terminado y desplegado en un servidor.

---

## Tabla de contenidos

- [Resumen de los tres escenarios](#resumen-de-los-tres-escenarios)
- [Conceptos clave](#conceptos-clave)
- [Requisitos](#requisitos)
- [Archivo .env](#archivo-env)
- [Escenario 1: Local](#escenario-1-local)
- [Escenario 2: Prod de prueba](#escenario-2-prod-de-prueba-en-tu-máquina)
- [Escenario 3: Prod real](#escenario-3-prod-real-proyecto-terminado)
- [Agregar cambios a la base de datos (V2, V3...)](#agregar-cambios-a-la-base-de-datos)
- [Errores comunes](#errores-comunes)
- [Comandos de Docker de referencia](#comandos-de-docker-de-referencia)

---

## Resumen de los tres escenarios

| | 1. Local | 2. Prod de prueba | 3. Prod real |
| --- | --- | --- | --- |
| Dónde corre la app | Tu máquina (Maven / IntelliJ) | Tu máquina (JAR) | Contenedor Docker en el servidor |
| Dónde corre MySQL | Docker | Docker | Docker (sin puerto público) |
| Perfil | `local` | `prod` | `prod` |
| Usuario de la app | `root` | `tecnofactory_app` (mínimo privilegio) | `tecnofactory_app` |
| Usuario de Flyway | `root` (el mismo de la app) | `tecnofactory_migrator` | `tecnofactory_migrator` |
| Tablas | Las crea Flyway | Las crea Flyway | Las crea Flyway |
| Hibernate | `validate` | `validate` | `validate` |
| Swagger | Visible | Apagado | Apagado |
| Errores de la API | Con detalle | Sin detalle | Sin detalle |
| `/actuator/health` | Con detalles | Solo `UP` / `DOWN` | Solo `UP` / `DOWN` |
| Valores por defecto | Algunos | Ninguno (si falta una variable, no arranca) | Ninguno |

> **Cambio respecto a la documentación anterior:** en local ya **no** crea las tablas Hibernate (`ddl-auto: update`) ni está Flyway apagado. Tu `application-local.yaml` actual deja Flyway **encendido** y Hibernate en `validate`. Así local y prod crean las tablas por el mismo camino, y un error en la migración aparece al desarrollar, no al desplegar.

---

## Conceptos clave

### Los tres usuarios de MySQL

| Usuario | Quién lo crea | Permisos | Lo usa |
| --- | --- | --- | --- |
| `root` | La imagen de MySQL (`MYSQL_ROOT_PASSWORD`) | Todo el servidor | Solo tú, para administrar. En local también la app. |
| `tecnofactory_migrator` | La imagen de MySQL (`MYSQL_USER` / `MYSQL_PASSWORD`) | `ALL` sobre la base `tecnofactory` (crear, alterar y borrar tablas) | **Flyway** |
| `tecnofactory_app` | El script `docker/mysql-init/01-crear-usuario-app.sh` | Solo `SELECT, INSERT, UPDATE, DELETE` sobre `tecnofactory` | **La aplicación** en ejecución |

Si alguien explota una inyección SQL en la API, entra como `tecnofactory_app`, que no puede hacer `DROP TABLE`. Por eso en producción la app y Flyway usan usuarios distintos.

### De dónde sale cada variable de entorno

Hay **dos mundos** que no se hablan entre sí:

- **Docker Compose** lee el archivo `.env` automáticamente y usa sus valores para crear MySQL.
- **Spring Boot no lee el `.env`.** Solo lee las variables de entorno del proceso Java. Alguien tiene que dárselas: tú en la terminal, IntelliJ, o el bloque `environment:` del servicio `backend` en el compose.

Cada nombre de variable del `.env` se traduce así:

| En el `.env` | Lo usa Compose para | Variable que recibe Spring |
| --- | --- | --- |
| `DB_PASSWORD` | Clave de `root` | `DB_PASSWORD` en local. **No se usa** en prod. |
| `MIGRATOR_PASSWORD` | Clave de `tecnofactory_migrator` | `FLYWAY_PASSWORD` |
| `APP_DB_PASSWORD` | Clave de `tecnofactory_app` | `DB_PASSWORD` en prod |
| `JWT_SECRET` | (no lo usa Compose) | `JWT_SECRET` |

Fíjate en que `DB_PASSWORD` significa **cosas distintas** según el perfil: en local es la clave de `root`; en prod es la clave de `tecnofactory_app`. Por eso en los comandos de prod se escribe `DB_PASSWORD="$APP_DB_PASSWORD"`.

### Cuándo se ejecuta cada cosa

- Los scripts de `docker/mysql-init/` y las variables `MYSQL_*` **solo se aplican la primera vez que se crea el volumen** `mysql_data`. Si cambias una clave en el `.env` o editas el script después, MySQL no se entera. Para repetir la inicialización hay que borrar el volumen (`docker compose down -v`, **borra los datos**).
- Flyway se ejecuta **cada vez que arranca la aplicación**: mira la tabla `flyway_schema_history` y aplica las migraciones que falten.

---

## Requisitos

- **JDK 21** completo, no solo el JRE. Comprueba con `javac -version`.
- **Docker** y **Docker Compose**. Comprueba con `docker compose version`.
- Ejecuta todos los comandos **desde la carpeta raíz del proyecto** (la que contiene `mvnw` y `docker-compose.yml`).

---

## Archivo .env

Crea un `.env` junto a `docker-compose.yml`:

```env
DB_PASSWORD=clave_de_root
MIGRATOR_PASSWORD=clave_del_migrator
APP_DB_PASSWORD=clave_de_la_app
JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres
```

Reglas:

- El `.env` **no se sube a Git**. Comprueba que `.gitignore` lo incluya.
- **Evita `$`, `'` y `"` en las contraseñas.** Compose interpreta `$` como variable, y el script de `mysql-init` mete la clave dentro de una comilla simple de SQL.
- Genera claves con `openssl rand -base64 24` (las de prod, distintas a las de desarrollo).
- Sin espacios alrededor del `=`.
- Las claves de `.env` de desarrollo **no se reutilizan** en producción.

---

## Escenario 1: Local

Para el trabajo diario. La app usa `root`; Flyway usa la misma conexión.

### 1. Levantar MySQL

```bash
docker compose up -d
docker compose ps        # debe decir "healthy"
```

### 2. Arrancar la aplicación

**Opción A: cargando el `.env` en la terminal (recomendada)**

```bash
set -a
source .env
set +a

SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
./mvnw spring-boot:run
```

`source .env` exporta `DB_PASSWORD` (clave de root) y `JWT_SECRET`, que es justo lo que pide el perfil `local`.

**Opción B: IntelliJ**

*Run, Edit Configurations*, en **Environment variables**:

```
SPRING_PROFILES_ACTIVE=local;DB_URL=jdbc:mysql://localhost:3306/tecnofactory;DB_USER=root;DB_PASSWORD=clave_de_root;JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres
```

### 3. Comprobar

En el log deben aparecer:

```
The following 1 profile is active: "local"
Successfully applied 1 migration to schema `tecnofactory`    (solo la primera vez)
HikariPool-1 - Start completed.
Tomcat started on port 8080 (http)
```

Mientras `SecurityConfig` esté vacío, Spring Security protege **todas** las rutas. En el log aparece `Using generated security password: ...`. Entra con usuario `user` y esa clave (cambia en cada arranque).

| Recurso | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Estado | http://localhost:8080/actuator/health |

### 4. Corrección pendiente en `application-local.yaml`

Hoy el bloque `jpa:` está mal indentado: `hibernate:` quedó al mismo nivel que `jpa:`, así que Spring **ignora** `show-sql` y `format_sql` (y `ddl-auto` cae al valor de `application.yaml`, que también es `validate`). Debe ser:

```yaml
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
```

Fíjate también en que `show-sql` va bajo `jpa:`, no bajo `hibernate:`.

### 5. Detener

- App: `Ctrl + C` o el botón de IntelliJ.
- MySQL: `docker compose stop` (conserva los datos).

---

## Escenario 2: Prod de prueba (en tu máquina)

Objetivo: comprobar que `V1__esquema_inicial.sql` crea todas las tablas desde cero con el usuario `tecnofactory_migrator`, y que la app funciona con el usuario limitado `tecnofactory_app`.

La app corre como JAR en tu máquina y MySQL en Docker. No hace falta el `Dockerfile`.

### Antes de empezar

- [ ] `src/main/resources/db/migration/V1__esquema_inicial.sql` existe (el nombre lleva **dos guiones bajos**: `V1__`).
- [ ] El `.env` tiene las **cuatro** variables (`DB_PASSWORD`, `MIGRATOR_PASSWORD`, `APP_DB_PASSWORD`, `JWT_SECRET`).
- [ ] `docker/mysql-init/01-crear-usuario-app.sh` existe.

### 1. Empezar con una base vacía

Es obligatorio para que la prueba sea real: si el volumen ya existía, MySQL no vuelve a crear los usuarios ni la base, y Flyway puede encontrar tablas viejas.

```bash
docker compose down -v      # BORRA todos los datos locales
docker compose up -d
docker compose ps           # esperar "healthy"
```

### 2. Verificar que los usuarios existen

```bash
docker exec -it tecnofactory-db mysql -uroot -p -e "SELECT user, host FROM mysql.user WHERE user LIKE 'tecnofactory%';"
```

Debes ver `tecnofactory_migrator` y `tecnofactory_app`. Si falta `tecnofactory_app`, el script de `mysql-init` no se ejecutó (ver [Errores comunes](#errores-comunes)).

### 3. Construir el JAR

```bash
./mvnw clean package -DskipTests
```

`-DskipTests` porque los tests necesitan Docker y una base.

### 4. Arrancar con el perfil prod

```bash
set -a
source .env
set +a

SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=tecnofactory_app \
DB_PASSWORD="$APP_DB_PASSWORD" \
FLYWAY_USER=tecnofactory_migrator \
FLYWAY_PASSWORD="$MIGRATOR_PASSWORD" \
java -jar target/TecnoFactory-0.0.1-SNAPSHOT.jar
```

Qué hace cada línea:

| Variable | Valor | Para qué |
| --- | --- | --- |
| `DB_USER` / `DB_PASSWORD` | `tecnofactory_app` / `$APP_DB_PASSWORD` | La app en ejecución (solo DML) |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | `tecnofactory_migrator` / `$MIGRATOR_PASSWORD` | Flyway al crear tablas (DDL) |
| `JWT_SECRET` | Viene del `.env` cargado con `source` | Firma de tokens |

La asignación `DB_PASSWORD="$APP_DB_PASSWORD"` en la misma línea del comando **tiene prioridad** sobre el `DB_PASSWORD` (clave de root) que cargó `source .env`.

### 5. Qué debe aparecer en el log

```
The following 1 profile is active: "prod"
Migrating schema `tecnofactory` to version "1 - esquema inicial"
Successfully applied 1 migration to schema `tecnofactory`
Started TecnoFactoryApplication in ... seconds
```

El log será corto porque en prod el nivel es `WARN` para librerías. Si no ves las líneas de Flyway, confirma las tablas en el paso 6.

### 6. Verificar que la migración funcionó

Tablas creadas (entra como migrator; te pide la clave `MIGRATOR_PASSWORD`):

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory -e "SHOW TABLES;"
```

Debes ver las 13 tablas de tu modelo más `flyway_schema_history` (14 en total):
`BLOG, BOLETA, CARRITO, CATEGORIA, COMUNA, DETALLE_BOLETA, DETALLE_CARRITO, IMAGEN_URL, PRODUCTO_CATEGORIA, PRODUCTOS, REGION, TIPO_USUARIO, USUARIO`.

Historial de Flyway:

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory -e "SELECT version, description, success FROM flyway_schema_history;"
```

Debe mostrar la versión `1` con `success = 1`.

### 7. Verificar los permisos mínimos

Con el usuario de la app (clave `APP_DB_PASSWORD`):

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_app -p tecnofactory -e "SELECT COUNT(*) FROM USUARIO;"
docker exec -it tecnofactory-db mysql -utecnofactory_app -p tecnofactory -e "DROP TABLE BLOG;"
```

El primer comando funciona. El segundo **debe fallar** con `ERROR 1142 ... DROP command denied`. Si funciona, los permisos están mal.

### 8. Verificar el comportamiento de prod

- Swagger **no carga**: `http://localhost:8080/swagger-ui/index.html` da error.
- `/actuator/health` solo dice `UP` o `DOWN`, sin detalles.
- Una ruta que falle no muestra mensajes ni rastros.
- Mientras `SecurityConfig` esté vacío, todo pide el usuario temporal `user` y la clave del log (con perfil prod el nivel de log es `WARN`, así que esa línea **no se imprime**: sube temporalmente `com.factory`/`root` o escribe el `SecurityConfig` antes de probar rutas).

### 9. Volver a desarrollo

`Ctrl + C` para la app. Si quieres seguir en local con la base que dejó prod, no hay problema (Flyway ya aplicó V1). Para empezar limpio otra vez: `docker compose down -v && docker compose up -d`.

---

## Escenario 3: Prod real (proyecto terminado)

Aquí todo corre en Docker en el servidor: MySQL y backend en la misma red interna.

### Lista de verificación antes de desplegar

- [ ] Escenario 2 probado de principio a fin sin errores.
- [ ] `SecurityConfig` escrito (hoy está vacío: toda la API pediría el usuario temporal).
- [ ] Las entidades coinciden con las tablas de `V1__...sql`. Con `ddl-auto: validate`, si no coinciden la app no arranca y el error dice qué falta. Puntos que suelen fallar con tu esquema: nombres de columna en camelCase (`stockCritico` frente a `stock_critico`), `DECIMAL` frente a `Long`/`Integer`, y `id` sin `AUTO_INCREMENT` (si usas `GenerationType.IDENTITY`, el script debe declarar `AUTO_INCREMENT`).
- [ ] `.env` del servidor con claves **nuevas y generadas**, distintas a desarrollo.
- [ ] Permisos del `.env`: `chmod 600 .env`.
- [ ] `Dockerfile` escrito (hoy está vacío).
- [ ] Servicio `backend` activado en el compose (hoy está comentado).
- [ ] Puerto `3306` de MySQL **sin publicar**.

### 1. `Dockerfile` (en la raíz)

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Agrega también un `.dockerignore` para que el `.env` y la basura no entren a la imagen:

```
.env
.git
target
*.md
```

### 2. `docker-compose.yml` para producción

Dos cambios respecto al actual: se activa `backend` (con los dos usuarios correctos) y se quita el puerto `3306` de `db`.

```yaml
services:
  db:
    image: mysql:8.4
    container_name: tecnofactory-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: tecnofactory
      MYSQL_USER: tecnofactory_migrator
      MYSQL_PASSWORD: ${MIGRATOR_PASSWORD}
      APP_DB_PASSWORD: ${APP_DB_PASSWORD}
      TZ: America/Santiago
    # SIN "ports": solo el backend llega a MySQL, por la red interna de Docker
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql-init:/docker-entrypoint-initdb.d:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${DB_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build: .
    container_name: tecnofactory-backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/tecnofactory
      DB_USER: tecnofactory_app
      DB_PASSWORD: ${APP_DB_PASSWORD}
      FLYWAY_USER: tecnofactory_migrator
      FLYWAY_PASSWORD: ${MIGRATOR_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}

volumes:
  mysql_data:
```

Puntos importantes:

- `DB_URL` usa `db` (el nombre del servicio), no `localhost`: dentro de Docker `localhost` sería el propio contenedor del backend.
- Para seguir administrando MySQL desde tu máquina en desarrollo, usa un `docker-compose.override.yml` con el `ports: "3306:3306"` solo en tu equipo (Compose lo combina solo).
- En el servidor, el `.env` lo lee Compose y rellena `${...}`; el contenedor recibe solo lo que aparece en `environment:`.

### 3. Primer despliegue

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

El orden es automático: MySQL arranca, crea los usuarios (primer arranque), pasa a `healthy`, arranca el backend y Flyway crea las tablas.

### 4. Comprobar

Igual que el [paso 8 del Escenario 2](#8-verificar-el-comportamiento-de-prod).

### 5. Recomendaciones de servidor

- Pon **Nginx o Caddy con HTTPS** delante del backend y publica solo los puertos 80/443. En ese caso el backend puede quedarse sin `ports:` y que el proxy lo alcance por la red de Docker.
- Haz respaldos periódicos y guárdalos **fuera** del servidor:

  ```bash
  docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql
  ```

- Las imágenes de productos subidas deben guardarse en un **volumen**, o se perderán al recrear el contenedor.
- Restringe CORS (`CorsConfig`) al dominio real del frontend.
- Cuando agregues Redis o un SMTP real: cambia `spring.cache.type` a `redis` en `application-prod.yaml` y vuelve a activar su health check.

### 6. Actualizar una versión ya desplegada

```bash
git pull
docker compose up -d --build backend
```

Flyway aplica automáticamente las migraciones nuevas al arrancar. **No** uses `down -v` en producción: borra los datos.

---

## Agregar cambios a la base de datos

Una vez que `V1` se aplicó en alguna base, **no la edites nunca más**. Flyway guarda un checksum de cada migración y, si cambia, la app no arranca (`Validate failed: Migration checksum mismatch`).

Para cambiar el esquema, crea un archivo nuevo en `src/main/resources/db/migration/`:

```
V2__agregar_columna_x.sql
V3__crear_tabla_y.sql
```

Reglas del nombre: `V` + número + **dos guiones bajos** + descripción con guiones bajos + `.sql`.

Mientras `V1` solo exista en tu máquina de desarrollo, sí puedes editarla: haz `docker compose down -v` y vuelve a arrancar para recrearla desde cero. Cuando esté en producción, ya no.

---

## Errores comunes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Could not resolve placeholder 'FLYWAY_USER'` (o `DB_URL`, etc.) | Falta una variable de entorno | Define la que indica el mensaje. Recuerda que Spring no lee el `.env` |
| `Access denied for user 'tecnofactory_app'` | El usuario no existe (el volumen se creó antes de que existiera el script) o la clave no coincide | `docker compose down -v && docker compose up -d` (borra datos) y verifica con `SELECT user, host FROM mysql.user` |
| `Access denied for user 'tecnofactory_migrator'` | `FLYWAY_PASSWORD` distinta de `MIGRATOR_PASSWORD` en el momento de crear el volumen | Igual que arriba, o cambia la clave dentro de MySQL con `ALTER USER` |
| `Access denied for user 'root'` | `DB_PASSWORD` no coincide con la guardada en el volumen | Usa la clave original o recrea el volumen |
| `Found non-empty schema(s) "tecnofactory" but no schema history table` | La base ya tenía tablas (por ejemplo, creadas por Hibernate con `ddl-auto: update` en el flujo anterior) | `docker compose down -v` y arrancar de nuevo |
| `Validate failed: Migration checksum mismatch` | Editaste una migración ya aplicada | Crea `V2__...`, o `down -v` si es solo desarrollo |
| `Schema-validation: missing table` / `wrong column type` | Las entidades no coinciden con las tablas | Ajusta la entidad o crea una migración nueva |
| `Communications link failure` / `Connection refused` | MySQL no está corriendo o aún no está `healthy` | `docker compose up -d` y esperar |
| `Port 8080 was already in use` | Otra instancia (IntelliJ o el contenedor) usa el puerto | Detén la otra |
| `The "Dockerfile" is empty` / `dockerfile parse error` | `docker compose up` con el servicio `backend` activo y el `Dockerfile` vacío | Escribe el `Dockerfile` o deja el bloque comentado |
| Swagger y Actuator piden usuario y clave | `SecurityConfig` vacío | Usa `user` y la clave del log, o escribe el `SecurityConfig` |
| `cannot read distributionUrl property` | `.mvn/wrapper/maven-wrapper.properties` incompleto | Recrearlo con `wrapperVersion`, `distributionType` y `distributionUrl` |

Para reiniciar la base desde cero (**borra todos los datos**):

```bash
docker compose down -v
docker compose up -d
```

---

## Comandos de Docker de referencia

| Comando | Acción |
| --- | --- |
| `docker compose up -d` | Levanta los servicios en segundo plano |
| `docker compose up -d --build` | Levanta y reconstruye la imagen del backend |
| `docker compose ps` | Muestra el estado (`healthy` cuando está listo) |
| `docker compose logs -f db` | Logs de MySQL en vivo |
| `docker compose logs -f backend` | Logs del backend en vivo |
| `docker compose stop` | Detiene los contenedores y conserva los datos |
| `docker compose down` | Elimina los contenedores y conserva los datos |
| `docker compose down -v` | Elimina contenedores **y datos** |
| `docker exec -it tecnofactory-db mysql -uroot -p tecnofactory` | Consola de MySQL como root |
| `docker exec -it tecnofactory-db mysql -utecnofactory_app -p tecnofactory` | Consola como el usuario de la app |
