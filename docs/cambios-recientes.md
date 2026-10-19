# Cambios recientes

**Un solo archivo vivo.** Aquí está lo que falta por hacer (arriba) y lo que ya se hizo, sesión por sesión (abajo). No se crean copias numeradas: en cada sesión nueva se **agrega una entrada al principio del Historial** y se actualiza la lista de Pendiente.

Cómo mantenerlo:

1. Al terminar algo, márcalo `[x]` en Pendiente o muévelo a la entrada de la sesión.
2. Al empezar una sesión, agrega arriba del Historial un título nuevo (`### fecha: tema`) y describe lo que cambió, por qué y cómo comprobarlo.
3. Las decisiones que sigan abiertas viven en Pendiente hasta que se resuelvan.

El plan completo por fases está en [10-pendientes-y-futuro.md](10-pendientes-y-futuro.md); este archivo es la lista corta de **lo próximo**.

---

## Pendiente

### Siguiente paso (en este orden)

- [x] **Confirmar un arranque completo**: el log del 18 oct muestra `Found 4 JPA repository interfaces` y `Started TecnoFactoryApplication`; Swagger carga y `/actuator/health` responde.
- [x] **Pasar `run_dv` y `fecha_nacimiento` a `runDv` y `fechaNacimiento`**: aplicado (el service usa `setRunDv` y `setFechaNacimiento` y compila).
- [ ] **Confirmar que se quitaron los `unique = true`** de `run_dv`, `nombre_region` y `nombre_comuna` ([13, sección 7](13-entidades-explicadas.md#7-observaciones-sobre-el-código-actual)).
- [x] **Paso 6: `UsuarioService`** con "crear administrador si no existe".
- [x] **Paso 7: `ApplicationRunner`**: el primer arranque creó al administrador (verificado en el log).
- [ ] **Completar las 3 comprobaciones del administrador** ([17, secciones 8 y 9](17-administrador-inicial-explicado.md#8-comandos-importantes)): la fila en la base (`rol`, `$2a$10$`, largo 60, `activo = 1`), un **segundo arranque** que no duplique, y que la clave **no** aparezca en `logs/tecnofactory.log`.
- [ ] **Paso 8: DTOs y mapper** de usuario (MapStruct) con validaciones, incluido el RUN chileno.
- [ ] **Paso 9: `UsuarioController`** probado en Swagger; será el **ejemplo para tu compañero**.
- [ ] **Entidades y repositories restantes**: `Categoria`, `Producto`, `ImagenUrl`, `Carrito`, `DetalleCarrito`, `Boleta`, `DetalleBoleta`, `Blog` (mapeo en [06](06-base-de-datos.md#mapeo-a-entidades-java)). Cuidado con `TEXT` (`blog.contenido`) y con la clave compuesta de `detalle_carrito`.
- [ ] **Primera "rebanada" completa**: `Categoria` (DTOs, mapper, service, controller) probada en Swagger; después `Producto`.
- [ ] **Probar el `GlobalExceptionHandler`** con las pruebas de [Cobertura de los errores de Spring](#cobertura-de-los-errores-de-spring) cuando exista el primer controller.

### Verificaciones pendientes

- [x] **Confirmar `V1` y `V2` en tu MySQL 8.4** (se probaron en MariaDB 10.11): **hecho**.
- [ ] **Confirmar el `DEFAULT TRUE` de `boleta.estado`** en tu `V1`: `SHOW COLUMNS FROM boleta LIKE 'estado';` debe mostrar `Default = 1`.
- [x] **Compilar el proyecto**: **hecho**. El log del 18 oct muestra `Compiling 20 source files` sin errores, incluidas las clases de `exception/`. Falta **probarlas** con un controller.
- [ ] **Confirmar que la sección 1.4 ampliada** (comprobadas y no comprobadas) quedó pegada en `11-excepciones-explicadas.md`: se entregó como un trozo aparte, no dentro del archivo.
- [x] **Comprobar que el `.env` nunca se subió a Git**: **hecho**.

### Decisiones abiertas

- [x] **Administrador inicial**: se decidió el **18 oct** crearlo por código al arrancar (opción B de [08](08-seguridad.md#administrador-inicial)) y descartar la plantilla `V4`.
- [x] **Datos del administrador**: correo y clave vienen de `ADMIN_EMAIL` y `ADMIN_PASSWORD`; el resto (RUN `11111111-1`, nombre, apellidos, fecha, dirección, teléfono, comuna `13101`) son **valores de relleno fijos** en el runner. Se eligió por defecto al no haber otra indicación; se puede cambiar.
- [ ] **401 frente a 403** en `AccessDeniedException` (fase 4): hoy un visitante sin token recibiría 403; ver [11](11-excepciones-explicadas.md#810-accessdeniedexception--403).
- [ ] **Cómo se aplica el descuento** (0 a 100) sobre el `precio` y su copia en `detalle_boleta`.
- [ ] **Detalle por parámetro** en `HandlerMethodValidationException` (hoy solo dice "Parámetros inválidos").

### Limpieza pendiente

- [x] Reemplazar los manuales viejos (`COMO_EJECUTAR.md`, `MANUAL_TECNOFACTORY.md`, `TecnoFactory - Guía del proyecto.md`) por la carpeta `docs/`: **hecho**.
- [x] Mover `ExplicacionesCrudas.md` fuera del repositorio o a `docs/archivo/`: **hecho**.
- [ ] Si activas el log en archivo, agregar `logs/` y `*.log` a `.gitignore` (ver [12](12-logs.md#8-guardar-el-log-en-un-archivo-opcional)).
- [ ] Actualizar el diagrama de `Diagrama/`: **pospuesto** (es de Oracle Data Modeler y sus tipos de dato difieren de los de MySQL; la fuente de verdad es `V1`).
- [x] No compartir el ZIP del proyecto con el `.env` ni `target/`: **tenido en cuenta**.
- [ ] Limpiar las entidades: `import jakarta.validation.constraints.Min` sin usar y bloques comentados al final de `Region.java` y `Comuna.java`; `Ctrl+Alt+O` en IntelliJ quita los `import` sobrantes de las entidades y los repositories.
- [ ] Cambiar `Optional<List<...>>` por `List<...>` en `RegionRepository`, `ComunaRepository` y `UsuarioRepository` (funciona, pero sobra el `Optional`).
- [ ] Borrar `docs/11-cambios-recientes.md`: es una copia antigua de este archivo y el README indica que no se crean copias numeradas.
- [x] El `README.md` enlaza `02-docker-y-mysql.md` y `03-variables-y-claves.md`, que no venían en el ZIP revisado: **recuperados** y añadidos al paquete de documentación (18 oct).

---

## Historial

### 18 oct 2026 (noche): administrador inicial al arrancar (pasos 5b, 6 y 7)

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| 5b | Entidades corregidas (`runDv`, `fechaNacimiento`) y arranque completo | Confirmado: Swagger carga y `/actuator/health` responde |
| 6 | `DatosAdministrador` (record) y `UsuarioService` con `crearAdministradorSiNoExiste` | Compila y arranca |
| 7 | `AdministradorInicialRunner` (`ApplicationRunner`) y variables `ADMIN_EMAIL` / `ADMIN_PASSWORD` en el `.env` | **El primer arranque creó al administrador** (log: `Administrador inicial creado con el correo ...`) |

**Qué mostró el log del primer arranque** (detalle en [17, sección 2](17-administrador-inicial-explicado.md#2-orden-de-ejecución)):

- El runner se ejecuta **después** de `Started TecnoFactoryApplication` y con Tomcat ya arriba.
- Cuatro consultas, en este orden: `existsByTipoUsuarioNombre` (con `JOIN` y `limit`), búsqueda del rol, búsqueda de la comuna e `insert into usuario`.
- La consulta de la comuna **no** hace `JOIN` con `region`: `LAZY` funciona.
- El `insert` muestra `?` en vez de valores: la clave no aparece en el log por esa vía.
- Sigue saliendo `Using generated security password` (inofensiva, ver [15](15-seguridad-codigo-explicado.md#36-dos-detalles-que-sorprenden)).

**Decisión tomada por defecto** (no hubo respuesta a la consulta): correo y clave desde variables de entorno; RUN, nombre, apellidos, fecha, dirección, teléfono y comuna como valores de relleno fijos en el runner.

**Documentación:** nueva [17-administrador-inicial-explicado.md](17-administrador-inicial-explicado.md) (código línea por línea, orden de ejecución, glosario, comandos y casos de error). Actualizados: `README.md`, `03`, `08`, `10`, `13`, `14`, `15` y este archivo.

**Qué no se pudo verificar:**

- El contenido de la fila en la base, el segundo arranque sin duplicar y la ausencia de la clave en `logs/tecnofactory.log`: son los comandos de [17, sección 8](17-administrador-inicial-explicado.md#8-comandos-importantes); el log pegado solo cubre el primer arranque.
- Que se quitaron los `unique = true` de las entidades (no se vieron tus archivos finales).
- La guía 17 documenta el código **tal como se entregó en los pasos 6 y 7**; no se compararon con tus archivos. El log es coherente con él (mismo mensaje y mismo orden de consultas).
- Que la clave y el hash funcionen juntos al iniciar sesión: solo posible con el login (fase 4).

### 18 oct 2026: entidades, repositories y `SecurityConfig`, paso a paso

Se avanzó **de a un paso**, arrancando la app al terminar cada uno. Cada pieza quedó explicada línea por línea en una guía nueva.

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| 1 | Entidad `TipoUsuario` + experimento de romper una columna a propósito | Arrancó; se aprendió a leer `Schema-validation: missing column` |
| 2 | `Region` y `Comuna` | **Falló** con `missing column [id_comuna] in table [region]`: `Comuna` tenía `@Table(name = "region")`. Corregido; arrancó |
| 3 | `Usuario` (con `@JdbcTypeCode(SqlTypes.CHAR)` para `run_dv`) | **Validó** contra MySQL 8.4: el log mostró `Initialized JPA EntityManagerFactory` |
| 4 | Cuatro repositories; se añadieron métodos a propósito para provocar errores | **Fallaron** `findBynombreComuna`, `findByNombreComuna` y (previsto) `findByNombreTipoUsuario`: se usó el nombre de la columna en vez del atributo. Corregidos; también el tipo del id (`Long` → `Integer` en `Region` y `Comuna`) |
| 5 | `PasswordEncoder` (BCrypt) y `SecurityConfig` abierto (`permitAll`) | Escritos y **confirmados** (Swagger carga; `/actuator/health` responde) |

**Decisiones:**

| Decisión | Motivo |
| --- | --- |
| El administrador se crea **por código al arrancar**, no con `V4` | Es más seguro (nada en Git) y sirve de ejemplo completo (service, repository, `PasswordEncoder`) para el equipo |
| `SecurityConfig` abierto mientras se desarrolla | Para que tú y tu compañero prueben controllers sin 401; se cierra en la fase 4 |
| CSRF desactivado y sin sesiones | La API usará tokens, no cookies |

**Documentación nueva:**

| Archivo | Contenido |
| --- | --- |
| [13-entidades-explicadas.md](13-entidades-explicadas.md) | Cada anotación (`@Entity`, `@ManyToOne`, `@JdbcTypeCode`...), cada entidad línea por línea y tres observaciones sobre el código actual |
| [14-repositories-explicados.md](14-repositories-explicados.md) | Cómo se lee el nombre de un método, qué retorno elegir, cada repository línea por línea |
| [15-seguridad-codigo-explicado.md](15-seguridad-codigo-explicado.md) | `SecurityConfig` línea por línea, BCrypt, qué cambia al llegar el login |
| [16-leer-errores-de-spring.md](16-leer-errores-de-spring.md) | Método de 4 pasos para leer un error de arranque, con los tres casos reales |

**Documentación actualizada:** `03` (variables `ADMIN_EMAIL` y `ADMIN_PASSWORD` planificadas), `README.md` (índice, estado y reglas de oro), `06` (mapeo de `CHAR(1)`, estado de las entidades), `08` (`SecurityConfig`, administrador por código), `09` (sección de entidades y repositories), `10` (fases, paso a paso del administrador).

**Observaciones sobre el código (no impiden arrancar):** `unique = true` en `run_dv`, `nombre_region` y `nombre_comuna` que `V1` no declara; `run_dv` y `fecha_nacimiento` con guion bajo en Java. Detalle y corrección en [13, sección 7](13-entidades-explicadas.md#7-observaciones-sobre-el-código-actual).

**Qué no se pudo verificar:**

- ~~El último arranque con los repositories corregidos y el `SecurityConfig` no se vio~~ (**resuelto esa noche**, ver la entrada de más arriba): no hay confirmación de `Started TecnoFactoryApplication` ni de que Swagger cargue.
- El proyecto no se compiló ni ejecutó en el entorno donde se escribió esta documentación (sin Spring ni MySQL disponibles). Las guías se basan en la lectura de tu código y de tus logs.
- El experimento de quitar `@JdbcTypeCode` para ver el error `wrong column type` quedó como sugerencia; el texto exacto del error que aparece en la guía 13 es el típico de Hibernate, pero no se vio en tu log.

### 17 oct 2026 (tercera parte): verificaciones, categorías base y administrador

**Verificado por ti:**

| Qué | Resultado |
| --- | --- |
| `V1` y `V2` en MySQL 8.4 de Docker | Aplicadas |
| `.env` fuera de Git | Confirmado |
| Manuales viejos reemplazados por `docs/`; `ExplicacionesCrudas.md` retirado | Hecho |
| Diagrama de `Diagrama/` | **Pospuesto**: es de Oracle y sus tipos difieren de los de MySQL |

**Borradores nuevos** (probados junto a `V1` y `V2` en MariaDB 10.11):

| Archivo | Contenido | Antes de aplicarlo |
| --- | --- | --- |
| `V3__categorias_base.sql` | 8 categorías de ejemplo para una tienda de tecnología | Ajustar nombres y descripciones a lo que venda TecnoFactory |
| `V4__administrador_inicial.sql` | `INSERT` del administrador: rol buscado **por nombre** (no por id), comuna 13101 | Generar el hash BCrypt y cambiar los valores marcados con `CAMBIAR` |

Decisión de diseño: el administrador se crea con una migración que guarda el **hash** (nunca la clave). La alternativa de crearlo al arrancar desde variables de entorno queda para antes de producción, porque necesita las entidades y el `PasswordEncoder`. Detalle y pasos en [08](08-seguridad.md#administrador-inicial).

Pendiente de comprobar con el login real: que la clave y el hash funcionen juntos (fase 4).

### 17 oct 2026 (segunda parte): organización de la documentación y logs

| Cambio | Archivo(s) |
| --- | --- |
| Este archivo pasa a ser un **archivo vivo sin número** con Pendiente e Historial | `cambios-recientes.md` |
| La guía de excepciones pasa a llevar el número 11 (antes 12), para no dejar un hueco en la serie | `11-excepciones-explicadas.md` |
| Guía nueva sobre logs: qué es, qué hace `@Slf4j`, dónde verlo, niveles, cómo guardarlo en archivo | `12-logs.md` (**nuevo**) |
| Explicación ampliada de excepciones comprobadas y no comprobadas (rollback en `@Transactional`), entregada como inserto para la sección 1.4 | `11-excepciones-explicadas.md` (ver Pendiente) |
| Enlaces a la guía de logs | `README.md`, `04`, `09`, `10` |

Aclaración que motivó la guía: **`@Slf4j` (Lombok) no crea ningún archivo**; solo agrega la variable `log`. El motor que escribe es Logback, y por defecto lo hace en la consola.

### 17 oct 2026: esquema, datos iniciales y manejo de errores

#### Resumen

| Cambio | Archivo(s) | Estado |
| --- | --- | --- |
| Revisión de integridad del proyecto | — | Hecha; hallazgos en [Revisión de integridad](#revisión-de-integridad-del-proyecto) |
| `detalle_boleta` guarda el producto; `carrito` borra en cascada; `boleta.estado` con valor por defecto | `V1__esquema_inicial.sql` | Hecho |
| Corrección de la indentación de `jpa:` | `application-local.yaml` | Hecho |
| Datos de referencia: roles, 16 regiones, 346 comunas | `V2__datos_iniciales.sql` (**nuevo**) | Hecho |
| Manejo de errores de la API, incluidos los errores propios de Spring (400, 404, 405, 403) | `exception/` (4 archivos) | Escrito y revisado por lectura; **sin compilar ni probar**. Explicado en [11](11-excepciones-explicadas.md) |
| Documentación actualizada | `README.md` y `docs/` | Hecho |
| `SecurityConfig` mínimo, entidades, repositorios | — | **Siguiente paso** |

---

#### Revisión de integridad del proyecto

Se revisó el ZIP completo (221 archivos, estructura íntegra). Hallazgos:

| Hallazgo | Gravedad | Resolución |
| --- | --- | --- |
| Las clases Java eran cascarones vacíos y las carpetas `entity`, `repository`, `service`, `controller`, `dto` y `mapper` no tenían archivos | Informativo | Es el trabajo de las fases 2 a 4 |
| `RecursoNoEncontradoException` no extendía `RuntimeException` | Alta | Corregido (ver [Manejo de errores](#manejo-de-errores)) |
| `SecurityConfig` vacío con `spring-boot-starter-security`: Spring protege todo (401), incluido Swagger | Alta | **Pendiente**: hace falta una versión mínima para probar controllers |
| Sin datos en `region`, `comuna` y `tipo_usuario`: no se podía crear ni un usuario (FK obligatorias) | Alta | `V2` (ver [Datos iniciales](#datos-iniciales-v2__datos_inicialessql)) |
| `application-local.yaml` con `properties` mal anidado: `format_sql` se ignoraba | Media | Corregido (ver [application-local.yaml](#cambios-en-application-localyaml)) |
| Documentación desactualizada respecto a `V1` (decía "sin adaptar") | Media | Documentos actualizados |
| FK `carrito` → `usuario` y `detalle_boleta` → `boleta` no coincidían con lo documentado | Media | Decidido y unificado (ver [Cambios en V1](#cambios-en-v1__esquema_inicialsql)) |
| `detalle_boleta` no guardaba qué producto se compró | Media | Corregido (ver [Cambios en V1](#cambios-en-v1__esquema_inicialsql)) |
| Diagrama (`.dmd`) desactualizado | Baja | Pendiente |
| `Dockerfile` vacío; servicio `backend` comentado | Baja | Normal en esta etapa; solo importa al desplegar |
| El ZIP incluía el `.env` con claves reales y `target/` (jar de 78 MB) | Atención | `.gitignore` los excluye; no compartir el ZIP así. Si se compartió, rotar las claves |
| El test `contextLoads` necesita MySQL | Baja | Pendiente (fase 8) |

---

#### Cambios en `V1__esquema_inicial.sql`

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

#### Cambios en `application-local.yaml`

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

#### Datos iniciales: `V2__datos_iniciales.sql`

Carga los datos de referencia que la API necesita para existir.

| Tabla | Filas | Detalle |
| --- | --- | --- |
| `tipo_usuario` | 3 | `ADMINISTRADOR`, `VENDEDOR`, `CLIENTE` (mayúsculas, para que el rol del JWT salga directo) |
| `region` | 16 | Id = número de región. Incluye Ñuble (16) y Arica y Parinacota (15) |
| `comuna` | 346 | Id = código **CUT** de SUBDERE (Santiago = 13101). Los códigos de las regiones I a IX no llevan cero inicial porque la columna es `INT` |

##### Cómo se generó y verificó

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

##### Qué queda fuera de V2 a propósito

| Pieza | Por qué | Cómo se resuelve |
| --- | --- | --- |
| **Usuario administrador** | Necesita un hash BCrypt. La contraseña no debe pasar por chats ni quedar en Git | Crearlo cuando exista `AuthService`, leyendo la clave desde una variable de entorno; o generar tú el hash y ponerlo en un `V3` |
| **Categorías base** | Dependen de lo que venda TecnoFactory | `V3` cuando definas la lista |

##### Uso en el código

- Los ids de `tipo_usuario` son `AUTO_INCREMENT`: **busca siempre por nombre** (`findByNombreTipoUsuario("CLIENTE")`), nunca asumas que el id es 1, 2 o 3.
- Los ids de `region` y `comuna` son fijos (CUT): se usan tal cual desde el frontend.

---

#### Manejo de errores

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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Excepciones propias ----

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(ReglaNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // ---- Validación ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", req, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parámetros inválidos", req, null);
    }

    // ---- Errores de la petición HTTP ----

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Ruta no encontrada", req, null);
    }

    // ---- Base de datos y seguridad ----

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de datos", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", req, null);
    }

    // ---- Todo lo demás ----

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

> `AccessDeniedException` debe ser la de **`org.springframework.security.access`** (IntelliJ ofrece varias). Spring elige el handler cuyo tipo de excepción esté más cerca del que se lanzó, así que no importa el orden en que los escribas.

##### Qué responde cada excepción

| Excepción | HTTP | Cuándo |
| --- | --- | --- |
| `RecursoNoEncontradoException` | 404 | `findById` sin resultado |
| `ReglaNegocioException` | 409 | Stock insuficiente, correo ya registrado, producto desactivado |
| `MethodArgumentNotValidException` | 400 | Falla `@Valid` en un `@RequestBody`; `detalles` trae un mensaje por campo |
| `HandlerMethodValidationException` | 400 | Falla la validación de un `@RequestParam` o `@PathVariable` |
| `HttpMessageNotReadableException` | 400 | JSON mal formado o ilegible en el cuerpo |
| `HttpRequestMethodNotSupportedException` | 405 | Por ejemplo, `DELETE` en una ruta que solo acepta `GET` |
| `NoResourceFoundException` | 404 | La ruta no existe |
| `DataIntegrityViolationException` | 409 | Choque con un `UNIQUE`, una FK o un `CHECK` de la base |
| `AccessDeniedException` | 403 | Falta de permisos (seguridad por métodos, cuando exista) |
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

##### Cobertura de los errores de Spring

El handler genérico `Exception.class` captura **todo** lo que no tenga un handler propio. Sin handlers específicos, errores que deberían ser 4xx saldrían como **500**. Por eso se agregaron los cinco handlers de la tabla anterior (JSON ilegible, método no permitido, ruta inexistente, validación de parámetros y acceso denegado). Ya forman parte del código de arriba.

Puntos a tener en cuenta:

- **`AccessDeniedException` solo se activa con seguridad por métodos** (por ejemplo `@PreAuthorize("hasRole('ADMINISTRADOR')")`). Las reglas por ruta de `SecurityConfig` las resuelve Spring Security **antes** de llegar a los controllers y no pasan por este handler. Los 401 (sin token o token inválido) también se resuelven en `SecurityConfig` (fase 4), no aquí.
- Los mensajes de las excepciones propias se muestran al cliente, incluso en `prod`; los de Spring (JSON inválido, método no permitido...) son textos fijos y no revelan datos internos.
- **Estado: escrito, sin compilar ni probar.** Pruebas sugeridas cuando exista el primer controller:

| Prueba | Resultado esperado |
| --- | --- |
| `POST` con un JSON roto | 400 con `ErrorResponse` |
| `GET` a una ruta que no existe | 404 con `ErrorResponse` |
| `DELETE` a una ruta que solo acepta `GET` | 405 con `ErrorResponse` |
| `GET /api/productos/999999` (id inexistente) | 404 con el mensaje "Producto no encontrado con id 999999" |
| `POST` con un campo que no cumple `@Valid` | 400 con `detalles` por campo |

---

#### Problemas de IntelliJ que aparecieron

| Mensaje | Causa | Solución |
| --- | --- | --- |
| `Class 'X' is public, should be declared in a file named 'X.java'` | Varias clases públicas pegadas en un solo archivo | Un archivo por clase (*New, Java Class* o *Record*) |
| `ErrorResponse(LocalDateTime, int, ...) cannot be applied to (java.time.LocalDateTime, ...)` | Faltaban los `import` en el archivo del `record` | Agregar `java.time.LocalDateTime` y `java.util.Map` |
| Dos opciones al importar `MethodArgumentNotValidException` | Existen en `spring-web` y en `spring-messaging` | Elegir `org.springframework.web.bind...` |

Más casos en [09](09-errores-comunes.md#java-e-intellij).

---

#### Documentación actualizada

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
| `cambios-recientes.md` | **Nuevo** (este archivo): un solo archivo vivo, sin número, con pendientes e historial |
| `11-excepciones-explicadas.md` | **Nuevo**: guía línea por línea de las cuatro clases de `exception/` |

Sin cambios: `02-docker-y-mysql.md` y `03-variables-y-claves.md`.

---

#### Cómo verificar todo en tu máquina

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

#### Qué no se pudo verificar

- **No se compiló el proyecto** (el entorno de revisión no accede a Maven Central), por lo que no se confirmó que el `pom.xml` resuelva con Spring Boot 4.1.1 ni que las clases de `exception/` compilen.
- `V1` y `V2` se probaron en **MariaDB 10.11**, no en MySQL 8.4.
- `GlobalExceptionHandler` no se ejecutó contra ningún controller. Los cuatro archivos finales del proyecto se revisaron **leyéndolos** (imports, firmas y llamadas coinciden con lo documentado), no compilándolos.
- El `DEFAULT TRUE` de `boleta.estado` figura como decidido; la copia de `V1` revisada por última vez aún no lo tenía. Comprobar con el `SHOW COLUMNS` de arriba.
