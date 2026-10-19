# 13. Entidades explicadas

Guía para entender, línea por línea, las cuatro entidades que existen hoy en `entity/`. Está pensada para leerse de arriba hacia abajo: primero los conceptos, luego un glosario de cada anotación, después cada archivo y al final las observaciones sobre el código actual.

Archivos que se explican (ruta `src/main/java/com/factory/tecnofactory/entity/`):

| Archivo | Tabla | Qué tiene de particular | Sección |
| --- | --- | --- | --- |
| `TipoUsuario.java` | `tipo_usuario` | La más simple: id autoincremental y un nombre | [3](#3-tipousuariojava) |
| `Region.java` | `region` | Id **asignado a mano** (sin `@GeneratedValue`) | [4](#4-regionjava) |
| `Comuna.java` | `comuna` | Primera relación `@ManyToOne` (hacia `Region`) | [5](#5-comunajava) |
| `Usuario.java` | `usuario` | `CHAR(1)`, fecha, booleano con valor inicial y **dos** relaciones | [6](#6-usuariojava) |

> Estado: las cuatro entidades **pasaron la validación de Hibernate** (`ddl-auto: validate`) contra tu MySQL 8.4. Se vio en el log del 18 oct: `Initialized JPA EntityManagerFactory for persistence unit 'default'`. Eso incluye `Usuario` con su `CHAR(1)`. Las observaciones de la [sección 7](#7-observaciones-sobre-el-código-actual) son mejoras y riesgos futuros, no errores de arranque.

---

## 1. Conceptos básicos

### 1.1 Qué es una entidad

Una **entidad** es una clase Java que representa **una fila** de una tabla. Cada atributo corresponde a una columna y cada objeto creado a partir de la clase corresponde a una fila.

```
Tabla tipo_usuario                         Objetos Java
+-----------------+---------------------+  
| id_tipo_usuario | nombre_tipo_usuario |    new TipoUsuario()  -> id=1, nombre="ADMINISTRADOR"
+-----------------+---------------------+    new TipoUsuario()  -> id=2, nombre="VENDEDOR"
| 1               | ADMINISTRADOR       |    new TipoUsuario()  -> id=3, nombre="CLIENTE"
| 2               | VENDEDOR            |
| 3               | CLIENTE             |
+-----------------+---------------------+
```

### 1.2 Quién es quién: JPA, Hibernate y Jakarta

| Nombre | Qué es | Dónde lo ves en tu código |
| --- | --- | --- |
| **JPA** (Jakarta Persistence) | Un **estándar**: define las anotaciones (`@Entity`, `@Id`...) y las reglas | `import jakarta.persistence.*;` |
| **Hibernate** | La **implementación** que lee esas anotaciones y habla con MySQL | `import org.hibernate...` (anotaciones propias, fuera del estándar) |
| **Spring Data JPA** | Capa que genera los repositories encima de JPA (guía [14](14-repositories-explicados.md)) | `extends JpaRepository` |
| **Jakarta** | El nombre actual del espacio de Java EE. Antes era `javax.*` | Usa siempre `jakarta.persistence`, nunca `javax.persistence` |

Regla práctica: si el `import` empieza con `jakarta.persistence`, es del estándar; si empieza con `org.hibernate`, solo funciona con Hibernate.

### 1.3 Qué comprueba `ddl-auto: validate`

En `application.yaml` tienes `ddl-auto: validate`. Al arrancar, Hibernate compara **cada entidad** con **su tabla**. Los errores aparecen como `SchemaManagementException`.

| Lo que `validate` **sí** comprueba | Lo que **no** comprueba |
| --- | --- |
| Que la tabla exista (`missing table [x]`) | Que `unique = true` exista como restricción en la base |
| Que cada columna exista (`missing column [x]`) | Que `nullable = false` coincida con `NOT NULL` |
| Que el tipo sea compatible (`wrong column type`) | Que `length` coincida con el largo real del `VARCHAR` |
| | Las claves foráneas y los `CHECK` |

Consecuencia importante: **en este proyecto el esquema lo crea Flyway (`V1`), no Hibernate.** Por eso `unique`, `nullable` y `length` dentro de `@Column` funcionan como **documentación**: no crean nada y no se verifican. La fuente de verdad es siempre `V1__esquema_inicial.sql`. Si la entidad dice una cosa y `V1` otra, manda `V1`, y la entidad hay que corregirla (ver [sección 7](#7-observaciones-sobre-el-código-actual)).

### 1.4 Convención de nombres

| En Java | En la base | Cómo se conectan |
| --- | --- | --- |
| Clase `TipoUsuario` | Tabla `tipo_usuario` | `@Table(name = "tipo_usuario")` |
| Atributo `nombre` | Columna `nombre_tipo_usuario` | `@Column(name = "nombre_tipo_usuario")` |
| Atributo `fechaNacimiento` | Columna `fecha_nacimiento` | Spring Boot convierte `camelCase` a `snake_case` solo; igual se escribe el nombre explícito |

En este proyecto **todas las columnas llevan el nombre explícito**, aunque Spring pudiera deducirlo. Así lo que se lee en Java es exactamente lo que dice `V1`.

---

## 2. Glosario: cada anotación y cada palabra

Todo lo que aparece en las cuatro entidades, una sola vez. Las secciones siguientes remiten aquí.

### 2.1 Anotaciones de JPA (`jakarta.persistence`)

| Anotación | Qué significa |
| --- | --- |
| `@Entity` | "Esta clase se guarda en la base de datos". Sin ella Hibernate ignora la clase |
| `@Table(name = "x")` | Nombre de la tabla. Sin `name`, Hibernate deduciría el nombre de la clase |
| `@Id` | Marca el atributo que es la **clave primaria** |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | "El id lo genera la base" (`AUTO_INCREMENT`). Hibernate hace el `INSERT` sin id y luego lee el que MySQL asignó |
| `@Column(name = "x")` | Une el atributo con la columna `x` |
| `nullable = false` (dentro de `@Column`) | La columna es `NOT NULL`. Documenta, no verifica |
| `unique = true` (dentro de `@Column`) | La columna es `UNIQUE`. Documenta, no verifica |
| `length = 15` (dentro de `@Column`) | Largo del texto. Documenta; en `CHAR(1)` ayuda a describir el tipo |
| `@ManyToOne` | "Muchas filas de esta tabla apuntan a **una** fila de otra". Va en el lado que tiene la clave foránea |
| `fetch = FetchType.LAZY` | Carga **perezosa**: el objeto relacionado no se consulta hasta que se usa |
| `optional = false` | La relación es obligatoria: no puede ser `null` (equivale a la FK `NOT NULL`) |
| `@JoinColumn(name = "x")` | Nombre de la **columna de clave foránea** en esta tabla |

### 2.2 Anotaciones de Hibernate (`org.hibernate`)

| Elemento | Qué significa |
| --- | --- |
| `@JdbcTypeCode(SqlTypes.CHAR)` | Obliga a Hibernate a tratar el atributo como `CHAR` de SQL. Ver [sección 6.3](#63-explicación-de-jdbctypecodesqltypeschar) |
| `SqlTypes.CHAR` | Constante con el código JDBC del tipo `CHAR`. Es el valor que `@JdbcTypeCode` recibe |

### 2.3 Anotaciones de Lombok

Lombok genera código en la compilación, para que no lo escribas tú. No ves el código generado, pero existe.

| Anotación | Qué genera | Por qué se usa aquí |
| --- | --- | --- |
| `@Getter` | Un `getX()` por cada atributo | Hibernate y tú leen los valores |
| `@Setter` | Un `setX(valor)` por cada atributo | Hibernate y tú los modifican |
| `@NoArgsConstructor` | Un constructor **sin parámetros** | **JPA lo exige**: Hibernate crea los objetos vacíos y luego los rellena |

**Por qué no `@Data`:** `@Data` agrega además `equals`, `hashCode` y `toString` que recorren **todos** los atributos. En entidades con relaciones eso puede disparar consultas inesperadas o bucles infinitos, y en `Usuario` imprimiría la contraseña (el hash) en cualquier `log`. Por eso solo se usan estas tres.

### 2.4 Tipos de Java y su par en MySQL

| Java | MySQL | Notas |
| --- | --- | --- |
| `Long` | `BIGINT` | Ids autoincrementales. Con mayúscula porque un objeto nuevo aún no tiene id y debe poder valer `null` |
| `Integer` / `int` | `INT` | `Integer` puede ser `null`; `int` nunca (vale `0` por defecto). Ver [7.4](#74-int-frente-a-integer) |
| `String` | `VARCHAR(n)` / `CHAR(n)` | `CHAR` necesita `@JdbcTypeCode` |
| `Boolean` | `BOOLEAN` (en MySQL es `TINYINT(1)`) | Con mayúscula, para poder representar `null` |
| `LocalDate` | `DATE` | Solo fecha, sin hora. Es la clase moderna (`java.time`); evita `java.util.Date` |

### 2.5 Importaciones

| `import` | Para qué |
| --- | --- |
| `jakarta.persistence.*` | El `*` importa **todas** las clases del paquete: `@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`, `FetchType`, `GenerationType`... |
| `lombok.Getter`, `lombok.Setter`, `lombok.NoArgsConstructor` | Las tres anotaciones de Lombok |
| `org.hibernate.annotations.JdbcTypeCode` | La anotación de Hibernate |
| `org.hibernate.type.SqlTypes` | La clase con las constantes de tipos |
| `java.time.LocalDate` | El tipo de fecha |

---

## 3. `TipoUsuario.java`

**Qué es:** los roles del sistema (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`). Se cargan en `V2`.

**Tabla (de `V1`):**

```sql
CREATE TABLE tipo_usuario (
    id_tipo_usuario BIGINT AUTO_INCREMENT NOT NULL,
    nombre_tipo_usuario VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT tipo_usuario_PK PRIMARY KEY (id_tipo_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Código:**

```java
package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_usuario")
@Getter
@Setter
@NoArgsConstructor
public class TipoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_usuario")
    private Long id;

    @Column(name = "nombre_tipo_usuario", nullable = false, unique = true)
    private String nombre;
}
```

**Línea por línea:**

| Línea | Qué significa |
| --- | --- |
| `package com.factory.tecnofactory.entity;` | La clase vive en el paquete `entity`. La ruta de carpetas debe coincidir |
| `import jakarta.persistence.*;` | Trae las anotaciones de JPA ([2.5](#25-importaciones)) |
| `import lombok.*` (tres líneas) | Trae las anotaciones de Lombok |
| `@Entity` | Hibernate administra esta clase |
| `@Table(name = "tipo_usuario")` | Se guarda en la tabla `tipo_usuario` |
| `@Getter` `@Setter` | Genera `getId()`, `setId()`, `getNombre()`, `setNombre()` |
| `@NoArgsConstructor` | Genera `public TipoUsuario() {}`, que JPA exige |
| `public class TipoUsuario {` | Una clase `public` por archivo, con el mismo nombre del archivo |
| `@Id` | `id` es la clave primaria |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | El valor lo pone MySQL con `AUTO_INCREMENT`. Si cambiaras la estrategia, Hibernate intentaría generar ids por su cuenta y chocaría con tu tabla |
| `@Column(name = "id_tipo_usuario")` | El atributo se llama `id`, la columna `id_tipo_usuario`: hay que decirlo |
| `private Long id;` | `BIGINT` ↔ `Long`. `private` porque se accede con getter y setter |
| `@Column(name = "nombre_tipo_usuario", nullable = false, unique = true)` | Columna `NOT NULL UNIQUE`, igual que en `V1` |
| `private String nombre;` | El nombre del rol. En Java se llama `nombre`; **los métodos de repository usan este nombre**, no el de la columna ([guía 14](14-repositories-explicados.md#4-por-qué-findbynombretipousuario-falla)) |

---

## 4. `Region.java`

**Qué es:** las 16 regiones de Chile. Se cargan en `V2` con el número de región como id.

**Tabla:**

```sql
CREATE TABLE region (
    id_region INT NOT NULL,
    nombre_region VARCHAR(255) NOT NULL,
    CONSTRAINT region_PK PRIMARY KEY (id_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Código (sin los comentarios antiguos que hay al final del archivo):**

```java
@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
public class Region {

    @Id
    @Column(name = "id_region")
    private int id;

    @Column(name = "nombre_region", nullable = false, unique = true)
    private String nombre;
}
```

**Lo nuevo respecto a `TipoUsuario`:**

| Línea | Qué significa |
| --- | --- |
| `@Id` sin `@GeneratedValue` | El id **no es autoincremental**: lo asignas tú. En `V2` cada región se inserta con su número (1 a 16). Si pusieras `@GeneratedValue`, Hibernate ignoraría el id que le entregues |
| `@Column(name = "id_region")` | `INT` en la base |
| `private int id;` | `INT` ↔ `int`. Ver [7.4](#74-int-frente-a-integer) |
| `unique = true` en `nombre_region` | ⚠ **`V1` no declara `UNIQUE` en esta columna.** Ver [7.2](#72-unique--true-en-nombres-de-region-y-comuna) |

---

## 5. `Comuna.java`

**Qué es:** las 346 comunas. Cada una pertenece a una región. El id es el Código Único Territorial (CUT).

**Tabla:**

```sql
CREATE TABLE comuna (
    id_comuna INT NOT NULL,
    nombre_comuna VARCHAR(255) NOT NULL,
    region_id_region INT NOT NULL,
    CONSTRAINT comuna_PK PRIMARY KEY (id_comuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- La clave foránea hacia region se agrega en V1 con ALTER TABLE.
```

**Código:**

```java
@Entity
@Table(name = "comuna")
@Getter
@Setter
@NoArgsConstructor
public class Comuna {

    @Id
    @Column(name = "id_comuna")
    private int id;

    @Column(name = "nombre_comuna", nullable = false, unique = true)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id_region", nullable = false)
    private Region region;
}
```

**Lo nuevo (la relación):**

| Línea | Qué significa |
| --- | --- |
| `@Table(name = "comuna")` | ⚠ Si aquí dijera `"region"`, dos entidades apuntarían a la misma tabla y el arranque fallaría con `missing column [id_comuna] in table [region]`. Fue el primer error real del proyecto ([guía 16](16-leer-errores-de-spring.md#51-error-1-missing-column-id_comuna-in-table-region)) |
| `@ManyToOne` | "Muchas comunas pertenecen a **una** región". Va en `Comuna` porque la clave foránea (`region_id_region`) está en la tabla `comuna` |
| `fetch = FetchType.LAZY` | La región **no** se consulta hasta que alguien llame a `getRegion().getNombre()`. Por defecto `@ManyToOne` es `EAGER` (siempre carga) y eso genera consultas de más |
| `optional = false` | Una comuna sin región no existe. Va junto con `nullable = false` de abajo: uno es para JPA y el otro para la comprobación del esquema |
| `@JoinColumn(name = "region_id_region", nullable = false)` | Nombre de la columna FK. Es el nombre **real** de `V1` |
| `private Region region;` | Fíjate que en Java el atributo es un **objeto** `Region`, no un número. Hibernate convierte entre el objeto y el id al leer y guardar |

```
Comuna (Java)                       tabla comuna
 id      ------------------------>  id_comuna
 nombre  ------------------------>  nombre_comuna
 region  (objeto Region)  -------->  region_id_region  --FK-->  region.id_region
```

---

## 6. `Usuario.java`

**Qué es:** las personas que usan el sistema. Es la entidad más delicada porque junta todo lo anterior.

**Tabla:**

```sql
CREATE TABLE usuario (
    id_usuario BIGINT AUTO_INCREMENT NOT NULL,
    run INT NOT NULL UNIQUE,
    run_dv CHAR(1) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    apellidos VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    contrasenia VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_usuario_id_tipo_usuario BIGINT NOT NULL,
    comuna_id_comuna INT NOT NULL,
    CONSTRAINT usuario_PK PRIMARY KEY (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Código actual del proyecto:**

```java
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "run", nullable = false, unique = true)
    private Integer run;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "run_dv", nullable = false, unique = true, length = 1)
    private String run_dv;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fecha_nacimiento;

    @Column(name = "telefono", nullable = false, length = 15)
    private String telefono;

    @Column(name = "contrasenia", nullable = false)
    private String contrasenia;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_usuario_id_tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comuna_id_comuna", nullable = false)
    private Comuna comuna;
}
```

### 6.1 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `@Id` + `@GeneratedValue(IDENTITY)` + `@Column(name = "id_usuario")` + `private Long id;` | Igual que `TipoUsuario`: `BIGINT AUTO_INCREMENT` ↔ `Long` |
| `@Column(name = "run", nullable = false, unique = true)` + `private Integer run;` | El RUN **sin** dígito verificador (`INT`). `UNIQUE` sí existe en `V1`: no puede haber dos usuarios con el mismo RUN |
| `@JdbcTypeCode(SqlTypes.CHAR)` | La anotación más especial del archivo. Ver [6.3](#63-explicación-de-jdbctypecodesqltypeschar) |
| `@Column(name = "run_dv", nullable = false, unique = true, length = 1)` | `length = 1`: el dígito verificador mide un carácter (0-9 o `K`). ⚠ `unique = true` **no** está en `V1`: ver [7.1](#71-run_dv-con-unique--true) |
| `private String run_dv;` | Se guarda como `String` de un carácter. ⚠ El nombre con guion bajo: ver [7.3](#73-nombres-con-guion-bajo-en-java) |
| `apellidos`, `nombre`, `direccion` | `VARCHAR(255) NOT NULL`: `String` y `nullable = false`. Sin `unique`: dos personas pueden llamarse igual |
| `@Column(name = "correo", nullable = false, unique = true)` | El correo es el **identificador de inicio de sesión**. `UNIQUE` sí está en `V1` |
| `private LocalDate fecha_nacimiento;` | `DATE` ↔ `LocalDate`. ⚠ Nombre con guion bajo: [7.3](#73-nombres-con-guion-bajo-en-java) |
| `@Column(name = "telefono", nullable = false, length = 15)` | `VARCHAR(15)`: el `length` coincide con `V1` |
| `@Column(name = "contrasenia", nullable = false)` | Aquí se guarda el **hash BCrypt** (unos 60 caracteres), **nunca** la clave en texto plano ([guía 15](15-seguridad-codigo-explicado.md#2-qué-es-bcrypt-y-por-qué-no-se-compara-con-equals)). La columna mide 255, de sobra |
| `private Boolean activo = true;` | Ver [6.2](#62-por-qué-activo--true-en-java) |
| `@ManyToOne(fetch = LAZY, optional = false)` + `@JoinColumn(name = "tipo_usuario_id_tipo_usuario", nullable = false)` + `private TipoUsuario tipoUsuario;` | Relación al rol. El tipo de la FK coincide con el id de `TipoUsuario`: `BIGINT` ↔ `Long` |
| `@ManyToOne(fetch = LAZY, optional = false)` + `@JoinColumn(name = "comuna_id_comuna", nullable = false)` + `private Comuna comuna;` | Relación a la comuna. La FK es `INT` y el id de `Comuna` es `int`: coinciden |

### 6.2 Por qué `activo = true` en Java

La base tiene `DEFAULT TRUE`, pero **ese valor por defecto solo se usa cuando el `INSERT` no menciona la columna**. Hibernate envía **todas** las columnas en cada `INSERT`. Si el atributo fuera `null`, mandaría `NULL` explícito y la base lo rechazaría (`NOT NULL`). Con `= true` en Java, un `new Usuario()` ya nace activo.

```java
Usuario u = new Usuario();
u.getActivo();   // true, sin haber llamado a ningún setter
```

### 6.3 Explicación de `@JdbcTypeCode(SqlTypes.CHAR)`

```java
@JdbcTypeCode(SqlTypes.CHAR)
```

| Parte | Significado |
| --- | --- |
| `@JdbcTypeCode` | Anotación de **Hibernate** (no de JPA). Dice: "para este atributo, usa este tipo JDBC en lugar del que deducirías" |
| `(...)` | Recibe un número entero: el código del tipo JDBC |
| `SqlTypes.CHAR` | Constante con ese código para `CHAR`. Se usa la constante en vez del número para que se lea |

**El problema que resuelve.** Un `String` de Java se asigna por defecto a `VARCHAR`. Pero `run_dv` es `CHAR(1)` en la base. Para `validate`, `VARCHAR` y `CHAR` son tipos distintos, y el error típico cuando no coinciden es:

```
Schema-validation: wrong column type encountered in column [run_dv] in table [usuario];
found [char (Types#CHAR)], but expecting [varchar(255) (Types#VARCHAR)]
```

Con `@JdbcTypeCode(SqlTypes.CHAR)` el tipo esperado pasa a ser `CHAR`, y el arranque valida bien (confirmado en tu log del 18 oct).

**Alternativas** (no usadas): mapear a `Character` en vez de `String`, o cambiar la columna a `VARCHAR(1)` con una migración nueva. Se eligió `String` + anotación para no tocar el esquema.

**Para ver el problema con tus ojos (2 minutos):** comenta la línea `@JdbcTypeCode(SqlTypes.CHAR)`, arranca y lee el error. Después devuélvela.

---

## 7. Observaciones sobre el código actual

Ninguna impide arrancar. Se ordenan de más a menos importante.

> **Actualización 18 oct (noche):** el cambio a `camelCase` de 7.3 **ya se aplicó**: `UsuarioService` llama a `setRunDv(...)` y `setFechaNacimiento(...)` y el proyecto compila y arranca. Por eso los bloques de código de las secciones 6 y 7 que muestran `run_dv` y `fecha_nacimiento` como nombres de atributo corresponden al **estado anterior**. Lo que **falta confirmar** es que se quitaron los `unique = true` de 7.1 y 7.2.

### 7.1 `run_dv` con `unique = true`

En `Usuario.java`:

```java
@Column(name = "run_dv", nullable = false, unique = true, length = 1)   // actual
@Column(name = "run_dv", nullable = false, length = 1)                  // correcto según V1
```

`V1` **no** declara `UNIQUE` en `run_dv`. Con `validate` no se nota, porque no se verifica. Pero es incorrecto: el dígito verificador solo tiene 11 valores posibles (0-9 y `K`), y la entidad dice que no puede repetirse. Si algún día alguien activa `ddl-auto: update` o `create` para probar, Hibernate crearía ese índice único y el **segundo** usuario con el mismo dígito fallaría al registrarse.

### 7.2 `unique = true` en nombres de `Region` y `Comuna`

`nombre_region` y `nombre_comuna` **no** son `UNIQUE` en `V1`. Es el mismo caso que 7.1: no rompe nada hoy, pero la entidad no refleja la base. Déjalos así:

```java
@Column(name = "nombre_region", nullable = false)
@Column(name = "nombre_comuna", nullable = false)
```

Si en el futuro quieres que sean únicos, se hace con una migración nueva (`V4__...`), no solo en la entidad.

### 7.3 Nombres con guion bajo en Java

`run_dv` y `fecha_nacimiento` funcionan, pero rompen la convención de Java (`camelCase`) y traerán problemas más adelante:

| Dónde | Qué pasa con `run_dv` |
| --- | --- |
| Lombok | Genera `getRun_dv()` y `setRun_dv()`, poco legibles |
| Repository (Spring Data) | El guion bajo es el **separador de ruta** en los nombres de método. Una búsqueda sobre este atributo se vuelve ambigua |
| MapStruct (fase de DTOs) | Mapea por nombre: `run_dv` no coincide con un DTO que diga `runDv` |
| JSON | La API devolvería `"run_dv"` en vez de `"runDv"` |

Recomendado:

```java
@Column(name = "run_dv", nullable = false, length = 1)
private String runDv;

@Column(name = "fecha_nacimiento", nullable = false)
private LocalDate fechaNacimiento;
```

La columna sigue siendo la misma (`name = "..."` no cambia). Conviene hacerlo **ahora**, antes de crear DTOs y mappers que dependan del nombre.

### 7.4 `int` frente a `Integer`

`Region` y `Comuna` usan `int id`, que funciona. La convención es `Integer`, por dos razones: un objeto nuevo sin id debería poder valer `null` (con `int` vale `0`), y Spring Data usa ese valor para decidir si un objeto es nuevo. En estas dos tablas el id siempre se asigna a mano y solo se leen, por lo que no causa errores. Si lo cambias, cambia también el segundo tipo de `JpaRepository<Region, Integer>` (ya está en `Integer`).

### 7.5 Limpieza

| Dónde | Qué |
| --- | --- |
| `Region.java` | `import jakarta.validation.constraints.Min;` sin usar. Y al final hay una copia comentada de `TipoUsuario` y del `CREATE TABLE`: bórrala (las migraciones ya documentan el esquema) |
| `Comuna.java` | El `CREATE TABLE` comentado al final |
| `Usuario.java` | Líneas en blanco de más |

IntelliJ marca los `import` sin usar en gris; `Ctrl+Alt+O` los quita.

---

## 8. Cómo se relacionan las cuatro

```
   region (1) <------- (N) comuna (1) <------- (N) usuario (N) -------> (1) tipo_usuario
   Region                  Comuna                    Usuario                  TipoUsuario
                       @ManyToOne Region        @ManyToOne Comuna       @ManyToOne TipoUsuario
```

Todas las flechas se leen "de la clase con la FK hacia la clase referida". Nadie tiene la lista inversa (`Region.getComunas()`): no la necesitamos y evitamos una relación `@OneToMany` más que mantener.

## 9. Errores de entidades que ya aparecieron

| Mensaje | Causa | Solución |
| --- | --- | --- |
| `missing column [id_comuna] in table [region]` | `@Table(name = "region")` copiado dentro de `Comuna` | `@Table(name = "comuna")` |
| `missing column [x] in table [y]` (en general) | El nombre en `@Column(name = ...)` no existe en `V1` | Copiar el nombre exacto de `V1` |
| `missing table [x]` | `@Table(name = ...)` apunta a una tabla inexistente | Corregir el nombre |
| `wrong column type ... found [char], expecting [varchar]` | `CHAR(1)` mapeado como `String` sin `@JdbcTypeCode` | Agregar `@JdbcTypeCode(SqlTypes.CHAR)` |

Cómo leer estos errores paso a paso: [guía 16](16-leer-errores-de-spring.md).
