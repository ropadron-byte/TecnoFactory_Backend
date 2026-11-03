# Cambios recientes

**Un solo archivo vivo.** Aquí está lo que falta por hacer (arriba) y lo que ya se hizo, sesión por sesión (abajo). No se crean copias numeradas: en cada sesión nueva se **agrega una entrada al principio del Historial** y se actualiza la lista de Pendiente.

Cómo mantenerlo:

1. Al terminar algo, márcalo `[x]` en Pendiente o muévelo a la entrada de la sesión.
2. Al empezar una sesión, agrega arriba del Historial un título nuevo (`### fecha: tema`) y describe lo que cambió, por qué y cómo comprobarlo.
3. Las decisiones que sigan abiertas viven en Pendiente hasta que se resuelvan.

El plan completo por fases está en [10-pendientes-y-futuro.md](../referencia/10-pendientes-y-futuro.md); este archivo es la lista corta de **lo próximo**.

---

## Pendiente

### Siguiente paso (en este orden)

- [x] **Confirmar un arranque completo** (`Found 4 JPA repository interfaces`, `Started ...`, Swagger y `/actuator/health`).
- [x] **`camelCase` en `Usuario`** (`runDv`, `fechaNacimiento`), **`unique = false` en `run_dv`** e **`Integer id`** en `Region` y `Comuna`: comprobado contra tu repositorio el 19 oct.
- [ ] **Quitar `unique = true`** de `nombre_region` (`Region`) y `nombre_comuna` (`Comuna`): siguen puestos ([13, 7.2](../referencia/13-entidades-explicadas.md#72-unique--true-en-nombres-de-region-y-comuna)).
- [x] **Paso 6: `UsuarioService`** y **paso 7: `AdministradorInicialRunner`**: el primer arranque creó al administrador.
- [x] **Administrador: clave fuera del log** (0 apariciones en los logs de archivo, 19 oct) y **rama sin variables** (`WARN` y la app arranca igual).
- [x] **Administrador: la fila en la base**: vista en MySQL Workbench (19 oct), existe.
- [ ] **Administrador: un segundo arranque** que no duplique: al reiniciar la app debe aparecer `Ya existe un administrador: no se crea otro` ([17, sección 8](../referencia/17-administrador-inicial-explicado.md#8-comandos-importantes)). Por hacer; basta mirar esa línea la próxima vez que reinicies.
- [x] **Paso 8.1: `UsuarioResponse` y `toResponse`**: compiló y se revisó el código generado.
- [x] **Paso 8.2: `UsuarioRequest`, `toEntity` y `UsuarioRequestTest`**: confirmado el 19 oct (los 8 tests pasan, dentro de la ejecución del 8.3).
- [x] **Paso 8.3: validar el dígito verificador** (módulo 11) con una validación personalizada de clase: **14 tests pasan** (19 oct). Ver [19](../referencia/19-validacion-run-service-y-controller-explicados.md).
- [x] **Paso 9.1: `UsuarioService`**: registrar (hash, rol `CLIENTE`, unicidad de correo y RUN, comuna existente) y buscar por id, mapeando **dentro** de la transacción: **5 tests pasan** y la app arranca (19 oct).
- [x] **Paso 9.2: `UsuarioController`** con `@Valid`, 201 con `Location`, más el handler de `MethodArgumentTypeMismatchException`: **8 tests pasan** y la app arranca (19 oct).
- [ ] **Limpieza del 9.1:** quitar los comentarios `// NUEVO` que quedaron en `UsuarioService`.
- [x] **Paso 9.3: probar en Swagger**: `POST /api/usuarios` devuelve 201 con comuna y región (20 oct). Es el **ejemplo para tu compañero**; falta ver todos los errores formateados desde Swagger con la seguridad nueva.
- [ ] **Entidades y repositories restantes**: `Categoria`, `Producto`, `ImagenUrl`, `Carrito`, `DetalleCarrito`, `Boleta`, `DetalleBoleta`, `Blog` (mapeo en [06](../referencia/06-base-de-datos.md#mapeo-a-entidades-java)). Cuidado con `TEXT` (`blog.contenido`) y con la clave compuesta de `detalle_carrito`.
- [ ] **Primera "rebanada" completa**: `Categoria` (DTOs, mapper, service, controller) probada en Swagger; después `Producto`.
- [x] **Probar el `GlobalExceptionHandler`** con un controller: los 8 tests de `UsuarioControllerTest` cubren 400 (validación, JSON roto, id mal escrito), 404, 405 y 409 (19 oct). **Sin cubrir todavía:** 500 y `DataIntegrityViolationException` (409 por el `UNIQUE` de la base).

### Configuración base antes de la segunda entidad

Lo que conviene dejar resuelto **una sola vez**, para que `Categoria`, `Producto` y las demás solo repitan la [receta de la guía 19](../referencia/19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad) sin configurar nada de camino.

- [x] **`spring.jpa.open-in-view: false`** (20 oct): el `application.yaml` del ZIP del 19 oct dice `true`, y los docs 04, 09 y 18 suponen `false`. Cambiarlo y volver a ejecutar los tests y la app: si algo lee una relación `LAZY` fuera del service, fallará ahora y no cuando haya 12 entidades.
- [x] **Handlers que faltan** (20 oct) en `GlobalExceptionHandler`: `HttpMediaTypeNotSupportedException` (415: un `POST` sin `Content-Type: application/json` hoy sale como 500) y `MissingServletRequestParameterException` (400: falta un `?parametro=`). Los dos caen hoy en el handler genérico (500).
- [x] **Convención de paginación** (20 oct): un `record PageResponse<T>` y un tamaño máximo de página, **antes** del primer endpoint que liste (`GET /api/categorias`, `GET /api/productos`). Después cuesta cambiar el contrato con el frontend.
- [x] **Zona horaria** (20 oct): `spring.jackson.time-zone` y `spring.jpa.properties.hibernate.jdbc.time_zone` en `America/Santiago` (o `UTC` y convertir en el frontend), antes de guardar la primera `Boleta` con fecha y hora.
- [x] **`OpenApiConfig`** (20 oct; Swagger ya estaba desactivado en `prod`): título de la API y esquema de seguridad `bearer` para que Swagger tenga el botón **Authorize** cuando llegue el JWT; y desactivar Swagger en `prod`.
- [x] **`CorsConfig`** hecha; **`WebConfig`** borrada por estar vacía (20 oct): CORS con el origen del frontend leído de una variable de entorno. Se necesita el día que el frontend llame a la API desde otro puerto.
- [x] **Login con JWT (fase 4) antes de los controllers de `Producto`, `Carrito` y `Boleta`**: código aplicado el 20 oct (ver la sección siguiente); falta confirmar los tests y el flujo en Swagger. `Categoria` ya nace con reglas por rol.
- [ ] **Un ejemplo de prueba de repository** con Testcontainers (ya está en el `pom.xml`), para consultas propias complejas (carrito, boleta). Una sola prueba sirve de plantilla.
- [ ] **Limpieza de las entidades** que se copiarán como modelo: los `unique = true` de `Region` y `Comuna`, imports sobrantes y `Optional<List<...>>` (ver Limpieza pendiente).
- [ ] **No hace falta ahora:** Redis y caché (`@Cacheable`), correo, subida de imágenes y el `Dockerfile` del backend. Los starters ya están en el `pom.xml`; se configuran el día que se usen.

### Seguridad: login con JWT y roles (fase 4)

Guía completa: [21-seguridad-jwt-y-roles-explicada.md](../referencia/21-seguridad-jwt-y-roles-explicada.md).

- [x] **Paso 1: `JwtService`** (`generarToken`, `esValido`, `obtenerCorreo`, `obtenerRol`) y `JwtServiceTest` (6): **confirmado por ti** (20 oct).
- [x] **Paso 2: `UserDetailsServiceImpl`** y su test (3): entregado y aplicado; seguiste con el paso siguiente.
- [x] **Paso 3: `JwtAuthFilter`** y su test (6): entregado y aplicado; seguiste con el paso siguiente.
- [ ] **Paso 4: `LoginRequest`, `AuthResponse`, `AuthService`, `AuthController`** (`POST /api/auth/login`), con `AuthServiceTest` (3) y `AuthControllerTest` (3): **aplicado en tu repo; falta confirmar** los tests.
- [ ] **Paso 5: `SecurityConfig` nueva** (reglas por ruta, filtro conectado, 401 y 403 en JSON con `ManejadorErroresSeguridad`, sin `httpBasic`), `UsuarioController` con `/me`, `UsuarioService.buscarPorCorreo` y dos handlers (`BadCredentialsException`, `AuthenticationException`): **aplicado en tu repo; falta confirmar**.
- [ ] **Confirmar los 56 tests** (`./mvnw test -Dtest="*Test"`), que la app arranca y que **desaparece** el aviso `Using generated security password` del log.
- [ ] **Probar el flujo en Swagger** ([21, sección 14](../referencia/21-seguridad-jwt-y-roles-explicada.md#14-cómo-probarlo-a-mano)): login del administrador, Authorize, `/me`, `/api/usuarios/1`, 401 sin token, 403 con un cliente.
- [ ] **Usuarios con roles:** `GET /api/usuarios` paginado (administrador y vendedor, solo lectura), `PUT /api/usuarios/me` (editar el propio perfil) y una ruta de administrador para **crear vendedores** (el rol nunca viene del cliente).
- [ ] **Reglas por rol de cada entidad nueva** (`Categoria`: administrador; `Producto` y órdenes: vendedor y administrador). Se agregan **antes** de `.anyRequest().authenticated()`.
- [ ] **Límite de intentos de login** y **HTTPS** antes de un despliegue real.
- [ ] **Confirmar la revisión del 3 nov** (ver la entrada del Historial): `./mvnw test` (los tests nuevos y los 56 anteriores), arranque con `./scripts/dev.sh` y la prueba de paginación en Swagger. **No se pudo compilar** en el entorno de revisión.
- [x] **`POST /api/imagenes/productos` no registraba la imagen en `imagen_url`**: resuelto el 3 nov (ver la entrada de las 03:10). Falta poder asignar imágenes a productos y categorías por id (sus controllers).
- [ ] **Mover `JwtServiceTest` y `UserDetailsServiceImplTest`** a `src/test/.../security/` (hoy están en `service/`; `JwtServiceTest` declara el paquete `security` y compila igual, pero la carpeta no coincide).

### Verificaciones pendientes

- [x] **Confirmar `V1` y `V2` en tu MySQL 8.4** (se probaron en MariaDB 10.11): **hecho**.
- [ ] **Confirmar el `DEFAULT TRUE` de `boleta.estado`** en tu `V1`: `SHOW COLUMNS FROM boleta LIKE 'estado';` debe mostrar `Default = 1`.
- [x] **Compilar el proyecto**: **hecho**. El log del 18 oct muestra `Compiling 20 source files` sin errores, incluidas las clases de `exception/`. Falta **probarlas** con un controller.
- [ ] **Confirmar que la sección 1.4 ampliada** (comprobadas y no comprobadas) quedó pegada en `11-excepciones-explicadas.md`: se entregó como un trozo aparte, no dentro del archivo.
- [x] **Comprobar que el `.env` nunca se subió a Git**: **hecho**.

### Decisiones abiertas

- [x] **Administrador inicial**: se decidió el **18 oct** crearlo por código al arrancar (opción B de [08](../referencia/08-seguridad.md#administrador-inicial)) y descartar la plantilla `V4`.
- [x] **Datos del administrador**: correo y clave vienen de `ADMIN_EMAIL` y `ADMIN_PASSWORD`; el resto (RUN `11111111-1`, nombre, apellidos, fecha, dirección, teléfono, comuna `13101`) son **valores de relleno fijos** en el runner. Se eligió por defecto al no haber otra indicación; se puede cambiar.
- [x] **401 frente a 403** (20 oct): resuelto. Sin token o con token inválido, **401** (`AuthenticationException`); con token pero sin el rol, **403** (`AccessDeniedException`). `ManejadorErroresSeguridad` los reenvía a `GlobalExceptionHandler`. Ver [21, sección 9](../referencia/21-seguridad-jwt-y-roles-explicada.md#9-manejadorerroresseguridadjava-y-los-dos-handlers-nuevos).
- [x] **Quién ve `GET /api/usuarios/{id}`** (20 oct): solo el administrador; el perfil propio es `/api/usuarios/me`.
- [ ] **Cómo se aplica el descuento** (0 a 100) sobre el `precio` y su copia en `detalle_boleta`.
- [ ] **Detalle por parámetro** en `HandlerMethodValidationException` (hoy solo dice "Parámetros inválidos").
- [ ] **Blogs ocultos en `GET /api/imagenes`**: el listado público muestra también imágenes y dueños de blogs ocultos. Decidir si se filtra ([24, decisiones abiertas](../referencia/24-decisiones-de-diseno.md#decisiones-aún-abiertas)).

### Limpieza pendiente

- [x] Reemplazar los manuales viejos (`COMO_EJECUTAR.md`, `MANUAL_TECNOFACTORY.md`, `TecnoFactory - Guía del proyecto.md`) por la carpeta `docs/`: **hecho**.
- [x] Mover `ExplicacionesCrudas.md` fuera del repositorio o a `docs/archivo/`: **hecho**.
- [ ] Si activas el log en archivo, agregar `logs/` y `*.log` a `.gitignore` (ver [12](../referencia/12-logs.md#8-guardar-el-log-en-un-archivo-opcional)).
- [ ] Actualizar el diagrama de `Diagrama/`: **pospuesto** (es de Oracle Data Modeler y sus tipos de dato difieren de los de MySQL; la fuente de verdad es `V1`).
- [x] No compartir el ZIP del proyecto con el `.env` ni `target/`: **tenido en cuenta**.
- [ ] Limpiar las entidades (siguen así el 19 oct): `import jakarta.validation.constraints.Min` sin usar y bloques comentados al final de `Region.java` y `Comuna.java`, y tres líneas comentadas en `RegionRepository.java`; `Ctrl+Alt+O` en IntelliJ quita los `import` sobrantes de las entidades y los repositories.
- [ ] Cambiar `Optional<List<...>>` por `List<...>` en `RegionRepository`, `ComunaRepository` y `UsuarioRepository` (funciona, pero sobra el `Optional`).
- [ ] Borrar `src/test/.../controller/UsuarioController.java` (copia sobrante del controller en la carpeta de tests).
- [ ] Borrar `docs/11-cambios-recientes.md` (**sigue en tu repositorio el 19 oct**): es una copia antigua de este archivo y el README indica que no se crean copias numeradas.
- [x] El `README.md` enlaza `02-docker-y-mysql.md` y `03-variables-y-claves.md`, que no venían en el ZIP revisado: **recuperados** y añadidos al paquete de documentación (18 oct).

---

## Historial

### 3 nov 2026 (03:10): subida de imágenes sin dueño

Pedido tuyo: `POST /api/imagenes/productos` solo guardaba el archivo (no creaba fila en `imagen_url`, así que no había `id` para asignarla), y faltaba un endpoint general.

- **Nuevo `POST /api/imagenes`** (`multipart/form-data`, campo `archivo`; ADMINISTRADOR o VENDEDOR): guarda el archivo en `uploads/general/`, crea la fila en `imagen_url` **sin dueño** y responde **201** con `{ id, url, duenos: [] }` y el encabezado `Location`.
- **Arreglado `POST /api/imagenes/productos`**: ahora hace lo mismo (carpeta `productos`) y también responde 201 con el `id`. Antes respondía 200 con solo `{ url }`; la `url` sigue ahí.
- **`ImagenService.subir(archivo, subcarpeta)`** (nuevo): si falla el INSERT, borra el archivo para no dejar huérfanos. `ImagenController` ya no usa `AlmacenamientoService` directamente.
- **Carpeta `general`** agregada a las permitidas en `AlmacenamientoService`.
- **Asignar por id** (ya existía): `POST /api/blogs/{id}/imagenes/{idImagen}` y `PUT /api/blogs/{id}/imagen-principal/{idImagen}`. Productos y categorías no tienen controller todavía, así que aún no se pueden asignar por API.
- **Tests:** `ImagenServiceTest` (+2), `ImagenControllerTest` (+4 y 2 reescritos al nuevo contrato). **No se compilaron ni ejecutaron.**
- **Se cierra** el pendiente «`POST /api/imagenes/productos` no registra la imagen».

Flujo para probar: `POST /api/imagenes` → copiar el `id` → `POST /api/blogs/{id}/imagenes/{idImagen}`; y `SELECT id_url, url FROM imagen_url;` debe mostrar la fila.

### 3 nov 2026: revisión del código (imágenes, errores, subida de archivos y secreto JWT)

Se revisó el ZIP `src` completo a petición tuya (disparador: Swagger enviaba mal el `Pageable` de `GET /api/imagenes`). Hallazgos aplicados:

| # | Qué | Archivos | Antes → ahora |
| --- | --- | --- | --- |
| 1 | **Paginación en Swagger**: `@ParameterObject` en el `Pageable` | `ImagenController`, `BlogController` | Un cuadro JSON que Swagger enviaba como `?pageable={...}` → tres campos `page`, `size` y `sort` |
| 2 | **`?sort=campoInexistente`** | `GlobalExceptionHandler` (`PropertyReferenceException` y la misma envuelta en `InvalidDataAccessApiUsageException`) | 500 → **400** «No se puede ordenar por 'x'» |
| 3 | **Subir sin el campo `archivo`** | `GlobalExceptionHandler` (`MissingServletRequestPartException`) | 500 → **400** |
| 4 | **Archivo de más de 5 MB** | `GlobalExceptionHandler` (`MaxUploadSizeExceededException`) | 500 → **413** |
| 5 | **Archivo sin `Content-Type`** | `AlmacenamientoService` | `NullPointerException` (`Map.of(...).get(null)`) → 400 |
| 6 | **Archivos que no son imagen** | `AlmacenamientoService` + nueva `ArchivoNoValidoException` | Se confiaba en el `Content-Type` del cliente y el error era 409 → se comprueban los primeros bytes (JPEG, PNG, WEBP) y responde **400** |
| 7 | **El autor veía 404 en su propio blog oculto** | `BlogAutorizacion.puedeVerBlogOculto`, usado por `BlogService.buscarPorId` e `ImagenService.buscarBlogVisible` | Solo el administrador lo veía → administrador **y su autor** |
| 8 | **Clave JWT pública por defecto** | `application.yaml` | `${JWT_SECRET:cambia-esta-clave...}` → `${JWT_SECRET}`: si falta, no arranca. `TecnoFactoryApplicationTests` define su propia clave |

**Tests agregados** (no se pudieron ejecutar, ver más abajo):

- `AlmacenamientoServiceTest` (nuevo, 10): PNG, JPEG y WEBP válidos; tipo no permitido; sin `Content-Type`; archivo vacío; contenido que no coincide (HTML como PNG, JPEG como PNG); que no se guarde nada si es inválido; subcarpeta no permitida; eliminar.
- `ImagenControllerTest` (+4): `sort` inválido → 400, sin `archivo` → 400, archivo inválido → 400, archivo grande → 413.
- `BlogServiceTest` (+2): el autor ve su blog oculto; otro vendedor no.

**Decisiones tomadas:**

- `ArchivoNoValidoException` **extiende** `ReglaNegocioException`, así el código y los tests que ya atrapan la clase madre siguen igual; solo el handler nuevo la distingue (400 en vez de 409).
- Solo se quitó el valor por defecto de `jwt.secret`. Los de la base (`root`/`root`, `localhost:3307`) siguen en `application.yaml`; `prod` ya no tiene ninguno y `local` pide `DB_PASSWORD`.
- **No se aplicó** (queda en Pendiente): límite de intentos de login, el filtrado de blogs ocultos en `GET /api/imagenes` (es una decisión tuya, ver [24](../referencia/24-decisiones-de-diseno.md#decisiones-aún-abiertas)) y el archivo huérfano de `POST /api/imagenes/productos`.

**Documentación tocada:** `guias/10`, `guias/18`, `guias/03`, `referencia/08`, `referencia/23`, `referencia/24`, `referencia/26`, `personal/03`, `personal/05`, este archivo y el `README.md` (estado y regla 14).

**Cómo comprobarlo:**

```bash
./mvnw test                       # los 16 tests nuevos y los anteriores
./scripts/dev.sh                  # la app debe arrancar con JWT_SECRET del .env
```

En Swagger: `GET /api/imagenes` debe mostrar `page`, `size` y `sort`; con `sort=string` responde 400. Subir un `.txt` renombrado a `.png` a `POST /api/imagenes/productos` responde 400.

**Qué no se pudo verificar:** el ZIP no traía `pom.xml` y Maven Central estaba bloqueado, así que **no se compiló ni se ejecutó ningún test**. Solo se probó la lógica de `AlmacenamientoService` con un programa aparte (PNG, JPEG y WEBP válidos, y seis rechazos, incluido el `Content-Type` nulo). Lo demás se revisó leyéndolo. Dos puntos a mirar si algo no compila: el import `org.springdoc.core.annotations.ParameterObject` (en springdoc 1.x es `org.springdoc.api.annotations.ParameterObject`) y el constructor de `PropertyReferenceException` en `ImagenControllerTest`. **Corrección (3 nov, 02:55):** al compilar, `PropertyReferenceException` no estaba en `org.springframework.data.mapping`; en Spring Data 4 (Spring Boot 4) vive en `org.springframework.data.core` (igual que `TypeInformation`). Ya corregido en `GlobalExceptionHandler` y `ImagenControllerTest`.

### 3 nov 2026: MySQL en el 3307, lanzador local y rotación del `.env`

- **Causa del fallo de arranque:** el contenedor se había creado con el mapeo viejo `3306:3306`; Docker no cambia los puertos publicados al reiniciar, solo al recrear. La app buscaba `localhost:3307` y recibía `Connection refused`. Se recreó con `docker compose up -d --force-recreate --wait db` y quedó en `0.0.0.0:3307->3306/tcp`.
- **Lanzador:** `scripts/dev.sh` y `scripts/dev.ps1` cargan el `.env`, esperan a que MySQL esté `healthy` y arrancan el backend con el perfil `local` apuntando al 3307.
- **Documentación:** todas las referencias locales pasaron de `localhost:3306` a `localhost:3307`; el 3306 se conserva solo para el puerto interno del contenedor (`db:3306`) y las advertencias de conflicto.
- **Seguridad:** `.env` salió de Git (`.gitignore` + `git rm --cached`) y se agregó `.env.example`. Se rotaron `DB_PASSWORD`, `MIGRATOR_PASSWORD`, `APP_DB_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD` (con `ALTER USER` y actualización del hash BCrypt del administrador), porque las claves viejas quedaron en el historial.
- **Comprobado:** `./scripts/dev.sh` arranca con `The following 1 profile is active: "local"`, `HikariPool-1 - Start completed` y `/actuator/health` con `"db":{"status":"UP"}`.

### 21 oct 2026: documentación ampliada

- **Nuevos en `referencia/`:** [22 diccionario de datos](../referencia/22-diccionario-de-datos.md), [23 catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md), [24 decisiones de diseño](../referencia/24-decisiones-de-diseno.md), [25 glosario](../referencia/25-glosario.md) y [26 configuración YAML explicada](../referencia/26-configuracion-yaml-explicada.md).
- **Nuevos manuales:** [guía 17 (React)](../guias/17-integracion-con-react.md) y [preguntas frecuentes](../personal/10-preguntas-frecuentes.md).
- **Correcciones:** la clave del registro es de **8 a 72** caracteres (no 100); en `local` el log a archivo ya está activo.
- **Hallazgo anotado (sin cambiar el código):** `prod` no define `CORS_ORIGINS`; al desplegar hay que fijarla con el dominio real ([26](../referencia/26-configuracion-yaml-explicada.md)).
- Los datos de estos documentos se sacaron leyendo `V1`, `V3`, los DTOs, `SecurityConfig` y los tres `application*.yaml`; **no se ejecutó nada** (Maven Central bloqueado).
### 21 oct 2026: documentación reorganizada en tres niveles

- **`docs/guias/`**: 16 guías rápidas, una por tema, con analogía, piezas, reglas y tabla «si algo falla».
- **`docs/personal/`**: manual por tareas (levantar, agregar una entidad, algo falla, comandos, probar la API, roles, desplegar, reglas y trampas).
- **`docs/referencia/`**: los 21 documentos técnicos de antes, **movidos sin cambiar su contenido** (solo se ajustaron los enlaces).
- **`docs/estado/`**: este archivo.
- Se eliminó la copia duplicada `docs/11-cambios-recientes.md`.
- Para aplicarlo: **borra la carpeta `docs/` antigua** y copia la nueva.
### 20 oct 2026 (noche): login con JWT, roles y documentación (pasos 1 a 5 de seguridad)

Se escribió la fase 4 en cinco pasos. Detalle, analogía y código línea por línea en [21-seguridad-jwt-y-roles-explicada.md](../referencia/21-seguridad-jwt-y-roles-explicada.md).

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| 1 | `JwtService` y `JwtServiceTest` (6) | Confirmado por ti |
| 2 | `UserDetailsServiceImpl` y su test (3) | Aplicado |
| 3 | `JwtAuthFilter` y su test (6) | Aplicado |
| 4 | Login: `LoginRequest`, `AuthResponse`, `AuthService`, `AuthController` y 6 tests | Aplicado; falta confirmar |
| 5 | `SecurityConfig` nueva, `ManejadorErroresSeguridad`, `/api/usuarios/me`, `buscarPorCorreo` y 2 handlers; `UsuarioControllerTest` pasa de 9 a 14 | Aplicado; falta confirmar |

**Reglas de acceso resultantes:**

| Ruta | Quién entra |
| --- | --- |
| Swagger, `/actuator/health`, `POST /api/auth/login`, `POST /api/usuarios` | Todos |
| `GET /api/usuarios/me` | Cualquiera con sesión |
| Resto de `/api/usuarios/**` | Solo `ADMINISTRADOR` |
| Todo lo demás | Cualquiera con token válido |

**Decisiones:**

| Decisión | Motivo |
| --- | --- |
| Correo inexistente, clave incorrecta y usuario desactivado dan el **mismo** 401 | Nadie puede averiguar qué correos están registrados |
| `GET /api/usuarios/{id}` solo para administrador; el perfil propio es `/me` | Un cliente no debe leer a otros cambiando el id |
| El filtro consulta la base en cada petición | Desactivar a un usuario o cambiarle el rol surte efecto al instante |
| `JwtAuthFilter` sin `@Component` | Evita que `@WebMvcTest` lo cargue y falle por no encontrar `JwtService` |
| `.anyRequest().authenticated()` al final | Una ruta olvidada queda protegida, no abierta |
| Los errores 401 y 403 salen por `GlobalExceptionHandler` | Mismo JSON (`ErrorResponse`) que el resto de errores |

**Revisión del ZIP (comparado archivo por archivo):** todo el código de los pasos 1 a 5 está presente, y la configuración base del 20 oct está bien aplicada (`open-in-view: false`, zona horaria con la ruta correcta, paginación, CORS, `WebConfig` borrada). Hallazgos menores:

- `UsuarioService` conserva siete comentarios `// NUEVO`.
- `JwtServiceTest` y `UserDetailsServiceImplTest` están en `src/test/.../service/`; deberían estar en `security/`.
- `docs/11-cambios-recientes.md` sigue en el repositorio.
- El ZIP incluye el `.env`; para desarrollo local no importa, pero no lo compartas ni reutilices sus claves en producción.
- Los `logs/` son del 19 oct: no sirven para confirmar el arranque con la seguridad nueva.

**Documentación:** nueva guía **21**. Actualizados: `README.md`, `01`, `03`, `08`, `09`, `10`, `11` (secciones 8.12 y 8.13), `15`, y este archivo. Se agregó una sección **«En palabras simples»** a `04`, `08`, `11`, `15`, `19`, `20` y `21`, **sin borrar** las explicaciones técnicas.

**Qué no se pudo verificar:**

- Los **56 tests**, el arranque y el flujo de login en Swagger: no hay salida posterior a los pasos 4 y 5.
- Que desaparezca el aviso `Using generated security password`.
- Que el código se ejecute: el entorno donde se escribió no podía compilar.

**Lo que viene:** confirmar los 56 tests y el login; luego `GET /api/usuarios` y `PUT /api/usuarios/me`, y la primera rebanada completa con `Categoria` (con sus reglas por rol).

### 20 oct 2026: configuración base (pasos A a F) y revisión de integridad

Se dejó resuelta la configuración que `Categoria`, `Producto` y las demás heredan. Detalle y explicación en [20-configuracion-base-explicada.md](../referencia/20-configuracion-base-explicada.md).

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| A | Handlers 415 y parámetro faltante | Test del 415 pasa; el de parámetro faltante queda para el primer listado con filtro |
| B | `PageResponse<T>` y tope de página (20 / 100) | `PageResponseTest` pasa |
| C | Zona horaria `America/Santiago` | **Estaba mal escrita** (ver abajo); corregida |
| D | `OpenApiConfig` | Swagger muestra "TecnoFactory API" y el botón Authorize |
| E | `CorsConfig`, `cors.allowed-origins` y `.cors(...)` en `SecurityConfig` | La app arranca |
| F | `WebConfig` borrada | — |
| — | `open-in-view: false` | Registro sigue devolviendo 201 con comuna y región |

**Comprobado por ti (20 oct):** **29 tests sin fallos**, la app arranca, Swagger carga y `POST /api/usuarios` devuelve 201.

**Hallazgos de la revisión del ZIP:**

- `application.yaml` tenía `spring.jpa.time_zone` y `Jackson: time_zone`: propiedades que no existen. Spring las ignora sin avisar, así que los tests pasaban y la zona horaria no estaba aplicada. Corregido a `spring.jpa.properties.hibernate.jdbc.time_zone` y `spring.jackson.time-zone`.
- Había una copia de `UsuarioController.java` en `src/test/.../controller/`. Hay que borrarla; la válida está en `src/main`.
- `cors.allowed-origins` solo trae `http://localhost:5173`; agregar el puerto 3000 si el frontend usa Create React App.

**Qué no se pudo verificar:** que la zona horaria surta efecto (se verá con la primera `Boleta`) y el handler de parámetro faltante (sin test).

### 19 oct 2026 (tarde): validación del RUN, service y controller (pasos 8.3, 9.1 y 9.2)

Se completó el **primer flujo de punta a punta** de `Usuario`: `POST /api/usuarios` y `GET /api/usuarios/{id}`.

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| 8.3 | `RunUtil` (módulo 11), `@RunValido` y `RunValidoValidator`; dos tests nuevos en `UsuarioRequestTest` | **Pasa**: 14 tests (10 de `UsuarioRequestTest` + 4 de `RunUtilTest`). Confirma también el 8.2 |
| 9.1 | `UsuarioService`: `registrar` y `buscarPorId`, con `UsuarioServiceTest` (Mockito, 5 tests) | **Pasa** y la app arranca con el mapper inyectado |
| 9.2 | `UsuarioController` (`@Valid`, 201 con `Location`) y `UsuarioControllerTest` (`@WebMvcTest`, 8 tests) | **Pasa** y la app arranca |
| — | `GlobalExceptionHandler`: handler nuevo de `MethodArgumentTypeMismatchException` (400) | Sin él, `GET /api/usuarios/abc` salía como 500. Cubierto por un test |

**Qué se comprobó con las pruebas:** 27 tests sin base de datos (14 + 5 + 8). El controller es la **primera vez** que se prueba el `GlobalExceptionHandler`: 400 por campo (`detalles.runDv`), 400 por JSON roto, 404, 405 y 409.

**Decisiones:**

| Decisión | Motivo |
| --- | --- |
| La validación del dígito verificador es una **validación de clase**, no de campo | Necesita dos campos a la vez (`run` y `runDv`); el error se asocia a `runDv` |
| El service **normaliza** la `k` a `K` antes de guardar | El Request acepta ambas |
| El rol `CLIENTE` lo fija el service, nunca el Request | Evita *mass assignment* |
| Correo o RUN repetido = 409; comuna o rol inexistente = 404 | El handler no tiene un 422/400 para reglas de negocio |
| `toResponse` se ejecuta **dentro** de la transacción del service | La región es `LAZY` |
| El `UNIQUE` de la base queda como segunda barrera contra duplicados simultáneos | Dos peticiones a la vez pueden pasar `existsByCorreo` |
| El controller **no tiene lógica**: valida, delega y responde | Facilita copiarlo para otras entidades |

**Hallazgo al revisar el ZIP:** `application.yaml` tiene `open-in-view: true`, pero los docs 04, 09 y 18 dicen `false`. No rompe nada porque el service mapea dentro de la transacción, pero esconde errores de carga perezosa. Quedó en la sección **Configuración base antes de la segunda entidad**.

**Documentación:** nueva [19-validacion-run-service-y-controller-explicados.md](../referencia/19-validacion-run-service-y-controller-explicados.md) (las tres piezas del RUN, el service, el controller, las tres pruebas, comandos, errores y la **receta para la próxima entidad**). Actualizados: `README.md`, `04`, `10`, `11` (sección 8.11 nueva), `18` y este archivo.

**Qué no se pudo verificar:**

- Registro, 409, 404 y 400 contra **MySQL real desde Swagger** (paso 9.3), y la fila del nuevo usuario con su hash (`$2a$10$`, largo 60).
- El segundo arranque del administrador (`Ya existe un administrador`).
- El `UNIQUE` de la base como segunda barrera (409 por `DataIntegrityViolationException`) y el 500 genérico.
- Que el código de los docs coincida **línea por línea** con tus archivos: se tomó del que se entregó en cada paso y de los tests que ya pasan. El `UsuarioController`, su test y el handler nuevo no se ejecutaron donde se escribieron (sin acceso a Maven); los ejecutaste tú.

### 19 oct 2026: documentación al día (pasos 8.1 y 8.2)

Se revisó el ZIP nuevo del proyecto contra la documentación: código, `pom.xml`, `.gitignore`, `docker-compose.yml`, tests y los logs de `logs/`.

**Qué coincide:** `DatosAdministrador`, `UsuarioService`, `AdministradorInicialRunner`, `UsuarioResponse`, `UsuarioRequest`, `UsuarioMapper` y `UsuarioRequestTest` (8 tests) son **idénticos** a lo que describen las guías 17 y 18. El `pom.xml` y el `docker-compose.yml` no cambiaron.

**Qué cambió en las entidades desde la última revisión:**

| Observación de la [guía 13, sección 7](../referencia/13-entidades-explicadas.md#7-observaciones-sobre-el-código-actual) | Estado |
| --- | --- |
| `run_dv` con `unique = true` | ✅ Ahora `unique = false` |
| `run_dv` y `fecha_nacimiento` con guion bajo | ✅ `runDv` y `fechaNacimiento` |
| `int id` en `Region` y `Comuna` | ✅ `Integer id` |
| `unique = true` en `nombre_region` y `nombre_comuna` | ⏳ Siguen puestos |
| Imports sobrantes y SQL comentado | ⏳ Siguen en `Region`, `Comuna` y `RegionRepository` |

**Qué mostraron los logs** (`logs/tecnofactory.log` y el rotado del 18 oct):

| Comprobación | Resultado |
| --- | --- |
| Tu `ADMIN_PASSWORD` en los logs de archivo | **0 apariciones** (verificado sin mostrar la clave) |
| Rama "sin variables" | **Verificada**: dos arranques (23:30 y 23:31 del 18 oct) mostraron `ADMIN_EMAIL o ADMIN_PASSWORD sin definir` y la app arrancó |
| Segundo arranque sin duplicar | **Sin evidencia**: el proceso de las 23:33 siguió corriendo hasta las 11:58 del 19 oct y no hay ningún arranque posterior |
| `insert into usuario` en el log de archivo | No aparece: `show-sql` escribe en la **consola**, no en el archivo (coherente con que la clave tampoco aparezca) |
| Fila del administrador en la base | **No verificable** desde los logs |

**Documentación actualizada:** `README.md` (fila 18, estado, regla 10), `08`, `09` (sección de DTOs, mapper y validaciones), `10` (pasos 8.1 a 9.3), `13` (código real y estado de la sección 7), `17` y `18` (estado verificado) y este archivo.

**Nota de seguridad:** el ZIP incluía el `.env` con las claves reales (`DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`...). Tu propia regla ([03](../referencia/03-variables-y-claves.md#reglas-de-seguridad-para-los-secretos)) dice que las claves compartidas fuera de tu equipo **ya no son secretas**. Para desarrollo local no importa, pero **no las reutilices en producción**; allí se generan otras. Conviene también quitar `.env` y `target/` de los ZIP que compartas.

**Qué no se pudo verificar:** que `./mvnw compile` y `UsuarioRequestTest` pasen con el código actual (no se ejecutó el proyecto), la fila del administrador en la base y el segundo arranque.

### 18 oct 2026 (noche): administrador inicial al arrancar (pasos 5b, 6 y 7)

| Paso | Qué se hizo | Resultado |
| --- | --- | --- |
| 5b | Entidades corregidas (`runDv`, `fechaNacimiento`) y arranque completo | Confirmado: Swagger carga y `/actuator/health` responde |
| 6 | `DatosAdministrador` (record) y `UsuarioService` con `crearAdministradorSiNoExiste` | Compila y arranca |
| 7 | `AdministradorInicialRunner` (`ApplicationRunner`) y variables `ADMIN_EMAIL` / `ADMIN_PASSWORD` en el `.env` | **El primer arranque creó al administrador** (log: `Administrador inicial creado con el correo ...`) |

**Qué mostró el log del primer arranque** (detalle en [17, sección 2](../referencia/17-administrador-inicial-explicado.md#2-orden-de-ejecución)):

- El runner se ejecuta **después** de `Started TecnoFactoryApplication` y con Tomcat ya arriba.
- Cuatro consultas, en este orden: `existsByTipoUsuarioNombre` (con `JOIN` y `limit`), búsqueda del rol, búsqueda de la comuna e `insert into usuario`.
- La consulta de la comuna **no** hace `JOIN` con `region`: `LAZY` funciona.
- El `insert` muestra `?` en vez de valores: la clave no aparece en el log por esa vía.
- Sigue saliendo `Using generated security password` (inofensiva, ver [15](../referencia/15-seguridad-codigo-explicado.md#36-dos-detalles-que-sorprenden)).

**Decisión tomada por defecto** (no hubo respuesta a la consulta): correo y clave desde variables de entorno; RUN, nombre, apellidos, fecha, dirección, teléfono y comuna como valores de relleno fijos en el runner.

**Documentación:** nueva [17-administrador-inicial-explicado.md](../referencia/17-administrador-inicial-explicado.md) (código línea por línea, orden de ejecución, glosario, comandos y casos de error). Actualizados: `README.md`, `03`, `08`, `10`, `13`, `14`, `15` y este archivo.

**Qué no se pudo verificar:**

- El contenido de la fila en la base, el segundo arranque sin duplicar y la ausencia de la clave en `logs/tecnofactory.log`: son los comandos de [17, sección 8](../referencia/17-administrador-inicial-explicado.md#8-comandos-importantes); el log pegado solo cubre el primer arranque.
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
| [13-entidades-explicadas.md](../referencia/13-entidades-explicadas.md) | Cada anotación (`@Entity`, `@ManyToOne`, `@JdbcTypeCode`...), cada entidad línea por línea y tres observaciones sobre el código actual |
| [14-repositories-explicados.md](../referencia/14-repositories-explicados.md) | Cómo se lee el nombre de un método, qué retorno elegir, cada repository línea por línea |
| [15-seguridad-codigo-explicado.md](../referencia/15-seguridad-codigo-explicado.md) | `SecurityConfig` línea por línea, BCrypt, qué cambia al llegar el login |
| [16-leer-errores-de-spring.md](../referencia/16-leer-errores-de-spring.md) | Método de 4 pasos para leer un error de arranque, con los tres casos reales |

**Documentación actualizada:** `03` (variables `ADMIN_EMAIL` y `ADMIN_PASSWORD` planificadas), `README.md` (índice, estado y reglas de oro), `06` (mapeo de `CHAR(1)`, estado de las entidades), `08` (`SecurityConfig`, administrador por código), `09` (sección de entidades y repositories), `10` (fases, paso a paso del administrador).

**Observaciones sobre el código (no impiden arrancar):** `unique = true` en `run_dv`, `nombre_region` y `nombre_comuna` que `V1` no declara; `run_dv` y `fecha_nacimiento` con guion bajo en Java. Detalle y corrección en [13, sección 7](../referencia/13-entidades-explicadas.md#7-observaciones-sobre-el-código-actual).

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

Decisión de diseño: el administrador se crea con una migración que guarda el **hash** (nunca la clave). La alternativa de crearlo al arrancar desde variables de entorno queda para antes de producción, porque necesita las entidades y el `PasswordEncoder`. Detalle y pasos en [08](../referencia/08-seguridad.md#administrador-inicial).

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
| Manejo de errores de la API, incluidos los errores propios de Spring (400, 404, 405, 403) | `exception/` (4 archivos) | Escrito y revisado por lectura; **sin compilar ni probar**. Explicado en [11](../referencia/11-excepciones-explicadas.md) |
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

**Decisión que se mantiene:** `detalle_boleta` → `boleta` queda en `RESTRICT` (no `CASCADE` como decía la documentación antigua), porque las boletas son registros de compra y no deben borrarse nunca. Tabla completa de reglas en [06](../referencia/06-base-de-datos.md#reglas-de-borrado-on-delete).

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

Más casos en [09](../referencia/09-errores-comunes.md#java-e-intellij).

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
