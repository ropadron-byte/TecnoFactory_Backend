# 09. Reglas y trampas

Las cosas que más salen caras si se olvidan. Cada una viene de algo que ya pasó en este proyecto.

## Reglas de oro

| # | Regla | Por qué |
| --- | --- | --- |
| 1 | **La base manda**: primero la migración, luego la entidad | `validate` rechaza lo que no calce |
| 2 | **Nunca edites una migración ya aplicada** | Flyway guarda su checksum; usa una `V` nueva |
| 3 | **Nada secreto en Git** | El historial no se borra |
| 4 | **Nunca devuelvas una entidad**: usa Response | Evita filtrar campos y errores `LAZY` |
| 5 | **El Request no tiene `id`, `activo` ni rol** | Evita la asignación masiva |
| 6 | **El controller no tiene lógica** | Se prueba y reutiliza en el service |
| 7 | **Mapea a DTO dentro de la transacción** | `open-in-view: false` |
| 8 | **Lanza excepciones propias**; no captures en el controller | El handler responde |
| 9 | **Lo específico antes que lo general** en `SecurityConfig` | La primera regla que coincide gana |
| 10 | **Ocultar un botón no es seguridad** | La API debe rechazarlo |
| 11 | **Toda ruta nueva decide su rol** | Si no, queda «cualquier sesión» |
| 12 | **Un test que nunca falla no sirve** | Rómpelo a propósito para comprobarlo |

## Trampas conocidas

| Trampa | Qué pasa | Cómo evitarla |
| --- | --- | --- |
| Escribir mal una propiedad del YAML | Spring la **ignora en silencio** (pasó con la zona horaria) | Usa el nombre exacto y compruébalo |
| Métodos de repository con nombre de columna | `No property 'x' found` | Usa el atributo Java |
| `@Column(name)` omitido | `validate` busca otro nombre | Pon siempre el nombre real |
| Campo `contrasenia` en un DTO | Sale en el JSON | El DTO usa `clave` y el Response no la incluye |
| `GET /api/x/abc` con `Long` | 500 si falta el handler | Ya cubierto (`MethodArgumentTypeMismatch`) |
| `JwtAuthFilter` con `@Component` | Los `@WebMvcTest` lo cargan y fallan | Créalo en `SecurityConfig` |
| `@MockitoBean` faltante en un test de controller | `No qualifying bean` | Mockea `UsuarioService`, `JwtService`, `UserDetailsService` |
| Pegar `Bearer` en Swagger | 401 | Pega solo el token |
| `unique = true` en `Region` y `Comuna` | Desajuste con la base | Pendiente de quitar |
| Archivo de test en carpeta equivocada | Confunde | `security/` para `JwtServiceTest` y `UserDetailsServiceImplTest` |
| Copia de un archivo de código dentro de `src/test` | Compila dos veces | Bórrala |
| Comentarios `// NUEVO` | Ruido | Pendiente de borrar (7 en `UsuarioService`) |
| Confiar en que «compila» | Maven no se ejecutó en este entorno | Corre tú `./mvnw test` |

## Cómo trabajar con ayuda de la IA

1. Dile **qué cambiaste** y pega el error exacto (solo las líneas útiles).
2. Pide que **explique el porqué**, no solo el código.
3. Prueba siempre tú: quien escribe aquí **no puede compilar ni ejecutar** tu proyecto.
4. Después de cada cambio, compara el ZIP o el `git diff` para ver que se aplicó lo previsto.

## Para saber más

[Guía 14](../guias/14-leer-errores-y-problemas.md) · [Referencia 09: errores comunes](../referencia/09-errores-comunes.md) · [Guía 16: hoja de ruta](../guias/16-hoja-de-ruta.md)
