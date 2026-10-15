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