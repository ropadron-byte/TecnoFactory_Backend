# Guía 18. Subida de imágenes

> **En una frase:** las fotos se guardan como **archivos** en una carpeta (`uploads/`) y la base de datos solo guarda su **dirección** (`/uploads/productos/<uuid>.jpg`); `ADMINISTRADOR` y `VENDEDOR` pueden subirlas, y cualquier persona puede verlas.

## La idea: un archivador con fichas

Una biblioteca no pega los libros dentro del catálogo: el catálogo tiene **fichas** que dicen en qué estante está cada libro. Si metieras los libros en las fichas, el catálogo pesaría toneladas y buscar sería lentísimo.

| En la biblioteca | En TecnoFactory |
| --- | --- |
| El **estante** con los libros | La carpeta `uploads/` con los archivos de imagen |
| La **ficha** del catálogo | Una fila de `imagen_url` con la columna `url` |
| El **número de estante** escrito en la ficha | `/uploads/productos/3f2a...c1.jpg` |
| El **bibliotecario** que recibe el libro, le pone un código único y lo guarda | `AlmacenamientoService` |
| La **puerta de lectura**, abierta a todos | `GET /uploads/**`, público |
| El **mostrador de recepción**, solo para el personal | `POST /api/v1/imagenes/**`, solo `ADMINISTRADOR` y `VENDEDOR` |

## El recorrido

```
SUBIR:  React (formulario multipart) -> POST /api/v1/imagenes/productos
        -> JwtAuthFilter (¿quién eres?) -> SecurityConfig (¿ADMINISTRADOR o VENDEDOR?)
        -> ImagenController -> ImagenService -> AlmacenamientoService (valida, nombre UUID, escribe el archivo)
        -> 201 {"id": 7, "url": "/uploads/productos/<uuid>.jpg", "duenos": []} + Location

VER:    <img src="http://localhost:8080/uploads/productos/<uuid>.jpg">
        -> SecurityConfig (GET /uploads/** es público) -> WebConfig -> carpeta uploads/
```

Sin permiso: sin token → **401**; con token de `CLIENTE` → **403**.

## Las piezas

| Pieza | Dónde | Qué hace |
| --- | --- | --- |
| `AlmacenamientoService` | `service/` | Valida la imagen, le pone un nombre único y la guarda; también puede borrarla |
| `ImagenController` | `controller/` | Recibe el archivo en `POST /api/v1/imagenes/productos` y devuelve `id`, `url` y `duenos` (201); también reemplaza, desacopla y borra |
| `WebConfig` | `config/` | Le dice a Spring que la carpeta `uploads/` se sirve en la ruta `/uploads/**` |
| `SecurityConfig` | `security/` | Reglas: ver `/uploads/**` es público; subir y borrar es de `ADMINISTRADOR` y `VENDEDOR` |
| `app.uploads.directorio` | `application.yaml` | Dónde está la carpeta (`${UPLOADS_DIR:./uploads}`) |
| Carpeta `uploads/` | Raíz del proyecto | Los archivos: `productos/`, `categorias/`, `blog/` y `general/` |
| `UPLOADS_DIR` / `UPLOADS_HOST_DIR` | `.env`, `docker-compose.yml` | Con Docker, `uploads/` **siempre vive fuera del contenedor**: se monta `${UPLOADS_HOST_DIR:-./uploads}:/app/uploads` y `.dockerignore` impide que entre en la imagen. Reconstruir o borrar el contenedor no pierde las fotos |

## Las reglas de hoy

| Ruta | Quién entra |
| --- | --- |
| `GET /uploads/**` (ver una imagen) | Todos, sin sesión |
| `GET /api/v1/imagenes/**` (metadatos) | `ADMINISTRADOR` y `VENDEDOR` |
| `POST /api/v1/imagenes/**` (subir) | `ADMINISTRADOR` y `VENDEDOR` |
| `PUT /api/v1/imagenes/{id}` (reemplazar) | `ADMINISTRADOR` y `VENDEDOR` |
| `DELETE /api/v1/imagenes/{id}` (borrar) | `ADMINISTRADOR` y `VENDEDOR`; responde **409** si todavía tiene dueños |
| `DELETE /api/v1/imagenes/{id}/duenos[/...]` (desacoplar) | `ADMINISTRADOR` y `VENDEDOR`; deja la imagen libre sin borrarla |

## Qué se valida al subir

| Control | Regla | Si falla |
| --- | --- | --- |
| Tipo | Solo `image/jpeg`, `image/png` e `image/webp` (si no viene `Content-Type`, también se rechaza) | **400** «Solo se permiten imágenes JPG, PNG o WEBP» |
| Contenido | Los primeros bytes del archivo deben coincidir con el tipo declarado (JPEG `FF D8 FF`, PNG `89 50 4E 47…`, WEBP `RIFF…WEBP`): el `Content-Type` lo escribe el cliente y no basta | **400** «El contenido del archivo no corresponde a una imagen…» |
| Archivo vacío | No se acepta | **400** «El archivo está vacío» |
| Carpeta de destino | Solo `productos`, `categorias`, `blog` o `general` | 409 «Carpeta de destino no válida» |
| Tamaño | Máximo 5 MB (`spring.servlet.multipart`) | **413** «El archivo supera el tamaño máximo permitido (5 MB)» |
| Campo `archivo` | Obligatorio | **400** «Falta el archivo obligatorio 'archivo'» |
| Nombre | Se ignora el original; se genera un UUID | — |

