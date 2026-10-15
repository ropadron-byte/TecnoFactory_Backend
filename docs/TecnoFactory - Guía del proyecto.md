# TecnoFactory - Guía del proyecto

Oct 15, 2026 · @Roman

## Lo que llevo hecho

Tienes la base armada: Spring Boot configurado, MySQL corriendo en Docker y la estructura de paquetes creada, pero todavía ninguna clase con lógica. Esto es lo que muestra tu zip al 15 de octubre de 2026.

### Qué hice, por qué, cómo y para qué sirve

| Qué hice | Por qué | Cómo y con qué | Para qué sirve más adelante |
| --- | --- | --- | --- |
| Crear el proyecto con Spring Boot 4.1.1, Java 21 y Maven | Boot 4.1.1 es la versión estable más nueva de tu lista; Java 21 es LTS y todas las librerías lo soportan | Spring Initializr; el `pom.xml` hereda de `spring-boot-starter-parent` 4.1.1 y define `java.version` 21 | El parent fija las versiones compatibles de casi todas las dependencias, así que casi no escribes versiones |
| Compilar con el Maven Wrapper (`mvnw`) | El Maven del IDE falló con un error de Guice; el wrapper usa una versión fija | `./mvnw clean install` y el JDK 21 completo (`openjdk-21-jdk`), no solo el JRE | El mismo comando compila en tu PC, en otro equipo y en un servidor, sin instalar Maven |
| Elegir las dependencias | Cubrir API REST, login con roles, base de datos, documentación y pruebas | Starters en el `pom.xml`: Web MVC, JPA, Security, Validation, Actuator, SpringDoc, MySQL Driver, Lombok, Mail, Cache, Redis y Testcontainers; además Flyway, jjwt y MapStruct | Cada una habilita una parte del backend; el detalle está en la tabla de dependencias |
| Organizar el código por capas | Saber en qué carpeta va cada cosa y mantener el flujo Controller, Service, Repository | Paquetes `config`, `controller`, `dto`, `entity`, `exception`, `mapper`, `repository`, `security` y `service` dentro de `com.factory.TecnoFactory` | Cada funcionalidad nueva se agrega en su capa sin mezclar responsabilidades |
| Crear 9 clases vacías | Marcar dónde irá cada pieza de configuración, seguridad y errores | `CorsConfig`, `OpenApiConfig`, `WebConfig`, `GlobalExceptionHandler`, `RecursoNoEncontradoException`, `JwtAuthFilter`, `JwtService`, `SecurityConfig`, `UserDetailsServiceImpl` | Ahí escribirás el login con JWT, CORS y el manejo de errores |
| Configurar `application.yaml` y `application-local.yaml` | Que conexión, JWT, correo y Redis se lean de variables de entorno | YAML con `${VARIABLE:valor_por_defecto}`; el perfil `local` usa `ddl-auto: update` y desactiva Flyway | La misma app corre en tu PC, en Docker o en producción cambiando solo variables |
| Levantar MySQL con Docker | No instalar MySQL en tu computador y llevar la base junto al proyecto | `docker-compose.yml` con `mysql:8.4`, volumen `mysql_data`, healthcheck y `utf8mb4`; la clave vive en `.env` | Despliegas en cualquier lado con `docker compose up`; después se suma el backend como otro servicio |
| Crear `db/migration/V1__esquema_inicial.sql` | Dejar lista la ruta que Flyway lee para versionar la base | Archivo vacío en `src/main/resources/db/migration` | Ahí irá tu esquema si usas Flyway; con el perfil `local` no se ejecuta |
| Dejar un `Dockerfile` vacío | Reservarlo para empaquetar el backend | Archivo en la raíz, sin contenido | Construirá la imagen del backend para el despliegue completo |
| Documentar | Tener tus apuntes y un README presentable para Git | `README.md` y `docs/Explicaciones.md` | Guía para ti y para quien vea tu repositorio |

### Problemas que encontré en tu zip

