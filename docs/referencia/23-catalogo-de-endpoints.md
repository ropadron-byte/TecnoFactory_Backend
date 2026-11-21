# 23. Catálogo de endpoints

Todas las rutas que existen **hoy**, con lo que piden, lo que devuelven y cada error posible. Se actualiza al agregar cada controller.

Base: `http://localhost:8080` · Formato: JSON · Autenticación: cabecera `Authorization: Bearer <token>`.

## Resumen

| Método | Ruta | Quién | Éxito |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Público | 200 |
| `POST` | `/api/v1/auth/google` | Público (ID token de Google) | 200 / 202 |
| `POST` | `/api/v1/auth/google/completar` | Público (token temporal) | 201 |
| `POST` | `/api/v1/auth/google/vincular` | Cualquier sesión | 204 |
| `POST` | `/api/v1/usuarios` | Público (registro) | 201 |
| `GET` | `/api/v1/usuarios` | `ADMINISTRADOR` o `VENDEDOR` (solo lectura) | 200 |
| `GET` | `/api/v1/usuarios/me` | Cualquier sesión | 200 |
| `PUT` | `/api/v1/usuarios/me` | Cualquier sesión (propio perfil) | 200 |
| `POST` | `/api/v1/usuarios/vendedores` | `ADMINISTRADOR` | 201 |
| `GET` | `/api/v1/usuarios/{id}` | `ADMINISTRADOR` | 200 |
| `PATCH` | `/api/v1/usuarios/me/desactivar` | `CLIENTE` (su propia cuenta) | 204 |
| `PATCH` | `/api/v1/usuarios/{id}/desactivar` | `ADMINISTRADOR` (solo cuentas de vendedor) | 200 |
| `DELETE` | `/api/v1/usuarios/{id}` | `ADMINISTRADOR` (solo cuentas de vendedor) | 204 |
| `POST` | `/api/v1/contactos` | **Público** (formulario de contacto) | 201 |
| `GET` | `/api/v1/contactos` | `ADMINISTRADOR` | 200 |
| `GET` | `/api/v1/contactos/{id}` | `ADMINISTRADOR` | 200 |
| `GET` | `/api/v1/blogs` | Público | 200 |
| `GET` | `/api/v1/blogs/{id}` | Público (solo blogs visibles) | 200 |
| `GET` | `/api/v1/blogs/{id}/imagen` · `/imagenes` | Público | 200 / 204 |
| `GET` | `/api/v1/blogs/gestion` · `/gestion/{id}` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `POST/PUT/PATCH/DELETE` | `/api/v1/blogs/**` | `ADMINISTRADOR` o `VENDEDOR` | 200 / 201 / 204 |
| `GET` | `/api/v1/imagenes` · `/api/v1/imagenes/{id}` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `POST/PUT/DELETE` | `/api/v1/imagenes/**` | `ADMINISTRADOR` o `VENDEDOR` | 200 / 201 / 204 |
| `GET` | `/api/v1/categorias`, `/api/v1/categorias/{id}` | Público | 200 |
| `POST/PUT/DELETE` | `/api/v1/categorias/**` | `ADMINISTRADOR` | 200 / 201 / 204 |
| `GET` | `/api/v1/productos`, `/api/v1/productos/ofertas`, `/api/v1/productos/{id}` | Público | 200 |
| `GET` | `/api/v1/productos/admin`, `/api/v1/productos/criticos` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `POST/PUT/PATCH/DELETE` | `/api/v1/productos/**` | `ADMINISTRADOR` | 200 / 201 / 204 |
| `GET/POST/PUT/DELETE` | `/api/v1/carrito/**` | Cualquier sesión (carrito propio) | 200 / 204 |
| `POST` | `/api/v1/boletas/checkout` | Cualquier sesión | 201 |
| `GET` | `/api/v1/boletas/mias`, `/api/v1/boletas/{id}` | Cualquier sesión (propias) | 200 |
| `GET` | `/api/v1/boletas` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `PATCH` | `/api/v1/boletas/{id}/anular` | `ADMINISTRADOR` | 200 |
| `GET` | `/uploads/**` | Público | 200 |
| `GET` | `/actuator/health` | Público | 200 |
| `GET` | `/swagger-ui/index.html`, `/v3/api-docs` | Público, **solo en local** | 200 |

