# Documentación de TecnoFactory

Índice de la documentación técnica del backend. Cada archivo cubre **una categoría**; léelos en el orden que necesites.

| # | Archivo | Qué responde |
| --- | --- | --- |
| 01 | [Arquitectura y stack](docs/01-arquitectura-y-stack.md) | Qué es el proyecto, con qué está hecho, cómo se organiza el código y qué es un JAR |
| 02 | [Docker y MySQL](docs/02-docker-y-mysql.md) | Cómo funciona el compose, los tres usuarios de MySQL, el script de inicio y los volúmenes |
| 03 | [Variables de entorno y claves](docs/03-variables-y-claves.md) | De dónde sale cada variable, quién la lee y cómo manejar los secretos |
| 04 | [Perfiles de Spring](docs/04-perfiles-spring.md) | Qué cambia entre `local` y `prod`, archivo por archivo |
| 05 | [Flyway y migraciones](docs/05-flyway-y-migraciones.md) | Cómo se versiona la base y qué reglas respetar |
| 06 | [Base de datos](docs/06-base-de-datos.md) | Esquema real (`V1`), datos iniciales (`V2`), mapeo a entidades y convenciones |
| 07 | [Ejecución: local y prod](docs/07-ejecucion-local-y-prod.md) | Pasos exactos para levantar local, probar prod y desplegar prod real |
| 08 | [Seguridad](docs/08-seguridad.md) | Modelo de seguridad actual, lo que falta y la lista de verificación |
| 09 | [Errores comunes](docs/09-errores-comunes.md) | Síntoma, causa y solución de los problemas ya encontrados |
| 10 | [Pendientes y futuro](docs/10-pendientes-y-futuro.md) | Qué falta construir, en qué orden y qué cambia en la configuración al hacerlo |
| 11 | [Excepciones explicadas](docs/11-excepciones-explicadas.md) | Guía línea por línea de `exception/`: qué hace cada archivo, cada librería y cada error |
| 12 | [Logs](docs/12-logs.md) | Qué es el log, qué hace Lombok (`@Slf4j`), dónde verlo, niveles, cómo escribirlo y guardarlo |
| 13 | [Entidades explicadas](docs/13-entidades-explicadas.md) | Guía línea por línea de `entity/`: cada anotación (`@Entity`, `@ManyToOne`, `@JdbcTypeCode`...), cada archivo y las observaciones sobre el código actual |
| 14 | [Repositories explicados](docs/14-repositories-explicados.md) | Guía línea por línea de `repository/`: cómo Spring Data convierte el nombre de un método en SQL y qué tipo de retorno elegir |
| 15 | [Seguridad: código explicado](docs/15-seguridad-codigo-explicado.md) | `SecurityConfig` línea por línea, qué es BCrypt y qué cambia al llegar el login |
| 16 | [Leer errores de Spring](docs/16-leer-errores-de-spring.md) | Método de 4 pasos para encontrar un error de arranque, con los casos reales resueltos |
| 17 | [Administrador inicial explicado](docs/17-administrador-inicial-explicado.md) | `DatosAdministrador`, `UsuarioService` y el `ApplicationRunner` línea por línea, orden de ejecución del arranque, comandos importantes |
| 18 | [DTOs, mapper y validaciones explicados](docs/18-dto-mapper-y-validaciones-explicados.md) | `UsuarioResponse`, `UsuarioRequest` y `UsuarioMapper` línea por línea, validaciones, cuándo se ejecuta cada pieza y el test de validaciones |
| 19 | [Validación del RUN, service, controller y pruebas](docs/19-validacion-run-service-y-controller-explicados.md) | `RunUtil` y `@RunValido`, `UsuarioService` (registrar y buscar), `UsuarioController`, las tres pruebas y la **receta para la próxima entidad** |
| 20 | [Configuración base explicada](docs/20-configuracion-base-explicada.md) | Handlers 415 y 400, paginación (`PageResponse`), zona horaria, `OpenApiConfig` y CORS, y por qué se hizo cada uno antes de la segunda entidad |

