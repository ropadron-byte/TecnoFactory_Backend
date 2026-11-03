# Guía 06. Ejecutar: local y producción

> **En una frase:** hay tres formas de correr el proyecto; para el día a día usas la primera (local): MySQL en Docker y la app desde Maven o IntelliJ.

## Los tres escenarios

| | 1. Local | 2. Prod de prueba | 3. Prod real |
| --- | --- | --- | --- |
| Dónde corre la app | Tu máquina (Maven / IntelliJ) | Tu máquina (JAR) | Contenedor en el servidor |
| Perfil | `local` | `prod` | `prod` |
| Usuario de la app | `root` | `tecnofactory_app` | `tecnofactory_app` |
| Swagger | Visible | Apagado | Apagado |
| Errores de la API | Con detalle | Sin detalle | Sin detalle |
| Para qué | Trabajo diario | Probar que prod funciona | Publicar |

## Requisitos

JDK 21 completo (`javac -version`), Docker y Docker Compose (`docker compose version`), y ejecutar todo **desde la raíz del proyecto** (donde está `mvnw`).

## Levantar en local (los 4 pasos)

La forma recomendada es el lanzador: levanta MySQL en el puerto **3307**, espera a que esté `healthy` y arranca el backend con el perfil `local`.

```bash
./scripts/dev.sh
```

En Windows: `.\scripts\dev.ps1`.

Si prefieres hacerlo a mano:

```bash
# 1. MySQL (publicado en el 3307)
docker compose up -d --wait db
docker compose ps                  # espera "healthy" y 0.0.0.0:3307->3306/tcp

# 2. Cargar el .env y arrancar
set -a; source .env; set +a
SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3307/tecnofactory \
DB_USER=root \
./mvnw spring-boot:run
```

> El **3307** es el puerto del host: deja libre el 3306 para el MySQL que ya esté instalado en el computador. Dentro de Docker, MySQL sigue escuchando en el 3306.

3. **Comprobar el log:** debe decir `The following 1 profile is active: "local"`, `Successfully applied N migrations` (solo la primera vez), `HikariPool-1 - Start completed` y `Tomcat started on port 8080`.
4. **Abrir** Swagger en http://localhost:8080/swagger-ui/index.html y el estado en http://localhost:8080/actuator/health.

**Con IntelliJ:** *Run → Edit Configurations → Environment variables*:
`SPRING_PROFILES_ACTIVE=local;DB_URL=jdbc:mysql://localhost:3307/tecnofactory;DB_USER=root;DB_PASSWORD=...;JWT_SECRET=...`

Para apagar: `Ctrl + C` en la app y `docker compose stop` (conserva los datos).

## Probar prod en tu máquina (resumen)

1. Base vacía: `docker compose down -v` y `docker compose up -d`.
2. Construir: `./mvnw package`.
3. Arrancar el JAR con `SPRING_PROFILES_ACTIVE=prod` y **todas** las variables (sin valores por defecto: si falta una, no arranca).
4. Comprobar que Flyway migró con `tecnofactory_migrator`, que Swagger no responde y que `tecnofactory_app` **no** puede hacer `DROP TABLE`.

El paso a paso con los comandos exactos está en la referencia.

## Reglas que no hay que olvidar

1. **Desde la raíz del proyecto.** `./mvnw: No existe el archivo` significa que estás en otra carpeta.
2. **Sin `SPRING_PROFILES_ACTIVE` la app intenta usar Redis y falla.**
3. **En local y en prod las tablas las crea Flyway**; Hibernate solo valida.
4. **Las variables de prod no tienen valor por defecto.**
5. **Antes de desplegar de verdad:** ver la [lista de despliegue](../personal/08-antes-de-desplegar.md).

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| `Port 8080 was already in use` | Otra instancia corriendo | Detenla |
| `Communications link failure` | MySQL apagado | `docker compose up -d` |
| `Could not resolve placeholder` | Falta una variable | Cárgala (Spring no lee el `.env`) |
| `Access denied for user` | Clave o usuario incorrectos | Revisa `DB_USER` y `DB_PASSWORD` |

## Para saber más

[Referencia 07: ejecución completa, los tres escenarios y comandos de Docker](../referencia/07-ejecucion-local-y-prod.md) · [Personal 01: levantar el proyecto](../personal/01-levantar-el-proyecto.md)
