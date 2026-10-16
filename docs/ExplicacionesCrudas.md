# TecnoFactory_Backend
Tecno Factory, Backend, Spring Boot y MySQL


Estructura de Archivos

TecnoFactory/
├── docker-compose.yml
├── .env                          (secretos, NO se sube a Git)
├── .gitignore
├── backend/                      (tu proyecto actual)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/factory/tecnofactory/
│       │   │   ├── TecnoFactoryApplication.java
│       │   │   ├── config/
│       │   │   │   ├── CorsConfig.java
│       │   │   │   ├── OpenApiConfig.java
│       │   │   │   └── WebConfig.java
│       │   │   ├── security/
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── JwtService.java
│       │   │   │   ├── JwtAuthFilter.java
│       │   │   │   └── UserDetailsServiceImpl.java
│       │   │   ├── entity/
│       │   │   │   ├── Usuario.java
│       │   │   │   ├── Rol.java
│       │   │   │   ├── Categoria.java
│       │   │   │   ├── Producto.java
│       │   │   │   ├── Pedido.java
│       │   │   │   ├── DetallePedido.java
│       │   │   │   └── EstadoPedido.java
│       │   │   ├── repository/
│       │   │   ├── dto/
│       │   │   ├── mapper/
│       │   │   ├── service/
│       │   │   ├── controller/
│       │   │   └── exception/
│       │   │       ├── RecursoNoEncontradoException.java
│       │   │       └── GlobalExceptionHandler.java
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-local.properties
│       │       └── db/migration/
│       │           └── V1__esquema_inicial.sql
│       └── test/
└── frontend/                     (React, más adelante)



Qué hace cada carpeta y por qué existe
Carpeta	Responsabilidad	Por qué separarla
entity	Clases que representan tablas de la BD	Es tu modelo de datos
repository	Interfaces para consultar la BD (JpaRepository)	Aísla el acceso a datos
dto	Objetos de entrada y salida de la API	Nunca expongas entidades: filtrarías la contraseña, y cambiar la BD rompería tu API
mapper	Convierte entidad ↔ DTO (MapStruct)	Evita código repetitivo
service	Lógica de negocio (stock, totales, validaciones)	Los controllers no deben tener lógica
controller	Endpoints REST	Solo recibe la petición, llama al service y responde
security	JWT, filtros, reglas de acceso	Todo lo de autenticación en un solo lugar
config	CORS, Swagger, recursos estáticos	Configuración transversal
exception	Errores propios y manejador global	Respuestas de error uniformes para React

Flujo de una petición: Controller → Service → Repository → BD, y de vuelta con DTOs.

COmandos para levantar Docker:
docker compose up -d          # levantar MySQL en segundo plano
docker compose ps             # ver estado (debe decir "healthy")
docker compose logs -f db     # ver logs
docker compose stop           # detener (conserva datos)
docker compose down           # eliminar contenedor (conserva datos)
docker compose down -v        # elimina contenedor Y datos (¡cuidado!)

Para entrar a MySQL desde la terminal:
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory


Instalar Docker en Ubuntu

# 1. Preparar el repositorio
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

# 2. Instalar
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 3. Usar Docker sin sudo
sudo usermod -aG docker $USER
newgrp docker

# 4. Verificar
docker run hello-world
docker compose version



## Qué es `db/migration`

Es la carpeta donde **Flyway busca los scripts SQL** que construyen y modifican tu base de datos. Flyway la lee por defecto en `src/main/resources/db/migration`, así que el nombre y la ruta no son arbitrarios: si cambias la carpeta, Flyway no encuentra nada.

Piensa en ella como el **historial de cambios de tu base de datos**, igual que Git lo es para tu código.

## Qué es `V1__esquema_inicial.sql`

Es la **primera migración**: el script que crea las tablas iniciales (`usuarios`, `categorias`, `productos`).

El nombre sigue una regla estricta:

```
V1__esquema_inicial.sql
│ │ │
│ │ └─ descripción (libre, con guiones bajos en vez de espacios)
│ └─── dos guiones bajos (separador obligatorio)
└───── V + número de versión
```

- **`V1`, `V2`, `V3`...** definen el orden de ejecución. Flyway las aplica de menor a mayor.
- **Son dos guiones bajos** `__`. Con uno solo, Flyway ignora el archivo o falla.
- **La descripción** es solo para que tú entiendas qué hace.

## Cómo funciona al arrancar la app

1. Flyway se conecta a MySQL y revisa la tabla `flyway_schema_history` (si no existe, la crea).
2. Compara los archivos de `db/migration` con lo que ya está registrado ahí.
3. Ejecuta solo las migraciones **nuevas**, en orden.
4. Guarda en la tabla qué versión se aplicó, cuándo, y un *checksum* (una huella del contenido del archivo).

Por eso **nunca debes editar una migración ya aplicada**: el checksum cambiaría y Flyway se negaría a arrancar. Para cambiar algo, creas una nueva:

```
V1__esquema_inicial.sql          ← ya aplicada, no se toca
V2__crear_pedidos.sql            ← nueva: crea pedidos y detalle_pedidos
V3__agregar_telefono_usuario.sql ← nueva: ALTER TABLE usuarios ADD telefono...
```

## Para qué sirve en la práctica

- **Tu base de datos queda versionada en Git** junto con tu código.
- **Todos los entornos quedan idénticos**: tu PC, el de un compañero y producción ejecutan los mismos scripts en el mismo orden.
- **Reproducible**: si borras el contenedor y levantas uno nuevo, al arrancar la app Flyway reconstruye todo solo.
- **Seguro**: por eso usamos `ddl-auto: validate` en vez de `update`. Hibernate solo comprueba que tus entidades coincidan con las tablas; Flyway es quien las crea.

## Sobre arrancar en local sin Redis

Lo entiendes bien. Con `SPRING_PROFILES_ACTIVE=local`, Spring carga además `application-local.yaml`, que tiene `spring.cache.type: none`. Eso desactiva el caché, así que la app no intenta usar Redis para cachear. Sin ese perfil, tu `application.yaml` dice `type: redis` y, al no haber Redis corriendo, los accesos al caché fallarían.

