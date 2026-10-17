# 11. Cambios recientes

Bitácora de la última sesión de trabajo (oct 2026): qué se revisó, qué se agregó, por qué se decidió así y cómo comprobarlo. Es el documento a leer primero si vuelves al proyecto después de un tiempo.

## Resumen

| # | Cambio | Archivo(s) | Estado |
| --- | --- | --- | --- |
| 1 | Revisión de integridad del proyecto | — | Hecha; hallazgos en la sección 1 |
| 2 | `detalle_boleta` guarda el producto; `carrito` borra en cascada; `boleta.estado` con valor por defecto | `V1__esquema_inicial.sql` | Hecho |
| 3 | Corrección de la indentación de `jpa:` | `application-local.yaml` | Hecho |
| 4 | Datos de referencia: roles, 16 regiones, 346 comunas | `V2__datos_iniciales.sql` (**nuevo**) | Hecho |
| 5 | Manejo de errores de la API | `exception/` (4 archivos) | Escrito, **sin probar** |
| 6 | Documentación actualizada | `README.md` y `docs/` | Hecho |
| 7 | `SecurityConfig` mínimo, entidades, repositorios | — | **Siguiente paso** |

---

## 1. Revisión de integridad del proyecto

Se revisó el ZIP completo (221 archivos, estructura íntegra). Hallazgos:

| Hallazgo | Gravedad | Resolución |
| --- | --- | --- |
| Las clases Java eran cascarones vacíos y las carpetas `entity`, `repository`, `service`, `controller`, `dto` y `mapper` no tenían archivos | Informativo | Es el trabajo de las fases 2 a 4 |
| `RecursoNoEncontradoException` no extendía `RuntimeException` | Alta | Corregido (sección 5) |
| `SecurityConfig` vacío con `spring-boot-starter-security`: Spring protege todo (401), incluido Swagger | Alta | **Pendiente**: hace falta una versión mínima para probar controllers |
| Sin datos en `region`, `comuna` y `tipo_usuario`: no se podía crear ni un usuario (FK obligatorias) | Alta | `V2` (sección 4) |
| `application-local.yaml` con `properties` mal anidado: `format_sql` se ignoraba | Media | Corregido (sección 3) |
| Documentación desactualizada respecto a `V1` (decía "sin adaptar") | Media | Documentos actualizados |
| FK `carrito` → `usuario` y `detalle_boleta` → `boleta` no coincidían con lo documentado | Media | Decidido y unificado (sección 2) |
| `detalle_boleta` no guardaba qué producto se compró | Media | Corregido (sección 2) |
| Diagrama (`.dmd`) desactualizado | Baja | Pendiente |
| `Dockerfile` vacío; servicio `backend` comentado | Baja | Normal en esta etapa; solo importa al desplegar |
| El ZIP incluía el `.env` con claves reales y `target/` (jar de 78 MB) | Atención | `.gitignore` los excluye; no compartir el ZIP así. Si se compartió, rotar las claves |
| El test `contextLoads` necesita MySQL | Baja | Pendiente (fase 8) |

---

## 2. Cambios en `V1__esquema_inicial.sql`

Se editó `V1` directamente (todavía no se despliega en ningún servidor). Tres cambios:

**a) `detalle_boleta` ahora sabe qué producto es.** Antes solo guardaba una copia congelada (nombre, precio, descuento, cantidad). Sin el producto no se puede hacer un reporte de ventas, devolver stock si se anula una compra ni ofrecer "volver a comprar".

```sql
-- dentro del CREATE TABLE detalle_boleta
producto_id_producto BIGINT NOT NULL,

-- junto a las demás FK
ALTER TABLE detalle_boleta
    ADD CONSTRAINT detalle_boleta_producto_FK FOREIGN KEY (producto_id_producto)
    REFERENCES producto (id_producto) ON DELETE RESTRICT;
```

La columna debe existir en el `CREATE TABLE`; si solo se agrega el `ALTER`, falla con `Key column ... doesn't exist in table`. Sigue guardándose la copia de `nombre` y `precio_unitario`: la boleta antigua conserva lo que se pagó aunque el producto cambie.

