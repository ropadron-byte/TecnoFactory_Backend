# 11. Excepciones explicadas

Guía para entender, línea por línea, las cuatro clases del paquete `exception/` y cada error que la API puede devolver. Está pensada para leerse de arriba hacia abajo: primero los conceptos, luego las librerías, después cada archivo y al final cada error por separado.

Archivos que se explican (ruta `src/main/java/com/factory/tecnofactory/exception/`):

| Archivo | Para qué sirve | Sección |
| --- | --- | --- |
| `RecursoNoEncontradoException.java` | Avisar que algo que se buscó no existe | [4](#4-recursonoencontradoexceptionjava) |
| `ReglaNegocioException.java` | Avisar que una regla del negocio impide la operación | [5](#5-reglanegocioexceptionjava) |
| `ErrorResponse.java` | Forma única del JSON de error | [6](#6-errorresponsejava) |
| `GlobalExceptionHandler.java` | Atrapar las excepciones y convertirlas en `ErrorResponse` | [7](#7-globalexceptionhandlerjava) |

> Estado: los cuatro archivos fueron revisados leyéndolos; **todavía no se han compilado ni probado** con un controller (ver [Cambios recientes](cambios-recientes.md#qué-no-se-pudo-verificar)).

---

## 1. Conceptos básicos

### 1.1 Qué es una excepción

Una **excepción** es un objeto que Java crea cuando algo sale mal mientras el programa corre: no se encontró un registro, un dato es inválido, la base rechazó un valor. Se "lanza" con `throw` y, al lanzarse, el programa **deja de ejecutar el resto del método** y sube por las llamadas (`repository` ← `service` ← `controller`) buscando a alguien que la atrape.

```java
throw new RecursoNoEncontradoException("Producto", 5);
// todo lo que estaba después de esta línea NO se ejecuta
```

### 1.2 Qué pasa si nadie la atrapa

Spring responde con su error por defecto, que:

- no tiene el formato que tu frontend espera;
- cambia según la configuración (`server.error.include-message`, `include-stacktrace`...);
- en el peor caso devuelve 500 para cosas que eran culpa del cliente (por ejemplo, un JSON mal escrito).

### 1.3 Qué se logra con el manejo centralizado

Con `GlobalExceptionHandler` **todos** los errores salen con el mismo formato, el código HTTP correcto y sin datos internos. El frontend (React) solo necesita entender una forma:

```json
{
  "timestamp": "2026-10-17T10:15:30.123",
  "status": 404,
  "error": "Not Found",
  "mensaje": "Producto no encontrado con id 5",
  "ruta": "/api/productos/5",
  "detalles": null
}
```

Además, los services quedan limpios: lanzan la excepción y no se preocupan de armar la respuesta.

### 1.4 Excepciones comprobadas y no comprobadas

| Tipo | Se declara con `throws` | Ejemplos | Rollback en `@Transactional` |
| --- | --- | --- | --- |
| **Comprobadas** (extienden `Exception`) | Sí, obligatorio | `IOException` | **No** (por defecto) |
| **No comprobadas** (extienden `RuntimeException`) | No | `IllegalArgumentException`, las tuyas | **Sí** (por defecto) |

Tus dos excepciones extienden `RuntimeException` por dos razones:

1. No hay que escribir `throws ...` en cada método del service y del controller.
2. Spring deshace la transacción automáticamente cuando sale una excepción no comprobada. Esto es clave en el pago: si falta stock a la mitad de crear una boleta, **no queda nada a medias** en la base.

#### 1.4.0 Excepciones comprobadas y no comprobadas

Java divide las excepciones en dos familias según una sola pregunta: **¿el compilador te obliga a tratarlas?** De la respuesta dependen tres cosas: cuánto código extra tienes que escribir, si Spring deshace los cambios en la base cuando algo falla y qué tipo conviene para cada error.

#### 1.4.1 La jerarquía

```
Throwable
├── Error                                 fallos graves de la JVM (OutOfMemoryError, StackOverflowError)
└── Exception
    ├── IOException, SQLException, ...    COMPROBADAS (checked)
    └── RuntimeException                  NO COMPROBADAS (unchecked)
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── RecursoNoEncontradoException  ← tuya
        └── ReglaNegocioException         ← tuya
```

La regla para clasificar cualquier excepción es mirar de quién hereda:

| Si hereda de... | Tipo |
| --- | --- |
| `Exception`, pero **no** de `RuntimeException` | **Comprobada** |
| `RuntimeException` | **No comprobada** |
| `Error` | No comprobada (y no se debe atrapar, ver 1.4.6) |

#### 1.4.2 Qué significa "comprobada": el compilador te vigila

`IOException` es comprobada. Este código **no compila**:

```java
public String leer(Path ruta) {
    return Files.readString(ruta);   // error: Unhandled exception: java.io.IOException
}
```

Java exige una de dos decisiones:

```java
// Opción A: tratarla aquí mismo
public String leer(Path ruta) {
    try {
        return Files.readString(ruta);
    } catch (IOException e) {
        return "";                    // decidir qué hacer si falla
    }
}

// Opción B: declararla con throws y pasar el problema a quien me llame
public String leer(Path ruta) throws IOException {
    return Files.readString(ruta);
}
```

Con la opción B, **quien llame a `leer` queda en la misma situación**: debe tratarla o declarar también `throws IOException`. Así la obligación sube por todas las capas.

#### 1.4.3 Qué significa "no comprobada": el compilador no exige nada

Una excepción que hereda de `RuntimeException` se puede lanzar sin declarar nada:

```java
public Producto buscar(Long id) {     // sin throws
    return productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
}
```

Eso no significa que sea "menos seria": se lanza igual, corta el método igual y sube por las capas igual. Solo cambia que el compilador no te obliga a escribir `throws` ni `try/catch`.

#### 1.4.4 Por qué este proyecto usa no comprobadas

Si `RecursoNoEncontradoException` heredara de `Exception` (comprobada), habría que escribir `throws` en **cada** método por el que pase:

```java
// repository → service → controller: los tres necesitarían la declaración
public Producto buscar(Long id) throws RecursoNoEncontradoException { ... }
public ProductoResponse obtener(Long id) throws RecursoNoEncontradoException { ... }
public ResponseEntity<ProductoResponse> get(Long id) throws RecursoNoEncontradoException { ... }
```

Además, dentro de un `stream().map(...)` o un `forEach` una excepción comprobada **no se puede lanzar** sin envolverla en un `try/catch`, lo que ensucia el código.

Por eso Spring también usa no comprobadas. Cuando MySQL falla, el driver lanza `SQLException` (comprobada); Spring la **traduce** a `DataAccessException` y sus hijas, que son no comprobadas. Así `DataIntegrityViolationException` (la de tu handler) no obliga a declarar nada.

Resumen para tus dos clases:

| Clase | Hereda de | Tipo | Consecuencia |
| --- | --- | --- | --- |
| `RecursoNoEncontradoException` | `RuntimeException` | No comprobada | Sin `throws`; rollback automático |
| `ReglaNegocioException` | `RuntimeException` | No comprobada | Sin `throws`; rollback automático |

#### 1.4.5 La consecuencia más importante: el rollback

Cuando un método está anotado con `@Transactional`, Spring abre una transacción al entrar y, al salir, decide entre:

- **Commit:** confirma todos los cambios en la base.
- **Rollback:** deshace todos los cambios hechos dentro del método.

Esa decisión depende de **cómo termina** el método:

| Cómo termina el método | Resultado por defecto |
| --- | --- |
| Termina normalmente | Commit |
| Sale con una excepción **no comprobada** (`RuntimeException`) o un `Error` | **Rollback** |
| Sale con una excepción **comprobada** | **Commit** (¡aunque haya fallado!) |

Ejemplo con el pago de una boleta:

```java
@Transactional
public Boleta pagar(Long usuarioId) {
    Boleta boleta = boletaRepository.save(nuevaBoleta);          // 1. se inserta la boleta

    for (DetalleCarrito d : detallesDelCarrito) {
        Producto p = d.getProducto();
        if (p.getStock() < d.getCantidad()) {
            throw new ReglaNegocioException(                      // 3. falla el segundo producto
                    "Stock insuficiente para " + p.getNombre());
        }
        p.setStock(p.getStock() - d.getCantidad());               // 2. se descuenta el stock del primero
    }
    ...
}
```

| Paso | Qué pasó en la base mientras el método corría | Qué queda tras la excepción |
| --- | --- | --- |
| 1 | Se insertó la boleta | **Desaparece** |
| 2 | Se descontó el stock del primer producto | **Se restituye** |
| 3 | Falla el segundo producto | Nada queda a medias |

Como `ReglaNegocioException` es no comprobada, Spring hace **rollback** y la base queda exactamente como estaba. Si fuera comprobada, la boleta quedaría guardada con el stock descontado a medias, y el cliente vería un error.

El orden de los hechos es:

```
1. La excepción sale del método del service
2. Spring hace ROLLBACK de la transacción
3. La excepción llega al GlobalExceptionHandler
4. El cliente recibe el 409 con el mensaje
```

Es decir, **cuando el handler responde, la base ya está limpia**.

Si algún día necesitas que una comprobada también deshaga los cambios, se declara expresamente: `@Transactional(rollbackFor = Exception.class)`. En este proyecto no hace falta porque todas las excepciones propias son no comprobadas.

**Cuidado con atraparlas dentro del método.** El rollback ocurre solo si la excepción **sale** del método. Esto lo rompe:

```java
@Transactional
public Boleta pagar(Long usuarioId) {
    try {
        ...
        throw new ReglaNegocioException("Stock insuficiente");
    } catch (Exception e) {
        log.warn("falló el pago");     // la excepción se queda aquí: el método termina "normal" → COMMIT
    }
    ...
}
```

Regla práctica: **dentro de un service no atrapes excepciones para ignorarlas**; deja que suban hasta el handler.

#### 1.4.6 Qué pasa con `Error`

`OutOfMemoryError` y `StackOverflowError` heredan de `Error`, no de `Exception`. Por eso el handler `Exception.class` (línea 50) **no los atrapa**: suben hasta el servidor. Es lo correcto; son fallos de la JVM, no errores de la aplicación, y ningún código de la API puede recuperarse de ellos.

#### 1.4.7 Cómo decidir qué usar

| Situación | Qué hacer |
| --- | --- |
| Se busca algo que no existe | `RecursoNoEncontradoException` (404) |
| Una regla del negocio lo impide (stock, duplicados, producto inactivo) | `ReglaNegocioException` (409) |
| Un error de programación (`NullPointerException`, índice fuera de rango) | No lo atrapes: sale como 500 y el detalle queda en el log |
| Una librería lanza una comprobada (guardar un archivo, enviar un correo) | Atrápala en el service y conviértela en una no comprobada con un mensaje claro |

Ejemplo del último caso (relevante para la subida de imágenes y el correo):

```java
public String guardarImagen(MultipartFile archivo) {
    try {
        archivo.transferTo(destino);                       // lanza IOException (comprobada)
        return destino.getFileName().toString();
    } catch (IOException e) {
        log.error("No se pudo guardar la imagen", e);      // el detalle técnico va al log
        throw new ReglaNegocioException("No se pudo guardar la imagen");   // mensaje para el usuario
    }
}
```

Aquí sí se atrapa la excepción, pero solo para **convertirla** y relanzarla, no para ignorarla. `ReglaNegocioException` recibe solo un mensaje y por eso el `IOException` original se registra en el log en vez de adjuntarse; si algún día quieres conservarlo como "causa", se agrega un segundo constructor `(String mensaje, Throwable causa)`.

#### 1.4.8 Errores frecuentes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `Unhandled exception: java.io.IOException` (rojo en IntelliJ) | Se llama a algo que lanza una comprobada sin tratarla | `try/catch` o `throws`, según 1.4.2 |
| La boleta quedó guardada aunque el pago falló | La excepción era comprobada, o se atrapó dentro del método | Usar no comprobadas y dejar que salgan del service |
| `throws` repetido en todas las capas | Una excepción propia hereda de `Exception` | Cambiar a `extends RuntimeException` |
| El `GlobalExceptionHandler` no atrapa un `OutOfMemoryError` | Hereda de `Error`, no de `Exception` | Es lo esperado |



### 1.5 Códigos HTTP que usa el proyecto

| Código | Nombre | Significa |
| --- | --- | --- |
| 400 | Bad Request | La petición está mal hecha (datos inválidos, JSON roto) |
| 403 | Forbidden | Estás identificado pero no tienes permiso |
| 404 | Not Found | No existe lo que pides (un registro o una ruta) |
| 405 | Method Not Allowed | La ruta existe, pero no con ese método (`GET`, `POST`...) |
| 409 | Conflict | La petición es válida pero choca con el estado actual (duplicado, stock insuficiente) |
| 500 | Internal Server Error | Falló algo inesperado del lado del servidor |

Regla práctica: **4xx = el cliente puede corregirlo; 5xx = es un problema del servidor.**

---

## 2. Cómo viaja un error por la aplicación

```
Cliente (React)
   │  PUT /api/productos/999
   ▼
Controller ──► Service ──► Repository ──► MySQL
                  │
                  │  findById(999) → vacío
                  │  throw new RecursoNoEncontradoException("Producto", 999)
                  ▼
   (la excepción sube, saltándose el resto del código)
                  │
                  ▼
GlobalExceptionHandler.noEncontrado(...)   ← @ExceptionHandler la atrapa
                  │  build(404, "Producto no encontrado con id 999", ...)
                  ▼
ResponseEntity<ErrorResponse>  ──► Jackson lo convierte a JSON
                  │
                  ▼
Cliente recibe HTTP 404 + JSON con el formato uniforme
```

Quien arma el JSON no es el controller ni el service: es el handler.

---

## 3. Dependencias y librerías

### 3.1 Qué aporta cada starter del `pom.xml`

Un **starter** es un paquete que trae varias librerías ya compatibles entre sí. Los que usan estas clases:

| Starter / dependencia (en el `pom.xml`) | Qué aporta | Lo que usan las clases de `exception/` |
| --- | --- | --- |
| `spring-boot-starter-webmvc` | Spring MVC, Tomcat embebido, Jackson (JSON) y la API de servlets | `@RestControllerAdvice`, `@ExceptionHandler`, `ResponseEntity`, `HttpStatus`, `HttpServletRequest` (`jakarta.servlet`), `NoResourceFoundException`, `HttpRequestMethodNotSupportedException` |
| `spring-boot-starter-validation` | Hibernate Validator (Bean Validation) | No se importa, pero es lo que **provoca** `MethodArgumentNotValidException` y `HandlerMethodValidationException` al usar `@Valid`, `@NotBlank`, `@Min`... |
| `spring-boot-starter-data-jpa` | Spring Data JPA, Hibernate y el soporte de transacciones | `DataIntegrityViolationException` (paquete `org.springframework.dao`) |
| `spring-boot-starter-security` | Spring Security | `AccessDeniedException` (paquete `org.springframework.security.access`) |
| `lombok` | Genera código repetitivo al compilar | `@Slf4j` |
| (incluido en todos los starters) `spring-boot-starter-logging` | SLF4J + Logback | El `log` que crea `@Slf4j` |

> `lombok` necesita además estar en `annotationProcessorPaths` del plugin de compilación (ya está) y el procesamiento de anotaciones activado en IntelliJ; si no, `log` aparece "sin resolver".

### 3.2 Los imports de `GlobalExceptionHandler` uno por uno

| Línea | Import | Librería | Qué hace |
| --- | --- | --- | --- |
| 3 | `jakarta.servlet.http.HttpServletRequest` | API de servlets (Tomcat) | Representa la petición HTTP entrante. Se usa para leer la ruta (`getRequestURI()`) |
| 4 | `lombok.extern.slf4j.Slf4j` | Lombok | Anotación que genera el objeto `log` de la clase |
| 5 | `org.springframework.dao.DataIntegrityViolationException` | spring-tx (JPA) | Excepción de Spring para violaciones de integridad en la base: `UNIQUE`, FK, `NOT NULL`, `CHECK` |
| 6 | `org.springframework.http.HttpStatus` | spring-web | Enumeración de códigos HTTP (`NOT_FOUND` = 404...) |
| 7 | `org.springframework.http.ResponseEntity` | spring-web | Respuesta HTTP completa: código + cuerpo (+ cabeceras) |
| 8 | `org.springframework.web.bind.MethodArgumentNotValidException` | spring-web | Falla la validación de un `@Valid @RequestBody`. **No** la de `spring-messaging`: esa pertenece a mensajería |
| 9 | `org.springframework.web.bind.annotation.ExceptionHandler` | spring-web | Anotación que declara "este método maneja esta excepción" |
| 10 | `org.springframework.web.bind.annotation.RestControllerAdvice` | spring-web | Anotación que convierte la clase en un manejador global para todos los controllers |
| 12 | `org.springframework.http.converter.HttpMessageNotReadableException` | spring-web | El cuerpo de la petición no se pudo leer (JSON roto, tipo incorrecto, cuerpo ausente) |
| 13 | `org.springframework.security.access.AccessDeniedException` | spring-security-core | Usuario sin permiso. **Debe ser de `...security.access`**; IntelliJ ofrece otras con el mismo nombre |
| 14 | `org.springframework.web.HttpRequestMethodNotSupportedException` | spring-web | Método HTTP no permitido en esa ruta |
| 15 | `org.springframework.web.method.annotation.HandlerMethodValidationException` | spring-web (6.1+) | Falla la validación de parámetros sueltos (`@RequestParam`, `@PathVariable`) |
| 16 | `org.springframework.web.servlet.resource.NoResourceFoundException` | spring-webmvc (6.1+) | La ruta no corresponde a ningún controller ni recurso |
| 18 | `java.time.LocalDateTime` | JDK | Fecha y hora sin zona; para `timestamp` |
| 19 | `java.util.LinkedHashMap` | JDK | Mapa que **conserva el orden de inserción** (los campos salen en el mismo orden que fallaron) |
| 20 | `java.util.Map` | JDK | Interfaz de mapa clave → valor |

### 3.3 Los imports de `ErrorResponse`

| Línea | Import | Qué hace |
| --- | --- | --- |
| 3 | `java.time.LocalDateTime` | Tipo del campo `timestamp` |
| 4 | `java.util.Map` | Tipo del campo `detalles` |

Sin estos dos imports el compilador no sabe qué es `LocalDateTime` ni `Map`; ese fue exactamente el error `cannot be applied to (java.time.LocalDateTime, ...)` que apareció antes.

---

## 4. `RecursoNoEncontradoException.java`

```java
 1  package com.factory.tecnofactory.exception;
 2
 3
 4  public class RecursoNoEncontradoException extends RuntimeException {
 5      public RecursoNoEncontradoException(String recurso, Object id) {
 6          super(recurso + " no encontrado con id " + id);
 7      }
 8  }
```

| Línea | Qué hace |
| --- | --- |
| 1 | Declara el paquete. Debe coincidir con la carpeta `exception/`; así el handler (mismo paquete) la ve sin `import` |
| 2-3 | Líneas en blanco (una sobra, es solo estética) |
| 4 | Declara la clase `public`, por lo que **el archivo debe llamarse exactamente `RecursoNoEncontradoException.java`**. `extends RuntimeException` la convierte en excepción no comprobada (ver 1.4) |
| 5 | **Constructor**: se ejecuta con `new RecursoNoEncontradoException(...)`. Recibe qué se buscó (`recurso`, por ejemplo `"Producto"`) y el id que no se encontró. `Object id` acepta `Long`, `Integer` o `String` sin sobrecargar el constructor |
| 6 | `super(...)` llama al constructor de `RuntimeException` para fijar el **mensaje**. Con `("Producto", 5)` queda `Producto no encontrado con id 5`; luego se lee con `ex.getMessage()` |
| 7-8 | Cierran el constructor y la clase |

Uso típico:

```java
Producto p = productoRepository.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
```

`findById` devuelve un `Optional`; `orElseThrow` lanza la excepción solo cuando está vacío.

---

## 5. `ReglaNegocioException.java`

```java
 1  package com.factory.tecnofactory.exception;
 2
 3  public class ReglaNegocioException extends RuntimeException {
 4      public ReglaNegocioException(String mensaje) {
 5          super(mensaje);
 6      }
 7  }
```

| Línea | Qué hace |
| --- | --- |
| 1 | Paquete |
| 3 | Clase pública (archivo `ReglaNegocioException.java`), no comprobada |
| 4 | Constructor que recibe el mensaje **completo** (a diferencia de la anterior, que lo arma) |
| 5 | Pasa el mensaje a `RuntimeException` |

Es la excepción "comodín" para cualquier regla del negocio que impida la operación aunque la petición esté bien escrita:

```java
if (p.getStock() < cantidad) {
    throw new ReglaNegocioException("Stock insuficiente para " + p.getNombre());
}
if (usuarioRepository.existsByCorreo(correo)) {
    throw new ReglaNegocioException("El correo ya está registrado");
}
if (!p.getActivo()) {
    throw new ReglaNegocioException("El producto no está disponible");
}
```

> El mensaje **se envía al cliente tal cual**, incluso en producción. Escríbelo para una persona y sin datos internos (nada de nombres de tablas, rutas ni SQL).

---

## 6. `ErrorResponse.java`

```java
 1  package com.factory.tecnofactory.exception;
 2
 3  import java.time.LocalDateTime;
 4  import java.util.Map;
 5
 6  public record ErrorResponse(
 7          LocalDateTime timestamp,
 8          int status,
 9          String error,
10          String mensaje,
11          String ruta,
12          Map<String, String> detalles) {
13  }
```

### 6.1 Línea por línea

| Línea | Qué hace |
| --- | --- |
| 1 | Paquete |
| 3-4 | Imports de los tipos de dos campos (ver 3.3) |
| 6 | `public record ErrorResponse(` declara un **record**: una clase de datos inmutable cuyos campos se declaran entre paréntesis |
| 7 | `timestamp`: cuándo ocurrió el error |
| 8 | `status`: el número del código HTTP (`404`). `int` y no `Integer`: nunca es nulo |
| 9 | `error`: el nombre del código en inglés (`Not Found`), que Spring entrega con `getReasonPhrase()` |
| 10 | `mensaje`: explicación en español para el usuario |
| 11 | `ruta`: la URL que se pidió (`/api/productos/5`), útil para depurar |
| 12 | `detalles`: mapa **campo → mensaje**. Solo se llena en errores de validación; en el resto vale `null` |
| 13 | Cierra el record (no necesita cuerpo) |

### 6.2 Qué es un `record`

Con esas pocas líneas Java genera automáticamente:

- el **constructor** con los seis parámetros, en ese orden (por eso el handler escribe `new ErrorResponse(a, b, c, d, e, f)` en ese orden exacto);
- los métodos de lectura `timestamp()`, `status()`, `error()`, `mensaje()`, `ruta()` y `detalles()` (sin `get`);
- `equals`, `hashCode` y `toString`.

Es **inmutable**: una vez creado no se puede cambiar. No hace falta Lombok para esto.

### 6.3 Cómo se convierte en JSON

Spring pasa el objeto por Jackson, que usa los nombres de los componentes como claves: `timestamp`, `status`, `error`, `mensaje`, `ruta` y `detalles`. Spring Boot escribe `LocalDateTime` como texto ISO-8601 (`2026-10-17T10:15:30.123`).

Con un campo nulo, Jackson lo imprime como `"detalles": null`. Es válido y el frontend puede ignorarlo; si prefieres que no aparezca, existe la anotación `@JsonInclude(NON_NULL)` de Jackson, pero **no es necesaria**.

El `timestamp` usa `LocalDateTime.now()`, es decir, la zona horaria del servidor Java (no la de MySQL).

---

## 7. `GlobalExceptionHandler.java`

### 7.1 Estructura general

| Líneas | Contenido |
| --- | --- |
| 1 | Paquete |
| 3-20 | Imports (explicados en 3.2); están en tres bloques porque se agregaron en momentos distintos |
| 22 | Comentario con el nombre del archivo (puedes borrarlo; no hace nada) |
| 23-25 | Anotaciones y declaración de la clase |
| 27-54 | Handlers de los errores propios, validación, base de datos y el genérico |
| 56-59 | Método `build`, que arma la respuesta |
| 62-85 | Handlers de los errores de la petición HTTP y de seguridad |

### 7.2 Anotaciones de la clase (líneas 23-25)

```java
23  @Slf4j
24  @RestControllerAdvice
25  public class GlobalExceptionHandler {
```

| Línea | Qué hace |
| --- | --- |
| 23 | `@Slf4j` (Lombok) genera al compilar `private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);`. Así existe la variable `log` sin escribirla |
| 24 | `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. Spring detecta la clase al arrancar (está bajo `com.factory.tecnofactory`), la usa para **todos** los controllers y escribe lo que devuelven sus métodos como JSON |
| 25 | La clase pública (archivo `GlobalExceptionHandler.java`). No se instancia a mano: la crea Spring |

### 7.3 Anatomía de un handler

Todos siguen el mismo patrón. Ejemplo (líneas 27-30):

```java
@ExceptionHandler(RecursoNoEncontradoException.class)
public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
}
```

| Parte | Qué significa |
| --- | --- |
| `@ExceptionHandler(X.class)` | "Cuando en un controller se lance `X` (o una subclase), ejecuta este método" |
| `public ResponseEntity<ErrorResponse>` | Devuelve una respuesta HTTP cuyo cuerpo es un `ErrorResponse` |
| `noEncontrado` | Nombre del método: libre, solo descriptivo |
| `RecursoNoEncontradoException ex` | Spring le entrega la excepción lanzada |
| `HttpServletRequest req` | Spring le entrega la petición actual (para leer la ruta) |
| `return build(...)` | Delega en `build` el armado de la respuesta |

Spring resuelve los parámetros solo: no los pasas tú. Si un handler no necesita la petición, puede omitirla.

### 7.4 El método `build` (líneas 56-59)

```java
56  private ResponseEntity<ErrorResponse> build(HttpStatus s, String msg, HttpServletRequest req, Map<String,String> det) {
57      return ResponseEntity.status(s).body(new ErrorResponse(
58              LocalDateTime.now(), s.value(), s.getReasonPhrase(), msg, req.getRequestURI(), det));
59  }
```

| Línea | Qué hace |
| --- | --- |
| 56 | Método **privado**: solo lo usa esta clase. Recibe el código (`s`), el mensaje (`msg`), la petición (`req`) y los detalles (`det`, normalmente `null`). Existe para no repetir las mismas líneas diez veces |
| 57 | `ResponseEntity.status(s)` fija el código HTTP de la respuesta; `.body(...)` le pone el cuerpo |
| 58 | Crea el `ErrorResponse` en el orden de sus campos: `LocalDateTime.now()` → `timestamp`; `s.value()` → `status` (404); `s.getReasonPhrase()` → `error` ("Not Found"); `msg` → `mensaje`; `req.getRequestURI()` → `ruta` (solo la ruta, sin `?parametros`); `det` → `detalles` |
| 59 | Cierra el método |

---

## 8. Catálogo de errores, uno por uno

Resumen (las líneas son las de tu archivo):

| Subsección | Excepción | HTTP | Líneas |
| --- | --- | --- | --- |
| [8.1](#81-recursonoencontradoexception--404) | `RecursoNoEncontradoException` | 404 | 27-30 |
| [8.2](#82-reglanegocioexception--409) | `ReglaNegocioException` | 409 | 32-35 |
| [8.3](#83-methodargumentnotvalidexception--400) | `MethodArgumentNotValidException` | 400 | 37-43 |
| [8.4](#84-dataintegrityviolationexception--409) | `DataIntegrityViolationException` | 409 | 45-48 |
| [8.5](#85-exception-genérica--500) | `Exception` | 500 | 50-54 |
| [8.6](#86-httpmessagenotreadableexception--400) | `HttpMessageNotReadableException` | 400 | 62-65 |
| [8.7](#87-httprequestmethodnotsupportedexception--405) | `HttpRequestMethodNotSupportedException` | 405 | 67-70 |
| [8.8](#88-noresourcefoundexception--404) | `NoResourceFoundException` | 404 | 72-75 |
| [8.9](#89-handlermethodvalidationexception--400) | `HandlerMethodValidationException` | 400 | 77-80 |
| [8.10](#810-accessdeniedexception--403) | `AccessDeniedException` | 403 | 82-85 |

### 8.1 `RecursoNoEncontradoException` → 404

```java
27  @ExceptionHandler(RecursoNoEncontradoException.class)
28  public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
29      return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
30  }
```

| Línea | Qué hace |
| --- | --- |
| 27 | Atrapa la excepción propia de "no existe" |
| 28 | Firma del handler (ver 7.3) |
| 29 | Código **404**; el mensaje es el de la excepción (`ex.getMessage()` → `Producto no encontrado con id 5`); sin detalles |

- **Quién la lanza:** tus services, cuando `findById` no encuentra el registro.
- **Por qué 404:** el recurso pedido no existe.
- **Respuesta:**

```json
{ "timestamp": "2026-10-17T10:15:30.123", "status": 404, "error": "Not Found",
  "mensaje": "Producto no encontrado con id 999", "ruta": "/api/productos/999", "detalles": null }
```

- **Ojo:** el mensaje incluye el id que pidió el cliente, que ya conoce; no revela nada interno.

### 8.2 `ReglaNegocioException` → 409

```java
32  @ExceptionHandler(ReglaNegocioException.class)
33  public ResponseEntity<ErrorResponse> negocio(ReglaNegocioException ex, HttpServletRequest req) {
34      return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
35  }
```

| Línea | Qué hace |
| --- | --- |
| 32 | Atrapa la excepción propia de reglas del negocio |
| 34 | Código **409**, con el mensaje escrito por quien la lanzó |

- **Quién la lanza:** tus services (stock insuficiente, correo ya registrado, producto desactivado...).
- **Por qué 409:** la petición está bien formada, pero choca con el estado actual de los datos o del negocio.
- **Respuesta:**

```json
{ "timestamp": "...", "status": 409, "error": "Conflict",
  "mensaje": "Stock insuficiente para Teclado Mecánico", "ruta": "/api/boletas", "detalles": null }
```

### 8.3 `MethodArgumentNotValidException` → 400

```java
37  @ExceptionHandler(MethodArgumentNotValidException.class)
38  public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
39      Map<String, String> campos = new LinkedHashMap<>();
40      ex.getBindingResult().getFieldErrors()
41              .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
42      return build(HttpStatus.BAD_REQUEST, "Datos inválidos", req, campos);
43  }
```

| Línea | Qué hace |
| --- | --- |
| 37-38 | Atrapa el fallo de validación de un `@Valid @RequestBody` |
| 39 | Crea un mapa vacío `campo → mensaje`. `LinkedHashMap` mantiene el orden |
| 40 | `getBindingResult()` es el informe de la validación; `getFieldErrors()` es la lista de campos que fallaron (un `FieldError` por cada falla) |
| 41 | Por cada error `e`: guarda `e.getField()` (el nombre del campo, p. ej. `precio`) y `e.getDefaultMessage()` (el mensaje de la regla) |
| 42 | Responde **400** con el mensaje fijo `Datos inválidos` y el mapa en `detalles` |

- **Quién la lanza:** Spring, automáticamente, cuando un DTO anotado con `@NotBlank`, `@Min`, `@Size`, etc. no se cumple y el parámetro del controller lleva `@Valid`.
- **Por qué 400:** el cliente envió datos inválidos y puede corregirlos.
- **Respuesta:**

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request",
  "mensaje": "Datos inválidos", "ruta": "/api/productos",
  "detalles": { "nombre": "<mensaje de la regla>", "precio": "<mensaje de la regla>" } }
```

- **Matices:** si un mismo campo falla dos reglas, `put` guarda solo el último mensaje. Los errores de nivel de clase (`getGlobalErrors()`) no se incluyen. El texto de los mensajes depende del idioma del servidor, salvo que escribas `message = "..."` en la anotación del DTO (recomendado: `@NotBlank(message = "El nombre es obligatorio")`).

### 8.4 `DataIntegrityViolationException` → 409

```java
45  @ExceptionHandler(DataIntegrityViolationException.class)
46  public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
47      return build(HttpStatus.CONFLICT, "La operación viola una restricción de datos", req, null);
48  }
```

| Línea | Qué hace |
| --- | --- |
| 45-46 | Atrapa las violaciones de integridad de la base |
| 47 | Código **409** con un mensaje **genérico fijo** (no usa `ex.getMessage()` a propósito) |

- **Quién la lanza:** Spring, al traducir un error de MySQL: valor duplicado en una columna `UNIQUE` (`correo`, `run`, `nombre_categoria`), FK que no existe o que impide borrar, `NOT NULL` o `CHECK` incumplido (`descuento` fuera de 0 a 100).
- **Por qué no se muestra `ex.getMessage()`:** ese mensaje trae el SQL, nombres de tablas y de restricciones; mostrarlo es dar información al atacante.
- **Por qué 409:** los datos chocan con las reglas de la base.
- **Consejo:** es la **red de seguridad**. Para dar un mensaje útil, valida antes en el service (`existsByCorreo`) y lanza `ReglaNegocioException`; este handler queda como respaldo para condiciones de carrera (dos peticiones simultáneas).

### 8.5 `Exception` genérica → 500

```java
50  @ExceptionHandler(Exception.class)
51  public ResponseEntity<ErrorResponse> generico(Exception ex, HttpServletRequest req) {
52      log.error("Error no controlado en {}", req.getRequestURI(), ex);
53      return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
54  }
```

| Línea | Qué hace |
| --- | --- |
| 50-51 | `Exception` es la madre de casi todas las excepciones, así que atrapa **todo lo que ningún otro handler cubra** |
| 52 | Escribe en el log del servidor un error con la ruta (`{}` se reemplaza por `req.getRequestURI()`). Al pasar `ex` como **último argumento**, SLF4J imprime además el **stack trace** completo |
| 53 | Responde **500** con un texto genérico: el cliente no ve nada interno |

- **Quién la lanza:** cualquier fallo inesperado (`NullPointerException`, error de lógica, base caída...).
- **Por qué 500:** es culpa del servidor, no del cliente.
- **Dónde ver el detalle:** en el log (`docker compose logs`, consola de IntelliJ). **Si ves un 500, la causa está en el log, no en la respuesta.**
- **Por qué existen los otros handlers:** como este atrapa *todo*, sin handlers específicos un JSON roto o una ruta inexistente saldrían como 500.

### 8.6 `HttpMessageNotReadableException` → 400

```java
62  @ExceptionHandler(HttpMessageNotReadableException.class)
63  public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
64      return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", req, null);
65  }
```

- **Quién la lanza:** Spring, antes de llegar al controller, cuando no puede convertir el cuerpo en el DTO: JSON mal formado (falta una coma o llave), un texto donde se espera un número (`"precio": "abc"`) o cuerpo ausente en un `@RequestBody` obligatorio.
- **Por qué 400:** el cliente envió algo ilegible.
- **Diferencia con 8.3:** aquí el JSON ni siquiera se pudo leer; en 8.3 se leyó bien pero sus valores no cumplen las reglas.
- **Cómo provocarla:** enviar `{"nombre": "Teclado",` (sin cerrar).

### 8.7 `HttpRequestMethodNotSupportedException` → 405

```java
67  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
68  public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
69      return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta", req, null);
70  }
```

- **Quién la lanza:** Spring, cuando la ruta existe pero ningún controller la atiende con ese método. Ejemplo: solo hay `GET /api/productos` y se envía `DELETE /api/productos`.
- **Por qué 405:** distingue "no existe la ruta" (404) de "existe, pero no así".
- **Nota:** la respuesta por defecto de Spring incluye una cabecera `Allow` con los métodos válidos; la de este handler no. No es necesaria para el frontend.

### 8.8 `NoResourceFoundException` → 404

```java
72  @ExceptionHandler(NoResourceFoundException.class)
73  public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
74      return build(HttpStatus.NOT_FOUND, "Ruta no encontrada", req, null);
75  }
```

- **Quién la lanza:** Spring MVC cuando la ruta no corresponde a ningún controller ni archivo estático (por ejemplo `/api/productoss`, un error de tipeo). También la provocan los navegadores al pedir `/favicon.ico`.
- **Por qué 404:** no hay nada en esa URL.
- **Diferencia con 8.1:** 8.1 es "la ruta existe, pero el registro no"; aquí es "la ruta misma no existe". Ambos son 404, pero con mensaje distinto.

### 8.9 `HandlerMethodValidationException` → 400

```java
77  @ExceptionHandler(HandlerMethodValidationException.class)
78  public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
79      return build(HttpStatus.BAD_REQUEST, "Parámetros inválidos", req, null);
80  }
```

- **Quién la lanza:** Spring (6.1 en adelante), cuando el método del controller lleva reglas directamente en sus parámetros: por ejemplo `@RequestParam @Min(1) int pagina` o `@PathVariable @Positive Long id`.
- **Por qué 400:** un parámetro de la URL no cumple su regla.
- **Limitación:** este handler no rellena `detalles`, por lo que el cliente solo ve "Parámetros inválidos". Es suficiente para empezar; si más adelante necesitas decir cuál parámetro falló, se amplía leyendo los resultados de validación de la excepción.

### 8.10 `AccessDeniedException` → 403

```java
82  @ExceptionHandler(AccessDeniedException.class)
83  public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
84      return build(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", req, null);
85  }
```

- **Quién la lanza:** Spring Security cuando un método protegido con seguridad por métodos (por ejemplo `@PreAuthorize("hasRole('ADMINISTRADOR')")`) lo invoca alguien sin el rol.
- **Por qué 403:** sabemos quién eres, pero no puedes hacer esto.
- **Hoy no se activa:** mientras `SecurityConfig` deje todo abierto (`permitAll`) y no existan `@PreAuthorize`, esta excepción no se lanza. Queda lista para la fase 4.
- **Matiz importante para la fase 4:** normalmente, Spring Security distingue entre un visitante **sin token** (401, "identifícate") y un usuario **identificado sin permiso** (403). Como este handler atrapa la excepción *antes* de que Spring Security decida, **ambos recibirían 403**. Cuando se escriba la seguridad habrá que elegir: aceptar esa simplificación, o dejar que esta excepción siga de largo (relanzarla) para que Spring Security la resuelva.
- **No cubre** las reglas por ruta de `SecurityConfig` (`.requestMatchers(...).hasRole(...)`): esas se resuelven en los filtros, antes de llegar a los controllers.

### 8.11 `MethodArgumentTypeMismatchException` → 400

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ErrorResponse> tipoDeParametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un formato inválido", req, null);
}
```

- **Quién la lanza:** Spring cuando no puede convertir un valor de la URL al tipo del parámetro: por ejemplo `GET /api/usuarios/abc` con `@PathVariable Long id`.
- **Por qué 400:** el cliente envió algo que no es un número. Sin este handler caería en el genérico `Exception` y saldría **500**, que sugiere un fallo del servidor cuando es un error del cliente.
- **`ex.getName()`:** el nombre del parámetro (`id`), un texto del código sin datos sensibles. **No** se muestra el valor enviado.
- **Cómo se descubrió:** al diseñar el `UsuarioController` (19 oct). Lo cubre el test `buscarConIdNoNumericoDevuelve400`.
- **Casos parecidos pendientes** (hoy caen en el 500 genérico): `HttpMediaTypeNotSupportedException` (415: `POST` sin `Content-Type: application/json`) y `MissingServletRequestParameterException` (400: falta un `?parametro=`). Ver [Cambios recientes](cambios-recientes.md#configuración-base-antes-de-la-segunda-entidad).

---

## 9. Cómo elige Spring el handler

Spring busca, entre todos los `@ExceptionHandler`, el que declare el tipo de excepción **más cercano** al que se lanzó. Dos consecuencias:

1. **El orden de los métodos en el archivo no importa.** `Exception.class` (línea 50) está antes que otros y aun así solo atrapa lo que nadie más cubre.
2. Un handler de una clase padre atrapa también a sus hijas. Por eso `Exception.class` es la red de seguridad final.

Ejemplo: si se lanza `RecursoNoEncontradoException`, Spring tiene dos candidatos (`RecursoNoEncontradoException` y `Exception`) y elige el primero por ser más específico.

---

## 10. Errores que NO pasan por este handler

`@RestControllerAdvice` solo actúa sobre excepciones lanzadas **dentro** de un controller (o de lo que este llama). No ve:

| Situación | Quién responde | Cuándo se resuelve |
| --- | --- | --- |
| Petición sin token o con token inválido (401) | Spring Security (filtros) | Fase 4, en `SecurityConfig` |
| Ruta bloqueada por rol (`requestMatchers(...)`) | Spring Security (filtros) | Fase 4 |
| Excepción dentro de un filtro propio (por ejemplo `JwtAuthFilter`) | Spring Security o el contenedor | Fase 4: el filtro debe responder el error por sí mismo |
| Errores anteriores a llegar a Spring MVC | La respuesta de error por defecto de Spring (`/error`) | Se controla con `server.error.*` (ver [04](04-perfiles-spring.md)) |

**Antes del 18 oct**, con `SecurityConfig` vacío, Spring Security bloqueaba todas las rutas con 401 antes de que el handler intervenga. **Hoy** el `SecurityConfig` mínimo ya existe y deja las rutas abiertas, así que los errores de tus controllers sí saldrán formateados por el handler (ver [15](15-seguridad-codigo-explicado.md)).

---

## 11. Cómo crear una excepción nueva

Ejemplo: un error 401 para credenciales incorrectas en el login.

1. **Crear el archivo**, un archivo por clase:

```java
package com.factory.tecnofactory.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
```

2. **Agregar un handler** en `GlobalExceptionHandler`:

```java
@ExceptionHandler(CredencialesInvalidasException.class)
public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
}
```

3. **Lanzarla** donde corresponda: `throw new CredencialesInvalidasException();`.

El mensaje es deliberadamente el mismo si falla el correo o la contraseña: separarlos permitiría descubrir qué correos existen.

**¿Una excepción nueva o `ReglaNegocioException`?** Si el código HTTP o el tratamiento del frontend es distinto (401, 403, 423...), crea una nueva. Si solo cambia el mensaje y sigue siendo un 409, usa `ReglaNegocioException`.

---

## 12. Cómo probar cada error

Requiere un controller y un `SecurityConfig` mínimo (ver [Cambios recientes](cambios-recientes.md#pendiente)). Los ejemplos suponen un `POST /api/categorias` y un `GET /api/categorias/{id}`; ajusta las rutas a las tuyas.

| Error | Petición de prueba | Resultado esperado |
| --- | --- | --- |
| 404 registro | `curl -i localhost:8080/api/categorias/999999` | 404 y `Categoria no encontrado con id 999999` |
| 404 ruta | `curl -i localhost:8080/api/categoriass` | 404 y `Ruta no encontrada` |
| 405 | `curl -i -X DELETE localhost:8080/api/categorias` (si no existe ese `DELETE`) | 405 |
| 400 id mal escrito | `curl -i localhost:8080/api/usuarios/abc` | 400 y `El parámetro 'id' tiene un formato inválido` |
| 400 JSON roto | `curl -i -X POST localhost:8080/api/categorias -H "Content-Type: application/json" -d '{"nombre":'` | 400 y `El cuerpo de la petición no es un JSON válido` |
| 400 validación | `curl -i -X POST localhost:8080/api/categorias -H "Content-Type: application/json" -d '{"nombre":""}'` | 400 con `detalles` |
| 409 duplicado | Crear dos categorías con el mismo nombre | 409 (de tu service o de la base) |
| 500 | Provocar un fallo a propósito en un método de prueba | 500 genérico y el detalle en el log |

`-i` muestra las cabeceras y el código. Con Swagger (`/swagger-ui/index.html`, solo en `local`) puedes hacer lo mismo desde el navegador.

---

## 13. Observaciones sobre tus archivos

| Observación | Gravedad | Detalle |
| --- | --- | --- |
| Los cuatro archivos coinciden en nombres, paquete e imports | — | Cada clase pública está en su propio archivo |
| Línea 22 de `GlobalExceptionHandler`: `// exception/GlobalExceptionHandler.java` | Estética | Es el comentario con el nombre del archivo que venía en el ejemplo; puedes borrarlo |
| Dos líneas en blanco seguidas (líneas 60-61 y 2-3 de `RecursoNoEncontradoException`) | Estética | No afecta |
| Los handlers de los errores de Spring quedan después de `build` | Estética | Funciona igual (ver 9). Para legibilidad puedes agruparlos antes de `Exception.class` |
| Handlers de 8.7 a 8.10 con `ex` sin usar | Ninguna | Spring lo exige en la firma si se quiere recibir la excepción; no es un error |
| `detalles` es `null` salvo en 8.3 | Diseño | Intencional |
| 8.10 devolvería 403 también a visitantes sin token | Diseño | A decidir en la fase 4 (ver 8.10) |
| No se compiló ni probó | **Pendiente** | La revisión fue por lectura |

---

## 14. Glosario

| Término | Significado |
| --- | --- |
| **Excepción** | Objeto que representa un error y corta la ejecución normal |
| **`throw`** | Palabra que lanza una excepción |
| **`RuntimeException`** | Excepción no comprobada: no exige `throws` y provoca *rollback* en `@Transactional` |
| **Handler** | Método que atrapa un tipo de excepción y decide la respuesta |
| **`@RestControllerAdvice`** | Clase que aplica handlers (y respuesta JSON) a todos los controllers |
| **`@ExceptionHandler`** | Anotación que indica qué excepción atiende un método |
| **`ResponseEntity`** | Respuesta HTTP completa: código, cabeceras y cuerpo |
| **`record`** | Clase de datos inmutable con constructor y lectores generados |
| **DTO** | Objeto que solo transporta datos entre el cliente y la API |
| **Bean Validation** | Estándar de reglas con anotaciones (`@NotBlank`, `@Min`...), implementado por Hibernate Validator |
| **Stack trace** | Lista de llamadas que llevó hasta el error; solo debe verse en el log, nunca en la respuesta |
| **Starter** | Dependencia de Spring Boot que agrupa varias librerías compatibles |
| **SLF4J / Logback** | Interfaz y motor de registro de logs de Spring Boot |
| **Jackson** | Librería que convierte objetos Java en JSON y viceversa |
| **409 Conflict** | La petición es válida pero choca con el estado actual |