Una nota por tus capturas: tu archivo se llama `application.yaml` y `application-local.yaml`. Funciona igual que `.yml`, solo asegúrate de no tener ambas extensiones a la vez.

Cuando la app arranque, dime si viste `Successfully applied 1 migration` en el log y seguimos con las entidades.


Hay tres confusiones distintas aquí, así que las separo.

## 1. Flyway no "levanta" la base de datos

Quien levanta MySQL es **Docker**. Flyway solo trabaja *dentro* de una base que ya existe (la `tecnofactory` que crea el compose). Su trabajo es crear y modificar **las tablas** dentro de ella.

```
Docker        → levanta el servidor MySQL y crea la base "tecnofactory" (vacía)
Flyway        → crea las tablas dentro (usuarios, productos...)
Hibernate/JPA → usa esas tablas para guardar y leer objetos
```

## 2. Tu idea: generar las tablas desde las entidades

Eso **existe y funciona**: es `ddl-auto: update` (o `create`). Hibernate lee tus entidades y crea las tablas solo. Es lo que se usa en tutoriales y es perfectamente válido para **aprender y prototipar**.

Dos aclaraciones sobre cómo lo planteas:
- **Lombok no tiene nada que ver con la base de datos.** Solo genera getters/setters/constructores.
- **JDBC tampoco crea tablas**: es el driver de conexión. Quien las crea es Hibernate con `ddl-auto`.

Si quieres ir por ese camino, es simple:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
```

Y quitas las dependencias de Flyway y el `V1`.

**Por qué te recomendé Flyway igualmente:**

| | `ddl-auto: update` | Flyway |
|---|---|---|
| Rapidez al empezar | Muy rápida | Tienes que escribir SQL |
| Cambios destructivos (renombrar o borrar columna) | No los hace, o los hace mal | Los controlas tú |
| Seguridad en producción | Riesgosa, el propio Spring la desaconseja | Es el estándar |
| Historial de cambios | No existe | Versionado en Git |
| Datos iniciales (roles, admin, categorías) | Hay que improvisar | Va en una migración |

Mi consejo: si es un proyecto de aprendizaje o portafolio y quieres avanzar rápido, `update` es aceptable. Si quieres que se parezca a un proyecto profesional, o desplegarlo en serio, usa Flyway. Puedes empezar con `update` y pasarte a Flyway después, aunque esa migración tiene su trabajo. Tú decides; ambos caminos funcionan con todo lo demás que armamos.

## 3. Para qué es V1

Es simplemente **la primera versión del esquema**: el estado inicial de la base. Cada cambio futuro es V2, V3, etc. Si usas Flyway, V1 es donde nacen tus tablas. Si usas `update`, no existe.

## 4. Entidades vs DTOs: no son lo mismo

No se reemplazan, cumplen funciones distintas.

| | **Entidad** | **DTO** |
|---|---|---|
| Representa | Una **fila de la base de datos** | Los **datos que viajan por la API** |
| Quién la usa | Hibernate/JPA | El controller y React |
| Lleva anotaciones | `@Entity`, `@Id`, `@ManyToOne` | `@NotBlank`, `@Positive` |

Si solo usaras DTOs, **Hibernate no sabría cómo guardar nada**: necesita las entidades para saber qué tabla y qué columnas existen.

Si solo usaras entidades (sin DTOs), funcionaría, pero tendrías problemas:

- **Filtrarías datos sensibles**: `Usuario` tiene `password`; devolverlo directo la expone en el JSON.
- **Tu API quedaría atada a la BD**: renombrar una columna rompería React.
- **Relaciones lazy y bucles**: serializar un `Pedido` con sus detalles y productos puede causar errores o cargar media base de datos.
- **No controlas lo que entra**: un cliente podría mandar `id` o `rol: ADMIN` en un registro y modificar campos que no debería.

Ejemplo concreto:

```
Entidad Usuario:     id, nombre, email, password, rol, activo
DTO RegistroRequest: nombre, email, password          ← lo que el cliente puede enviar
DTO UsuarioResponse: id, nombre, email, rol           ← lo que le devuelves (sin password)
```

Para un proyecto muy pequeño puedes devolver entidades directamente, pero en cuanto hay usuarios y contraseñas, los DTOs dejan de ser opcionales.

## Qué hacemos

Dime cuál prefieres:

1. **Flyway** (lo que ya dejamos armado): sigues con `V1` y `ddl-auto: validate`.
2. **`ddl-auto: update`**: más rápido para empezar, sin SQL manual.

Con cualquiera seguimos con las entidades `Categoria` y `Producto`.

Sí, es un doble trabajo: describes la tabla en una entidad Java y otra vez en SQL, y tienen que coincidir. Es el costo real de Flyway, y por eso mucha gente en proyectos pequeños no lo usa al principio.

## Por qué la gente lo acepta igual

- **Hibernate te avisa si no coinciden.** Con `ddl-auto: validate`, si olvidas una columna o el tipo no cuadra, la app no arranca y el error dice exactamente qué falta. No es un fallo silencioso.
- **La base de datos es más importante que el código.** Los datos de producción viven años. Las entidades se pueden cambiar y redesplegar, pero una tabla con datos reales no se puede "recrear" sin más.
- **`update` tiene límites.** Agrega tablas y columnas nuevas, pero no renombra, no borra, no cambia tipos de forma segura ni carga datos iniciales (el admin, las categorías base).

## Cómo reducir el doble trabajo

No tienes que escribir el SQL a mano. Hay un camino intermedio que es el que más se usa:

1. **Desarrolla con `ddl-auto: update`** y Flyway desactivado. Cambias entidades libremente y Hibernate crea las tablas.
2. **Cuando el esquema se estabilice**, generas el SQL a partir de la base ya creada:
   ```bash
   docker exec tecnofactory-db mysqldump -uroot -p --no-data tecnofactory > V1__esquema_inicial.sql
   ```
3. Activas Flyway, cambias a `validate`, y desde ahí cada cambio es una migración nueva.

Así escribes entidades como querías, y el SQL lo produce una herramienta.

## Mi recomendación para tu caso

Para un proyecto de aprendizaje o portafolio con una sola persona, usa `update` ahora. Avanzas más rápido y te concentras en Spring, seguridad y React. Cuando quieras desplegarlo en serio, haces el paso del `mysqldump` y pasas a Flyway.

Para dejarlo así:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: false
```

