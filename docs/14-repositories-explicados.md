# 14. Repositories explicados

Guía para entender, línea por línea, las cuatro interfaces del paquete `repository/` y cómo Spring Data JPA convierte el **nombre de un método** en una consulta SQL. Léela de arriba hacia abajo: conceptos, reglas para escribir nombres, cada archivo y los errores que ya aparecieron.

Archivos que se explican (ruta `src/main/java/com/factory/tecnofactory/repository/`):

| Archivo | Entidad | Id | Sección |
| --- | --- | --- | --- |
| `TipoUsuarioRepository.java` | `TipoUsuario` | `Long` | [5.1](#51-tipousuariorepositoryjava) |
| `RegionRepository.java` | `Region` | `Integer` | [5.2](#52-regionrepositoryjava) |
| `ComunaRepository.java` | `Comuna` | `Integer` | [5.3](#53-comunarepositoryjava) |
| `UsuarioRepository.java` | `Usuario` | `Long` | [5.4](#54-usuariorepositoryjava) |

> Estado: Spring encontró las 4 interfaces (`Found 4 JPA repository interfaces`). Los tres errores de nombres de método que salieron durante la sesión del 18 oct se corrigieron en el código; **falta confirmar un arranque completo** con los archivos corregidos (ver [Cambios recientes](cambios-recientes.md)).

---

## 1. Conceptos básicos

### 1.1 Qué es un repository

Una entidad describe **cómo es** una fila. Un repository es lo que **busca y guarda** esas filas. Con Spring Data JPA no escribes la implementación: declaras una **interfaz** y Spring genera el código al arrancar.

```java
public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Long> {
    Optional<TipoUsuario> findByNombre(String nombre);
}
// No hay "class ... implements TipoUsuarioRepository" en ningún lado. Spring la crea solo.
```

### 1.2 Qué trae gratis `JpaRepository`

Con solo escribir `extends JpaRepository<Entidad, TipoDelId>` ya tienes estos métodos, sin declararlos:

| Método | Qué hace | SQL aproximado |
| --- | --- | --- |
| `save(entidad)` | Inserta si es nueva, actualiza si ya existe | `INSERT` o `UPDATE` |
| `findById(id)` | Busca por clave primaria. Devuelve `Optional` | `SELECT ... WHERE id = ?` |
| `findAll()` | Devuelve todas las filas como `List` | `SELECT ...` |
| `existsById(id)` | ¿Existe esa fila? | `SELECT ... LIMIT 1` |
| `count()` | Cuántas filas hay | `SELECT COUNT(*)` |
| `deleteById(id)` | Borra por clave primaria | `DELETE ... WHERE id = ?` |

### 1.3 Cuándo se valida

Spring revisa **al arrancar** que cada nombre de método corresponda a atributos reales de la entidad. Si no, la aplicación **no levanta** y el error aparece de inmediato. Es una ventaja: los errores de nombre no llegan a producción.

---

## 2. Glosario: cada palabra de un repository

| Elemento | Qué significa |
| --- | --- |
| `public interface X` | Una **interfaz** (no una clase): solo declara qué métodos existen |
| `extends JpaRepository<A, B>` | Hereda todos los métodos de la tabla 1.2. `A` = la **entidad**, `B` = el **tipo de su `@Id`** |
| `Optional<T>` | Una caja que puede traer un `T` o estar vacía. Obliga a decidir qué hacer si no se encontró nada, en vez de recibir un `null` que explote más tarde |
| `List<T>` | Lista de resultados. **Si no hay nada, viene vacía, nunca `null`** |
| `boolean` / `Boolean` | Respuesta sí/no. `Boolean` (con mayúscula) funciona; la costumbre es `boolean` porque un `exists` nunca devuelve `null` |
| `findBy...` | "Busca filas donde..." |
| `existsBy...` | "¿Existe alguna fila donde...?" |
| `import java.util.List` / `Optional` | Las dos clases de Java que usas en los tipos de retorno |
| No hace falta `@Repository` | Spring detecta solo las interfaces que extienden `JpaRepository` |

### 2.1 Los dos tipos entre `< >`

```java
JpaRepository<Comuna, Integer>
//            ^^^^^^  ^^^^^^^
//            entidad tipo del @Id de esa entidad
```

La regla: **el segundo tipo es el de la columna `@Id`.** Se mira la entidad, no el repository:

| Entidad | `@Id` | Tipo en el repository |
| --- | --- | --- |
| `TipoUsuario` | `Long id` | `Long` |
| `Region` | `int id` | `Integer` (la versión con mayúscula, porque los genéricos no admiten `int`) |
| `Comuna` | `int id` | `Integer` |
| `Usuario` | `Long id` | `Long` |

Si te equivocas, **no falla al arrancar**, pero falla la primera vez que llamas `findById(...)`. Fue el error que había en `ComunaRepository` y `RegionRepository` (decían `Long`) y ya está corregido.

---

## 3. Cómo se lee el nombre de un método

Spring parte el nombre en trozos, usando las **mayúsculas** como puntos de corte, y busca cada trozo como **atributo de la entidad Java**.

```
findByCorreo
└┬─┘└──┬──┘
 │     └─ atributo "correo" de Usuario      -> WHERE correo = ?
 └─ "busca filas donde"
```

### 3.1 Palabras que Spring entiende

| Palabra | Efecto | Ejemplo | SQL aproximado |
| --- | --- | --- | --- |
| `findBy` | Busca y devuelve las filas | `findByCorreo(String c)` | `WHERE correo = ?` |
| `findAllBy` | Igual que `findBy`; el `All` solo ayuda a leer | `findAllByActivo(Boolean a)` | `WHERE activo = ?` |
| `existsBy` | Devuelve `true` si hay al menos una fila | `existsByRun(Integer r)` | `SELECT ... WHERE run = ? LIMIT 1` |
| `countBy` | Cuenta las filas | `countByActivo(Boolean a)` | `SELECT COUNT(*) WHERE activo = ?` |
| `deleteBy` | Borra las filas | `deleteByCorreo(String c)` | `DELETE ... WHERE correo = ?` |
| `And` | Une dos condiciones | `findByNombreAndApellidos(String n, String a)` | `WHERE nombre = ? AND apellidos = ?` |
| `Or` | Una u otra | `findByCorreoOrRun(...)` | `WHERE correo = ? OR run = ?` |

Los parámetros del método se entregan **en el mismo orden** en que aparecen en el nombre.

### 3.2 Recorrer una relación

Cuando el atributo es otra entidad, se encadena con el nombre del atributo siguiente, **todo junto y con mayúsculas**:

```java
existsByTipoUsuarioNombre(String nombre)
//       ^^^^^^^^^^^ ^^^^^^
//       atributo    atributo dentro de TipoUsuario
//       "tipoUsuario" de Usuario
//
// Se lee: usuario.tipoUsuario.nombre = ?
// SQL:    SELECT ... FROM usuario u JOIN tipo_usuario t ON ... WHERE t.nombre_tipo_usuario = ?
```

Spring hace el `JOIN` solo. Esta es la consulta que se usará para saber si ya existe un administrador sin escribir SQL.

### 3.3 Qué tipo de retorno elegir

| Si la columna que buscas... | Retorna | Por qué |
| --- | --- | --- |
| es `UNIQUE` (correo, run, nombre de rol) | `Optional<Entidad>` | Hay 0 o 1 resultado |
| **no** es única (nombre, apellidos, el rol de un usuario) | `List<Entidad>` | Puede haber varios. Con `Optional` y dos filas, Spring lanza `IncorrectResultSizeDataAccessException` y **solo cuando ocurra el caso**, semanas después |
| solo quieres saber si existe | `boolean` | No necesitas la fila |
| son varias filas posibles | `List<Entidad>`, **no** `Optional<List<...>>` | La lista vacía ya significa "nada". Con `Optional<List>` tendrías dos formas de decir "nada" y tu código tendría que revisar ambas |

### 3.4 Los nombres se escriben contra los **atributos Java**, no las columnas

| | Entidad Java | Columna en la base |
| --- | --- | --- |
| Rol | `TipoUsuario.nombre` | `nombre_tipo_usuario` |
| Comuna | `Comuna.nombre` | `nombre_comuna` |

Por eso es `findByNombre`, **no** `findByNombreTipoUsuario` ni `findByNombreComuna`. La columna se llama distinto, pero eso a Spring Data no le importa: él mira los atributos de tu clase.

### 3.5 Mayúsculas y guiones bajos

- Cada palabra del nombre empieza con mayúscula: `findByCorreo`, no `findBycorreo`. Las mayúsculas son las costuras donde Spring corta.
- El guion bajo `_` tiene significado especial: separa explícitamente un nivel de relación (`TipoUsuario_Nombre`). Por eso conviene **no usar guiones bajos en los nombres de atributos** (ver [guía 13, 7.3](13-entidades-explicadas.md#73-nombres-con-guion-bajo-en-java)).

---

## 4. Por qué `findByNombreTipoUsuario` falla

Este fue un error de `TipoUsuarioRepository` y es el mismo patrón de los otros dos que viste en el log. Así se razona:

```java
Optional<TipoUsuario> findByNombreTipoUsuario(String nombre);   // fallaba
```

1. Spring corta por las mayúsculas: `Nombre` · `Tipo` · `Usuario`.
2. Busca `nombre` en `TipoUsuario`: **existe**, es un `String`.
3. Sigue y busca `tipoUsuario` **dentro de ese `String`**: un `String` no tiene eso.
4. Error: `No property 'tipoUsuario' found for type 'String'; Traversed path: TipoUsuario.nombre`.

Cómo leer la parte final: `Traversed path: TipoUsuario.nombre` es el camino que alcanzó a recorrer antes de atascarse. El texto antes de `found for type` es lo que no pudo encontrar. Corrección:

```java
Optional<TipoUsuario> findByNombre(String nombre);
```

Paso a paso para otros errores del mismo tipo: [guía 16](16-leer-errores-de-spring.md).

---

## 5. Cada archivo

### 5.1 `TipoUsuarioRepository.java`

```java
package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;          // sobra
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;                                    // sobra
import java.util.Optional;

public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Long> {

    Optional<TipoUsuario> findByNombre(String nombre);

}
```

| Línea | Qué significa |
| --- | --- |
| `package ...repository;` | Vive en el paquete `repository` |
| `import ...entity.TipoUsuario;` | La entidad con la que trabaja |
| `import ...JpaRepository;` | La interfaz de la que se hereda |
| `import java.util.Optional;` | Para el tipo de retorno |
| `public interface TipoUsuarioRepository` | Interfaz pública, con el mismo nombre que el archivo |
| `extends JpaRepository<TipoUsuario, Long>` | Entidad `TipoUsuario`; su `@Id` es `Long` |
| `Optional<TipoUsuario> findByNombre(String nombre);` | `WHERE nombre_tipo_usuario = ?`. `Optional` porque la columna es `UNIQUE`: a lo más una fila |

### 5.2 `RegionRepository.java`

```java
public interface RegionRepository extends JpaRepository<Region, Integer> {

    Optional<List<Region>> findByNombre(String nombre);
}
```

| Línea | Qué significa |
| --- | --- |
| `JpaRepository<Region, Integer>` | El `@Id` de `Region` es `int`: se escribe `Integer` |
| `Optional<List<Region>> findByNombre(String nombre);` | `WHERE nombre_region = ?`. **Funciona**, pero `Optional<List<...>>` sobra ([3.3](#33-qué-tipo-de-retorno-elegir)). Recomendado: `List<Region> findByNombre(String nombre);` |

El archivo trae además, comentadas, tres líneas que sobraron de `UsuarioRepository`. Bórralas.

### 5.3 `ComunaRepository.java`

```java
public interface ComunaRepository extends JpaRepository<Comuna, Integer> {

    Optional<List<Comuna>> findByNombre(String nombre);
}
```

Igual que el anterior: tipo del id `Integer`, y la misma recomendación de usar `List<Comuna>`. En comunas hay una razón real para `List`: `nombre_comuna` no es único en la base.

Imports que sobran: `Region` y `Usuario`.

### 5.4 `UsuarioRepository.java`

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByRun(Integer run);

    Boolean existsByRun(Integer run);
    Boolean existsByCorreo(String correo);
    Boolean existsByTipoUsuarioNombre(String nombre);

    Optional<List<Usuario>> findAllByTipoUsuarioNombre(String nombre);
}
```

| Método | SQL aproximado | Para qué se usará |
| --- | --- | --- |
| `Optional<Usuario> findByCorreo(String correo)` | `WHERE correo = ?` | Iniciar sesión (el correo es único) |
| `Optional<Usuario> findByRun(Integer run)` | `WHERE run = ?` | Buscar por RUN sin dígito (único) |
| `Boolean existsByRun(Integer run)` | `SELECT ... WHERE run = ? LIMIT 1` | Rechazar un registro con RUN repetido **antes** de que la base lo rechace |
| `Boolean existsByCorreo(String correo)` | ídem con `correo` | Rechazar un correo repetido |
| `Boolean existsByTipoUsuarioNombre(String nombre)` | `JOIN tipo_usuario ... WHERE nombre_tipo_usuario = ?` | **"¿Ya existe un administrador?"**: `existsByTipoUsuarioNombre("ADMINISTRADOR")` |
| `Optional<List<Usuario>> findAllByTipoUsuarioNombre(String nombre)` | `JOIN ... WHERE nombre_tipo_usuario = ?` | Listar usuarios de un rol. Mejor `List<Usuario>` |

Nota sobre `Boolean` y `boolean`: funcionan los dos. Si lo cambias a `boolean`, el código que lo llame no necesita ningún cambio.

### 5.5 Versión recomendada (con todo lo anterior aplicado)

```java
public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Long> {
    Optional<TipoUsuario> findByNombre(String nombre);
}

public interface RegionRepository extends JpaRepository<Region, Integer> {
    List<Region> findByNombre(String nombre);
}

public interface ComunaRepository extends JpaRepository<Comuna, Integer> {
    List<Comuna> findByNombre(String nombre);
}

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByRun(Integer run);
    List<Usuario> findAllByTipoUsuarioNombre(String nombre);

    boolean existsByRun(Integer run);
    boolean existsByCorreo(String correo);
    boolean existsByTipoUsuarioNombre(String nombre);
}
```

(Cada interfaz va en su propio archivo.)

---

## 6. Lo que sale en el log y no es un error

Al arrancar aparece esto, cuatro veces, y es normal:

```
INFO ... RepositoryConfigurationExtensionSupport : Spring Data Redis - Could not safely identify
store assignment for repository candidate interface ...ComunaRepository; If you want this
repository to be a Redis repository, consider annotating your entities with ... @RedisHash ...
```

Tu proyecto tiene **dos** módulos de Spring Data (JPA y Redis). Spring prueba cada repository con ambos y avisa, a nivel `INFO`, que no puede asignarlo a Redis. Termina asignándolo a JPA (`Found 4 JPA repository interfaces`, `Found 0 Redis repository interfaces`). Es solo ruido: no hay que hacer nada.

## 7. Errores de repositories que ya aparecieron

| Mensaje | Causa | Solución |
| --- | --- | --- |
| `No property 'comuna' found for type 'String'; Traversed path: Region.nombre` | Método `findBynombreComuna` en `RegionRepository`: se usó el nombre de la **columna** y con la `n` en minúscula | `findByNombre` |
| `No property 'comuna' found for type 'String'; Traversed path: Comuna.nombre` | Método `findByNombreComuna` en `ComunaRepository` | `findByNombre` |
| `No property 'tipoUsuario' found for type 'String'; Traversed path: TipoUsuario.nombre` | `findByNombreTipoUsuario` en `TipoUsuarioRepository` (previsto al revisar el código; se corrigió antes de verlo en el log) | `findByNombre` |
| `No property 'correoo' found for type 'Usuario'` | Error de tipeo en el nombre del método | Corregir el nombre |
| Falla al **llamar** `findById(...)` con un tipo raro | El segundo tipo de `JpaRepository<...>` no coincide con el `@Id` | Alinearlo ([2.1](#21-los-dos-tipos-entre--)) |
