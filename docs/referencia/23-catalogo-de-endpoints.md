# 23. Catálogo de endpoints

Todas las rutas que existen **hoy**, con lo que piden, lo que devuelven y cada error posible. Se actualiza al agregar cada controller.

Base: `http://localhost:8080` · Formato: JSON · Autenticación: cabecera `Authorization: Bearer <token>`.

## Resumen

| Método | Ruta | Quién | Éxito |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Público | 200 |
| `POST` | `/api/usuarios` | Público (registro) | 201 |
| `GET` | `/api/usuarios/me` | Cualquier sesión | 200 |
| `GET` | `/api/usuarios/{id}` | `ADMINISTRADOR` | 200 |
| `PATCH` | `/api/usuarios/me/desactivar` | `CLIENTE` (su propia cuenta) | 204 |
| `DELETE` | `/api/usuarios/me` | `CLIENTE` (su propia cuenta) | 204 |
| `PATCH` | `/api/usuarios/{id}/desactivar` | `ADMINISTRADOR` (solo cuentas de vendedor) | 200 |
| `DELETE` | `/api/usuarios/{id}` | `ADMINISTRADOR` (solo cuentas de vendedor) | 204 |
| `POST` | `/api/contactos` | **Público** (formulario de contacto) | 201 |
| `GET` | `/api/contactos` | `ADMINISTRADOR` | 200 |
| `GET` | `/api/contactos/{id}` | `ADMINISTRADOR` | 200 |
| `GET` | `/api/blogs` | Público | 200 |
| `GET` | `/api/blogs/{id}` | Público (solo blogs visibles) | 200 |
| `GET` | `/api/blogs/{id}/imagen` · `/imagenes` | Público | 200 / 204 |
| `GET` | `/api/blogs/gestion` · `/gestion/{id}` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `POST/PUT/PATCH/DELETE` | `/api/blogs/**` | `ADMINISTRADOR` o `VENDEDOR` | 200 / 201 / 204 |
| `GET` | `/api/imagenes` · `/api/imagenes/{id}` | `ADMINISTRADOR` o `VENDEDOR` | 200 |
| `POST/PUT/DELETE` | `/api/imagenes/**` | `ADMINISTRADOR` o `VENDEDOR` | 200 / 201 / 204 |
| `GET` | `/uploads/**` | Público | 200 |
| `GET` | `/actuator/health` | Público | 200 |
| `GET` | `/swagger-ui/index.html`, `/v3/api-docs` | Público, **solo en local** | 200 |

**Todo lo demás** responde 401 sin token y **403 con token**: `SecurityConfig` termina en
`anyRequest().denyAll()`, así que una ruta nueva sin regla explícita no queda accesible.

---

## `POST /api/auth/login`

Cambia correo y clave por un token.

**Pide:**

```json
{ "correo": "admin@tecnofactory.cl", "clave": "MiClaveSegura1" }
```

| Campo | Regla |
| --- | --- |
| `correo` | Obligatorio, máx. 255 |
| `clave` | Obligatoria, máx. 72 |

**Devuelve 200:**

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "correo": "admin@tecnofactory.cl", "rol": "ADMINISTRADOR" }
```

| Código | Cuándo |
| --- | --- |
| 400 | Falta un campo o se pasa del máximo (`detalles` indica cuál) |
| 401 | Correo inexistente, clave incorrecta **o usuario desactivado** (mismo mensaje en los tres casos, a propósito) |

El token dura **1 hora** (`jwt.expiration-ms: 3600000`).

---

## `POST /api/usuarios` (registro)

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

**Devuelve 201** con la cabecera `Location: /api/usuarios/{id}` y:

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
  "mensaje": "Datos inválidos", "ruta": "/api/usuarios",
  "detalles": { "runDv": "El dígito verificador no corresponde al RUN", "clave": "La clave debe tener entre 8 y 72 caracteres" }
}
```

---

## `GET /api/usuarios/me`

El perfil de **quien tiene la sesión**. Cualquier rol. No recibe id: lo toma del token (y de la base).

**Devuelve 200:** un `UsuarioResponse` (igual al del registro).

| Código | Cuándo |
| --- | --- |
| 401 | Sin token, token inválido o vencido, o usuario desactivado |

---

## `GET /api/usuarios/{id}`

Cualquier usuario por id. **Solo `ADMINISTRADOR`.**

