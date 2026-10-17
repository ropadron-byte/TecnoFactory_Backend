# 09. Errores comunes

Tabla única de síntomas, causas y soluciones. Los primeros nueve ya ocurrieron en el proyecto.

## Ya ocurridos

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `'url' must start with "jdbc"` | `${DB_URL}` sin valor por defecto y variable no definida: Spring usó el texto literal | Define `DB_URL`; en local hay valor por defecto |
| `Access denied for user '${DB_USER}'` | Misma causa, con el usuario | Define `DB_USER` |
| `No se ha encontrado la orden «mysql»` / `SCRIPT: no se encontró la orden` | Se pegó en la terminal el **contenido** del script, no el comando que lo crea | Crear el archivo con el editor o con `cat > archivo <<'EOF'` completo |
| MySQL no arranca | Faltaba `MYSQL_ROOT_PASSWORD` en el compose | Agregar `MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}` |
| `Migration checksum mismatch` | Se editó una migración ya aplicada (ocurrió con el `V1` vacío) | Desarrollo: `docker compose down -v`. Producción: crear una versión nueva |
| El script de usuarios no corre / falta `tecnofactory_app` | El volumen ya existía | `docker compose down -v` y `up -d` |
| Versiones de Maven incompatibles / error de Guice | Se usó el Maven del IDE | Usar el wrapper `./mvnw` y, en IntelliJ, "Use Maven wrapper" |
| `Missing versions` de Testcontainers | Nombres de artefactos antiguos | `testcontainers-junit-jupiter` y `testcontainers-mysql` |
| `ERROR 1045 Access denied` al entrar con `mysql -p` | La clave se tecleó mal: `-p` no muestra lo que escribes | Repetir con cuidado |

## Java e IntelliJ

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Class 'X' is public, should be declared in a file named 'X.java'` | Hay varias clases `public` en un mismo archivo | Una clase `public` por archivo, con el mismo nombre. Crea cada una con *New, Java Class* (o *Record*) |
| `'ErrorResponse(LocalDateTime, int, ...)' cannot be applied to '(java.time.LocalDateTime, ...)'` | En el archivo del `record` faltan los `import` (`java.time.LocalDateTime`, `java.util.Map`) y los tipos no se resuelven; o se importó otro `Map` | Agregar los imports correctos en `ErrorResponse.java` |
| Al importar `MethodArgumentNotValidException` IntelliJ ofrece dos clases | Hay una de `spring-web` y otra de `spring-messaging` | Elegir `org.springframework.web.bind.MethodArgumentNotValidException` |
| `HttpServletRequest` no se encuentra | Se importó `javax.servlet` | Usar `jakarta.servlet.http.HttpServletRequest` (Spring Boot 3 y 4) |
| `@Slf4j` no genera `log` | Lombok no procesa anotaciones | Activar *Annotation Processing* en IntelliJ y recompilar con `./mvnw clean compile` |

## Base de datos y Docker

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Access denied for user 'root'` | `DB_PASSWORD` no coincide con la guardada en el volumen | Usa la clave original o recrea el volumen |
| `Access denied for user 'tecnofactory_app'` | El usuario no existe, o la clave no coincide con la usada al crear el volumen | `docker exec ... SELECT user, host FROM mysql.user`; si falta, `down -v` y `up -d` |
| `Access denied for user 'tecnofactory_migrator'` | Igual, con `FLYWAY_PASSWORD` | Igual, o `ALTER USER` |
| `Communications link failure` / `Connection refused` | MySQL no corre o aún no está `healthy` | `docker compose up -d` y esperar |
| `Unknown database 'tecnofactory'` | `MYSQL_DATABASE` distinto o el volumen es de otro proyecto | Verifica el nombre; recrea el volumen si es desarrollo |
| `Port 8080 was already in use` | Otra instancia (IntelliJ o contenedor) usa el puerto | Detén la otra |
| `Bind for 0.0.0.0:3306 failed: port is already allocated` | Otro MySQL usa el 3306 | Detén el otro MySQL o cambia el puerto publicado |
| `The "Dockerfile" is empty` | Servicio `backend` activo con el `Dockerfile` vacío | Escribirlo (ver 07) o dejar el bloque comentado |

