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
| El **mostrador de recepción**, solo para el personal | `POST /api/imagenes/**`, solo `ADMINISTRADOR` y `VENDEDOR` |

## El recorrido

```
SUBIR:  React (formulario multipart) -> POST /api/imagenes/productos
        -> JwtAuthFilter (¿quién eres?) -> SecurityConfig (¿ADMINISTRADOR o VENDEDOR?)
        -> ImagenController -> AlmacenamientoService (valida, nombre UUID, escribe el archivo)
        -> 200 {"url": "/uploads/productos/<uuid>.jpg"}

VER:    <img src="http://localhost:8080/uploads/productos/<uuid>.jpg">
        -> SecurityConfig (GET /uploads/** es público) -> WebConfig -> carpeta uploads/
```

Sin permiso: sin token → **401**; con token de `CLIENTE` → **403**.

## Las piezas

| Pieza | Dónde | Qué hace |
| --- | --- | --- |
| `AlmacenamientoService` | `service/` | Valida la imagen, le pone un nombre único y la guarda; también puede borrarla |
| `ImagenController` | `controller/` | Recibe el archivo en `POST /api/imagenes/productos` y devuelve la `url` |
| `WebConfig` | `config/` | Le dice a Spring que la carpeta `uploads/` se sirve en la ruta `/uploads/**` |
| `SecurityConfig` | `security/` | Reglas: ver `/uploads/**` es público; subir y borrar es de `ADMINISTRADOR` y `VENDEDOR` |
| `app.uploads.directorio` | `application.yaml` | Dónde está la carpeta (`${UPLOADS_DIR:./uploads}`) |
| Carpeta `uploads/` | Raíz del proyecto | Los archivos: `productos/`, `categorias/`, `blog/` |

## Las reglas de hoy

| Ruta | Quién entra |
| --- | --- |
| `GET /uploads/**` (ver una imagen) | Todos, sin sesión |
| `POST /api/imagenes/**` (subir) | `ADMINISTRADOR` y `VENDEDOR` |
| `DELETE /api/imagenes/**` (borrar) | `ADMINISTRADOR` y `VENDEDOR` (la regla existe; el endpoint **aún no**) |

## Qué se valida al subir

| Control | Regla | Si falla |
| --- | --- | --- |
| Tipo | Solo `image/jpeg`, `image/png` e `image/webp` | 409 «Solo se permiten imágenes JPG, PNG o WEBP» |
| Archivo vacío | No se acepta | Mismo 409 |
| Carpeta de destino | Solo `productos`, `categorias` o `blog` | 409 «Carpeta de destino no válida» |
| Tamaño | Máximo 5 MB (`spring.servlet.multipart`) | Hoy sale como **500**: falta su handler (ver abajo) |
| Nombre | Se ignora el original; se genera un UUID | — |

## Qué hay y qué falta

| Hecho | Pendiente |
| --- | --- |
| Guardar el archivo con nombre único | Entidad `ImagenUrl` y su repository |
| Servir la imagen por URL | Guardar la `url` en `imagen_url` ligada a su producto, categoría o blog |
| Reglas por rol | Endpoint `DELETE` (borra la fila **y** el archivo) |
| Límite de 5 MB | Handler para `MaxUploadSizeExceededException` (413) |

Hoy el endpoint solo **sube y devuelve la URL**: aún no la anota en la base. Cuando exista `ImagenUrl`, un service hará las dos cosas juntas.

## Cómo probarlo

1. Inicia sesión como `ADMINISTRADOR` (o `VENDEDOR`) y copia el `token`.
2. En Swagger: **Authorize** con el token → `POST /api/imagenes/productos` → *Try it out* → elige una imagen → *Execute*.
3. Copia la `url` de la respuesta y ábrela en el navegador: `http://localhost:8080` + la `url`.
4. Repite con el token de un `CLIENTE` → debe dar **403**; sin token → **401**.

Con curl está en el [manual personal 11](../personal/11-subir-y-ver-imagenes.md).

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
| 409 «Solo se permiten imágenes…» | El archivo no es JPG, PNG o WEBP (o está vacío) | Cambia el formato |
| 500 al subir un archivo grande | Pasa de 5 MB y falta el handler | Reduce la imagen; ver el pendiente |
| 500 sin enviar `archivo` | El campo no se llama `archivo` | El nombre del campo debe ser exactamente `archivo` |
| 415 | No se envió como `multipart/form-data` | No pongas `Content-Type` a mano al usar `FormData` |
| Las imágenes desaparecen al recrear Docker | `uploads/` estaba dentro del contenedor | Monta un volumen (`uploads_data`) |

## Para saber más

[Referencia 27: subida de imágenes, línea por línea](../referencia/27-subida-de-imagenes-explicada.md) · [Manual personal 11: subir y ver imágenes](../personal/11-subir-y-ver-imagenes.md) · [Guía 11: seguridad y login](11-seguridad-y-login.md) · [Referencia 24: decisiones D31 a D36](../referencia/24-decisiones-de-diseno.md)
