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
| `POST` | `/api/contactos` | **Público** (formulario de contacto) | 201 |
| `GET` | `/api/contactos` | `ADMINISTRADOR` | 200 |
| `GET` | `/api/contactos/{id}` | `ADMINISTRADOR` | 200 |
| `GET` | `/actuator/health` | Público | 200 |
| `GET` | `/swagger-ui/index.html`, `/v3/api-docs` | Público, **solo en local** | 200 |

**Todo lo demás** responde 401 sin token y, con token, 404 (la ruta no existe todavía).

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

## Formato de todos los errores

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "...", "ruta": "/api/usuarios", "detalles": null }
```

`detalles` solo lleva contenido en los errores de validación (`campo → mensaje`). Detalle de cada código en la [guía 10](../guias/10-errores-de-la-api.md).

## Paginación

Ya existen tres listados paginados: `GET /api/imagenes`, `GET /api/blogs` y `GET /api/contactos`. La forma es la misma en todos:

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
