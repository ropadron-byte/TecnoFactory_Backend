# Documentación de TecnoFactory

La documentación tiene **tres lugares**, según lo que necesites:

| Necesito... | Carpeta |
| --- | --- |
| **Hacer algo** (levantar, agregar una entidad, arreglar un fallo) | [`docs/personal/`](docs/personal/00-empieza-aqui.md): manual por tareas |
| **Entender un tema rápido**, con analogías | [`docs/guias/`](docs/guias/01-el-proyecto-en-una-pagina.md): 16 guías cortas |
| **Todo el detalle**, línea por línea | [`docs/referencia/`](docs/referencia/): 21 documentos técnicos |
| **Saber qué cambió y qué falta** | [`docs/estado/cambios-recientes.md`](docs/estado/cambios-recientes.md) |

**Primera vez:** empieza por [Empieza aquí](docs/personal/00-empieza-aqui.md).

## Manual personal (por tareas)

| # | Página |
| --- | --- |
| 00 | [Empieza aquí](docs/personal/00-empieza-aqui.md) |
| 01 | [Levantar el proyecto](docs/personal/01-levantar-el-proyecto.md) |
| 02 | [Agregar una entidad](docs/personal/02-agregar-una-entidad.md) |
| 03 | [Algo falla](docs/personal/03-algo-falla.md) |
| 04 | [Comandos](docs/personal/04-comandos.md) |
| 05 | [Probar la API](docs/personal/05-probar-la-api.md) |
| 06 | [Cómo viaja una petición](docs/personal/06-como-viaja-una-peticion.md) |
| 07 | [Roles y permisos](docs/personal/07-roles-y-permisos.md) |
| 08 | [Antes de desplegar](docs/personal/08-antes-de-desplegar.md) |
| 09 | [Reglas y trampas](docs/personal/09-reglas-y-trampas.md) |

## Guías rápidas (un tema, una página)

| # | Guía |
| --- | --- |
| 01 | [El proyecto en una página](docs/guias/01-el-proyecto-en-una-pagina.md) |
| 02 | [Docker y MySQL](docs/guias/02-docker-y-mysql.md) |
| 03 | [Variables y claves](docs/guias/03-variables-y-claves.md) |
| 04 | [Perfiles y configuración base](docs/guias/04-perfiles-y-configuracion-base.md) |
| 05 | [Base de datos y Flyway](docs/guias/05-base-de-datos-y-flyway.md) |
| 06 | [Ejecutar en local y prod](docs/guias/06-ejecutar-local-y-prod.md) |
| 07 | [Entidades y repositories](docs/guias/07-entidades-y-repositories.md) |
| 08 | [DTO, mapper y validaciones](docs/guias/08-dto-mapper-y-validaciones.md) |
| 09 | [Service y controller](docs/guias/09-service-y-controller.md) |
| 10 | [Errores de la API](docs/guias/10-errores-de-la-api.md) |
| 11 | [Seguridad y login](docs/guias/11-seguridad-y-login.md) |
| 12 | [Administrador inicial](docs/guias/12-administrador-inicial.md) |
| 13 | [Logs](docs/guias/13-logs.md) |
| 14 | [Leer errores y problemas](docs/guias/14-leer-errores-y-problemas.md) |
| 15 | [Pruebas](docs/guias/15-pruebas.md) |
| 16 | [Hoja de ruta](docs/guias/16-hoja-de-ruta.md) |

## Referencia técnica

