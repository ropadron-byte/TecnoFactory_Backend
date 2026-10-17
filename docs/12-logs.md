# 12. Logs

Guía para entender qué es el log de la aplicación, qué hace Lombok en todo esto, dónde encontrar los mensajes y cómo escribir los tuyos. Complementa a [11](11-excepciones-explicadas.md), donde `GlobalExceptionHandler` escribe un log cuando ocurre un error 500.

---

## 1. Qué es un log y para qué sirve

Un **log** es el registro de lo que va haciendo la aplicación mientras corre: arranques, avisos, errores. Es la **única forma de saber qué pasó** cuando algo falla, porque el cliente nunca ve los detalles internos.

Ejemplo del proyecto: ante un error inesperado, la API responde al cliente solo `"Error interno del servidor"` (500). La causa real (el archivo y la línea donde falló el código) queda en el log.

| Quién lo ve | Qué ve |
| --- | --- |
| El cliente (React) | La respuesta JSON del `ErrorResponse`, sin detalles internos |
| Tú (el desarrollador) | El log, con el detalle completo y el *stack trace* |

---

## 2. Las piezas: Lombok, SLF4J y Logback

Intervienen tres piezas, cada una con un trabajo distinto:

| Pieza | Qué es | Qué hace |
| --- | --- | --- |
| **Lombok** (`@Slf4j`) | Librería que genera código al compilar | Agrega a la clase la variable `log`, para que no la escribas tú |
| **SLF4J** | Una interfaz (un contrato) | Define los métodos disponibles: `log.error(...)`, `log.warn(...)`, `log.info(...)`, `log.debug(...)` |
| **Logback** | El motor que realmente escribe | Decide **dónde** (consola, archivo) y **cómo** (formato, niveles) se imprime |

Spring Boot trae SLF4J y Logback incluidos en cualquier starter (`spring-boot-starter-logging`), así que no hay que agregar nada al `pom.xml` para tener logs. Lombok solo se encarga del paso cómodo.

### 2.1 Qué genera `@Slf4j`

Al escribir esto:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    ...
    log.error("Error no controlado en {}", req.getRequestURI(), ex);
}
```

Lombok agrega, al compilar y sin que lo veas en el archivo, esta línea:

```java
private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
```

| Parte | Significado |
| --- | --- |
| `private static final` | Una sola variable por clase, que no se reasigna |
| `Logger log` | El objeto con el que escribes mensajes |
| `getLogger(GlobalExceptionHandler.class)` | Le pone al logger **el nombre de la clase**; ese nombre aparece en cada línea del log y permite configurar niveles por paquete |

**Lombok no crea ningún archivo ni guarda nada.** Solo ahorra escribir esa línea en cada clase. Si IntelliJ marca `log` en rojo, falta activar el procesamiento de anotaciones (ver [09](09-errores-comunes.md#java-e-intellij)).

---

## 3. Dónde encontrar el log

Por defecto Spring Boot escribe en la **consola** (la salida estándar del proceso). No crea ningún archivo.

| Cómo ejecutas la app | Dónde verlo |
| --- | --- |
| IntelliJ (botón Run) | Pestaña **Run**, abajo en el IDE |
| Terminal (`./mvnw spring-boot:run` o `java -jar`) | La misma terminal |
| Contenedor `backend` (cuando exista) | `docker compose logs backend` |
| Contenedor de MySQL | `docker compose logs db` |

> Hoy el servicio `backend` del compose está comentado. El log de tu API sale de IntelliJ o de la terminal desde donde la arrancaste; `docker compose logs` solo mostrará el de MySQL.

### 3.1 Comandos útiles con Docker

```bash
docker compose logs backend              # todo lo guardado
docker compose logs -f backend           # seguirlo en vivo (Ctrl+C para salir)
docker compose logs --tail 100 backend   # solo las últimas 100 líneas
docker compose logs --since 10m backend  # los últimos 10 minutos
```

### 3.2 Buscar un error concreto

```bash
docker compose logs backend | grep ERROR
```

---

## 4. Cómo leer una línea del log

Cuando ocurre un error 500 aparece algo así (el formato puede variar levemente según la versión):

```
2026-10-17T10:15:30.123-03:00 ERROR 12345 --- [nio-8080-exec-1] c.f.t.exception.GlobalExceptionHandler : Error no controlado en /api/productos
java.lang.NullPointerException: Cannot invoke "String.length()" because "nombre" is null
    at com.factory.tecnofactory.service.ProductoService.crear(ProductoService.java:42)
    at com.factory.tecnofactory.controller.ProductoController.crear(ProductoController.java:28)
    ...