Y en el `pom.xml` no agregues `spring-boot-starter-flyway` ni `flyway-mysql`. Si ya los agregaste, quítalos, porque con Flyway presente y un `V1` vacío podría fallar el arranque. Deja el archivo `V1` guardado fuera de `resources` o bórralo por ahora.

Con `update` ten presente que no es seguro para producción y que si cambias el tipo de una columna existente no lo ajustará. Mientras desarrollas, si algo queda raro, `docker compose down -v` y `up -d` te da una base limpia.

¿Vamos con las entidades `Categoria` y `Producto` usando `update`?


El archivo `application-prod.yaml` es un segundo archivo de configuración que Spring carga solo cuando arrancas con el perfil `prod`. Se aplica encima de `application.yaml`: lo que escribes aquí reemplaza al archivo base, y lo que no escribes se queda como estaba (correo, Redis, tamaño de subidas, etc.).

## Paso 1. Crear el archivo

1. En IntelliJ, clic derecho sobre `src/main/resources`.
2. *New → File*.
3. Nómbralo exactamente `application-prod.yaml`.

El nombre sigue la regla `application-` + nombre del perfil. Si escribes `application-produccion.yaml`, el perfil se llamaría `produccion` y no `prod`.

## Paso 2. Pegar este contenido

Usa **2 espacios por nivel y nunca tabuladores**; es el error más común en YAML.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true

jwt:
  secret: ${JWT_SECRET}

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

logging:
  level:
    root: WARN
    com.factory: INFO

server:
  error:
    include-message: never
    include-stacktrace: never
```

## Paso 3. Qué hace cada línea

### Bloque de base de datos

| Línea | Qué hace |
| --- | --- |
| `spring:` | Raíz de las propiedades de Spring. Todo lo que está más indentado debajo le pertenece. |
| `datasource:` | Agrupa la configuración de la conexión a la base. |
| `url: ${DB_URL}` | Lee la URL de la variable de entorno `DB_URL`. **No lleva valor por defecto** (en el archivo base sí: `${DB_URL:jdbc:...localhost...}`). Si la variable falta, la app no arranca. En producción es lo correcto: prefieres un error claro a que se conecte por accidente a `localhost`. |
| `username: ${DB_USER}` | Igual, para el usuario de la base. |
| `password: ${DB_PASSWORD}` | Igual, para la contraseña. Así no existe una clave por defecto como `root` en producción. |

### Bloque de JPA y Flyway

| Línea | Qué hace |
| --- | --- |
| `jpa:` | Agrupa la configuración de JPA y Hibernate. |
| `hibernate:` | Agrupa lo específico de Hibernate. |
| `ddl-auto: validate` | Hibernate solo comprueba que tus entidades coincidan con las tablas; **no crea ni modifica nada**. Es lo opuesto a `update`, que usas en `local`. |
| `show-sql: false` | No imprime cada consulta SQL en los logs. Es más rápido y evita filtrar datos en los registros. |
| `flyway:` | Agrupa la configuración de Flyway. |
| `enabled: true` | Activa Flyway. Como en producción las tablas las crea Flyway, esta línea reactiva lo que `local` desactiva. |

### Bloque de JWT

| Línea | Qué hace |
| --- | --- |
| `jwt:` | Grupo propio de tu aplicación (lo leerá `JwtService` con `@Value`). |
| `secret: ${JWT_SECRET}` | La clave que firma los tokens, sin valor por defecto. Si alguien conociera la clave de ejemplo del archivo base, podría fabricar tokens falsos. |

### Bloque de Swagger

| Línea | Qué hace |
| --- | --- |
| `springdoc:` | Grupo de configuración de SpringDoc. |
| `api-docs:` / `enabled: false` | Apaga el endpoint `/v3/api-docs`, que describe toda tu API. |
| `swagger-ui:` / `enabled: false` | Apaga la pantalla `/swagger-ui/index.html`. En producción no conviene mostrarle tu API completa a cualquiera. Si prefieres dejarla activa, borra este bloque. |

### Bloque de logs

| Línea | Qué hace |
| --- | --- |
| `logging:` / `level:` | Define cuánto detalle se escribe en los registros. |
| `root: WARN` | Todas las librerías (Spring, Hibernate, etc.) solo registran advertencias y errores. |
| `com.factory: INFO` | Tu propio código registra eventos informativos. Como tu paquete es `com.factory.TecnoFactory`, esta línea lo cubre. |

### Bloque de errores del servidor

| Línea | Qué hace |
| --- | --- |
| `server:` / `error:` | Controla cómo se ven los errores que Spring devuelve. |
| `include-message: never` | No incluye el mensaje interno de la excepción en la respuesta. |
| `include-stacktrace: never` | No muestra el rastro del error. Ambos ya son el valor por defecto, pero dejarlos escritos evita que alguien los active por error y exponga detalles internos. |

## Paso 4. Activar el perfil

Las variables que el archivo exige (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`) tienen que existir al arrancar.

**Desde la terminal, para probarlo:**

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
DB_PASSWORD=tu_clave \
JWT_SECRET=una-clave-larga-de-al-menos-32-caracteres \
./mvnw spring-boot:run
```

**Cuando tengas el backend en Docker**, el servicio de `docker-compose.yml` lleva estas líneas:

```yaml
  backend:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/tecnofactory
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
```

| Línea | Qué hace |
| --- | --- |
| `backend:` | Nombre del servicio del backend dentro del compose. |
| `build: .` | Construye la imagen con el `Dockerfile` de esa carpeta. |
| `environment:` | Lista de variables de entorno que recibe el contenedor. |
| `SPRING_PROFILES_ACTIVE: prod` | Hace que Spring cargue `application-prod.yaml`. |
| `DB_URL: jdbc:mysql://db:3306/...` | Usa `db`, el nombre del servicio MySQL, en vez de `localhost`, porque dentro de Docker cada servicio se encuentra por su nombre. |
| `DB_USER: root` | Usuario de la base. |
| `DB_PASSWORD: ${DB_PASSWORD}` | Toma el valor de tu `.env`. |
| `JWT_SECRET: ${JWT_SECRET}` | También se toma del `.env`. |

