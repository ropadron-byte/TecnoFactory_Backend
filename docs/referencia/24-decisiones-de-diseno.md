# 24. Decisiones de diseño

Cada decisión importante del proyecto con su **porqué**, qué alternativa se descartó y qué consecuencia trae. Sirve para que, en seis meses, nadie (tú incluido) la deshaga sin saber qué protegía.

Formato: **Decisión · Motivo · Alternativa descartada · Consecuencia.**

## Base de datos

### D1. La base manda; las entidades la copian
- **Motivo:** el esquema vive en migraciones versionadas (`V1`...); Hibernate solo **comprueba** (`ddl-auto: validate`).
- **Descartado:** `ddl-auto: update`, que modifica la base sin que nadie lo revise.
- **Consecuencia:** una entidad mal escrita impide arrancar. Es deliberado: el error aparece al instante y no en producción.

### D2. Flyway con un usuario distinto al de la app
- **Motivo:** `tecnofactory_migrator` tiene permisos de estructura (DDL); `tecnofactory_app` solo de datos (DML). Si alguien explota la app, no puede borrar tablas.
- **Descartado:** un solo usuario con todos los permisos.
- **Consecuencia:** hay tres usuarios y más variables que manejar.

### D3. Las migraciones aplicadas no se editan
- **Motivo:** Flyway guarda el checksum de cada una; editarla rompe el arranque en cualquier base que ya la aplicó.
- **Consecuencia:** un cambio es siempre una `V` nueva. En desarrollo se puede empezar de cero (`down -v`).

### D4. Casi no se borra: baja lógica con `activo`
- **Motivo:** las claves foráneas con `RESTRICT` impiden borrar lo que tiene historia (boletas, blogs), y los datos de una venta no deben desaparecer.
- **Consecuencia:** «eliminar» un producto o un usuario es poner `activo = false`; los listados deberán filtrar por `activo`.

### D5. El dinero son pesos enteros (`BIGINT`)
- **Motivo:** el peso chileno no usa decimales; evita errores de redondeo de `double`.
- **Consecuencia:** el frontend formatea el número (`$12.990`).

### D6. La boleta guarda una copia del nombre y del precio
- **Motivo:** es un documento histórico; no debe cambiar si el producto cambia después.

### D7. Los roles se buscan por nombre, nunca por id
- **Motivo:** los ids son `AUTO_INCREMENT`; el nombre es estable.

### D8. Los ids de región y comuna son los códigos oficiales
- **Motivo:** vienen de `V2` y representan datos externos; por eso no llevan autoincremento.

## Código

### D9. Capas: controller → service → repository
- **Motivo:** cada capa tiene un trabajo y se prueba por separado. El controller no tiene lógica; el service tiene las reglas y la transacción.

### D10. Nunca se devuelve una entidad: se usan DTOs (`Request` y `Response`)
- **Motivo:** (1) no filtrar campos internos como el hash de la clave; (2) no depender de relaciones perezosas fuera de la transacción; (3) el contrato de la API no cambia cuando cambia la tabla.

### D11. El `Request` no tiene `id`, `activo` ni rol
- **Motivo:** protección contra la **asignación masiva**: si el cuerpo pudiera traer `"rol": "ADMINISTRADOR"`, cualquiera se haría administrador al registrarse.
- **Consecuencia:** el service decide esos valores. La clave se llama `clave` (no `contrasenia`) para no confundirla con la columna.

### D12. `open-in-view: false`
- **Motivo:** evita consultas a la base escondidas en la capa web y deja claro dónde empieza y termina la transacción.
- **Consecuencia:** el service **mapea a DTO dentro** de la transacción (`Comuna.region` es `LAZY`). Si lo mapeas fuera, `LazyInitializationException`.

### D13. Excepciones propias y un único `GlobalExceptionHandler`
- **Motivo:** el service lanza `RecursoNoEncontradoException` (404) o `ReglaNegocioException` (409) sin saber nada de HTTP; el handler traduce.
- **Consecuencia:** todos los errores salen con el mismo JSON (`ErrorResponse`).

### D14. Las validaciones están en el DTO (Bean Validation)
- **Motivo:** se declaran una vez, junto al campo; `@Valid` en el controller las dispara.
- **Detalle:** el dígito verificador del RUN se valida con una anotación propia a nivel de clase (`@RunValido`), porque depende de **dos** campos.

