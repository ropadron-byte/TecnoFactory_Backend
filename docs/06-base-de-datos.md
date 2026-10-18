# 06. Base de datos

## Fuente de verdad

La **fuente de verdad del esquema es `V1__esquema_inicial.sql`** (más los datos de `V2`). El diagrama de `Diagrama/` (SQL Developer Data Modeler) quedó **desactualizado**: todavía muestra `PRODUCTOS`, `stockCritico` y `cantidad_productos`, y no tiene `activo` ni `producto_id` en el detalle de boleta. Si lo necesitas para una entrega, actualízalo a partir de la base real; no al revés.

> El Data Modeler no tiene MySQL como destino, por eso el script exportado se adaptó a mano. Alternativa con exportación directa a MySQL: **MySQL Workbench**.

## Estado actual de `V1__esquema_inicial.sql`

13 tablas, motor InnoDB, `utf8mb4`, 15 claves foráneas.

```
REGION 1 ──< COMUNA 1 ──< USUARIO >── 1 TIPO_USUARIO
                           │ 1
        ┌──────────────────┼──────────────┐
        │                  │              │
     CARRITO (1:1)      BOLETA           BLOG
        │ 1               │ 1             │ 1
        │ N               │ N             │ N
  DETALLE_CARRITO    DETALLE_BOLETA     IMAGEN_URL

DETALLE_CARRITO y DETALLE_BOLETA >── 1 PRODUCTO >──< CATEGORIA
                                     (tabla intermedia PRODUCTO_CATEGORIA)

IMAGEN_URL: cada imagen pertenece a UN producto, UNA categoría o UN blog
```

| Tabla | Propósito | Claves y restricciones |
| --- | --- | --- |
| `region`, `comuna` | Geografía de Chile | `comuna.region_id_region` → `region`. Ids **manuales** (sin `AUTO_INCREMENT`) |
| `tipo_usuario` | Roles | `nombre_tipo_usuario` único |
| `usuario` | Cuentas | `run` y `correo` únicos; FK a `comuna` y `tipo_usuario`; `activo` por defecto `TRUE` |
| `producto` | Catálogo | `CHECK` en precio, stock, stock crítico y descuento (0 a 100); `activo` por defecto `TRUE` |
| `categoria` | Categorías | `nombre_categoria` único |
| `producto_categoria` | Muchos a muchos | PK compuesta `(producto, categoria)` |
| `imagen_url` | URLs de imágenes | 3 FK opcionales + `CHECK` que exige **exactamente un** dueño |
| `carrito` | Un carrito por usuario | `usuario_id_usuario` único |
| `detalle_carrito` | Líneas del carrito | PK compuesta `(carrito, producto)`; `cantidad >= 1` |
| `boleta` | Compras | `total_boleta >= 0`; `estado` por defecto `TRUE` |
| `detalle_boleta` | Líneas de la boleta | Copia congelada de `nombre`, `precio_unitario`, `descuento`, `cantidad` **más** `producto_id_producto` |
| `blog` | Publicaciones | `visible` por defecto `TRUE` |

### Reglas de borrado (`ON DELETE`)

Ninguna FK define `ON UPDATE`, así que no se puede cambiar un id que otra fila referencia (los ids no cambian).

| FK | Regla | Razón |
| --- | --- | --- |
| `comuna` → `region` | `RESTRICT` | Datos de referencia |
| `usuario` → `comuna`, `tipo_usuario` | `RESTRICT` | Datos de referencia |
| `boleta` → `usuario` | `RESTRICT` | El historial de compras no desaparece con el usuario |
| `blog` → `usuario` | `RESTRICT` | No se borra un usuario con publicaciones |
| `detalle_boleta` → `boleta` | `RESTRICT` | **Las boletas no se borran nunca**; no tiene sentido borrarlas en cascada |
| `detalle_boleta` → `producto` | `RESTRICT` | No borres productos vendidos: desactívalos (`activo = FALSE`) |
| `carrito` → `usuario` | `CASCADE` | Sin usuario, el carrito no sirve |
| `detalle_carrito` → `carrito` | `CASCADE` | Vaciar el carrito borra sus líneas |
| `detalle_carrito` → `producto` | `RESTRICT` | No se borra un producto que está en un carrito |
| `producto_categoria` → `producto` | `CASCADE` | Al borrar el producto se borran sus vínculos |
| `producto_categoria` → `categoria` | `RESTRICT` | No se borra una categoría con productos |
| `imagen_url` → `producto`, `categoria`, `blog` | `CASCADE` | La imagen pertenece a su dueño |

