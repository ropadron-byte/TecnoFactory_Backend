# 17. Administrador inicial explicado

Guía para entender, línea por línea, las tres piezas que crean el usuario administrador la primera vez que arranca la aplicación, en qué **orden** se ejecuta todo, para qué sirve cada cosa y qué **comandos** conviene dominar. Se apoya en el log real del primer arranque (18 oct 2026), en el que el administrador se creó correctamente.

| Archivo | Ruta (`src/main/java/com/factory/tecnofactory/`) | Para qué sirve | Sección |
| --- | --- | --- | --- |
| `DatosAdministrador.java` | `service/` | Agrupa en un solo objeto los 10 datos que necesita el administrador | [4](#4-datosadministradorjava) |
| `UsuarioService.java` | `service/` | Contiene la **regla**: "crear el administrador si no existe" | [5](#5-usuarioservicejava) |
| `AdministradorInicialRunner.java` | `config/` | Se ejecuta **al arrancar**, reúne los datos y llama al service | [6](#6-administradorinicialrunnerjava) |

> Estado (19 oct): el código de tu repositorio (`DatosAdministrador`, `UsuarioService` y `AdministradorInicialRunner`) **es idéntico al de esta guía** (comparado archivo por archivo). **Verificado** con tus logs: la creación del administrador en el primer arranque, la rama "sin variables" (`WARN` y la app arranca igual) y que tu `ADMIN_PASSWORD` **no aparece** en los logs de archivo. **Falta confirmar** dos cosas: el contenido de la fila en la base y que un segundo arranque no duplique nada (comandos en la [sección 8](#8-comandos-importantes)).

---

## 1. Para qué sirve

Sin al menos un administrador, nadie puede crear ni editar productos y categorías. Pero una cuenta no puede venir dentro de una migración con una clave real. Por eso se crea **por código, al arrancar**:

| Decisión | Motivo |
| --- | --- |
| Correo y clave salen de **variables de entorno** (`ADMIN_EMAIL`, `ADMIN_PASSWORD`) | Nada secreto queda en Git |
| Se crea **solo si no existe** un administrador | Reiniciar la app mil veces no duplica nada |
| La clave se guarda como **hash BCrypt** | Si alguien lee la base, no obtiene la clave |
| Rol buscado **por nombre** (`ADMINISTRADOR`), no por id | Los ids los asigna `AUTO_INCREMENT`; el nombre es estable |
| El resto de los datos (RUN, nombre, dirección...) son **valores de relleno** fijos en el runner | Son datos de sistema, no de una persona real. Se pueden editar después |

Se descartó la alternativa de una migración `V4` con un hash pegado: dejaba el hash para siempre en el historial de Git ([08](08-seguridad.md#administrador-inicial)).

---

## 2. Orden de ejecución

### 2.1 Línea de tiempo del arranque (log real del 18 oct)

Spring arranca por etapas. El administrador se crea en la **última**, **después** de que la app ya está lista.

| # | Hora | Línea del log | Qué ocurrió |
| --- | --- | --- | --- |
| 1 | 23:33:20.938 | `The following 1 profile is active: "local"` | Se eligió el perfil |
| 2 | 23:33:23.249 | `Found 4 JPA repository interfaces` | Spring encontró tus 4 repositories |
| 3 | 23:33:25.959 | `HikariPool-1 - Start completed` | Conexión a MySQL lista |
| 4 | 23:33:26.283 | `Schema ... is up to date. No migration necessary` | Flyway: nada nuevo que aplicar |
| 5 | 23:33:29.114 | `Initialized JPA EntityManagerFactory` | Hibernate validó las 4 entidades |
| 6 | 23:33:30.016 | `Using generated security password: ...` | Spring Security configurado (esa clave temporal no se usa) |
| 7 | 23:33:32.252 | `Tomcat started on port 8080` | El servidor web ya acepta peticiones |
| 8 | 23:33:32.273 | `Started TecnoFactoryApplication in 12.246 seconds` | **La app terminó de arrancar** |
| 9 | justo después | cuatro bloques `Hibernate: select ... / insert ...` | **El runner llama al service**: aquí se crea el administrador |
| 10 | 23:33:33.349 | `Administrador inicial creado con el correo admin@tecnofactory.cl` | El service terminó |

Tres cosas que conviene notar:

- **El runner corre después de `Started ...`.** Si buscas su mensaje, está **debajo** de esa línea, no arriba.
- **Cuando el runner corre, Tomcat ya está arriba** (etapa 7). Durante ese instante (poco más de un segundo aquí) la API ya acepta peticiones. Para un administrador sembrado es irrelevante; sirve para saber que el runner no es "antes de abrir".
- **Todo lo anterior ya validó.** Si el administrador no se crea, **no** hay que revisar Flyway ni las entidades: el log ya mostró que pasaron.

### 2.2 Qué ocurre dentro del runner, paso a paso (primera vez)

```
Spring termina de arrancar y escribe "Started ..."
   |
   v
Spring busca todos los beans que implementan ApplicationRunner y llama a run()
   |
   v
AdministradorInicialRunner.run()
   |-- 1. Lee adminEmail y adminPassword (inyectados desde las variables de entorno)
   |-- 2. ¿Alguna está vacía?  -> sí: log.warn y termina (la app sigue funcionando)
   |-- 3. Arma un DatosAdministrador con los 10 datos
   |-- 4. Llama a usuarioService.crearAdministradorSiNoExiste(datos)
   |
   v
UsuarioService.crearAdministradorSiNoExiste()      (dentro de una TRANSACCIÓN)
   |-- 5. ¿Ya existe un usuario con rol ADMINISTRADOR?  -> sí: log.info, devuelve false
   |-- 6. Busca el rol "ADMINISTRADOR"        -> si no existe: lanza RecursoNoEncontradoException
   |-- 7. Busca la comuna 13101               -> si no existe: lanza RecursoNoEncontradoException
   |-- 8. Arma el Usuario y convierte la clave en hash BCrypt
   |-- 9. save() -> INSERT en la tabla usuario
   |-- 10. log.info("Administrador inicial creado ...") y devuelve true
   |
   v
Fin de la transacción: COMMIT (los cambios quedan guardados)
```

### 2.3 Qué ocurre en un segundo arranque

El paso 5 responde "sí" y el método sale de inmediato:

```
Ya existe un administrador: no se crea otro
```

No hay `insert`. Es la propiedad que hace seguro reiniciar la app.

### 2.4 Qué se ejecuta cuando falta algo

| Situación | Dónde se detiene | Resultado |
| --- | --- | --- |
| Faltan `ADMIN_EMAIL` o `ADMIN_PASSWORD` | Paso 2 | `WARN` y la app **sigue** funcionando |
| Ya hay un administrador | Paso 5 | `INFO` y la app sigue |
| El rol `ADMINISTRADOR` no existe en la base | Paso 6 | La excepción **sale del runner y la app no arranca** (`Application run failed`). Lo correcto: la base está mal |
| La comuna 13101 no existe | Paso 7 | Igual que el anterior |

Las excepciones del runner **no** pasan por el `GlobalExceptionHandler`: ese solo atiende peticiones HTTP, y aquí no hay ninguna.

---

## 3. Lo que muestra el SQL del log

Con `show-sql: true`, Hibernate imprime cada consulta. Las cuatro del arranque cuentan la historia completa:

| # | SQL (resumido) | Qué línea del service la causa |
| --- | --- | --- |
| 1 | `select u1_0.id_usuario from usuario u1_0 join tipo_usuario tu1_0 on ... where tu1_0.nombre_tipo_usuario=? limit ?` | `existsByTipoUsuarioNombre(...)`. Hace el `JOIN` solo y usa `limit` porque basta con saber si hay **una** fila |
| 2 | `select ... from tipo_usuario tu1_0 where tu1_0.nombre_tipo_usuario=?` | `tipoUsuarioRepository.findByNombre(...)` |
| 3 | `select c1_0.id_comuna, c1_0.nombre_comuna, c1_0.region_id_region from comuna c1_0 where c1_0.id_comuna=?` | `comunaRepository.findById(...)` |
| 4 | `insert into usuario (activo, apellidos, comuna_id_comuna, contrasenia, correo, ...) values (?, ?, ?, ...)` | `usuarioRepository.save(admin)` |

Cuatro detalles que enseñan mucho:

- **La consulta 3 confirma que `LAZY` funciona.** Trae `region_id_region` (solo el número) pero **no** hace `JOIN` con `region` ni lee el nombre de la región. Como nadie la necesitó, nunca se consultó ([13, sección 5](13-entidades-explicadas.md#5-comunajava)).
- **El `insert` no incluye `id_usuario`.** Es `IDENTITY`: lo asigna MySQL.
- **El `insert` muestra `?`, no los valores.** Por eso ni la clave ni el hash aparecen en el log. Aparecerían solo si activaras `org.hibernate.orm.jdbc.bind: TRACE`: **no lo actives** con datos reales.
- **El `insert` sale en el momento del `save`**, antes de `Administrador inicial creado`. Con `IDENTITY`, Hibernate ejecuta el `INSERT` enseguida para conocer el id.

---

## 4. `DatosAdministrador.java`

**Qué es:** un `record` que agrupa los datos. Sin él, el service recibiría diez parámetros sueltos, fáciles de desordenar (dos `String` seguidos se confunden sin que el compilador lo note).

```java
package com.factory.tecnofactory.service;

import java.time.LocalDate;

public record DatosAdministrador(
        Integer run,
        String runDv,
        String nombre,
        String apellidos,
        String correo,
        String clave,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        Integer idComuna
) {
}
```

| Línea | Qué significa |
| --- | --- |
| `package ...service;` | Vive junto al service que lo usa |
| `import java.time.LocalDate;` | El tipo de la fecha |
| `public record DatosAdministrador(...)` | Un **record**: clase de datos **inmutable**. Java genera por ti el constructor, un getter por campo, `equals`, `hashCode` y `toString` |
| `Integer run` | El RUN **sin** dígito verificador |
| `String runDv` | El dígito verificador (`"1"`, `"K"`...) |
| `String nombre`, `apellidos` | Textos obligatorios |
| `String correo` | Será el identificador de inicio de sesión |
| `String clave` | La clave en **texto plano**. Es el único lugar donde existe así; el service la convierte en hash enseguida |
| `LocalDate fechaNacimiento` | Solo fecha |
| `String direccion`, `telefono` | Textos obligatorios (`telefono` mide como máximo 15) |
| `Integer idComuna` | El **código** de la comuna, no el objeto `Comuna`: quien llama solo conoce el número; el service busca el objeto |
| `{ }` vacío | Un record no necesita cuerpo |

**Cómo se usa un record:** los getters se llaman igual que el campo, **sin** `get`:

```java
datos.correo()        // no datos.getCorreo()
datos.idComuna()
```

> **Cuidado:** el `toString()` automático de un record imprime **todos** los campos, **incluida `clave`**. Nunca hagas `log.info("..." + datos)` ni `log.debug("{}", datos)`. En tu perfil local `com.factory` está en `DEBUG` y el log se guarda en un archivo.

---

## 5. `UsuarioService.java`

**Qué es:** la capa de **lógica de negocio**. Decide; no recibe HTTP ni habla SQL directamente. Así, la misma regla la puede usar el arranque hoy y un controller mañana.

```java
package com.factory.tecnofactory.service;

import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.repository.ComunaRepository;
import com.factory.tecnofactory.repository.TipoUsuarioRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public boolean crearAdministradorSiNoExiste(DatosAdministrador datos) {
        if (usuarioRepository.existsByTipoUsuarioNombre(ROL_ADMINISTRADOR)) {
            log.info("Ya existe un administrador: no se crea otro");
            return false;
        }

        TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_ADMINISTRADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_ADMINISTRADOR));

        Comuna comuna = comunaRepository.findById(datos.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", datos.idComuna()));

        Usuario admin = new Usuario();
        admin.setRun(datos.run());
        admin.setRunDv(datos.runDv());
        admin.setNombre(datos.nombre());
        admin.setApellidos(datos.apellidos());
        admin.setCorreo(datos.correo());
        admin.setContrasenia(passwordEncoder.encode(datos.clave()));
        admin.setFechaNacimiento(datos.fechaNacimiento());
        admin.setDireccion(datos.direccion());
        admin.setTelefono(datos.telefono());
        admin.setTipoUsuario(rol);
        admin.setComuna(comuna);

        usuarioRepository.save(admin);
        log.info("Administrador inicial creado con el correo {}", admin.getCorreo());
        return true;
    }
}
```

### 5.1 Importaciones

| `import` | Qué es |
| --- | --- |
| `...entity.Comuna`, `TipoUsuario`, `Usuario` | Las entidades con las que trabaja |
| `...exception.RecursoNoEncontradoException` | Tu excepción propia ([11](11-excepciones-explicadas.md)) |
| `...repository.*Repository` | Los tres repositories que necesita |
| `lombok.RequiredArgsConstructor` | Genera el constructor |
| `lombok.extern.slf4j.Slf4j` | Agrega la variable `log` ([12](12-logs.md)) |
| `...crypto.password.PasswordEncoder` | La **interfaz** del codificador (el bean `BCryptPasswordEncoder` de `SecurityConfig`) |
| `org.springframework.stereotype.Service` | La anotación `@Service` |
| `org.springframework.transaction.annotation.Transactional` | **La de Spring.** IntelliJ también ofrece `jakarta.transaction.Transactional`: elige la de Spring |

### 5.2 La clase

| Línea | Qué significa |
| --- | --- |
| `@Service` | "Crea **un** objeto de esta clase al arrancar y guárdalo." Es lo que permite que el runner lo reciba sin `new`. Hace lo mismo que `@Component`; el nombre indica que es la capa de lógica |
| `@RequiredArgsConstructor` | Lombok genera un constructor con **un parámetro por cada campo `final`** |
| `@Slf4j` | Lombok agrega `private static final Logger log = ...` |
| `private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";` | Una **constante** (`static final`: de la clase y que no cambia). El texto se usa en tres lugares; un error de tipeo (`"ADMINISTRDOR"`) fallaría en silencio sin que el compilador avise. Debe coincidir exactamente con el de `V2` |

### 5.3 Los cuatro campos y la inyección de dependencias

```java
private final UsuarioRepository usuarioRepository;
private final TipoUsuarioRepository tipoUsuarioRepository;
private final ComunaRepository comunaRepository;
private final PasswordEncoder passwordEncoder;
```

No se escribe `new` en ninguna parte (los repositories ni siquiera se pueden instanciar: son interfaces). Declaras lo que **necesitas** como campos `final`; Lombok genera un constructor que los pide; y **Spring**, al crear el service, busca esos objetos entre sus beans y **se los entrega**. Eso es la **inyección de dependencias por constructor**.

```java
// Lo que Lombok genera (no lo ves, pero existe):
public UsuarioService(UsuarioRepository usuarioRepository,
                      TipoUsuarioRepository tipoUsuarioRepository,
                      ComunaRepository comunaRepository,
                      PasswordEncoder passwordEncoder) { ... }
```

| Por qué `final` | Se asigna una sola vez, en el constructor; y es lo que hace que `@RequiredArgsConstructor` lo incluya |
| --- | --- |
| Por qué no `@Autowired` sobre el campo | El objeto nunca queda a medio armar y se puede probar pasando objetos falsos |

El `PasswordEncoder` recibido es **el mismo** que declaraste con `@Bean` en `SecurityConfig` ([15](15-seguridad-codigo-explicado.md#32-la-clase-y-el-codificador)).

### 5.4 `@Transactional`

```java
@Transactional
public boolean crearAdministradorSiNoExiste(DatosAdministrador datos)
```

Una **transacción** agrupa operaciones de base de datos: se aplican **todas** o **ninguna**. Spring la abre al entrar al método y la cierra al salir:

| Cómo termina el método | Qué hace Spring |
| --- | --- |
| Sin errores | `COMMIT`: los cambios quedan guardados |
| Con una excepción no comprobada (`RuntimeException` y sus hijas, como `RecursoNoEncontradoException`) | `ROLLBACK`: se deshace todo |

Aquí hay lecturas y una escritura; si algo falla a mitad de camino, no queda nada a medias. Funciona porque quien llama (el runner) recibe un **intermediario** que Spring puso delante del service: ese intermediario abre y cierra la transacción.

### 5.5 El método, bloque por bloque

| Línea | Qué hace |
| --- | --- |
| `public boolean crearAdministradorSiNoExiste(...)` | Devuelve `true` si **creó** al administrador y `false` si ya existía. Quien llama puede saber qué pasó |
| `if (usuarioRepository.existsByTipoUsuarioNombre(ROL_ADMINISTRADOR))` | Usa el método del paso 4. Genera el `JOIN` con `tipo_usuario` (consulta 1 del log) |
| `log.info("Ya existe un administrador: no se crea otro");` | Nivel `INFO`. `{}` no hace falta porque no hay valores que insertar |
| `return false;` | Sale **sin hacer nada más** |
| `TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_ADMINISTRADOR)` | Devuelve un `Optional<TipoUsuario>`: una caja que puede estar vacía |
| `.orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_ADMINISTRADOR))` | Si la caja trae un valor, lo entrega; si está vacía, **lanza** tu excepción |
| `() -> new ...` | Una **lambda**: una función sin nombre. Solo se ejecuta **si hace falta** (si el `Optional` está vacío) |
| `RecursoNoEncontradoException("Rol", ROL_ADMINISTRADOR)` | Su constructor arma el mensaje `Rol no encontrado con id ADMINISTRADOR` |
| `comunaRepository.findById(datos.idComuna())` | `findById` viene gratis de `JpaRepository`. Recibe el tipo del `@Id` (`Integer`: por eso se corrigió el repository en el paso 4) |
| `datos.idComuna()` | Getter del record (sin `get`) |
| `Usuario admin = new Usuario();` | Crea la entidad vacía. Usa el constructor que generó `@NoArgsConstructor` |
| `admin.setRun(...)`, `setRunDv(...)`, etc. | Cada `set` los generó `@Setter`. `activo` **no** se toca: ya nace en `true` por el valor inicial de la entidad |
| `admin.setContrasenia(passwordEncoder.encode(datos.clave()))` | **La línea más importante.** `encode` convierte la clave en un hash BCrypt de 60 caracteres. Lo que llega a `setContrasenia` es el hash, **nunca** la clave. BCrypt es lento a propósito: tarda una fracción de segundo |
| `admin.setTipoUsuario(rol)` / `admin.setComuna(comuna)` | Se asignan **objetos**, no números. Hibernate extrae el id al guardar |
| `usuarioRepository.save(admin);` | Ejecuta el `INSERT` (consulta 4 del log). Como `id` es `null`, Hibernate sabe que es un registro nuevo |
| `log.info("... {}", admin.getCorreo());` | `{}` se reemplaza por el valor. Se registra el **correo**, **nunca** la clave ni el hash |
| `return true;` | Avisa que sí se creó |

### 5.6 Qué queda fuera a propósito

- **No se valida el RUN ni el correo.** Es un administrador sembrado por nosotros, no un registro de un usuario externo. Eso va en los DTOs (paso 8).
- **No se valida la fortaleza de la clave.** Se confía en la variable de entorno.
- Si el `INSERT` choca con un `UNIQUE` (`correo` o `run` ya existentes), la base lanza una excepción y la transacción se deshace.

---

## 6. `AdministradorInicialRunner.java`

**Qué es:** una clase que Spring ejecuta **una sola vez, al terminar de arrancar**. Su trabajo es mínimo: reunir los datos y llamar al service. **No decide nada**; la regla vive en el service.

```java
package com.factory.tecnofactory.config;

import com.factory.tecnofactory.service.DatosAdministrador;
import com.factory.tecnofactory.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdministradorInicialRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea el administrador inicial");
            return;
        }

        DatosAdministrador datos = new DatosAdministrador(
                11111111, "1",
                "Administrador", "TecnoFactory",
                adminEmail, adminPassword,
                LocalDate.of(2000, 1, 1),
                "Por definir", "+56900000000",
                13101
        );

        usuarioService.crearAdministradorSiNoExiste(datos);
    }
}
```

### 6.1 Importaciones

| `import` | Qué es |
| --- | --- |
| `...service.DatosAdministrador`, `UsuarioService` | Las dos clases del paquete `service` |
| `...annotation.Value` | La anotación `@Value`, que lee una propiedad o variable de entorno |
| `org.springframework.boot.ApplicationRunner` | El contrato "ejecútame al arrancar" |
| `org.springframework.boot.ApplicationArguments` | El tipo del parámetro de `run` (argumentos de línea de comandos) |
| `org.springframework.stereotype.Component` | La anotación `@Component` |
| `java.time.LocalDate` | Para la fecha de nacimiento de relleno |

### 6.2 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `@Component` | Registra la clase en Spring. Se usa `@Component` y no `@Service` porque no es lógica de negocio: es una pieza de arranque |
| `@RequiredArgsConstructor` | Genera el constructor con los campos `final`: aquí solo `usuarioService` |
| `@Slf4j` | Agrega `log` |
| `implements ApplicationRunner` | Es el contrato. Al arrancar, Spring busca **todos** los beans que lo implementan y llama a su método `run` |
| `private final UsuarioService usuarioService;` | Spring lo inyecta por constructor, como en el service |
| `@Value("${ADMIN_EMAIL:}")` | Inyecta el valor de la variable de entorno `ADMIN_EMAIL` en el campo |
| `${ADMIN_EMAIL:}` | La parte tras `:` es el **valor por defecto**: aquí, vacío. Si la variable no existe, el campo vale `""` y la app **no** falla ([03](03-variables-y-claves.md#sintaxis-variablevalor)) |
| Campos `@Value` **sin** `final` | Spring los asigna **después** de crear el objeto, así que no pueden ser `final` ni entran en el constructor de Lombok |
| `@Override` | Indica que implementa un método del contrato. Si te equivocas en el nombre, el compilador avisa |
| `public void run(ApplicationArguments args)` | El método que Spring llama. `args` no se usa aquí |
| `adminEmail.isBlank() \|\| adminPassword.isBlank()` | `isBlank()` es verdadero para `""` **y** para solo espacios. `\|\|` significa "o": basta con que falte una |
| `log.warn("...")` | Nivel `WARN`: conviene saberlo, pero no impide funcionar. **No imprime ni el correo ni la clave** |
| `return;` | Termina `run` sin hacer nada; la app sigue funcionando |
| `new DatosAdministrador(...)` | Arma el record. Los argumentos van **en el mismo orden** que los campos del record |
| `11111111, "1"` | RUN de relleno con **dígito verificador válido** |
| `"Administrador", "TecnoFactory"` | Nombre y apellidos de relleno |
| `adminEmail, adminPassword` | Los dos únicos datos que vienen del entorno |
| `LocalDate.of(2000, 1, 1)` | Fecha fija: año, mes, día |
| `"Por definir", "+56900000000"` | Dirección y teléfono (cabe en `VARCHAR(15)`) |
| `13101` | Código de la comuna de Santiago; existe en `V2` |
| `usuarioService.crearAdministradorSiNoExiste(datos);` | La única llamada importante. El resultado `true`/`false` no se usa aquí |

### 6.3 Decisiones de diseño

| Decisión | Por qué |
| --- | --- |
| El runner **no decide**; solo llama | La regla "crear si no existe" queda en un solo lugar y otras clases pueden usarla |
| **No falla** si faltan las variables | Un compañero sin `ADMIN_EMAIL` en su `.env` puede arrancar y trabajar. Para **producción** conviene lo contrario: cambiar el `log.warn` por `throw new IllegalStateException(...)` para que falle de forma visible |
| Datos de relleno **fijos** | Son datos de sistema. Se pueden pasar a variables de entorno más adelante sin tocar el service |

---

## 7. Conceptos nuevos de esta guía

| Concepto | Qué es | Dónde aparece |
| --- | --- | --- |
| **`record`** | Clase de datos inmutable con constructor, getters, `equals`, `hashCode` y `toString` generados | `DatosAdministrador` |
| **`@Service`** | Registra una clase de lógica de negocio en Spring | `UsuarioService` |
| **`@Component`** | Registro genérico; `@Service` es una especialización | Runner |
| **Inyección por constructor** | Spring entrega los objetos que la clase declara como campos `final` | Service y runner |
| **`@RequiredArgsConstructor`** | Lombok genera ese constructor | Service y runner |
| **`@Transactional`** | Todo o nada: `COMMIT` si termina bien, `ROLLBACK` si hay una excepción no comprobada | Service |
| **`Optional` + `orElseThrow`** | Obliga a decidir qué pasa si no se encontró nada | Service |
| **Lambda `() -> ...`** | Función sin nombre; se ejecuta solo si hace falta | `orElseThrow` |
| **`static final`** | Constante de la clase | `ROL_ADMINISTRADOR` |
| **`@Value("${X:}")`** | Lee una variable de entorno, con valor por defecto | Runner |
| **`ApplicationRunner`** | Código que Spring ejecuta una vez, al terminar de arrancar | Runner |
| **BCrypt** | Hash lento y con sal, pensado para claves ([15, sección 2](15-seguridad-codigo-explicado.md#2-qué-es-bcrypt-y-por-qué-no-se-compara-con-equals)) | `passwordEncoder.encode` |

---

## 8. Comandos importantes

### 8.1 Preparar la clave

| Comando | Qué hace |
| --- | --- |
| `openssl rand -hex 16` | Genera 16 bytes aleatorios escritos en hexadecimal (32 caracteres, solo letras y números). Es una buena clave para el `.env` porque evita `$`, `#`, `'`, `@` y `"`, que dan problemas |

### 8.2 Cargar las variables y arrancar

```bash
set -a
source .env
set +a

SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
./mvnw spring-boot:run
```

| Parte | Qué hace |
| --- | --- |
| `set -a` | "De ahora en adelante, **exporta** toda variable que se defina": las deja visibles para los programas que lances |
| `source .env` | Ejecuta el archivo **en esta terminal**, definiendo `DB_PASSWORD`, `ADMIN_EMAIL`, etc. |
| `set +a` | Desactiva el modo de exportar |
| `VARIABLE=valor \` antes del comando | La variable vale **solo para ese comando**. El `\` al final de la línea significa "el comando sigue en la siguiente" |
| `./mvnw spring-boot:run` | Compila y arranca la app con el Maven del proyecto (el wrapper) |

**Importante:** las variables viven en **esa terminal**. Si abres otra, hay que repetir `set -a; source .env; set +a`.

### 8.3 Comprobar que una variable está cargada, sin mostrarla

```bash
echo "${#ADMIN_PASSWORD}"       # imprime el LARGO de la clave (32), no la clave
[ -n "$ADMIN_EMAIL" ] && echo "ADMIN_EMAIL definida"
```

`${#VARIABLE}` entrega la cantidad de caracteres. Si imprime `0`, la variable no está cargada.

### 8.4 Comprobar el resultado en la base

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory -e "
SELECT u.id_usuario, u.correo, t.nombre_tipo_usuario AS rol,
       LEFT(u.contrasenia, 7) AS prefijo, LENGTH(u.contrasenia) AS largo, u.activo
FROM usuario u JOIN tipo_usuario t ON t.id_tipo_usuario = u.tipo_usuario_id_tipo_usuario;"
```

| Parte | Qué hace |
| --- | --- |
| `docker exec` | Ejecuta un comando **dentro** de un contenedor que ya está corriendo |
| `-it` | `-i` mantiene la entrada abierta y `-t` simula una terminal; juntos permiten escribir la clave |
| `tecnofactory-db` | El nombre del contenedor de MySQL (de tu `docker-compose.yml`) |
| `mysql -uroot -p tecnofactory` | Abre el cliente de MySQL como `root` en la base `tecnofactory`. `-p` **pide** la clave (la de `DB_PASSWORD`); no se escribe pegada |
| `-e "..."` | Ejecuta esa consulta y sale |
| `LEFT(u.contrasenia, 7)` | Primeros 7 caracteres: sirven para ver el prefijo del hash sin mostrarlo entero |
| `LENGTH(u.contrasenia)` | Largo total |

**Resultado esperado:**

| Columna | Valor |
| --- | --- |
| `correo` | El de tu `ADMIN_EMAIL` |
| `rol` | `ADMINISTRADOR` |
| `prefijo` | `$2a$10$` |
| `largo` | `60` |
| `activo` | `1` |

### 8.5 Comprobar el log

```bash
grep -c "$ADMIN_PASSWORD" logs/tecnofactory.log      # debe imprimir 0
grep -n "Administrador" logs/tecnofactory.log        # muestra las líneas del administrador
tail -n 40 logs/tecnofactory.log                     # las últimas 40 líneas
```

| Parte | Qué hace |
| --- | --- |
| `grep -c` | **Cuenta** las líneas que contienen el texto. `0` significa que la clave **no** está en el log |
| `"$ADMIN_PASSWORD"` | El shell sustituye la variable por su valor. Así **no escribes la clave a mano** y no queda en el historial de comandos |
| `grep -n` | Muestra las líneas coincidentes con su número |
| `tail -n 40` | Las últimas 40 líneas del archivo |

### 8.6 Filtrar el log al arrancar

```bash
./mvnw spring-boot:run 2>&1 | grep -E "ERROR|WARN|Caused by|Administrador"
```

`2>&1` une los errores con la salida normal; `|` pasa el resultado a `grep`; `-E` permite usar `|` como "o".

### 8.7 Docker

| Comando | Qué hace | Cuidado |
| --- | --- | --- |
| `docker compose up -d` | Levanta MySQL en segundo plano | |
| `docker compose ps` | Muestra el estado; espera `healthy` | |
| `docker compose down` | Elimina el contenedor, **conserva los datos** | |
| `docker compose down -v` | Elimina el contenedor **y borra los datos** | La base queda vacía. El administrador **se vuelve a crear** al arrancar, porque `V1`, `V2` y `V3` se reaplican. Nunca en producción |

### 8.8 Probar los casos

| Para probar... | Haz esto | Esperado |
| --- | --- | --- |
| Primera vez | Base vacía, variables cargadas, arrancar | `Administrador inicial creado ...` |
| Segunda vez | Detener y volver a arrancar | `Ya existe un administrador: no se crea otro` y **ningún** `insert` |
| Sin variables | `unset ADMIN_PASSWORD` y arrancar | `WARN ... sin definir` y la app **sí** arranca |
| Volver a normal | `set -a; source .env; set +a` | Variables otra vez cargadas |

---

## 9. Qué se verificó y qué no

| Comprobación | Estado |
| --- | --- |
| El runner se ejecutó tras `Started ...` y llamó al service | **Verificado** (log del 18 oct) |
| Las 4 consultas se ejecutaron en el orden esperado | **Verificado** |
| `LAZY` en `Comuna.region` (la consulta no hace `JOIN` con `region`) | **Verificado** en el log |
| El `insert` no muestra valores | **Verificado** |
| `Administrador inicial creado con el correo ...` | **Verificado** |
| Rama "sin variables": `WARN` y la app arranca igual | **Verificado**: dos arranques del 18 oct (23:30 y 23:31) sin variables cargadas mostraron `ADMIN_EMAIL o ADMIN_PASSWORD sin definir` y la app arrancó |
| Tu código coincide con esta guía | **Verificado** (19 oct) |
| Contenido de la fila (`rol`, `prefijo $2a$10$`, `largo 60`, `activo 1`) | **Pendiente** (8.4) |
| Segundo arranque sin duplicar | **Pendiente**: los logs no registran ningún arranque posterior a la creación (el proceso de las 23:33 siguió corriendo hasta las 11:58 del día siguiente) ([8.8](#88-probar-los-casos)) |
| La clave no aparece en `logs/tecnofactory.log` | **Verificado** (19 oct): 0 apariciones de tu `ADMIN_PASSWORD` en el log actual ni en el rotado del 18 oct. Alcance: el log de **archivo**; en la consola el `insert` muestra `?` en vez de valores |
| Que la clave y el hash funcionen juntos al iniciar sesión | **Pendiente**: solo posible con el login (fase 4) |

## 10. Errores relacionados

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `WARN ... ADMIN_EMAIL o ADMIN_PASSWORD sin definir` aunque las agregaste al `.env` | La terminal no cargó el `.env` | Repite `set -a; source .env; set +a` en **la misma** terminal donde arrancas |
| `No qualifying bean of type '...UsuarioService'` | Falta `@Service` en el service | Agregarlo |
| `Rol no encontrado con id ADMINISTRADOR` (y la app no arranca) | `V2` no se aplicó, o el nombre del rol difiere | `SELECT * FROM tipo_usuario;` |
| `Comuna no encontrado con id 13101` (y la app no arranca) | `V2` no cargó las comunas | `SELECT COUNT(*) FROM comuna;` debe dar 346 |
| `Duplicate entry '...' for key 'usuario.correo'` | Ya hay un usuario con ese correo que **no** es administrador | Usar otro `ADMIN_EMAIL` |
| `Data too long for column 'telefono'` | El teléfono de relleno excede 15 caracteres | Acortarlo |
| El administrador existe pero con la clave "vieja" | Cambiaste `ADMIN_PASSWORD` **después** de la primera vez | Es lo esperado: solo se usa la primera vez. Cambia la clave con la API (fase 4) o recrea la base en desarrollo |

## 11. Mejoras para más adelante

- **En producción, fallar si faltan las variables**: cambiar el `log.warn` + `return` por una excepción.
- **Pasar los datos de relleno a variables de entorno**, o agruparlas con `@ConfigurationProperties`, si el equipo lo prefiere.
- **Una prueba automática** del service con un repository falso (fase 8): "si ya existe un administrador, `save` no se llama".
- **Cambiar la clave inicial** apenas exista el endpoint de perfil.
