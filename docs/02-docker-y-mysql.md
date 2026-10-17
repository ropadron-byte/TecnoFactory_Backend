# 02. Docker y MySQL

## El servicio `db`

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
```

| Línea | Qué hace |
| --- | --- |
| `image: mysql:8.4` | Versión LTS de MySQL |
| `restart: unless-stopped` | Se reinicia solo tras una caída o un reinicio del equipo |
| `MYSQL_ROOT_PASSWORD` | Clave de `root`. **Sin ella MySQL no arranca** |
| `MYSQL_DATABASE` | Crea la base `tecnofactory` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | Crea `tecnofactory_migrator` con `ALL` sobre esa base |
| `APP_DB_PASSWORD` | No es una variable de MySQL: se pasa al contenedor para que la lea el script de inicio |
| `TZ` | Zona horaria del servidor |
| `ports: "3306:3306"` | Publica MySQL en tu máquina. **Se quita en producción** |
| `mysql_data:/var/lib/mysql` | Volumen con los datos: sobreviven a `down` y a recrear el contenedor |
| `docker-entrypoint-initdb.d` | Carpeta de scripts que MySQL ejecuta **solo al crear el volumen** |
| `utf8mb4` | Codificación completa (tildes, ñ, emojis) |
| `healthcheck` | Marca el servicio `healthy` cuando MySQL responde; el backend espera esa señal |

## Los tres usuarios de MySQL

En MySQL un usuario **no es dueño de tablas**: es una credencial con permisos. Las tablas viven en la base `tecnofactory`.

| Usuario | Quién lo usa | Permisos | Cómo se crea |
| --- | --- | --- | --- |
| `root` | Tú, para administrar. En local también la app | Todo el servidor | `MYSQL_ROOT_PASSWORD` |
| `tecnofactory_migrator` | **Flyway** | `ALL` solo sobre `tecnofactory` (DDL: crear/alterar/borrar tablas) | `MYSQL_USER` / `MYSQL_PASSWORD` |
| `tecnofactory_app` | **La API** en ejecución | `SELECT, INSERT, UPDATE, DELETE` solo sobre `tecnofactory` | Script `01-crear-usuario-app.sh` |

**Por qué dos usuarios en producción:** si alguien explota una inyección SQL, entra como `tecnofactory_app`, que no puede hacer `DROP TABLE`, crear usuarios ni ver otras bases. El usuario con poder de DDL solo se usa mientras Flyway aplica migraciones.

**Verificado el 16 oct 2026** (prueba de prod): `tecnofactory_app` pudo ejecutar `SELECT` y `DROP TABLE BLOG` fue rechazado con `ERROR 1142 ... DROP command denied`.

## El script de inicio

`docker/mysql-init/01-crear-usuario-app.sh`:

```bash
#!/bin/bash
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE USER 'tecnofactory_app'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON tecnofactory.* TO 'tecnofactory_app'@'%';
FLUSH PRIVILEGES;
EOF
```

| Detalle | Explicación |
| --- | --- |
| Se ejecuta una sola vez | Solo cuando `/var/lib/mysql` está vacío (volumen nuevo) |
| Orden | Los scripts se ejecutan por orden alfabético; el `01-` va primero |
| `@'%'` | El usuario puede conectarse desde cualquier host (el backend vive en otro contenedor) |
| Permiso sobre `tecnofactory.*` | Las tablas que Flyway cree en el futuro **ya quedan accesibles**; no hay que repetir el `GRANT` |
| Fragilidad | La clave se inserta dentro de comillas simples de SQL: **no uses `'` ni `$` en `APP_DB_PASSWORD`** |

## Volúmenes: qué conservan y qué no

| Comando | Contenedor | Datos (volumen) |
| --- | --- | --- |
| `docker compose stop` | Detenido | Se conservan |
| `docker compose down` | Eliminado | Se conservan |
| `docker compose down -v` | Eliminado | **Se borran** |
| Si cae el contenedor del backend | La base sigue funcionando (son contenedores independientes) | Intactos |

**El volumen no es un respaldo.** Protege contra reiniciar contenedores, no contra un disco dañado, un `down -v` accidental o una migración con error. Los respaldos se hacen con `mysqldump` y se guardan **fuera del servidor**:

```bash
# Crear una copia
docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql

# Restaurarla
docker exec -i tecnofactory-db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' < respaldo.sql
```

## El servicio `backend`

Hoy está **comentado** en el compose y el `Dockerfile` está vacío. La versión correcta está en [07-ejecucion-local-y-prod.md](07-ejecucion-local-y-prod.md#escenario-3-prod-real-proyecto-terminado). Resumen de cómo se conecta:

| Variable del contenedor | Valor | Motivo |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://db:3306/tecnofactory` | `db` es el nombre del servicio; `localhost` sería el propio contenedor |
| `DB_USER` / `DB_PASSWORD` | `tecnofactory_app` / `${APP_DB_PASSWORD}` | La API usa el usuario limitado |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | `tecnofactory_migrator` / `${MIGRATOR_PASSWORD}` | Flyway necesita DDL |
| `depends_on: service_healthy` | — | El backend espera a que MySQL esté listo |

## Implicaciones a futuro

- Cambiar una clave en el `.env` **después** de crear el volumen no cambia la de MySQL. Opciones: `ALTER USER 'usuario'@'%' IDENTIFIED BY 'nueva';` o recrear el volumen (solo en desarrollo).
- Si la app necesita un permiso nuevo (por ejemplo `EXECUTE` para un procedimiento), se otorga a mano con `GRANT`.
- Para subir de versión mayor de MySQL, se prueba antes con una copia de los datos.