### Nombres reales (convención del proyecto)

Los nombres vienen del modelador y **se mantienen**:

| Elemento | Patrón | Ejemplo |
| --- | --- | --- |
| Tabla | minúsculas con guion bajo | `detalle_boleta` |
| Clave primaria | `id_<tabla>` | `id_producto` |
| Clave foránea (columna) | `<tabla_padre>_<pk_padre>` | `usuario_id_usuario`, `comuna_id_comuna` |
| Restricción PK | `<tabla>_PK` | `producto_PK` |
| Restricción FK | `<hija>_<padre>_FK` | `detalle_boleta_producto_FK` |
| `CHECK` | `chk_<tabla>_<campo>` | `chk_producto_descuento` |

En MySQL el nombre de una restricción FK debe ser **único en toda la base**.

## Observaciones del script original: estado

Las 17 observaciones de la revisión inicial quedaron así:

| # | Observación | Estado |
| --- | --- | --- |
| 1 | Sin `AUTO_INCREMENT` | Resuelto (excepto `region` y `comuna`, a propósito) |
| 2 | Nombres en MAYÚSCULAS | Resuelto: todo en minúsculas |
| 3 | `stockCritico` | Resuelto: `stock_critico` |
| 4 | Tablas sin PK | Resuelto: PK compuestas en `detalle_carrito` y `producto_categoria` |
| 5 | `DECIMAL` en ids y cantidades | Resuelto: `INT` / `BIGINT` |
| 6 | `detalle_boleta` sin producto ni cantidad | Resuelto: `producto_id_producto` y `cantidad` |
| 7 | `blog.id_blog` `VARCHAR` | Resuelto: `BIGINT AUTO_INCREMENT` |
| 8 | `correo` y `run` sin `UNIQUE` | Resuelto (`run_dv` no entra: `run` ya es único) |
| 9 | `imagen_url` ambigua | Resuelto: `CHECK chk_imagen_dueno` |
| 10 | `fecha_nacimiento` `DATETIME` | Resuelto: `DATE` |
| 11 | `telefono` `DECIMAL` | Resuelto: `VARCHAR(15)` |
| 12 | `boleta.estado` y `cantidad_productos` | `estado` se mantiene con `DEFAULT TRUE`; `cantidad_productos` se eliminó (se calcula desde los detalles) |
| 13 | `detalle_boleta.descripcion` | Eliminada |
| 14 | `ON DELETE` | Decidido caso por caso (tabla de arriba) |
| 15 | Nombres de restricciones | Se mantiene la convención del modelador |
| 16 | `descripcion` de producto | Se mantiene `VARCHAR(500)` |
| 17 | Sin `activo` en producto | Resuelto: `producto.activo` |

## Datos iniciales: `V2__datos_iniciales.sql`

| Tabla | Filas | Id | Notas |
| --- | --- | --- | --- |
| `tipo_usuario` | 3 | `AUTO_INCREMENT` | `ADMINISTRADOR`, `VENDEDOR`, `CLIENTE` |
| `region` | 16 | Número de región (Ñuble = 16, Arica y Parinacota = 15) | |
| `comuna` | 346 | Código **CUT** de SUBDERE (Santiago = 13101, Valparaíso = 5101) | Los códigos de las regiones I a IX no llevan cero a la izquierda porque la columna es `INT` |

