# 06. Base de datos

## Herramienta de diseño

El modelo se diseña en **SQL Developer Data Modeler**, que no tiene MySQL como destino. El script exportado es por tanto genérico y **debe adaptarse** antes de ser la migración definitiva. Alternativa con exportación directa a MySQL: **MySQL Workbench**.

## Estado actual de `V1__esquema_inicial.sql`

Crea 13 tablas, motor InnoDB y `utf8mb4`.

```
REGION 1 ──< COMUNA 1 ──< USUARIO >── 1 TIPO_USUARIO
                           │
        ┌──────────────────┼───────────────┐
        │ 1                │ 1             │ 1
     CARRITO (1:1)      BOLETA           BLOG
        │ 1              │ 1               │ 1
        │                │                 │
  DETALLE_CARRITO   DETALLE_BOLETA      IMAGEN_URL (opcional)
        │ N
        └── PRODUCTOS >──< CATEGORIA   (tabla PRODUCTO_CATEGORIA)
               │ 1            │ 1
               └── IMAGEN_URL ┘  (cada imagen: producto, categoría o blog)
```

| Tabla | Propósito | Claves |
| --- | --- | --- |
| `REGION`, `COMUNA` | Geografía de Chile para las direcciones | `COMUNA.REGION_id_region` → `REGION` |
| `TIPO_USUARIO` | Roles (administrador, vendedor, cliente...) | PK `id_tipo_usuario` |
| `USUARIO` | Cuentas: RUN, nombre, correo, contraseña (hash), comuna, tipo | FK a `COMUNA` y `TIPO_USUARIO` |
| `PRODUCTOS`, `CATEGORIA` | Catálogo | `PRODUCTO_CATEGORIA` (muchos a muchos) |
| `IMAGEN_URL` | URLs de imágenes de productos, categorías o blog | 3 FK opcionales |
| `CARRITO`, `DETALLE_CARRITO` | Carrito (uno por usuario) y sus líneas | `CARRITO__IDX` único por usuario |
| `BOLETA`, `DETALLE_BOLETA` | Compras realizadas | `BOLETA` → `USUARIO` |
| `BLOG` | Publicaciones | `BLOG` → `USUARIO` |

Todas las claves foráneas usan `ON DELETE RESTRICT ON UPDATE CASCADE`: no se puede borrar un registro que otro referencia.

**Verificado:** el script se aplica sin errores desde cero con `tecnofactory_migrator`.

## Observaciones sobre el script actual

Son los puntos a resolver **antes de escribir las entidades**. Mientras `V1` no se haya desplegado en un servidor, puedes corregirla directamente (`down -v` para recrear). Después ya sería `V2`.

### Importantes (bloquean o complican las entidades)

| # | Observación | Consecuencia | Qué hacer |
| --- | --- | --- | --- |
| 1 | **Ningún `id` tiene `AUTO_INCREMENT`** | Con `@GeneratedValue(strategy = IDENTITY)` los inserts fallan: MySQL exige el id | Agregar `AUTO_INCREMENT` a las PK numéricas, o generar ids a mano (no recomendado) |
| 2 | **Nombres en MAYÚSCULAS** (`PRODUCTOS`, `USUARIO`) | En Linux MySQL distingue mayúsculas en tablas; la estrategia de nombres por defecto de Spring Boot convierte a minúsculas y **la tabla no se encuentra** | Pasar tablas y columnas a minúsculas con guion bajo (`producto`, `detalle_boleta`), o cambiar la estrategia de nombres |
| 3 | **`stockCritico` en camelCase** | Hibernate lo busca como `stock_critico`: falla `validate` | Renombrar a `stock_critico` |
| 4 | **`DETALLE_CARRITO` y `PRODUCTO_CATEGORIA` sin clave primaria** | JPA necesita identificar cada fila; las relaciones muchos a muchos pierden integridad | PK compuesta: `(carrito_id, producto_id)` y `(producto_id, categoria_id)` |
| 5 | **`DECIMAL(n,0)` en ids y cantidades** (`id_comuna`, `id_region`, `cantidad`, `descuento`, `run`, `telefono`) | En Java se mapean como `BigDecimal`, no `Integer`/`Long`; `validate` falla si la entidad usa otro tipo | `INT`/`SMALLINT` para ids y cantidades; descuento como `TINYINT` 0-100 |
| 6 | **`DETALLE_BOLETA` sin `producto_id` ni `cantidad`** | No se sabe cuántas unidades se compraron, ni de qué producto | Agregar `cantidad` y `producto_id` (o al menos `nombre_producto`) |
| 7 | **`BLOG.id_blog` es `VARCHAR(255)` como PK** | Ineficiente y sin autoincremento; `IMAGEN_URL` lo repite como FK | `BIGINT AUTO_INCREMENT` |

