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