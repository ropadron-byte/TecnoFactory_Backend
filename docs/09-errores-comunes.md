# 09. Errores comunes

Tabla única de síntomas, causas y soluciones. Los primeros nueve ya ocurrieron en el proyecto, y la sección **Entidades y repositories** recoge los de la sesión del 18 oct. Para aprender **a razonar** un error de arranque (y no solo a buscarlo en esta tabla), lee [16-leer-errores-de-spring.md](16-leer-errores-de-spring.md).

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
| Un 500 sin explicación en la respuesta | El detalle no va al cliente, va al log | Mirar la consola o `docker compose logs`; ver [12](12-logs.md) |

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

## Entidades y repositories

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Schema-validation: missing column [id_comuna] in table [region]` | `@Table(name = "region")` copiado dentro de `Comuna`: dos entidades apuntan a la misma tabla | `@Table(name = "comuna")`. Ver [16, 5.1](16-leer-errores-de-spring.md#51-error-1-missing-column-id_comuna-in-table-region) |
| `Schema-validation: wrong column type ... found [char], expecting [varchar]` | `CHAR(1)` mapeado como `String` sin `@JdbcTypeCode` | `@JdbcTypeCode(SqlTypes.CHAR)`. Ver [13, 6.3](13-entidades-explicadas.md#63-explicación-de-jdbctypecodesqltypeschar) |
| `Error creating bean with name 'xRepository' ... No property 'comuna' found for type 'String'` | El método de repository usa el nombre de la **columna** (`findByNombreComuna`) en vez del **atributo** (`findByNombre`) | Usar los atributos de la entidad. Ver [14, sección 3.4](14-repositories-explicados.md#34-los-nombres-se-escriben-contra-los-atributos-java-no-las-columnas) |
| `No property 'correoo' found for type 'Usuario'` | Error de tipeo en el nombre de un método | Corregir el nombre |
| Falla al llamar `findById(...)`, pero el arranque fue limpio | El segundo tipo de `JpaRepository<E, X>` no coincide con el `@Id` (`Long` en vez de `Integer`) | Alinearlo con el tipo del `@Id` |
| `IncorrectResultSizeDataAccessException` | Un método devuelve `Optional<E>` pero la columna no es única y hay dos filas | Devolver `List<E>` |
| `LazyInitializationException` | Se leyó una relación `LAZY` (`comuna.getRegion()`) fuera de una transacción (`open-in-view: false`) | Cargar los datos dentro del service con `@Transactional`, o convertir a DTO dentro de él |
| Parece un error en el log pero es `INFO`: `Could not safely identify store assignment for repository candidate` | Spring prueba cada repository con JPA y con Redis | Ignorarlo ([14, sección 6](14-repositories-explicados.md#6-lo-que-sale-en-el-log-y-no-es-un-error)) |

## Spring y seguridad

| Síntoma | Causa | Solución |
| --- | --- | --- |
| Swagger o Actuator piden usuario y clave | `SecurityConfig` vacío o sin `SecurityFilterChain` | Escribir el `SecurityConfig` ([15](15-seguridad-codigo-explicado.md#33-la-cadena-de-filtros)) |
| `Could not resolve placeholder 'FLYWAY_USER'` | Falta la variable en perfil `prod` | Definirla: Spring no lee el `.env` |
| Ningún perfil activo: la app intenta usar Redis | Se arrancó sin `SPRING_PROFILES_ACTIVE` | Arrancar siempre con `local` o `prod` |
| `show-sql` no imprime nada en local | `jpa:`/`hibernate:` mal indentados en `application-local.yaml` | Corregir como en 04 |
| `/actuator/health` sale DOWN | Un componente (Redis, correo) falla | Verificar que sus `health` estén desactivados en el perfil |
| Un `403` o `401` en cualquier ruta | La seguridad por defecto está activa | Escribir el `SecurityConfig` |
| `403` solo en `POST`, `PUT` y `DELETE`; los `GET` funcionan | CSRF activo | `.csrf(AbstractHttpConfigurer::disable)` ([15, 3.4](15-seguridad-codigo-explicado.md#34-por-qué-se-desactiva-csrf)) |
| La app no arranca: `Can't configure requestMatchers after anyRequest` | Una regla `requestMatchers(...)` escrita después de `anyRequest()` | Dejar `anyRequest()` como la última regla |
| Sigue saliendo `Using generated security password` en el log | Normal hasta que exista un `UserDetailsService` propio | Ignorarlo en desarrollo ([15, 3.6](15-seguridad-codigo-explicado.md#36-dos-detalles-que-sorprenden)) |
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