## Flyway y esquema

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Found non-empty schema(s) ... without schema history table` | La base tenía tablas antes que Flyway (Hibernate `update`, creación manual) | Desarrollo: `down -v`. Existente: `baseline` |
| `Detected failed migration to version X` | Una migración falló a medias (MySQL no deshace DDL) | Desarrollo: `down -v`. Producción: restaurar y reparar |
| `Key column 'x' doesn't exist in table` al agregar una FK | El `ALTER ... FOREIGN KEY` apunta a una columna que no está en el `CREATE TABLE` | Agregar la columna al `CREATE TABLE` además del `ALTER` |
| `Duplicate key name` / `constraint already exists` | Se agregó una FK con un nombre que ya existe | Editar la FK existente en vez de crear otra con el mismo nombre |
| `Cannot add or update a child row: a foreign key constraint fails` al crear un usuario | `V2` no se aplicó: `comuna` o `tipo_usuario` están vacías | Verificar `flyway_schema_history` (versiones 1 y 2) y los conteos de `V2` |
| `Schema-validation: missing table [x]` | La entidad apunta a una tabla que no existe o a un nombre distinto (mayúsculas, snake_case) | Ajustar `@Table` o la migración |
| `Schema-validation: wrong column type ... expected [x]; found [y]` | Tipos distintos (`DECIMAL` frente a `Long`) | Alinear tipo de la entidad y de la columna |
| `Schema-validation: missing column [stock_critico]` | La columna es `stockCritico` en la base | Renombrar en una migración |
| Los inserts fallan con `Field 'id' doesn't have a default value` | PK sin `AUTO_INCREMENT` con `GenerationType.IDENTITY` | Agregar `AUTO_INCREMENT` |
| Flyway no ejecuta nada y no hay error | Nombre del archivo incorrecto, carpeta equivocada o falta `spring-boot-starter-flyway` | Revisar `V1__...`, la ruta `db/migration` y el `pom.xml` |

## Spring y seguridad

| Síntoma | Causa | Solución |
| --- | --- | --- |
| Swagger o Actuator piden usuario y clave | `SecurityConfig` vacío | Usar `user` y la clave del log, o escribir el `SecurityConfig` |
| `Could not resolve placeholder 'FLYWAY_USER'` | Falta la variable en perfil `prod` | Definirla: Spring no lee el `.env` |
| Ningún perfil activo: la app intenta usar Redis | Se arrancó sin `SPRING_PROFILES_ACTIVE` | Arrancar siempre con `local` o `prod` |
| `show-sql` no imprime nada en local | `jpa:`/`hibernate:` mal indentados en `application-local.yaml` | Corregir como en 04 |
| `/actuator/health` sale DOWN | Un componente (Redis, correo) falla | Verificar que sus `health` estén desactivados en el perfil |
| Un `403` o `401` en cualquier ruta | La seguridad por defecto está activa | Escribir el `SecurityConfig` |
| CORS bloquea al frontend | `CorsConfig` vacío | Implementarlo con el dominio de React |

## Maven y compilación

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `cannot read distributionUrl property` | `.mvn/wrapper/maven-wrapper.properties` incompleto | Recrearlo con `wrapperVersion`, `distributionType` y `distributionUrl` |
| `./mvnw: No existe el archivo o el directorio` | Estás en otra carpeta | `cd` a la raíz del proyecto |
| `Unrecognised tag: 'dependency'` | Dependencias fuera de `<dependencies>` | Mover `</dependencies>` |
| El test `contextLoads` falla | Levanta toda la app y necesita MySQL | `-DskipTests` o Testcontainers con Docker |
| `javac: command not found` | Solo hay JRE | Instalar `openjdk-21-jdk` |

## Reiniciar la base desde cero (solo desarrollo)

```bash
docker compose down -v
docker compose up -d
docker compose ps        # esperar a "healthy"
```