1. **El `pom.xml` está mal armado y bloquea el build.** Las dependencias de Flyway, jjwt y MapStruct quedaron después de `</dependencies>`, y Maven exige que cada `<dependency>` esté dentro de ese bloque: fallará con "Unrecognised tag: 'dependency'". Mueve la línea `</dependencies>` para dejarla justo debajo del bloque de MapStruct. El `<build>` ya está bien ubicado.
2. **El perfil por defecto exige tablas que aún no existen.** Usa `ddl-auto: validate` y `V1__esquema_inicial.sql` está vacío. Hoy no falla porque no hay entidades, pero cuando las agregues no arrancará sin el perfil `local` (que usa `update`). Mientras uses `update`, arranca con `SPRING_PROFILES_ACTIVE=local`.
3. **El perfil `local` ya no desactiva Redis.** El bloque `cache: type: none` quedó comentado. Hoy no afecta porque no usas `@Cacheable`, pero fallará cuando lo uses, y `/actuator/health` puede salir DOWN sin Redis ni servidor de correo. Une todo en un solo bloque `spring:`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
  cache:
    type: none
```

4. **Flyway sigue en el `pom.xml` aunque está desactivado en local.** Decide: lo usas (V1 con tus tablas y `validate`) o quitas `spring-boot-starter-flyway` y `flyway-mysql` mientras trabajas con `update`.
5. **El test `contextLoads` falla sin MySQL.** Levanta toda la aplicación y necesita la base. Compila con `./mvnw clean install -DskipTests` o ejecútalo con MySQL arriba y el perfil `local`.
6. **`RecursoNoEncontradoException` debe extender `RuntimeException`.** Vacía no funciona como excepción. Las otras clases vacías están bien por ahora.
7. **El paquete `com.factory.TecnoFactory` lleva mayúscula.** La convención de Java es minúscula (`com.factory.tecnofactory`); cambiarlo ahora, sin código, es casi gratis (*Refactor, Rename* en IntelliJ).
8. **El zip no trae `.gitignore`.** Confirma que existe en tu repositorio y que incluye `.env`, `target/` y `.idea/`.

## Roadmap de archivos

Después de la base que ya tienes, el backend se construye en ocho fases; cada una se apoya en la anterior, así que conviene probarla en Swagger antes de pasar a la siguiente. Los nombres de entidades son una propuesta: adáptalos a tu diagrama de base de datos.

Flujo de una petición: React llama a un `controller`, este delega en un `service`, que usa un `repository` para hablar con MySQL, y la respuesta vuelve convertida en `dto`.

### Fase 0. Raíz y entorno (ya la tienes)

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `pom.xml` | Declara dependencias, versión de Java y plugins de compilación | Maven no compila ni descarga nada sin él | Hecho, con un error de estructura |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven Wrapper: descarga y usa una versión fija de Maven | Compilas igual en cualquier equipo sin instalar Maven | Hecho |
| `docker-compose.yml` | Define el contenedor de MySQL (y luego backend, Redis y Mailpit) | Levanta toda la infraestructura con un comando | Hecho (solo MySQL) |
| `.env` | Guarda secretos como `DB_PASSWORD` | Compose lo lee solo y evitas escribir claves en el código | Hecho |
| `.gitignore` | Lista lo que Git no debe subir | Impide publicar `.env`, `target/` y archivos del IDE | Verificar que exista |
| `Dockerfile` | Receta para construir la imagen del backend | Permite desplegar la app dentro de Docker | Vacío |
| `README.md`, `docs/` | Documentación del proyecto | Quien abra el repositorio entiende cómo arrancarlo | Hecho |

### Fase 1. Configuración de la aplicación

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `TecnoFactoryApplication.java` | Punto de entrada que arranca Spring | Sin él no hay aplicación; ahí se agrega `@EnableCaching` | Hecho |
| `application.yaml` | Configuración base: BD, JWT, correo, Redis, Actuator | Un solo lugar para ajustar la app sin tocar código | Hecho |
| `application-local.yaml` | Ajustes de desarrollo: `update`, sin Flyway, sin caché | Desarrollas sin Redis ni migraciones | Hecho, falta descomentar el caché |
| `application-prod.yaml` | Ajustes de producción: `validate`, logs sobrios, sin Swagger si quieres | Separa lo seguro de lo cómodo | Falta |
| `config/CorsConfig.java` | Permite que React (por ejemplo `localhost:5173`) llame a la API | Sin CORS el navegador bloquea todas las peticiones del frontend | Vacío |
| `config/OpenApiConfig.java` | Da título, versión y botón de token JWT a Swagger | Pruebas endpoints protegidos desde el navegador | Vacío |
| `config/WebConfig.java` | Sirve la carpeta `/uploads` como archivos estáticos | Las imágenes de productos se muestran por URL | Vacío |

### Fase 2. Base de datos: entidades y repositorios

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `entity/Rol.java` (enum) | Lista los roles: ADMIN, VENDEDOR, CLIENTE | Controla quién puede hacer qué | Falta |
| `entity/Usuario.java` | Fila de la tabla de usuarios, con contraseña hasheada | Es la base del login y de los pedidos | Falta |
| `entity/Categoria.java` | Categoría del catálogo, opcionalmente con categoría padre | Organiza los productos | Falta |
| `entity/Producto.java` | Producto con precio `BigDecimal`, stock e `imagenUrl` | Es el centro del ecommerce | Falta |
| `entity/EstadoPedido.java` (enum) | PENDIENTE, PAGADO, RECHAZADO, ENVIADO | Sigue el ciclo de vida del pedido | Falta |
| `entity/Pedido.java` | Compra de un usuario: total, estado y fecha | Guarda el historial de compras | Falta |
| `entity/DetallePedido.java` | Línea del pedido: producto, cantidad y precio unitario al comprar | Un cambio de precio posterior no altera pedidos antiguos | Falta |
| `repository/*Repository.java` | Una interfaz `JpaRepository` por entidad (Usuario, Categoria, Producto, Pedido) | Spring genera las consultas sin que escribas SQL | Falta |
| `db/migration/V1__esquema_inicial.sql` | Crea las tablas si usas Flyway | Versiona la base en Git; opcional mientras uses `update` | Vacío |

### Fase 3. Manejo de errores

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `exception/RecursoNoEncontradoException.java` | Error propio para "no existe ese id" (extiende `RuntimeException`) | Los services avisan el problema sin devolver `null` | Vacío |
| `exception/GlobalExceptionHandler.java` | Convierte excepciones en respuestas JSON con el código HTTP correcto | React recibe siempre el mismo formato de error | Vacío |

### Fase 4. Catálogo: categorías y productos

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `dto/CategoriaRequest`, `CategoriaResponse` | Datos que entran y salen de la API de categorías | No expones entidades y validas la entrada con `@Valid` | Falta |
| `dto/ProductoRequest`, `ProductoResponse` | Lo mismo para productos | Controlas qué campos puede enviar y recibir el cliente | Falta |
| `mapper/CategoriaMapper`, `ProductoMapper` | Convierten entidad a DTO y al revés con MapStruct | Evitas escribir getters y setters a mano | Falta |
| `service/CategoriaService`, `ProductoService` | Reglas de negocio: crear, editar, buscar, paginar, validar stock | Los controllers quedan sin lógica | Falta |
| `controller/CategoriaController`, `ProductoController` | Endpoints REST `/api/categorias` y `/api/productos` | Es lo que React consume; el GET es público y el resto exige rol ADMIN | Falta |

### Fase 5. Seguridad y usuarios

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `security/JwtService.java` | Genera y valida tokens con jjwt | Autenticación sin sesiones en el servidor | Vacío |
| `security/UserDetailsServiceImpl.java` | Carga un usuario por email y le asigna `ROLE_<rol>` | Spring Security necesita saber quién es y qué rol tiene | Vacío |
| `security/JwtAuthFilter.java` | Lee `Authorization: Bearer ...` en cada petición y registra al usuario | Conecta el token con las reglas de acceso | Vacío |
| `security/SecurityConfig.java` | Define rutas públicas y protegidas, BCrypt, CORS y modo sin sesión | Es el guardia de toda la API | Vacío |
| `dto/LoginRequest`, `RegistroRequest`, `AuthResponse`, `UsuarioResponse` | Datos de login, registro y respuesta con token | La contraseña nunca sale en una respuesta | Falta |
| `service/AuthService`, `controller/AuthController` | Registro y login en `/api/auth/**` | Es la puerta de entrada de los usuarios | Falta |
| `service/UsuarioService`, `controller/UsuarioController` | Perfil propio y gestión de usuarios y roles para ADMIN | Cubre "gestión de usuarios con distintos roles" | Falta |

### Fase 6. Pedidos y pago simulado

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `dto/PedidoRequest`, `PedidoResponse` | Carrito que llega y pedido que vuelve | Define el contrato del checkout con React | Falta |
| `mapper/PedidoMapper` | Convierte pedido y detalles a DTO | Evita bucles al serializar relaciones | Falta |
| `service/PedidoService` | Calcula el total, descuenta stock y cambia el estado, todo en una transacción | Si algo falla, no queda un pedido a medias | Falta |
| `service/pago/PasarelaPago.java` (interfaz) | Contrato para cobrar un pedido | Cambiar de pago simulado a real no toca el resto del código | Falta |
| `service/pago/PasarelaPagoSimulada.java` | Aprueba o rechaza con una regla de prueba | Es tu pago simulado; no guardes números de tarjeta | Falta |
| `controller/PedidoController` | Endpoints para crear, ver y listar pedidos; cambiar estado como ADMIN | Es el checkout y el historial del cliente | Falta |

### Fase 7. Extras

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `service/ImagenService`, endpoint de subida | Valida tipo y tamaño, guarda con nombre UUID y devuelve la URL | Las imágenes viven en disco o en la nube; la BD solo guarda la URL | Falta |
| `service/EmailService` | Envía correos de bienvenida y confirmación de pedido | Usa Spring Mail; en desarrollo, Mailpit | Falta |
| `config/CacheConfig` + `@EnableCaching` | Define TTL y serialización del caché en Redis | Acelera listados que casi no cambian, como categorías | Falta |

### Fase 8. Pruebas y despliegue

| Archivo | Qué hace | Por qué lo necesitas | Estado |
| --- | --- | --- | --- |
| `src/test/.../*Test.java` | Pruebas de repositorios (con Testcontainers) y de endpoints | Detectas errores antes de desplegar | Hay un `contextLoads` por defecto |
| `Dockerfile` (backend) | Compila el JAR y lo empaqueta en una imagen con Java 21 | Meter el backend en un contenedor | Vacío |
| `docker-compose.yml` (ampliado) | Suma los servicios `backend`, `redis` y `mailpit` | Todo el sistema arranca con `docker compose up` | Falta |
| `application-prod.yaml` | Configuración para el servidor | Separa los ajustes de producción | Falta |

## Dependencias del proyecto

Tu `pom.xml` declara 25 dependencias y 2 procesadores de anotaciones. Las que dicen "administrada por Boot" no llevan versión porque el parent la fija; las demás la llevan porque Spring Boot no las controla. Flyway, jjwt y MapStruct solo funcionarán cuando corrijas la posición de `</dependencies>`.

### Núcleo de la API

| Dependencia | Versión y alcance | Para qué sirve | Dónde la usarás |
| --- | --- | --- | --- |
| `spring-boot-starter-webmvc` | Administrada por Boot | Spring MVC con Tomcat embebido; permite crear endpoints REST (en Boot 4 reemplaza a `starter-web`) | Todos los `@RestController` |
| `spring-boot-starter-validation` | Administrada por Boot | Bean Validation con Hibernate Validator: `@NotBlank`, `@Email`, `@Positive` | Anotaciones en los DTOs `Request` y `@Valid` en los controllers |
| `springdoc-openapi-starter-webmvc-ui` | 3.1.0 | Genera la documentación OpenAPI y la interfaz Swagger UI | `/swagger-ui/index.html` para probar la API; se configura en `OpenApiConfig` |
| `spring-boot-starter-actuator` | Administrada por Boot | Endpoints de monitoreo como `/actuator/health` | Healthchecks de Docker y revisión del estado de la app |
| `lombok` | Administrada por Boot, opcional | Genera getters, setters y constructores con anotaciones | `@Getter`, `@Setter`, `@RequiredArgsConstructor` en entidades y services |

### Seguridad

| Dependencia | Versión y alcance | Para qué sirve | Dónde la usarás |
| --- | --- | --- | --- |
| `spring-boot-starter-security` | Administrada por Boot | Autenticación, autorización por roles, BCrypt, filtros de seguridad | Paquete `security`; `@PreAuthorize("hasRole('ADMIN')")` |
| `jjwt-api` | 0.12.6 | Interfaces para crear y leer tokens JWT | `JwtService` |
| `jjwt-impl` | 0.12.6, runtime | Implementación interna de jjwt; no se importa en el código | Se carga sola al ejecutar |
| `jjwt-jackson` | 0.12.6, runtime | Convierte el contenido del token a JSON | Se carga sola al ejecutar |

### Datos

| Dependencia | Versión y alcance | Para qué sirve | Dónde la usarás |
| --- | --- | --- | --- |
| `spring-boot-starter-data-jpa` | Administrada por Boot | JPA con Hibernate y Spring Data: guardas y consultas objetos sin SQL | `entity` y `repository` |
| `mysql-connector-j` | Administrada por Boot, runtime | Driver JDBC para conectar con MySQL | Se usa solo desde la configuración de `application.yaml` |
| `spring-boot-starter-flyway` | Administrada por Boot | Activa Flyway en el arranque (en Boot 4 el starter es necesario) | Migraciones en `db/migration` |
| `flyway-mysql` | Administrada por Boot | Soporte de Flyway para MySQL | Migraciones SQL |
| `spring-boot-starter-cache` | Administrada por Boot | Abstracción de caché con `@Cacheable` y `@CacheEvict` | `CategoriaService` y listados que cambian poco |
| `spring-boot-starter-data-redis` | Administrada por Boot | Cliente de Redis, que actúa como almacén del caché | Configuración `spring.data.redis` y `CacheConfig` |

### Correo y mapeo

| Dependencia | Versión y alcance | Para qué sirve | Dónde la usarás |
| --- | --- | --- | --- |
| `spring-boot-starter-mail` | Administrada por Boot | Envío de correos con `JavaMailSender` | `EmailService`; en desarrollo con Mailpit |
| `mapstruct` | 1.6.3 | Anotaciones para definir mappers entidad a DTO | Interfaces en el paquete `mapper` |

### Pruebas (todas con alcance `test`)

| Dependencia | Para qué sirve | Dónde la usarás |
| --- | --- | --- |
| `spring-boot-starter-webmvc-test` | JUnit, Mockito y MockMvc para probar controllers | Tests de endpoints |
| `spring-boot-starter-data-jpa-test` | Soporte para probar repositorios con `@DataJpaTest` | Tests de consultas |
| `spring-boot-starter-security-test` | Simula usuarios y roles en las pruebas | Tests de rutas protegidas |
| `spring-boot-starter-validation-test` | Soporte de pruebas de validación | Tests de DTOs |
| `spring-boot-starter-actuator-test` | Soporte de pruebas de Actuator | Poco uso al principio |
| `spring-boot-testcontainers` | Integra Testcontainers con Spring (`@ServiceConnection`) | Configura solo la conexión al MySQL de prueba |
| `testcontainers-junit-jupiter` | Integra Testcontainers con JUnit 5 (`@Testcontainers`) | Ciclo de vida del contenedor en cada test |
| `testcontainers-mysql` | Contenedor `MySQLContainer` | Levanta un MySQL real durante los tests; requiere Docker |

### Compilación (sección `<build>`)

| Componente | Para qué sirve |
| --- | --- |
| `lombok` en `annotationProcessorPaths` | Genera el código de Lombok al compilar |
| `lombok-mapstruct-binding` 0.2.0 | Hace que Lombok y MapStruct funcionen juntos (el orden importa: Lombok, binding, MapStruct) |
| `mapstruct-processor` 1.6.3 | Genera las implementaciones de tus mappers al compilar |
| `spring-boot-maven-plugin` | Empaqueta el JAR ejecutable y habilita `spring-boot:run`; excluye Lombok del JAR |
| `maven-compiler-plugin` | Compila con los procesadores de anotaciones configurados |

No están incluidas a propósito: `spring-boot-devtools` (reinicio automático en desarrollo) y los SDK de pago real (Stripe, Transbank) o de almacenamiento (S3, Cloudinary), que solo harán falta cuando salgas del pago simulado y de las imágenes en disco.

## Guía para diseñar tu base de datos

Tu frontend en React ya es el requisito: cada pantalla muestra datos, y esos datos son las tablas y columnas que necesitas. El método es sacar sustantivos de las pantallas, definir sus campos y luego decidir cómo se relacionan.

### 1. Qué te dice cada pantalla

| Pantalla del frontend | Qué te dice del modelo |
| --- | --- |
| Catálogo y filtros | Productos y categorías; hace falta paginar, buscar por nombre y filtrar por categoría |
| Detalle de producto | Descripción, precio, stock y una o varias imágenes |
| Carrito | Si vive en el navegador no necesita tabla; si debe persistir entre dispositivos, sí |
| Checkout | Dirección, teléfono, método de pago, pedido y su detalle |
| Login y registro | Usuarios con email único, contraseña hasheada y rol |
| Perfil e historial | Datos del usuario y sus pedidos pasados |
| Panel admin | CRUD de productos y categorías, gestión de usuarios y roles, estados de pedidos |

También revisa los tipos o interfaces de TypeScript, los JSON de ejemplo, los campos de cada formulario y los archivos con `fetch` o `axios`: son el contrato exacto que el backend debe cumplir.

### 2. Método en cinco pasos

1. **Lista las entidades.** Cada sustantivo con vida propia (usuario, producto, categoría, pedido) es una tabla candidata.
2. **Define los atributos de cada una.** Cada campo de un formulario o de una tarjeta es una columna; marca cuáles son obligatorios y cuáles únicos.
3. **Define las relaciones y su cardinalidad.** Pregunta en ambos sentidos: "un pedido tiene muchos productos, ¿un producto está en muchos pedidos?". Eso decide la forma.
4. **Elige las claves.** Cada tabla lleva un `id` autoincremental; las relaciones se guardan como clave foránea en el lado "muchos".
5. **Dibuja el diagrama** en dbdiagram.io, Mermaid o el diagrama ER de MySQL Workbench y revísalo contra las pantallas: si una pantalla muestra un dato que no está en el diagrama, falta algo.

### 3. Cómo se relacionan las cosas

| Relación | Cuándo ocurre | Cómo se modela | En JPA |
| --- | --- | --- | --- |
| Uno a muchos | Una categoría tiene muchos productos | Clave foránea en la tabla del lado "muchos" (`categoria_id` en productos) | `@ManyToOne` en `Producto`; `@OneToMany(mappedBy = ...)` opcional en `Categoria` |
| Muchos a muchos | Un usuario con varios roles | Tabla intermedia con dos claves foráneas | `@ManyToMany` con `@JoinTable` |
| Muchos a muchos con datos propios | Pedido y producto con cantidad y precio | Una tabla propia (`detalle_pedido`) con sus columnas | Entidad `DetallePedido` con dos `@ManyToOne` |
| Uno a uno | Un pedido y su pago | Clave foránea única en una de las dos tablas | `@OneToOne` |
| Auto-relación | Subcategorías | Columna `categoria_padre_id` que apunta a la misma tabla | `@ManyToOne` hacia `Categoria` |

Usa `FetchType.LAZY` en las relaciones `@ManyToOne` para no cargar datos de más.

### 4. Tipos recomendados

| Dato | Java | MySQL | Nota |
| --- | --- | --- | --- |
| Identificador | `Long` | `BIGINT AUTO_INCREMENT` | Con `GenerationType.IDENTITY` |
| Dinero | `BigDecimal` | `DECIMAL(12,2)` | Nunca `double` ni `float` |
| Cantidad o stock | `Integer` | `INT` | Agrega una regla para que no sea negativo |
| Texto corto | `String` | `VARCHAR(255)` | Define `length` si necesitas otro tamaño |
| Texto largo | `String` | `VARCHAR(1000)` o `TEXT` | Para descripciones |
| Estado o rol | `enum` | `VARCHAR(20)` | Con `@Enumerated(EnumType.STRING)`, nunca `ORDINAL` |
| Fecha y hora | `LocalDateTime` | `DATETIME` | Para `fecha_creacion` o `fecha` del pedido |
| Booleano | `boolean` | `BOOLEAN` | Por ejemplo `activo` |
| Imagen | `String` | `VARCHAR(255)` | Solo la URL o la ruta, no el archivo |

En MySQL usa nombres en `snake_case` (`imagen_url`); Spring Boot convierte `imagenUrl` a ese formato solo.

### 5. Decisiones que debes tomar mirando tu frontend

- [ ] ¿El carrito se guarda en la base o solo en el navegador?
- [ ] ¿Las categorías tienen subcategorías?
- [ ] ¿Cada producto tiene una imagen o varias?
- [ ] ¿Un usuario puede tener varios roles o uno solo?
- [ ] ¿Se guardan direcciones de envío? ¿Una o varias por usuario?
- [ ] ¿Se borran productos o se desactivan con un campo `activo`? (desactivar conserva el historial de pedidos)
- [ ] ¿Hay reseñas, favoritos, cupones o descuentos en el frontend?
- [ ] ¿Necesitas saber cuándo se creó o modificó cada registro?
- [ ] ¿El pago simulado guarda un registro propio (tabla `pago`) o solo un estado en el pedido?

### 6. Errores comunes que conviene evitar

- **Guardar solo la clave del producto en el detalle del pedido.** Guarda también `precio_unitario`, para que cambiar el precio no altere compras antiguas.
- **Guardar la contraseña en texto plano.** Siempre con BCrypt, y la columna debe tener al menos 60 caracteres.
- **Guardar imágenes o números de tarjeta en la base.** Las imágenes van en disco o en la nube; los datos de tarjeta no se guardan ni siquiera simulados.
- **Olvidar la unicidad.** Marca `email` como `UNIQUE`, y el nombre de categoría si no debe repetirse.
- **Usar `ddl-auto: update` como si fuera migración.** Crea tablas y columnas nuevas, pero no renombra ni borra; si algo queda raro en desarrollo, `docker compose down -v` y `docker compose up -d` te dan una base limpia.
- **Diseñar relaciones que dependen de ambos lados.** Elige un lado dueño de la relación y evita bucles al devolver JSON; para eso existen los DTOs.

### 7. Del diseño a la base real

1. Diseña el diagrama a partir de tus pantallas.
2. Escribe las entidades en el paquete `entity` y arranca con el perfil `local` (`ddl-auto: update`): Hibernate crea las tablas.
3. Abre DBeaver o IntelliJ Database en `localhost:3306` y compara las tablas con tu diagrama.
4. Cuando el esquema se estabilice, genera el SQL con `docker exec tecnofactory-db mysqldump -uroot -p --no-data tecnofactory > V1__esquema_inicial.sql`, pégalo en `db/migration` y pasa a `ddl-auto: validate` con Flyway activado.
5. Desde ahí, cada cambio de esquema va en un archivo nuevo (`V2__...sql`); nunca edites uno ya aplicado.

## Próximos pasos

Lo primero es que el proyecto compile; después diseñas la base y avanzas por las fases en orden.

1. Corrige el `pom.xml` (mueve `</dependencies>` debajo de MapStruct) y ejecuta `./mvnw clean install -DskipTests`.
2. Une el perfil `local` en un solo bloque `spring:` con `cache: type: none`, y decide si usarás Flyway o `update`.
3. Renombra el paquete a `com.factory.tecnofactory` antes de escribir código.
4. Dibuja tu diagrama de base de datos desde las pantallas de React y resuelve la lista de decisiones de la guía.
5. Escribe las entidades, arranca con el perfil `local` y comprueba las tablas en DBeaver.
6. Avanza por las fases 3 a 6 del roadmap, probando cada una en Swagger.

Cuando tengas el diagrama, pásamelo y lo reviso contra tus pantallas antes de que escribas las entidades.
