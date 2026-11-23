# 10. Pendientes y futuro

Lo que falta construir, en orden recomendado, y qué cambia en la configuración al hacerlo. Cada fase se apoya en la anterior: pruébala en Swagger (local) antes de pasar a la siguiente.

## Resumen de fases

| Fase | Qué | Depende de | Prioridad | Estado |
| --- | --- | --- | --- | --- |
| 0 | Limpieza y decisiones del esquema | — | **Ahora** | **Hecha** |
| 1 | Esquema definitivo y datos iniciales (`V1`, `V2`, `V3`) | 0 | **Ahora** | **Hecha** (el administrador pasó a crearse por código, ver fase 2) |
| 2 | Entidades, repositorios y manejo de errores | 1 | Alta | **Hecha**: 14 entidades + 2 claves compuestas, 12 repositories, errores probados; el `SecurityConfig` temporal ya no existe |
| 3 | Catálogo: categorías y productos | 2 | Alta | **Hecha**: CRUD, paginación, ofertas y `V9` ([30](30-catalogo-y-precios.md)) |
| 4 | Seguridad: JWT, roles y usuarios | 2 | Alta | **Hecha en código**: 269 tests y `contextLoads` en verde; falta solo el recorrido manual en Swagger |
| 5 | Carrito y boleta | 3, 4 | Media | **Hecha**: checkout transaccional, anulación y paginación ([31](31-carrito-y-boletas.md)) |
| 6 | Imágenes y blog | 3, 4 | Media | **Hecha** en el backend (subida, panel y CRUD de blogs) |
| 7 | Correo y caché (Redis) | 4 | Baja | Pendiente |
| 8 | Pruebas automáticas | todas | Continua | 269 tests; faltan pruebas de catálogo, carrito y boletas |
| 9 | Despliegue completo | todas | Al final | Pendiente |
| 10 | Frontend React | 3 en adelante | En paralelo | **Hecho**: catálogo sincronizado desde `/api/v1` |

---

## Fase 0. Limpieza inmediata

- [x] **Verificar que `.env` no está en Git.** `git ls-files | grep -i env` no devuelve nada y `.gitignore` contiene `.env`, `target/` y `.idea/`. **Hecho.**
- [x] Corregir la indentación de `jpa:` en `application-local.yaml` (ver 04). **Hecho.**
- [x] Actualizar la documentación (`README.md` y `docs/`) al estado actual. **Hecho** (ver [Cambios recientes](../estado/cambios-recientes.md)).
- [x] Reemplazar los manuales viejos (`COMO_EJECUTAR.md`, `MANUAL_TECNOFACTORY.md`, `TecnoFactory - Guía del proyecto.md`) por esta carpeta `docs/`. **Hecho.**
- [x] Mover `ExplicacionesCrudas.md` fuera del repositorio o a `docs/archivo/`. **Hecho.**
- [x] No compartir el ZIP del proyecto con el `.env` ni la carpeta `target/`. **Tenido en cuenta.**

## Fase 1. Esquema definitivo

- [x] Resolver las observaciones 1 a 7 de [06-base-de-datos.md](06-base-de-datos.md). **Hecho.**
- [x] `UNIQUE` en `correo` y `run`. **Hecho.**
- [x] Decisiones del esquema:

| Decisión | Resultado |
| --- | --- |
| Roles | Se mantiene la tabla `tipo_usuario` (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`) |
| Imágenes | Tabla `imagen_url` con 3 FK y `CHECK` de un solo dueño |
| Estado de boleta | Se mantiene `estado BOOLEAN NOT NULL DEFAULT TRUE` |
| Borrado de productos | Se **desactivan** (`activo`); `detalle_boleta` → `producto` es `RESTRICT` |
| Detalle de boleta | Lleva `producto_id_producto` además de la copia congelada |
| Borrado en cascada | Solo `carrito`, `detalle_carrito`, `producto_categoria` e `imagen_url`; las boletas nunca |
| Descuento | Entero 0-100 en producto (y copia en el detalle de boleta). **Resuelto**: `Precios.conDescuento` redondea al peso; ver [30](30-catalogo-y-precios.md) |

- [x] `V1` aplicada desde cero (probada en MariaDB 10.11).
- [x] **`V2__datos_iniciales.sql`**: 3 tipos de usuario, 16 regiones y 346 comunas. **Hecho.**
- [x] **Confirmar `V1` + `V2` en tu MySQL 8.4 de Docker.** **Hecho.**
- [x] **Categorías base** (`V3`). **Aplicada** en tu MySQL 8.4 (fue la versión 3 de la historia; hoy Flyway llega a `V9`). Si cambias la lista antes de desplegar, recrea la base (`down -v`); después de desplegar, se hace con una migración nueva.
- [x] **Administrador inicial: se cambió de `V4` a creación por código** (decisión del 18 oct). La plantilla `V4` ya no se usa. Sigue en la fase 2 (ver abajo). **Nunca** una contraseña en texto plano en Git.
- [ ] Actualizar el diagrama de `Diagrama/`. **Pospuesto**: está hecho en Oracle SQL Developer Data Modeler, cuyos tipos de dato son distintos a los de MySQL, y no se puede exportar directo. La fuente de verdad es `V1`.

**`V1` y `V2` se congelan** en cuanto se desplieguen: cualquier cambio será `V3`, `V4`...

## Fase 2. Entidades, repositorios y errores

| Archivo | Estado |
| --- | --- |
| `exception/RecursoNoEncontradoException` | **Hecho** (extiende `RuntimeException`) |
| `exception/ReglaNegocioException` | **Hecho** |
| `exception/ErrorResponse` | **Hecho** (`record`) |
| `exception/GlobalExceptionHandler` | **Hecho**: handlers para 400, 401, 403, 404, 405, 409, 415, 413, 500 y el 409 por `UNIQUE`. Los tests cubren 400/404/405/409/415/413; el 500, el 409 por `UNIQUE` y el 400 por parámetro faltante siguen sin test directo |
| `entity/TipoUsuario`, `Region`, `Comuna`, `Usuario` | **Hechas**; las cuatro validan contra MySQL 8.4. `camelCase`, `unique = false` en `run_dv`, `Integer id` y el `UNIQUE` real de nombres (`V7`) están aplicados; siguen pendientes solo los comentarios `// NUEVO` de `UsuarioService` |
| `entity/*` restantes | **Hechas**: `Categoria`, `Producto`, `ImagenUrl`, `Carrito`, `DetalleCarrito`, `Boleta`, `DetalleBoleta`, `Blog`, más las claves compuestas `ProductoCategoriaId` y `DetalleCarritoId` |
| `repository/*` de las 4 entidades | **Hechos**; errores de nombres de método corregidos. Explicados en [14](14-repositories-explicados.md) |
| `repository/*` restantes | **Hechos**: 12 interfaces en total, una por entidad con id propio |
| `SecurityConfig` | **Reescrito el 20 oct**: la versión temporal (todo abierto) ya no existe. Ver [21](21-seguridad-jwt-y-roles-explicada.md) y, para la primera versión, [15](15-seguridad-codigo-explicado.md) |
| `service/DatosAdministrador`, `service/UsuarioService`, `config/AdministradorInicialRunner` | **Hechos** (18 oct): el administrador se crea por código al arrancar. Verificado en el log; faltan 2 comprobaciones (fila en la base y segundo arranque). Explicados en [17](17-administrador-inicial-explicado.md) |
| `dto/UsuarioResponse`, `dto/UsuarioRequest`, `mapper/UsuarioMapper`, `UsuarioRequestTest` | **Hechos y confirmados** (19 oct): 10 tests de `UsuarioRequestTest`. Explicados en [18](18-dto-mapper-y-validaciones-explicados.md) |
| `validation/RunUtil`, `RunValido`, `RunValidoValidator`, `UsuarioService.registrar` y `buscarPorId`, `controller/UsuarioController` y sus tres pruebas | **Hechos y confirmados** (19 oct): 14 + 5 + 8 tests y la app arranca. Explicados en [19](19-validacion-run-service-y-controller-explicados.md). Falta probar en Swagger (9.3) |

Cómo comprobar: arrancar con `local` y `validate`; si Hibernate no se queja, las entidades calzan con el esquema.

### Siguiente tramo de la fase 2: administrador al arrancar y primer controller

Se avanza **de a un paso** y se arranca la app al terminar cada uno. Los pasos 1 a 9.3 ya están hechos y confirmados.

| Paso | Qué | Qué se aprende | Estado |
| --- | --- | --- | --- |
| 1 | Entidad `TipoUsuario` | Qué es una entidad y qué comprueba `validate` | Hecho |
| 2 | `Region` y `Comuna` | Relación `@ManyToOne` | Hecho |
| 3 | `Usuario` | `CHAR(1)` con `@JdbcTypeCode`, fecha, dos relaciones | Hecho |
| 4 | Repositories | Nombres de método y tipos de retorno | Hecho |
| 5 | `PasswordEncoder` y `SecurityConfig` | BCrypt y la cadena de filtros | Hecho |
| 6 | `UsuarioService` con "crear administrador si no existe" | Dónde va la lógica de negocio; `@Service`, `@Transactional`; usar `existsByTipoUsuarioNombre` | Hecho |
| 7 | `ApplicationRunner` + variables `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Ejecutar código al arrancar; leer variables de entorno | Hecho |
| 8.1 | `UsuarioResponse` y `UsuarioMapper.toResponse` | Por qué no se expone la entidad; MapStruct | Hecho (compiló; código generado revisado) |
| 8.2 | `UsuarioRequest` con validaciones, `toEntity` y `UsuarioRequestTest` | Bean Validation; mass assignment; tests sin base de datos | **Hecho** (confirmado el 19 oct) |
| 8.3 | Validar que el dígito verificador corresponda al RUN (módulo 11) | Validación personalizada de clase | **Hecho** (14 tests) |
| 9.1 | `UsuarioService`: registrar (hash, rol `CLIENTE`, unicidad, comuna existente) y buscar por id | Mapear dentro de la transacción | **Hecho** (5 tests) |
| 9.2 | `UsuarioController` con `@Valid` | La capa HTTP | **Hecho** (8 tests) |
| 9.3 | Probar en Swagger y ver los errores formateados | El flujo completo; sirve de **ejemplo para tu compañero** | **Hecho** (20 oct, 201) |

Antes del paso 8 conviene aplicar las tres mejoras a las entidades ([13, sección 7](13-entidades-explicadas.md#7-observaciones-sobre-el-código-actual)).

## Fase 3. Catálogo — **hecha**

Detalle completo en [30-catalogo-y-precios.md](30-catalogo-y-precios.md). El catálogo inicial de 21 productos y 6 categorías viene de `V9__catalogo_inicial.sql`.

- `dto/CategoriaRequest|Response`, `ProductoRequest|Response`.
- `mapper/CategoriaMapper`, `ProductoMapper` (MapStruct).
- `service/CategoriaService`, `ProductoService`: crear, editar, buscar, paginar, validar stock y descuento.
- `controller/CategoriaController`, `ProductoController`: `/api/v1/categorias`, `/api/v1/productos`. **GET público; escritura solo administrador.**
- Paginación resuelta con `Pageable` + `@ParameterObject` y filtro por `categoriaId` (texto y rango de precio siguen sin implementarse).

## Fase 4. Seguridad y usuarios

Guía: [21-seguridad-jwt-y-roles-explicada.md](21-seguridad-jwt-y-roles-explicada.md).

| Archivo | Contenido | Estado (20 oct) |
| --- | --- | --- |
| `security/JwtService` | Genera y valida tokens (jjwt 0.12.6) | Hecho; test confirmado |
| `security/UserDetailsServiceImpl` | Carga el usuario por correo y le asigna el rol | Hecho |
| `security/JwtAuthFilter` | Lee `Authorization: Bearer ...` en cada petición | Hecho |
| `security/SecurityConfig` | Rutas públicas, protegidas y por rol; BCrypt; CORS; sin sesión | Hecho; falta confirmar |
| `security/ManejadorErroresSeguridad` | 401 y 403 con el JSON de `ErrorResponse` | Hecho; falta confirmar |
| `dto/LoginRequest`, `AuthResponse` | Entrada y salida del login; la clave nunca sale | Hecho |
| `service/AuthService`, `controller/AuthController` | `POST /api/v1/auth/login` | Hecho; falta confirmar |
| `UsuarioController` (`/me`) y `UsuarioService.buscarPorCorreo` | Perfil propio | Hecho; falta confirmar |
| `config/OpenApiConfig`, `config/CorsConfig` | Botón Authorize en Swagger; dominio de React | Hechos (configuración base, [20](20-configuracion-base-explicada.md)) |

**Pendiente de la fase:**

- **Confirmar** el flujo de login en Swagger; los 269 tests y `contextLoads` con MySQL pasan.
- Usuarios ya implementados: `GET /api/v1/usuarios` paginado (administrador y vendedor, solo
  lectura), `PUT /api/v1/usuarios/me` y `POST /api/v1/usuarios/vendedores`.
- **Límite de intentos de login**.
- Decidir el flujo de **recuperar contraseña** (necesita correo, fase 7).
- **Validar el RUN chileno**: hecho (paso 8.3, [19](19-validacion-run-service-y-controller-explicados.md)).

Cambios de configuración: ninguno, salvo `JWT_SECRET` de producción nuevo (mínimo 32 caracteres) y `CORS_ORIGINS` con la URL real del frontend.

## Fase 5. Carrito y boleta — **hecha en el backend**

Detalle en [31-carrito-y-boletas.md](31-carrito-y-boletas.md). No existe una interfaz `PasarelaPago` en el backend: la simulación de pago exitoso/rechazado está en el frontend React, que decide si llama al `checkout`.

- `Carrito` (uno por usuario), `DetalleCarrito`: agregar, cambiar cantidad, quitar, vaciar.
- `Boleta`, `DetalleBoleta`: copia de nombre y precio al comprar.
- `service/pago/PasarelaPago` (interfaz) y `PasarelaPagoSimulada`: aprueba o rechaza con una regla de prueba.
- `service/BoletaService`: **una sola transacción** (`@Transactional`): validar stock, crear boleta, copiar detalles, **descontar stock**, vaciar carrito.
- Controles: stock insuficiente (409), producto desactivado, concurrencia (dos compras del último producto → bloqueo optimista con `@Version` o `SELECT ... FOR UPDATE`).
- **Nunca** guardes números de tarjeta.
- Alerta de `stock_critico` (por ahora un registro en el log; luego correo al administrador).

## Fase 6. Imágenes y blog — **hecha en el backend**

| Pieza | Detalle |
| --- | --- |
| Subida | Valida contenido (jpg, png, webp) y tamaño (límite actual 5 MB), guarda con nombre UUID y devuelve la URL |
| `config/WebConfig` | Sirve la carpeta `/uploads` como estática, con cabeceras de `CabecerasUploadsFilter` |
| Almacenamiento | **Volumen Docker** o almacenamiento en la nube; sin volumen se pierden al recrear el contenedor |
| `IMAGEN_URL` | Solo guarda la URL |
| Blog | `BlogController` público para lectura (`visible = true`), edición por administrador |

Cambios: el compose (aún con el servicio `backend` comentado) ya prevé el volumen `${UPLOADS_HOST_DIR:-./uploads}:/app/uploads` y la variable es `UPLOADS_DIR` / `UPLOADS_HOST_DIR` (no `UPLOAD_DIR`).

## Fase 7. Correo y caché

**Correo**

- `service/EmailService`: bienvenida, confirmación de compra, recuperación de contraseña.
- En desarrollo: **Mailpit** (servicio en el compose, SMTP en `1025`, interfaz web en `8025`).
- En producción: SMTP real; variables `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` y `starttls`.
- Reactivar `management.health.mail.enabled` en `application-prod.yaml`.

**Caché con Redis**

- Agregar el servicio `redis` al compose.
- `config/CacheConfig` + `@EnableCaching` en `TecnoFactoryApplication`; `@Cacheable` en listados que casi no cambian (categorías, regiones y comunas).
- `spring.cache.type: redis` en `application-prod.yaml` y, si quieres, también en `local`.
- Reactivar `management.health.redis.enabled`.
- **Invalidar** el caché al modificar categorías (`@CacheEvict`).

## Fase 8. Pruebas

- Pruebas de repositorios con **Testcontainers** (usa `testcontainers-mysql` y el mismo `V1` real).
- Pruebas de servicio de catálogo, carrito y boletas (stock, totales y descuento): **aún no existen**.
- Pruebas de endpoints con `@WebMvcTest` y Spring Security Test.
- Reemplazar el `contextLoads` por defecto (hoy falla sin MySQL).
- Ejecutar `./mvnw verify` en cada `git push` con **GitHub Actions** (el repositorio ya está en GitHub).

## Fase 9. Despliegue completo

Ver el [Escenario 3 de 07](07-ejecucion-local-y-prod.md#escenario-3-prod-real-proyecto-terminado).

- [ ] `Dockerfile` del backend y `.dockerignore`.
- [ ] Servicio `backend` activo en el compose, con los dos usuarios.
- [ ] MySQL sin publicar en el host (usa un `docker-compose.override.yml` solo en tu máquina).
- [x] Nginx con HTTPS (Let's Encrypt) en `eva03` para `186-64-113-33.sslip.io` (23 nov). Pendiente: usarlo desde `vercel.json` y cerrar el 8081. Ver [guía 20](../guias/20-despliegue-frontend-vercel.md).
- [ ] Servicios `redis` y `mailpit`/SMTP si se implementaron.
- [ ] Respaldos programados (`cron` + `mysqldump`), copiados fuera del servidor, con **restauración probada**.
- [ ] Monitoreo: `/actuator/health` en un chequeo externo (UptimeRobot o similar).
- [ ] Logs: rotación y retención (Docker `logging` con `max-size`; ver [12](12-logs.md#9-logs-en-docker-y-producción)).
- [ ] Estrategia de **actualización** (`git pull`, `docker compose up -d --build backend`) y de **vuelta atrás** (respaldo previo a cada migración).
- [ ] `server.shutdown: graceful` y límites de memoria del contenedor (`-XX:MaxRAMPercentage`).

## Fase 10. Frontend (React)

- Consumir la API según el contrato de Swagger.
- Guardar el token JWT y manejar su expiración (1 hora) con renovación o reingreso.
- Manejar el formato de error uniforme del `GlobalExceptionHandler`.
- Build estático servido por Nginx en el mismo servidor.
- Configurar la URL de la API por variable de entorno del build. En Vercel se resolvió con `rewrites` y rutas relativas ([guía 20](../guias/20-despliegue-frontend-vercel.md)).

---

## Cómo cambia la configuración cuando se agrega cada pieza

| Al agregar... | `application-local.yaml` | `application-prod.yaml` | `docker-compose.yml` | `.env` |
| --- | --- | --- | --- | --- |
| Redis | `cache.type: redis` (opcional) | `cache.type: redis`, `health.redis` activo, `REDIS_HOST` | Servicio `redis` | — |
| Correo real | Mailpit en `1025` | `MAIL_*`, `health.mail` activo | Servicio `mailpit` solo en local | `MAIL_USER`, `MAIL_PASSWORD` |
| Imágenes | `UPLOAD_DIR` local | `UPLOAD_DIR` del volumen | Volumen `uploads` (`UPLOADS_HOST_DIR`) | — |
| Nuevos usuarios de BD | — | — | Otro script `02-...sh` | Nueva clave |
| Dominio / HTTPS | — | CORS con el dominio real | Servicio `proxy` (Nginx o Caddy) | — |
| Administrador inicial por código | — | — | — | `ADMIN_EMAIL`, `ADMIN_PASSWORD` (el resto de los datos va fijo en el runner). Con `set -a; source .env` quedan disponibles al arrancar |
| Nueva tabla o columna | — | — | — | — (se hace con una migración nueva, `V10__...sql` en adelante) |

## Mejoras opcionales a más largo plazo

| Mejora | Beneficio |
| --- | --- |
| Rate limiting en login | Frena la fuerza bruta |
| Auditoría (`creado_en`, `modificado_en`, `creado_por`) | Trazabilidad |
| Paginación y búsqueda de texto completo (`FULLTEXT`) | Catálogo grande |
| Pago real (Webpay, Mercado Pago) | Hoy no hay `PasarelaPago` en el backend: habría que crear esa abstracción |
| Roles con permisos finos | Más allá de administrador/vendedor/cliente |
| Almacenamiento de imágenes en la nube (S3) | Sin depender del disco del servidor |
| Observabilidad (Prometheus + Grafana) | Métricas del JVM y de las consultas |
| Migraciones de datos grandes con `repeatable migrations` (`R__`) | Vistas y datos de referencia |
| Backups cifrados y retención por días | Cumplir buenas prácticas |
| Entorno de *staging* igual al de producción | Probar despliegues antes |

## Riesgos conocidos si no se atienden

| Riesgo | Consecuencia | Mitigación |
| --- | --- | --- |
| Tipos de la entidad distintos a los de la tabla (`Long` frente a `INT`, `CHAR`, `TEXT`) | La app no arranca con `validate` | Tabla de mapeo en [06](06-base-de-datos.md#mapeo-a-entidades-java) |
| Editar una migración ya desplegada | La app no arranca | Siempre una versión nueva |
| Perder el volumen sin respaldo | Pérdida de todos los datos | Respaldos fuera del servidor |
| `.env` en Git | Claves expuestas | Fase 0; cambiar las claves |
| Dejar la seguridad por defecto | API inutilizable o abierta | Fase 4 |
| Guardar imágenes dentro del contenedor | Se pierden al recrearlo | Volumen (fase 6) |
