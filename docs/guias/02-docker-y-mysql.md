# Guía 02. Docker y MySQL

> **En una frase:** MySQL corre dentro de un contenedor de Docker; tú solo lo enciendes, lo apagas y miras que esté «healthy».

## La idea

Un **contenedor** es una caja cerrada con un programa adentro (aquí, MySQL) que funciona igual en cualquier computador. Los datos viven en un **volumen**, que es como un disco externo conectado a la caja: puedes tirar la caja y los datos siguen.

## Las piezas

| Pieza | Qué es | Dónde está |
| --- | --- | --- |
| Servicio `db` | La caja de MySQL 8.4 | `docker-compose.yml` |
| Volumen `mysql_data` | Donde se guardan los datos | Lo administra Docker |
| Script de inicio | Crea el usuario limitado `tecnofactory_app` | `docker/mysql-init/01-crear-usuario-app.sh` |
| `.env` | Las claves que lee Docker Compose | Raíz del proyecto (**no va a Git**) |

## Los tres usuarios de MySQL

| Usuario | Lo usa | Puede |
| --- | --- | --- |
| `root` | Tú; en local también la app | Todo |
| `tecnofactory_migrator` | Flyway (prod) | Crear y modificar tablas |
| `tecnofactory_app` | La API en producción | Solo `SELECT`, `INSERT`, `UPDATE`, `DELETE` |

**Por qué dos usuarios en producción:** si alguien explota una inyección SQL, entra como `tecnofactory_app` y **no puede** hacer `DROP TABLE`.

## Cómo se usa

```bash
docker compose up -d        # encender
docker compose ps           # debe decir "healthy"
docker compose stop         # apagar (conserva los datos)
docker compose down         # quitar el contenedor (conserva los datos)
docker compose down -v      # quitar contenedor Y datos (solo desarrollo)
```

| Quiero... | Comando |
| --- | --- |
| Entrar a MySQL como root | `docker exec -it tecnofactory-db mysql -uroot -p tecnofactory` |
| Ver los logs de MySQL | `docker compose logs -f db` |
| Hacer un respaldo | `docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql` |

## Reglas que no hay que olvidar

1. **El script de inicio y las claves `MYSQL_*` solo se aplican al crear el volumen.** Cambiar una clave en el `.env` después no cambia la de MySQL.
2. **`down -v` borra los datos.** Nunca en producción.
3. **El volumen no es un respaldo.** Los respaldos se hacen con `mysqldump` y se guardan fuera del servidor.
4. **Sin `$`, `'` ni `"` en las claves**: Compose y el script SQL los interpretan.
5. En producción se **quita** el mapeo `ports: "3307:3306"`.

## Si algo falla

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| `docker compose ps` no dice `healthy` | MySQL todavía arranca | Espera unos segundos y revisa `docker compose logs db` |
| `Access denied for user` | Clave distinta a la del volumen | Ver regla 1: `ALTER USER` o recrear el volumen en desarrollo |
| `Communications link failure` desde la app | MySQL apagado | `docker compose up -d` |

## Para saber más

[Referencia 02: Docker y MySQL](../referencia/02-docker-y-mysql.md) · [Guía 03: variables y claves](03-variables-y-claves.md) · [Guía 06: ejecutar](06-ejecutar-local-y-prod.md)
