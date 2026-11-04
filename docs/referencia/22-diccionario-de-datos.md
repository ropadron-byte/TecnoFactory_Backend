# 22. Diccionario de datos

Las 13 tablas de `V1__esquema_inicial.sql` y la tabla `contacto` (`V6__contacto.sql`), columna por columna, con sus reglas. **La base manda**: si una entidad no coincide con esto, `ddl-auto: validate` impide que la app arranque.

Estado de cada tabla en el código: ✅ tiene entidad y repository · ⏳ solo existe en la base.

## Mapa de relaciones

```
region 1──N comuna 1──N usuario N──1 tipo_usuario
                          │
        ┌─────────────────┼─────────────────┐
        1                 1                 1
        N blog            N boleta 1──N detalle_boleta N──1 producto
                          1 carrito 1──N detalle_carrito N──1 producto
producto N──N categoria   (tabla puente: producto_categoria)
imagen_url → pertenece a UN producto, UNA categoría o UN blog
contacto      (sin relaciones: la escribe cualquier visitante, tenga cuenta o no)
```

## Tablas

### `region` ✅ y `comuna` ✅
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `region.id_region` | `INT` | PK, **sin** `AUTO_INCREMENT` (viene de `V2`: es el código oficial) |
| `region.nombre_region` | `VARCHAR(255)` | Obligatorio |
| `comuna.id_comuna` | `INT` | PK, sin autoincremento (código oficial, p. ej. `13101`) |
| `comuna.nombre_comuna` | `VARCHAR(255)` | Obligatorio |
| `comuna.region_id_region` | `INT` | FK → `region`, `RESTRICT` |

Datos: 16 regiones y 346 comunas (`V2`).

### `tipo_usuario` ✅ (los roles)
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_tipo_usuario` | `BIGINT` | PK, `AUTO_INCREMENT` |
| `nombre_tipo_usuario` | `VARCHAR(255)` | Obligatorio, **único** |

Datos (`V2`): `ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`. El código **busca por nombre**, nunca por id.

### `usuario` ✅
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_usuario` | `BIGINT` | PK, autoincremento |
| `run` | `INT` | Obligatorio, **único** |
| `run_dv` | `CHAR(1)` | Obligatorio (`0-9` o `K`); **no** único |
| `nombre`, `apellidos` | `VARCHAR(255)` | Obligatorios |
| `correo` | `VARCHAR(255)` | Obligatorio, **único** (es el nombre de usuario del login) |
| `fecha_nacimiento` | `DATE` | Obligatoria |
| `direccion` | `VARCHAR(255)` | Obligatoria |
| `telefono` | `VARCHAR(15)` | Obligatorio |
| `contrasenia` | `VARCHAR(255)` | Hash **BCrypt** (60 caracteres). Nunca sale en una respuesta |
| `activo` | `BOOLEAN` | Por defecto `TRUE`. En `FALSE` el usuario pierde el acceso al instante |
| `tipo_usuario_id_tipo_usuario` | `BIGINT` | FK → `tipo_usuario`, `RESTRICT` |
| `comuna_id_comuna` | `INT` | FK → `comuna`, `RESTRICT` |

### `categoria` ⏳
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_categoria` | `BIGINT` | PK, autoincremento |
| `nombre_categoria` | `VARCHAR(255)` | Obligatorio, **único** |
| `descripcion_categoria` | `VARCHAR(255)` | **Obligatoria** (máx. 255) |

Datos (`V3`): Notebooks, Computadores de escritorio, Componentes, Almacenamiento, Monitores, Periféricos, Redes, Accesorios. *`V3` se marcó como borrador: ajusta los nombres a lo que venda la tienda antes de producción.*

### `producto` ⏳
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_producto` | `BIGINT` | PK, autoincremento |
| `nombre` | `VARCHAR(255)` | Obligatorio |
| `descripcion` | `VARCHAR(500)` | **Opcional** (única columna que acepta `NULL`) |
| `precio` | `BIGINT` | `CHECK >= 0`. **Pesos enteros**, sin decimales |
| `stock` | `BIGINT` | `CHECK >= 0` |
| `stock_critico` | `BIGINT` | `CHECK >= 0`. Umbral para avisar que queda poco |
| `descuento` | `INT` | `CHECK` entre 0 y 100, por defecto 0 (porcentaje) |
| `activo` | `BOOLEAN` | Por defecto `TRUE` (baja lógica) |

### `producto_categoria` ⏳ (tabla puente)
| Columna | Reglas |
| --- | --- |
| `producto_id_producto` | PK compuesta; FK → `producto`, **`CASCADE`** |
| `categoria_id_categoria` | PK compuesta; FK → `categoria`, `RESTRICT` |

### `imagen_url` ⏳
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_url` | `BIGINT` | PK, autoincremento |
| `url` | `VARCHAR(1000)` | Obligatoria |
| `producto_id_producto`, `categoria_id_categoria`, `blog_id_blog` | `BIGINT NULL` | FKs a `producto`, `categoria` y `blog`, las tres con `CASCADE` |

**Regla `chk_imagen_dueno`:** exactamente **una** de las tres FKs tiene valor; las otras dos deben ser `NULL`. Una imagen no puede quedar sin dueño ni tener dos.

### `blog` ⏳
| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_blog` | `BIGINT` | PK, autoincremento |
| `usuario_id_usuario` | `BIGINT` | Autor; FK → `usuario`, `RESTRICT` |
| `titulo` | `VARCHAR(255)` | Obligatorio |
| `contenido` | `TEXT` | Obligatorio |
| `visible` | `BOOLEAN` | Por defecto `TRUE` |

