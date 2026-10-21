# Guía 15. Pruebas automáticas

> **En una frase:** las pruebas son pequeños programas que comprueban que tu código hace lo que debe, sin necesidad de abrir la app ni la base de datos.

## La idea

Una prueba es como un **simulacro de incendio**: se hace en un entorno controlado, rápido y sin riesgo. Los tests de este proyecto no usan MySQL: simulan las piezas que no son la que quieres probar.

## Los tres tipos que hay

| Tipo | Qué prueba | Cómo simula el resto | Ejemplo |
| --- | --- | --- | --- |
| **Unitaria pura** | Una clase sola | No hace falta | `RunUtilTest`, `JwtServiceTest` |
| **De service** (Mockito) | La lógica de negocio | Repositories falsos (`@Mock`) | `UsuarioServiceTest`, `AuthServiceTest` |
| **De controller** (`@WebMvcTest` + `MockMvc`) | HTTP: códigos, JSON, validaciones, seguridad | Service falso (`@MockitoBean`) | `UsuarioControllerTest`, `AuthControllerTest` |

## Los 56 tests (más `contextLoads`)

| Archivo | Tests |
| --- | --- |
| `UsuarioRequestTest` | 10 |
| `RunUtilTest` | 4 |
| `PageResponseTest` | 1 |
| `UsuarioServiceTest` | 6 |
| `AuthServiceTest` | 3 |
| `UserDetailsServiceImplTest` | 3 |
| `JwtServiceTest` | 6 |
| `JwtAuthFilterTest` | 6 |
| `AuthControllerTest` | 3 |
| `UsuarioControllerTest` | 14 |

`TecnoFactoryApplicationTests` (`contextLoads`) arranca **toda** la app y necesita la base: por eso se ejecutan los demás con `-Dtest="*Test"`.

## Cómo se usa

```bash
./mvnw test -Dtest="*Test"                    # todos, sin la base de datos
./mvnw test -Dtest=UsuarioServiceTest         # uno solo
./mvnw test -Dtest="UsuarioRequestTest,RunUtilTest"
```

Resultado esperado: `Tests run: 56, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

## El vocabulario

| Palabra | Qué es |
| --- | --- |
| `@Mock` / `@MockitoBean` | Un objeto falso que reemplaza a uno real |
| `when(x.metodo()).thenReturn(v)` | «Cuando se llame a esto, devuelve esto» |
| `verify(x, never()).save(any())` | Comprueba que **no** se llamó a `save` |
| `ArgumentCaptor` | Atrapa lo que el service le pasó al repository, para revisarlo |
| `assertThrows(Clase.class, () -> ...)` | Comprueba que ese código lanza esa excepción |
| `MockMvc` | Simula peticiones HTTP sin abrir un puerto |
| `jsonPath("$.mensaje")` | Lee un campo del JSON de respuesta |

## Reglas que no hay que olvidar

1. **Un test por comportamiento**, con un nombre que diga qué pasa (`registrarConCorreoRepetidoDevuelve409`).
2. **Cada `when(...)` debe usarse**, o Mockito falla con `UnnecessaryStubbingException`.
3. **Cada `@WebMvcTest` necesita** `@Import(SecurityConfig.class)` y un `@MockitoBean` de `UsuarioService`, `JwtService` y `UserDetailsService`.
4. **Un test que nunca falla no sirve**: rompe el código a propósito y comprueba que el test lo detecta (los «experimentos» de la referencia).
5. **Las carpetas de test** deben reflejar el paquete: `JwtServiceTest` y `UserDetailsServiceImplTest` van en `security/`.
6. **Aún sin cubrir:** el 500 genérico, el 409 por el `UNIQUE` de la base y el parámetro faltante.

## Para saber más

[Referencia 19, sección 8](../referencia/19-validacion-run-service-y-controller-explicados.md#8-las-pruebas) · [Referencia 21, sección 13](../referencia/21-seguridad-jwt-y-roles-explicada.md#13-las-pruebas-56-en-total)
