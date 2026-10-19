# 05. Flyway y migraciones

## Qué es y qué no es

Flyway **no crea la base de datos**: la crea MySQL (`MYSQL_DATABASE`). Flyway **crea y modifica las tablas** dentro de ella a partir de archivos SQL versionados, y registra qué archivos ya aplicó en la tabla `flyway_schema_history`.

Tu idea de generar las tablas desde las entidades (`ddl-auto: update`) sirve para prototipar, pero no deja historial, no se puede revisar en Git y puede alterar datos reales. Por eso se usa Flyway y Hibernate queda en `validate`.

## Cómo se usa

No hay ningún comando. Haces tres cosas:

1. Creas un archivo `V<número>__<descripcion>.sql` en `src/main/resources/db/migration`.
2. Escribes tu SQL dentro.
3. Arrancas la aplicación. Flyway aplica lo que falte y lo registra.

El log lo confirma (en prod solo se ve si el nivel de Flyway permite `INFO`; si no, consulta la tabla):

```
Successfully applied 1 migration to schema `tecnofactory`
```

## El nombre del archivo

```
V1__esquema_inicial.sql
│ │ │
│ │ └─ descripción (guiones bajos en vez de espacios)
│ └─── dos guiones bajos (obligatorio)
└───── V mayúscula + número
```

Con un solo guion bajo o una `v` minúscula, Flyway **ignora** el archivo sin avisar.

## Cómo funciona el versionamiento

Las versiones **no son alternativas**: son capas apiladas. La base final es el resultado de aplicar V1, luego V2, luego V3. Flyway gratuito **no deshace** migraciones: si algo está mal, se corrige hacia adelante con una versión nueva.

Al arrancar, Flyway:

1. Mira `flyway_schema_history`.
2. Compara los archivos aplicados con sus **checksums** (huella del contenido).
3. Aplica los archivos pendientes en orden.
4. Si un archivo ya aplicado cambió, **se detiene** con `Migration checksum mismatch`.

## Cuándo editar y cuándo crear una versión nueva

| Situación | Qué hacer |
| --- | --- |
| `V1` y `V2` solo existen en tu base de desarrollo | Puedes editarlas: `docker compose down -v`, `up -d` y arrancar de nuevo |
| `V1` o `V2` ya se aplicaron en un servidor o las usa otra persona | **No se editan nunca más.** Cambio nuevo = `V3__...` |
| Necesitas más datos de referencia | Una migración nueva (`V4__...` en adelante: `V1`, `V2` y `V3` ya existen; el administrador inicial **no** se hace con una migración, se crea por código al arrancar) |
| Un cambio de entidad | Una migración equivalente en SQL, y viceversa: con `validate` deben coincidir |

## Reglas al escribir migraciones

| Regla | Por qué |
| --- | --- |
| Crea primero las tablas que otras referencian | Una FK a una tabla inexistente falla |
| Nombra las restricciones (`fk_producto_categoria`) | Más fácil de modificar después |
| Mantén cada migración pequeña | **MySQL no deshace cambios de estructura** si algo falla a medias: lo ya ejecutado queda aplicado |
| Los tipos deben coincidir con las entidades | Con `validate`, si no calzan la app no arranca |
| Datos iniciales en una migración aparte | Separa estructura estable de datos |
| Contraseñas iniciales (admin) ya hasheadas con BCrypt | Nunca texto plano en Git. El administrador **no** va en `V2`: se crea aparte (ver [Cambios recientes](cambios-recientes.md#qué-queda-fuera-de-v2-a-propósito)) |
| Datos de referencia con id fijo (regiones, comunas) | Se insertan con su id explícito (CUT); no dependas de `AUTO_INCREMENT` |
| Datos de referencia con `AUTO_INCREMENT` (`tipo_usuario`) | En el código **busca por nombre** (`ADMINISTRADOR`), nunca asumas que el id es 1, 2 o 3 |
| No cambies tablas a mano (Workbench, DBeaver) | La base y los archivos se desincronizan |
| No mezcles Hibernate `update` con Flyway | Se pisan |
| Guarda los archivos en UTF-8 | Evita problemas con tildes y la ñ |

`validate` comprueba que tablas, columnas y tipos coincidan con las entidades. **No** comprueba índices, claves únicas ni claves foráneas: esas dependen solo de tu SQL.

## Qué pasa si una migración falla a medias

MySQL confirma cada sentencia DDL al ejecutarla. Si la sentencia 5 de 10 falla, las 4 anteriores quedan aplicadas y Flyway marca la migración como fallida (`success = 0`). Para arreglarlo:

- **Desarrollo:** `docker compose down -v` y vuelve a empezar.
- **Producción:** restaura el respaldo, o repara a mano y ejecuta `flyway repair`. Por eso se prueba **siempre** con una copia antes de desplegar.

## Cómo ver el historial

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory \
  -e "SELECT version, description, success FROM flyway_schema_history;"
```

Resultado de la prueba del 16 oct 2026: versión `1`, descripción `esquema inicial`, `success = 1`.

## Migraciones del proyecto

| Versión | Archivo | Qué hace |
| --- | --- | --- |
| 1 | `V1__esquema_inicial.sql` | Crea las 13 tablas, sus restricciones y claves foráneas |
| 2 | `V2__datos_iniciales.sql` | Inserta 3 tipos de usuario, 16 regiones y 346 comunas |

| Dato | Valor |
| --- | --- |
| Tablas tras migrar | 13 de negocio + `flyway_schema_history` |
| Usuario que migra (prod) | `tecnofactory_migrator` |
| Usuario que lee/escribe luego | `tecnofactory_app` |

La prueba del 16 oct 2026 en MySQL 8.4 se hizo con el `V1` original. Tras los cambios posteriores, `V1` y `V2` se aplicaron sin errores en una base de prueba MariaDB 10.11; **falta repetir la prueba en tu MySQL 8.4 de Docker** (ver la lista de verificación en [Cambios recientes](cambios-recientes.md#cómo-verificar-todo-en-tu-máquina)).

## Errores típicos

| Mensaje | Qué significa |
| --- | --- |
| `Migration checksum mismatch` | Editaste una migración ya aplicada |
| `Found non-empty schema(s) ... but no schema history table` | La base tenía tablas antes que Flyway (por ejemplo, creadas por Hibernate) |
| `Detected failed migration` | Una migración anterior falló a medias |
| `Unsupported Database: MySQL 8.4` | Falta el módulo `flyway-mysql` en el `pom.xml` (ya está) |
| La migración no se ejecuta y no hay error | Nombre mal escrito (`V1_` con un guion bajo, o `v1__`) o carpeta incorrecta |
