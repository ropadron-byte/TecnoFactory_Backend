# Guía 16. Hoja de ruta: qué hay y qué falta

> **En una frase:** la API ya cubre usuarios, seguridad, catálogo, carrito, boletas, blogs e imágenes (todo bajo `/api/v1`); faltan la verificación manual en Swagger, correo/caché y el despliegue completo.

## Dónde estamos

| Fase | Qué | Estado |
| --- | --- | --- |
| 0 | Limpieza inicial | Hecha |
| 1 | Esquema y datos iniciales (`V1`, `V2`, `V3`) | Hecha |
| 2 | Entidades, repositories y errores | **Hecha**: 14 entidades + 2 claves compuestas, 12 repositories |
| 3 | Catálogo: categorías y productos | **Hecha**: [referencia 30](../referencia/30-catalogo-y-precios.md) |
| 4 | Seguridad: JWT, roles y usuarios | **Hecha en código** (269 tests, 0 fallos); falta recorrer el login en Swagger |
| 5 | Carrito y boleta | **Hecha**: [referencia 31](../referencia/31-carrito-y-boletas.md) (la simulación de pago vive en el frontend) |
| 6 | Imágenes y blog | **Hecha** en el backend: panel de imágenes y blogs con CRUD |
| 7 | Correo y caché (Redis) | Pendiente |
| 8 | Pruebas automáticas | 269 tests; faltan tests de catálogo, carrito y boletas |
| 9 | Despliegue completo (`Dockerfile`, compose de prod) | Pendiente |
| 10 | Frontend React | **Hecho**: catálogo sincronizado desde la API; ver su README |

## Lo que sigue, en orden

1. **Confirmar** el flujo de login en Swagger; los 269 tests y `contextLoads` con MySQL pasan.
2. **Limpieza pendiente:** mover `JwtServiceTest` y `UserDetailsServiceImplTest` a `security/` y borrar los `// NUEVO` que siguen en `UsuarioService`.
3. **Usuarios:** ya están hechos `GET /api/v1/usuarios` paginado (administrador y vendedor, solo lectura), `PUT /api/v1/usuarios/me` y `POST /api/v1/usuarios/vendedores`. Falta probarlos en vivo tras reiniciar el backend.
4. **Tests nuevos:** cubrir `CategoriaService`, `ProductoService`, `CarritoService` y `BoletaService` (hoy no los hay), además de una prueba de repository con Testcontainers.
5. **Correo y caché** (fase 7), y después el despliegue completo (fase 9).
6. **Contacto:** probarlo en Swagger y agregar un límite de envíos (es una ruta pública). Ver [guía 19](19-formulario-de-contacto.md).

## Decisiones abiertas

| Decisión | Notas |
| --- | --- |
| Descuento (0 a 100) | **Resuelto**: `Precios.conDescuento` y `precioFinal` en productos, carrito y boletas ([30](../referencia/30-catalogo-y-precios.md)) |
| Detalle por parámetro en `HandlerMethodValidationException` | Hoy solo dice «Parámetros inválidos» |
| Dónde guarda el frontend el token | `localStorage` es simple pero expuesto a XSS |

## No hace falta todavía

Redis y caché (`@Cacheable`), correo y el `Dockerfile` del backend. Los starters ya están en el `pom.xml`; se configuran el día que se usen. La subida de imágenes ya está implementada ([guía 18](18-subida-de-imagenes.md)).

## Riesgos conocidos

| Riesgo | Mitigación |
| --- | --- |
| Token no revocable | Vida de 1 hora; desactivar al usuario lo bloquea al instante |
| Sin límite de intentos de login | Pendiente antes de producción |
| Sin HTTPS | Obligatorio al desplegar |
| Claves del `.env` compartidas | No reutilizarlas en producción |

## Para saber más

[Referencia 10: plan completo por fases](../referencia/10-pendientes-y-futuro.md) · [Estado y cambios recientes](../estado/cambios-recientes.md)