**Todo lo demás** responde 401 sin token y **403 con token**: `SecurityConfig` termina en
`anyRequest().denyAll()`, así que una ruta nueva sin regla explícita no queda accesible.

---

## `POST /api/v1/auth/login`

Cambia correo y clave por un token.

**Pide:**

```json
{ "correo": "ro.padron@duocuc.cl", "clave": "MiClaveSegura1" }
```

| Campo | Regla |
| --- | --- |
| `correo` | Obligatorio, máx. 255 |
| `clave` | Obligatoria, máx. 72 |

**Devuelve 200:**

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "correo": "ro.padron@duocuc.cl", "rol": "ADMINISTRADOR" }
```

| Código | Cuándo |
| --- | --- |
| 400 | Falta un campo o se pasa del máximo (`detalles` indica cuál) |
| 401 | Correo inexistente, clave incorrecta **o usuario desactivado** (mismo mensaje en los tres casos, a propósito) |

El token dura **1 hora** (`jwt.expiration-ms: 3600000`).

---

## `POST /api/v1/auth/google`

Recibe el **ID token** que Google Identity Services entrega en el navegador y lo verifica
(firma, emisor, audiencia y `email_verified`). No usa redirección OAuth2 ni Client Secret.

**Pide:**

```json
{ "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..." }
```

**Devuelve 200** si la cuenta de Google ya está vinculada:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "correo": "ana@correo.cl", "rol": "CLIENTE" }
```

**Devuelve 202** si es la primera vez (la cuenta **todavía no se crea**):

```json
{
  "requiereCompletarPerfil": true,
  "registroToken": "eyJhbGciOiJIUzI1NiJ9...",
  "correo": "ana@correo.cl", "nombre": "Ana", "apellidos": "Pérez"
}
```

| Código | Cuándo |
| --- | --- |
| 400 | Falta `credential` |
| 401 | Token de Google inválido, expirado, de otro Client ID, correo no verificado **o cuenta desactivada** |
| 409 | El correo ya existe con contraseña: hay que iniciar sesión y usar `/api/v1/auth/google/vincular`; o ya está vinculado a otra cuenta de Google |

El `registroToken` dura **10 minutos**, solo sirve para `/api/v1/auth/google/completar` y **no vale como `Bearer`**.

---

## `POST /api/v1/auth/google/completar`

Crea la cuenta `CLIENTE` con los datos que Google no entrega. El correo y el `google_id`
salen del token temporal, no del cuerpo.

**Pide:**

```json
{
  "registroToken": "eyJhbGciOiJIUzI1NiJ9...",
  "run": 12345678, "runDv": "5",
  "nombre": "Ana", "apellidos": "Pérez Soto",
  "fechaNacimiento": "1995-04-10",
  "direccion": "Av. Siempre Viva 742", "telefono": "+56912345678",
  "idComuna": 13101
}
```

| Campo | Regla |
| --- | --- |
| `registroToken` | Obligatorio, emitido por `/api/v1/auth/google` y vigente |
| `run` / `runDv` | Mismas reglas que el registro normal (módulo 11, `@RunValido`) |
| `nombre`, `apellidos`, `direccion` | Obligatorios, máx. 255 |
| `fechaNacimiento` | Obligatoria, anterior a hoy |
| `telefono` | 8 a 14 dígitos, `+` opcional |
| `idComuna` | Obligatorio, debe existir |

**Devuelve 201** con el mismo `AuthResponse` del login (`token`, `tipo`, `correo`, `rol`).
La cuenta queda con `contrasenia = NULL`: solo puede entrar con Google.