| # | Archivo | Qué responde |
| --- | --- | --- |
| 01 | [Arquitectura y stack](docs/referencia/01-arquitectura-y-stack.md) | Qué es el proyecto, con qué está hecho, cómo se organiza el código y qué es un JAR |
| 02 | [Docker y MySQL](docs/referencia/02-docker-y-mysql.md) | Cómo funciona el compose, los tres usuarios de MySQL, el script de inicio y los volúmenes |
| 03 | [Variables de entorno y claves](docs/referencia/03-variables-y-claves.md) | De dónde sale cada variable, quién la lee y cómo manejar los secretos |
| 04 | [Perfiles de Spring](docs/referencia/04-perfiles-spring.md) | Qué cambia entre `local` y `prod`, archivo por archivo |
| 05 | [Flyway y migraciones](docs/referencia/05-flyway-y-migraciones.md) | Cómo se versiona la base y qué reglas respetar |
| 06 | [Base de datos](docs/referencia/06-base-de-datos.md) | Esquema real (`V1`), datos iniciales (`V2`), mapeo a entidades y convenciones |
| 07 | [Ejecución: local y prod](docs/referencia/07-ejecucion-local-y-prod.md) | Pasos exactos para levantar local, probar prod y desplegar prod real |
| 08 | [Seguridad](docs/referencia/08-seguridad.md) | Modelo de seguridad actual, lo que falta y la lista de verificación |
| 09 | [Errores comunes](docs/referencia/09-errores-comunes.md) | Síntoma, causa y solución de los problemas ya encontrados |
| 10 | [Pendientes y futuro](docs/referencia/10-pendientes-y-futuro.md) | Qué falta construir, en qué orden y qué cambia en la configuración al hacerlo |
| 11 | [Excepciones explicadas](docs/referencia/11-excepciones-explicadas.md) | Guía línea por línea de `exception/`: qué hace cada archivo, cada librería y cada error |
| 12 | [Logs](docs/referencia/12-logs.md) | Qué es el log, qué hace Lombok (`@Slf4j`), dónde verlo, niveles, cómo escribirlo y guardarlo |
| 13 | [Entidades explicadas](docs/referencia/13-entidades-explicadas.md) | Guía línea por línea de `entity/`: cada anotación (`@Entity`, `@ManyToOne`, `@JdbcTypeCode`...), cada archivo y las observaciones sobre el código actual |
| 14 | [Repositories explicados](docs/referencia/14-repositories-explicados.md) | Guía línea por línea de `repository/`: cómo Spring Data convierte el nombre de un método en SQL y qué tipo de retorno elegir |
| 15 | [Seguridad: código explicado](docs/referencia/15-seguridad-codigo-explicado.md) | `SecurityConfig` línea por línea, qué es BCrypt y qué cambia al llegar el login |
| 16 | [Leer errores de Spring](docs/referencia/16-leer-errores-de-spring.md) | Método de 4 pasos para encontrar un error de arranque, con los casos reales resueltos |
| 17 | [Administrador inicial explicado](docs/referencia/17-administrador-inicial-explicado.md) | `DatosAdministrador`, `UsuarioService` y el `ApplicationRunner` línea por línea, orden de ejecución del arranque, comandos importantes |
| 18 | [DTOs, mapper y validaciones explicados](docs/referencia/18-dto-mapper-y-validaciones-explicados.md) | `UsuarioResponse`, `UsuarioRequest` y `UsuarioMapper` línea por línea, validaciones, cuándo se ejecuta cada pieza y el test de validaciones |
| 19 | [Validación del RUN, service, controller y pruebas](docs/referencia/19-validacion-run-service-y-controller-explicados.md) | `RunUtil` y `@RunValido`, `UsuarioService` (registrar y buscar), `UsuarioController`, las tres pruebas y la **receta para la próxima entidad** |
| 20 | [Configuración base explicada](docs/referencia/20-configuracion-base-explicada.md) | Handlers 415 y 400, paginación (`PageResponse`), zona horaria, `OpenApiConfig` y CORS, y por qué se hizo cada uno antes de la segunda entidad |
| 21 | [Seguridad con JWT y roles explicada](docs/referencia/21-seguridad-jwt-y-roles-explicada.md) | Login, token JWT, filtro, reglas por ruta, 401 y 403 en JSON; explicado primero con una analogía y después línea por línea |


**Archivo vivo:** [Cambios recientes](docs/estado/cambios-recientes.md) reúne lo que falta y lo que ya se hizo, sesión por sesión.

## Estado del proyecto (oct 2026)

