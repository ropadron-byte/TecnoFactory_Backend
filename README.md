# Documentación de TecnoFactory

Índice de la documentación técnica del backend. Cada archivo cubre **una categoría**; léelos en el orden que necesites.

| # | Archivo | Qué responde |
| --- | --- | --- |
| 01 | [Arquitectura y stack](docs/01-arquitectura-y-stack.md) | Qué es el proyecto, con qué está hecho, cómo se organiza el código y qué es un JAR |
| 02 | [Docker y MySQL](docs/02-docker-y-mysql.md) | Cómo funciona el compose, los tres usuarios de MySQL, el script de inicio y los volúmenes |
| 03 | [Variables de entorno y claves](docs/03-variables-y-claves.md) | De dónde sale cada variable, quién la lee y cómo manejar los secretos |
| 04 | [Perfiles de Spring](docs/04-perfiles-spring.md) | Qué cambia entre `local` y `prod`, archivo por archivo |
| 05 | [Flyway y migraciones](docs/05-flyway-y-migraciones.md) | Cómo se versiona la base y qué reglas respetar |
| 06 | [Base de datos](docs/06-base-de-datos.md) | Estado actual de `V1`, observaciones y convenciones de diseño |
| 07 | [Ejecución: local y prod](docs/07-ejecucion-local-y-prod.md) | Pasos exactos para levantar local, probar prod y desplegar prod real |
| 08 | [Seguridad](docs/08-seguridad.md) | Modelo de seguridad actual, lo que falta y la lista de verificación |
| 09 | [Errores comunes](docs/09-errores-comunes.md) | Síntoma, causa y solución de los problemas ya encontrados |
| 10 | [Pendientes y futuro](docs/10-pendientes-y-futuro.md) | Qué falta construir, en qué orden y qué cambia en la configuración al hacerlo |

## Estado del proyecto (17 oct 2026)

| Pieza | Estado |
| --- | --- |
| MySQL 8.4 en Docker con volumen y tres usuarios | Funcionando |
| Flyway aplicando `V1` con `tecnofactory_migrator` | Probado en perfil `prod` |
| La app arrancando como `tecnofactory_app` (solo DML) | Probado en perfil `prod` |
| `V1` con las 13 tablas | Aplicada, pero **sin adaptar** a las convenciones de MySQL (ver 06) |
| Entidades, DTOs, services, controllers | No existen todavía |
| `SecurityConfig`, JWT | Clases vacías |
| `Dockerfile` y servicio `backend` del compose | Vacío / comentado |

## Reglas de oro

1. **Spring no lee el `.env`.** Lo lee Docker Compose; a Spring hay que pasarle las variables al arrancar.
2. **Una migración aplicada no se edita.** Los cambios son archivos nuevos (`V2__...`). Mientras `V1` solo exista en tu máquina, puedes recrearla con `docker compose down -v`.
3. **Los scripts de `docker/mysql-init` y las claves `MYSQL_*` solo se aplican al crear el volumen.**
4. **`docker compose down -v` borra los datos.** Nunca en producción.
5. **La app en producción usa `tecnofactory_app`**, nunca `root`.