| Código | Cuándo |
| --- | --- |
| 200 | `UsuarioResponse` |
| 400 | El id no es un número (`/api/usuarios/abc`) |
| 401 | Sin sesión |
| 403 | Con sesión, pero no es administrador |
| 404 | No existe ese usuario |

---

## Baja de cuentas

| Ruta | Quién | Qué hace |
| --- | --- | --- |
| `PATCH /api/usuarios/me/desactivar` | `CLIENTE` | Pone `activo = false` en **su** cuenta. Pierde el acceso al instante (el rol y el estado salen de la base en cada petición); los datos se conservan. 204. |
| `DELETE /api/usuarios/me` | `CLIENTE` | Borra **su** cuenta de forma definitiva. 204. |
| `PATCH /api/usuarios/{id}/desactivar` | `ADMINISTRADOR` | Desactiva a un **vendedor**. Devuelve el `UsuarioResponse` con `activo: false`. |
| `DELETE /api/usuarios/{id}` | `ADMINISTRADOR` | Borra a un **vendedor**. 204. |

Reglas: el cliente solo actúa sobre su propia cuenta (el correo sale del token, no de la URL); el administrador solo puede gestionar **vendedores**, nunca clientes ni otros administradores.

| Código | Cuándo |
| --- | --- |
| 401 | Sin sesión |
| 403 | El rol no corresponde (por ejemplo, un vendedor llamando a `DELETE /api/usuarios/me`) |
| 404 | No existe el usuario |
| 409 | La cuenta de `{id}` no es de vendedor; el vendedor tiene **blogs** (hay que desactivarlo, no borrarlo); o el cliente tiene registros ligados (boletas, carrito) que impiden borrarlo: usa desactivar |

---

## `POST /api/contactos` (formulario de contacto)

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

**Devuelve 201** con la cabecera `Location: /api/contactos/{id}` y:

```json
{ "id": 1, "nombre": "Ana", "correo": "ana@correo.cl", "comentario": "Hola, quiero más información" }
```

| Código | Cuándo |
| --- | --- |
| 400 | Falta un campo, el correo no tiene formato o se pasa del máximo (`detalles` indica cuál); JSON ilegible |
| 415 | El cuerpo no es JSON |

---

## `GET /api/contactos` y `GET /api/contactos/{id}`

Leer lo que llegó. **Solo `ADMINISTRADOR`**: cada mensaje trae el correo y el comentario de una persona.

