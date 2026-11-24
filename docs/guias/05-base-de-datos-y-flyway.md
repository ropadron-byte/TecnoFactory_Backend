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
| `V4__blog_fecha_creacion.sql` | Agrega `blog.fecha_creacion` |
| `V5__imagen_portada_y_galeria.sql` | Una imagen puede tener varios dueños: crea `blog_imagen`, `producto_imagen`, `categoria_imagen` y la portada del blog |
| `V6__contacto.sql` | Crea la tabla `contacto` |
| `V7__unicidad_region_comuna.sql` | Agrega `UNIQUE` a `region.nombre_region` y `comuna.nombre_comuna` (las entidades ya lo declaraban) |
| `V8__google_auth.sql` | Agrega `usuario.google_id` (único) y permite `contrasenia NULL` para las cuentas creadas solo con Google |
| `V9__catalogo_inicial.sql` | Inserta el catálogo de la tienda: 21 productos, 6 categorías (agrega `Audio` y `Smartphones`) y 63 URLs de imagen. No crea tablas |

El administrador **no** está en ninguna migración: se crea por código al arrancar ([guía 12](12-administrador-inicial.md)).

## Las tablas

| Grupo | Tablas |
| --- | --- |
| Geografía y usuarios | `region`, `comuna`, `tipo_usuario`, `usuario` |
| Catálogo | `producto`, `categoria`, `producto_categoria`, `imagen_url` |
| Compras | `carrito`, `detalle_carrito`, `boleta`, `detalle_boleta` |
| Contenido | `blog`, `contacto` |
| Relaciones de imágenes (V5) | `blog_imagen`, `producto_imagen`, `categoria_imagen` |

Son **17 tablas de negocio** (13 de `V1` + 3 de `V5` + `contacto` de `V6`), más `flyway_schema_history`.
`V9` no agrega tablas: solo datos. Las 9 migraciones se aplican en orden en arranques nuevos.

Dos ideas de diseño importantes:

- **`detalle_boleta` guarda una copia congelada** del nombre y el precio del producto: si mañana el precio cambia, la boleta antigua no.
- **Las boletas nunca se borran**, y un producto vendido se **desactiva** (`activo = FALSE`), no se elimina.

## Cómo se usa

Crear una migración nueva son tres pasos, sin ningún comando:

1. Crear `src/main/resources/db/migration/V10__descripcion.sql` (las versiones 1 a 9 ya existen).
2. Escribir el SQL.
3. Arrancar la app: Flyway aplica lo que falte.

El nombre importa: `V` mayúscula + número + **dos** guiones bajos + descripción. Con un guion bajo o una `v` minúscula, Flyway **ignora** el archivo sin avisar.

Ver el historial:

```bash
docker exec -it tecnofactory-db mysql -utecnofactory_migrator -p tecnofactory \
  -e "SELECT version, description, success FROM flyway_schema_history;"
```

## Trampa al desplegar: el SQL viaja dentro del JAR

Escribir `V10__...sql` en tu computador **no basta** para que producción lo aplique. El `Dockerfile` de la raíz hace `COPY src ./src` y compila el JAR con Maven **dentro de Docker**; las migraciones de `src/main/resources/db/migration` quedan **dentro del JAR** que se mete en la imagen `tecnofactory-backend:latest`. En el contenedor no se monta esa carpeta: el backend monta solo `uploads/` y MySQL monta `docker/mysql-init`, nunca `db/migration`.

Por eso `docker compose -f docker-compose.prod.yml up -d` **sin `--build`** reutiliza la imagen antigua: Flyway solo ve las migraciones que tenía el JAR viejo. La forma correcta de levantar o actualizar producción es:

```bash
docker compose -f docker-compose.prod.yml up -d --build --wait --wait-timeout 900
```

`--build` es obligatorio porque reconstruye el JAR desde `src/` y mete los SQL nuevos en la imagen. En eva03 (1 CPU y ~1 GiB) el primer build tarda unos minutos —no lo canceles—; el `Dockerfile` limita la memoria de Maven y guarda `target/` en caché de BuildKit para que los siguientes sean más rápidos. Ver [referencia 07](../referencia/07-ejecucion-local-y-prod.md#1-dockerfile-en-la-raíz) y [guía 20](20-despliegue-frontend-vercel.md).

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
| En prod Flyway se queda en `V9` aunque en el host haya un SQL nuevo | Se levantó con `up -d` sin `--build`: la imagen antigua no trae ese SQL. Usa el comando de arriba |

## Para saber más

[Referencia 05: Flyway](../referencia/05-flyway-y-migraciones.md) · [Referencia 06: esquema completo y mapeo a entidades](../referencia/06-base-de-datos.md)