### Recomendadas

| # | Observación | Qué hacer |
| --- | --- | --- |
| 8 | `correo` y `run` sin `UNIQUE` | Agregar `UNIQUE` en `correo` y `(run, run_dv)`: dos cuentas con el mismo correo rompen el login |
| 9 | `IMAGEN_URL` permite tener los tres FK nulos, o dos a la vez | Añadir `CHECK` (MySQL 8.0.16+) que exija exactamente uno, o separar en tablas |
| 10 | `fecha_nacimiento` es `DATETIME` | `DATE` |
| 11 | `telefono DECIMAL(11,0)` | `VARCHAR(15)`: puede llevar `+` y no se opera con él |
| 12 | `BOLETA.estado` (BOOLEAN) y `cantidad_productos` | Si el pago es inmediato (simulado), el estado sobra; `cantidad_productos` se calcula desde los detalles |
| 13 | `DETALLE_BOLETA.descripcion` NOT NULL de 1000 caracteres | Basta con `nombre` y `precio_unitario` como copia congelada de lo vendido |
| 14 | `ON DELETE RESTRICT` en todo | Para `DETALLE_CARRITO → CARRITO` y `DETALLE_BOLETA → BOLETA` conviene `CASCADE`: borrar el padre borra sus líneas |
| 15 | Nombres de restricciones genéricos (`BLOG_USUARIO_FK`) | Convención `fk_<hija>_<padre>`; en MySQL deben ser únicos en toda la base |
| 16 | `descripcion` de producto opcional (500) | Evalúa `TEXT` si las descripciones serán largas |
| 17 | Sin `activo`/`deleted_at` en `PRODUCTOS` | Para productos ya vendidos, mejor **desactivar** que borrar |

### Ejemplo de la forma recomendada

