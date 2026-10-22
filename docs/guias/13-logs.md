# Guía 13. Logs

> **En una frase:** el log es el diario de la aplicación; lo escribes con `log.info(...)` y lo lees en la consola para saber qué pasó.

## La idea

Un log es la **bitácora de un barco**: cada línea dice cuándo, quién y qué ocurrió. Cuando algo falla, es lo primero que se lee.

## Cómo se escribe

1. Pon `@Slf4j` sobre la clase (Lombok crea la variable `log`).
2. Escribe: `log.info("Usuario registrado con el correo {}", correo);`

**Usa `{}`**, no concatenes: es más rápido y limpio. Para una excepción, pásala **al final**: `log.error("Falló el registro de {}", correo, ex);`

## Los niveles

| Nivel | Cuándo | Ejemplo |
| --- | --- | --- |
| `DEBUG` | Detalle para desarrollar | `Buscando producto 5` |
| `INFO` | Hechos normales importantes | `Boleta 120 creada` |
| `WARN` | Algo raro, no un fallo | `Intento de login fallido` |
| `ERROR` | Algo falló | `Error no controlado en /api/...` |

Al fijar un nivel se muestran ese y los mayores.

| Perfil | Librerías | Tu código |
| --- | --- | --- |
| `local` | `INFO` | `DEBUG` |
| `prod` | `WARN` | `INFO` |

## Dónde verlo

| Cómo ejecutas | Dónde |
| --- | --- |
| IntelliJ | Pestaña **Run** |
| Terminal | La misma terminal |
| MySQL en Docker | `docker compose logs -f db` |

Para buscar un error: `./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by"`.

## Qué NO escribir nunca

| No registrar | Motivo |
| --- | --- |
| Contraseñas (ni hasheadas) | Quien lea el log las obtiene |
| Tokens JWT completos | Permiten suplantar al usuario |
| `JWT_SECRET` o cualquier clave del `.env` | Es un secreto |
| RUN, teléfono o dirección completos | Datos personales |

Un log puede copiarse a otro servidor, pegarse en un chat o subirse a Git: **escribe pensando en que cualquiera podría leerlo.**

## Reglas que no hay que olvidar

1. **Un error esperado (404, 409) no necesita `log.error`.**
2. **El log de un fallo de login no lleva correo ni clave** (`Intento de login fallido`).
3. **`show-sql` imprime en la consola, no en el archivo de log.**
4. **En `local` el log ya se guarda en `logs/tecnofactory.log`** (10 MB por archivo, 7 archivos). `logs/` y `*.log` deben estar en `.gitignore`.

## Para saber más

[Referencia 12: logs, completa](../referencia/12-logs.md) · [Guía 14: leer errores](14-leer-errores-y-problemas.md)