Agrega `JWT_SECRET=...` a tu `.env`. Genera una clave segura con:

```bash
openssl rand -base64 48
```

La clave debe tener **al menos 32 caracteres**, porque la firma HMAC de `jjwt` la exige.

## Paso 5. Comprobar que funciona

1. En el log de arranque debe aparecer `The following 1 profile is active: "prod"`.
2. `http://localhost:8080/swagger-ui/index.html` ya no debe cargar.
3. `http://localhost:8080/actuator/health` sí debe responder.
4. Prueba quitando `DB_PASSWORD`: la app debe fallar con `Could not resolve placeholder 'DB_PASSWORD'`. Eso confirma que no hay claves por defecto.

## Antes de usarlo en serio

- **`V1__esquema_inicial.sql` debe tener tus tablas.** Con `validate`, si las tablas no existen, la app no arranca. Hoy el archivo está vacío.
- **Falta Redis.** El archivo base usa `cache: type: redis`, así que en producción necesitas un servicio Redis. Si no lo vas a usar, agrega a `application-prod.yaml`:
  ```yaml
  spring:
    cache:
      type: none
  ```
  Al ser otra rama de `spring:`, debes unirla al bloque `spring:` que ya existe en el archivo, no escribir un segundo `spring:`.
- **El correo necesita un SMTP real.** El archivo base apunta a Mailpit sin autenticación. Con un proveedor real tendrás que agregar `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD`, y activar `smtp.auth` y `starttls.enable` en `prod`.
- **Corrige antes el `pom.xml`**, porque Flyway solo funcionará cuando sus dependencias queden dentro de `<dependencies>`.

Si quieres, te explico igual de detallado cómo funciona `application-local.yaml` o el `CorsConfig`, que también tendrá una versión para producción.



Tienes razón, rehago esto desde lo que quieres. Entendí que en **local** quieres todo encendido menos caché, Redis y correo, que quedan apagados hasta que los pruebes (se activan cambiando una línea). Y que en **prod** quieres que desde afuera no se vea cómo funciona el sistema.

## 1. Qué se controla con los perfiles y qué no

Los perfiles (`local` y `prod`) solo cambian **propiedades** del yaml. Lo que viene del `pom.xml` (Lombok, MapStruct, Security, Testcontainers) siempre está presente y no se enciende ni se apaga por perfil.

| Tecnología | Local | Prod | Dónde se controla |
| --- | --- | --- | --- |
| Web MVC, JPA, Validation, Lombok, MapStruct | Activos | Activos | Dependencias del `pom.xml`; no hay interruptor |
| Security + JWT | Activo | Activo, secreto sin valor por defecto | Solo cambia `jwt.secret` |
| MySQL | `update`, sin Flyway | `validate` con Flyway | yaml |
| Docker y Compose | MySQL en contenedor | Igual, más el backend | `docker-compose.yml`, fuera de Spring |
| Swagger (SpringDoc) | Visible | Apagado | yaml |
| Actuator | Con detalles y más endpoints | Solo `health`, sin detalles | yaml |
| Caché y Redis | Apagado por ahora | Apagado por ahora | yaml y compose |
| Correo | Sin uso hasta que escribas `EmailService` | Igual | yaml |
| Testing y Testcontainers | Disponibles (con Docker encendido) | No corren en producción | `pom.xml` con scope `test`; `-DskipTests` para saltarlos |

## 2. `application-local.yaml`

```yaml
spring:
  datasource:
    url: ${DB_URL}              # URL de MySQL, viene de la variable de entorno
    username: ${DB_USER}        # usuario de MySQL
    password: ${DB_PASSWORD}    # clave de MySQL (la misma de tu .env)

  jpa:
    hibernate:
      ddl-auto: update          # Hibernate crea y ajusta tablas desde tus entidades
    show-sql: true              # imprime cada consulta SQL
    properties:
      hibernate:
        format_sql: true        # el SQL sale en varias líneas, legible

  flyway:
    enabled: false              # sin migraciones mientras uses update

  cache:
    type: none                  # APAGADO. Cambia a "redis" cuando lo agregues al compose

jwt:
  secret: ${JWT_SECRET}         # clave que firma los tokens

springdoc:
  api-docs:
    enabled: true               # endpoint que describe tu API
  swagger-ui:
    enabled: true               # pantalla de Swagger

logging:
  level:
    root: INFO                  # librerías: eventos normales y errores
    com.factory: DEBUG          # tu código: todo el detalle

management:
  endpoint:
    health:
      show-details: always      # /actuator/health muestra cada componente
  endpoints:
    web:
      exposure:
        include: health,info,metrics   # endpoints de Actuator publicados
  health:
    redis:
      enabled: false            # cambia a true cuando levantes Redis
    mail:
      enabled: false            # cambia a true cuando levantes Mailpit

server:
  error:
    include-message: always         # el error trae su mensaje
    include-stacktrace: on_param    # rastro solo con ?trace=true en la URL
    include-binding-errors: always  # indica qué campo falló la validación
```

El correo no tiene interruptor de "apagado": simplemente no se envía nada hasta que tu código llame a `EmailService`. Lo único que se apaga es su revisión en `/actuator/health`.

## 3. `application-prod.yaml`

