# 20. Configuración base explicada

Lo que se dejó resuelto **una sola vez** antes de la segunda entidad (20 oct 2026), para que `Categoria`, `Producto` y las demás solo repitan la [receta de la guía 19](19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad). Todo está **comprobado**: 29 tests pasan, la app arranca, Swagger muestra "TecnoFactory API" con el botón **Authorize** y `POST /api/usuarios` devuelve 201.

## 1. Resumen

| Pieza | Dónde | Qué hace |
| --- | --- | --- |
| `open-in-view: false` | `application.yaml` | La conexión a la base se cierra al salir del service; leer una relación `LAZY` en el controller falla en vez de esconderse |
| Handler 415 | `GlobalExceptionHandler` | `POST` sin `Content-Type: application/json` devuelve 415, no 500 |
| Handler parámetro faltante | `GlobalExceptionHandler` | Falta un `?parametro=` obligatorio: 400 con el nombre del parámetro |
| `PageResponse<T>` | `dto/` | Formato único para todos los listados |
| Tope de paginación | `application.yaml` | Página de 20 por defecto, máximo 100 |
| Zona horaria | `application.yaml` | `America/Santiago` en Jackson y en Hibernate |
| `OpenApiConfig` | `config/` | Título de la API y esquema `bearer` JWT (botón Authorize) |
| `CorsConfig` | `config/` | Orígenes permitidos leídos de `CORS_ORIGINS` |
| `WebConfig` | — | **Borrada**: estaba vacía; se crea el día que se necesite |

## 2. Los dos handlers

`HttpMediaTypeNotSupportedException` (415) y `MissingServletRequestParameterException` (400). Antes caían en el handler genérico y salían como 500, que le dice al frontend que el fallo es del servidor cuando es de quien llama. El 415 tiene test (`postSinContentTypeJsonDevuelve415`). El de parámetro faltante **no tiene test todavía**: `Usuario` no tiene parámetros obligatorios; se prueba con el primer listado con filtro.

## 3. Paginación

```json
{ "contenido": [...], "pagina": 0, "tamanio": 20, "totalElementos": 53, "totalPaginas": 3 }
```

| Pieza | Significado |
| --- | --- |
| `record PageResponse<T>` | `<T>` es un tipo genérico: sirve para `PageResponse<UsuarioResponse>`, `PageResponse<ProductoResponse>`, etc. |
| `de(Page<T> page)` | Convierte el `Page` de Spring Data. Un listado hará `PageResponse.de(repository.findAll(pageable).map(mapper::toResponse))` |
| `pagina` | Número de página, **desde 0** |
| `max-page-size: 100` | Si el cliente pide `?size=5000`, Spring lo recorta a 100 |

Se fijó **antes** del primer listado porque después cambiar el contrato obliga a cambiar el frontend.

## 4. Zona horaria

```yaml
spring:
  jackson:
    time-zone: America/Santiago
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: America/Santiago
```

Hoy no se nota (`fechaNacimiento` es solo fecha). Se nota con la primera `Boleta`, que tiene fecha y hora.

**Error real de este paso:** se había escrito `spring.jpa.time_zone` y `Jackson:` con mayúscula. Spring **ignora en silencio** las propiedades que no reconoce, así que los 29 tests pasaban igual y la zona horaria no estaba configurada. La ruta correcta de Hibernate es `spring.jpa.properties.hibernate.jdbc.time_zone`, y la de Jackson es `spring.jackson.time-zone`. Ver [09](09-errores-comunes.md).

## 5. Swagger (`OpenApiConfig`)

| Pieza | Significado |
| --- | --- |
| `@Configuration` + `@Bean` | Spring ejecuta el método y springdoc usa el `OpenAPI` que devuelve |
| `Info` | Título y descripción de la parte superior de Swagger |
| `SecurityScheme` HTTP, bearer, JWT | Declara que la API usa `Authorization: Bearer <token>` |
| `addSecurityItem` | Lo aplica a **todos** los endpoints: de ahí el candado en cada uno |

Hoy el candado **no hace nada**: `SecurityConfig` sigue con `.anyRequest().permitAll()`. Queda listo para el login. Swagger está desactivado en `prod` (`springdoc.*.enabled: false`).

## 6. CORS

El navegador bloquea que el frontend (otro puerto) llame a la API. Son tres cambios:

1. `config/CorsConfig.java`: un bean `CorsConfigurationSource` que permite los orígenes de `cors.allowed-origins`, los métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS` y las cabeceras `Authorization` y `Content-Type`, solo para `/api/**`.
2. `application.yaml`: `cors.allowed-origins: ${CORS_ORIGINS:http://localhost:5173}`. Si el frontend usa Create React App, agrega `,http://localhost:3000`.
3. `SecurityConfig`: `.cors(Customizer.withDefaults())`. Spring Security debe aplicar CORS **antes** de sus filtros; encuentra solo el bean.

En producción se define `CORS_ORIGINS` con la URL real, **sin `*`**.

## 7. Cómo comprobarlo

```bash
./mvnw test -Dtest="*Test"
```

Esperado: **29 tests, 0 fallos** (14 de RUN y request, 5 del service, 9 del controller y 1 de `PageResponse`). Luego arrancar la app, abrir `/swagger-ui/index.html` y repetir `POST /api/usuarios` (debe dar 201 con comuna y región).

## 8. Errores que aparecieron

| Síntoma | Causa | Solución |
| --- | --- | --- |
| Zona horaria sin efecto y ningún error | Propiedad mal escrita; Spring la ignora | Usar `spring.jackson.time-zone` y `spring.jpa.properties.hibernate.jdbc.time_zone` |
| Dos clases `UsuarioController` (main y test) | Se copió el controller a `src/test` | Borrar la de `src/test`; la válida es la de `src/main` |
| 415 salía como 500 | Faltaba el handler | `tipoDeContenidoNoSoportado` |
