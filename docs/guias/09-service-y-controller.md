# Guía 09. Service y controller

> **En una frase:** el **controller** recibe la petición HTTP, valida y delega; el **service** decide (reglas del negocio) y guarda, todo dentro de una transacción.

## La idea

El **mesero** (controller) no cocina y el **cocinero** (service) no atiende mesas. Si aparece un `if` de negocio en el controller, probablemente pertenece al service.

## El service: `UsuarioService.registrar`

Las reglas, en orden:

| # | Regla | Si falla | HTTP |
| --- | --- | --- | --- |
| 1 | El correo no está registrado | `ReglaNegocioException` | 409 |
| 2 | El RUN no está registrado | `ReglaNegocioException` | 409 |
| 3 | La comuna existe | `RecursoNoEncontradoException` | 404 |
| 4 | El rol `CLIENTE` existe | `RecursoNoEncontradoException` | 404 |

Si todo está bien: el mapper copia los datos, el service pone la `K` en mayúscula, **hashea la clave con BCrypt**, **fija el rol `CLIENTE`**, asigna la comuna, guarda y devuelve un `UsuarioResponse`.

| Pieza | Qué hace |
| --- | --- |
| `@Transactional` | Todo o nada: si algo falla, no queda nada a medias. Además mantiene abierta la sesión de Hibernate para que el mapper lea la región |
| `@Transactional(readOnly = true)` | Igual, para métodos que solo leen (`buscarPorId`, `buscarPorCorreo`) |
| `throw new ...Exception(...)` | Sale del método y provoca el *rollback* |
| `passwordEncoder.encode(...)` | **El único lugar** donde la clave en texto plano se convierte en hash |

**Dos barreras contra duplicados:** el service da un mensaje claro (`existsByCorreo`); el `UNIQUE` de la base es la que garantiza cuando dos peticiones llegan a la vez (409 por `DataIntegrityViolationException`).

## El controller

```java
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);   // 201 + Location
    }
}
```

| Pieza | Qué hace |
| --- | --- |
| `@RestController` | Las respuestas se convierten a JSON |
| `@Valid @RequestBody` | Convierte el JSON en el Request y ejecuta las validaciones |
| `ResponseEntity.created(uri)` | Responde **201** con la cabecera `Location` |
| `@PathVariable Long id` | Toma el `{id}` de la URL (si no es número: 400) |
| `Authentication` (parámetro) | La persona autenticada; se usa en `/me` |

## Reglas que no hay que olvidar

1. **Controller sin lógica de negocio:** valida, delega y responde.
2. **El service devuelve DTOs, nunca entidades**, y mapea **dentro** de la transacción.
3. **El rol nunca lo decide el cliente.**
4. **Para errores, lanza** `RecursoNoEncontradoException` (404) o `ReglaNegocioException` (409): el handler ya sabe formatearlas.
5. **Un `POST` que crea responde 201 con `Location`**, no 200.
6. **El mensaje de una excepción propia llega al cliente**: escríbelo pensando en él.

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| `LazyInitializationException` | Se mapeó fuera del service | Mapea dentro |
| El service falla al crearse en un test | El orden de los argumentos del constructor | Lombok usa el orden de los campos `final` |
| 500 en vez de 404 o 409 | Se lanzó una excepción genérica | Usa las excepciones propias |

## Para saber más

[Referencia 19: service, controller y pruebas](../referencia/19-validacion-run-service-y-controller-explicados.md) · [Personal 02: agregar una entidad nueva](../personal/02-agregar-una-entidad.md)
