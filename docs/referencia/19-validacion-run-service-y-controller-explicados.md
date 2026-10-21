# 19. Validación del RUN, service, controller y pruebas explicados

Guía para entender, línea por línea, lo que completa el **primer flujo de punta a punta** de `Usuario`: la validación del dígito verificador (paso 8.3), el registro y la búsqueda en el service (9.1) y el controller (9.2), con sus pruebas. Junto con la [guía 18](18-dto-mapper-y-validaciones-explicados.md), es el **ejemplo que se copia para cualquier otra entidad** (`Categoria`, `Producto`...).

| Archivo | Ruta | Para qué sirve | Sección |
| --- | --- | --- | --- |
| `RunUtil.java` | `src/main/java/com/factory/tecnofactory/validation/` | El algoritmo módulo 11, en una clase pura | [3](#3-runutiljava) |
| `RunValido.java` | `validation/` | La anotación que se pone sobre el Request | [4](#4-runvalidojava) |
| `RunValidoValidator.java` | `validation/` | El puente entre Bean Validation y `RunUtil` | [5](#5-runvalidovalidatorjava) |
| `UsuarioService.java` | `service/` | Registrar y buscar, con todas las reglas de negocio | [6](#6-usuarioservicejava-registrar-y-buscar) |
| `UsuarioController.java` | `controller/` | La capa HTTP: recibe, valida, delega y responde | [7](#7-usuariocontrollerjava) |
| `RunUtilTest`, `UsuarioServiceTest`, `UsuarioControllerTest` | `src/test/java/...` | Pruebas sin base de datos | [8](#8-las-pruebas) |

> Estado (19 oct): **8.3, 9.1 y 9.2 confirmados por ti** (las pruebas pasaron y la app arrancó). Pendiente: **9.3**, probarlo a mano en Swagger contra tu MySQL, y el segundo arranque del administrador.

## En palabras simples

Un **restaurante** sirve para ver qué hace cada pieza:

| En el restaurante | En TecnoFactory | Qué NO debe hacer |
| --- | --- | --- |
| El **mesero** toma el pedido y lo lleva a la cocina; trae el plato | `UsuarioController` | Cocinar |
| El **cocinero** sigue la receta y decide si hay ingredientes | `UsuarioService` (las reglas: correo repetido, comuna inexistente, clave en hash) | Atender mesas |
| La **despensa** guarda los ingredientes | `UsuarioRepository` y la base de datos | Decidir recetas |
| La **hoja del pedido** (qué se puede pedir) | `UsuarioRequest` (sus validaciones) | Llevar datos que el cliente no debe elegir (como su rol) |
| El **plato servido** | `UsuarioResponse` | Llevar la contraseña |
| Un **cartel de «no hay ese plato»** | `GlobalExceptionHandler` | — |

**La regla que importa:** cada pieza hace **una** cosa. Si el mesero empieza a cocinar (un `if` de negocio en el controller), nadie sabe dónde buscar el error.

**Por qué el dígito verificador se valida aparte:** el RUN `12.345.678` tiene un dígito al final (`-5`) calculado a partir de los demás. Es como el dígito de control de un código de barras: sirve para detectar un número mal tipeado **sin consultar ninguna base de datos**. Por eso es un cálculo (`RunUtil`) y no una consulta.

**Por qué `toResponse` va dentro del service:** el service es la cocina, y ahí todavía tienes abierta la despensa (la sesión de la base). Si sacas el ingrediente a la sala (el controller), ya no puedes volver a buscar lo que falte.

---

## 1. El flujo completo

```
POST /api/usuarios  (JSON)
   |
   v
[ Spring Security ]   -> hoy todo abierto (permitAll)
   |
   v
[ UsuarioController ] -> @Valid: reglas de campo (8.2) + RunValido (8.3)
   |                      falla -> MethodArgumentNotValidException -> 400
   v
[ UsuarioService ]    -> @Transactional: correo, RUN, comuna, rol; hash; save; toResponse
   |                      falla -> ReglaNegocioException (409) / RecursoNoEncontradoException (404)
   v
[ Repository ] -> MySQL (UNIQUE como segunda barrera -> DataIntegrityViolationException -> 409)
   |
   v
UsuarioResponse (JSON, sin clave) + 201 + cabecera Location
```

**Quién decide qué:**

| Capa | Decide | No decide |
| --- | --- | --- |
| Request (DTO) | Si el dato **tiene forma válida** | Si ya existe en la base |
| Controller | Qué ruta, qué verbo y qué código HTTP de éxito | Reglas de negocio |
| Service | Las **reglas de negocio** y la transacción | Cómo viaja por HTTP |
| `GlobalExceptionHandler` | Cómo se **ve** cada error | Cuándo ocurre |

---

## 2. El dígito verificador: el algoritmo (módulo 11)

Hoy `UsuarioRequest` solo comprueba que `runDv` sea `0-9` o `K`. Falta comprobar que **corresponda** al RUN. Ejemplo con 12.345.678:

1. Toma los dígitos **de derecha a izquierda** y multiplícalos por la secuencia `2, 3, 4, 5, 6, 7`, que se repite.
2. Suma los resultados.
3. Calcula `11 - (suma mod 11)`.
4. Si da `11`, el dígito es `0`; si da `10`, es `K`; en otro caso, es ese número.

```
RUN:          1   2   3   4   5   6   7   8
Derecha a izq: 8   7   6   5   4   3   2   1
Multiplicador: 2   3   4   5   6   7   2   3
Producto:     16  21  24  25  24  21   4   3   =  138

138 mod 11 = 6        (11 × 12 = 132, sobran 6)
11 - 6 = 5            ->  dígito verificador: 5   ->  12.345.678-5
```

**Por qué una validación de clase y no una anotación en el campo:** las anotaciones del paso 8.2 miran **un campo a la vez**. Esta regla necesita **dos** (`run` y `runDv`), así que se declara sobre la clase completa. Se compone de tres piezas: el algoritmo (`RunUtil`), la anotación (`@RunValido`) y el puente (`RunValidoValidator`).

---

## 3. `RunUtil.java`

```java
package com.factory.tecnofactory.validation;

public final class RunUtil {

    private RunUtil() {
    }

    public static String calcularDv(int run) {
        int suma = 0;
        int multiplicador = 2;

        for (int n = run; n > 0; n /= 10) {
            suma += (n % 10) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resultado = 11 - (suma % 11);

        if (resultado == 11) {
            return "0";
        }
        if (resultado == 10) {
            return "K";
        }
        return String.valueOf(resultado);
    }
}
```

| Línea | Qué significa |
| --- | --- |
| `public final class RunUtil` | `final`: nadie puede heredar de ella. Es una **clase de utilidad**: solo tiene métodos, no guarda datos |
| `private RunUtil() { }` | Constructor privado: impide `new RunUtil()`, que no tiene sentido. Es la costumbre en clases de utilidad |
| `public static String calcularDv(int run)` | `static`: se llama sin crear objetos (`RunUtil.calcularDv(12345678)`). Recibe el RUN sin dígito y devuelve el dígito como texto |
| `int suma = 0; int multiplicador = 2;` | La suma acumulada y el multiplicador actual, que arranca en 2 |
| `for (int n = run; n > 0; n /= 10)` | Recorre el número de derecha a izquierda: `n /= 10` le quita el último dígito en cada vuelta; termina cuando no quedan |
| `n % 10` | El último dígito de `n` (el resto de dividir por 10) |
| `suma += (n % 10) * multiplicador;` | Suma el dígito multiplicado |
| `multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;` | **Operador ternario**: si llegó a 7, vuelve a 2; si no, sube uno. Es la secuencia 2..7 que se repite |
| `11 - (suma % 11)` | El paso 3 del algoritmo |
| `return "0"`, `"K"`, `String.valueOf(...)` | Los tres casos del paso 4 |

**Por qué es una clase aparte:** una clase pura (sin Spring ni base de datos) se prueba en milisegundos y se reutiliza (por ejemplo, desde un futuro script de carga de datos).

---

## 4. `RunValido.java`

```java
@Documented
@Constraint(validatedBy = RunValidoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RunValido {

    String message() default "El dígito verificador no corresponde al RUN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

| Línea | Qué significa |
| --- | --- |
| `public @interface RunValido` | Así se declara una **anotación propia**: parece una interfaz, con una `@` delante |
| `@Constraint(validatedBy = RunValidoValidator.class)` | Lo que la convierte en regla de validación: indica **qué clase hace la comprobación** |
| `@Target(ElementType.TYPE)` | Dónde se puede poner: sobre una **clase** (los `record` también cuentan). Otras opciones: `FIELD`, `METHOD` |
| `@Retention(RetentionPolicy.RUNTIME)` | La anotación sigue existiendo **mientras corre el programa**. Sin esto, Bean Validation no podría verla |
| `@Documented` | Aparece en el Javadoc. Es cosmético |
| `String message() default "..."` | El mensaje por defecto cuando falla |
| `groups()` y `payload()` | Dos miembros que la especificación de Bean Validation **exige** en toda anotación de validación. No se usan, pero deben estar |

Se aplica así, sobre el record (los imports y el resto del Request no cambian):

```java
@RunValido
public record UsuarioRequest(
        // ... todo igual que antes
```

---

## 5. `RunValidoValidator.java`

```java
public class RunValidoValidator implements ConstraintValidator<RunValido, UsuarioRequest> {

    @Override
    public boolean isValid(UsuarioRequest request, ConstraintValidatorContext context) {
        if (request == null || request.run() == null || request.runDv() == null) {
            return true;
        }

        boolean correcto = RunUtil.calcularDv(request.run()).equalsIgnoreCase(request.runDv());

        if (!correcto) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("runDv")
                    .addConstraintViolation();
        }
        return correcto;
    }
}
```

| Línea | Qué significa |
| --- | --- |
| `implements ConstraintValidator<RunValido, UsuarioRequest>` | El contrato: "valido la anotación `RunValido` aplicada sobre objetos `UsuarioRequest`". Los dos tipos entre `< >` son esos |
| `isValid(UsuarioRequest request, ConstraintValidatorContext context)` | El método que Bean Validation llama. Devuelve `true` si es válido |
| `if (request == null \|\| request.run() == null \|\| request.runDv() == null) return true;` | Si falta alguno de los dos datos, este validador **no opina**. Ya hay `@NotNull` y `@NotBlank` que lo reportan: así el cliente no recibe dos mensajes por el mismo problema |
| `RunUtil.calcularDv(request.run())` | Calcula el dígito que **debería** tener ese RUN (`Integer` pasa a `int` solo) |
| `.equalsIgnoreCase(request.runDv())` | Lo compara con el que envió el cliente, sin distinguir `k` de `K` |
| `context.disableDefaultConstraintViolation();` | Apaga el error "por defecto", que se asociaría a la clase entera |
| `context.buildConstraintViolationWithTemplate(...)` | Construye un error nuevo con el mismo mensaje |
| `.addPropertyNode("runDv")` | Lo asocia **al campo `runDv`**. Así el frontend muestra el mensaje junto a ese campo, y `GlobalExceptionHandler` lo pone en `detalles.runDv` |
| `.addConstraintViolation();` | Registra el error |
| `return correcto;` | `false` si no corresponde |

No hace falta `@Component` ni nada de Spring: Bean Validation crea esta clase por su cuenta con su constructor vacío.

**Detalle:** con `runDv = "Z"` fallan **dos** reglas, y las dos apuntan a `runDv`. `GlobalExceptionHandler` guarda los errores en un `Map` por campo, así que el cliente ve **un** mensaje (el último). Es suficiente.

**Limitación conocida:** el validador está atado a `UsuarioRequest`. Si otra clase también lleva RUN, se generaliza con una interfaz que ambas implementen. Con una sola clase no vale la pena complicarlo.

---

## 6. `UsuarioService.java`: registrar y buscar

### 6.1 Qué reglas aplica el registro

| Regla | Qué ocurre si falla | Respuesta HTTP |
| --- | --- | --- |
| El correo no está registrado | `ReglaNegocioException("El correo ya está registrado")` | 409 |
| El RUN no está registrado | `ReglaNegocioException("El RUN ya está registrado")` | 409 |
| La comuna existe | `RecursoNoEncontradoException("Comuna", id)` | 404 |
| El rol `CLIENTE` existe | `RecursoNoEncontradoException("Rol", "CLIENTE")` | 404 |

Si todo está bien: copia los datos con el mapper, asigna el rol `CLIENTE` (**nunca lo decide el cliente**), guarda la clave como hash, guarda y devuelve un `UsuarioResponse`.

### 6.2 El código

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_CLIENTE = "CLIENTE";

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    // crearAdministradorSiNoExiste(...) sin cambios (ver guía 17)

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRun(request.run())) {
            throw new ReglaNegocioException("El RUN ya está registrado");
        }

        Comuna comuna = comunaRepository.findById(request.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", request.idComuna()));

        TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_CLIENTE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_CLIENTE));

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setRunDv(request.runDv().toUpperCase());
        usuario.setContrasenia(passwordEncoder.encode(request.clave()));
        usuario.setTipoUsuario(rol);
        usuario.setComuna(comuna);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario registrado con el correo {}", guardado.getCorreo());
        return usuarioMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        return usuarioMapper.toResponse(usuario);
    }
}
```

> El campo `usuarioMapper` va **al final** de la lista de campos `final`: Lombok arma el constructor en ese orden y `UsuarioServiceTest` lo usa así.

### 6.3 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `private final UsuarioMapper usuarioMapper;` | Spring inyecta el `UsuarioMapperImpl` que MapStruct generó (por eso existe `componentModel = "spring"`). Lombok lo agrega solo al constructor |
| `@Transactional` en `registrar` | **Todo o nada**: si algo falla, no queda nada a medias. Además mantiene abierta la sesión de Hibernate hasta el final del método, lo que importa para el mapper (6.4) |
| `existsByCorreo(...)` / `existsByRun(...)` | Los métodos del [paso 4](14-repositories-explicados.md). Devuelven `Boolean`; Java lo desempaqueta solo en el `if` |
| `throw new ReglaNegocioException(...)` | `throw` lanza la excepción y **sale del método** en ese punto. Un `throw` dentro de un `@Transactional` provoca el **rollback** |
| `findById(request.idComuna())` | `idComuna` es `Integer`: el tipo del `@Id` de `Comuna` |
| `usuarioMapper.toEntity(request)` | Copia los 8 campos simples. Lo que el mapper ignoró (`id`, contraseña, `activo`, rol, comuna) queda vacío o con su valor inicial |
| `usuario.setRunDv(request.runDv().toUpperCase())` | Normaliza la `k` a `K` antes de guardar |
| `passwordEncoder.encode(request.clave())` | **El único lugar** donde la clave en texto plano se convierte en hash. Nunca llega a la entidad sin pasar por aquí |
| `usuario.setTipoUsuario(rol)` | El rol lo fija el service: el cliente no puede elegirlo |
| `usuarioRepository.save(usuario)` | `INSERT`. Devuelve la entidad guardada (ya con `id`) |
| `return usuarioMapper.toResponse(guardado)` | Convierte a DTO **antes de salir** del método |
| `@Transactional(readOnly = true)` | Indica que el método solo lee: pista para Spring e Hibernate y documentación de la intención. La transacción sigue abierta, así que el mapper puede cargar la región |

### 6.4 Por qué `toResponse` va dentro del service

`toResponse` lee `comuna.region.nombre`, y la región es `LAZY`. Hibernate solo puede cargarla **mientras la sesión está abierta**, es decir, dentro de la transacción. Si el service devolviera la entidad y el controller la mapeara después, saldría `LazyInitializationException`. Devolviendo el DTO ya armado, el problema desaparece.

> **Ojo con `open-in-view`:** el diseño y los docs suponen `spring.jpa.open-in-view: false`, pero el `application.yaml` del ZIP del 19 oct decía `true`. Con `true` la sesión sigue abierta durante toda la petición y el error **se esconde**; con `false` se ve enseguida. Ver [Cambios recientes](../estado/cambios-recientes.md#pendiente).

### 6.5 Dos aclaraciones de diseño

- **Dos registros simultáneos con el mismo correo.** Los dos podrían pasar `existsByCorreo` y chocar en el `INSERT`. La base lo detiene con su `UNIQUE`, y el handler ya convierte esa excepción (`DataIntegrityViolationException`) en un 409. Son **dos barreras**: la del service da un mensaje claro; la de la base es la que garantiza.
- **Comuna inexistente = 404.** Es discutible (algunos equipos prefieren un 400), pero el handler no tiene un 422/400 para reglas de negocio, así que se usa la excepción que ya existe.

---

## 7. `UsuarioController.java`

> **Actualización 20 oct:** el controller ganó `GET /api/usuarios/me` y `@SecurityRequirements` en el registro, y `buscarPorId` pasó a ser solo para administrador. La versión vigente y la explicación de la seguridad están en la [guía 21, sección 11](21-seguridad-jwt-y-roles-explicada.md#11-lo-que-cambió-en-usuariocontroller-y-usuarioservice). Lo explicado aquí (`@RestController`, `@Valid`, 201 con `Location`) sigue siendo válido.

```java
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro y consulta de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Registrar un usuario con rol CLIENTE")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un usuario por id")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }
}
```

| Línea | Qué significa |
| --- | --- |
| `@RestController` | Controller cuyas respuestas se convierten **a JSON** (es `@Controller` + `@ResponseBody`) |
| `@RequestMapping("/api/usuarios")` | Prefijo común de todas las rutas de la clase |
| `@RequiredArgsConstructor` + `private final UsuarioService` | Inyección por constructor, igual que en el service |
| `@Tag` y `@Operation` | Solo **rotulan Swagger** (springdoc). No cambian el comportamiento |
| `@PostMapping` | Responde a `POST /api/usuarios` |
| `@Valid` | Ejecuta las validaciones del Request (8.2 y 8.3) **antes** de entrar al método. Si fallan, el método ni se ejecuta y se lanza `MethodArgumentNotValidException` (el handler la convierte en 400) |
| `@RequestBody` | Convierte el JSON de la petición en un `UsuarioRequest` (Jackson). Si el JSON está roto: `HttpMessageNotReadableException` (400) |
| `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")` | Arma la URL del recurso nuevo a partir de la petición actual: `http://.../api/usuarios/7` |
| `ResponseEntity.created(ubicacion).body(creado)` | Responde **201 Created** con la cabecera `Location` y el JSON. Es la convención REST para un recurso creado |
| `@GetMapping("/{id}")` | Responde a `GET /api/usuarios/7` |
| `@PathVariable Long id` | Toma el `{id}` de la URL y lo convierte a `Long`. Si no es un número: `MethodArgumentTypeMismatchException` (400, ver [11, 8.11](11-excepciones-explicadas.md#811-methodargumenttypemismatchexception--400)) |
| `return usuarioService.buscarPorId(id)` | Devuelve el DTO directamente; Spring responde **200** |

**El controller no tiene lógica:** valida, llama al service y devuelve. Si aparece un `if` de negocio aquí, probablemente pertenece al service.

---

## 8. Las pruebas

### 8.1 `RunUtilTest` (4 tests)

Comprueba el algoritmo con valores calculados aparte: `12345678 → 5`, `11111111 → 1`, `22222222 → 2`, `6000000 → K` (el caso que da 10), `14000000 → 0` (el caso que da 11) y los extremos `1 → 9` y `99999999 → 9`. **Los dos casos especiales (`K` y `0`) son los que más fácil se rompen**, por eso tienen su propio test.

### 8.2 Dos tests nuevos en `UsuarioRequestTest` (ahora 10)

| Test | Qué comprueba |
| --- | --- |
| `digitoVerificadorQueNoCorrespondeAlRunEsInvalido` | RUN `12345678` con dígito `9`: pasa el `@Pattern` (es un número) pero no corresponde, y falla **solo** `runDv` |
| `digitoVerificadorKEnMinusculaEsValidoSiCorresponde` | `6000000` con `k` minúscula es válido |

### 8.3 `UsuarioServiceTest` (5 tests, sin base de datos)

Usa **Mockito** (viene con `spring-boot-starter-test`): se le entregan repositories falsos al service.

| Elemento | Qué hace |
| --- | --- |
| `@ExtendWith(MockitoExtension.class)` | Activa Mockito en JUnit: crea los `@Mock` antes de cada test |
| `@Mock private UsuarioRepository ...` | Objeto falso que simula el repository sin tocar la base. Por defecto devuelve `false`, `Optional.empty()` o `null` |
| `new UsuarioService(...)` en `@BeforeEach` | El service se crea a mano con los falsos (posible gracias a la inyección por constructor). El **mapper es el real** (`UsuarioMapperImpl`) porque no depende de nada |
| `when(x.metodo(...)).thenReturn(valor)` | "Cuando se llame a este método con estos argumentos, devuelve este valor" |
| `thenAnswer(inv -> inv.getArgument(0))` | El `save` falso devuelve el mismo objeto que recibió, como haría el real |
| `ArgumentCaptor` + `verify(...).save(captor.capture())` | Atrapa el `Usuario` que el service le pasó a `save`, para inspeccionarlo |
| `assertThrows(Clase.class, () -> ...)` | Comprueba que ese código lanza esa excepción |
| `verify(usuarioRepository, never()).save(any())` | Comprueba que **nunca** se guardó: si hay un error, no se guarda nada |

Los cinco tests: registro correcto (hash, `K` en mayúscula, rol `CLIENTE`, comuna y región en la respuesta), correo repetido, RUN repetido, comuna inexistente y usuario inexistente al buscar.

> **`UnnecessaryStubbingException`:** con `MockitoExtension`, si programas un `when(...)` que el test no usa, el test falla. Por eso cada test programa solo lo que realmente se ejecuta antes de que el service se detenga.

### 8.4 `UsuarioControllerTest` (8 tests, sin base de datos)

> **Actualización 20 oct:** ahora tiene **14 tests** (el 415 y, con la seguridad, 401, 403, `/me` y el token Bearer) y tres `@MockitoBean`: `UsuarioService`, `JwtService` y `UserDetailsService`. Ver [21, sección 13](21-seguridad-jwt-y-roles-explicada.md#13-las-pruebas-56-en-total).

```java
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UsuarioService usuarioService;
    ...
}
```

| Elemento | Qué hace |
| --- | --- |
| `@WebMvcTest(UsuarioController.class)` | Levanta **solo la capa web** (controller, `GlobalExceptionHandler`, Jackson, Spring Security). No hay base de datos, ni Redis, ni Flyway |
| `@Import(SecurityConfig.class)` | Sin esto, Spring Security aplicaría su configuración por defecto (exige credenciales y CSRF) y el `POST` daría 401/403. Con esto se usa **tu** configuración |
| `MockMvc` | Simula peticiones HTTP sin abrir un puerto: `mockMvc.perform(post(...))` |
| `@MockitoBean` | Reemplaza `UsuarioService` por un falso dentro del contexto de Spring (en versiones anteriores se llamaba `@MockBean`) |
| `jsonPath("$.detalles.runDv").exists()` | Comprueba un campo del JSON de respuesta |

| Test | Esperado |
| --- | --- |
| Registro válido | 201, `Location: http://localhost/api/usuarios/7`, y **sin** `clave` ni `contrasenia` en el JSON |
| Dígito verificador incorrecto | 400 con `detalles.runDv`; el service **nunca** se llama |
| JSON roto | 400 y "El cuerpo de la petición no es un JSON válido" |
| Correo repetido | 409 con el mensaje de la excepción |
| GET existente | 200 |
| GET inexistente | 404 |
| GET `/abc` | 400 y "El parámetro 'id' tiene un formato inválido" |
| `DELETE /api/usuarios` | 405 |

**Estos tests también cubren por primera vez el `GlobalExceptionHandler`**: era la prueba pendiente desde el 18 oct ([11, sección 12](11-excepciones-explicadas.md#12-cómo-probar-cada-error)).

---

## 9. Comandos importantes

```bash
# Las pruebas de hoy
./mvnw test -Dtest="UsuarioRequestTest,RunUtilTest"      # 14 tests
./mvnw test -Dtest=UsuarioServiceTest                    # 5 tests
./mvnw test -Dtest=UsuarioControllerTest                 # 8 tests

# Todas, sin la prueba que necesita la base (TecnoFactoryApplicationTests)
./mvnw test -Dtest="*Test"
```

Probar a mano (con la app arrancada y Swagger en `/swagger-ui/index.html`):

```bash
curl -i -X POST localhost:8080/api/usuarios -H "Content-Type: application/json" -d '{
  "run": 12345678, "runDv": "5", "nombre": "Ana", "apellidos": "Pérez",
  "correo": "ana@correo.cl", "clave": "Clave12345",
  "fechaNacimiento": "1990-05-20", "direccion": "Calle Falsa 123",
  "telefono": "+56912345678", "idComuna": 13101 }'
```

| Prueba | Esperado |
| --- | --- |
| La petición de arriba | 201, `Location` y JSON **sin** clave |
| La misma petición otra vez | 409 "El correo ya está registrado" |
| Mismo `run` con otro correo | 409 "El RUN ya está registrado" |
| `"runDv": "9"` | 400 con `detalles.runDv` |
| `"idComuna": 999999` | 404 "Comuna no encontrado con id 999999" |
| `GET /api/usuarios/1` | 200 |
| `GET /api/usuarios/999999` | 404 |
| `GET /api/usuarios/abc` | 400 |
| `SELECT contrasenia FROM usuario WHERE correo = 'ana@correo.cl';` | Empieza con `$2a$10$`, largo 60 |

---

## 10. Experimentos (3 minutos cada uno)

| Experimento | Qué verás |
| --- | --- |
| Comentar `@RunValido` en `UsuarioRequest` y ejecutar los tests | Falla `digitoVerificadorQueNoCorrespondeAlRunEsInvalido`: nadie rechaza ya el `9` |
| En `registrar`, cambiar `ROL_CLIENTE` por `ROL_ADMINISTRADOR` en la búsqueda del rol | Falla `registraUnClienteConLaClaveEncriptada`: es el error de seguridad que ningún cambio futuro debe introducir sin que se note |
| Cambiar `passwordEncoder.encode(request.clave())` por `request.clave()` | Falla el `assertEquals("HASH", ...)` |
| Comentar el handler de `MethodArgumentTypeMismatchException` | `GET /api/usuarios/abc` deja de dar 400 y pasa a 500 |

Devuelve cada cambio al terminar.

---

## 11. Qué se verificó y qué no

| Comprobación | Estado |
| --- | --- |
| `RunUtilTest` y `UsuarioRequestTest` (14 tests) | **Verificado** por ti (19 oct) |
| `UsuarioServiceTest` (5 tests) y arranque con el mapper inyectado | **Verificado** por ti (19 oct) |
| `UsuarioControllerTest` (8 tests) y arranque | **Verificado** por ti (19 oct) |
| `UsuarioMapperImpl.toEntity` no copia la contraseña | **Pendiente** (`grep -c "Contrasenia"` sobre el archivo generado debe dar `0`) |
| Registro, 409, 404 y 400 contra **MySQL real**, desde Swagger | **Pendiente** (9.3) |
| El `UNIQUE` de la base como segunda barrera (409 con `DataIntegrityViolationException`) | **Pendiente**: solo se prueba con dos peticiones simultáneas |
| Que `toResponse` funcione **sin** `open-in-view` | **Pendiente**: depende de cambiar el `application.yaml` |

---

## 12. Errores relacionados

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `UnnecessaryStubbingException` | Un `when(...)` que el test no usa | Quitarlo o programar solo lo que el service alcanza a ejecutar |
| `package org.mockito does not exist` | La librería no llega con las dependencias | Revisar `spring-boot-starter-test` en el `pom.xml` |
| `POST` del test devuelve 401/403 | Falta `@Import(SecurityConfig.class)` y Spring Security usa su configuración por defecto | Importarla en el test |
| `No qualifying bean of type '...UsuarioMapper'` al arrancar | MapStruct no generó la clase | Revisar el orden de los procesadores en el `pom.xml` ([18, 7.5](18-dto-mapper-y-validaciones-explicados.md#75-cómo-está-configurado-mapstruct-en-tu-pomxml)) |
| `LazyInitializationException` al registrar | Se mapeó fuera del service, o `open-in-view` pasó a `false` y un código lee la relación fuera de la transacción | Mapear **dentro** del service |
| `GET /api/usuarios/abc` da 500 | Falta el handler de `MethodArgumentTypeMismatchException` | Agregarlo ([11, 8.11](11-excepciones-explicadas.md#811-methodargumenttypemismatchexception--400)) |
| El test falla con `expected: <[runDv]> but was: <[]>` | El dato de prueba cumple la regla | Revisar el dígito: debe **no** corresponder al RUN |

---

## 13. La receta para la próxima entidad

Cada entidad nueva repite este recorrido. Es la lista a copiar:

| # | Pieza | Ejemplo con `Usuario` | Guía |
| --- | --- | --- | --- |
| 1 | Entidad (validada contra `V1`) | `Usuario` | [13](13-entidades-explicadas.md) |
| 2 | Repository (solo los métodos que se usen) | `UsuarioRepository` | [14](14-repositories-explicados.md) |
| 3 | `Response` y `Request` con validaciones | `UsuarioResponse`, `UsuarioRequest` | [18](18-dto-mapper-y-validaciones-explicados.md) |
| 4 | Mapper (`toResponse`, `toEntity`) | `UsuarioMapper` | [18](18-dto-mapper-y-validaciones-explicados.md) |
| 5 | Service `@Transactional`, reglas de negocio, mapea **dentro** | `UsuarioService` | Esta guía, sección 6 |
| 6 | Controller sin lógica, `@Valid`, 201 con `Location` | `UsuarioController` | Esta guía, sección 7 |
| 7 | Test del service (Mockito) y del controller (`@WebMvcTest`) | `UsuarioServiceTest`, `UsuarioControllerTest` | Esta guía, sección 8 |
| 8 | Probar en Swagger | 9.3 | Sección 9 |

Los errores (`404`, `409`, `400`) **no se repiten**: ya los resuelve `GlobalExceptionHandler`. Basta con lanzar `RecursoNoEncontradoException` o `ReglaNegocioException` desde el service.

---

## 14. Lo que viene

| Paso | Qué |
| --- | --- |
| **9.3** | Probar en Swagger contra MySQL real y comprobar la fila y el hash |
| Configuración base | Lo que conviene dejar listo **antes** de la segunda entidad: ver [Cambios recientes](../estado/cambios-recientes.md#pendiente) y [10](10-pendientes-y-futuro.md) |
| Primera "rebanada" | `Categoria` (DTOs, mapper, service, controller y pruebas) siguiendo la receta de la sección 13 |
