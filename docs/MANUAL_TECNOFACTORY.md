# TecnoFactory - Manual de lo construido

Este manual explica lo que dejamos funcionando, por qué se hizo así, qué consecuencias tiene a futuro, cómo repetirlo desde cero y cómo usarlo en el día a día.

## Índice

1. [Dónde quedó el proyecto](#1-dónde-quedó-el-proyecto)
2. [Mapa de archivos](#2-mapa-de-archivos)
3. [Los tres usuarios de MySQL](#3-los-tres-usuarios-de-mysql)
4. [Variables de entorno y claves](#4-variables-de-entorno-y-claves)
5. [Docker Compose y el script de inicio](#5-docker-compose-y-el-script-de-inicio)
6. [Perfiles de Spring (local y prod)](#6-perfiles-de-spring-local-y-prod)
7. [Flyway: cómo se usa y qué cuidar](#7-flyway-cómo-se-usa-y-qué-cuidar)
8. [Diseño de la base de datos](#8-diseño-de-la-base-de-datos)
9. [Uso diario: comandos](#9-uso-diario-comandos)
10. [Repetir todo desde cero (máquina nueva)](#10-repetir-todo-desde-cero-máquina-nueva)
11. [Llevarlo a producción](#11-llevarlo-a-producción)
12. [Implicaciones a futuro](#12-implicaciones-a-futuro)
13. [Errores que ya encontramos y cómo se resolvieron](#13-errores-que-ya-encontramos-y-cómo-se-resolvieron)
14. [Observaciones sobre el script exportado de Data Modeler](#14-observaciones-sobre-el-script-exportado-de-data-modeler)
15. [Pendientes](#15-pendientes)

---

## 1. Dónde quedó el proyecto

**Funciona hoy:**

| Pieza | Estado |
| --- | --- |
| MySQL 8.4 en Docker, con volumen persistente | Funcionando, `healthy` |
| Tres usuarios de base de datos con permisos distintos | Creados automáticamente al levantar Docker |
| `.env` con cuatro claves | Listo |
| Spring Boot en perfil `local` | Arranca y se conecta |
| Flyway | Se conecta con el usuario y aplica `V1` (hoy vacío) |
| Hibernate con `ddl-auto: validate` | Arranca sin errores |

**Falta:** las tablas en `V1`, las entidades, la seguridad con JWT, el `Dockerfile`, el backend dentro del compose y los ajustes finales de producción (sección 15).

La arquitectura de ejecución es esta:

```
Tu PC
 |
 |- Docker
 |    '- contenedor tecnofactory-db (MySQL 8.4)
 |         |- base: tecnofactory
 |         |- usuario root                  (administración manual)
 |         |- usuario tecnofactory_migrator (Flyway)
 |         '- usuario tecnofactory_app      (la API)
 |
 '- Spring Boot (IntelliJ o ./mvnw), perfil local
      '- se conecta a localhost:3306
```

---

## 2. Mapa de archivos

| Archivo | Qué hace | Quién lo lee | ¿Va a Git? |
| --- | --- | --- | --- |
| `.env` | Guarda las claves | Docker Compose | **No** |
| `docker-compose.yml` | Describe el contenedor de MySQL (y luego el backend) | Docker | Sí |
| `docker/mysql-init/01-crear-usuario-app.sh` | Crea el usuario limitado al iniciar MySQL por primera vez | MySQL, dentro del contenedor | Sí |
| `src/main/resources/application.yaml` | Configuración base, se carga siempre | Spring | Sí |
| `src/main/resources/application-local.yaml` | Ajustes de desarrollo | Spring, con perfil `local` | Sí |
| `src/main/resources/application-prod.yaml` | Ajustes de producción | Spring, con perfil `prod` | Sí |
| `src/main/resources/db/migration/V1__...sql` | Historial de cambios de la base | Flyway | Sí |
| `Dockerfile` | Construirá la imagen del backend (pendiente) | Docker | Sí |

Regla para recordar: **`.env` lo lee Docker Compose; Spring Boot no lo lee.** Por eso, al arrancar la app desde la terminal, cargamos el `.env` a mano (sección 9).

La carpeta `docker/` es solo para los archivos que los contenedores *usan* (scripts de inicio, y más adelante configuración de Nginx, por ejemplo). El `docker-compose.yml`, el `Dockerfile` y el `.env` se quedan en la raíz del proyecto, por convención y porque los comandos quedan más cortos.

---

## 3. Los tres usuarios de MySQL

### Qué son

En MySQL un usuario **no es dueño de tablas**: es solo una credencial con permisos. Las tablas viven en la base `tecnofactory`, y cada usuario puede hacer más o menos cosas sobre ella.

| Usuario | Quién lo usa | Permisos |
| --- | --- | --- |
| `root` | Tú, para administrar a mano | Todos, sobre todo el servidor |
| `tecnofactory_migrator` | Flyway | Todos, **solo sobre `tecnofactory`** (crear y modificar tablas) |
| `tecnofactory_app` | La API en su operación diaria | Solo `SELECT, INSERT, UPDATE, DELETE` sobre `tecnofactory` |

### Por qué se hizo así

Si la API usara `root` y alguien lograra explotarla (por ejemplo, con una inyección SQL), podría destruir todo el servidor. Con `tecnofactory_app` el daño máximo es sobre los datos de esa base: no puede borrar tablas, crear usuarios ni ver otras bases.

Flyway sí necesita crear y modificar tablas, por eso tiene su propio usuario con más permisos, que **solo se usa mientras se aplican las migraciones**.

### Cómo se crean

- `tecnofactory_migrator`: lo crea MySQL solo, gracias a las variables `MYSQL_USER` y `MYSQL_PASSWORD` del compose. Esa imagen le da todos los permisos sobre la base `MYSQL_DATABASE`.
- `tecnofactory_app`: lo crea el script `docker/mysql-init/01-crear-usuario-app.sh`, que MySQL ejecuta automáticamente al crear el volumen por primera vez.

### Qué implica

- **En `local` seguimos usando `root`** para no complicar el desarrollo (los valores por defecto del yaml). Eso significa que en local **no se prueban los permisos limitados**. Antes de desplegar conviene probar al menos una vez con los usuarios reales (sección 11).
- Las tablas que Flyway cree en el futuro **ya quedan accesibles** para `tecnofactory_app`, porque el permiso es sobre `tecnofactory.*`.
- Si cambias la clave de un usuario en el `.env` *después* de haber creado el volumen, MySQL **no se entera**: la clave quedó guardada en el volumen. Hay que cambiarla dentro de MySQL (`ALTER USER`) o recrear el volumen.

---

## 4. Variables de entorno y claves

### Las cuatro claves del `.env`

```env
DB_PASSWORD=...          # clave de root
MIGRATOR_PASSWORD=...    # clave de tecnofactory_migrator
APP_DB_PASSWORD=...      # clave de tecnofactory_app
JWT_SECRET=...           # firma los tokens de login (mínimo 32 caracteres)
```

Usa **claves distintas** para cada una.

### Cómo generarlas

```bash
openssl rand -hex 32
```

Entrega 64 caracteres aleatorios. Se usa `-hex` porque solo produce letras y números: los símbolos como `$`, `#`, `'` o `@` dan problemas en `.env`, en Docker Compose y en scripts.

### Quién lee cada variable

| Variable | La lee | Para qué |
| --- | --- | --- |
| `DB_PASSWORD` | Compose (crea MySQL) y Spring (en local, para entrar como root) | Clave de root |
| `MIGRATOR_PASSWORD` | Compose | Clave del usuario de Flyway |
| `APP_DB_PASSWORD` | Compose, y pasa al script de inicio | Clave del usuario de la API |
| `JWT_SECRET` | Spring | Firma de tokens |

### Reglas importantes

- `.env` **nunca** va a Git. Verifica con `git status` que no aparezca, y que `.gitignore` tenga la línea `.env`. Si ya estaba subido: `git rm --cached .env`.
- Protégelo: `chmod 600 .env`.
- **Las claves que se compartieron en chats, capturas o repositorios ya no son secretas.** Úsalas solo para desarrollo y genera otras para producción.
- Guarda una copia del `.env` de producción en un gestor de contraseñas: si lo pierdes, no hay forma de recuperarlo.

---

## 5. Docker Compose y el script de inicio

### El servicio `db`

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
    ports:
      - "3306:3306"
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

volumes:
  mysql_data:
```

| Línea | Qué hace |
| --- | --- |
| `db:` | Nombre del servicio. También es el **nombre de red**: dentro de Docker, otros contenedores lo encuentran como `db` (por eso la URL del backend dirá `jdbc:mysql://db:3306/...`) |
| `MYSQL_ROOT_PASSWORD` | Clave de root. **Obligatoria**: sin ella MySQL no arranca |
| `MYSQL_DATABASE` | Crea la base `tecnofactory` vacía |
| `MYSQL_USER` / `MYSQL_PASSWORD` | Crea el usuario `migrator` con todos los permisos sobre esa base |
| `APP_DB_PASSWORD` | Solo para que el script de inicio conozca la clave del usuario limitado |
| `ports: "3306:3306"` | Hace accesible MySQL desde tu PC (la app y Workbench se conectan por aquí). **En producción se elimina** |
| `mysql_data:/var/lib/mysql` | Guarda los datos en un volumen del disco, fuera del contenedor |
| `./docker/mysql-init:/docker-entrypoint-initdb.d:ro` | Conecta tu carpeta con la que MySQL revisa al arrancar la primera vez (`:ro` = solo lectura) |
| `healthcheck` | Docker comprueba cada 10 s si MySQL responde. El estado `healthy` indica que ya está listo |

### El script de inicio

Archivo `docker/mysql-init/01-crear-usuario-app.sh`:

```bash
#!/bin/bash
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE USER 'tecnofactory_app'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON tecnofactory.* TO 'tecnofactory_app'@'%';
FLUSH PRIVILEGES;
EOF
```

| Línea | Qué hace |
| --- | --- |
| `#!/bin/bash` | Indica que es un script de shell |
| `mysql -uroot -p"$MYSQL_ROOT_PASSWORD"` | Entra a MySQL como root, con la clave que el contenedor ya conoce |
| `CREATE USER ... IDENTIFIED BY` | Crea el usuario con la clave de `APP_DB_PASSWORD` |
| `GRANT SELECT, INSERT, UPDATE, DELETE ON tecnofactory.*` | Le da solo esos cuatro permisos sobre todas las tablas de la base |
| `FLUSH PRIVILEGES` | Aplica los cambios |

Necesita permiso de ejecución:

```bash
chmod +x docker/mysql-init/01-crear-usuario-app.sh
```

El `01-` del nombre solo define el orden si algún día hay más archivos: MySQL los ejecuta alfabéticamente.

### Qué implica

- **El script solo corre cuando el volumen se crea por primera vez.** En un volumen que ya existe no se ejecuta, aunque lo cambies.
- Si el `CREATE USER` falla (por ejemplo, porque el usuario ya existía), el contenedor de MySQL puede caerse al iniciar. Siempre se prueba con un volumen limpio (`down -v`).
- Ese script no es obligatorio: hace lo mismo que tres comandos SQL escritos a mano. Su valor es que un servidor nuevo queda listo con un solo `docker compose up -d`.

### El servicio `backend` (aún comentado)

Cuando existan el `Dockerfile`, el `V1` con tablas y el `SecurityConfig`, se activa este bloque, al mismo nivel que `db:`:

```yaml
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
```

`depends_on` con `service_healthy` hace que el backend espere a que MySQL esté `healthy`. La URL usa `db` y no `localhost`, porque dentro de Docker cada servicio se encuentra por su nombre.

Para que Flyway use su propio usuario, `application-prod.yaml` necesita (pendiente de agregar):

```yaml
spring:
  flyway:
    user: ${FLYWAY_USER}
    password: ${FLYWAY_PASSWORD}
```

Si no se definen, Flyway usa las credenciales del `datasource`, es decir, el usuario limitado, que no puede crear tablas.

### Datos y respaldos

El volumen `mysql_data` **no es un respaldo**: es la única copia de los datos. Se pierde con `docker compose down -v`, con `docker volume rm` o si falla el disco. Un respaldo es una copia independiente (sección 9).

---

## 6. Perfiles de Spring (local y prod)

### Cómo se elige

El perfil lo decides tú al arrancar, con la variable `SPRING_PROFILES_ACTIVE`:

- `application.yaml` se carga **siempre**.
- `application-local.yaml` se carga encima solo si el perfil es `local`.
- `application-prod.yaml` se carga encima solo si el perfil es `prod`.
- Lo que escribe el perfil reemplaza al base; lo que no escribe se mantiene.

En el log de arranque debe aparecer `The following 1 profile is active: "local"`.

### Valores por defecto (lo que corregimos hoy)

En `application-local.yaml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/tecnofactory}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD}
```

La sintaxis `${VARIABLE:valor}` significa "usa la variable si existe, y si no, este valor".

| Variable | En local | En prod |
| --- | --- | --- |
| `DB_URL` | Valor por defecto (localhost) | **Obligatoria, sin valor por defecto** |
| `DB_USER` | Por defecto `root` | **Obligatoria** |
| `DB_PASSWORD` | Obligatoria (viene del `.env`) | **Obligatoria** |
| `JWT_SECRET` | Obligatoria (viene del `.env`) | **Obligatoria** |

En producción no se ponen valores por defecto a propósito: prefieres que la app se detenga con un error claro antes que se conecte por accidente a `localhost` o con una clave de ejemplo.

### Qué hace cada perfil

| Aspecto | local | prod |
| --- | --- | --- |
| Hibernate | `ddl-auto: validate` (Flyway crea las tablas) | `validate` |
| Flyway | Activo | Activo |
| SQL en logs | Visible | Oculto |
| Swagger | Activo | Apagado |
| Actuator | Detalles, `health,info,metrics` | Solo `health`, sin detalles |
| Errores de la API | Con mensaje y detalle de validación | Sin mensaje ni rastro |
| Caché | `none` (sin Redis) | `none` por ahora |

### Detalle importante sobre variables sin definir

Si usas `${DB_URL}` sin valor por defecto y la variable no existe, Spring **no** se detiene con un mensaje claro: deja el texto literal `${DB_URL}` como si fuera el valor. Por eso apareció `'url' must start with "jdbc"` y `Access denied for user '${DB_USER}'`. Cuando veas un `${...}` dentro de un mensaje de error, es una variable no definida.

---

## 7. Flyway: cómo se usa y qué cuidar

### Cómo se usa

No hay ningún comando. Haces tres cosas:

1. Creas un archivo `V<número>__<descripcion>.sql` en `src/main/resources/db/migration`.
2. Escribes tu SQL dentro.
3. Arrancas la aplicación. Flyway aplica lo que falte y lo registra en la tabla `flyway_schema_history`.

En el log se ve:

```
Successfully applied 1 migration to schema `tecnofactory`
```

### El nombre del archivo

```
V1__crear_tablas_base.sql
│ │ │
│ │ └─ descripción
│ └─── dos guiones bajos (obligatorio)
└───── V mayúscula + número
```

Con un solo guion bajo o una `v` minúscula, Flyway ignora el archivo.

### Las versiones no son alternativas

Las versiones **no sirven para volver a probar una anterior**: son capas apiladas. La base final es lo que resulta de aplicar V1, luego V2, luego V3. Flyway gratuito no deshace migraciones: si una falla, se corrige hacia adelante.

| Momento | Qué haces |
| --- | --- |
| Mientras diseñas (hoy) | Un solo archivo `V1__crear_tablas_base.sql`. Lo editas y reinicias la base cada vez |
| Cuando necesites datos iniciales (roles, categorías, admin) | `V2__datos_iniciales.sql` |
| Cuando el esquema sea definitivo o lo use otra persona o un servidor | Congelas V1 y V2: ya no se editan |
| Desde ahí | Cada cambio es un archivo nuevo: `V3`, `V4`... |

### Lo que ocurrió hoy con el `V1` vacío

Flyway ya registró el `V1` vacío como **aplicado**, con una huella (checksum) de su contenido. Si ahora escribes tus tablas dentro de ese mismo archivo, Flyway verá que cambió y se detendrá con:

```
Validate failed: Migration checksum mismatch for migration version 1
```

En desarrollo se resuelve reiniciando la base antes de arrancar:

```bash
docker compose down -v
docker compose up -d
docker compose ps        # esperar a "healthy"
```

Eso es válido mientras sea tu base local de pruebas. En una base que ya tiene datos importantes, **nunca** se edita una migración aplicada.

### Reglas al escribir migraciones

| Regla | Por qué |
| --- | --- |
| Crea primero las tablas que otras referencian | Una FK a una tabla que aún no existe falla |
| Ponle nombre a tus restricciones (`fk_producto_categoria`) | Más fácil de borrar o modificar después |
| Mantén cada migración pequeña | MySQL **no deshace** cambios de estructura si algo falla a medias: lo ya ejecutado queda aplicado |
| Los tipos deben coincidir con las entidades | Con `validate`, si no calzan la app no arranca |
| Datos iniciales en una migración aparte | Separa la estructura estable de los datos |
| Contraseñas iniciales (admin) siempre ya hasheadas con BCrypt | Nunca texto plano en Git |
| No cambies tablas a mano desde Workbench o DBeaver | La base y los archivos se desincronizan |
| No mezcles Hibernate `update` con Flyway | Se pisan |
| Guarda los archivos en UTF-8 | Evita problemas con tildes y la ñ |

`validate` comprueba que tablas, columnas y tipos coincidan con las entidades. **No** comprueba índices, claves únicas ni claves foráneas: esas dependen solo de tu SQL.

Para ver el registro de Flyway en la base:

```sql
SELECT version, description, script, success FROM flyway_schema_history;
```

---

## 8. Diseño de la base de datos

### Herramienta

Se diseña en **SQL Developer Data Modeler**. No tiene MySQL como destino: ofrece Oracle, SQL Server y DB2. Se usa **SQL Server 2012** porque sus tipos (`BIGINT`, `VARCHAR`, `DATETIME`, `DECIMAL`) son los más cercanos a MySQL.

El script exportado **no se pega tal cual**: se adapta y el resultado va en `V1`.

| SQL Server exporta | MySQL necesita |
| --- | --- |
| `IDENTITY(1,1)` | `AUTO_INCREMENT` |
| Nombres entre corchetes `[producto]` | Sin corchetes |
| `VARCHAR(MAX)` | `TEXT` |
| `DATETIME2` | `DATETIME` |
| `BIT` | `BOOLEAN` |
| `GO`, `USE`, `CREATE DATABASE`, `DROP` | Eliminarlos |
| `CLUSTERED`, `WITH (ALLOW_PAGE_LOCKS ...)` | Eliminarlos |
| Falta motor y charset | Agregar `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` |

Alternativa con exportación directa a MySQL: **MySQL Workbench**. Instalación en Ubuntu:

```bash
sudo snap install mysql-workbench-community
sudo snap connect mysql-workbench-community:password-manager-service
```

Conexión a la base de Docker: host `127.0.0.1`, puerto `3306`, usuario `root`, clave `DB_PASSWORD`, esquema `tecnofactory`.

### Tipos de datos

| Dato | Tipo | En Java |
| --- | --- | --- |
| Clave primaria | `BIGINT NOT NULL AUTO_INCREMENT` (sin `UNSIGNED`) | `Long` |
| Clave foránea | Mismo tipo exacto que la clave a la que apunta | `Long` |
| Nombre, correo, slug | `VARCHAR(255)` | `String` |
| Descripción corta | `VARCHAR(500)` | `String` |
| Descripción larga | `TEXT` | `String` con `@Column(columnDefinition = "TEXT")` |
| URL de imagen | `VARCHAR(500)` (hasta 2048 si aceptas URLs externas con parámetros largos) | `String` |
| Precio en pesos chilenos (sin decimales) | `BIGINT` | `Long` |
| Precio con decimales | `DECIMAL(12,2)` | `BigDecimal` |
| Fecha y hora | `DATETIME` | `LocalDateTime` |
| Solo fecha (nacimiento) | `DATE` | `LocalDate` |
| Verdadero o falso | `BOOLEAN` | `Boolean` |

Reglas: nunca `FLOAT` ni `DOUBLE` para dinero. `TEXT` no admite `DEFAULT` ni índice completo, úsalo solo donde haga falta. No indexes columnas de URL.

### Nombres

- Todo en **minúsculas con guion bajo** (`detalle_boleta`). En Linux, MySQL distingue mayúsculas en nombres de tabla.
- Puedes repetir nombres de columna en tablas distintas (`id`, `nombre`): solo deben ser únicos dentro de la misma tabla.
- Clave primaria: `id`. Clave foránea: `<tabla_padre>_id`.
- Evita palabras reservadas: `order`, `group`, `key`, `index`, `rank`, `condition`.
- Los nombres de **restricciones** sí deben ser únicos en toda la base:

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Clave primaria | `pk_<tabla>` | `pk_producto` |
| Clave foránea | `fk_<tabla_hija>_<tabla_padre>` | `fk_producto_categoria` |
| Único | `uq_<tabla>_<columna>` | `uq_usuario_correo` |
| Índice | `idx_<tabla>_<columna>` | `idx_producto_nombre` |

En Data Modeler la relación tiene un nombre descriptivo (un verbo: "contiene", "realiza"), que es solo documentación. El nombre que llega a MySQL es el de la restricción FK, que debes renombrar si sale genérico (`Relation_1`).

### Relaciones

| Tipo | Cómo se resuelve |
| --- | --- |
| Uno a muchos | FK en la tabla "muchos" |
| Muchos a muchos | Tabla intermedia con dos FK y clave primaria compuesta (`usuario_rol`) |
| Muchos a muchos con datos propios | La tabla intermedia pasa a ser una entidad con sus columnas (`detalle_boleta`) |

Entidades: si la tabla intermedia solo tiene las dos FK, no necesita entidad (`@ManyToMany` con `@JoinTable`). Si tiene columnas extra, sí.

### Imágenes de producto

Una imagen pertenece a un solo producto, así que es **uno a muchos**:

```sql
CREATE TABLE producto_imagen (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    producto_id  BIGINT       NOT NULL,
    url          VARCHAR(500) NOT NULL,
    orden        INT          NOT NULL DEFAULT 0,
    es_principal BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_producto_imagen PRIMARY KEY (id),
    CONSTRAINT fk_producto_imagen_producto
        FOREIGN KEY (producto_id) REFERENCES producto (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

La URL no es una clave: la clave es el `id`. Una clave compuesta con una tabla `imagen` aparte solo se justifica si una misma imagen se **reutiliza** en varios productos.

### Carrito y boleta

```
usuario 1 ── 1 carrito 1 ──< detalle_carrito >── 1 producto
usuario 1 ──< boleta 1 ──< detalle_boleta
```

| Tabla | Columnas principales |
| --- | --- |
| `carrito` | `id`, `usuario_id` (`NOT NULL`, `UNIQUE`), `fecha_creacion` |
| `detalle_carrito` | `id`, `carrito_id`, `producto_id`, `cantidad`, `UNIQUE (carrito_id, producto_id)` |
| `boleta` | `id`, `usuario_id`, `fecha`, `total` |
| `detalle_boleta` | `id`, `boleta_id`, `producto_id`, `nombre_producto`, `precio_unitario`, `cantidad` |

Decisiones y razones:

- Cada usuario tiene **un solo carrito**, permanente. Al pagar se vacían sus detalles, no se crea otro.
- `detalle_carrito` **no guarda precio**: muestra el precio actual del producto.
- La boleta es un documento histórico y **no depende del carrito**. Se une al usuario con `usuario_id`, no con el `carrito_id` (el carrito es permanente, y su id sería el mismo en todas las boletas del usuario).
- `detalle_boleta` guarda una **copia** de nombre y precio: la boleta antigua conserva lo que se pagó aunque el producto cambie o desaparezca.
- Sin `estado` en la boleta, porque el pago es inmediato (simulado): si el pago falla, simplemente no se crea la boleta.

Flujo de pago, en **una sola transacción** (`@Transactional`):

1. Crear la `boleta` con `usuario_id` y el total.
2. Copiar cada `detalle_carrito` a `detalle_boleta`, con nombre y precio actuales.
3. Borrar los `detalle_carrito`.

Reglas de borrado:

| FK | Regla | Razón |
| --- | --- | --- |
| `carrito` -> `usuario` | `ON DELETE CASCADE` | Sin usuario, el carrito no sirve |
| `detalle_carrito` -> `carrito` | `ON DELETE CASCADE` | Vaciar o borrar el carrito borra sus detalles |
| `detalle_boleta` -> `boleta` | `ON DELETE CASCADE` | Los detalles pertenecen a la boleta |
| `boleta` -> `usuario` | Sin cascade (`RESTRICT`) | Un registro de compras no debe desaparecer con el usuario |
| `detalle_boleta` -> `producto` | Sin cascade | No borres productos vendidos; mejor desactívalos con una columna `activo` |

### Vistas de base de datos

Se evaluó y **no se usan por ahora**. La API se conecta con un solo usuario de MySQL, así que la base no sabe si quien pide los datos es un cliente o un administrador: eso lo decide Spring Security. Las vistas son sobre todo de lectura, y las escrituras seguirían yendo a las tablas. Sirven más adelante para reportes o consultas de lectura complejas. La protección real viene de los DTOs y del usuario `tecnofactory_app` con permisos mínimos.

---

## 9. Uso diario: comandos

### Arrancar el día

```bash
# 1. Levantar MySQL
docker compose up -d
docker compose ps                    # esperar a "healthy"

# 2. Cargar las claves y arrancar la app
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

| Línea | Qué hace |
| --- | --- |
| `set -a` | Desde aquí, toda variable que se defina se exporta automáticamente |
| `source .env` | Lee el `.env` y define sus variables en esta terminal |
| `set +a` | Apaga la exportación automática |

Las variables quedan solo en esa ventana de terminal. En una terminal nueva hay que repetirlo.

Alternativa en IntelliJ: *Run, Edit Configurations, Environment variables*, con `SPRING_PROFILES_ACTIVE=local;DB_PASSWORD=...;JWT_SECRET=...`, y arrancas con el botón de ejecutar.

### Terminar el día

```bash
# En la terminal de la app: Ctrl+C
docker compose stop                  # detiene MySQL, conserva los datos
```

### Comandos de Docker

| Comando | Acción |
| --- | --- |
| `docker compose up -d` | Levanta MySQL en segundo plano |
| `docker compose ps` | Estado (debe decir `healthy`) |
| `docker compose logs -f db` | Logs en vivo |
| `docker compose stop` | Detiene (conserva datos) |
| `docker compose down` | Elimina el contenedor (conserva datos) |
| `docker compose down -v` | Elimina contenedor **y datos** (cuidado) |

### Reiniciar la base desde cero (solo desarrollo)

```bash
docker compose down -v
docker compose up -d
docker compose ps
```

Esto borra todo, recrea el volumen, vuelve a crear los usuarios y la próxima vez que arranque la app Flyway aplicará todas las migraciones. Es lo que hay que hacer cada vez que edites un `V1` ya aplicado.

### Entrar a MySQL

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory
docker exec -it tecnofactory-db mysql -utecnofactory_app -p tecnofactory
```

Cada comando pide la clave del usuario correspondiente. Comprobación de permisos:

| Usuario | `CREATE TABLE prueba (id INT);` |
| --- | --- |
| `tecnofactory_app` | Debe fallar con `CREATE command denied` |
| `tecnofactory_migrator` | Debe funcionar (y luego `DROP TABLE prueba;`) |

### Ver el registro de Flyway

```sql
SELECT version, description, script, success FROM flyway_schema_history;
```

### Respaldos

```bash
# Crear una copia
docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql

# Restaurarla
docker exec -i tecnofactory-db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' < respaldo.sql
```

Guarda el archivo **fuera de ese disco o servidor** (pendrive, otro disco, nube). El volumen no es un respaldo.

### Generar claves

```bash
openssl rand -hex 32
```

---

## 10. Repetir todo desde cero (máquina nueva)

1. **Instalar** JDK 21 (completo), Docker y Docker Compose. Maven no hace falta: se usa `./mvnw`.
2. **Clonar** el repositorio y entrar a la carpeta del proyecto.
3. **Crear el `.env`** con claves nuevas:

   ```bash
   echo "DB_PASSWORD=$(openssl rand -hex 32)" > .env
   echo "MIGRATOR_PASSWORD=$(openssl rand -hex 32)" >> .env
   echo "APP_DB_PASSWORD=$(openssl rand -hex 32)" >> .env
   echo "JWT_SECRET=$(openssl rand -hex 32)" >> .env
   chmod 600 .env
   ```

4. **Comprobar** que el script de inicio tiene permiso de ejecución:

   ```bash
   chmod +x docker/mysql-init/01-crear-usuario-app.sh
   ```

5. **Levantar MySQL** y esperar a `healthy`:

   ```bash
   docker compose up -d
   docker compose ps
   ```

6. **Verificar** que el script se ejecutó y que el usuario limitado existe:

   ```bash
   docker compose logs db | grep -i "crear-usuario"
   docker exec -it tecnofactory-db mysql -utecnofactory_app -p tecnofactory
   ```

7. **Arrancar la app** como en la sección 9. En el log deben aparecer:
   - `The following 1 profile is active: "local"`
   - `Successfully applied N migration(s)`
   - `Started TecnoFactoryApplication`

Para instalar Docker en Ubuntu, usa los comandos del `README.md` del proyecto.

---

## 11. Llevarlo a producción

### Qué debe estar en el repositorio

```
docker-compose.yml
Dockerfile
docker/mysql-init/01-crear-usuario-app.sh      (con permiso de ejecución)
src/main/resources/db/migration/V1__...sql     (con las tablas)
```

Para que el script conserve su permiso en Git, hazlo una vez:

```bash
chmod +x docker/mysql-init/01-crear-usuario-app.sh
git add docker/mysql-init/01-crear-usuario-app.sh
git commit -m "Script de creación del usuario de la app"
```

En producción, en el compose: **elimina** la línea `"3306:3306"` del servicio `db` (el backend alcanza MySQL por la red interna de Docker) y activa el bloque `backend`.

### Pasos en el servidor

1. Instalar Docker y clonar el proyecto.
2. Crear el `.env` con **claves nuevas** (comandos de la sección 10, paso 3), y guardar una copia en un lugar seguro.
3. Levantar todo:

   ```bash
   docker compose up -d --build
   ```

   En orden: construye la imagen del backend, MySQL crea la base y el usuario migrator, el script crea el usuario de la app, pasa el healthcheck, el backend arranca, Flyway aplica las migraciones y la API empieza a trabajar con `tecnofactory_app`.

4. Comprobar:

   ```bash
   docker compose ps
   docker compose logs -f backend
   curl http://localhost:8080/actuator/health
   ```

   Debe responder `{"status":"UP"}`.

### Actualizar después

```bash
git pull
docker compose up -d --build backend
```

Reconstruye solo el backend; Flyway aplica automáticamente las versiones nuevas. **Nunca** `down -v` en producción.

### Probar los usuarios reales antes de desplegar

En tu PC, para comprobar que los permisos limitados alcanzan (una vez que `application-prod.yaml` tenga las variables de Flyway):

```bash
set -a; source .env; set +a
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=tecnofactory_app DB_PASSWORD=$APP_DB_PASSWORD \
FLYWAY_USER=tecnofactory_migrator FLYWAY_PASSWORD=$MIGRATOR_PASSWORD \
./mvnw spring-boot:run
```

### Otras medidas

- Firewall: dejar abierto solo lo necesario (SSH y el puerto de la API o HTTPS).
- Más adelante, HTTPS con un proxy (Nginx o Caddy) delante del backend.
- Programar respaldos con `mysqldump` antes de tener datos reales.
- Las imágenes de productos que se suban al servidor necesitan su propio volumen, o se perderán al recrear el contenedor del backend.

---

## 12. Implicaciones a futuro

| Qué hicimos | Consecuencia |
| --- | --- |
| Script de inicio en `initdb.d` | Solo corre al crear el volumen. Cambiarlo no afecta a una base ya creada |
| Claves en `.env` | Cambiarlas después de crear el volumen no cambia las de MySQL (`ALTER USER` o recrear el volumen) |
| Tres usuarios | Los permisos nuevos que necesite el usuario de la app deben otorgarse a mano (`GRANT`) |
| Flyway con checksum | Una migración aplicada no se edita; en producción todo cambio es una versión nueva |
| `ddl-auto: validate` | Cada cambio de entidad exige un cambio de SQL equivalente, y viceversa |
| `local` usa `root` | Los permisos limitados no se prueban en local; probar antes de desplegar |
| `V1` vacío aplicado | Antes de escribir las tablas hay que reiniciar la base (`down -v`) |
| Puerto 3306 publicado | Cómodo para desarrollo; en producción se elimina |
| MySQL no deshace DDL | Migraciones pequeñas; una que falla a medias deja la base parcialmente cambiada |
| Claves compartidas en chats | Ya no son secretas: usarlas solo en desarrollo |
| Nombres de tabla en minúsculas | En Linux las mayúsculas importan; un nombre mal escrito falla en producción |
| Spring no lee el `.env` | Siempre hay que cargarlo al arrancar desde la terminal, o configurar IntelliJ |

---

## 13. Errores que ya encontramos y cómo se resolvieron

| Error | Causa | Solución |
| --- | --- | --- |
| `'url' must start with "jdbc"` | `${DB_URL}` sin valor por defecto y variable no definida: Spring usó el texto literal | Valor por defecto: `${DB_URL:jdbc:mysql://localhost:3306/tecnofactory}` |
| `Access denied for user '${DB_USER}'` | Misma causa, con el usuario | `${DB_USER:root}` |
| `No se ha encontrado la orden «mysql»` y `SCRIPT: no se encontró la orden` | Se pegó en la terminal el **contenido** del script, no el comando que lo crea | Crear el archivo con `cat > archivo <<'SCRIPT'` completo, o desde el editor |
| MySQL no arranca | Faltaba `MYSQL_ROOT_PASSWORD` en el compose | Agregar `MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}` |
| `Migration checksum mismatch` | Se editó una migración ya aplicada | En desarrollo, `down -v`; en producción, una versión nueva |
| `Access denied for user 'root'` | `DB_PASSWORD` distinta a la de MySQL | Usar la misma clave del `.env`; si se cambió después de crear el volumen, recrearlo |
| El script de usuarios no corre | El volumen ya existía | `docker compose down -v` y `up -d` |
| Versiones de Maven incompatibles | El Maven del IDE no coincide con el wrapper | Usar el wrapper (`./mvnw`) y configurar IntelliJ con "Use Maven wrapper" |
| `Missing versions` de Testcontainers | Nombres de artefactos antiguos | `testcontainers-junit-jupiter` y `testcontainers-mysql` |

---

## 14. Observaciones sobre el script exportado de Data Modeler

Estas observaciones son del primer script exportado y conviene resolverlas antes de pegarlo en `V1`:

| Observación | Qué hacer |
| --- | --- |
| Tablas y columnas en mayúsculas (`BLOG`, `USUARIO`) | Pasar todo a minúsculas |
| Columnas camelCase (`stockCritico`) | `stock_critico` |
| Varios `id` sin `AUTO_INCREMENT` | Agregarlo a todas las claves primarias numéricas |
| `BLOG.id_blog` es `VARCHAR(255)` como clave primaria | Usar `BIGINT AUTO_INCREMENT` |
| `DETALLE_BOLETA.id_detalle_boleta` permite `NULL` y no tiene clave primaria | `BIGINT NOT NULL AUTO_INCREMENT` más `PRIMARY KEY` |
| `DETALLE_CARRITO` y `PRODUCTO_CATEGORIA` sin clave primaria | Clave compuesta (`carrito_id, producto_id`) y (`producto_id, categoria_id`) |
| `NUMERIC(n)` en cantidades, descuentos, ids de comuna y región | `INT` (o `SMALLINT`/`TINYINT UNSIGNED` según el rango); el descuento como entero 0 a 100 |
| `NUMERIC(9)` para el RUN y `NUMERIC(11)` para el teléfono | RUN como `INT` (cabe hasta 99.999.999) y teléfono como `VARCHAR(15)`: puede llevar `+` y un número no se opera |
| `fecha_nacimiento DATETIME` | `DATE` |
| `BOLETA.estado` y `cantidad_productos` | La decisión de diseño fue sin estado; `cantidad_productos` se calcula desde los detalles |
| `IMAGEN_URL` con `producto_id` y `categoria_id` ambos opcionales | Decidir si una imagen es de producto o de categoría; evitar filas con ambos o ninguno |
| `DETALLE_BOLETA` guarda `descripcion` de 1000 caracteres | Basta con `nombre_producto` y `precio_unitario` como copia congelada |
| Nombres de FK genéricos (`BLOG_USUARIO_FK`) | Renombrar a la convención `fk_<hija>_<padre>` |
| `correo` y `run` sin restricción de unicidad | `UNIQUE` en `correo` y en el RUN |
| Falta `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | Agregarlo en cada tabla |

La columna de contraseña (`contrasenia`) guarda el **hash BCrypt**, no el texto: reserva `VARCHAR(255)`.

---

## 15. Pendientes

Orden sugerido:

1. **Escribir `V1__crear_tablas_base.sql`** con el diseño adaptado a MySQL y revisarlo (sección 14).
2. Reiniciar la base (`down -v`, `up -d`) y arrancar la app: Flyway debe aplicar `V1` y Hibernate no debe quejarse.
3. **`V2__datos_iniciales.sql`**: roles, categorías base y el administrador (con la contraseña ya hasheada con BCrypt).
4. **Entidades** que coincidan con las tablas, **DTOs**, **mappers** (MapStruct), **repositories**, **services** y **controllers**.
5. **Seguridad**: `SecurityConfig`, `JwtService`, `JwtAuthFilter` y `UserDetailsServiceImpl`. Hoy, con la clase vacía, Spring protege todo con un usuario `user` y una contraseña generada que aparece en el log.
6. **`GlobalExceptionHandler`** y excepciones propias.
7. **Subida de imágenes** y un volumen para la carpeta de archivos.
8. **`Dockerfile`** del backend y activación del bloque `backend` en el compose.
9. **`application-prod.yaml`**: agregar `flyway.user` y `flyway.password`.
10. **Pago simulado** y flujo de boleta transaccional.
11. Más adelante: Redis y caché, correo con un SMTP real, HTTPS, respaldos programados.

Opcional: renombrar el paquete `com.factory.TecnoFactory` a minúsculas (`com.factory.tecnofactory`) para seguir la convención de Java.