**b) `carrito` → `usuario` pasa a `ON DELETE CASCADE`.** El carrito es desechable y no vale nada sin su usuario. Se **editó la FK existente** (`carrito_usuario_FK`) en vez de crear una nueva: dos restricciones con el mismo nombre dan error.

**c) `boleta.estado` con valor por defecto.**

```sql
estado BOOLEAN NOT NULL DEFAULT TRUE,
```

Así un `INSERT` que no lo envíe no falla. Ojo: el valor por defecto de la base **no se aplica** si Hibernate manda `NULL` explícito, por eso la entidad también debe inicializarlo (`private Boolean estado = true;`).

**Decisión que se mantiene:** `detalle_boleta` → `boleta` queda en `RESTRICT` (no `CASCADE` como decía la documentación antigua), porque las boletas son registros de compra y no deben borrarse nunca. Tabla completa de reglas en [06](06-base-de-datos.md#reglas-de-borrado-on-delete).

> Cada vez que se edita `V1` hay que recrear la base: `docker compose down -v` y `docker compose up -d`. Si no, Flyway falla con `Migration checksum mismatch`.

---

## 3. `application-local.yaml`

El bloque `jpa:` tenía `properties` anidado dentro de `hibernate`, así que `format_sql` se ignoraba sin avisar. Quedó:

```yaml
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
```

Se verificó leyendo el archivo con un parser YAML: `spring.jpa` contiene `show-sql`, `hibernate` y `properties` como claves hermanas. El resto del archivo no cambió.

---

## 4. `V2__datos_iniciales.sql`

Carga los datos de referencia que la API necesita para existir.

| Tabla | Filas | Detalle |
| --- | --- | --- |
| `tipo_usuario` | 3 | `ADMINISTRADOR`, `VENDEDOR`, `CLIENTE` (mayúsculas, para que el rol del JWT salga directo) |
| `region` | 16 | Id = número de región. Incluye Ñuble (16) y Arica y Parinacota (15) |
| `comuna` | 346 | Id = código **CUT** de SUBDERE (Santiago = 13101). Los códigos de las regiones I a IX no llevan cero inicial porque la columna es `INT` |

### Cómo se generó y verificó

Las comunas se generaron desde un paquete con los códigos CUT y se cruzaron con un segundo paquete independiente. El cruce encontró 6 nombres mal escritos en las fuentes, corregidos a mano:

| Código | Fuente decía | Queda |
| --- | --- | --- |
| 11201 | Aisén | **Aysén** |
| 11101 | Coihaique | **Coyhaique** |
| 6204 | Marichihue | **Marchigüe** |
| 16207 | Ranqui | **Ránquil** |
| 5303 | Riconada | **Rinconada** |
| 16305 | San Nico | **San Nicolás** |

Comprobaciones: 346 ids únicos, cada código calza con su región (`código ÷ 1000 = región`), las cantidades por región coinciden con las oficiales (por ejemplo Metropolitana 52, Valparaíso 38, Ñuble 21) y 17 comunas de control tienen el código correcto. Los nombres se contrastaron con la grafía oficial conocida, **no** con el decreto de SUBDERE; si necesitas literalidad legal, conviene una pasada final.

### Qué queda fuera de V2 a propósito

| Pieza | Por qué | Cómo se resuelve |
| --- | --- | --- |
| **Usuario administrador** | Necesita un hash BCrypt. La contraseña no debe pasar por chats ni quedar en Git | Crearlo cuando exista `AuthService`, leyendo la clave desde una variable de entorno; o generar tú el hash y ponerlo en un `V3` |
| **Categorías base** | Dependen de lo que venda TecnoFactory | `V3` cuando definas la lista |

### Uso en el código

- Los ids de `tipo_usuario` son `AUTO_INCREMENT`: **busca siempre por nombre** (`findByNombreTipoUsuario("CLIENTE")`), nunca asumas que el id es 1, 2 o 3.
- Los ids de `region` y `comuna` son fijos (CUT): se usan tal cual desde el frontend.

---

## 5. Manejo de errores

Cuatro archivos en `src/main/java/com/factory/tecnofactory/exception/`. **Cada clase pública va en su propio archivo.**

**`RecursoNoEncontradoException.java`**

```java
package com.factory.tecnofactory.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " no encontrado con id " + id);
    }
}
```

**`ReglaNegocioException.java`** (stock insuficiente, correo duplicado, etc.)

```java
package com.factory.tecnofactory.exception;

public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
```

**`ErrorResponse.java`**

```java
package com.factory.tecnofactory.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles) {
}
```

**`GlobalExceptionHandler.java`**

```java
package com.factory.tecnofactory.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(ReglaNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", req, campos);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de datos", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje,
                                                HttpServletRequest req, Map<String, String> detalles) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), detalles));
    }
}
```

### Qué responde cada excepción

| Excepción | HTTP | Cuándo |
| --- | --- | --- |
| `RecursoNoEncontradoException` | 404 | `findById` sin resultado |
| `ReglaNegocioException` | 409 | Stock insuficiente, correo ya registrado, producto desactivado |
| `MethodArgumentNotValidException` | 400 | Falla `@Valid` en un `@RequestBody`; `detalles` trae un mensaje por campo |
| `DataIntegrityViolationException` | 409 | Choque con un `UNIQUE`, una FK o un `CHECK` de la base |
| Cualquier otra | 500 | Error inesperado: se registra en el log y el cliente recibe un texto genérico |

Ejemplo de respuesta de validación:

```json
{
  "timestamp": "2026-10-16T21:30:00.123",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Datos inválidos",
  "ruta": "/api/productos",
  "detalles": { "nombre": "<mensaje de la validación>", "precio": "<mensaje de la validación>" }
}
```

Uso desde un service:

```java
Producto p = productoRepository.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));   // 404

if (p.getStock() < cantidad) {
    throw new ReglaNegocioException("Stock insuficiente para " + p.getNombre());  // 409
}
```

> Los mensajes de las excepciones propias **se muestran al cliente** (incluso en `prod`), así que escríbelos pensando en el usuario y sin datos internos.

### Limitación conocida y mejora recomendada

El handler genérico `Exception.class` captura **todo** lo que no tenga un handler propio. Eso incluye errores del propio Spring que deberían ser 4xx y hoy saldrían como **500**:

| Situación | Debería ser | Excepción |
| --- | --- | --- |
| JSON mal formado en el cuerpo | 400 | `HttpMessageNotReadableException` |
| Método HTTP no permitido | 405 | `HttpRequestMethodNotSupportedException` |
| Ruta inexistente | 404 | `NoResourceFoundException` |
| Falla de validación en `@RequestParam` o `@PathVariable` | 400 | `HandlerMethodValidationException` |
| Acceso denegado (cuando exista Security con roles) | 403 | `AccessDeniedException` (Spring Security) |

Esta mejora **no está aplicada ni probada**. Conviene agregarla al probar el primer controller; el patrón es el mismo, por ejemplo:

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", req, null);
}