```

| Parte | Qué es |
| --- | --- |
| `2026-10-17T10:15:30.123-03:00` | Fecha y hora |
| `ERROR` | **Nivel** (ver sección 5) |
| `12345` | Número de proceso (PID) |
| `---` | Separador fijo |
| `[nio-8080-exec-1]` | Hilo que atendía la petición |
| `c.f.t.exception.GlobalExceptionHandler` | **Logger**: la clase que escribió el mensaje, con los paquetes abreviados |
| `Error no controlado en /api/productos` | El mensaje (el `{}` se reemplazó por la ruta) |
| `java.lang.NullPointerException: ...` | El tipo de error y su causa |
| `at ...ProductoService.crear(ProductoService.java:42)` | El *stack trace*: la lista de llamadas hasta el fallo |

**Cómo se lee un stack trace:** busca la **primera línea que empiece por `at com.factory.tecnofactory`**. Esa es la parte que es tuya, con el archivo y la línea exactos (`ProductoService.java:42`). Las líneas de `org.springframework...` o `jdk...` son el camino interno que recorrió Spring.

---

## 5. Niveles

Cada mensaje tiene un nivel que indica su importancia. De menor a mayor:

| Nivel | Cuándo se usa | Ejemplo |
| --- | --- | --- |
| `TRACE` | Detalle extremo, casi nunca | Cada paso interno de un algoritmo |
| `DEBUG` | Información para el desarrollador | `Buscando producto 5` |
| `INFO` | Hechos normales importantes | `Boleta 120 creada` |
| `WARN` | Algo raro que no es un fallo, pero conviene vigilar | `Stock crítico en Teclado Mecánico` |
| `ERROR` | Algo falló | `Error no controlado en /api/productos` |

**Al fijar un nivel se muestra ese nivel y todos los mayores.** Con `INFO` se ven `INFO`, `WARN` y `ERROR`; los `DEBUG` quedan ocultos. Por defecto Spring Boot muestra desde `INFO`.

### 5.1 Lo que tiene configurado el proyecto

| Perfil | `logging.level.root` (librerías) | `logging.level.com.factory` (tu código) |
| --- | --- | --- |
| `local` | `INFO` | `DEBUG` |
| `prod` | `WARN` | `INFO` |

En `local` ves todos tus `log.debug`. En `prod` solo `INFO` hacia arriba, y las librerías solo avisos y errores (por eso el log de producción es corto). `log.error` se muestra siempre.

Para subir el detalle de una sola parte sin tocar el resto:

```yaml
logging:
  level:
    root: INFO
    com.factory: DEBUG
    com.factory.tecnofactory.service: TRACE   # solo los services, más detalle
```

---

## 6. Cómo escribir logs en tu código

### 6.1 Los pasos

1. Agrega `@Slf4j` (import `lombok.extern.slf4j.Slf4j`) sobre la clase.
2. Escribe `log.nivel("mensaje", valores...)`.

```java
@Slf4j
@Service
public class BoletaService {

