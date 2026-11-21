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

## Los 269 tests (incluido `contextLoads`)

| Archivo | Tests |
| --- | --- |
| `UsuarioRequestTest` | 11 |
| `RunUtilTest` | 4 |
| `PageResponseTest` | 1 |
| `PerfilRequestTest` | 5 |
| `UsuarioServiceTest` | 10 |
| `UsuarioServiceVendedorTest` | 3 |
| `UsuarioServiceBajaTest` | 7 |
| `AuthServiceTest` | 3 |
| `GoogleAuthServiceTest` | 11 |
| `UserDetailsServiceImplTest` | 4 |
| `JwtServiceTest` | 8 |
| `GoogleTokenVerifierTest` | 5 |
| `JwtAuthFilterTest` | 6 |
| `AuthControllerTest` | 11 |
| `UsuarioControllerTest` | 22 |
| `UsuarioControllerBajaTest` | 9 |
| `BlogServiceTest` | 14 |
| `BlogServiceGestionTest` | 12 |
| `BlogControllerTest` | 15 |
| `ImagenServiceTest` | 16 |
| `ImagenServiceGestionTest` | 17 |
| `ImagenControllerTest` | 13 |
| `AlmacenamientoServiceTest` | 10 |
| `ContactoServiceTest` | 4 |
| `ContactoControllerTest` | 10 |
| `GestionSeguridadTest` | 16 |
| `AdministradorInicialRunnerTest` | 6 |
| `ConfiguracionValidatorTest` | 7 |
| `ValidadorClavesTest` | 4 |
| `CabecerasUploadsFilterTest` | 2 |
| `GlobalExceptionHandlerTest` | 2 |

`GestionSeguridadTest` es la prueba de la política «todo lo no listado queda cerrado» (`denyAll`): reglas por rol, `/api/v1/imagenes/**` y cabeceras de `/uploads/**`. Estado al 20 nov 2026: **269 tests, 0 fallos**, incluido `contextLoads` en verde contra MySQL 8.4. Los 4 tests nuevos de `UsuarioControllerTest` cubren el listado: administrador 200, vendedor 200, cliente 403 y sin token 401. No hay tests propios de Categoría, Producto, Carrito ni Boleta. Contacto: [referencia 28, sección 11](../referencia/28-contacto-explicado.md#11-las-pruebas).

`TecnoFactoryApplicationTests` (`contextLoads`) arranca **toda** la app y necesita la base: por eso se ejecutan los demás con `-Dtest="*Test"`.

## Cómo se usa

```bash
./mvnw test -Dtest="*Test"                    # todos, sin la base de datos
./mvnw test -Dtest=UsuarioServiceTest         # uno solo
./mvnw test -Dtest="UsuarioRequestTest,RunUtilTest"
```

Resultado esperado: `Tests run: 269, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

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
5. **Las carpetas de test** deben reflejar el paquete: `JwtAuthFilterTest` ya está en `security/`; `JwtServiceTest` y `UserDetailsServiceImplTest` siguen en `service/` y conviene moverlos a `security/`.
6. **Errores: implementados no es lo mismo que probados.** `GlobalExceptionHandler` ya responde el 500 genérico (`generico`), el 409 por `UNIQUE` de la base (`integridad`) y el 400 por parámetro faltante (`parametroFaltante`). `GlobalExceptionHandlerTest` comprueba el mensaje del **413** con la configuración; los 409 de regla de negocio se ven en `UsuarioControllerTest` e `ImagenServiceTest`. El 500 genérico, el 409 por `UNIQUE` y el parámetro faltante siguen **sin un test directo** en la suite actual.

## Para saber más

[Referencia 19, sección 8](../referencia/19-validacion-run-service-y-controller-explicados.md#8-las-pruebas) · [Referencia 21, sección 13](../referencia/21-seguridad-jwt-y-roles-explicada.md#13-las-pruebas-de-seguridad)