### `carrito` ⏳ y `detalle_carrito` ⏳
| Columna | Reglas |
| --- | --- |
| `carrito.id_carrito` | PK, autoincremento |
| `carrito.usuario_id_usuario` | **Único**: un usuario tiene **un** carrito; FK → `usuario`, **`CASCADE`** |
| `detalle_carrito.carrito_id_carrito` + `producto_id_producto` | PK compuesta: un producto aparece **una vez** por carrito. FK a carrito `CASCADE`; a producto `RESTRICT` |
| `detalle_carrito.cantidad` | `CHECK >= 1` |

### `boleta` ⏳ y `detalle_boleta` ⏳
| Columna | Reglas |
| --- | --- |
| `boleta.id_boleta` | PK, autoincremento |
| `boleta.usuario_id_usuario` | FK → `usuario`, `RESTRICT` |
| `boleta.fecha` | `DATETIME`, obligatoria |
| `boleta.total_boleta` | `BIGINT`, `CHECK >= 0` (pesos enteros) |
| `boleta.estado` | `BOOLEAN`, por defecto `TRUE` |
| `detalle_boleta.boleta_id_boleta` | FK → `boleta`, `RESTRICT` |
| `detalle_boleta.nombre` | **Copia** del nombre del producto al comprar |
| `detalle_boleta.precio_unitario` | **Copia** del precio al comprar, `CHECK >= 0` |
| `detalle_boleta.producto_id_producto` | FK → `producto`, `RESTRICT` |
| `detalle_boleta.descuento` | `CHECK` entre 0 y 100; **sin** valor por defecto |
| `detalle_boleta.cantidad` | `CHECK >= 1` |

**Por qué se copian nombre y precio:** una boleta es un documento histórico. Si mañana el producto cambia de precio o de nombre, la boleta de ayer no debe cambiar.

### `contacto` ✅ (`V6`)

Mensajes del formulario de contacto. **No tiene claves foráneas**: quien escribe puede no tener cuenta, por eso `nombre` y `correo` van en la propia fila.

| Columna | Tipo | Reglas |
| --- | --- | --- |
| `id_contacto` | `BIGINT` | PK, autoincremento |
| `nombre` | `VARCHAR(100)` | Obligatorio |
| `correo` | `VARCHAR(250)` | Obligatorio |
| `comentario` | `VARCHAR(500)` | Obligatorio |

No tiene fecha de envío (ver [referencia 28, sección 13](28-contacto-explicado.md#13-límites-y-mejoras-futuras)). Los máximos del DTO `ContactoRequest` coinciden con estas columnas: lo que pasa la validación siempre cabe.

## Qué pasa al borrar (`ON DELETE`)

| Si borras... | Resultado |
| --- | --- |
| Un `usuario` | Se borra su `carrito` y sus detalles (`CASCADE`). **Falla** si tiene boletas o blogs (`RESTRICT`) |
| Un `producto` | Se borran sus imágenes y sus filas de `producto_categoria`. **Falla** si está en un carrito o en una boleta |
| Una `categoria` | Se borran sus imágenes. **Falla** si algún producto la usa |
| Una `boleta` | **Falla** si tiene detalles |
| Una `region` o `comuna` | **Falla** si hay comunas o usuarios que dependen |
| Un `contacto` | Se borra sin problema: ninguna tabla depende de él (todavía no hay `DELETE` en la API) |

**Consecuencia práctica:** casi nada se borra de verdad. Para «eliminar» un producto o un usuario se usa la **baja lógica** (`activo = false`).

## Convenciones de nombres

| Cosa | Convención | Ejemplo |
| --- | --- | --- |
| Tablas y columnas | `snake_case`, singular | `detalle_boleta` |
| Clave foránea | `<tabla>_<id de esa tabla>` | `region_id_region` |
| Clave primaria | `id_<tabla>` | `id_usuario` |
| Atributos Java | `camelCase` | `runDv`, `fechaNacimiento` |

## Cosas a tener presentes

1. **Dinero en `BIGINT`:** pesos enteros, sin decimales. No se usa `DECIMAL` ni `double`.
2. **`descuento` es un porcentaje entero** (0 a 100). Aún no está decidido cómo se aplica al precio (decisión abierta en la [guía 16](../guias/16-hoja-de-ruta.md)).
3. **`stock` es solo un número:** al comprar habrá que descontarlo dentro de una transacción para que dos compradores no se lleven la última unidad.
4. **La unicidad del RUN** está en `run`, no en `run_dv`.
5. **`ddl-auto: validate` no comprueba los `CHECK`**: esas reglas las aplica MySQL al insertar.

## Para saber más

[Referencia 06: la base de datos](06-base-de-datos.md) · [Referencia 13: entidades explicadas](13-entidades-explicadas.md) · [Referencia 05: Flyway](05-flyway-y-migraciones.md)