@ExceptionHandler(AccessDeniedException.class)   // org.springframework.security.access
public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", req, null);
}
```

Los 401 de autenticación se resuelven en `SecurityConfig` (fase 4), no aquí.

---

## 6. Problemas de IntelliJ que aparecieron

| Mensaje | Causa | Solución |
| --- | --- | --- |
| `Class 'X' is public, should be declared in a file named 'X.java'` | Varias clases públicas pegadas en un solo archivo | Un archivo por clase (*New, Java Class* o *Record*) |
| `ErrorResponse(LocalDateTime, int, ...) cannot be applied to (java.time.LocalDateTime, ...)` | Faltaban los `import` en el archivo del `record` | Agregar `java.time.LocalDateTime` y `java.util.Map` |
| Dos opciones al importar `MethodArgumentNotValidException` | Existen en `spring-web` y en `spring-messaging` | Elegir `org.springframework.web.bind...` |

Más casos en [09](09-errores-comunes.md#java-e-intellij).

---

## 7. Documentación actualizada

| Archivo | Cambio |
| --- | --- |
| `README.md` | Estado del proyecto, índice (agrega el 11) y reglas de oro |
| `01-arquitectura-y-stack.md` | Árbol de carpetas, estado por paquete y migraciones |
| `04-perfiles-spring.md` | `GlobalExceptionHandler` ya existe; indentación de `jpa:` corregida |
| `05-flyway-y-migraciones.md` | `V1` y `V2`, reglas para datos de referencia |
| `06-base-de-datos.md` | **Reescrito**: esquema real, FK, observaciones resueltas, `V2`, mapeo a entidades |
| `07-ejecucion-local-y-prod.md` | Dos migraciones, nombres de tablas en minúsculas, verificación de datos |
| `08-seguridad.md` | Manejo de errores escrito y limitaciones; administrador inicial |
| `09-errores-comunes.md` | Sección Java e IntelliJ y errores de migración nuevos |
| `10-pendientes-y-futuro.md` | Estado por fase; fases 0 a 2 detalladas |
| `11-cambios-recientes.md` | **Nuevo** (este archivo) |

Sin cambios: `02-docker-y-mysql.md` y `03-variables-y-claves.md`.

---

## Cómo verificar todo en tu máquina

La verificación de `V1` y `V2` se hizo en MariaDB 10.11. Para confirmarla en tu MySQL 8.4:

```bash
# 1. Base limpia
docker compose down -v
docker compose up -d
docker compose ps                  # esperar "healthy"

