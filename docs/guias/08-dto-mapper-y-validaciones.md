# Guía 08. DTOs, mapper y validaciones

> **En una frase:** un **DTO** es la clase que describe exactamente lo que entra y sale de la API; el **mapper** copia entre DTO y entidad; las **validaciones** rechazan los datos malos antes de que lleguen al service.

## La idea

En un restaurante, la **hoja del pedido** (lo que el cliente puede pedir) es distinta de la **receta** (cómo se cocina) y del **plato servido**. Si le entregaras al cliente la receta, verías secretos de la cocina. La entidad es la receta; el DTO es la hoja o el plato.

## Por qué no se devuelve la entidad

| Problema | Qué pasaría con `Usuario` |
| --- | --- |
| Filtra datos | La contraseña (el hash) saldría en el JSON |
| Relaciones `LAZY` | Serializar fuera de una transacción lanza `LazyInitializationException` |
| Acopla la API a la base | Renombrar una columna rompería el frontend |

## Request y Response son distintos a propósito

| | `UsuarioRequest` (entra) | `UsuarioResponse` (sale) |
| --- | --- | --- |
| Contraseña | Sí, como `clave` | **Nunca** |
| `id` | No | Sí |
| Rol | **No** | Sí, como texto |
| Comuna | Número (`idComuna`) | Nombres (`comuna`, `region`) |
| Validaciones | Sí | No |

**Por qué el Request no tiene rol:** si lo tuviera, cualquiera podría registrarse como `ADMINISTRADOR` enviando ese valor (*mass assignment*). El rol lo fija el service.

## Las validaciones del Request

| Anotación | Rechaza |
| --- | --- |
| `@NotBlank` | Texto vacío o solo espacios |
| `@NotNull` | `null` (no rechaza `""`: para textos usa `@NotBlank`) |
| `@Size(max = ...)` | Textos más largos que la columna |
| `@Email`, `@Pattern`, `@Past`, `@Min`, `@Max` | Formato de correo, expresión regular, fechas futuras, rangos |
| **`@RunValido`** (sobre toda la clase) | Un dígito verificador que no corresponde al RUN |

Se activan con `@Valid` en el controller. Si algo falla, no se ejecuta el método y el cliente recibe **400** con un mapa `campo → mensaje`.

## El dígito verificador (módulo 11)

Se multiplican los dígitos del RUN, de derecha a izquierda, por `2, 3, 4, 5, 6, 7` (repitiendo), se suman, y el dígito es `11 - (suma mod 11)`: si da 11 es `0`, si da 10 es `K`. Ejemplo: `12.345.678` → `5`. Vive en `RunUtil` (clase pura, fácil de probar); `@RunValido` y `RunValidoValidator` la conectan con Bean Validation y asocian el error al campo `runDv`.

## El mapper (MapStruct)

Una interfaz con dos métodos (`toResponse` y `toEntity`); MapStruct escribe la implementación al compilar. Lo que **no** debe copiar (contraseña, rol, comuna, `id`, `activo`) se marca con `@Mapping(target = "...", ignore = true)` y lo asigna el service.

## Reglas que no hay que olvidar

1. **El DTO de salida nunca lleva la contraseña; el de entrada nunca lleva rol, `id` ni `activo`.**
2. **La clave se llama `clave` en el Request**, no `contrasenia`: así el mapper no puede copiarla a la entidad por descuido.
3. **`toString()` del Request no imprime la clave**, para que nunca llegue a un log.
4. **`@Size` en cada texto**, con el largo de la columna, o la base responderá `Data too long`.
5. **Mapear siempre dentro del service** (la región es `LAZY`).

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| `Unmapped target property: "x"` al compilar | El destino tiene un campo que no se llena | Mapéalo o ignóralo con `ignore = true` |
| `No qualifying bean of type '...Mapper'` | Falta `componentModel = "spring"` o MapStruct no generó la clase | Revisa `@Mapper` y el orden de procesadores del `pom.xml` |
| Un campo con `@NotNull` acepta `""` | `@NotNull` solo rechaza `null` | Usa `@NotBlank` |

## Para saber más

[Referencia 18: DTOs, mapper y validaciones](../referencia/18-dto-mapper-y-validaciones-explicados.md) · [Referencia 19: RUN y pruebas](../referencia/19-validacion-run-service-y-controller-explicados.md)
