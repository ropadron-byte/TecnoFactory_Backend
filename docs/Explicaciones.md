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