### D15. MapStruct para convertir entre entidad y DTO
- **Motivo:** genera el código en compilación (rápido y sin trucos en ejecución).

## Seguridad

### D16. Sin sesión de servidor: JWT sin estado (`STATELESS`)
- **Motivo:** el frontend en React es una aplicación separada; el token viaja en cada petición.
- **Consecuencia:** un token no se puede revocar antes de expirar → vida corta (1 hora). CSRF se desactiva porque no hay cookies de sesión.

### D17. El filtro consulta la base en cada petición
- **Motivo:** si desactivas o cambias el rol de un usuario, tiene efecto **al instante**, aunque su token siga vigente.
- **Costo:** una consulta por petición (aceptable hoy; se puede cachear después).

### D18. `JwtAuthFilter` identifica, no rechaza
- **Motivo:** quien decide el 401 o el 403 es `SecurityConfig`; así hay una sola fuente de reglas.

### D19. Un solo 401 para todo fallo de login
- **Motivo:** si «el correo no existe» y «la clave es incorrecta» dieran mensajes distintos, un atacante podría descubrir qué correos están registrados.

### D20. Todo lo no listado exige sesión (`anyRequest().authenticated()`)
- **Motivo:** si olvidas proteger una ruta nueva, queda cerrada y no abierta.

### D21. El 401 y el 403 pasan por el mismo manejador de errores
- **Motivo:** las reglas por ruta fallan en los filtros, antes del controller; `ManejadorErroresSeguridad` los reenvía al `GlobalExceptionHandler` para que el JSON sea idéntico.

### D22. El administrador inicial se crea por código, no por migración
- **Motivo:** una migración con una clave quedaría en Git para siempre. Se toma de `ADMIN_EMAIL` y `ADMIN_PASSWORD` y se guarda con BCrypt.

### D23. El registro público solo crea clientes
- **Motivo:** nadie debe poder auto-asignarse un rol alto. Vendedores y administradores se crean desde una ruta de administrador (pendiente).

### D24. La clave se limita a 72 caracteres
- **Motivo:** BCrypt solo considera los primeros 72 bytes; permitir más daría una falsa sensación de seguridad.

## Configuración

### D25. Secretos solo por variables de entorno, sin valor por defecto en producción
- **Motivo:** si falta `JWT_SECRET` en producción, la app **no arranca**, en lugar de arrancar con una clave conocida.

### D26. Perfiles `local` y `prod`
- **Motivo:** en local ves todo (SQL, Swagger, `DEBUG`); en producción lo mínimo (sin Swagger, errores sin detalle, logs en `WARN`).

### D27. Zona horaria `America/Santiago`
- **Motivo:** las fechas de las boletas deben corresponder a la hora chilena; se fija en Jackson (JSON) y en Hibernate (base de datos).

### D28. Paginación con tope de 100
- **Motivo:** que nadie pida `?size=1000000` y tumbe el servidor.

### D29. CORS por variable (`CORS_ORIGINS`)
- **Motivo:** el origen del frontend cambia entre desarrollo y producción; por defecto solo `http://localhost:5173`.

## Pruebas

### D30. Los tests de service y controller no usan la base
- **Motivo:** son rápidos y no dependen de Docker. Solo `contextLoads` necesita MySQL.

## Decisiones aún abiertas

| Tema | Opciones |
| --- | --- |
| Cómo se aplica `descuento` al precio | Sobre el total de la línea o sobre el precio unitario antes de copiarlo a `detalle_boleta` |
| Dónde guarda React el token | `localStorage` (simple, expuesto a XSS) o cookie `HttpOnly` (más segura, exige ajustes de CORS y CSRF) |
| Cómo se crean los vendedores | Ruta de administrador (decidido), forma exacta pendiente |
| Revocación de tokens | Lista de tokens anulados (Redis) o *refresh tokens* |
| Blogs ocultos en `GET /api/imagenes` | Hoy el listado público devuelve también las imágenes de blogs ocultos y, en `duenos`, el id de esos blogs. Opciones: dejarlo (las URLs ya son públicas en `/uploads/**`) o filtrar los dueños ocultos para quien no sea administrador ni autor (exige pasar `Authentication` al service) |

## Para saber más

[Guía 16: hoja de ruta](../guias/16-hoja-de-ruta.md) · [Referencia 10: pendientes y futuro](10-pendientes-y-futuro.md)