```yaml
spring:
  datasource:
    url: ${DB_URL}              # sin valor por defecto: si falta, la app no arranca
    username: ${DB_USER}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate        # solo comprueba; nunca modifica la base
    show-sql: false             # no imprime consultas

  flyway:
    enabled: true               # Flyway crea las tablas

  cache:
    type: none                  # hoy none porque aún no existe Redis; luego "redis"

jwt:
  secret: ${JWT_SECRET}         # sin valor por defecto

springdoc:
  api-docs:
    enabled: false              # nadie ve la descripción de tu API
  swagger-ui:
    enabled: false              # sin pantalla de Swagger

logging:
  level:
    root: WARN                  # solo advertencias y errores
    com.factory: INFO           # tu código: eventos importantes

management:
  endpoint:
    health:
      show-details: never       # solo "UP" o "DOWN", sin componentes
  endpoints:
    web:
      exposure:
        include: health         # único endpoint de Actuator publicado
  health:
    redis:
      enabled: false            # hasta que exista Redis
    mail:
      enabled: false            # hasta que exista un SMTP real

server:
  error:
    include-message: never          # los errores no revelan mensajes internos
    include-stacktrace: never       # ni rastros
    include-binding-errors: never   # tu GlobalExceptionHandler decide qué mostrar
```

Tres comentarios:

- `cache: none` y `health: redis/mail: false` en prod son temporales. Si los dejaras en `redis` sin tener Redis, `/actuator/health` saldría `DOWN`.
- Con `validate`, producción solo arrancará cuando `V1__esquema_inicial.sql` tenga tus tablas. Mientras tanto, prueba con `local`.
- Más adelante conviene mover Actuator a otro puerto no publicado, pero eso es un paso extra.

## 4. Tus dudas sobre Docker

**¿El compose es una instrucción para levantar Docker al arrancar el sistema, o antes?** Ninguna de las dos exactamente: es un archivo que **describe** qué contenedores quieres. Lo ejecutas tú con `docker compose up -d`, **antes** de arrancar tu app; Spring no lo lanza. El motor de Docker sí arranca con tu PC, y como tu servicio tiene `restart: unless-stopped`, MySQL vuelve a levantarse solo tras reiniciar, salvo que lo hayas detenido a mano.

**¿Solo tiene MySQL?** Sí.

**¿Redis se añade en el propio contenedor?** No. Va como un servicio aparte, con su propio contenedor, en el mismo `docker-compose.yml`, junto a `db`. Un contenedor por programa.

**¿El backend son las variables de entorno?** No. El backend es tu código Spring Boot. Las variables son los datos que tu app lee para saber dónde está MySQL y con qué claves entrar. El perfil es el conjunto de yaml que decide cómo se comporta. El camino es este:

1. Arrancas tu app con un perfil y las variables.
2. Spring lee `application.yaml` y el yaml del perfil.
3. `DB_URL` apunta a `localhost:3306`.
4. Docker redirige ese puerto de tu PC al 3306 del contenedor (`"3306:3306"`).

Hoy tu backend corre **fuera** de Docker, desde IntelliJ o `mvnw`; solo MySQL está en un contenedor.

Tu imagen del "mini computador" sirve, con un matiz: un contenedor es un proceso aislado con su propio sistema de archivos y red, pero **comparte el núcleo de tu sistema**, así que es mucho más liviano que una máquina virtual.

## 5. ¿Si el contenedor del backend se cae, muere la base de datos?

No, por dos razones:

- **Son contenedores separados.** El backend y MySQL no dependen uno del otro para existir. Si el backend se cae, MySQL sigue funcionando y guardando datos. El backend no guarda datos: los lee y escribe en MySQL.
- **Los datos de MySQL no viven dentro del contenedor.** Están en el volumen `mysql_data` (`volumes: - mysql_data:/var/lib/mysql`), guardado en el disco de tu PC. Si el contenedor de MySQL se cae, se reinicia o lo borras, el volumen y tus datos siguen ahí.

| Qué haces | ¿Se pierden los datos? |
| --- | --- |
| Se cae el contenedor del backend | No |
| Se cae o reinicia el contenedor de MySQL | No |
| `docker compose stop` o `docker compose down` | No |
| `docker compose down -v` | **Sí**, borra el volumen |
| Falla el disco o borras el volumen a mano | **Sí** |

Por eso, cuando tengas datos que importen, haz respaldos. Este comando deja una copia en tu carpeta actual:

```bash
docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql
```

Las imágenes de productos seguirán la misma regla: la carpeta `uploads` del backend también tendrá que ser un volumen, o se perderán al recrear su contenedor.


No. Son dos cosas distintas, y conviene no confundirlas:

- **Persistencia**: que los datos sobrevivan cuando el contenedor se borra o se reinicia.
- **Respaldo**: una copia **independiente** de los datos, guardada en otro lugar.

Levantar Docker no crea ninguno de los dos automáticamente.

## La persistencia depende de tu volumen

Tu compose declara `mysql_data:/var/lib/mysql`. Esa línea es la que hace que los datos vivan en tu disco, fuera del contenedor. Sin un volumen declarado, los datos viven dentro del contenedor y se pierden cuando lo eliminas (según la imagen, puede quedar un volumen anónimo, pero es difícil de ubicar). Por eso se declara uno con nombre.

En tu Ubuntu, Docker lo guarda en una carpeta propia (`/var/lib/docker/volumes/tecnofactory_mysql_data/`). No la edites a mano. Puedes ver tus volúmenes con:

```bash
docker volume ls
```

## Por qué el volumen no es un respaldo

El volumen es **la única copia** de tus datos. Se pierde si:

- ejecutas `docker compose down -v`,
- borras el volumen con `docker volume rm`,
- falla o se formatea el disco,
- alguna operación corrompe la base y no tienes copia anterior.

Un respaldo es lo que te salva en esos casos. Para MySQL, lo más simple es un volcado en un archivo SQL:

```bash
docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql
```

Para que sirva de verdad, **guarda ese archivo fuera de esa máquina o de ese disco**: un pendrive, otro disco o la nube. Y créalo con cierta frecuencia, por ejemplo antes de cada cambio grande.

## Resumen

| | Qué es | ¿Lo da Docker solo? |
| --- | --- | --- |
| Volumen | Los datos viven fuera del contenedor, en tu disco | Solo si lo declaras (tu compose ya lo hace) |
| Respaldo | Copia independiente en otro lugar | No, lo haces tú |