| Código | Cuándo |
| --- | --- |
| 400 | Falta un campo o no cumple una regla |
| 401 | `registroToken` inválido o expirado |
| 404 | La comuna no existe |
| 409 | El correo, el RUN o la cuenta de Google ya están registrados |

---

## `POST /api/v1/auth/google/vincular`

Vincula Google a la cuenta con la que ya se inició sesión **con contraseña**.
Exige `Authorization: Bearer <token>`; el correo de Google debe coincidir con el de la cuenta.

**Pide:**

```json
{ "credential": "eyJhbGciOiJSUzI1NiIsImtpZCI6..." }
```

| Código | Cuándo |
| --- | --- |
| 204 | Vinculado (o ya estaba vinculado a la misma cuenta de Google) |
| 400 | Falta `credential` |
| 401 | Sin sesión o ID token de Google inválido |
| 404 | La cuenta autenticada ya no existe |
| 409 | El correo de Google no coincide, o esa cuenta de Google ya pertenece a otro usuario |

---

## `POST /api/v1/usuarios` (registro)

Crea un usuario. **Siempre con rol `CLIENTE`**: el cuerpo no tiene campo de rol, de activo ni de id.

**Pide:**

```json
{
  "run": 12345678, "runDv": "5",
  "nombre": "Ana", "apellidos": "Pérez Soto",
  "correo": "ana@correo.cl", "clave": "ClaveSegura1",
  "fechaNacimiento": "1995-04-10",
  "direccion": "Av. Siempre Viva 742", "telefono": "+56912345678",
  "idComuna": 13101
}
```

| Campo | Regla |
| --- | --- |
| `run` | Obligatorio, entre 1.000.000 y 99.999.999 |
| `runDv` | `0-9` o `K`; debe corresponder al RUN (módulo 11, `@RunValido`) |
| `nombre`, `apellidos`, `direccion` | Obligatorios, máx. 255 |
| `correo` | Obligatorio, formato de correo, máx. 255 |
| `clave` | Obligatoria, **8 a 72** caracteres (72 es el límite de BCrypt) |
| `fechaNacimiento` | Obligatoria, anterior a hoy, formato `AAAA-MM-DD` |
| `telefono` | Obligatorio, 8 a 14 dígitos, `+` opcional |
| `idComuna` | Obligatorio, debe existir |

**Devuelve 201** con la cabecera `Location: /api/v1/usuarios/{id}` y:

```json
{
  "id": 7, "run": 12345678, "runDv": "5", "nombre": "Ana", "apellidos": "Pérez Soto",
  "correo": "ana@correo.cl", "fechaNacimiento": "1995-04-10",
  "direccion": "Av. Siempre Viva 742", "telefono": "+56912345678",
  "activo": true, "tipoUsuario": "CLIENTE", "comuna": "Santiago", "region": "Metropolitana de Santiago"
}
```

La respuesta **no incluye la clave** ni su hash.

| Código | Cuándo |
| --- | --- |
| 400 | Cualquier regla de la tabla anterior; JSON ilegible |
| 404 | La comuna no existe |
| 409 | El correo o el RUN ya están registrados |
| 415 | El cuerpo no es JSON |

Ejemplo de 400:

```json
{
  "timestamp": "2026-10-21T12:00:00", "status": 400, "error": "Bad Request",
  "mensaje": "Datos inválidos", "ruta": "/api/v1/usuarios",
  "detalles": { "runDv": "El dígito verificador no corresponde al RUN", "clave": "La clave debe tener entre 8 y 72 caracteres" }
}
```

---

## `GET /api/v1/usuarios`

Listado paginado de cuentas para los paneles. **`ADMINISTRADOR` y `VENDEDOR`, solo lectura**;
un `CLIENTE` recibe 403 y sin sesión 401. Acepta `page`, `size` y `sort` y devuelve un
`PageResponse<UsuarioResponse>` (sin claves).

