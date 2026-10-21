# Guía 10. Errores de la API

> **En una frase:** cualquier error del backend sale con **el mismo formato JSON** gracias a `GlobalExceptionHandler`, y el código HTTP dice de quién es la culpa.

## La idea

Es un **mostrador de reclamos**: no importa qué falló ni dónde, el cliente recibe siempre la misma «carta» (`ErrorResponse`).

```json
{
  "timestamp": "2026-10-20T20:55:00",
  "status": 409,
  "error": "Conflict",
  "mensaje": "El correo ya está registrado",
  "ruta": "/api/usuarios",
  "detalles": null
}
```

Cuando es un error de validación, `detalles` trae el campo que falló: `{ "runDv": "El dígito verificador no corresponde al RUN" }`.

## Qué código sale y cuándo

| Código | Cómo contarlo | Ejemplo en TecnoFactory | Excepción |
| --- | --- | --- | --- |
| **400** | «Tu pedido está mal escrito» | Falta el correo; JSON roto; `GET /api/usuarios/abc` | `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException` |
| **401** | «No sé quién eres» | Sin token, token vencido, clave incorrecta | `AuthenticationException`, `BadCredentialsException` |
| **403** | «Sé quién eres, pero no puedes» | Un cliente en `/api/usuarios/1` | `AccessDeniedException` |
| **404** | «Eso no existe» | Usuario o comuna inexistente; ruta inexistente | `RecursoNoEncontradoException`, `NoResourceFoundException` |
| **405** | «Ese método no está permitido aquí» | `DELETE /api/usuarios` | `HttpRequestMethodNotSupportedException` |
| **409** | «Es válido, pero choca con lo que hay» | Correo o RUN repetido | `ReglaNegocioException`, `DataIntegrityViolationException` |
| **415** | «No leo ese formato» | `POST` con texto plano | `HttpMediaTypeNotSupportedException` |
| **500** | «Se descompuso algo nuestro» | Un fallo inesperado | `Exception` (el detalle va al log, **no** al cliente) |

**Regla:** 4xx = el cliente puede corregirlo; 5xx = problema del servidor.

## Cómo lanzar un error desde tu código

```java
throw new RecursoNoEncontradoException("Producto", id);   // 404: "Producto no encontrado con id 5"
throw new ReglaNegocioException("Stock insuficiente");    // 409
```

Nada más: el handler hace el resto. **No** escribas `try/catch` para esto en el controller.

## Cómo se agrega un error nuevo

1. Crea la excepción propia (o usa una de Spring).
2. Agrega un método en `GlobalExceptionHandler` con `@ExceptionHandler(MiExcepcion.class)` que llame a `build(HttpStatus.X, "mensaje", req, null)`.
3. Escribe un test en el controller que lo provoque.

Spring elige siempre **el handler más específico**; `Exception.class` es la red de seguridad.

## Por qué el 401 y el 403 de las rutas necesitan ayuda

Las reglas por ruta de `SecurityConfig` se resuelven en los **filtros**, antes de llegar a ningún controller, y el handler solo atiende lo que sale de un controller. Por eso `ManejadorErroresSeguridad` recoge esos errores y los reenvía al mismo handler. Resultado: el 401 y el 403 tienen el mismo JSON que los demás.

## Reglas que no hay que olvidar

1. **El 500 nunca revela detalles internos.**
2. **Los mensajes de las excepciones propias sí llegan al cliente**: sin datos internos.
3. **Un error esperado (404, 409) no necesita `log.error`.**
4. **Un 500 donde debería haber un 4xx** casi siempre significa que falta un handler.

## Si algo falla

| Síntoma | Qué hacer |
| --- | --- |
| Un error del cliente sale como 500 | Falta un handler para esa excepción; agrégalo |
| Un 401 o 403 sale con la página de error por defecto | Falta `.exceptionHandling(...)` con `ManejadorErroresSeguridad` |

## Para saber más

[Referencia 11: excepciones, una por una](../referencia/11-excepciones-explicadas.md) · [Referencia 21, sección 9](../referencia/21-seguridad-jwt-y-roles-explicada.md#9-manejadorerroresseguridadjava-y-los-dos-handlers-nuevos)
