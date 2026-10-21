# Guía 04. Perfiles y configuración base

> **En una frase:** un *perfil* decide qué ajustes usa la app (`local` para desarrollar, `prod` para producción), y la *configuración base* son los ajustes comunes que se dejaron listos una sola vez.

## La idea

Un perfil es como el **modo de un electrodoméstico**: el mismo aparato, dos configuraciones. En `local` ves todo (errores con detalle, Swagger); en `prod` ocultas lo sensible y no hay valores por defecto.

| | `local` | `prod` |
| --- | --- | --- |
| Swagger | Visible | Apagado |
| Errores de la API | Con detalle | Sin detalle |
| `/actuator/health` | Con detalles | Solo `UP`/`DOWN` |
| Valores por defecto | Algunos | Ninguno: si falta una variable, no arranca |
| Log de tu código | `DEBUG` | `INFO` |

Se elige con `SPRING_PROFILES_ACTIVE`. **Sin perfil, la app intenta usar Redis y falla**: arráncala siempre con `local` o `prod`.

## La configuración base (`application.yaml`)

| Ajuste | Qué hace | Analogía |
| --- | --- | --- |
| `open-in-view: false` | Hibernate solo lee datos **dentro del service** | En una biblioteca los libros se leen en el mostrador, no se llevan a casa |
| `time-zone: America/Santiago` (Jackson e Hibernate) | Misma hora para el JSON y la base | Un reloj de pared común para toda la oficina |
| `max-page-size: 100` | Tope de elementos por página en los listados | Un buscador muestra 10 resultados por página, no un millón |
| `PageResponse<T>` | Formato único de todos los listados | Todas las páginas de resultados se ven igual |
| `OpenApiConfig` | Título de Swagger y botón **Authorize** | El menú del restaurante |
| `CorsConfig` | Qué frontend puede llamar a la API | El portero con la lista de visitas |
| `jwt.expiration-ms: 3600000` | El token dura 1 hora | La tarjeta del hotel vence |

El formato de un listado:

```json
{ "contenido": [...], "pagina": 0, "tamanio": 20, "totalElementos": 53, "totalPaginas": 3 }
```

Un listado nuevo hace: `PageResponse.de(repository.findAll(pageable).map(mapper::toResponse))`.

## Reglas que no hay que olvidar

1. **Un nombre mal escrito en el YAML no da error:** Spring lo ignora en silencio. Pasó con la zona horaria (`Jackson:` y `jpa.time_zone`). Comprueba el nombre exacto.
2. **Hibernate en `validate`** nunca crea tablas: solo comprueba que coincidan.
3. **CORS lo bloquea el navegador**, no la API. Swagger y `curl` funcionan siempre; el problema aparece cuando llama el frontend desde otro puerto.
4. **En producción `CORS_ORIGINS` lleva la URL real, nunca `*`.**
5. **`jpa:` con mala indentación** hace que se ignoren `show-sql` y `format_sql` sin avisar.

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| El frontend recibe un error de CORS | Su origen no está en `CORS_ORIGINS` | Agrégalo exacto (puerto incluido, sin `/` final) |
| `LazyInitializationException` | Se leyó una relación fuera del service | Mapea a DTO **dentro** del service |
| Una configuración «no hace nada» | Nombre mal escrito | Revisa mayúsculas y la ruta completa |

## Para saber más

[Referencia 04: perfiles](../referencia/04-perfiles-spring.md) · [Referencia 20: configuración base](../referencia/20-configuracion-base-explicada.md)
