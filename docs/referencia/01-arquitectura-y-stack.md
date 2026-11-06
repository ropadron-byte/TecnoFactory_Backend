# 01. Arquitectura y stack

## Qué es

**TecnoFactory** es el backend de un ecommerce: API REST con usuarios y roles, catálogo por categorías, carrito, boletas con pago simulado, blog e imágenes. Está pensado para un frontend en React y para desplegarse con Docker.

## Stack

| Área | Tecnología | Para qué |
| --- | --- | --- |
| Lenguaje | Java 21 (LTS) | Versión soportada por todas las librerías |
| Framework | Spring Boot 4.1.1 | Web MVC, JPA, Validation |
| Seguridad | Spring Security + JWT (jjwt) | Login sin sesiones en el servidor |
| Base de datos | MySQL 8.4 | Datos persistentes |
| Migraciones | Flyway (`spring-boot-starter-flyway` + `flyway-mysql`) | Versionar el esquema en Git |
| Contenedores | Docker y Docker Compose | MySQL (y luego el backend) |
| Documentación de API | SpringDoc OpenAPI (Swagger UI) | Probar endpoints desde el navegador |
| Utilidades | Lombok, MapStruct | Menos código repetitivo |
| Monitoreo | Spring Boot Actuator | `/actuator/health` |
| Extras (aún sin usar) | Spring Mail, Spring Cache + Redis | Correos y caché |
| Pruebas | Spring Boot Test, Testcontainers | Probar contra una MySQL real |
| Build | Maven con wrapper (`./mvnw`) | Compilar igual en cualquier equipo |

> En Spring Boot 4 las integraciones se activan con *starters* propios. Por eso Flyway necesita `spring-boot-starter-flyway`: sin él, las migraciones **no se ejecutan** aunque `flyway-core` esté en el classpath.

## Arquitectura en capas (Package by Layer)

```
Cliente (React)
      |  JSON
      v
 Controller   ->  recibe la petición HTTP y responde
      |
      v
  Service     ->  lógica de negocio (stock, totales, validaciones)
      |
      v
 Repository   ->  acceso a datos (Spring Data JPA)
      |
      v
   MySQL
```

Los datos viajan por la API como **DTOs**; las entidades nunca se exponen. Un DTO evita filtrar la contraseña y desacopla la API de la base.

## Estructura del proyecto

```
TecnoFactory/
├── docker-compose.yml
├── .env                          # secretos (NO va a Git)
├── Dockerfile                    # vacío por ahora
├── pom.xml
├── docker/
│   └── mysql-init/
│       └── 01-crear-usuario-app.sh
└── src/
    ├── main/
    │   ├── java/com/factory/tecnofactory/
    │   │   ├── TecnoFactoryApplication.java
    │   │   ├── config/        # AdministradorInicialRunner, CorsConfig, OpenApiConfig
    │   │   ├── controller/    # endpoints REST
    │   │   ├── dto/           # entrada y salida de la API
    │   │   ├── entity/        # modelo de datos (tablas)
    │   │   ├── exception/     # GlobalExceptionHandler, ErrorResponse y excepciones propias
    │   │   ├── mapper/        # entidad <-> DTO (MapStruct)
    │   │   ├── repository/    # interfaces JpaRepository
    │   │   ├── security/      # JwtService, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl, ManejadorErroresSeguridad
    │   │   ├── service/       # lógica de negocio (UsuarioService, AuthService)
    │   │   └── validation/    # validación personalizada del RUN (RunUtil, @RunValido)
    │   └── resources/
    │       ├── application.yaml
    │       ├── application-local.yaml
    │       ├── application-prod.yaml
    │       └── db/migration/
    │           ├── V1__esquema_inicial.sql   # 13 tablas
    │           ├── V2__datos_iniciales.sql   # roles, regiones y comunas
    │           ├── V3__categorias_base.sql   # categorías base
    │           ├── V4__blog_fecha_creacion.sql
    │           ├── V5__imagen_portada_y_galeria.sql
    │           ├── V6__contacto.sql
    │           └── V7__unicidad_region_comuna.sql
    └── test/
```

Estado actual:

| Paquete | Estado |
| --- | --- |
| `exception` | **Escrito**: `RecursoNoEncontradoException`, `ReglaNegocioException`, `ErrorResponse` y `GlobalExceptionHandler` (ver [Cambios recientes](../estado/cambios-recientes.md)) |
| `security` | **Escrito** (20 oct): `JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl`, `SecurityConfig` con reglas por rol y `ManejadorErroresSeguridad`. Ver [21](21-seguridad-jwt-y-roles-explicada.md) |
| `config` | `AdministradorInicialRunner`, `CorsConfig` y `OpenApiConfig` escritas (`WebConfig` se borró por estar vacía) |
| `entity`, `repository` | 4 de 12 entidades y sus 4 repositories |
| `dto`, `mapper`, `validation` | `Usuario` completo (Request, Response, mapper, validación del RUN), `LoginRequest`, `AuthResponse` y `PageResponse` |
| `service`, `controller` | `Usuario` y login (`UsuarioService`, `AuthService`, `UsuarioController`, `AuthController`) |

> En Java, cada clase `public` va en **su propio archivo** con el mismo nombre (`ErrorResponse.java` contiene solo `ErrorResponse`).

### Responsabilidad de cada paquete

| Paquete | Responsabilidad |
| --- | --- |
| `entity` | Clases que representan las tablas |
| `repository` | Consultas a la base; aísla el acceso a datos |
| `dto` | Datos que entran y salen de la API |
| `mapper` | Convierte entidad <-> DTO |
| `service` | Reglas de negocio; los controllers no llevan lógica |
| `controller` | Endpoints REST: recibe, delega y responde |
| `security` | JWT y autorización en un solo lugar |
| `validation` | Reglas de validación propias que no caben en una anotación estándar (el RUN) |
| `config` | CORS, Swagger, recursos estáticos |
| `exception` | Respuestas de error uniformes para el frontend |

## Mapa de archivos de configuración

| Archivo | Qué hace | Quién lo lee | ¿Va a Git? |
| --- | --- | --- | --- |
| `.env` | Guarda las claves | Docker Compose | **No** |
| `docker-compose.yml` | Describe MySQL (y luego el backend) | Docker | Sí |
| `docker/mysql-init/01-crear-usuario-app.sh` | Crea el usuario limitado | MySQL, al crear el volumen | Sí |
| `application.yaml` | Configuración base, siempre se carga | Spring | Sí |
| `application-local.yaml` | Ajustes de desarrollo | Spring, perfil `local` | Sí |
| `application-prod.yaml` | Ajustes de producción | Spring, perfil `prod` | Sí |
| `db/migration/V1__...sql`, `V2__...sql` | Historial de cambios de la base (estructura y datos de referencia) | Flyway | Sí |
| `Dockerfile` | Receta de la imagen del backend | Docker | Sí |

## Qué es el JAR

Un **JAR** (*Java ARchive*) es un archivo `.jar`, básicamente un ZIP con clases Java compiladas. Normalmente no se ejecuta solo, porque necesita las librerías que usa.

En Spring Boot, `./mvnw package` genera un **"fat JAR"** (JAR ejecutable): incluye tu código **y todas las dependencias** (Spring, Hibernate, Tomcat, el driver de MySQL...) dentro de `BOOT-INF/`, más un cargador que sabe arrancarlas. Por eso se ejecuta con un solo comando:

```bash
java -jar target/TecnoFactory-0.0.1-SNAPSHOT.jar
```

Puntos clave:

| Pregunta | Respuesta |
| --- | --- |
| ¿Es un ejecutable? | Sí, pero no como un `.exe`: necesita **Java instalado** (JRE 21) para correr. No es un binario nativo. |
| ¿Qué contiene? | Tu código compilado, las librerías, y los archivos de `resources` (los yaml y las migraciones de Flyway). |
| ¿Trae el servidor web? | Sí: Tomcat va embebido, no instalas nada más. |
| ¿Es el mismo en local y en prod? | Sí. Es un solo JAR; **el perfil y las variables** deciden cómo se comporta. |
| ¿Y el `.jar.original`? | Es el JAR sin las dependencias, antes del empaquetado de Spring Boot. Se ignora. |
| ¿Dónde se genera? | En `target/`, al ejecutar `./mvnw clean package`. Esa carpeta no va a Git. |
| ¿Qué tiene que ver con Docker? | El `Dockerfile` compila el JAR en una etapa y lo copia a una imagen liviana con solo el JRE; el contenedor ejecuta `java -jar app.jar`. |

Los yaml **van dentro del JAR**: por eso los secretos nunca se escriben en ellos, sino que llegan por variables de entorno al arrancar.