`GET /api/contactos` es **paginado** (`?page=`, `?size=`, `?sort=`; ver [Paginación](#paginación)) y devuelve un `PageResponse` de contactos. `GET /api/contactos/{id}` devuelve un contacto con la misma forma que el 201 de arriba.

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

### `GET /api/blogs` (público)

Página de blogs **visibles** con la forma `BlogResumenResponse`:

```json
{ "id": 1, "titulo": "Mi blog", "fechaCreacion": "2026-11-03T12:00:00", "imagenPrincipal": { "id": 7, "url": "/uploads/blog/a.jpg" } }
```

**No incluye** contenido, galería ni datos del autor.

### `GET /api/blogs/{id}` (público)

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

### `GET /api/blogs/{id}/imagen` y `/api/blogs/{id}/imagenes` (público)

La portada y la galería. La portada responde **204** si el blog no tiene imagen principal.

### `GET /api/blogs/gestion` y `/api/blogs/gestion/{id}` (personal)

- `/gestion` pagina **todos** los blogs, también los desactivados: `BlogGestionResponse`
  (`id`, `titulo`, `visible`, `fechaCreacion`, `autorNombre`, `imagenPrincipal`).
- `/gestion/{id}` devuelve un `BlogResponse` **con** `autorId`, `autorNombre` y `autorCorreo`.
  Es el único contrato que expone el correo del autor.

### `POST /api/blogs`, `PUT /api/blogs/{id}` y cambios de estado (personal)

`BlogRequest`:

```json
{ "titulo": "Mi blog", "contenido": "...", "visible": true, "idImagenPrincipal": 7 }
```

| Ruta | Efecto |
| --- | --- |
| `POST /api/blogs` | Crea; **201** con `Location: /api/blogs/{id}` |
| `PUT /api/blogs/{id}` | Reemplaza título y contenido; `visible` e `idImagenPrincipal` son opcionales |
| `PATCH /api/blogs/{id}/activar` · `/desactivar` | Cambia la visibilidad |
| `DELETE /api/blogs/{id}` | **204**. No borra las imágenes, solo sus relaciones |
| `POST /api/blogs/{id}/imagenes` (`multipart`, campo `archivo`) | Sube y agrega; **201** |
| `POST /api/blogs/{id}/imagenes/{idImagen}` | Agrega una existente; **201** con `Location: /api/blogs/{id}/imagenes` |
| `PUT /api/blogs/{id}/imagen-principal/{idImagen}` | Cambia la portada |
| `DELETE /api/blogs/{id}/imagenes/{idImagen}` | Quita de la galería sin borrar la imagen |

Todas las rutas de escritura exigen `ADMINISTRADOR` o `VENDEDOR`.

---

## Imágenes

### `GET /api/imagenes` y `GET /api/imagenes/{id}` (personal)

Listado paginado y detalle de `ImagenResponse`, con sus dueños (`BLOG`, `PRODUCTO`,
`CATEGORIA`). **Ya no son públicas**: revelaban ids de blogs ocultos. Exigen
`ADMINISTRADOR` o `VENDEDOR`; sin token responden 401 y con otro rol, 403.

### Subida y gestión (personal)

| Ruta | Efecto |
| --- | --- |
| `POST /api/imagenes` (`multipart`, campo `archivo`) | Sube sin dueño; **201** con `Location: /api/imagenes/{id}` |
| `POST /api/imagenes/productos` | Igual, guardando en `uploads/productos` |
| `PUT /api/imagenes/{id}` | Reemplaza el archivo conservando el id y sus relaciones |
| `DELETE /api/imagenes/{id}` | **204**; **409** si todavía tiene dueños |
| `DELETE /api/imagenes/{id}/duenos` | Desacopla de todos sus dueños |
| `DELETE /api/imagenes/{id}/duenos/{tipo}/{idDueno}` | Desacopla de un dueño |

Solo se aceptan **JPG, PNG y WEBP** de hasta **5 MB** (el mensaje de error lee el límite
de `spring.servlet.multipart.max-file-size`).

### `GET /uploads/**` (público)

Los archivos se sirven sin sesión para que funcionen los `<img>`. La respuesta incluye
`X-Content-Type-Options: nosniff` y `Content-Security-Policy: default-src 'none'` para que
el navegador no interprete una subida como HTML o script.

---

## Formato de todos los errores

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "...", "ruta": "/api/usuarios", "detalles": null }
```

`detalles` solo lleva contenido en los errores de validación (`campo → mensaje`). Detalle de cada código en la [guía 10](../guias/10-errores-de-la-api.md).

## Paginación

Ya existen varios listados paginados (`GET /api/blogs`, `/api/blogs/gestion`, `/api/imagenes` y `/api/contactos`). La forma es la misma en todos:

| Parámetro | Efecto |
| --- | --- |
| `?page=0` | Número de página, **desde 0** |
| `?size=20` | Tamaño (por defecto 20, **máximo 100**) |
| `?sort=nombre,asc` | Orden |

En **Swagger** esos tres parámetros aparecen como campos separados gracias a `@ParameterObject` en el `Pageable` del controller (3 nov). Sin esa anotación, Swagger muestra un solo cuadro JSON y lo envía mal (`?pageable={...}`). **Todo `Pageable` nuevo debe llevarla.**

Si `sort` nombra un campo que no existe (`?sort=string`), la API responde **400** «No se puede ordenar por 'string'» (antes salía 500).

Respuesta (`PageResponse`):

```json
{ "contenido": [ ... ], "pagina": 0, "tamanio": 20, "totalElementos": 135, "totalPaginas": 7 }
```

## Rutas que vendrán

| Ruta | Quién | Cuándo |
| --- | --- | --- |
| `GET /api/usuarios` (listado paginado) | `ADMINISTRADOR` y `VENDEDOR` (solo lectura) | Pendiente |
| `PUT /api/usuarios/me` | Cualquier sesión (solo el propio perfil) | Pendiente |
| `POST /api/usuarios/vendedores` (nombre provisional) | `ADMINISTRADOR` | Pendiente |
| `/api/categorias`, `/api/productos` | Lectura: sesión; escritura: `ADMINISTRADOR` y `VENDEDOR` | Con cada entidad |

## Para saber más

[Referencia 19: registro, service y controller](19-validacion-run-service-y-controller-explicados.md) · [Referencia 21: login y roles](21-seguridad-jwt-y-roles-explicada.md) · [Referencia 28: contacto](28-contacto-explicado.md) · [Guía 17: usarlo desde React](../guias/17-integracion-con-react.md)
