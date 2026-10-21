# 01. Levantar el proyecto

Para cuando abres el computador y quieres que todo funcione.

## Lista de 5 pasos

| # | Paso | Comando | Esperado |
| --- | --- | --- | --- |
| 1 | Docker Desktop encendido | (abrir la aplicación) | Ballena estable |
| 2 | Levantar MySQL | `docker compose up -d` | El contenedor aparece como `healthy` |
| 3 | Cargar las variables | `set -a; source .env; set +a` | Sin salida |
| 4 | Arrancar la app | `./mvnw spring-boot:run` | `Started TecnoFactoryApplication` |
| 5 | Comprobar | Abrir `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

Swagger: `http://localhost:8080/swagger-ui/index.html`.

> En IntelliJ, las variables se definen una vez en **Run → Edit Configurations → Environment variables**. Spring **no** lee el `.env` por sí solo.

## Variables que necesita

`DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32 o más caracteres), `ADMIN_EMAIL`, `ADMIN_PASSWORD`. Opcional: `CORS_ORIGINS` (por defecto `http://localhost:5173`).

## Primera vez en un computador nuevo

1. Instala Docker y Java 21.
2. Copia `.env.example` a `.env` y completa los valores.
3. `docker compose up -d` (crea la base y los 3 usuarios).
4. Arranca la app: Flyway crea las 13 tablas, y la app crea el administrador.

## Empezar de cero (solo desarrollo)

```bash
docker compose down -v      # BORRA todos los datos
docker compose up -d
```

## Si algo falla

Mira [03 Algo falla](03-algo-falla.md). Los dos casos más comunes: `Could not resolve placeholder` (faltan variables) y `Communications link failure` (MySQL apagado).

## Para saber más

[Guía 02: Docker y MySQL](../guias/02-docker-y-mysql.md) · [Guía 03: variables](../guias/03-variables-y-claves.md) · [Guía 06: local y producción](../guias/06-ejecutar-local-y-prod.md)