## Qué hay y qué falta

| Hecho | Detalle |
| --- | --- |
| Guardar el archivo con nombre único y registrarlo en `imagen_url` (3 nov) | `POST /api/v1/imagenes` y `/productos` devuelven `201` con `id`, `url` y `duenos: []` |
| Servir la imagen por URL | `GET /uploads/**`, público |
| Panel de imágenes completo | `PUT /api/v1/imagenes/{id}` (reemplazar), `DELETE /api/v1/imagenes/{id}` (borra la fila **y** el archivo, 409 si tiene dueños) y `DELETE /api/v1/imagenes/{id}/duenos[...]` (desacoplar) |
| Asignar a productos y categorías | **Hecho, pero por URL, no por id**: `ProductoRequest` y `CategoriaRequest` llevan una lista `imagenes` con las URLs de `/uploads/...`; sus controllers (`ProductoController` y `CategoriaController`) existen |
| Asignar a blogs | Sigue siendo por id: `POST /api/v1/blogs/{id}/imagenes/{idImagen}` (galería) y `PUT /api/v1/blogs/{id}/imagen-principal/{idImagen}` (portada) |
| Límite de 5 MB con su 413 (3 nov) | El mensaje se arma desde `spring.servlet.multipart.max-file-size` |

Desde el 3 nov, subir **guarda el archivo y crea la fila en `imagen_url`** (sin dueño) y devuelve `201` con `id`, `url` y `duenos: []`. Dos rutas, mismo comportamiento: `POST /api/v1/imagenes` (carpeta `general`) y `POST /api/v1/imagenes/productos` (carpeta `productos`). A un blog se le asigna por id con las dos rutas de la tabla. A productos y categorías se les asigna **por URL**, en el cuerpo de `POST`/`PUT`: `imagenes: ["/uploads/productos/<uuid>.jpg", ...]` (en producto, la primera es la portada). Si el campo no viene no se tocan las imágenes; si viene vacío, se quitan todas. Si falla el INSERT al subir, el archivo se borra; si la imagen tiene dueños, borrarla da 409 hasta desacoplarla.

## Cómo probarlo

1. Inicia sesión como `ADMINISTRADOR` (o `VENDEDOR`) y copia el `token`.
2. En Swagger: **Authorize** con el token → `POST /api/v1/imagenes/productos` → *Try it out* → elige una imagen → *Execute*.
3. Copia la `url` de la respuesta y ábrela en el navegador: `http://localhost:8080` + la `url`.
4. Repite con el token de un `CLIENTE` → debe dar **403**; sin token → **401**.

Las rutas, cuerpos y códigos están en el [catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md); el panel de imágenes del frontend se documenta en el [README de la app React](../../../TecnoFactory_ReactVersion/README.md).

## Reglas que no hay que olvidar

1. **Nunca guardes la imagen dentro de la base**: solo la URL.
2. **Guarda la ruta relativa** (`/uploads/...`), no `http://localhost:8080/...`: si cambia el dominio, no hay que tocar ninguna fila.
3. **Nunca uses el nombre original** del archivo: puede traer `../` o pisar otra imagen. Siempre un UUID.
4. **La carpeta `uploads/` no va dentro de `src/`** y **no va a Git**: está en el `.gitignore`.
5. **La regla de rol vive en `SecurityConfig`**, no en `@PreAuthorize` (el proyecto no activa la seguridad por método).
6. **`GET /uploads/**` debe ser público**: un `<img>` del navegador no envía el token.
7. **Al borrar la fila de `imagen_url`, borra también el archivo**: la base no lo hace sola.
8. **Respalda `uploads/` junto con la base de datos**; en Docker, en un volumen.

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| 401 al subir | Sin token, token vencido o `Bearer` repetido | Inicia sesión de nuevo; pega solo el token |
| 403 al subir | Eres `CLIENTE` | Usa un usuario `ADMINISTRADOR` o `VENDEDOR` |
| 401 al **ver** la imagen | Falta `GET /uploads/**` en `SecurityConfig` o está después de `anyRequest()` | Ponla antes, con `permitAll()` |
| 404 al ver la imagen | Url mal escrita, archivo borrado, o la carpeta `uploads/` no existía al arrancar | Revisa la URL; crea `uploads/` y reinicia |
| 400 «Solo se permiten imágenes…» o «El contenido del archivo no corresponde…» | El archivo no es JPG, PNG o WEBP, está vacío, o se renombró otro formato (un `.txt` llamado `a.png` no pasa) | Sube una imagen real en uno de los tres formatos |
| 413 al subir | Pasa de 5 MB | Reduce la imagen |
| 400 «Falta el archivo obligatorio» | El campo no se llama `archivo` | El nombre del campo debe ser exactamente `archivo` |
| 415 | No se envió como `multipart/form-data` | No pongas `Content-Type` a mano al usar `FormData` |
| Las imágenes desaparecen al recrear Docker | `uploads/` estaba dentro del contenedor | Monta un volumen (`uploads_data`) |

## Para saber más

[Referencia 23: catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md) · [README del frontend React: panel de imágenes](../../../TecnoFactory_ReactVersion/README.md) · [Guía 11: seguridad y login](11-seguridad-y-login.md) · [Referencia 24: decisiones D31 a D36](../referencia/24-decisiones-de-diseno.md)