Mientras estás en desarrollo, con datos de prueba, el volumen basta: si se pierde, lo recreas. El respaldo empieza a importar cuando tengas datos que no quieras volver a cargar.


El perfil no lo decide ningún archivo: **lo decides tú, cada vez que arrancas la app**, con una sola variable: `SPRING_PROFILES_ACTIVE`.

## Cómo funciona

- **`application.yaml` se carga siempre**, sin importar el perfil. Son los ajustes de fábrica.
- **`application-local.yaml` y `application-prod.yaml` solo se cargan si su nombre coincide con el perfil activo.** La palabra después del guion (`local`, `prod`) es el nombre del perfil.
- Spring junta el archivo base con el del perfil elegido. El del perfil se pone encima y **solo reemplaza lo que él escribe**; todo lo demás queda como en el base.

Ejemplo con `ddl-auto`:

| Qué pasa | Resultado |
| --- | --- |
| Base dice `validate` | Es el valor de fábrica |
| Arrancas con `local`, que dice `update` | Se usa `update` |
| Arrancas con `prod`, que dice `validate` | Se usa `validate` |
| `mail.host` solo está en el base | Se mantiene en ambos perfiles |

Nunca se cargan los dos perfiles a la vez, salvo que tú lo indiques (por ejemplo `local,debug`).

## Dónde escribes la elección

| Forma | Cómo |
| --- | --- |
| Variable de entorno en IntelliJ | *Run, Edit Configurations, Environment variables*: `SPRING_PROFILES_ACTIVE=local` |
| Campo "Active profiles" de IntelliJ | En algunas versiones, la configuración de Spring Boot trae un campo con ese nombre; escribes `local` |
| Terminal | `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` |
| Docker Compose | `SPRING_PROFILES_ACTIVE: local` dentro de `environment:` del backend |

Las cuatro hacen lo mismo. Elige una por cada forma de arrancar la app.

## Cómo comprobar cuál quedó activo

En el log de arranque aparece una línea:

- `The following 1 profile is active: "local"` si funcionó.
- `No active profile set, falling back to 1 default profile: "default"` si no definiste ninguno. En ese caso solo se usa `application.yaml`.

## Dos cuidados

- **No escribas el perfil dentro de `application.yaml`.** Si lo dejas fijo ahí, un día puedes arrancar en `local` por error en un servidor.
- **Si te olvidas de definirlo, no falla**: la app arranca con el base, que pide Redis y usa `validate`. Si ves comportamientos raros, revisa esa línea del log primero.

En resumen: los tres archivos conviven en la carpeta, y el que se aplica encima del base depende de lo que escribas en `SPRING_PROFILES_ACTIVE` al arrancar. Hoy, en tu PC, siempre `local`.

¿Quieres que pasemos a la pieza 2 (MySQL en Docker)?

Casi: `db` **no es una variable**, es el **nombre de un servicio**. Un servicio es un bloque que describe un contenedor: "quiero un contenedor así, con estos ajustes". Se parece más a la etiqueta de una ficha que a una variable.

La diferencia, en tu propio código:

| Qué es | Cómo se ve | Para qué sirve |
| --- | --- | --- |
| Nombre de servicio | `db:` y `backend:` | Etiqueta del contenedor; **también es su nombre de red** |
| Variable | `${DB_PASSWORD}` | Un valor que Compose busca en tu `.env` y reemplaza |

Por eso en el backend la URL dice `jdbc:mysql://db:3306/...`: dentro de la red de Docker, los contenedores se encuentran por el nombre de su servicio. Si renombraras `db:` a `mysql:`, esa URL tendría que ser `jdbc:mysql://mysql:3306/...`.

## Y `backend`

Sí: es **otro contenedor**, el que contendrá tu aplicación Spring Boot. Y como tu app no existe en ninguna tienda de Docker, la imagen hay que construirla, y para eso sirve el Dockerfile. La línea `build: .` es lo que une ambos.

## ¿Debes hacerlo o es opcional?

Depende de lo que quieras:

| Si quieres... | ¿Necesitas Dockerfile? |
| --- | --- |
| Desarrollar y probar en tu PC (IntelliJ + MySQL en Docker) | No |
| Desplegar todo el proyecto en un servidor con un solo comando | Sí |

Tu objetivo final es poder desplegar donde quieras, así que sí lo harás, pero **más adelante**. Mientras desarrollas, lo más cómodo es correr la app desde IntelliJ (reinicio rápido y depuración) y dejar solo MySQL en Docker, como ya tienes. Por eso el bloque `backend` sigue comentado.

Un detalle: cuando lo actives, cambia `SPRING_PROFILES_ACTIVE: prod` por `local`. Con `prod`, la app no arrancará hasta que tus tablas existan en Flyway.

¿Pasamos a la pieza 3, las variables de entorno?


## Qué es una variable de entorno

Es una nota con formato `NOMBRE=valor` que le entregas a un programa cuando arranca. El programa la lee por su nombre. No está en tu código ni en ningún archivo de la app, y por eso sirve para claves: no se suben a Git y cambian según dónde corra el programa.

## La confusión más común: dos programas distintos las leen

Ves `${DB_PASSWORD}` en el compose y también en el yaml de Spring. Se escriben igual, pero **las lee un programa distinto cada vez**, y cada uno la busca en un lugar diferente:

| Quién la lee | Dónde está escrita la línea | De dónde saca el valor |
| --- | --- | --- |
| **Docker Compose** | `docker-compose.yml` | Del archivo `.env` (lo lee solo) |
| **Spring Boot** | `application-local.yaml` | De las variables con que **tú** arrancas la app (IntelliJ o terminal) |

Spring **no lee** el `.env`. Por eso el valor hay que darlo en dos sitios, y tiene que ser el mismo.

## Las variables de tu proyecto

| Variable | Qué es | Quién la lee | ¿La inventas tú? |
| --- | --- | --- | --- |
| `DB_PASSWORD` | Clave de MySQL | Compose (para crear MySQL) y Spring (para entrar) | Sí |
| `JWT_SECRET` | Clave que firma los tokens de login | Solo Spring | Sí |
| `DB_USER` | Usuario de MySQL: `root` | Spring | No, es fijo |
| `DB_URL` | Dirección de la base: `jdbc:mysql://localhost:3306/tecnofactory` | Spring | No, es fijo |
| `SPRING_PROFILES_ACTIVE` | Perfil: `local` | Spring | No, eliges `local` |