```sql
CREATE TABLE producto (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(255) NOT NULL,
    descripcion    VARCHAR(500),
    precio         BIGINT       NOT NULL,           -- pesos chilenos, sin decimales
    stock          INT          NOT NULL DEFAULT 0,
    stock_critico  INT          NOT NULL DEFAULT 0,
    descuento      TINYINT      NOT NULL DEFAULT 0, -- 0 a 100
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_producto PRIMARY KEY (id),
    CONSTRAINT ck_producto_descuento CHECK (descuento BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE producto_categoria (
    producto_id  BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    CONSTRAINT pk_producto_categoria PRIMARY KEY (producto_id, categoria_id),
    CONSTRAINT fk_producto_categoria_producto  FOREIGN KEY (producto_id)  REFERENCES producto (id),
    CONSTRAINT fk_producto_categoria_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Convenciones de diseño

### Tipos de datos

| Dato | Tipo MySQL | Tipo en Java |
| --- | --- | --- |
| Clave primaria | `BIGINT NOT NULL AUTO_INCREMENT` (sin `UNSIGNED`) | `Long` |
| Clave foránea | Mismo tipo exacto que la clave a la que apunta | `Long` |
| Nombre, correo | `VARCHAR(255)` | `String` |
| Descripción corta / larga | `VARCHAR(500)` / `TEXT` | `String` |
| URL de imagen | `VARCHAR(500)` (hasta 2048 si aceptas URLs externas largas) | `String` |
| Precio en pesos chilenos | `BIGINT` | `Long` |
| Precio con decimales | `DECIMAL(12,2)` | `BigDecimal` |
| Fecha y hora / solo fecha | `DATETIME` / `DATE` | `LocalDateTime` / `LocalDate` |
| Verdadero o falso | `BOOLEAN` | `Boolean` |
| Contraseña | `VARCHAR(255)` (hash BCrypt, nunca el texto) | `String` |

Nunca `FLOAT`/`DOUBLE` para dinero. `TEXT` no admite `DEFAULT` ni índice completo.

### Nombres

- Minúsculas con guion bajo (`detalle_boleta`); en Linux las mayúsculas importan.
- Clave primaria: `id`. Clave foránea: `<tabla_padre>_id`.
- Evita palabras reservadas: `order`, `group`, `key`, `index`, `rank`, `condition`.
- Restricciones: `pk_<tabla>`, `fk_<hija>_<padre>`, `uq_<tabla>_<columna>`, `idx_<tabla>_<columna>`.

### Relaciones

| Tipo | Cómo se resuelve |
| --- | --- |
| Uno a muchos | FK en la tabla "muchos" |
| Muchos a muchos | Tabla intermedia con dos FK y PK compuesta |
| Muchos a muchos con datos propios | La intermedia pasa a ser una entidad con columnas (`detalle_boleta`) |

### Carrito y boleta (decisiones de diseño)

- Cada usuario tiene **un solo carrito permanente**; al pagar se vacían sus detalles.
- `detalle_carrito` **no guarda precio**: muestra el precio actual del producto.
- La boleta es un documento histórico y se une al **usuario**, no al carrito.
- `detalle_boleta` guarda una **copia** del nombre y precio: la boleta antigua conserva lo que se pagó aunque el producto cambie.
- El pago ocurre en **una sola transacción** (`@Transactional`): crear la boleta, copiar los detalles, vaciar el carrito.

| FK | Regla de borrado | Razón |
| --- | --- | --- |
| `carrito` → `usuario` | `CASCADE` | Sin usuario, el carrito no sirve |
| `detalle_carrito` → `carrito` | `CASCADE` | Vaciar el carrito borra sus líneas |
| `detalle_boleta` → `boleta` | `CASCADE` | Las líneas pertenecen a la boleta |
| `boleta` → `usuario` | `RESTRICT` | Un registro de compras no debe desaparecer con el usuario |
| `detalle_boleta` → `producto` | `RESTRICT` | No borres productos vendidos: desactívalos |

### Imágenes

La URL **no** es una clave: la clave es el `id`. Una imagen pertenece a un solo dueño. `IMAGEN_URL` con tres FK opcionales (producto, categoría, blog) es válido, pero exige el `CHECK` de la observación 9 para evitar filas huérfanas o ambiguas.

### Vistas

Se evaluó y **no se usan por ahora**: la API usa un solo usuario de MySQL, así que la base no sabe si quien consulta es cliente o administrador; eso lo decide Spring Security. La protección real viene de los DTOs y del usuario `tecnofactory_app` de permisos mínimos.

## Entidades: reglas que dependen de este esquema

- Con `ddl-auto: validate`, cada entidad debe coincidir en nombre de tabla, columnas y tipos.
- Las entidades **no se devuelven por la API**: se convierten a DTO.
- Si se queda `TIPO_USUARIO` como tabla, el rol del JWT sale de `nombre_tipo_usuario`; si se prefiere un `enum`, hay que decidirlo antes de escribir la entidad (ver [10-pendientes-y-futuro.md](10-pendientes-y-futuro.md)).
