# 30 · Catálogo y precios

Cómo están implementadas las categorías y los productos, y cómo se calcula el precio que ve el cliente. Es el detalle que faltaba en la [guía 17](../guias/17-integracion-con-react.md) cuando el catálogo vivía solo en el frontend.

## Piezas

| Capa | Clases |
| --- | --- |
| Controller | `CategoriaController`, `ProductoController` |
| Service | `CategoriaService`, `ProductoService`, `ResolutorImagenes` |
| Repository | `CategoriaRepository`, `ProductoRepository`, `ProductoCategoriaRepository` |
| DTO | `CategoriaRequest`, `CategoriaResponse`, `ProductoRequest`, `ProductoResponse`, `PageResponse` |
| Mapper | `CategoriaMapper`, `ProductoMapper` |
| Utilidad | `Precios` |

Todas las rutas son `/api/v1/...`. La lectura del catálogo es pública; las escrituras son solo de `ADMINISTRADOR` (ver [23, permisos](23-catalogo-de-endpoints.md)).

## Categorías (`/api/v1/categorias`)

| Método | Ruta | Rol | Resultado |
| --- | --- | --- | --- |
| `GET` | `/api/v1/categorias` | Público | Todas, ordenadas por nombre, con `cantidadProductos` (solo activos) |
| `GET` | `/api/v1/categorias/{id}` | Público | Una categoría; 404 si no existe |
| `POST` | `/api/v1/categorias` | Administrador | 201 con `Location` |
| `PUT` | `/api/v1/categorias/{id}` | Administrador | Categoría actualizada |
| `DELETE` | `/api/v1/categorias/{id}` | Administrador | 204; **409** si tiene productos (aunque estén desactivados) |

`CategoriaRequest` exige `nombre` y `descripcion` (hasta 255 caracteres) y acepta hasta 5 `imagenes`. El nombre se recorta y se valida sin distinguir mayúsculas: no pueden existir dos iguales (`existsByNombreIgnoreCase` / `...AndIdNot`). El campo `imagenes` tiene tres estados: si no viene, no se tocan; si viene vacía, se quitan todas; si trae URLs, `ResolutorImagenes` reutiliza las filas existentes en `imagen_url` y crea las que falten.

No hay paginación: son pocas categorías (`findAll(Sort.by("nombre"))`).

## Productos (`/api/v1/productos`)

| Método | Ruta | Rol | Resultado |
| --- | --- | --- | --- |
| `GET` | `/api/v1/productos?categoriaId=` | Público | Página de productos **activos**; filtro opcional por categoría |
| `GET` | `/api/v1/productos/ofertas` | Público | Página de activos con `descuento > 0` |
| `GET` | `/api/v1/productos/{id}` | Público | Detalle; un producto desactivado devuelve 404 al público |
| `GET` | `/api/v1/productos/admin` | Admin / Vendedor | Todos, incluidos desactivados, paginado |
| `GET` | `/api/v1/productos/criticos` | Admin / Vendedor | `List` de productos con stock crítico, sin paginar |
| `POST` | `/api/v1/productos` | Administrador | 201 con `Location` |
| `PUT` | `/api/v1/productos/{id}` | Administrador | Producto actualizado |
| `DELETE` | `/api/v1/productos/{id}` | Administrador | 204: **baja lógica** (`activo = false`) |
| `PATCH` | `/api/v1/productos/{id}/activar` | Administrador | Vuelve a `activo = true` |

`ProductoRequest` pide nombre, precio, stock, stock crítico y al menos una categoría; `descuento` (0 a 100) e `imagenes` son opcionales. `ProductoService` valida que todas las categorías existan (404 si falta alguna) y sincroniza la relación `producto_categoria` quitando las que sobran y agregando las nuevas, sin borrar y reinsertar la misma fila.

**No se borra un producto**: las boletas antiguas apuntan a él y la clave foránea lo impide. Por eso `DELETE` es una baja lógica y existe `PATCH /activar`.

## Precio final

`ProductoResponse` incluye `precio` (sin descuento) y `precioFinal`:

```java
// util/Precios.java
public static long conDescuento(long precio, int descuento) {
    return Math.round(precio * (100 - descuento) / 100.0);
}
```

Es un porcentaje entero sobre pesos chilenos, redondeado al peso. Lo aplica `ProductoMapper` y también `CarritoMapper`, `BoletaMapper` y `BoletaService` (ver [31](31-carrito-y-boletas.md)). El frontend no debe recalcularlo: debe mostrar `precioFinal`.

## Paginación

Los listados usan `@ParameterObject Pageable`, así que Swagger muestra `page`, `size` y `sort` como parámetros separados. Un `?sort=` con un campo inexistente devuelve **400**, no 500. Las respuestas son `PageResponse`:

```json
{ "contenido": [], "pagina": 0, "tamanio": 20, "totalElementos": 21, "totalPaginas": 2 }
```

## Catálogo inicial (`V9__catalogo_inicial.sql`)

La migración `V9` inserta 21 productos, 6 categorías de forma idempotente (agrega `Audio` y `Smartphones` a las de `V3`) y 63 URLs de imagen de ejemplo en `imagen_url`. No crea tablas: las 17 tablas vienen de `V1`, `V5` y `V6`. Es la versión actual de Flyway; la próxima migración es `V10`.

## Sincronización con React

El frontend no consulta estas rutas desde cada vista: `src/main.jsx` llama a `sincronizarCatalogo()` antes de crear el router. Ese servicio pide `GET /api/v1/categorias` y `GET /api/v1/productos?page=0&size=100` (hasta 50 páginas) y guarda el resultado en `tf_categorias` / `tf_productos` con el formato que ya usaban las vistas. Si el backend no responde, se conserva la semilla local. Detalle: [README del frontend](../../../TecnoFactory_ReactVersion/README.md#datos).
