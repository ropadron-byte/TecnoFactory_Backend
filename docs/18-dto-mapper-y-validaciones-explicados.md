# 18. DTOs, mapper y validaciones explicados

Guía para entender, línea por línea, los tres archivos de `Usuario` que describen **lo que entra y lo que sale de la API** (`UsuarioResponse`, `UsuarioRequest`, `UsuarioMapper`) y la prueba que valida las reglas de entrada (`UsuarioRequestTest`). Sirve de **ejemplo para cualquier otra entidad**: quien cree `Categoria` o `Producto` debe seguir este mismo patrón.

| Archivo | Ruta | Para qué sirve | Sección |
| --- | --- | --- | --- |
| `UsuarioResponse.java` | `src/main/java/com/factory/tecnofactory/dto/` | Lo que la API **devuelve**. No lleva contraseña | [5](#5-usuarioresponsejava) |
| `UsuarioRequest.java` | `dto/` | Lo que la API **recibe** para registrar. Lleva validaciones | [6](#6-usuariorequestjava) |
| `UsuarioMapper.java` | `mapper/` | Convierte entre entidad y DTOs (lo escribe MapStruct al compilar) | [7](#7-usuariomapperjava) |
| `UsuarioRequestTest.java` | `src/test/java/com/factory/tecnofactory/dto/` | Comprueba las validaciones sin base de datos ni controller | [9](#9-usuariorequesttestjava) |

> Estado (19 oct): los cuatro archivos **existen en tu repositorio y coinciden con esta guía** (comparados uno a uno; `UsuarioRequestTest` tiene los 8 tests). `UsuarioResponse` y `toResponse` **compilaron y se revisó el código generado** (18 oct). **Sin confirmar**: que `./mvnw compile` pase con `UsuarioRequest` y `toEntity`, y que los 8 tests pasen. La validación del dígito verificador del RUN (módulo 11) es el sub-paso **8.3**, aún pendiente.

---

## 1. Para qué sirven los DTOs

Un **DTO** (*Data Transfer Object*) es una clase que describe **exactamente lo que viaja por la API**. La entidad describe **cómo se guarda en la base**. Son cosas distintas y se mantienen separadas.

```
          JSON de entrada                              JSON de salida
Cliente ----------------> UsuarioRequest   ...   UsuarioResponse ----------------> Cliente
                              |                       ^
                              v                       |
                          [ Mapper ]  <-----------> [ Mapper ]
                              |                       ^
                              v                       |
                          Entidad Usuario  <---->  Base de datos
```

**Por qué no devolver la entidad directamente:**

| Problema | Qué pasaría con `Usuario` |
| --- | --- |
| **Filtra datos** | `contrasenia` (el hash) saldría en el JSON |
| **Relaciones `LAZY`** | `tipoUsuario` y `comuna` son proxies de Hibernate: serializarlos fuera de una transacción lanza `LazyInitializationException` |
| **Acopla la API a la base** | Si renombras una columna, cambia el JSON y se rompe el frontend |
| **Estructura anidada** | El cliente recibiría `comuna → region → ...` completo, cuando solo quiere los nombres |

### 1.1 Request y Response son distintos a propósito

| | `UsuarioRequest` (entra) | `UsuarioResponse` (sale) |
| --- | --- | --- |
| Contraseña | **Sí**, como `clave` en texto plano | **Nunca** |
| `id` | No (lo asigna la base) | Sí |
| `activo` | No (nace en `true`) | Sí |
| Rol (`tipoUsuario`) | **No** | Sí, como texto |
| Comuna | Como número (`idComuna`) | Como nombre (`comuna`, `region`) |
| Validaciones | Sí | No (son datos que ya existen) |

### 1.2 Mass assignment: por qué el Request no tiene rol

**Mass assignment** es que el cliente asigne campos que no le corresponden. Si el `UsuarioRequest` incluyera el rol, cualquiera podría registrarse como `ADMINISTRADOR` enviando un JSON con ese valor.

Por eso el Request **no tiene** `id`, `activo`, `tipoUsuario` ni `contrasenia`. Además, Spring Boot **ignora** los campos desconocidos del JSON: si alguien envía `"tipoUsuario": "ADMINISTRADOR"`, se descarta sin error. El rol lo asignará el service (`CLIENTE`).

### 1.3 Por qué la clave se llama `clave` y no `contrasenia`

Si el campo del Request se llamara igual que el de la entidad, MapStruct lo copiaría **tal cual**, y la clave en texto plano llegaría a la base. Con otro nombre (`clave`), ese descuido es imposible. Y como además la entidad tiene `@Mapping(target = "contrasenia", ignore = true)`, hay dos barreras.

---

## 2. Cuándo se ejecuta cada cosa

Cada pieza actúa en un momento distinto. Es la parte que más confunde al principio.

| Momento | Qué ocurre | Pieza |
| --- | --- | --- |
| **Al compilar** (`./mvnw compile`) | MapStruct **lee tu interfaz y escribe una clase** (`UsuarioMapperImpl.java`). Los errores de mapeo aparecen aquí | `UsuarioMapper` |
| **Al arrancar** | Spring registra `UsuarioMapperImpl` como bean (`componentModel = "spring"`) | `@Mapper` |
| **Al llegar una petición** | Spring convierte el JSON en `UsuarioRequest` y **valida**. Si falla, responde 400 **sin llegar al service** | `@Valid` (paso 9) |
| **Dentro del service** | `toEntity` copia los campos simples; el service completa lo delicado (hash, rol, comuna) y guarda | `UsuarioMapper`, `UsuarioService` |
| **Antes de responder** | `toResponse` convierte la entidad en `UsuarioResponse`. **Debe ocurrir dentro de la transacción del service** | `UsuarioMapper` |

Flujo completo de un registro (el controller y el service se escriben en el paso 9):

```
POST /api/usuarios  { "run": 12345678, "correo": "...", "clave": "..." }
   |
   v
Jackson convierte el JSON en UsuarioRequest
   |
   v
Bean Validation revisa las anotaciones  ----- falla ----->  400 con los mensajes
   |  ok
   v
UsuarioController llama a usuarioService.registrar(request)
   |
   v
UsuarioService (transacción abierta)
   |-- mapper.toEntity(request)             copia campos simples
   |-- passwordEncoder.encode(clave)        hash BCrypt
   |-- asigna rol CLIENTE y comuna          objetos, no números
   |-- usuarioRepository.save(...)          INSERT
   |-- mapper.toResponse(guardado)          lee comuna y región (LAZY, dentro de la transacción)
   v
UsuarioResponse  ->  JSON de salida (sin contraseña)
```

### 2.1 Por qué `toResponse` va dentro del service

`toResponse` lee `comuna.nombre` y `comuna.region.nombre`. Como esas relaciones son `LAZY`, Hibernate necesita una **transacción abierta** para consultarlas. Con `open-in-view: false`, esa transacción termina al salir del service. Si el controller intentara mapear después, saldría `LazyInitializationException`. Por eso **el service devuelve el DTO ya armado**, no la entidad.

---

## 3. Glosario

### 3.1 MapStruct

| Elemento | Qué significa |
| --- | --- |
| **MapStruct** | Librería que **genera código Java** para convertir un objeto en otro. Lo genera al compilar, no usa reflexión: es rápido y los errores aparecen antes de ejecutar |
| `@Mapper` | "Esta interfaz declara conversiones; escribe la implementación" |
| `componentModel = "spring"` | La clase generada se registra como bean de Spring, para poder inyectarla. Sin esto no se puede inyectar |
| `@Mapping(source, target)` | Una regla para un campo que no se llama igual o no tiene la misma forma |
| `source = "a.b.c"` | Ruta dentro de la entidad: el atributo `a`, dentro de él `b`, dentro `c`. Se genera con revisión de `null` en cada nivel |
| `ignore = true` | "No toques este campo del destino a propósito" |
| `UsuarioMapperImpl` | La clase que MapStruct escribe. Vive en `target/generated-sources/annotations/` y **no se edita ni se sube a Git** |
| `Unmapped target property: "x"` | Advertencia de MapStruct: un campo del **destino** se quedó sin valor. Se resuelve mapeándolo o ignorándolo |

Reglas automáticas de MapStruct: une por **nombre igual y tipo compatible**. Las advertencias son sobre campos del **destino** sin valor; un campo de **origen** que no se usa (por ejemplo `clave` en `toEntity`) **no** avisa.

### 3.2 Anotaciones de validación (`jakarta.validation.constraints`)

| Anotación | Qué comprueba | Notas |
| --- | --- | --- |
| `@NotNull` | No es `null` | `""` sí pasa. Para números, fechas y objetos |
| `@NotBlank` | No es `null`, ni vacío, ni solo espacios | Solo para `String` |
| `@Size(min, max)` | Largo de un texto | Copia los límites de las columnas de `V1` |
| `@Min(n)` / `@Max(n)` | Valor numérico mínimo y máximo | |
| `@Email` | Formato de correo | Acepta `ana@correo` (sin `.cl`); no comprueba que el dominio exista |
| `@Pattern(regexp)` | El texto debe coincidir **entero** con la expresión regular | No necesita `^` ni `$` |
| `@Past` | La fecha es anterior a hoy | |
| `message = "..."` | El texto que verá el cliente | En español, para que el frontend lo muestre tal cual |

Las anotaciones **no hacen nada por sí solas**: alguien debe pedir la validación. En el controller será `@Valid` ([paso 9](10-pendientes-y-futuro.md#siguiente-tramo-de-la-fase-2-administrador-al-arrancar-y-primer-controller)); en el test, `validator.validate(...)`.

### 3.3 Expresiones regulares usadas

| Expresión | Significa |
| --- | --- |
| `[0-9kK]` | Un solo carácter: un dígito, `k` o `K` |
| `\\+?[0-9]{8,14}` | `\\` es una `\` dentro de un `String` de Java. Un `+` opcional (`?`) y de 8 a 14 dígitos (`{8,14}`) |

---

## 4. Lo que comparten los tres archivos

- Los DTOs son `record`: datos inmutables con constructor, getters **sin** `get` (`request.correo()`), `equals`, `hashCode` y `toString` generados ([17, sección 4](17-administrador-inicial-explicado.md#4-datosadministradorjava)).
- Los nombres van en `camelCase` (`runDv`, `fechaNacimiento`): es lo que saldrá en el JSON.
- Un `record` de entrada con un campo secreto **debe sobrescribir `toString()`**, porque el automático imprime todos los campos.

---

## 5. `UsuarioResponse.java`

```java
package com.factory.tecnofactory.dto;

import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        Integer run,
        String runDv,
        String nombre,
        String apellidos,
        String correo,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        Boolean activo,
        String tipoUsuario,
        String comuna,
        String region
) {
}
```

| Línea | Qué significa |
| --- | --- |
| `package ...dto;` | Los DTOs viven en el paquete `dto` |
| `import java.time.LocalDate;` | Tipo de la fecha |
| `public record UsuarioResponse(` | Un record: lo que se devuelve, inmutable |
| `Long id` | Id de la entidad (`BIGINT`) |
| `Integer run`, `String runDv` | RUN sin dígito y dígito verificador, por separado |
| `String nombre, apellidos, correo` | Mismos nombres que la entidad: MapStruct los une solo |
| `LocalDate fechaNacimiento` | Jackson lo escribe como `"1990-05-20"` |
| `String direccion, telefono` | Igual que la entidad |
| `Boolean activo` | Si la cuenta está activa |
| `String tipoUsuario` | El **nombre** del rol (`"ADMINISTRADOR"`), no el objeto. Viene de `usuario.tipoUsuario.nombre` |
| `String comuna` | El nombre de la comuna. Viene de `usuario.comuna.nombre` |
| `String region` | El nombre de la región. Viene de `usuario.comuna.region.nombre` |
| *(ausente)* `contrasenia` | **A propósito.** MapStruct construye el record mirando **sus** campos; como no existe, no puede salir. La seguridad viene del diseño del DTO |

> **Regla para el equipo:** no agregar nunca `contrasenia` a un `Response`. MapStruct la uniría con la entidad por tener el mismo nombre y el hash saldría en el JSON.

---

## 6. `UsuarioRequest.java`

```java
package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UsuarioRequest(

        @NotNull(message = "El RUN es obligatorio")
        @Min(value = 1_000_000, message = "El RUN no es válido")
        @Max(value = 99_999_999, message = "El RUN no es válido")
        Integer run,

        @NotBlank(message = "El dígito verificador es obligatorio")
        @Pattern(regexp = "[0-9kK]", message = "El dígito verificador debe ser un número o K")
        String runDv,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 255, message = "Los apellidos no pueden superar los 255 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String correo,

        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        String clave,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
        String direccion,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\+?[0-9]{8,14}", message = "El teléfono debe tener entre 8 y 14 dígitos, con + opcional")
        String telefono,

        @NotNull(message = "La comuna es obligatoria")
        Integer idComuna
) {

    @Override
    public String toString() {
        return "UsuarioRequest[correo=" + correo + ", run=" + run + "-" + runDv + "]";
    }
}
```

### 6.1 Campo por campo

| Campo | Anotaciones | Por qué |
| --- | --- | --- |
| `Integer run` | `@NotNull` + `@Min(1_000_000)` + `@Max(99_999_999)` | Es un número: `@NotNull` (no `@NotBlank`). El rango es el de un RUN real. `1_000_000` es `1000000` (Java admite guiones bajos para leer mejor) |
| `String runDv` | `@NotBlank` + `@Pattern("[0-9kK]")` | Un solo carácter válido. **No** comprueba que corresponda al RUN: eso es el 8.3 |
| `String nombre` | `@NotBlank` + `@Size(max = 255)` | La columna es `VARCHAR(255)`. Sin `@Size`, un texto largo llegaría a MySQL y fallaría con `Data too long` (un 500 poco claro); con `@Size` es un 400 claro |
| `String apellidos` | Igual que `nombre` | |
| `String correo` | `@NotBlank` + `@Email` + `@Size(max = 255)` | Será el identificador de inicio de sesión. La unicidad **no** se valida aquí: se comprueba en el service con `existsByCorreo` |
| `String clave` | `@NotBlank` + `@Size(min = 8, max = 72)` | Mínimo razonable. BCrypt solo considera los primeros 72 bytes; limitarla evita que dos claves largas distintas se vuelvan equivalentes |
| `LocalDate fechaNacimiento` | `@NotNull` + `@Past` | Una fecha futura no es una fecha de nacimiento |
| `String direccion` | `@NotBlank` + `@Size(max = 255)` | |
| `String telefono` | `@NotBlank` + `@Pattern(...)` | La columna mide 15: `+` más 14 dígitos es el máximo |
| `Integer idComuna` | `@NotNull` | Es el **código** (CUT) de la comuna. Que exista se comprueba en el service |

### 6.2 El `toString()` sobrescrito

```java
@Override
public String toString() {
    return "UsuarioRequest[correo=" + correo + ", run=" + run + "-" + runDv + "]";
}
```

El `toString()` automático de un record imprime **todos** los campos, incluida la `clave`. Con este, un `log.debug("{}", request)` accidental no la revela. Es una **red de seguridad**: no reemplaza la regla de no registrar nunca la clave. `DatosAdministrador` ([17](17-administrador-inicial-explicado.md#4-datosadministradorjava)) tiene el mismo riesgo y conviene darle el mismo tratamiento.

### 6.3 Qué se valida dónde

| Qué | Dónde | Por qué ahí |
| --- | --- | --- |
| Formato, largos, obligatoriedad | `UsuarioRequest` (anotaciones) | Se puede comprobar mirando solo el dato |
| Dígito verificador **correcto** | 8.3: validación personalizada | Depende de dos campos a la vez |
| Correo o RUN **ya registrados** | `UsuarioService` | Requiere consultar la base |
| Comuna **existente** | `UsuarioService` | Requiere consultar la base |

---

## 7. `UsuarioMapper.java`

```java
package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "tipoUsuario.nombre", target = "tipoUsuario")
    @Mapping(source = "comuna.nombre", target = "comuna")
    @Mapping(source = "comuna.region.nombre", target = "region")
    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasenia", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "tipoUsuario", ignore = true)
    @Mapping(target = "comuna", ignore = true)
    Usuario toEntity(UsuarioRequest request);
}
```

### 7.1 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `import org.mapstruct.Mapper` / `Mapping` | Las dos anotaciones de MapStruct |
| `import ...dto.*`, `...entity.Usuario` | Los tres tipos que se convierten |
| `@Mapper(componentModel = "spring")` | Genera la implementación **y** la registra como bean |
| `public interface UsuarioMapper` | Una **interfaz**: declaras *qué* conversión quieres; MapStruct escribe el *cómo* al compilar |
| `UsuarioResponse toResponse(Usuario usuario);` | Convierte la entidad en la salida. Entrada entre paréntesis, salida a la izquierda |
| `@Mapping(source = "tipoUsuario.nombre", target = "tipoUsuario")` | Aplana: del objeto `TipoUsuario` toma solo su `nombre` y lo pone en el campo `String tipoUsuario` del DTO |
| `@Mapping(source = "comuna.nombre", target = "comuna")` | Igual, para la comuna |
| `@Mapping(source = "comuna.region.nombre", target = "region")` | Recorre **dos** niveles: comuna y luego región |
| `Usuario toEntity(UsuarioRequest request);` | Convierte la entrada en una entidad **incompleta** |
| `@Mapping(target = "id", ignore = true)` | La base lo asigna (`AUTO_INCREMENT`) |
| `@Mapping(target = "contrasenia", ignore = true)` | **El mapper no debe ver ni hashear contraseñas**; lo hace el service |
| `@Mapping(target = "activo", ignore = true)` | La entidad ya lo inicia en `true` |
| `@Mapping(target = "tipoUsuario", ignore = true)` | El rol lo decide el service, no el cliente |
| `@Mapping(target = "comuna", ignore = true)` | El Request trae un número (`idComuna`); el service busca la entidad |

### 7.2 Qué copia MapStruct por su cuenta

En `toEntity`: `run`, `runDv`, `nombre`, `apellidos`, `correo`, `fechaNacimiento`, `direccion` y `telefono` (mismo nombre en ambos lados). En `toResponse`: `id`, `run`, `runDv`, `nombre`, `apellidos`, `correo`, `fechaNacimiento`, `direccion`, `telefono` y `activo`.

### 7.3 Por qué `ignore = true` explícito

Si no se declara, MapStruct emite `Unmapped target property: "contrasenia"` en cada compilación, y a la larga uno aprende a ignorar las advertencias. Con `ignore = true`, el código dice que **se omite a propósito**.

### 7.4 El reparto entre mapper y service

```
UsuarioRequest --[mapper: copia lo simple]--> Usuario (incompleto)
                                                  |
                                    [service: completa lo delicado]
                                    - hash de la clave
                                    - rol CLIENTE
                                    - objeto Comuna
                                                  v
                                           Usuario listo --> save()
```

Es el patrón que debe seguir cualquier entidad: **el mapper copia datos; las decisiones y los secretos son del service.**

### 7.5 Cómo está configurado MapStruct en tu `pom.xml`

Ya viene listo (MapStruct 1.6.3). En el plugin del compilador, los procesadores van en este **orden**: Lombok, `lombok-mapstruct-binding`, MapStruct. Importa porque MapStruct necesita ver los getters y setters que Lombok genera. Si lo reordenas, el error típico es que el código generado no encuentra `getNombre()`.

---

## 8. El código que MapStruct genera

Después de `./mvnw compile`:

```bash
cat target/generated-sources/annotations/com/factory/tecnofactory/mapper/UsuarioMapperImpl.java
```

| Qué buscar | Qué significa |
| --- | --- |
| `@Component` | La clase es un bean de Spring (por `componentModel = "spring"`) |
| `implements UsuarioMapper` | Implementa tu interfaz |
| `if ( usuario == null ) { return null; }` | Protección contra entrada nula |
| `usuario.getId()`, `getRun()`... | Los getters que generó Lombok |
| Métodos auxiliares que recorren `comuna.region.nombre` revisando `null` | La ruta con puntos se genera segura frente a `null` |
| `new UsuarioResponse( id, run, runDv, ... )` | Construye el record con su constructor |
| En `toEntity`: `usuario.setRun(...)`, `setNombre(...)`... | Solo los campos simples |
| **Sin** `setContrasenia`, `setTipoUsuario`, `setComuna`, `setId` | Los `ignore = true` |
| **Sin** `getContrasenia` en ningún método | La contraseña nunca se lee |

---

## 9. `UsuarioRequestTest.java`

**Para qué sirve:** ejecuta las validaciones **sin** arrancar Spring ni la base de datos. Todavía no hay controller, pero las reglas ya se pueden comprobar, y quedan protegidas para siempre: si alguien cambia una regla sin querer, el test falla.

```java
package com.factory.tecnofactory.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioRequestTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static UsuarioRequest crear(String runDv, String correo, String clave,
                                        LocalDate fecha, String telefono) {
        return new UsuarioRequest(12345678, runDv, "Ana", "Pérez", correo, clave,
                fecha, "Calle Falsa 123", telefono, 13101);
    }

    private static UsuarioRequest valido() {
        return crear("5", "ana@correo.cl", "Clave12345",
                LocalDate.of(1990, 5, 20), "+56912345678");
    }

    private static Set<String> camposConError(UsuarioRequest request) {
        Set<ConstraintViolation<UsuarioRequest>> errores = validator.validate(request);
        return errores.stream()
                .map(e -> e.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    // ... ocho métodos @Test (ver sección 9.2)
}
```

### 9.1 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `class UsuarioRequestTest` | Sin `public`: JUnit no lo exige. El nombre termina en `Test` (convención) |
| `import org.junit.jupiter.api.Test` / `BeforeAll` | JUnit 5: la anotación de cada prueba y la de preparación |
| `import static ...Assertions.assertEquals` | `import static` permite escribir `assertEquals(...)` sin el prefijo `Assertions.` |
| `private static Validator validator;` | El validador, compartido por todos los tests |
| `@BeforeAll static void crearValidador()` | Se ejecuta **una vez** antes de todos los tests (debe ser `static`) |
| `Validation.buildDefaultValidatorFactory().getValidator()` | Crea el **mismo validador que Spring usará** con `@Valid`, sin arrancar Spring ni la base. Por eso el test tarda milisegundos |
| `crear(...)` | Construye un `UsuarioRequest` variando solo los datos que cada test cambia. El RUN `12345678` y el resto son fijos |
| `valido()` | Un Request correcto. Cada test cambia **un solo** dato respecto a él |
| `camposConError(request)` | Devuelve los **nombres de los campos** que fallaron |
| `validator.validate(request)` | Ejecuta todas las anotaciones; devuelve las violaciones (vacío si todo está bien) |
| `e.getPropertyPath().toString()` | El nombre del campo que falló (`"correo"`, `"clave"`...) |
| `.collect(Collectors.toSet())` | Los junta en un conjunto, sin repetidos |

### 9.2 Los ocho tests

| Test | Qué comprueba | Cómo |
| --- | --- | --- |
| `datosValidosNoGeneranErrores` | Un Request correcto no genera errores | `assertTrue(camposConError(valido()).isEmpty())` |
| `correoSinFormatoEsInvalido` | `"no-es-un-correo"` falla **solo** en `correo` | `assertEquals(Set.of("correo"), ...)` |
| `claveCortaEsInvalida` | `"abc"` falla solo en `clave` | |
| `digitoVerificadorFueraDeRangoEsInvalido` | `"Z"` falla solo en `runDv` | |
| `fechaFuturaEsInvalida` | Mañana falla solo en `fechaNacimiento` | `LocalDate.now().plusDays(1)` |
| `telefonoConLetrasEsInvalido` | `"abc"` falla solo en `telefono` | |
| `todosLosCamposSonObligatorios` | Con todo en `null` fallan los **10** campos | `new UsuarioRequest(null, ..., null)` y `assertEquals(10, ...size())` |
| `toStringNoRevelaLaClave` | El `toString` no contiene la clave | `assertFalse(valido().toString().contains("Clave12345"))` |

`assertEquals(Set.of("correo"), ...)` es estricto a propósito: comprueba que falló **exactamente** ese campo y ningún otro. Así un test no pasa por casualidad.

---

## 10. Comandos importantes

| Comando | Qué hace |
| --- | --- |
| `./mvnw compile` | Compila el proyecto. **MapStruct genera aquí `UsuarioMapperImpl`**; los errores de mapeo aparecen en este paso |
| `cat target/generated-sources/annotations/com/factory/tecnofactory/mapper/UsuarioMapperImpl.java` | Muestra el código que escribió MapStruct |
| `find target/generated-sources -name "*.java"` | Lista todo lo generado (si no encuentras la ruta exacta) |
| `grep -c "Contrasenia" target/generated-sources/annotations/com/factory/tecnofactory/mapper/UsuarioMapperImpl.java` | Cuenta las líneas que mencionan la contraseña. **Esperado: `0`** |
| `./mvnw test -Dtest=UsuarioRequestTest` | Ejecuta **solo** este test. `-Dtest=` filtra; el test `contextLoads` necesita MySQL y variables, por eso conviene filtrar |
| `./mvnw test` | Ejecuta todos los tests (incluido `contextLoads`, que arranca la app y necesita la base) |
| `./mvnw compile 2>&1 \| grep -E "WARNING\|ERROR"` | Muestra solo advertencias y errores. Aquí aparecen los `Unmapped target property` |

**Resultado esperado del test:** `Tests run: 8, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

---

## 11. Qué se verificó y qué no

| Comprobación | Estado |
| --- | --- |
| `UsuarioResponse` y `toResponse` compilan | **Verificado** (18 oct) |
| El `UsuarioMapperImpl` generado se revisó: está todo menos la contraseña | **Verificado** por ti (18 oct) |
| `UsuarioRequest`, `toEntity` y el test existen en tu repositorio y coinciden con esta guía | **Verificado** (19 oct, comparación de archivos) |
| `UsuarioRequest` y `toEntity` compilan | **Pendiente** |
| `UsuarioMapperImpl.toEntity` no llama a `setContrasenia`, `setTipoUsuario`, `setComuna` ni `setId` | **Pendiente** (`grep -c "Contrasenia"` debe dar `0`) |
| Los 8 tests de `UsuarioRequestTest` pasan | **Pendiente**: no se ejecutaron donde se escribió esta guía |
| Que las validaciones respondan 400 con `@Valid` en un controller | **Pendiente**: paso 9 |
| Que `toResponse` funcione dentro de una transacción (sin `LazyInitializationException`) | **Pendiente**: paso 9 |

---

## 12. Errores relacionados

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Unmapped target property: "x"` al compilar | Un campo del destino no se llena | Mapearlo con `@Mapping` o ignorarlo con `ignore = true` |
| `No property named "x" exists in source parameter(s)` | Error de tipeo en `source = "..."` | Usar los **atributos de la entidad** (no las columnas) |
| `No qualifying bean of type '...UsuarioMapper'` | Falta `componentModel = "spring"` | Agregarlo a `@Mapper` |
| El código generado no encuentra `getNombre()` | Orden de procesadores o Lombok sin procesar | Revisar el orden en el `pom.xml` (7.5) |
| `No validator could be created for constraint '...'` | Una anotación aplicada a un tipo que no admite (por ejemplo `@Min` sobre un `String`) | Usar la anotación correcta para el tipo ([3.2](#32-anotaciones-de-validación-jakartavalidationconstraints)) |
| El test falla con `expected: <[correo]> but was: <[]>` | La regla no se cumple: el dato de prueba es válido para esa anotación | Revisar la anotación o el dato |
| Un campo con `@NotNull` acepta `""` | `@NotNull` solo rechaza `null` | Usar `@NotBlank` para textos |
| `Data too long for column '...'` en la base | Falta `@Size` en ese campo | Agregar `@Size(max = ...)` con el largo de la columna |

## 13. Lo que viene

| Paso | Qué |
| --- | --- |
| **8.3** | Validación personalizada: que el dígito verificador **corresponda** al RUN (módulo 11) |
| **9.1** | `UsuarioService`: registrar (hash, rol `CLIENTE`, unicidad de correo y RUN, comuna existente) y buscar por id |
| **9.2** | `UsuarioController` con `@Valid` |
| **9.3** | Probar en Swagger y ver los errores formateados por el `GlobalExceptionHandler` |
