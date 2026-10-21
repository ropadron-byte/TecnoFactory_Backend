# 04. Comandos

## Docker y MySQL

| Quiero... | Comando |
| --- | --- |
| Levantar la base | `docker compose up -d` |
| Ver el estado | `docker compose ps` |
| Ver sus logs | `docker compose logs -f db` |
| Apagar (conserva datos) | `docker compose down` |
| Apagar y **borrar datos** | `docker compose down -v` |
| Entrar a MySQL | `docker exec -it tecnofactory-db mysql -uroot -p tecnofactory` |

## Variables

| Quiero... | Comando |
| --- | --- |
| Cargar el `.env` en la terminal | `set -a; source .env; set +a` |
| Comprobar una | `echo $DB_USER` |

## Maven

| Quiero... | Comando |
| --- | --- |
| Arrancar la app | `./mvnw spring-boot:run` |
| Todos los tests (sin base) | `./mvnw test -Dtest="*Test"` |
| Un test | `./mvnw test -Dtest=UsuarioServiceTest` |
| Compilar sin tests | `./mvnw -DskipTests package` |
| Limpiar y compilar | `./mvnw clean package` |

## Perfiles

| Quiero... | Comando |
| --- | --- |
| Local (por defecto) | `./mvnw spring-boot:run` |
| Producción | `SPRING_PROFILES_ACTIVE=prod java -jar target/*.jar` |

## Consultas útiles a la base

```sql
SHOW TABLES;
SELECT version, description, success FROM flyway_schema_history;
SELECT correo, activo FROM usuario;
```

## Revisar el log

```bash
./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by"
```

## Git (si lo usas)

| Quiero... | Comando |
| --- | --- |
| Ver qué cambió | `git status` |
| Comprobar que `.env` no se sube | `git check-ignore .env` (debe imprimir `.env`) |

## Para saber más

[Guía 02](../guias/02-docker-y-mysql.md) · [Guía 06](../guias/06-ejecutar-local-y-prod.md) · [Guía 15](../guias/15-pruebas.md)
