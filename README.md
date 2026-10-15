# TecnoFactory - Backend

API REST para un ecommerce con usuarios, roles y catálogo por categorías.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven&logoColor=white)

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Puesta en marcha](#puesta-en-marcha)
- [Comandos útiles de Docker](#comandos-útiles-de-docker)
- [Configuración](#configuración)
- [Instalar Docker en Ubuntu](#instalar-docker-en-ubuntu)
- [Roadmap](#roadmap)

---

## Descripción

**TecnoFactory** es el backend de un ecommerce construido con **Spring Boot** y **MySQL**.
Está pensado para trabajar junto a un frontend en **React** y desplegarse con **Docker**.

Funcionalidades previstas:

- Autenticación con **JWT** y gestión de usuarios con distintos **roles**
- Catálogo de productos organizado por **categorías**
- Pedidos con pago **simulado**
- Subida de imágenes de productos
- Documentación interactiva con **Swagger / OpenAPI**

---

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Seguridad | Spring Security + JWT (jjwt) |
| Base de datos | MySQL 8.4 |
| Contenedores | Docker y Docker Compose |
| Documentación | SpringDoc OpenAPI (Swagger UI) |
| Utilidades | Lombok, MapStruct |
| Monitoreo | Spring Boot Actuator |
| Extras | Spring Mail, Spring Cache + Redis |
| Testing | Spring Boot Test, Testcontainers |
| Build | Maven (Wrapper incluido) |

---

## Arquitectura

El proyecto sigue una **arquitectura en capas** organizada con el patrón *Package by Layer*:

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

Los datos viajan por la API mediante **DTOs**; las entidades nunca se exponen directamente.

---

## Estructura del proyecto

```
TecnoFactory/
├── docker-compose.yml
├── .env                         # secretos (no se sube a Git)
├── Dockerfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/factory/tecnofactory/
    │   │   ├── TecnoFactoryApplication.java
    │   │   ├── config/          # CORS, Swagger, recursos estáticos
    │   │   ├── controller/      # endpoints REST
    │   │   ├── dto/             # objetos de entrada y salida de la API
    │   │   ├── entity/          # modelo de datos (tablas)
    │   │   ├── exception/       # errores propios y manejador global
    │   │   ├── mapper/          # conversión entidad <-> DTO (MapStruct)
    │   │   ├── repository/      # interfaces JpaRepository
    │   │   ├── security/        # JWT, filtros y reglas de acceso
    │   │   └── service/         # lógica de negocio
    │   └── resources/
    │       ├── application.yaml
    │       └── application-local.yaml
    └── test/
```

### Responsabilidad de cada paquete

| Paquete | Responsabilidad | Por qué existe |
|---|---|---|
| `entity` | Clases que representan las tablas | Es el modelo de datos |
| `repository` | Consultas a la base de datos | Aísla el acceso a datos |
| `dto` | Datos que entran y salen de la API | Evita exponer contraseñas y desacopla la API de la BD |
| `mapper` | Convierte entidad <-> DTO | Elimina código repetitivo |
| `service` | Reglas de negocio | Los controllers no deben tener lógica |
| `controller` | Endpoints REST | Solo recibe, delega y responde |
| `security` | JWT y autorización | Toda la autenticación en un solo lugar |
| `config` | Configuración transversal | CORS, Swagger, recursos estáticos |
| `exception` | Manejo de errores | Respuestas de error uniformes para el frontend |

---

## Requisitos

- **JDK 21** (el JDK completo, no solo el JRE)
- **Docker** y **Docker Compose**
- Maven no es necesario: se usa el wrapper (`./mvnw`)

---

## Puesta en marcha

**1. Clonar el repositorio**

```bash
git clone <url-del-repositorio>
cd TecnoFactory
```

**2. Crear el archivo `.env`** en la raíz del proyecto

```env
DB_PASSWORD=una_clave_segura
```

**3. Levantar MySQL**

```bash
docker compose up -d
docker compose ps        # debe aparecer como "healthy"
```

**4. Ejecutar la aplicación** con el perfil `local` (desactiva el caché Redis)

```bash
SPRING_PROFILES_ACTIVE=local DB_PASSWORD=una_clave_segura ./mvnw spring-boot:run
```

**5. Abrir la documentación**

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health check | http://localhost:8080/actuator/health |

---

## Comandos útiles de Docker

| Comando | Acción |
|---|---|
| `docker compose up -d` | Levanta MySQL en segundo plano |
| `docker compose ps` | Muestra el estado (debe decir `healthy`) |
| `docker compose logs -f db` | Muestra los logs en vivo |
| `docker compose stop` | Detiene los contenedores (conserva datos) |
| `docker compose down` | Elimina los contenedores (conserva datos) |
| `docker compose down -v` | Elimina contenedores **y datos** (¡cuidado!) |

Entrar a MySQL desde la terminal:

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory
```

---

## Configuración

Las propiedades se leen desde variables de entorno, con valores por defecto para desarrollo.

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL de conexión JDBC | `jdbc:mysql://localhost:3306/tecnofactory` |
| `DB_USER` | Usuario de la base de datos | `root` |
| `DB_PASSWORD` | Contraseña de la base de datos | - |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) | solo desarrollo |
| `REDIS_HOST` | Servidor Redis | `localhost` |
| `MAIL_HOST` / `MAIL_PORT` | Servidor SMTP | `localhost` / `1025` |

**Perfiles de Spring**

- **default**: usa Redis como caché.
- **`local`**: desactiva el caché (`spring.cache.type: none`) para desarrollar sin Redis.

---

## Instalar Docker en Ubuntu

```bash
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
```

---

## Roadmap

- [x] Estructura base del proyecto y dependencias
- [x] MySQL con Docker Compose
- [ ] Entidades y CRUD de Categorías y Productos
- [ ] Manejo global de errores
- [ ] Registro, login y roles con JWT
- [ ] Pedidos y pago simulado
- [ ] Subida de imágenes de productos
- [ ] Envío de correos y caché con Redis
- [ ] Dockerfile del backend y despliegue completo
- [ ] Frontend en React