    public Boleta pagar(Long usuarioId) {
        log.debug("Iniciando pago del usuario {}", usuarioId);
        ...
        log.info("Boleta {} creada para el usuario {}", boleta.getId(), usuarioId);
        ...
        if (producto.getStock() <= producto.getStockCritico()) {
            log.warn("Stock crítico en '{}': quedan {}", producto.getNombre(), producto.getStock());
        }
    }
}
```

### 6.2 Usa `{}` y no concatenes

| Forma | Evaluación |
| --- | --- |
| `log.debug("Usuario {}", id)` | **Correcta.** El mensaje solo se arma si el nivel está activo |
| `log.debug("Usuario " + id)` | Evitar: el texto se arma siempre, aunque `DEBUG` esté apagado |

### 6.3 Registrar una excepción

Si el **último argumento** es una excepción, SLF4J imprime además su *stack trace*. Es lo que hace tu línea del handler:

```java
log.error("Error no controlado en {}", req.getRequestURI(), ex);
//                                      ↑ llena el {}         ↑ se imprime con su stack trace
```

Si solo concatenas `ex.getMessage()`, pierdes el stack trace y con él el archivo y la línea del fallo.

### 6.4 Qué nivel usar

| Situación | Nivel |
| --- | --- |
| Una excepción inesperada | `error` (con la excepción como último argumento) |
| Un error esperado que ya se informa al cliente (404, 409) | **No hace falta** registrarlo, o a lo sumo `debug` |
| Stock crítico, intento de login fallido repetido | `warn` |
| Un evento importante (boleta creada, usuario registrado) | `info` |
| Valores internos para depurar | `debug` |

### 6.5 Qué NO escribir nunca en el log

| No registrar | Motivo |
| --- | --- |
| Contraseñas (ni siquiera hasheadas) | Quien lea el log las obtiene |
| Tokens JWT completos | Permiten suplantar al usuario |
| El valor de `JWT_SECRET` o de cualquier clave del `.env` | Es un secreto |
| Números de tarjeta (el pago es simulado, pero no los guardes ni los muestres) | Datos sensibles |
| RUN, teléfono o dirección completos | Datos personales; el log no se protege como la base de datos |

Un log puede copiarse a otro servidor, pegarse en un chat o subirse a Git por error: **escribe pensando en que cualquiera podría leerlo.**

---

## 7. El log del `GlobalExceptionHandler`

Solo un handler escribe en el log: el genérico (línea 52 de tu archivo).

```java
50  @ExceptionHandler(Exception.class)
51  public ResponseEntity<ErrorResponse> generico(Exception ex, HttpServletRequest req) {
52      log.error("Error no controlado en {}", req.getRequestURI(), ex);
53      return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
54  }
```

| Error | ¿Se escribe en el log? | Por qué |
| --- | --- | --- |
| 500 (cualquier excepción no cubierta) | **Sí**, nivel `ERROR` con el stack trace | Es un fallo del servidor y hay que investigarlo |
| 400, 404, 405, 409, 403 | No | Son errores del cliente o reglas del negocio, y el cliente ya recibe el motivo |

**Si ves un 500 en el navegador o en Swagger, la causa está en el log, no en la respuesta.** Primer paso para depurar: buscar la línea `ERROR ... GlobalExceptionHandler` y leer el primer `at com.factory...`.

---

## 8. Guardar el log en un archivo (opcional)

Por defecto no se guarda nada. Si en `local` quieres conservarlo, en `application-local.yaml`:

```yaml
logging:
  file:
    name: logs/tecnofactory.log      # se crea dentro de la carpeta del proyecto
  logback:
    rollingpolicy:
      max-file-size: 10MB            # al llegar a 10 MB abre un archivo nuevo
      max-history: 7                 # conserva los últimos 7 archivos
```

Se combina con el bloque `logging.level` que ya tienes: ambos van bajo la **misma** clave `logging:`, no dos veces.

| Detalle | Explicación |
| --- | --- |
| Con `logging.file.name` el log sale por **consola y por archivo** | No reemplaza la consola |
| `max-file-size` y `max-history` | Evitan que el archivo crezca sin límite |
| Agrega `logs/` y `*.log` a `.gitignore` | Un log puede tener datos que no deben subirse a Git |
| Si cambias el nivel, no hace falta borrar el archivo | Los mensajes nuevos se añaden al final |

> Valores por defecto: si activas el archivo y no indicas los límites, Spring Boot usa 10 MB por archivo y conserva 7. Conviene dejarlos escritos para que quede claro.

---

## 9. Logs en Docker y producción

En producción con Docker lo habitual es **no usar archivo**: la app escribe en la consola y Docker guarda esa salida. El problema es que, sin límites, ese registro de Docker crece sin parar. Se limita en el servicio del compose (cuando actives `backend`):

```yaml
  backend:
    ...
    logging:
      driver: json-file
      options:
        max-size: "10m"     # tamaño máximo por archivo
        max-file: "3"       # cuántos archivos conserva
