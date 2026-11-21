# 31 · Carrito y boletas

El carrito y la compra ya están implementados en el backend. Antes vivían solo en `localStorage`
del frontend; hoy el carrito es una fila real por usuario y la boleta es el resultado de
convertirlo en una compra. Detalle complementario: [30 · Catálogo y precios](30-catalogo-y-precios.md).

## Piezas

| Capa | Clases |
| --- | --- |
| Controller | `CarritoController`, `BoletaController` |
| Service | `CarritoService`, `BoletaService` |
| Repository | `CarritoRepository`, `BoletaRepository` |
| DTO | `CarritoItemRequest`, `CantidadRequest`, `CarritoResponse`, `DetalleCarritoResponse`, `BoletaResponse`, `DetalleBoletaResponse`, `PageResponse` |
| Mapper | `CarritoMapper`, `BoletaMapper` |
| Utilidad | `Precios`, `Roles` |

Todas las rutas van bajo `/api/v1` y **exigen sesión** (`SecurityConfig`). No hay carrito
anónimo ni compra sin token.

## Carrito (`/api/v1/carrito`)

El carrito se identifica por el correo del token: no hay id en la URL ni campo `idUsuario`
en el request. `CarritoService.carritoDe()` busca por `findByUsuarioCorreo` y, si el usuario
todavía no tiene carrito, lo crea y guarda en la primera petición.

| Método | Ruta | Cuerpo | Resultado |
| --- | --- | --- | --- |
| `GET` | `/api/v1/carrito` | – | 200 `CarritoResponse` |
| `POST` | `/api/v1/carrito/items` | `CarritoItemRequest` | 200 `CarritoResponse` |
| `PUT` | `/api/v1/carrito/items/{idProducto}` | `CantidadRequest` | 200 `CarritoResponse` |
| `DELETE` | `/api/v1/carrito/items/{idProducto}` | – | 200 `CarritoResponse` |
| `DELETE` | `/api/v1/carrito` | – | 204 (vacía) |

`CarritoItemRequest` y `CantidadRequest` exigen `cantidad >= 1` (`@NotNull`, `@Min(1)`);
el primero además exige `idProducto`. Agregar un producto que ya está **suma** la cantidad
en vez de duplicar la fila. Cambiar la cantidad o quitar un producto que no está en el
carrito devuelve **404**.

Reglas de `CarritoService`:

- No se puede agregar un producto **desactivado** (`activo = false`): **409**.
- La cantidad nunca puede superar el `stock` disponible: **409** con el mensaje
  «Stock insuficiente para '…': quedan N unidades».
- El carrito se crea o se vacía de forma transaccional; vaciar no borra la fila, solo sus
  detalles.

`CarritoResponse` trae `id`, `items` y `total`. Cada `DetalleCarritoResponse` trae
`idProducto`, `nombre`, `precioUnitario`, `descuento`, `precioFinalUnitario`, `cantidad`
y `subtotal`. Los ítems van **ordenados por nombre** y el total es la suma de los subtotales.
Precios enteros en pesos chilenos, descuento aplicado con `Precios.conDescuento`.

```json
{
  "id": 3,
  "items": [
    { "idProducto": 5, "nombre": "Notebook Lenovo", "precioUnitario": 599990,
      "descuento": 10, "precioFinalUnitario": 539991, "cantidad": 1, "subtotal": 539991 }
  ],
  "total": 539991
}
```

## Boletas (`/api/v1/boletas`)

| Método | Ruta | Quién | Resultado |
| --- | --- | --- | --- |
| `POST` | `/api/v1/boletas/checkout` | Cualquier sesión | **201** con `Location: /api/v1/boletas/{id}` y la boleta |
| `GET` | `/api/v1/boletas/mias` | Cualquier sesión | 200 página de **mis** boletas, de la más nueva a la más antigua |
| `GET` | `/api/v1/boletas` | Admin / Vendedor | 200 página de **todas** las boletas |
| `GET` | `/api/v1/boletas/{id}` | Cualquier sesión | 200; un cliente solo ve las suyas (las ajenas dan **404**) |
| `PATCH` | `/api/v1/boletas/{id}/anular` | **Solo administrador** | 200 `BoletaResponse` |

Los dos listados (`/mias` y `/`) aceptan `page`, `size` y `sort` (`@ParameterObject
Pageable`, ver [23, paginación](23-catalogo-de-endpoints.md#paginación)) y devuelven
`PageResponse<BoletaResponse>`.

### `checkout`: de carrito a boleta

`BoletaService.generarDesdeCarrito()` hace todo en **una transacción** (todo o nada):

1. Busca el carrito del correo autenticado. Si no existe o está vacío → **409** «El carrito está vacío».
2. Ordena los ítems por nombre y valida cada uno: el producto debe seguir activo (**409** si no)
   y la cantidad no puede superar el stock (**409** si no).
3. **Descuenta el stock** de cada producto.
4. Copia en cada `DetalleBoleta` el `nombre`, `precioUnitario` y `descuento` vigentes.
   Es una copia a propósito: si mañana cambia el precio o el nombre del producto, la boleta
   antigua no se altera.
5. Guarda la boleta con `estado = true` y `fecha = LocalDateTime.now()`, calcula
   `totalBoleta` con `Precios.conDescuento(...) * cantidad` y **vacía el carrito**.
6. Responde 201 con la cabecera `Location`.

Si algo falla en cualquier paso, la transacción se revierte: no se descuenta stock y el
carrito queda intacto.

`BoletaResponse` trae `id`, `idUsuario`, `fecha`, `total`, `estado` y `detalles`. Cada
detalle trae `id`, `idProducto`, `nombre`, `precioUnitario`, `descuento`,
`precioFinalUnitario`, `cantidad` y `subtotal`. El `estado` es `true` (vigente) o `false`
(anulada). `BoletaMapper` calcula el precio final y el subtotal con el **precio copiado en
la boleta**, no con el precio actual del catálogo.

| Código | Cuándo |
| --- | --- |
| 400 | `sort` nombra un campo inexistente en los listados |
| 401 | Sin token o token inválido |
| 403 | Un cliente intenta `GET /` (todas) o `PATCH /{id}/anular` |
| 404 | La boleta no existe, o es de otro cliente |
| 409 | Carrito vacío, producto dado de baja, stock insuficiente o boleta ya anulada |

### `anular`

Solo `ADMINISTRADOR`. Vuelve a sumar `cantidad` al `stock` de cada producto, pone
`estado = false` y devuelve la boleta actualizada. No borra la boleta ni sus detalles:
el historial se conserva. Anular dos veces da **409** «La boleta ya está anulada».

## Relación con el frontend

El panel usa el cliente HTTP de `src/services/api.js` (prefijo `/api/v1`, JWT en
`tf_token`) para el carrito y las boletas de la API, mientras que la tienda pública todavía
guarda `tf_cart` / `tf_ordenes` en `localStorage`. La boleta de `/compra/exitosa/:id` y la
vista `/admin/ordenes/:id` son las que muestran este contrato. Detalle:
[README del frontend](../../../TecnoFactory_ReactVersion/README.md#flujo-de-compra).

## Sin tests propios (por ahora)

Igual que categorías y productos, `CarritoService` y `BoletaService` no tienen classes de
prueba dedicadas: la cobertura actual es de seguridad, mappers y conexión. Ver
[guía 15](../guias/15-pruebas.md).
