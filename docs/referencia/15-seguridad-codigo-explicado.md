# 15. Seguridad: `SecurityConfig` y `PasswordEncoder` explicados

Guía para entender, línea por línea, la clase `security/SecurityConfig.java` y el concepto de BCrypt. Complementa a [08-seguridad.md](08-seguridad.md), que describe **qué medidas hay y cuáles faltan**; esta guía explica **el código que ya existe**.

Archivo que se explica (ruta `src/main/java/com/factory/tecnofactory/security/`):

| Archivo | Para qué sirve | Estado |
| --- | --- | --- |
| `SecurityConfig.java` | Reglas de acceso a las rutas y el codificador de contraseñas | **Reescrito el 20 oct** (esta guía explica la **primera versión**; la actual está en la [guía 21](21-seguridad-jwt-y-roles-explicada.md#10-securityconfigjava-las-reglas-de-acceso)) |
| `JwtService.java`, `JwtAuthFilter.java`, `UserDetailsServiceImpl.java` | Login con tokens | **Escritos** el 20 oct: ver la [guía 21](21-seguridad-jwt-y-roles-explicada.md) |

> **Actualización 20 oct:** las rutas **ya no están todas abiertas**. Lo que sigue en esta guía (secciones 3 a 5) describe la primera versión de `SecurityConfig`, con `permitAll()` temporal, y se conserva porque explica los conceptos base: filtros, CSRF, orden de las reglas y BCrypt. Para el código vigente, lee la [guía 21](21-seguridad-jwt-y-roles-explicada.md).

## En palabras simples

- **`SecurityConfig` es el reglamento de un edificio con varias puertas.** Dice qué puertas están abiertas a todos (Swagger, el registro), cuáles piden una tarjeta (casi todas) y cuáles piden una tarjeta de un nivel concreto (administración).
- **Los filtros son los guardias.** Cada petición pasa por una fila de guardias antes de llegar al controller; cada uno revisa algo (¿trae tarjeta?, ¿es un ataque?, ¿viene del sitio permitido?).
- **BCrypt es una trituradora de contraseñas con una particularidad:** convierte `Clave12345` en un código (`$2a$10$...`) del que **no se puede volver atrás**. Para comprobar una clave, se tritura la que escribe la persona y se compara el resultado con el guardado. Aunque alguien robe la base de datos, no obtiene las claves.
- **«Sin sesiones» (`STATELESS`)** significa que el edificio **no recuerda** a nadie entre una visita y otra: en cada puerta hay que enseñar la tarjeta de nuevo.
- **CSRF** es un ataque que aprovecha que el navegador manda solo las *cookies*. Como esta API no usa cookies sino una cabecera con el token, no aplica y se desactiva.

Para la analogía completa del login y los tokens, mira [21, sección 1](21-seguridad-jwt-y-roles-explicada.md#1-en-palabras-simples).

---

## 1. Conceptos básicos

### 1.1 Qué hace Spring Security

Spring Security es una **cadena de filtros** que cada petición HTTP atraviesa **antes** de llegar a tu controller. Cada filtro puede dejarla pasar o cortarla con un error.

```
Navegador / Swagger / React
        |
        v
  +---------------------------------------------+
  | Cadena de filtros (SecurityFilterChain)     |
  |   - ¿Es CSRF válido?       (desactivado)    |
  |   - ¿Hay sesión?           (no: STATELESS)  |
  |   - ¿La ruta es pública?   (reglas)         |
  |   - ¿Trae credenciales?    (httpBasic)      |
  +---------------------------------------------+
        |  pasa
        v
   Tu controller  ->  service  ->  repository  ->  MySQL
```

Si no escribes un `SecurityConfig`, Spring aplica una cadena **por defecto** que exige usuario y clave en **todo**. Por eso, sin esta clase, hasta Swagger devolvía 401.

### 1.2 Qué es un `@Bean`

Un **bean** es un objeto que Spring crea una sola vez y **guarda** para entregárselo a quien lo pida. Un método marcado con `@Bean` le dice a Spring: "lo que devuelve este método, guárdalo".

```java
@Bean
public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
```

Más adelante, `UsuarioService` pedirá un `PasswordEncoder` en su constructor y Spring le entregará **ese mismo objeto**, sin que tú escribas `new`. Eso se llama **inyección de dependencias**.

---

## 2. Qué es BCrypt y por qué no se compara con `equals`

Una contraseña **nunca** se guarda tal cual: se guarda su **hash**, el resultado de pasarla por una función de un solo sentido. Del hash no se puede volver a la clave. Si alguien roba la base, no tiene las claves.

BCrypt es un algoritmo de hash **diseñado para contraseñas**. Tiene dos propiedades clave:

| Propiedad | Qué significa |
| --- | --- |
| **Lento a propósito** | Calcular un hash toma una fracción de segundo. Para un usuario no se nota; para quien prueba millones de claves, es una barrera |
| **Lleva sal (*salt*)** | Mezcla un valor aleatorio con la clave. La **misma clave genera hashes distintos** cada vez |

Por la sal, **no se puede comparar con `equals`**:

```java
// Valores ilustrativos, no son hashes reales de ninguna clave
encoder.encode("miClave");   // $2a$10$N9qo8uLOickgx2ZMRZoMye...   (una vez)
encoder.encode("miClave");   // $2a$10$e0MYzXyjpJS7Pd0RVvHwHe...   (otra vez: distinto)

// Forma correcta de verificar:
encoder.matches("miClave", hashGuardado);   // true o false
```

`matches` lee la sal que viene **dentro** del propio hash, repite el cálculo y compara.

### 2.1 Cómo se ve un hash

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
└┬─┘└┬┘└──────────┬─────────┘└────────────────┬───────────────┘
 │   │            │                           └─ el hash (31 caracteres)
 │   │            └─ la sal (22 caracteres)
 │   └─ costo: 10 -> 2^10 = 1024 rondas de cálculo
 └─ versión del algoritmo
```

Total: **60 caracteres**. La columna `usuario.contrasenia` mide 255, de sobra.

| Prefijo | Quién lo genera | ¿Lo acepta Spring Security? |
| --- | --- | --- |
| `$2a$` | `BCryptPasswordEncoder` de Spring | Sí |
| `$2b$` | La librería `bcrypt` de Python | Sí |
| `$2y$` | `htpasswd` (Docker) | Sí |

---

## 3. `SecurityConfig.java` línea por línea

```java
package com.factory.tecnofactory.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: documentación y salud
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // FASE DE DESARROLLO: todo abierto para poder probar.
                        // TODO (fase login/JWT): cambiar por .anyRequest().authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### 3.1 Importaciones

| `import` | Qué es |
| --- | --- |
| `...annotation.Bean` | La anotación `@Bean` |
| `...annotation.Configuration` | La anotación `@Configuration` |
| `...config.Customizer` | Interfaz auxiliar; `Customizer.withDefaults()` significa "usa la configuración por defecto" |
| `...builders.HttpSecurity` | El **constructor** de la cadena de filtros. Spring te lo entrega ya creado |
| `...configurers.AbstractHttpConfigurer` | Clase base de los configuradores. Se usa por su método `disable` |
| `...http.SessionCreationPolicy` | Enumeración con las políticas de sesión (`STATELESS`, `ALWAYS`...) |
| `...bcrypt.BCryptPasswordEncoder` | La implementación de BCrypt |
| `...password.PasswordEncoder` | La **interfaz** de codificadores. Se declara el tipo como interfaz para poder cambiar de algoritmo sin tocar el resto del código |
| `...web.SecurityFilterChain` | El tipo que representa la cadena ya construida |

### 3.2 La clase y el codificador

| Línea | Qué significa |
| --- | --- |
| `@Configuration` | "Esta clase **define beans**." Spring la lee al arrancar y ejecuta sus métodos `@Bean` |
| `public class SecurityConfig` | Sin `extends`: en Spring Security 6 y 7 se configura con beans, no heredando de `WebSecurityConfigurerAdapter` (esa clase ya no existe) |
| `@Bean` | Guarda lo que el método devuelve |
| `public PasswordEncoder passwordEncoder()` | El tipo de retorno es la **interfaz**; el nombre del método es el nombre del bean |
| `return new BCryptPasswordEncoder();` | Crea el codificador con el costo por defecto (10). Tiene dos métodos que usarás: `encode(clavePlana)` y `matches(clavePlana, hash)` |

### 3.3 La cadena de filtros

| Línea | Qué significa |
| --- | --- |
| `@Bean` | Guarda la cadena construida |
| `SecurityFilterChain securityFilterChain(HttpSecurity http)` | Spring entrega el constructor `http` ya listo. El método devuelve la cadena terminada |
| `throws Exception` | `http.build()` declara que puede lanzar una excepción comprobada. Hay que propagarla |
| `http` | Se encadenan llamadas con `.` (estilo *fluent*): cada una configura una parte y devuelve el mismo objeto |
| `.csrf(AbstractHttpConfigurer::disable)` | Desactiva la protección CSRF. Ver [3.4](#34-por-qué-se-desactiva-csrf) |
| `AbstractHttpConfigurer::disable` | Referencia a método: equivale a `csrf -> csrf.disable()` |
| `.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` | **Sin sesión**: el servidor no guarda nada entre peticiones y no crea la cookie `JSESSIONID`. Cada petición deberá traer su propio token. Es lo que corresponde a JWT |
| `sm -> ...` | Una **lambda**: recibe el configurador `sm` y lo usa. Es una función sin nombre |
| `.authorizeHttpRequests(auth -> auth ...)` | Aquí van las reglas de **quién entra a qué ruta** |
| `.requestMatchers("/swagger-ui/**", ...)` | Elige rutas. `**` significa "cualquier subruta" |
| `.permitAll()` | Cualquiera puede entrar, sin credenciales |
| `.requestMatchers("/actuator/health").permitAll()` | La salud de la app es pública (solo dice `UP`/`DOWN`) |
| `.anyRequest().permitAll()` | **Todo lo demás también es público.** Es lo que deja trabajar sin que el sistema bloquee |
| comentarios `// ...` y `// TODO ...` | Los `TODO` los marca IntelliJ en su panel *TODO*: no se pierden |
| `.httpBasic(Customizer.withDefaults())` | Activa autenticación con usuario y clave en un header (`Authorization: Basic ...`). Es una forma rápida de probar cuando se cierre el acceso, antes de tener JWT |
| `return http.build();` | Construye y entrega la cadena |

### 3.4 Por qué se desactiva CSRF

CSRF es un ataque que se aprovecha de las **cookies de sesión**: una página maliciosa hace que tu navegador envíe una petición con tu cookie. Tu API **no usa cookies de sesión** (es `STATELESS` y usará un token en un header), así que la protección no tiene nada que proteger.

**Y si lo dejas activo**, Spring exige un token CSRF en cada `POST`, `PUT` y `DELETE`, y todos fallan con **403**, incluso con `permitAll`. Es el motivo número uno de "mi POST no funciona en Swagger".

### 3.5 El orden de las reglas importa

Spring evalúa las reglas **de arriba hacia abajo** y **gana la primera que coincide**.

```java
.requestMatchers("/swagger-ui/**").permitAll()   // 1.ª: se mira primero
.requestMatchers("/actuator/health").permitAll() // 2.ª
.anyRequest().permitAll()                        // última: atrapa todo lo que sobró
```

`anyRequest()` va **siempre al final**. Si escribes otra regla después, Spring lo detecta y la app no arranca (`Can't configure requestMatchers after anyRequest`).

### 3.6 Dos detalles que sorprenden

**La clave generada sigue apareciendo en el log.** Verás aún:

```
Using generated security password: 3f2a...
```

Es inofensiva: esa clave solo se usaría si una ruta exigiera autenticación, y hoy todas son públicas. Desaparece cuando exista un `UserDetailsService` propio (`UserDetailsServiceImpl`, fase 4).

**`httpBasic` y un header inválido.** Si una petición trae un header `Authorization: Basic ...` con credenciales erróneas, recibe **401 incluso en una ruta pública**. Una petición **sin** header pasa sin problema.

---

## 4. Cómo comprobar que funciona

Arranca con el comando de siempre y verifica tres cosas:

| Prueba | Resultado esperado |
| --- | --- |
| Log | `Started TecnoFactoryApplication` |
| Navegador: `http://localhost:8080/swagger-ui.html` | Carga la pantalla de Swagger (aún sin endpoints: no hay controllers) |
| Terminal: `curl -i http://localhost:8080/actuator/health` | `200` y `{"status":"UP"...}` |

### 4.1 Experimento (2 minutos)

Cambia `.anyRequest().permitAll()` por `.anyRequest().authenticated()`, arranca y abre `http://localhost:8080/algo`. Verás **401** y el navegador pedirá usuario y clave (por `httpBasic`). Es lo que verán cuando se cierre el acceso en la fase de login. Después **devuélvelo a `permitAll()`**.

### 4.2 Qué NO verifica esto

Que `BCryptPasswordEncoder` funcione con claves reales y que el login acepte un hash solo se comprueba cuando exista el login. Hasta entonces el `PasswordEncoder` es un bean disponible, sin usar.

---

## 5. Lo que cambia cuando llegue el login

> **Esto ya ocurrió (20 oct).** La tabla de abajo era el plan; el resultado está en la [guía 21](21-seguridad-jwt-y-roles-explicada.md#10-securityconfigjava-las-reglas-de-acceso). Cambió así: `permitAll()` pasó a `authenticated()`, se agregó el filtro JWT, `httpBasic` se quitó, y CORS se activó con `CorsConfig`.

| Hoy | En la fase 4 |
| --- | --- |
| `.anyRequest().permitAll()` | `.anyRequest().authenticated()` |
| Rutas públicas: Swagger y `health` | Además: `POST /api/auth/**`, `GET /api/productos/**`, `GET /api/categorias/**` |
| Sin roles | `.requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMINISTRADOR")` y similares |
| Sin filtro JWT | `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` |
| Sin CORS | `.cors(...)` con el dominio de React |

El diseño completo del control de acceso está en [08-seguridad.md](08-seguridad.md#diseño-previsto-del-control-de-acceso).

## 6. Errores relacionados

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `401` en todas las rutas | `SecurityConfig` vacío o sin `SecurityFilterChain` | Escribir el bean ([3.3](#33-la-cadena-de-filtros)) |
| `403` solo en `POST`, `PUT`, `DELETE` | CSRF activo | `.csrf(AbstractHttpConfigurer::disable)` |
| `cannot find symbol: WebSecurityConfigurerAdapter` | Ejemplo viejo (Spring Security 5) | Usar beans, como aquí |
| La app no arranca con `Can't configure requestMatchers after anyRequest` | Se escribió una regla `requestMatchers(...)` **después** de `anyRequest()` | Mover `anyRequest()` a la última posición |
| `PasswordEncoder` no se encuentra al inyectarlo | El bean no está o la clase no tiene `@Configuration` | Revisar la anotación |