```

| Tema | Recomendación |
| --- | --- |
| Rotación | Configurar `max-size` y `max-file` en el compose |
| Retención | Decidir cuánto tiempo guardar (por ejemplo 7 a 30 días) |
| Nivel en `prod` | `WARN` para librerías, `INFO` para tu código (ya configurado) |
| Copia fuera del servidor | Si el disco falla, los logs se pierden igual que los datos |
| Datos personales | Revisar que no se escriban (ver 6.5) |

Esto está en el plan de la fase 9 ([10](10-pendientes-y-futuro.md)).

---

## 10. Ver el SQL que ejecuta Hibernate (solo local)

Hay dos formas, con comportamientos distintos:

| Forma | Cómo | Qué muestra |
| --- | --- | --- |
| `spring.jpa.show-sql: true` (la que tienes) | Imprime cada consulta directamente por la salida estándar | La consulta, **sin** los valores de los parámetros (aparecen como `?`) |
| `logging.level.org.hibernate.SQL: DEBUG` | Usa el sistema de logs | La consulta, y respeta formato, niveles y el archivo de log |

Para ver también los valores de los parámetros se suele añadir `logging.level.org.hibernate.orm.jdbc.bind: TRACE`. Es un detalle que **depende de la versión de Hibernate** y que no se ha verificado en este proyecto; úsalo solo en `local`. Los valores pueden incluir datos personales, y por eso **nunca debe activarse en `prod`**.

El formato en varias líneas (`format_sql: true`) ya funciona desde que se corrigió la indentación del yaml.

---

## 11. Problemas frecuentes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `log` aparece en rojo en IntelliJ | Lombok no procesa anotaciones | Activar *Annotation Processing* y recompilar |
| `Cannot resolve symbol 'Slf4j'` | Falta el import | `import lombok.extern.slf4j.Slf4j;` |
| Escribo `log.debug` y no sale nada | El nivel está en `INFO` o superior para ese paquete | Revisar `logging.level.com.factory` (en `local` es `DEBUG`) |
| En `prod` no veo mis `log.debug` | Es lo esperado: `prod` usa `INFO` | Subir el nivel temporalmente solo si es imprescindible |
| No encuentro el detalle de un 500 | Se mira la respuesta del cliente en vez del log | Mirar la consola o `docker compose logs backend` |
| Salen demasiados mensajes de Hibernate o Spring | `root` está en `DEBUG` | Dejar `root: INFO` y subir solo `com.factory` |
| El archivo `logs/` aparece en `git status` | Falta ignorarlo | Agregar `logs/` y `*.log` a `.gitignore` |
| `docker compose logs backend` dice `no such service` | El servicio `backend` sigue comentado | Usar la consola de IntelliJ o la terminal |
| El stack trace no muestra mi código | Hay varias capas de excepciones | Buscar más abajo la línea `Caused by:` y el primer `at com.factory...` |

---

## 12. Glosario

| Término | Significado |
| --- | --- |
| **Log** | Registro de eventos de la aplicación |
| **Logger** | Objeto con el que una clase escribe mensajes (`log`) |
| **Nivel** | Importancia del mensaje: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |
| **Appender** | Destino de los mensajes: consola o archivo |
| **Stack trace** | Lista de llamadas que llevó hasta el error; indica archivo y línea |
| **Rotación** | Abrir un archivo nuevo cuando el actual se llena, y borrar los más antiguos |
| **PID** | Número con el que el sistema identifica al proceso de la aplicación |
| **SLF4J** | Interfaz común de logging en Java |
| **Logback** | Motor de logging que incluye Spring Boot |
| **`@Slf4j`** | Anotación de Lombok que crea la variable `log` |