Solo **dos** son secretos que tú inventas: `DB_PASSWORD` y `JWT_SECRET`.

## El recorrido de `DB_PASSWORD`

```
.env  (DB_PASSWORD=miClave)
  │
  ├─► Compose lo lee ─► MySQL se crea con la clave "miClave"
  │
  └─► (tú lo copias) ─► variables de IntelliJ ─► Spring entra a MySQL con "miClave"
```

Si ambas no coinciden, la app falla al arrancar con `Access denied for user 'root'`.

## Qué escribe cada uno

**Archivo `.env`** (lo lee Compose, junto a `docker-compose.yml`):

```env
DB_PASSWORD=la_clave_que_elijas
JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres
```

**Variables de IntelliJ** (*Run, Edit Configurations, Environment variables*; las lee Spring):

```
SPRING_PROFILES_ACTIVE=local;DB_URL=jdbc:mysql://localhost:3306/tecnofactory;DB_USER=root;DB_PASSWORD=la_misma_del_env;JWT_SECRET=cualquiera_de_32_o_mas
```

`JWT_SECRET` en local no tiene que coincidir con ninguna otra: es solo la clave con que tu app firma sus propios tokens.

## Qué pasa si se hace mal (y cómo se nota)

| Error | Síntoma | Causa |
| --- | --- | --- |
| Falta una variable en IntelliJ | `Could not resolve placeholder 'DB_URL'` | Spring no la encontró; el mensaje dice cuál |
| `DB_PASSWORD` distinta a la de MySQL | `Access denied for user 'root'` | No coincide con la del `.env` |
| Cambiaste `DB_PASSWORD` en `.env` después de crear MySQL | Sigue funcionando la clave antigua | MySQL guardó la primera en el volumen; para cambiarla hay que borrar el volumen con `docker compose down -v` (solo si no tienes datos que perder) |
| `.env` subido a Git | Tus claves quedan expuestas | Verifica `.gitignore` |
| Escribiste `$` en la contraseña | Compose la interpreta mal | Evita ese símbolo |

## Resumen en una frase

Las variables son las notas que conectan las piezas: el `.env` alimenta a Compose, las variables de IntelliJ alimentan a Spring, y `DB_PASSWORD` es la única que debes mantener **igual en ambos lados**.

¿Quieres que pasemos a la pieza 4 (el backend en Docker) o prefieres que antes te ayude a comprobar que tu `.env` y tus variables de IntelliJ están bien?

Comando para cargar Spring Boot desde la terminal

SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
DB_PASSWORD=la_misma_del_env \
JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres \
./mvnw spring-boot:run


## 1. Cómo se usa Flyway

No lo "ejecutas": **no hay ningún comando**. Solo haces tres cosas:

1. Creas un archivo `.sql` con el nombre correcto en `src/main/resources/db/migration`.
2. Escribes tu SQL dentro.
3. Arrancas la aplicación.

Al arrancar, Flyway se conecta a MySQL, ejecuta los archivos que aún no se han aplicado y deja registro. En el log debes ver algo como:

```
Successfully applied 1 migration to schema `tecnofactory`
```

Para ver el registro en la base:

```sql
SELECT version, description, script, success FROM flyway_schema_history;
```

## 2. El nombre del archivo importa

```
V1__crear_tablas_base.sql
│ │ │
│ │ └─ descripción: palabras con guion bajo
│ └─── dos guiones bajos (obligatorio)
└───── V mayúscula + número de versión
```

- Flyway las ejecuta **de menor a mayor versión**, comparando como números: `V2` va antes que `V10`.
- No puede haber dos archivos con la misma versión.
- Con un solo guion bajo o una `v` minúscula, el archivo se ignora y no lo ves hasta que faltan las tablas.

## 3. Cómo funciona el versionamiento

Flyway crea una tabla propia, `flyway_schema_history`, y en cada arranque compara esa tabla con tus archivos:

| Columna | Qué guarda |
| --- | --- |
| `version` | Versión aplicada (1, 2, 3...) |
| `description` | La descripción del nombre |
| `checksum` | Una **huella del contenido** del archivo |
| `installed_on` | Cuándo se aplicó |
| `success` | Si terminó bien |

Un ejemplo: la base está en la versión 2 y tienes tres archivos.

```
V1__...  ya aplicada   → se omite
V2__...  ya aplicada   → se omite
V3__...  nueva         → se ejecuta y se registra
```

Por eso la regla clave es: **un archivo ya aplicado no se edita**. Si lo cambias, la huella ya no coincide y Flyway se niega a arrancar:

```
Validate failed: Migration checksum mismatch for migration version 1
```

Cualquier cambio posterior va en una versión nueva, siempre **mayor** que la última aplicada. Si creas un `V2` cuando ya existe un `V3`, Flyway lo considera fuera de orden y falla.

## 4. Mientras desarrollas tienes una excepción

Si aún no has desplegado nada, puedes editar el `V1` cuantas veces quieras, siempre que reinicies la base antes de volver a arrancar:

```bash
docker compose down -v
docker compose up -d
```

Cuando el esquema ya sea definitivo, o lo use otra persona o un servidor, deja de editarlo.

## 5. Qué considerar al escribir las migraciones

**MySQL no deshace los cambios de estructura si algo falla a medias.** Si un archivo tiene 5 sentencias y la tercera falla, las dos primeras ya quedaron aplicadas, y Flyway marca la migración como fallida (`success = 0`). Dos consecuencias:

- Mantén cada migración pequeña y enfocada.
- En desarrollo, si falla, corriges el SQL y reinicias la base con `down -v`.

Otras reglas:

| Qué cuidar | Por qué |
| --- | --- |
| Crea primero las tablas que otras referencian | Una clave foránea a una tabla que aún no existe falla (`categorias` antes que `productos`) |
| Ponle nombre a tus restricciones (`CONSTRAINT fk_productos_categoria ...`) | Facilita borrarlas o modificarlas después |
| Los tipos deben coincidir con tus entidades | Con `validate`, si no calzan la app no arranca |
| Datos iniciales (roles, categorías base) van en una migración aparte, por ejemplo `V2__datos_iniciales.sql` | Mantiene el esquema separado de los datos |
| Contraseñas iniciales, como un admin, siempre ya hasheadas con BCrypt | Nunca texto plano en un archivo que va a Git |
| No cambies tablas a mano desde DBeaver | La base y los archivos se desincronizan; todo cambio va en una migración |
| Cambios con datos (renombrar una columna) en pasos | Agregar la nueva, copiar los datos y recién después borrar la antigua |
| No mezcles Hibernate `update` con Flyway | Se pisan; con Flyway, usa `ddl-auto: validate` |
| Guarda los archivos en UTF-8 | Evita problemas con tildes y la ñ |

Una secuencia típica de archivos se ve así:

```
V1__crear_tablas_base.sql
V2__datos_iniciales.sql
V3__agregar_isbn_a_libro.sql
```

Y el contenido de un cambio posterior es solo el cambio:

```sql
ALTER TABLE libro ADD COLUMN isbn VARCHAR(20);
```

## 6. Errores típicos y qué significan

| Error | Causa | Solución |
| --- | --- | --- |
| `Migration checksum mismatch` | Editaste un archivo ya aplicado | En desarrollo, `down -v`; en producción, nunca editar, crear uno nuevo |
| `Detected failed migration to version X` | Una ejecución anterior falló a medias | Corregir el SQL y reiniciar la base (en desarrollo) |
| `Found non-empty schema(s) ... but no schema history table` | Había tablas creadas por Hibernate antes de usar Flyway | Reiniciar la base con `down -v` |
| `Detected resolved migration not applied to database` | Creaste una versión menor que otra ya aplicada | Renombrarla con una versión mayor |
| La app arranca pero no existen las tablas | Nombre mal escrito o carpeta equivocada | Revisar `V` mayúscula, dos guiones bajos y la ruta `db/migration` |
| `Schema-validation: missing column ...` | La entidad y el script no coinciden | Corregir uno de los dos |

## 7. Resumen en una frase

Cada archivo `V#__...sql` es un paso numerado del historial de tu base, Flyway ejecuta solo los nuevos en orden, y la regla de oro es: **lo ya aplicado no se toca, solo se agregan versiones nuevas**.

Cuando tengas tu primer script, pásamelo y lo reviso antes de que lo ejecutes.

No exactamente. Las versiones **no son alternativas para volver a probar una anterior**: son capas que se apilan una encima de la otra. La base final es el resultado de aplicar V1, luego V2 y luego V3, siempre en ese orden.

## La idea correcta

Piensa en construir un edificio por pisos:

| Versión | Qué construye |
| --- | --- |
| V1 | Los cimientos: las tablas |
| V2 | Los muebles: los datos iniciales |
| V3 | Una ventana nueva: una columna que faltaba |

Si falla el piso 3, no "pruebas el piso 1": arreglas el piso 3, porque el edificio es la suma de los tres. Lo que sí ofrece el versionamiento es:

- **Historial**: sabes qué se cambió, cuándo y en qué orden.
- **Reproducibilidad**: en una base vacía, Flyway reconstruye todo en el mismo orden.
- **Orden de aplicación**: cada entorno aplica solo lo que le falta.

Flyway gratis **no deshace** migraciones. Si una falla, la solución es corregirla y reiniciar la base (en desarrollo), o escribir una versión nueva que arregle el error (en producción). El respaldo es tu verdadera red de seguridad.

Si quisieras reconstruir la base hasta cierto punto, existe `spring.flyway.target: 2`, que aplica hasta V2 y se detiene. Se usa sobre una base vacía y rara vez lo vas a necesitar.

## Lo que te confundió: esos nombres eran un ejemplo

`V1__crear_tablas_base`, `V2__datos_iniciales` y `V3__agregar_isbn_a_libro` eran una muestra de cómo se ve un historial **después de semanas**. No tienes que crear los tres ahora.

## La secuencia que debes seguir tú

| Momento | Qué haces |
| --- | --- |
| **Hoy**, mientras diseñas | Un solo archivo: `V1__crear_tablas_base.sql` con todas las tablas. Lo editas libremente, y cada vez reinicias la base con `docker compose down -v` y `up -d` |
| Cuando las tablas funcionen con tus entidades | Si necesitas datos de partida (roles, categorías base), los pones en `V2__datos_iniciales.sql` |
| Cuando des el esquema por definitivo (antes de desplegar o de compartir la base) | **Congelas** V1 y V2: ya no se editan |
| Desde ahí, cada cambio | Un archivo nuevo: `V3`, `V4`... |

La regla práctica: **creas un archivo nuevo solo cuando el anterior ya se aplicó en algún lugar que no quieres reiniciar** (producción, una base compartida, datos que te importan). Mientras todo sea tu base local de pruebas, editas y reinicias.

## Cuándo editar y cuándo crear uno nuevo

| Situación | Qué haces |
| --- | --- |
| Me equivoqué en una tabla y aún no he desplegado nada | Edito `V1` y reinicio la base con `down -v` |
| Quiero agregar una columna después de desplegar | Nuevo `V3` con un `ALTER TABLE` |
| Quiero datos iniciales | `V2` (o el siguiente número libre) |
| Una migración ya aplicada en producción tiene un error | Nueva versión que lo corrige; no se edita la antigua |

## Por qué separar estructura y datos

Aunque V1 y V2 se apliquen el mismo día, conviene tenerlos en archivos distintos: el esquema es la parte estable, y los datos iniciales suelen cambiar más. Así un cambio en los datos no obliga a tocar el archivo de las tablas.

Para esta semana, tu trabajo se reduce a una cosa: escribir **un solo `V1__crear_tablas_base.sql`** con tu diseño. Pásamelo cuando lo tengas y lo reviso.