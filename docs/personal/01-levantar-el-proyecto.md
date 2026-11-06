# 01. Levantar el proyecto

Para cuando abres el computador y quieres que todo funcione.

## Lista de 3 pasos

| # | Paso | Comando | Esperado |
| --- | --- | --- | --- |
| 1 | Docker Desktop encendido | (abrir la aplicación) | Ballena estable |
| 2 | Arrancar MySQL y la app | `./scripts/dev.sh` (Windows: `.\scripts\dev.ps1`) | MySQL `healthy` en el 3307 y `Started TecnoFactoryApplication` |
| 3 | Comprobar | Abrir `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

El lanzador carga el `.env`, levanta MySQL en el puerto **3307** (deja libre el 3306 para otro MySQL instalado) y espera a que esté `healthy` antes de arrancar el backend.

Si prefieres hacerlo a mano:

```bash
docker compose up -d --wait db
set -a; source .env; set +a
SPRING_PROFILES_ACTIVE=local DB_URL=jdbc:mysql://localhost:3307/tecnofactory DB_USER=root ./mvnw spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui/index.html`.

> En IntelliJ, las variables se definen una vez en **Run → Edit Configurations → Environment variables**. Spring **no** lee el `.env` por sí solo.

## Variables que necesita

`DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32 o más caracteres), `ADMIN_EMAIL`, `ADMIN_PASSWORD`. Opcional: `CORS_ORIGINS` (por defecto `http://localhost:5173`).

## Primera vez en un computador nuevo

1. Instala Docker y Java 21.
2. Copia `.env.example` a `.env` y completa los valores.
3. `./scripts/dev.sh` (crea la base, los 3 usuarios y arranca la app).
4. Flyway crea las tablas y la app crea el administrador.

## Empezar de cero (solo desarrollo)

```bash
docker compose down -v      # BORRA todos los datos
docker compose up -d
```

## Si algo falla

Mira [03 Algo falla](03-algo-falla.md). Los dos casos más comunes: `Could not resolve placeholder` (faltan variables) y `Communications link failure` (MySQL apagado).

## Para saber más

[Guía 02: Docker y MySQL](../guias/02-docker-y-mysql.md) · [Guía 03: variables](../guias/03-variables-y-claves.md) · [Guía 06: local y producción](../guias/06-ejecutar-local-y-prod.md)