**Archivo vivo (sin número):** [Cambios recientes](docs/cambios-recientes.md) reúne en un solo lugar **lo que falta por hacer** y **lo que ya se hizo**, sesión por sesión. No se crean copias numeradas: se agrega una entrada nueva al historial.

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
| Manejo de errores (`exception/`) | **Probado** con el primer controller: 400, 404, 405, 409 y 415. Faltan 500 y 409 por el `UNIQUE` de la base. Ver [11](docs/11-excepciones-explicadas.md) |
| Entidades | **4 de 12** (`TipoUsuario`, `Region`, `Comuna`, `Usuario`); **validan** contra MySQL 8.4. Ver [13](docs/13-entidades-explicadas.md) |
| Repositories | **4** (uno por entidad); errores de nombres corregidos. Arranque completo **confirmado** (log del 18 oct). Ver [14](docs/14-repositories-explicados.md) |
| `SecurityConfig` y `PasswordEncoder` (BCrypt) | **Escritos**; todas las rutas **abiertas** (`permitAll`) a propósito para desarrollar. Swagger carga y `/actuator/health` responde (confirmado). Ver [15](docs/15-seguridad-codigo-explicado.md) |
| JWT (`JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl`) | Clases vacías (fase 4) |
| Administrador inicial | **Se crea por código al arrancar** (no con `V4`): verificado en el log del 18 oct y en la base (fila vista en Workbench). Falta el segundo arranque; la clave fuera del log ya está verificada. Ver [17](docs/17-administrador-inicial-explicado.md) |
| Services | `UsuarioService`: administrador inicial, `registrar` y `buscarPorId` (**5 tests**, 19 oct). Ver [19](docs/19-validacion-run-service-y-controller-explicados.md) |
| DTOs y mapper de `Usuario` | `UsuarioResponse`, `UsuarioRequest` y `UsuarioMapper` **confirmados** (10 tests de `UsuarioRequestTest`), con validación del dígito verificador (módulo 11, `RunUtilTest`). Ver [18](docs/18-dto-mapper-y-validaciones-explicados.md) y [19](docs/19-validacion-run-service-y-controller-explicados.md) |
| Controllers | `UsuarioController` (`POST /api/usuarios`, `GET /api/usuarios/{id}`): **9 tests**, arranque y prueba en Swagger contra MySQL confirmados (201, 20 oct) |
| Configuración base | `open-in-view: false`, handlers 415 y 400, `PageResponse`, zona horaria, `OpenApiConfig` y CORS: **29 tests** y arranque confirmados (20 oct). Ver [20](docs/20-configuracion-base-explicada.md) |
| `Dockerfile` y servicio `backend` del compose | Vacío / comentado |

## Reglas de oro

1. **Spring no lee el `.env`.** Lo lee Docker Compose; a Spring hay que pasarle las variables al arrancar.
2. **Una migración aplicada no se edita.** Los cambios son archivos nuevos (`V4__...` en adelante). Mientras `V1` y `V2` solo existan en tu máquina, puedes recrearlas con `docker compose down -v`.
3. **Los scripts de `docker/mysql-init` y las claves `MYSQL_*` solo se aplican al crear el volumen.**
4. **`docker compose down -v` borra los datos.** Nunca en producción.
5. **La app en producción usa `tecnofactory_app`**, nunca `root`.
6. **Una clase `public` por archivo Java**, con el mismo nombre que la clase.
7. **Los métodos de repository se escriben con los atributos de Java, no con las columnas** (`findByNombre`, no `findByNombreComuna`). Ver [14](docs/14-repositories-explicados.md#34-los-nombres-se-escriben-contra-los-atributos-java-no-las-columnas).
8. **La entidad refleja a `V1`, no al revés.** `unique`, `nullable` y `length` en `@Column` no crean ni verifican nada: manda el esquema. Ver [13](docs/13-entidades-explicadas.md#13-qué-comprueba-ddl-auto-validate).
9. **`.anyRequest().permitAll()` en `SecurityConfig` es temporal** y no se despliega. Ver [15](docs/15-seguridad-codigo-explicado.md#5-lo-que-cambia-cuando-llegue-el-login).
10. **El DTO de salida nunca lleva la contraseña; el de entrada nunca lleva rol, `id` ni `activo`.** El mapper copia datos; los hashes y las decisiones son del service. Ver [18](docs/18-dto-mapper-y-validaciones-explicados.md#12-mass-assignment-por-qué-el-request-no-tiene-rol).
11. **El controller no tiene lógica de negocio.** Valida (`@Valid`), delega en el service y responde; el service es `@Transactional` y devuelve DTOs. Los errores se lanzan como `RecursoNoEncontradoException` o `ReglaNegocioException` y los formatea el handler. Ver [19](docs/19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad).
