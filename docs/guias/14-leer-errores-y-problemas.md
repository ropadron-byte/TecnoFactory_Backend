# Guía 14. Leer errores y resolver problemas

> **En una frase:** un error de Spring parece enorme, pero casi siempre la causa está en **una línea**; hay un método de 4 pasos para encontrarla.

## El método de 4 pasos

| Paso | Qué hacer |
| --- | --- |
| 1 | Ve al final y confirma que falló (`BUILD FAILURE`). **El motivo está más arriba** |
| 2 | Busca el primer `ERROR` o `WARN` que mencione una `Exception`: te dice **qué componente** falló |
| 3 | Baja a los `Caused by:` y lee el que **nombra tu código** (tu clase o tu método) |
| 4 | Ignora las líneas `at org.springframework...`: solo busca tu paquete `com.factory.tecnofactory` |

El mismo mensaje se repite varias veces en el log: **no son varios errores**, es el mismo visto desde cada capa.

Para filtrar: `./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by"`.

## Líneas que asustan y no son errores

| Línea | Qué es |
| --- | --- |
| `WARNING: A Java agent has been loaded dynamically` (Mockito) | Habitual en Java 21; inofensivo |
| `Using generated security password` | Desaparece al existir `UserDetailsServiceImpl` |
| `open-in-view` en el log antiguo | Ya está en `false` |

## Reconocer un error rápido

| El mensaje dice | Es un problema de | Mira |
| --- | --- | --- |
| `Schema-validation: missing table [x]` | `@Table` | La entidad de esa tabla |
| `Schema-validation: missing column [c] in table [t]` | `@Column(name)` o `@Table` | La entidad de `t` frente a `V1` |
| `Schema-validation: wrong column type` | Tipo Java | El tipo frente a `V1`; quizá falte `@JdbcTypeCode` |
| `No property 'x' found for type 'T'` | Método de repository | Usa **atributos Java**, no columnas |
| `Could not resolve placeholder 'X'` | Variable ausente | Defínela (Spring no lee el `.env`) |
| `Access denied for user` | Credenciales de MySQL | `DB_USER` y `DB_PASSWORD` |
| `Communications link failure` | MySQL apagado | `docker compose ps` |
| `Port 8080 was already in use` | Otra instancia | Detenla |
| `Can't configure requestMatchers after anyRequest` | `SecurityConfig` | `anyRequest()` va al final |
| `WeakKeyException` | `JWT_SECRET` corto | Mínimo 32 caracteres |
| `No qualifying bean of type '...JwtService'` en un test | Falta un `@MockitoBean` | Agrégalo |
| `UnnecessaryStubbingException` | Un `when(...)` que el test no usa | Quítalo |
| `Migration checksum mismatch` | Editaste una migración aplicada | Nueva versión, o `down -v` en desarrollo |

Cuando un error no esté aquí: pega solo las líneas que muestra `grep`, léelo de derecha a izquierda y **escribe tu interpretación antes de buscar la respuesta**. Es la mejor forma de aprender.

## Si se complica

1. ¿Qué cambié justo antes? El último cambio casi siempre es el culpable.
2. ¿Falla un test o la app? Un test aísla el problema.
3. ¿Es el nombre de algo? Entidades, columnas, métodos de repository y propiedades del YAML fallan por **un nombre mal escrito**.

## Para saber más

[Referencia 16: leer errores, con los casos reales](../referencia/16-leer-errores-de-spring.md) · [Referencia 09: catálogo de errores comunes](../referencia/09-errores-comunes.md) · [Personal 03: algo falla](../personal/03-algo-falla.md)
