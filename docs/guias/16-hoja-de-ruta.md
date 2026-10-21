# Guía 16. Hoja de ruta: qué hay y qué falta

> **En una frase:** el esqueleto de la API (usuarios, errores, seguridad, configuración) está construido; falta repetir la receta para las demás entidades y sumar correo, imágenes y despliegue.

## Dónde estamos

| Fase | Qué | Estado |
| --- | --- | --- |
| 0 | Limpieza inicial | Hecha |
| 1 | Esquema y datos iniciales (`V1`, `V2`, `V3`) | Hecha |
| 2 | Entidades, repositories y errores | **En curso**: 4 de 12 entidades |
| 3 | Catálogo: categorías y productos | Pendiente |
| 4 | Seguridad: JWT, roles y usuarios | **En curso**: código escrito; falta confirmar 56 tests y el login en Swagger |
| 5 | Carrito, boleta y pago simulado | Pendiente |
| 6 | Imágenes y blog | Pendiente |
| 7 | Correo y caché (Redis) | Pendiente |
| 8 | Pruebas automáticas | Continua |
| 9 | Despliegue completo (`Dockerfile`, compose de prod) | Pendiente |
| 10 | Frontend React | En paralelo |

## Lo que sigue, en orden

1. **Confirmar** los 56 tests, el arranque y el login en Swagger.
2. **Limpieza:** mover `JwtServiceTest` y `UserDetailsServiceImplTest` a `security/`; borrar los `// NUEVO` de `UsuarioService`; quitar `unique = true` de `Region` y `Comuna`.
3. **Usuarios con roles:** `GET /api/usuarios` paginado (administrador y vendedor, solo lectura), `PUT /api/usuarios/me`, y una ruta de administrador para **crear vendedores**.
4. **Primera rebanada completa:** `Categoria` (entidad, repository, DTOs, mapper, service, controller, tests, regla de rol). Es la plantilla del resto: sigue la [receta](../personal/02-agregar-una-entidad.md).
5. **`Producto`**, con permisos para vendedor y administrador.
6. **Carrito y boleta**, con la copia congelada del precio.
7. **Después:** imágenes, blog, correo y caché.

## Decisiones abiertas

| Decisión | Notas |
| --- | --- |
| Cómo se aplica el descuento (0 a 100) sobre el precio y su copia en `detalle_boleta` | Antes de `Boleta` |
| Detalle por parámetro en `HandlerMethodValidationException` | Hoy solo dice «Parámetros inválidos» |
| Dónde guarda el frontend el token | `localStorage` es simple pero expuesto a XSS |

## No hace falta todavía

Redis y caché (`@Cacheable`), correo, subida de imágenes y el `Dockerfile` del backend. Los starters ya están en el `pom.xml`; se configuran el día que se usen.

## Riesgos conocidos

| Riesgo | Mitigación |
| --- | --- |
| Token no revocable | Vida de 1 hora; desactivar al usuario lo bloquea al instante |
| Sin límite de intentos de login | Pendiente antes de producción |
| Sin HTTPS | Obligatorio al desplegar |
| Claves del `.env` compartidas | No reutilizarlas en producción |

## Para saber más

[Referencia 10: plan completo por fases](../referencia/10-pendientes-y-futuro.md) · [Estado y cambios recientes](../estado/cambios-recientes.md)