# 2. Arrancar la app en local (carga el .env en la terminal)
set -a; source .env; set +a
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

En el log debe aparecer `Successfully applied 2 migrations`. Luego, en otra terminal:

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory -e "
SELECT version, description, success FROM flyway_schema_history;
SELECT (SELECT COUNT(*) FROM tipo_usuario) tipos,
       (SELECT COUNT(*) FROM region) regiones,
       (SELECT COUNT(*) FROM comuna) comunas;
SHOW COLUMNS FROM boleta LIKE 'estado';
SHOW COLUMNS FROM detalle_boleta LIKE 'producto_id_producto';"
```

| Comprobación | Resultado esperado |
| --- | --- |
| `flyway_schema_history` | Versiones 1 y 2, `success = 1` |
| Conteos | 3 / 16 / 346 |
| `boleta.estado` | `Default = 1` (si sale `NULL`, falta el `DEFAULT TRUE` en `V1`) |
| `detalle_boleta.producto_id_producto` | La columna existe |

Si la app arranca con el perfil `local` y `ddl-auto: validate` sin entidades, solo se valida Flyway; la validación de tipos llega al escribir las entidades.

---

## Qué sigue

1. **`SecurityConfig` mínimo y temporal** (permitir todas las rutas), para poder probar controllers. Solo local; se reemplaza en la fase 4.
2. **Entidades**, en este orden: `Region`, `Comuna`, `TipoUsuario`, `Usuario`, `Categoria`, `Producto`, `ImagenUrl`, `Carrito`, `DetalleCarrito`, `Boleta`, `DetalleBoleta`, `Blog` (mapeo en [06](06-base-de-datos.md#mapeo-a-entidades-java)).
3. **Repositories**, uno por entidad. Arrancar con `validate` para comprobar que todo calza.
4. **Primera "rebanada" completa**: `Categoria` (DTOs, mapper, service, controller) probada en Swagger; después `Producto`.
5. **Ampliar el `GlobalExceptionHandler`** (sección 5) al probar el primer controller.

---

## Qué no se pudo verificar

- **No se compiló el proyecto** (el entorno de revisión no accede a Maven Central), por lo que no se confirmó que el `pom.xml` resuelva con Spring Boot 4.1.1 ni que las clases de `exception/` compilen.
- `V1` y `V2` se probaron en **MariaDB 10.11**, no en MySQL 8.4.
- `GlobalExceptionHandler` no se ejecutó contra ningún controller.
- El `DEFAULT TRUE` de `boleta.estado` figura como decidido; la copia de `V1` revisada por última vez aún no lo tenía. Comprobar con el `SHOW COLUMNS` de arriba.
