# 28. Contacto explicado

El formulario de contacto, pieza por pieza: migración, entidad, repository, DTOs, mapper, service, controller, regla de seguridad y pruebas. Es la **receta de la [referencia 19](19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad)** aplicada a una entidad **pública y sin relaciones**, así que sirve de ejemplo de la versión más corta de la receta.

> **Estado:** código escrito el 3 nov 2026. **No se compiló ni se ejecutó** (el ZIP no traía `pom.xml`): falta `./mvnw test` y probarlo en Swagger.

## Índice

1. [Qué se pidió y qué se decidió](#1-qué-se-pidió-y-qué-se-decidió)
2. [`V6__contacto.sql`](#2-v6__contactosql)
3. [`Contacto.java`](#3-contactojava)
4. [`ContactoRepository.java`](#4-contactorepositoryjava)
5. [`ContactoRequest.java`](#5-contactorequestjava)
6. [`ContactoResponse.java`](#6-contactoresponsejava)
7. [`ContactoMapper.java`](#7-contactomapperjava)
8. [`ContactoService.java`](#8-contactoservicejava)
9. [`ContactoController.java`](#9-contactocontrollerjava)
10. [`SecurityConfig.java`: las dos reglas nuevas](#10-securityconfigjava-las-dos-reglas-nuevas)
11. [Las pruebas](#11-las-pruebas)
12. [Cómo probarlo a mano](#12-cómo-probarlo-a-mano)
13. [Límites y mejoras futuras](#13-límites-y-mejoras-futuras)

---

## 1. Qué se pidió y qué se decidió

**Pedido:** convertir a MySQL (con Flyway) una tabla `CONTACTO` escrita para otro motor, y crear entidad, mapper, DTOs y endpoints, con la condición de que **cualquier persona pueda crear o enviar su contacto**.

| Decisión | Motivo |
| --- | --- |
| `POST /api/contactos` es **público** | Es el requisito: un visitante no tiene cuenta ni token |
| La **lectura** es solo de `ADMINISTRADOR` | Cada mensaje trae el correo y el comentario de una persona; si cualquiera con sesión pudiera listarlos, un cliente leería los datos de otros (mismo criterio que `GET /api/usuarios/{id}`, [referencia 21](21-seguridad-jwt-y-roles-explicada.md#101-las-reglas-en-una-tabla)) |
| No hay `PUT` ni `DELETE` | No se pidieron; se agregan cuando el panel los necesite |
| `id_contacto` es `BIGINT AUTO_INCREMENT` | Igual que el resto de las tablas con id propio; el script original no generaba el id |
| La tabla **no** tiene fecha de envío | El script original no la traía; se puede agregar (ver sección 13) |
| Sin relaciones con `usuario` | Quien escribe puede no tener cuenta; por eso `nombre` y `correo` van en la propia fila |

## 2. `V6__contacto.sql`

Ruta: `src/main/resources/db/migration/V6__contacto.sql`

```sql
CREATE TABLE contacto (
    id_contacto BIGINT AUTO_INCREMENT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(250) NOT NULL,
    comentario VARCHAR(500) NOT NULL,
    CONSTRAINT contacto_PK PRIMARY KEY (id_contacto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

El script original estaba escrito para **otro motor** (usaba `GO`, `CLUSTERED` y `ALLOW_PAGE_LOCKS`). Qué cambió al pasarlo a MySQL:

| Original | MySQL | Por qué |
| --- | --- | --- |
| `GO` | (se quita) | Es un separador del cliente de SQL Server; MySQL no lo entiende |
| `PRIMARY KEY CLUSTERED` | `PRIMARY KEY` | En InnoDB la clave primaria ya es el índice agrupado |
| `WITH (ALLOW_PAGE_LOCKS ...)` | (se quita) | Opciones propias de SQL Server |
| `NUMERIC (8)` | `BIGINT AUTO_INCREMENT` | Convención del proyecto; el id lo genera la base |
| `Comentario` | `comentario` | `snake_case` en minúsculas; en Linux MySQL distingue mayúsculas en nombres de tabla |
| `ALTER TABLE ... ADD CONSTRAINT` aparte | `CONSTRAINT contacto_PK` dentro del `CREATE TABLE` | Mismo estilo que `V1` |
| (sin motor ni charset) | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | Igual que las demás tablas (tildes y emojis) |

**Por qué `V6`:** ya existen `V1` a `V5`; una migración aplicada no se edita ([referencia 05](05-flyway-y-migraciones.md)). Si `V6` solo existe en tu máquina y aún no la aplicaste, puedes cambiarla; una vez aplicada, un cambio es una `V7`.

## 3. `Contacto.java`

```java
@Entity
@Table(name = "contacto")
@Getter @Setter @NoArgsConstructor
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contacto")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 250)
    private String correo;

    @Column(name = "comentario", nullable = false, length = 500)
    private String comentario;
}
```

| Línea | Qué hace |
| --- | --- |
| `@Entity` y `@Table(name = "contacto")` | Esta clase es la tabla `contacto` |
| `@GeneratedValue(IDENTITY)` | El id lo genera MySQL (`AUTO_INCREMENT`); Hibernate lo lee tras el `INSERT` |
| `Long id` | `BIGINT` en la base ↔ `Long` en Java (no `long`: antes de guardar el id es `null`) |
| `@Column(... length = 100)` | Solo documenta el largo: **no crea ni verifica nada**, manda `V6` ([regla de oro 8](../../README.md#reglas-de-oro)) |
| `@Getter @Setter @NoArgsConstructor` | Lombok genera lo que JPA necesita |

Sin `@ManyToOne` ni colecciones: no hay nada `LAZY` que pueda fallar con `open-in-view: false`.

## 4. `ContactoRepository.java`

```java
public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
```

Vacío a propósito: `save`, `findById` y `findAll(Pageable)` vienen de `JpaRepository` ([referencia 14](14-repositories-explicados.md)).

## 5. `ContactoRequest.java`

```java
public record ContactoRequest(
        @NotBlank @Size(max = 100)                  String nombre,
        @NotBlank @Email @Size(max = 250)           String correo,
        @NotBlank @Size(max = 500)                  String comentario
) { }
```

(El archivo real lleva el `message` en español en cada anotación.)

| Anotación | Efecto |
| --- | --- |
| `@NotBlank` | Rechaza `null`, `""` y `"   "` |
| `@Email` | Comprueba el formato (`algo@dominio`) |
| `@Size(max = N)` | Rechaza lo que no cabría en la columna |

**Mass assignment:** el record **no tiene `id`**. Aunque el JSON traiga `"id": 99`, Jackson lo ignora y la base genera el suyo ([referencia 18, sección 12](18-dto-mapper-y-validaciones-explicados.md#12-mass-assignment-por-qué-el-request-no-tiene-rol)).

Un campo que falta o se pasa del máximo produce un **400** con `detalles` (`campo → mensaje`), formateado por `GlobalExceptionHandler`.

## 6. `ContactoResponse.java`

```java
public record ContactoResponse(Long id, String nombre, String correo, String comentario) { }
```

Es lo que sale en el 201 y en las lecturas. Como el contenido es lo que la propia persona acaba de escribir, devolverlo no expone nada nuevo.

## 7. `ContactoMapper.java`

```java
@Mapper(componentModel = "spring")
public interface ContactoMapper {

    ContactoResponse toResponse(Contacto contacto);

    @Mapping(target = "id", ignore = true)
    Contacto toEntity(ContactoRequest request);
}
```

| Línea | Qué hace |
| --- | --- |
| `@Mapper(componentModel = "spring")` | MapStruct genera `ContactoMapperImpl` como bean de Spring |
| `toResponse` | Copia los 4 campos de igual nombre |
| `toEntity` | Copia nombre, correo y comentario |
| `@Mapping(target = "id", ignore = true)` | El id lo genera la base; sin esta línea MapStruct avisa de un campo sin mapear |

El mapper **solo copia**: quitar espacios y guardar es trabajo del service ([regla de oro 10](../../README.md#reglas-de-oro)).

## 8. `ContactoService.java`

```java
@Transactional
public ContactoResponse crear(ContactoRequest request) {
    Contacto contacto = contactoMapper.toEntity(request);
    contacto.setNombre(contacto.getNombre().trim());
    contacto.setCorreo(contacto.getCorreo().trim());
    contacto.setComentario(contacto.getComentario().trim());

    Contacto guardado = contactoRepository.save(contacto);
    log.info("Contacto {} recibido", guardado.getId());
    return contactoMapper.toResponse(guardado);
}

@Transactional(readOnly = true)
public PageResponse<ContactoResponse> listar(Pageable pageable) { ... }

@Transactional(readOnly = true)
public ContactoResponse buscarPorId(Long id) { ... }   // 404 si no existe
```

| Parte | Qué hace |
| --- | --- |
| `.trim()` | Quita espacios al inicio y al final. `@NotBlank` ya garantiza que no son `null`, así que `trim()` es seguro |
| `save` | `INSERT`; tras él, `guardado.getId()` ya tiene el valor |
| `log.info("Contacto {} recibido", id)` | Registra **solo el id**, no el correo ni el comentario (datos personales fuera del log, [referencia 12](12-logs.md)) |
| `listar` | `findAll(pageable)` → `PageResponse` (misma forma que blogs e imágenes) |
| `buscarPorId` | `RecursoNoEncontradoException("Contacto", id)` → **404** |
| `readOnly = true` | Le avisa a la base que solo se lee |

Se mapea **dentro** de la transacción, como en el resto de los services.

## 9. `ContactoController.java`

```java
@RestController
@RequestMapping("/api/contactos")
@Tag(name = "Contacto", ...)
public class ContactoController {

    @PostMapping
    @SecurityRequirements
    public ResponseEntity<ContactoResponse> crear(@Valid @RequestBody ContactoRequest request) { ... }

    @GetMapping
    public PageResponse<ContactoResponse> listar(@ParameterObject Pageable pageable) { ... }

    @GetMapping("/{id}")
    public ContactoResponse buscarPorId(@PathVariable Long id) { ... }
}
```

| Pieza | Qué hace |
| --- | --- |
| `@Valid` | Ejecuta las validaciones del DTO; si falla, 400 antes de entrar al método |
| `ResponseEntity.created(ubicacion)` | **201** con `Location: /api/contactos/{id}` |
| `@SecurityRequirements` (vacío) | Quita el candado de Swagger **solo** en el `POST` público |
| `@ParameterObject` | Imprescindible en todo `Pageable` ([regla de oro 14](../../README.md#reglas-de-oro)): Swagger muestra `page`, `size` y `sort` |
| `@PathVariable Long id` | `/api/contactos/abc` → 400 (lo maneja el handler de tipo incorrecto) |

Sin lógica de negocio: valida, delega y responde.

## 10. `SecurityConfig.java`: las dos reglas nuevas

```java
// Contacto: cualquier persona puede enviar; solo el administrador lee
.requestMatchers(HttpMethod.POST, "/api/contactos").permitAll()
.requestMatchers("/api/contactos/**").hasRole(ROL_ADMINISTRADOR)
```

Van **antes** de `anyRequest().authenticated()`, justo sobre la regla de `/api/usuarios/me`.

| Petición | Regla que coincide | Resultado |
| --- | --- | --- |
| `POST /api/contactos` | La primera (`permitAll`) | Entra cualquiera |
| `GET /api/contactos` | La segunda (`/**` también cubre la ruta base) | Solo administrador |
| `GET /api/contactos/7` | La segunda | Solo administrador |
| `PUT /api/contactos/7` o `DELETE` | La segunda | Solo administrador (y el endpoint no existe: 405) |

**El orden importa:** la primera regla que coincide gana. Si se invirtieran, el `POST` público caería en la regla de administrador y el formulario daría 401.

## 11. Las pruebas

| Archivo | Tests | Qué cubre |
| --- | --- | --- |
| `ContactoServiceTest` | 4 | `crear` quita espacios y devuelve el contacto; `listar`; `buscarPorId`; 404 si no existe |
| `ContactoControllerTest` | 10 | `POST` sin sesión → 201 con `Location`; 400 por correo inválido, comentario faltante y comentario de 501 caracteres; `GET` como administrador → 200; sin sesión → 401; como cliente y como vendedor → 403; `GET /{id}` → 200 y 404 |

Los dos usan el mismo estilo del proyecto: `MockitoExtension` para el service y `@WebMvcTest` con `@Import(SecurityConfig.class)` y `@MockitoBean` para el controller ([guía 15](../guias/15-pruebas.md)). El de controller prueba **la regla de seguridad real**, no una simulada.

```bash
./mvnw test -Dtest="ContactoServiceTest,ContactoControllerTest"
```

## 12. Cómo probarlo a mano

1. Levanta el proyecto (`./scripts/dev.sh`): Flyway aplica `V6` (en el log: `Migrating schema ... to version "6 - contacto"`).
2. En Swagger, grupo **Contacto**, `POST /api/contactos` → *Try it out* con:
   ```json
   { "nombre": "Ana", "correo": "ana@correo.cl", "comentario": "Hola, quiero más información" }
   ```
   Esperado: **201**, cuerpo con `id` y encabezado `Location`.
3. En la base: `SELECT * FROM contacto;` muestra la fila.
4. `GET /api/contactos` **sin** Authorize → **401**.
5. Inicia sesión como administrador, Authorize, y repite → **200** con la lista.
6. Prueba los errores: correo `no-es-correo` → 400 con `detalles.correo`; comentario vacío → 400.

## 13. Límites y mejoras futuras

| Tema | Detalle |
| --- | --- |
| **Spam** | Es una ruta pública sin límite: cualquiera puede enviar miles de mensajes. Antes de desplegar, agrega límite de peticiones por IP (junto con el del login, ya pendiente) o un captcha. El tope de 500 caracteres solo limita el tamaño de cada mensaje |
| **Fecha de envío** | Para saber cuándo llegó cada mensaje: `ALTER TABLE contacto ADD COLUMN fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);` en una `V7` (o en `V6` si aún no se aplicó), el atributo en la entidad y el campo en `ContactoResponse`. Es el mismo patrón de `V4` en `blog` |
| **Leído / respondido** | Una columna `leido BOOLEAN` y un `PUT /api/contactos/{id}/leido` solo para administrador |
| **Borrar** | `DELETE /api/contactos/{id}` para administrador (la tabla no tiene claves foráneas, se puede borrar de verdad) |
| **Lectura para el vendedor** | Una línea en `SecurityConfig`: `.requestMatchers(HttpMethod.GET, "/api/contactos/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")` **antes** de la regla de administrador |
| **Aviso por correo** | Los starters de correo ya están en el `pom.xml`; falta configurarlo y enviar el aviso desde el service |
| **Texto con HTML** | El comentario se guarda tal cual. La API devuelve JSON (no interpreta HTML), pero el frontend debe mostrarlo **como texto** (`{comentario}` en React, nunca `dangerouslySetInnerHTML`) |

## Para saber más

[Guía 19: formulario de contacto](../guias/19-formulario-de-contacto.md) · [Catálogo de endpoints](23-catalogo-de-endpoints.md) · [Receta para una entidad nueva](19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad) · [Seguridad con JWT y roles](21-seguridad-jwt-y-roles-explicada.md)
