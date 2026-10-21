# 06. Cómo viaja una petición

Lo que pasa, paso a paso, entre que el frontend hace clic y recibe respuesta.

## La idea

Piensa en un edificio con **portería, recepción, oficina y archivo**:

| Edificio | Proyecto |
| --- | --- |
| Portería (revisa si puedes entrar) | Filtros de seguridad: CORS, `JwtAuthFilter`, reglas de `SecurityConfig` |
| Recepción (recibe y verifica el formulario) | Controller: valida con `@Valid` y delega |
| Oficina (decide y aplica las reglas) | Service: lógica de negocio y transacción |
| Archivo (guarda y busca) | Repository → MySQL |
| Buzón de reclamos | `GlobalExceptionHandler` |

## El recorrido

```
Navegador / Swagger
   │  POST /api/usuarios   (JSON)
   ▼
1. CORS ............ ¿viene de un origen permitido?
2. JwtAuthFilter ... ¿trae token? ¿es válido? ¿el usuario sigue activo?
3. SecurityConfig .. ¿esta ruta exige sesión o rol?      ──► 401 / 403
   ▼
4. Controller ...... JSON → Request, `@Valid`            ──► 400
   ▼
5. Service ......... reglas (¿correo repetido?)          ──► 404 / 409
   │                 mapea a DTO dentro de la transacción
   ▼
6. Repository ...... JPA/Hibernate → SQL → MySQL
   ▲
   │  Entity → Response → JSON
   ▼
Respuesta (200 / 201 / ...)
```

## Dónde se decide cada código

| Código | Quién lo produce |
| --- | --- |
| 401, 403 | Seguridad (`ManejadorErroresSeguridad`) |
| 400 | Validación del controller o JSON ilegible |
| 404, 409 | Service (excepciones propias) |
| 500 | Cualquier fallo no previsto |

Todos los errores salen con el mismo JSON (`ErrorResponse`).

## Ejemplo: registrar un usuario

1. Llega `POST /api/usuarios` (ruta pública: pasa sin token).
2. El controller convierte el JSON en `UsuarioRequest` y valida (RUN con módulo 11, correo, clave).
3. El service comprueba que el correo y el RUN no existan, hashea la clave con BCrypt, asigna el rol `CLIENTE` (el cliente **no** lo puede elegir), y guarda.
4. Devuelve `UsuarioResponse` (sin clave) con `201` y `Location`.

## Por qué cada capa tiene su trabajo

| Si... | Entonces |
| --- | --- |
| El controller tuviera lógica | No podrías reutilizarla ni probarla fácil |
| El service devolviera entidades | `open-in-view: false` lo rompería por las relaciones perezosas |
| La entidad saliera como JSON | Se filtrarían campos internos |

## Para saber más

[Guía 09](../guias/09-service-y-controller.md) · [Guía 11](../guias/11-seguridad-y-login.md) · [Guía 10](../guias/10-errores-de-la-api.md)
