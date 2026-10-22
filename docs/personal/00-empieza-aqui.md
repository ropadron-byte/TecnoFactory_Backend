# Empieza aquí

Este manual es **tuyo**: está organizado por lo que quieres hacer, no por tema técnico. Cuando necesites entender el porqué, cada página te manda a la guía rápida o a la referencia.

## ¿Qué quieres hacer?

| Quiero... | Ve a |
| --- | --- |
| Encender el proyecto después de días sin tocarlo | [01 Levantar el proyecto](01-levantar-el-proyecto.md) |
| Agregar una tabla nueva a la API (Categoría, Producto...) | [02 Agregar una entidad](02-agregar-una-entidad.md) |
| Arreglar algo que falla | [03 Algo falla](03-algo-falla.md) |
| Recordar un comando | [04 Comandos](04-comandos.md) |
| Probar la API (Swagger, curl) | [05 Probar la API](05-probar-la-api.md) |
| Entender qué pasa cuando llega una petición | [06 Cómo viaja una petición](06-como-viaja-una-peticion.md) |
| Saber quién puede hacer qué | [07 Roles y permisos](07-roles-y-permisos.md) |
| Subir el proyecto a producción | [08 Antes de desplegar](08-antes-de-desplegar.md) |
| Repasar lo que no debo olvidar | [09 Reglas y trampas](09-reglas-y-trampas.md) |
| Resolver una duda rápida | [10 Preguntas frecuentes](10-preguntas-frecuentes.md) |
| Conectar el frontend en React | [Guía 17](../guias/17-integracion-con-react.md) |
| Consultar una tabla o una ruta | [Diccionario de datos](../referencia/22-diccionario-de-datos.md) · [Catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md) |
| Saber por qué algo es así | [Decisiones de diseño](../referencia/24-decisiones-de-diseno.md) · [Glosario](../referencia/25-glosario.md) |

## Si necesitas entender, no solo hacer

- **Explicaciones cortas con analogías:** [guías rápidas](../guias/) (17, una por tema).
- **Todo el detalle, línea por línea:** [referencia técnica](../referencia/).
- **Qué cambió y qué falta:** [estado](../estado/cambios-recientes.md) y [hoja de ruta](../guias/16-hoja-de-ruta.md).

## Estado en una línea

El esqueleto de la API está hecho (usuarios, errores, seguridad con JWT y roles, configuración). Falta confirmar los 56 tests y el login en Swagger, y empezar con `Categoria` como primera entidad completa.
