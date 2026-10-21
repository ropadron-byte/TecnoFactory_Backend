# 16. Cómo leer un error de arranque de Spring

Método para encontrar el problema cuando la aplicación **no levanta**, con los tres errores reales del 18 oct 2026 como ejemplos. Complementa a [09-errores-comunes.md](09-errores-comunes.md) (tabla de síntomas) y a [12-logs.md](12-logs.md) (qué es el log y sus niveles). Esta guía enseña **cómo razonar**; las otras dos sirven para **consultar**.

| Sección | Contenido |
| --- | --- |
| [1](#1-el-método-en-4-pasos) | El método |
| [2](#2-el-orden-de-arranque-te-dice-dónde-falló) | Qué se ejecuta primero y qué significa cada línea sana |
| [3](#3-líneas-que-asustan-y-no-son-errores) | Lo que se puede ignorar |
| [4](#4-cómo-filtrar-el-log) | Comandos para no leer cientos de líneas |
| [5](#5-los-errores-reales-de-esta-sesión) | Los tres casos resueltos |
| [6](#6-patrones-para-reconocer-un-error-rápido) | Tabla de reconocimiento rápido |

---

## 1. El método en 4 pasos

### Paso 1: ve al final y confirma que falló

Las últimas líneas dicen `BUILD FAILURE`. Solo confirma que falló. **El motivo está más arriba.**

### Paso 2: busca el primer `ERROR` o `WARN` que mencione una `Exception`

```
WARN ... Exception encountered during context initialization - cancelling refresh attempt:
BeanCreationException: Error creating bean with name 'comunaRepository' ...
No property 'comuna' found for type 'String'; Traversed path: Comuna.nombre
```

Ya te dice **qué componente** falló (`comunaRepository`). El mismo mensaje se repite varias veces en el log, una por capa del framework. No son varios errores: es el mismo.

### Paso 3: baja a los `Caused by:` y lee el que nombra tu código

Los `Caused by:` van de lo general a lo específico. El útil es el que menciona **tu clase o tu método**:

```
Caused by: QueryCreationException: Cannot create query for method
[ComunaRepository.findByNombreComuna(java.lang.String)]; No property 'comuna' found ...
```

Aquí está el método exacto: `findByNombreComuna`.

### Paso 4: ignora las líneas `at org.springframework...`

Son el recorrido interno del framework. Solo busca en ellas tu paquete (`com.factory.tecnofactory`). Si solo aparece `TecnoFactoryApplication.main`, el error **no está en una línea de tu código**, sino en una **configuración o declaración** que Spring leyó (una anotación, el nombre de un método). La pista la da el mensaje.

---

## 2. El orden de arranque te dice dónde falló

Spring arranca en etapas. La **última línea sana** antes del error indica en qué etapa se detuvo y descarta todas las anteriores.

| Orden | Etapa | Línea sana que verás | Si falla aquí, revisa |
| --- | --- | --- | --- |
| 1 | Perfil | `The following 1 profile is active: "local"` | Que `SPRING_PROFILES_ACTIVE` esté definido |
| 2 | Repositories (escaneo) | `Found 4 JPA repository interfaces` | Que existan las interfaces |
| 3 | Conexión a la base | `HikariPool-1 - Start completed` | `DB_URL`, `DB_USER`, `DB_PASSWORD`; que MySQL esté `healthy` |
| 4 | Flyway | `Successfully applied N migrations` o `up to date` | Las migraciones `V1__`, `V2__`... |
| 5 | **Hibernate y entidades** | `Initialized JPA EntityManagerFactory for persistence unit 'default'` | Las **entidades** (`@Table`, `@Column`, tipos): errores `Schema-validation` |
| 6 | **Repositories (creación)** | *(no imprime línea propia; el error aparece como `Error creating bean with name 'xRepository'`)* | Los **nombres de método** de los repositories |
| 7 | Servidor web | `Tomcat started on port 8080` | Que el puerto esté libre |
| 8 | Final | `Started TecnoFactoryApplication in X seconds` | — |

Aplicado a tus logs:

- **Primer error:** el log llegó hasta la etapa 4 (`Successfully applied 3 migrations`) y falló con `Schema validation` **antes** de `Initialized JPA EntityManagerFactory`. Etapa 5: el problema está en una **entidad**.
- **Segundo y tercer error:** el log mostró `Initialized JPA EntityManagerFactory` y **después** falló con `Error creating bean with name 'regionRepository'`. Etapa 6: las entidades estaban bien (¡incluida `Usuario` con su `CHAR(1)`!) y el problema estaba en un **repository**.

Con solo mirar dónde se cortó, ya sabes en qué carpeta buscar.

---

## 3. Líneas que asustan y no son errores

| Línea | Nivel | Qué es |
| --- | --- | --- |
| `Spring Data Redis - Could not safely identify store assignment for repository candidate ...` | `INFO` | Spring prueba cada repository con JPA y con Redis; avisa que no es de Redis. Termina asignándolo a JPA. Se repite 4 veces |
| `Multiple Spring Data modules found, entering strict repository configuration mode` | `INFO` | Aviso de que hay JPA y Redis en el proyecto |
| `No LoadTimeWeaver setup: ignoring JPA class transformer` | `INFO` | Mejora opcional de Hibernate que no se usa |
| `HHH000489: No JTA platform available` | `INFO` | JTA son transacciones distribuidas; no se usan |
| `Hibernate is in classpath; If applicable, HQL parser will be used.` | `INFO` | Informativa |
| `Using generated security password: ...` | `WARN` | Clave temporal de Spring Security. Inofensiva mientras todo sea `permitAll` ([guía 15, 3.6](15-seguridad-codigo-explicado.md#36-dos-detalles-que-sorprenden)) |

Regla general: las líneas `INFO` son noticias; solo `ERROR`, y los `WARN` que mencionen `Exception`, merecen atención.

---

## 4. Cómo filtrar el log

Para no recorrer cientos de líneas:

```bash
./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by"
```

| Parte | Qué hace |
| --- | --- |
| `2>&1` | Une los mensajes de error con la salida normal, para filtrar ambos |
| `\|` | Pasa la salida del primer comando al segundo |
| `grep -E "..."` | Deja solo las líneas que contengan alguna de esas palabras (`-E` permite el `\|` como "o") |

Tu error completo cabe en unas 6 líneas con este filtro.

Para guardar el log completo y buscar después:

```bash
./mvnw spring-boot:run > arranque.log 2>&1
grep -n "Caused by" arranque.log
```

(Si lo haces, `*.log` ya está en tu `.gitignore`.)

---

## 5. Los errores reales de esta sesión

### 5.1 Error 1: `missing column [id_comuna] in table [region]`

```
Schema validation: missing column [id_comuna] in table [region]
                                   ^^^^^^^^^^           ^^^^^^
                                   columna que pide     tabla donde la busca
```

**Etapa:** 5 (entidades).

**Cómo se razona.** Hibernate lee una clase y busca la columna `id_comuna` en la tabla `region`. Esa tabla solo tiene `id_region` y `nombre_region`: la columna no falta en `region`, **la clase `Comuna` estaba apuntando a la tabla equivocada**. La pista es la palabra `table`: cuando el error nombra una tabla que no esperabas, revisa el `@Table` de la entidad a la que pertenece esa columna.

**Causa.** `Comuna.java` decía `@Table(name = "region")` (copiado de `Region`). Dos entidades apuntaban a la misma tabla.

**Solución.** `@Table(name = "comuna")`.

**Lección.** Al copiar una entidad para crear otra, revisa `@Table` **y** cada `@Column(name = ...)`. Además: el primer diagnóstico de este error apuntó a `Region.java`, y no era ahí; **el mensaje nombra la tabla, no la clase culpable**. Abrir los archivos reales antes de diagnosticar evita perder tiempo.

### 5.2 Error 2: `No property 'comuna' found for type 'String'; Traversed path: Region.nombre`

```
Cannot create query for method [RegionRepository.findBynombreComuna(java.lang.String)];
No property 'comuna' found for type 'String'; Traversed path: Region.nombre
```

**Etapa:** 6 (repositories).

**Cómo se razona.** Léelo de derecha a izquierda:

1. `Traversed path: Region.nombre`: Spring recorrió la entidad `Region` y llegó al atributo `nombre`.
2. `for type 'String'`: ese atributo es un `String`.
3. `No property 'comuna' found`: Spring buscó algo llamado `comuna` **dentro de ese `String`**, y un `String` no tiene eso.

¿Por qué lo buscó? Porque el método se llamaba `findBy**nombreComuna**`. Spring lo parte en `nombre` (existe) y `Comuna`, que interpreta como un atributo **dentro de** `nombre`. Pero el atributo se llama `nombre`, no `nombreComuna`. `nombre_comuna` es el nombre de la **columna**; **los métodos se escriben contra los atributos de Java**.

**Solución.** `findByNombre`. Además, ese método tenía la `n` en minúscula (`findBynombre...`): las mayúsculas son las costuras donde Spring corta el nombre.

### 5.3 Error 3: `No property 'comuna' found for type 'String'; Traversed path: Comuna.nombre`

Es **el mismo error** en `ComunaRepository.findByNombreComuna`. Apareció después de corregir el anterior porque **Spring se detiene en el primer repository que falla**. Los errores salen de a uno: tras corregir uno, vuelve a arrancar y espera el siguiente.

### 5.4 Un cuarto error evitado: `findByNombreTipoUsuario`

Al revisar el código se vio el mismo patrón en `TipoUsuarioRepository` y se corrigió antes de que apareciera en el log. Se razona igual: `Nombre` existe, `TipoUsuario` se busca dentro de un `String` y falla. Detalle en [guía 14, sección 4](14-repositories-explicados.md#4-por-qué-findbynombretipousuario-falla).

### 5.5 Qué errores se ven y cuáles quedan ocultos

| Se ve **al arrancar** | Se ve **solo al usarlo** |
| --- | --- |
| Tabla o columna inexistente | `JpaRepository<Entidad, Long>` con un `@Id` que es `Integer` (falla al llamar `findById`) |
| Tipo de columna incompatible | `Optional<Entidad>` sobre un campo que no es único (falla si hay dos filas) |
| Nombre de método que no existe en la entidad | `LazyInitializationException` al leer una relación `LAZY` fuera de una transacción |

El segundo grupo es el más peligroso: el arranque limpio **no garantiza** que todo esté bien. Por eso conviene escribir pruebas (fase 8) y revisar los tipos con la [guía 14](14-repositories-explicados.md#21-los-dos-tipos-entre--).

---

## 6. Patrones para reconocer un error rápido

| El mensaje dice | Es un problema de | Mira |
| --- | --- | --- |
| `Schema-validation: missing table [x]` | `@Table` | La entidad que debería mapear a `x` |
| `Schema-validation: missing column [c] in table [t]` | `@Column(name = ...)` o `@Table` | La entidad de la tabla `t`: ¿el nombre existe en `V1`? ¿el `@Table` es el correcto? |
| `Schema-validation: wrong column type` | Tipo Java o falta de `@JdbcTypeCode` | El tipo del atributo frente al de `V1` |
| `No property 'x' found for type 'T'; Traversed path: E.a` | Nombre de método de repository | Usa los **atributos Java** de `E`, no columnas |
| `Error creating bean with name 'xRepository'` | Un método de ese repository | Busca `Cannot create query for method [...]` más abajo |
| `Could not resolve placeholder 'X'` | Variable de entorno ausente | Define `X` (Spring no lee el `.env`) |
| `Access denied for user '...'` | Credenciales de MySQL | `DB_USER` y `DB_PASSWORD` |
| `Communications link failure` | MySQL no corre | `docker compose ps` |
| `Port 8080 was already in use` | Otra instancia corriendo | Detenla |
| `Can't configure requestMatchers after anyRequest` | `SecurityConfig` | Mueve `anyRequest()` al final |

Cuando un error no esté en esta tabla: pega solo las líneas que `grep` muestra (sección 4), lee el mensaje de derecha a izquierda y escribe tu interpretación antes de buscar la respuesta. Es la mejor forma de aprender a leerlos.