No incluye el administrador ni categorías: ver [Cambios recientes](cambios-recientes.md#qué-queda-fuera-de-v2-a-propósito).

## Mapeo a entidades Java

Con `ddl-auto: validate`, cada entidad debe coincidir con la tabla. Spring Boot convierte `camelCase` a `snake_case` solo (`stockCritico` → `stock_critico`), pero las claves `id_<tabla>` y las FK necesitan `@Column(name = ...)` o `@JoinColumn(name = ...)` explícitos.

| Tipo MySQL | Tipo Java | Dónde |
| --- | --- | --- |
| `INT` | `Integer` | Ids de `region` y `comuna`, `run`, `descuento`, `cantidad` |
| `BIGINT` | `Long` | Ids restantes, `precio`, `stock`, `stock_critico`, `precio_unitario`, `total_boleta` |
| `VARCHAR` | `String` | Textos |
| `BOOLEAN` | `Boolean` | `activo`, `visible`, `estado` |
| `DATE` / `DATETIME` | `LocalDate` / `LocalDateTime` | `fecha_nacimiento` / `boleta.fecha` |
| `CHAR(1)` | `String` + `@JdbcTypeCode(SqlTypes.CHAR)` | `usuario.run_dv` (**validado** en MySQL 8.4) |
| `TEXT` | `String` | `blog.contenido` |

| Tabla | Entidad | Notas de mapeo |
| --- | --- | --- |
| `region` | `Region` | `Integer` sin `@GeneratedValue` (id asignado) |
| `comuna` | `Comuna` | `Integer` sin `@GeneratedValue`; `@ManyToOne Region` |
| `tipo_usuario` | `TipoUsuario` | `Long`, `IDENTITY` |
| `usuario` | `Usuario` | `@ManyToOne` a `TipoUsuario` y `Comuna`; `activo = true` por defecto en Java |
| `categoria` | `Categoria` | `Long`, `IDENTITY` |
| `producto` | `Producto` | `@ManyToMany` a `Categoria` con `@JoinTable(name = "producto_categoria", ...)` (no lleva entidad propia) |
| `imagen_url` | `ImagenUrl` | 3 `@ManyToOne` opcionales; solo uno tiene valor |
| `blog` | `Blog` | `@ManyToOne Usuario` |
| `carrito` | `Carrito` | `@OneToOne Usuario` |
| `detalle_carrito` | `DetalleCarrito` | **Clave compuesta**: `@EmbeddedId` con `@MapsId` |
| `boleta` | `Boleta` | `estado = true` por defecto en Java |
| `detalle_boleta` | `DetalleBoleta` | `@ManyToOne` a `Boleta` y `Producto` |

**Puntos a vigilar con `validate`:** `CHAR(1)` y `TEXT` son los tipos con más riesgo de que Hibernate se queje (`wrong column type`). Si ocurre, se resuelve en la entidad (por ejemplo con `@JdbcTypeCode`), **no** cambiando el esquema. `CHAR(1)` ya está resuelto en `Usuario.run_dv` (ver [13](13-entidades-explicadas.md#63-explicación-de-jdbctypecodesqltypeschar)); `TEXT` (`blog.contenido`) queda por comprobar cuando exista esa entidad.

**Estado de las entidades (18 oct):** existen `Region`, `Comuna`, `TipoUsuario` y `Usuario`, y las cuatro pasaron `validate`. El código y cada anotación están explicados en [13-entidades-explicadas.md](13-entidades-explicadas.md). Ahí también se anotan tres diferencias entre las entidades y este esquema (`unique = true` que `V1` no tiene, y nombres con guion bajo en `Usuario`).

**El valor por defecto de la base no se aplica si Hibernate envía `NULL` explícito.** Por eso `estado`, `activo` y `visible` también se inicializan en Java (`private Boolean activo = true;`).

## Convenciones de diseño

### Tipos de datos

| Dato | Tipo MySQL | Tipo en Java |
| --- | --- | --- |
| Clave primaria | `BIGINT NOT NULL AUTO_INCREMENT` (sin `UNSIGNED`) | `Long` |
| Clave foránea | Mismo tipo exacto que la clave a la que apunta | `Long` (o `Integer` hacia `region`/`comuna`) |
| Nombre, correo | `VARCHAR(255)` | `String` |
| Descripción corta / larga | `VARCHAR(500)` / `TEXT` | `String` |
| URL de imagen | `VARCHAR(1000)` | `String` |
| Precio en pesos chilenos | `BIGINT` | `Long` |
| Precio con decimales | `DECIMAL(12,2)` | `BigDecimal` |
| Fecha y hora / solo fecha | `DATETIME` / `DATE` | `LocalDateTime` / `LocalDate` |
| Verdadero o falso | `BOOLEAN` | `Boolean` |
| Contraseña | `VARCHAR(255)` (hash BCrypt, nunca el texto) | `String` |

Nunca `FLOAT`/`DOUBLE` para dinero. `TEXT` no admite `DEFAULT` ni índice completo. Evita palabras reservadas como nombre: `order`, `group`, `key`, `index`, `rank`, `condition`.

### Relaciones

| Tipo | Cómo se resuelve |
| --- | --- |
| Uno a muchos | FK en la tabla "muchos" |
| Muchos a muchos | Tabla intermedia con dos FK y PK compuesta (`producto_categoria`) |
| Muchos a muchos con datos propios | La intermedia pasa a ser una entidad con columnas (`detalle_carrito`, `detalle_boleta`) |

### Carrito y boleta (decisiones de diseño)

- Cada usuario tiene **un solo carrito permanente**; al pagar se vacían sus detalles.
- `detalle_carrito` **no guarda precio**: muestra el precio actual del producto.
- La boleta es un documento histórico y se une al **usuario**, no al carrito.
- `detalle_boleta` guarda una **copia** del nombre, precio y descuento, además del `producto_id_producto`: la boleta antigua conserva lo que se pagó aunque el producto cambie, y aún se sabe qué producto era.
- El pago ocurre en **una sola transacción** (`@Transactional`): crear la boleta, copiar los detalles, descontar stock, vaciar el carrito.

### Imágenes

La URL **no** es una clave: la clave es el `id_url`. Una imagen pertenece a un solo dueño; el `CHECK chk_imagen_dueno` impide filas huérfanas o con dos dueños.

### Vistas

Se evaluó y **no se usan por ahora**: la API usa un solo usuario de MySQL, así que la base no sabe si quien consulta es cliente o administrador; eso lo decide Spring Security. La protección real viene de los DTOs y del usuario `tecnofactory_app` de permisos mínimos.

## Verificación realizada

`V1` y `V2` se aplicaron **sin errores** en una base limpia de prueba (**MariaDB 10.11**, no MySQL 8.4): 13 tablas, 15 FK, 3 tipos de usuario, 16 regiones y 346 comunas. La sintaxis usada (`CHECK`, `DEFAULT`, FK) es válida en MySQL 8.4, pero **la prueba definitiva es arrancar la app contra tu contenedor MySQL 8.4** (ver [Cambios recientes](cambios-recientes.md#cómo-verificar-todo-en-tu-máquina)).

**18 oct 2026:** `V1`, `V2` y `V3` se aplicaron con Flyway en tu MySQL 8.4 (`Successfully applied 3 migrations ... now at version v3`) y las cuatro entidades actuales validaron contra ese esquema. Con eso queda confirmado en MySQL 8.4 lo que antes solo se había probado en MariaDB.

## Entidades: reglas que dependen de este esquema

- Con `ddl-auto: validate`, cada entidad debe coincidir en nombre de tabla, columnas y tipos. **No** verifica `UNIQUE`, `NOT NULL` ni largos: esos los impone la base ([13, sección 1.3](13-entidades-explicadas.md#13-qué-comprueba-ddl-auto-validate)).
- Las entidades **no se devuelven por la API**: se convierten a DTO.
- Los roles salen de `tipo_usuario.nombre_tipo_usuario`. Se decidió mantener la tabla (en vez de un `enum`) para poder agregar roles sin recompilar.