| Código | Cuándo |
| --- | --- |
| 200 | `{ "contenido": [...], "pagina": 0, "tamanio": 20, "totalElementos": ..., "totalPaginas": ... }` |
| 400 | `sort` nombra un campo que no existe |
| 401 | Sin sesión |
| 403 | Con sesión de `CLIENTE` |

---

## `GET /api/v1/usuarios/me`

El perfil de **quien tiene la sesión**. Cualquier rol. No recibe id: lo toma del token (y de la base).

**Devuelve 200:** un `UsuarioResponse` (igual al del registro).

| Código | Cuándo |
| --- | --- |
| 401 | Sin token, token inválido o vencido, o usuario desactivado |

---

## `GET /api/v1/usuarios/{id}`

Cualquier usuario por id. **Solo `ADMINISTRADOR`.**

| Código | Cuándo |
| --- | --- |
| 200 | `UsuarioResponse` |
| 400 | El id no es un número (`/api/v1/usuarios/abc`) |
| 401 | Sin sesión |
| 403 | Con sesión, pero no es administrador |
| 404 | No existe ese usuario |

---

## Baja de cuentas

| Ruta | Quién | Qué hace |
| --- | --- | --- |
| `PATCH /api/v1/usuarios/me/desactivar` | `CLIENTE` | Pone `activo = false` en **su** cuenta. Pierde el acceso al instante (el rol y el estado salen de la base en cada petición); los datos se conservan. 204. |
| `PATCH /api/v1/usuarios/{id}/desactivar` | `ADMINISTRADOR` | Desactiva a un **vendedor**. Devuelve el `UsuarioResponse` con `activo: false`. |
| `DELETE /api/v1/usuarios/{id}` | `ADMINISTRADOR` | Borra a un **vendedor**. 204. |

Reglas: el cliente **solo puede desactivar** su cuenta, nunca borrarla (sus boletas deben conservarse; para el «derecho al olvido» la vía futura es anonimizar los datos, no borrar la fila). El correo sale del token, no de la URL; el administrador solo puede gestionar **vendedores**, nunca clientes ni otros administradores.

| Código | Cuándo |
| --- | --- |
| 401 | Sin sesión |
| 403 | El rol no corresponde (por ejemplo, un cliente llamando a `DELETE /api/v1/usuarios/{id}` o `/me`) |
| 404 | No existe el usuario |
| 409 | La cuenta de `{id}` no es de vendedor; el vendedor tiene **blogs** (hay que desactivarlo, no borrarlo) |

---

## `POST /api/v1/contactos` (formulario de contacto)

Guarda el mensaje de **cualquier persona**, sin iniciar sesión. Es una ruta pública a propósito: un visitante no tiene cuenta ni token.

**Pide:**

```json
{ "nombre": "Ana", "correo": "ana@correo.cl", "comentario": "Hola, quiero más información" }
```

| Campo | Regla |
| --- | --- |
| `nombre` | Obligatorio, máx. 100 |
| `correo` | Obligatorio, formato de correo, máx. 250 |
| `comentario` | Obligatorio, máx. 500 |

Los espacios al inicio y al final se quitan antes de guardar. El cuerpo no tiene `id`: lo genera la base.

**Devuelve 201** con la cabecera `Location: /api/v1/contactos/{id}` y:

```json
{ "id": 1, "nombre": "Ana", "correo": "ana@correo.cl", "comentario": "Hola, quiero más información" }
```

| Código | Cuándo |
| --- | --- |
| 400 | Falta un campo, el correo no tiene formato o se pasa del máximo (`detalles` indica cuál); JSON ilegible |
| 415 | El cuerpo no es JSON |

---

## `GET /api/v1/contactos` y `GET /api/v1/contactos/{id}`

Leer lo que llegó. **Solo `ADMINISTRADOR`**: cada mensaje trae el correo y el comentario de una persona.

