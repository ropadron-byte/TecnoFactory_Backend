# Guía 05. Base de datos y Flyway

> **En una frase:** las tablas se crean con archivos SQL numerados (Flyway), no con las entidades; la base de datos manda y las entidades se adaptan.

## La idea

Flyway es como el **historial de obras de un edificio**: cada migración es un plano nuevo que se apila sobre los anteriores. No se borra ni se corrige un plano ya construido; si hay un error, se hace un plano nuevo que lo repare.

## Las piezas

| Migración | Qué hace |
| --- | --- |
| `V1__esquema_inicial.sql` | Crea las 13 tablas, sus restricciones y 15 claves foráneas |
| `V2__datos_iniciales.sql` | Inserta 3 roles (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`), 16 regiones y 346 comunas |
| `V3__categorias_base.sql` | Categorías base |

El administrador **no** está en ninguna migración: se crea por código al arrancar ([guía 12](12-administrador-inicial.md)).

## Las 13 tablas

| Grupo | Tablas |
| --- | --- |
| Geografía y usuarios | `region`, `comuna`, `tipo_usuario`, `usuario` |
| Catálogo | `producto`, `categoria`, `producto_categoria`, `imagen_url` |
| Compras | `carrito`, `detalle_carrito`, `boleta`, `detalle_boleta` |
| Contenido | `blog` |

Dos ideas de diseño importantes:

- **`detalle_boleta` guarda una copia congelada** del nombre y el precio del producto: si mañana el precio cambia, la boleta antigua no.
- **Las boletas nunca se borran**, y un producto vendido se **desactiva** (`activo = FALSE`), no se elimina.

## Cómo se usa

Crear una migración nueva son tres pasos, sin ningún comando:

1. Crear `src/main/resources/db/migration/V4__descripcion.sql`.
2. Escribir el SQL.
3. Arrancar la app: Flyway aplica lo que falte.

El nombre importa: `V` mayúscula + número + **dos** guiones bajos + descripción. Con un guion bajo o una `v` minúscula, Flyway **ignora** el archivo sin avisar.

Ver el historial:

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory \
  -e "SELECT version, description, success FROM flyway_schema_history;"
```

## Reglas que no hay que olvidar

1. **Una migración aplicada no se edita nunca más** (daría `Migration checksum mismatch`). Cambio nuevo = versión nueva.
2. **Excepción de desarrollo:** si solo existe en tu máquina, `docker compose down -v` y vuelve a empezar.
3. **MySQL no deshace cambios de estructura a medias**: mantén cada migración pequeña.
4. **Una entidad nueva exige una migración equivalente**, y viceversa. Con `validate`, si no calzan, la app no arranca.
5. **No cambies tablas a mano** (Workbench, DBeaver): se desincronizan de los archivos.
6. **Busca los roles por nombre**, nunca por id: los ids los asigna `AUTO_INCREMENT`.

## Si algo falla

| Mensaje | Qué significa |
| --- | --- |
| `Migration checksum mismatch` | Editaste una migración ya aplicada |
| `Detected failed migration` | Una migración anterior falló a medias |
| La migración no corre y no hay error | Nombre mal escrito o carpeta equivocada |
| `Schema-validation: missing column` | Una entidad nombra una columna que no existe en `V1` |

## Para saber más

[Referencia 05: Flyway](../referencia/05-flyway-y-migraciones.md) · [Referencia 06: esquema completo y mapeo a entidades](../referencia/06-base-de-datos.md)