| Pieza | Estado |
| --- | --- |
| MySQL 8.4 en Docker con volumen y tres usuarios | Funcionando |
| Flyway aplicando `V1` con `tecnofactory_migrator` | Probado en perfil `prod` (con el `V1` original) |
| La app arrancando como `tecnofactory_app` (solo DML) | Probado en perfil `prod` |
| `V1` con las 13 tablas y 15 claves foráneas | **Aplicada en MySQL 8.4** (Flyway, 18 oct) |
| `V2` con 3 roles, 16 regiones y 346 comunas | **Aplicada en MySQL 8.4** |
| `V3` con categorías base | **Aplicada en MySQL 8.4** (Flyway quedó en la versión 3) |
| `application-local.yaml` | Corregido |
| Manejo de errores (`exception/`) | **Probado** con el primer controller: 400, 404, 405, 409 y 415. Faltan 500 y 409 por el `UNIQUE` de la base. Ver [11](docs/referencia/11-excepciones-explicadas.md) |
| Entidades | **4 de 12** (`TipoUsuario`, `Region`, `Comuna`, `Usuario`); **validan** contra MySQL 8.4. Ver [13](docs/referencia/13-entidades-explicadas.md) |
| Repositories | **4** (uno por entidad); errores de nombres corregidos. Arranque completo **confirmado** (log del 18 oct). Ver [14](docs/referencia/14-repositories-explicados.md) |
| `SecurityConfig` y `PasswordEncoder` (BCrypt) | **Reescrito el 20 oct**: ya **no** hay `permitAll` general. Públicas: Swagger, `health`, login y registro; `/api/usuarios/me` con sesión; el resto de `/api/usuarios/**` solo administrador; todo lo demás exige token. Ver [21](docs/referencia/21-seguridad-jwt-y-roles-explicada.md) (la [15](docs/referencia/15-seguridad-codigo-explicado.md) explica la primera versión) |
| JWT (`JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl`), login (`AuthService`, `AuthController`) y errores 401/403 en JSON (`ManejadorErroresSeguridad`) | **Escritos y aplicados** (20 oct); **56 tests** escritos. Falta confirmar los 56 tests, el arranque y el flujo de login en Swagger. Ver [21](docs/referencia/21-seguridad-jwt-y-roles-explicada.md) |
| Administrador inicial | **Se crea por código al arrancar** (no con `V4`): verificado en el log del 18 oct y en la base (fila vista en Workbench). Falta el segundo arranque; la clave fuera del log ya está verificada. Ver [17](docs/referencia/17-administrador-inicial-explicado.md) |
| Services | `UsuarioService` (administrador inicial, `registrar`, `buscarPorId`, `buscarPorCorreo`) y `AuthService` (login). Ver [19](docs/referencia/19-validacion-run-service-y-controller-explicados.md) y [21](docs/referencia/21-seguridad-jwt-y-roles-explicada.md) |
| DTOs y mapper de `Usuario` | `UsuarioResponse`, `UsuarioRequest` y `UsuarioMapper` **confirmados** (10 tests de `UsuarioRequestTest`), con validación del dígito verificador (módulo 11, `RunUtilTest`). Ver [18](docs/referencia/18-dto-mapper-y-validaciones-explicados.md) y [19](docs/referencia/19-validacion-run-service-y-controller-explicados.md) |
| Controllers | `UsuarioController` (`POST /api/usuarios`, `GET /api/usuarios/me`, `GET /api/usuarios/{id}`) y `AuthController` (`POST /api/auth/login`). El registro se probó en Swagger contra MySQL (201, 20 oct); el login, pendiente de confirmar |
| Configuración base | `open-in-view: false`, handlers 415 y 400, `PageResponse`, zona horaria, `OpenApiConfig` y CORS: **29 tests** y arranque confirmados (20 oct). Ver [20](docs/referencia/20-configuracion-base-explicada.md) |
| `Dockerfile` y servicio `backend` del compose | Vacío / comentado |

## Reglas de oro

1. **Spring no lee el `.env`.** Lo lee Docker Compose; a Spring hay que pasarle las variables al arrancar.
2. **Una migración aplicada no se edita.** Los cambios son archivos nuevos (`V4__...` en adelante). Mientras `V1` y `V2` solo existan en tu máquina, puedes recrearlas con `docker compose down -v`.
3. **Los scripts de `docker/mysql-init` y las claves `MYSQL_*` solo se aplican al crear el volumen.**
4. **`docker compose down -v` borra los datos.** Nunca en producción.
5. **La app en producción usa `tecnofactory_app`**, nunca `root`.
6. **Una clase `public` por archivo Java**, con el mismo nombre que la clase.
7. **Los métodos de repository se escriben con los atributos de Java, no con las columnas** (`findByNombre`, no `findByNombreComuna`). Ver [14](docs/referencia/14-repositories-explicados.md#34-los-nombres-se-escriben-contra-los-atributos-java-no-las-columnas).
8. **La entidad refleja a `V1`, no al revés.** `unique`, `nullable` y `length` en `@Column` no crean ni verifican nada: manda el esquema. Ver [13](docs/referencia/13-entidades-explicadas.md#13-qué-comprueba-ddl-auto-validate).
9. **Lo que no está en una regla de `SecurityConfig` exige token** (`anyRequest().authenticated()`). Una ruta pública nueva se agrega **a propósito**, antes de esa línea. Ver [21](docs/referencia/21-seguridad-jwt-y-roles-explicada.md#10-securityconfigjava-las-reglas-de-acceso).
10. **El DTO de salida nunca lleva la contraseña; el de entrada nunca lleva rol, `id` ni `activo`.** El mapper copia datos; los hashes y las decisiones son del service. Ver [18](docs/referencia/18-dto-mapper-y-validaciones-explicados.md#12-mass-assignment-por-qué-el-request-no-tiene-rol).
11. **El controller no tiene lógica de negocio.** Valida (`@Valid`), delega en el service y responde; el service es `@Transactional` y devuelve DTOs. Los errores se lanzan como `RecursoNoEncontradoException` o `ReglaNegocioException` y los formatea el handler. Ver [19](docs/referencia/19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad).
12. **El token se firma con `JWT_SECRET` (mínimo 32 caracteres) y no se comparte.** Los fallos de login (correo inexistente, clave incorrecta, usuario desactivado) devuelven **el mismo** 401. El rol que decide es el de la base de datos; el del token es solo informativo para el frontend. Ver [21](docs/referencia/21-seguridad-jwt-y-roles-explicada.md#17-decisiones-y-límites).
13. **Ocultar un botón en el frontend no protege nada**: la regla real está en `SecurityConfig`.