`GET /api/v1/contactos` es **paginado** (`?page=`, `?size=`, `?sort=`; ver [Paginación](#paginación)) y devuelve un `PageResponse` de contactos. `GET /api/v1/contactos/{id}` devuelve un contacto con la misma forma que el 201 de arriba.

| Código | Cuándo |
| --- | --- |
| 200 | La página o el contacto |
| 400 | El id no es un número, o `sort` nombra un campo que no existe |
| 401 | Sin sesión |
| 403 | Con sesión, pero no es administrador (incluye `VENDEDOR` y `CLIENTE`) |
| 404 | No existe ese contacto (solo en `/{id}`) |

Aún **no existen** `PUT` ni `DELETE` de contactos. Detalle en la [referencia 28](28-contacto-explicado.md) y la [guía 19](../guias/19-formulario-de-contacto.md).

---

## Blogs

### `GET /api/v1/blogs` (público)

Página de blogs **visibles** con la forma `BlogResumenResponse`:

```json
{ "id": 1, "titulo": "Mi blog", "fechaCreacion": "2026-11-03T12:00:00", "imagenPrincipal": { "id": 7, "url": "/uploads/blog/a.jpg" } }
```

**No incluye** contenido, galería ni datos del autor.

### `GET /api/v1/blogs/{id}` (público)

Devuelve un `BlogPublicoResponse`:

```json
{
  "id": 1, "titulo": "Mi blog", "contenido": "...", "fechaCreacion": "2026-11-03T12:00:00",
  "imagenPrincipal": { "id": 7, "url": "/uploads/blog/a.jpg" },
  "imagenes": [ { "id": 7, "url": "/uploads/blog/a.jpg" } ]
}
```

**No incluye** `autorId`, `autorNombre` ni `autorCorreo`: el correo del autor nunca se publica.
Un blog desactivado responde **404** salvo para `ADMINISTRADOR` o `VENDEDOR`.

### `GET /api/v1/blogs/{id}/imagen` y `/api/v1/blogs/{id}/imagenes` (público)

La portada y la galería. La portada responde **204** si el blog no tiene imagen principal.

### `GET /api/v1/blogs/gestion` y `/api/v1/blogs/gestion/{id}` (personal)

- `/gestion` pagina **todos** los blogs, también los desactivados: `BlogGestionResponse`
  (`id`, `titulo`, `visible`, `fechaCreacion`, `autorNombre`, `imagenPrincipal`).
- `/gestion/{id}` devuelve un `BlogResponse` **con** `autorId`, `autorNombre` y `autorCorreo`.
  Es el único contrato que expone el correo del autor.

### `POST /api/v1/blogs`, `PUT /api/v1/blogs/{id}` y cambios de estado (personal)

`BlogRequest`:

```json
{ "titulo": "Mi blog", "contenido": "...", "visible": true, "idImagenPrincipal": 7 }
```

| Ruta | Efecto |
| --- | --- |
| `POST /api/v1/blogs` | Crea; **201** con `Location: /api/v1/blogs/{id}` |
| `PUT /api/v1/blogs/{id}` | Reemplaza título y contenido; `visible` e `idImagenPrincipal` son opcionales |
| `PATCH /api/v1/blogs/{id}/activar` · `/desactivar` | Cambia la visibilidad |
| `DELETE /api/v1/blogs/{id}` | **204**. No borra las imágenes, solo sus relaciones |
| `POST /api/v1/blogs/{id}/imagenes` (`multipart`, campo `archivo`) | Sube y agrega; **201** |
| `POST /api/v1/blogs/{id}/imagenes/{idImagen}` | Agrega una existente; **201** con `Location: /api/v1/blogs/{id}/imagenes` |
| `PUT /api/v1/blogs/{id}/imagen-principal/{idImagen}` | Cambia la portada |
| `DELETE /api/v1/blogs/{id}/imagenes/{idImagen}` | Quita de la galería sin borrar la imagen |

Todas las rutas de escritura exigen `ADMINISTRADOR` o `VENDEDOR`.

---

## Imágenes

### `GET /api/v1/imagenes` y `GET /api/v1/imagenes/{id}` (personal)

Listado paginado y detalle de `ImagenResponse`, con sus dueños (`BLOG`, `PRODUCTO`,
`CATEGORIA`). **Ya no son públicas**: revelaban ids de blogs ocultos. Exigen
`ADMINISTRADOR` o `VENDEDOR`; sin token responden 401 y con otro rol, 403.

### Subida y gestión (personal)

| Ruta | Efecto |
| --- | --- |
| `POST /api/v1/imagenes` (`multipart`, campo `archivo`) | Sube sin dueño; **201** con `Location: /api/v1/imagenes/{id}` |
| `POST /api/v1/imagenes/productos` | Igual, guardando en `uploads/productos` |
| `PUT /api/v1/imagenes/{id}` | Reemplaza el archivo conservando el id y sus relaciones |
| `DELETE /api/v1/imagenes/{id}` | **204**; **409** si todavía tiene dueños |
| `DELETE /api/v1/imagenes/{id}/duenos` | Desacopla de todos sus dueños |
| `DELETE /api/v1/imagenes/{id}/duenos/{tipo}/{idDueno}` | Desacopla de un dueño |

Solo se aceptan **JPG, PNG y WEBP** de hasta **5 MB** (el mensaje de error lee el límite
de `spring.servlet.multipart.max-file-size`).

### `GET /uploads/**` (público)

Los archivos se sirven sin sesión para que funcionen los `<img>`. La respuesta incluye
`X-Content-Type-Options: nosniff` y `Content-Security-Policy: default-src 'none'` para que
el navegador no interprete una subida como HTML o script.

---

## Catálogo: categorías y productos

La lectura del catálogo es **pública**; las escrituras son solo de `ADMINISTRADOR`. Hay
detalle completo (DTOs, reglas de negocio, precio final y Catálogo inicial `V9`) en la
[referencia 30](30-catalogo-y-precios.md).

| Ruta | Quién | Qué hace |
| --- | --- | --- |
| `GET /api/v1/categorias` | Público | Lista ordenada por nombre con `cantidadProductos` (sin paginar) |
| `GET /api/v1/categorias/{id}` | Público | Detalle; 404 si no existe |
| `POST /api/v1/categorias` | Administrador | 201 con `Location` |
| `PUT /api/v1/categorias/{id}` | Administrador | Edita nombre, descripción e imágenes |
| `DELETE /api/v1/categorias/{id}` | Administrador | 204; **409** si tiene productos |
| `GET /api/v1/productos?categoriaId=` | Público | Página de productos activos |
| `GET /api/v1/productos/ofertas` | Público | Página de activos con descuento |
| `GET /api/v1/productos/{id}` | Público | Detalle; un desactivado da 404 al público |
| `GET /api/v1/productos/admin` | Admin / Vendedor | Todos, incluidos desactivados |
| `GET /api/v1/productos/criticos` | Admin / Vendedor | Stock crítico (lista, sin paginar) |
| `POST /api/v1/productos` | Administrador | 201 con `Location` |
| `PUT /api/v1/productos/{id}` | Administrador | Edición |
| `DELETE /api/v1/productos/{id}` | Administrador | 204, **baja lógica** (`activo = false`) |
| `PATCH /api/v1/productos/{id}/activar` | Administrador | Reactiva el producto |

`ProductoResponse` incluye `precio` y `precioFinal`. El descuento lo aplica el backend
(`Precios.conDescuento`, redondeo al peso): el frontend solo lo muestra.

## Carrito y boletas

Todas exigen sesión. El carrito se identifica por el correo del token, no por un id de la URL.
El checkout valida stock, descuenta existencias, copia nombre y precio en la boleta y vacía
el carrito en una sola transacción. Detalle en la [referencia 31](31-carrito-y-boletas.md).

| Ruta | Quién | Qué hace |
| --- | --- | --- |
| `GET /api/v1/carrito` | Cualquier sesión | Ver el carrito (se crea si no existe) |
| `POST /api/v1/carrito/items` | Cualquier sesión | Agregar; si ya está, suma la cantidad |
| `PUT /api/v1/carrito/items/{idProducto}` | Cualquier sesión | Cambiar cantidad (`cantidad >= 1`) |
| `DELETE /api/v1/carrito/items/{idProducto}` | Cualquier sesión | Quitar un producto |
| `DELETE /api/v1/carrito` | Cualquier sesión | Vaciar (204) |
| `POST /api/v1/boletas/checkout` | Cualquier sesión | Comprar: 201 + `Location` |
| `GET /api/v1/boletas/mias` | Cualquier sesión | Mis boletas, paginadas |
| `GET /api/v1/boletas` | Admin / Vendedor | Todas, paginadas |
| `GET /api/v1/boletas/{id}` | Cualquier sesión | Detalle; ajena = 404 para un cliente |
| `PATCH /api/v1/boletas/{id}/anular` | Administrador | Anula y devuelve el stock |

Errores típicos: **409** por carrito vacío, producto desactivado, stock insuficiente o boleta
ya anulada; **403** si un cliente pide todas las boletas o intenta anular.

## Formato de todos los errores

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "...", "ruta": "/api/v1/usuarios", "detalles": null }
```

`detalles` solo lleva contenido en los errores de validación (`campo → mensaje`). Detalle de cada código en la [guía 10](../guias/10-errores-de-la-api.md).

## Paginación

Ya existen varios listados paginados: `GET /api/v1/blogs`, `/api/v1/blogs/gestion`, `/api/v1/imagenes`, `/api/v1/contactos`, `/api/v1/productos` (y `/ofertas`, `/admin`), `/api/v1/boletas` (y `/mias`). La forma es la misma en todos:

| Parámetro | Efecto |
| --- | --- |
| `?page=0` | Número de página, **desde 0** |
| `?size=20` | Tamaño (por defecto 20, **máximo 100**) |
| `?sort=nombre,asc` | Orden |

En **Swagger** esos tres parámetros aparecen como campos separados gracias a `@ParameterObject` en el `Pageable` del controller (3 nov, extendido a productos y boletas el 20 nov). Sin esa anotación, Swagger muestra un solo cuadro JSON y lo envía mal (`?pageable={...}`). **Todo `Pageable` nuevo debe llevarla.**

Si `sort` nombra un campo que no existe (`?sort=string`), la API responde **400** «No se puede ordenar por 'string'» (antes salía 500).

Respuesta (`PageResponse`):

```json
{ "contenido": [ ... ], "pagina": 0, "tamanio": 20, "totalElementos": 135, "totalPaginas": 7 }
```

## Estado de rutas que antes eran futuras

Ya están implementadas y documentadas arriba: `GET /api/v1/usuarios` (listado paginado,
20 nov), `PUT /api/v1/usuarios/me`, `POST /api/v1/usuarios/vendedores`, `/api/v1/categorias`,
`/api/v1/productos`, `/api/v1/carrito` y `/api/v1/boletas`. No queda ninguna de estas rutas
pendiente.

## Para saber más

[Referencia 19: registro, service y controller](19-validacion-run-service-y-controller-explicados.md) · [Referencia 21: login y roles](21-seguridad-jwt-y-roles-explicada.md) · [Referencia 28: contacto](28-contacto-explicado.md) · [Referencia 30: catálogo y precios](30-catalogo-y-precios.md) · [Referencia 31: carrito y boletas](31-carrito-y-boletas.md) · [Guía 17: usarlo desde React](../guias/17-integracion-con-react.md)
