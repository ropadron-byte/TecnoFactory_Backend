# 21. Seguridad con JWT y roles explicada

Guía para entender, de lo simple a lo técnico, **cómo inicia sesión un usuario y cómo la API decide qué puede hacer**: el login (`POST /api/auth/login`), el token JWT, el filtro que lo lee en cada petición, las reglas por ruta y los errores 401 y 403 en JSON. Reemplaza en la práctica a la sección 5 de la [guía 15](15-seguridad-codigo-explicado.md) («Lo que cambia cuando llegue el login»), que ya ocurrió.

| Archivo | Ruta | Para qué sirve | Sección |
| --- | --- | --- | --- |
| `JwtService.java` | `src/main/java/com/factory/tecnofactory/security/` | Crea y comprueba los tokens | [5](#5-jwtservicejava) |
| `UserDetailsServiceImpl.java` | `security/` | Busca a la persona por su correo y le pone su rol | [6](#6-userdetailsserviceimpljava) |
| `JwtAuthFilter.java` | `security/` | Lee el token en cada petición y deja autenticada a la persona | [7](#7-jwtauthfilterjava) |
| `LoginRequest`, `AuthResponse` | `dto/` | Lo que entra y lo que sale del login | [8](#8-el-login-loginrequest-authresponse-authservice-y-authcontroller) |
| `AuthService`, `AuthController` | `service/`, `controller/` | La lógica y la ruta `POST /api/auth/login` | [8](#8-el-login-loginrequest-authresponse-authservice-y-authcontroller) |
| `ManejadorErroresSeguridad.java` | `security/` | Entrega el 401 y el 403 con el mismo JSON que los demás errores | [9](#9-manejadorerroresseguridadjava-y-los-dos-handlers-nuevos) |
| `SecurityConfig.java` | `security/` | Qué rutas son públicas, cuáles exigen sesión y cuáles un rol | [10](#10-securityconfigjava-las-reglas-de-acceso) |
| `UsuarioController` y `UsuarioService` | `controller/`, `service/` | Se agregan `GET /api/usuarios/me` y `buscarPorCorreo` | [11](#11-lo-que-cambió-en-usuariocontroller-y-usuarioservice) |

> Estado (20 oct): **el código de los pasos 1 a 5 está aplicado en tu repositorio** (comparado archivo por archivo). Confirmado por ti en la conversación: `JwtService` (paso 1). Hechos pero **sin confirmación todavía**: los 56 tests y el flujo de login en Swagger (secciones 13 y 14).

---

## 1. En palabras simples

Piensa en un **hotel**.

| En el hotel | En TecnoFactory |
| --- | --- |
| Llegas a recepción y muestras tu documento. Te dan una **tarjeta magnética** | Haces `POST /api/auth/login` con correo y clave. La API te devuelve un **token** |
| La tarjeta tiene tu nombre, tu nivel de acceso y una **fecha de vencimiento** | El token lleva tu correo, tu rol y cuándo expira (1 hora) |
| Cada puerta tiene un lector. Acercas la tarjeta y la puerta decide | En cada petición mandas el token. El `JwtAuthFilter` lo lee |
| El reglamento dice qué tarjetas abren qué puertas (piscina: huéspedes; bodega: personal) | `SecurityConfig` dice qué rutas abre cada rol |
| Tarjeta falsificada o vencida: la puerta no abre | Token alterado o expirado: **401** |
| Tarjeta válida, pero de huésped en la puerta de la bodega: la puerta no abre | Token válido de `CLIENTE` en una ruta de administrador: **403** |
| Si el hotel desactiva tu habitación, la tarjeta deja de funcionar aunque no haya vencido | El filtro consulta la base en cada petición: un usuario desactivado pierde el acceso al instante |

**Tres ideas para quedarse:**

1. **Autenticación** es *quién eres*. **Autorización** es *qué puedes hacer*. El login resuelve la primera; los roles, la segunda.
2. El token **no se guarda en el servidor**. La API no recuerda quién inició sesión: lo averigua en cada petición leyendo el token. Por eso se dice que es *sin estado* (`STATELESS`).
3. El token está **firmado, no escondido**. Cualquiera puede leer lo que dice; nadie puede cambiarlo sin que la firma falle. Por eso **nunca** lleva la contraseña.

---

## 2. 401 y 403: la diferencia que más se confunde

| Código | Significa | Pasa cuando | Mensaje de la API |
| --- | --- | --- | --- |
| **401** Unauthorized | «No sé quién eres» | No hay token, el token es inválido o expiró, o el login falla | `Se requiere un token válido en la cabecera Authorization: Bearer ...` o `Correo o contraseña incorrectos` |
| **403** Forbidden | «Sé quién eres, pero no puedes» | El token es válido pero el rol no alcanza | `No tienes permiso para esta acción` |

El nombre «Unauthorized» engaña: en HTTP 401 significa **no autenticado**. La decisión que estaba abierta en `cambios-recientes.md` («401 frente a 403») quedó resuelta: son dos errores distintos y cada uno tiene su respuesta.

---

## 3. Qué es un JWT

Un **JWT** (*JSON Web Token*) es un texto con tres partes separadas por puntos:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJhbmFAY29ycmVvLmNsIiwicm9sIjoiQ0xJRU5URSIs... . 5kP3x...
      cabecera                         datos (payload)                           firma
```

| Parte | Contiene | Ejemplo decodificado |
| --- | --- | --- |
| Cabecera | El algoritmo de firma | `{"alg":"HS256"}` |
| Datos (*claims*) | Quién eres y cuándo vence | `{"sub":"ana@correo.cl","rol":"CLIENTE","iat":1760000000,"exp":1760003600}` |
| Firma | Un código calculado con tu `JWT_SECRET` | (texto ilegible) |

| Dato | Qué es |
| --- | --- |
| `sub` (*subject*) | El correo de la persona |
| `rol` | El rol, sin el prefijo `ROLE_` (dato propio de TecnoFactory) |
| `iat` (*issued at*) | Cuándo se emitió |
| `exp` (*expiration*) | Cuándo vence (`jwt.expiration-ms`: 1 hora) |

**Cómo funciona la firma:** el servidor mezcla la cabecera y los datos con `JWT_SECRET` y obtiene la firma (algoritmo HMAC-SHA256). Al recibir un token, repite el cálculo. Si alguien cambió una sola letra de los datos (por ejemplo `CLIENTE` por `ADMINISTRADOR`), la firma ya no coincide y el token se rechaza. Para falsificar uno haría falta conocer el `JWT_SECRET`, por eso **no se comparte y mide al menos 32 caracteres**.

**Qué NO hace:** no cifra. Si pegas un token en una web como jwt.io, verás los datos. Por eso no lleva contraseñas ni nada sensible.

---

## 4. El recorrido completo

### 4.1 Iniciar sesión

```
POST /api/auth/login   {"correo":"ana@correo.cl","clave":"Clave12345"}
   |
   v
[ SecurityConfig ]   -> esta ruta es pública: pasa sin token
   |
   v
[ AuthController ]   -> @Valid: correo y clave no vacíos
   |
   v
[ AuthService ]      -> AuthenticationManager.authenticate(correo, clave)
   |                         |
   |                         v
   |                  [ UserDetailsServiceImpl ] -> busca el usuario en MySQL (hash y rol)
   |                         |
   |                         v
   |                  [ BCrypt ]  -> compara la clave con el hash
   |
   |   falla (cualquier motivo) -> BadCredentialsException -> 401 "Correo o contraseña incorrectos"
   v
[ JwtService ]       -> generarToken(correo, rol)
   |
   v
200 {"token":"eyJ...","tipo":"Bearer","correo":"ana@correo.cl","rol":"CLIENTE"}
```

### 4.2 Una petición con token

```
GET /api/usuarios/me      Authorization: Bearer eyJ...
   |
   v
[ JwtAuthFilter ]  -> ¿hay cabecera "Bearer "? -> ¿el token es válido? -> busca el usuario en MySQL
   |                   ¿está activo? -> lo deja autenticado con su rol
   v
[ SecurityConfig: reglas ]  -> ¿esta ruta exige sesión o un rol? ¿lo cumple?
   |         no autenticado -> 401        sin el rol -> 403   (ambos con el JSON de ErrorResponse)
   v
[ UsuarioController ] -> [ UsuarioService ] -> 200 con el perfil
```

**El filtro nunca rechaza por sí mismo.** Solo identifica. Si el token falta o no vale, deja la petición pasar **sin autenticar**, y es `SecurityConfig` quien responde 401 si esa ruta lo exige. Así las rutas públicas (login, registro, Swagger) funcionan aunque alguien mande un token vencido.

---

## 5. `JwtService.java`

```java
@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${jwt.secret}") String secreto,
                      @Value("${jwt.expiration-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String correo, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_ROL, rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    private Claims leerClaims(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    public boolean esValido(String token) {
        try { leerClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public String obtenerCorreo(String token) { return leerClaims(token).getSubject(); }

    public String obtenerRol(String token) { return leerClaims(token).get(CLAIM_ROL, String.class); }
}
```

| Pieza | Qué significa |
| --- | --- |
| `@Service` | Spring crea **un** `JwtService` y lo entrega a quien lo pida |
| `@Value("${jwt.secret}")` | Lee `jwt.secret` del `application.yaml`, que a su vez toma `JWT_SECRET` del entorno |
| `Keys.hmacShaKeyFor(bytes)` | Convierte el texto del secreto en una clave HMAC. **Si mide menos de 32 bytes lanza una excepción y la app no arranca**: es una protección, no un error |
| `SecretKey clave` | Se construye **una vez** en el constructor y se reutiliza |
| `Jwts.builder()` | Empieza a armar un token |
| `.subject(correo)` / `.claim("rol", rol)` | Los datos que lleva el token |
| `.issuedAt` / `.expiration` | Emisión y vencimiento (`ahora + 1 hora`) |
| `.signWith(clave)` | Firma. La librería (jjwt 0.12.6) elige HS256 por el tamaño de la clave |
| `.compact()` | Produce el texto final `xxxxx.yyyyy.zzzzz` |
| `leerClaims` (privado) | Comprueba **firma y vencimiento** y devuelve los datos. Si algo falla, lanza `JwtException` |
| `esValido` | Responde `true`/`false` sin lanzar excepciones. Atrapa `IllegalArgumentException` por si llega `null` o texto vacío |
| `obtenerCorreo` / `obtenerRol` | Leen los datos. Se llaman **solo después** de `esValido` |

---

## 6. `UserDetailsServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasenia())
                .roles(usuario.getTipoUsuario().getNombre())
                .disabled(!usuario.getActivo())
                .build();
    }
}
```

| Pieza | Qué significa |
| --- | --- |
| `implements UserDetailsService` | El contrato de Spring Security: «dame un nombre de usuario y te devuelvo a la persona». Aquí el nombre de usuario es el **correo** |
| `findByCorreo(...)` | El método del repository del paso 4 |
| `.orElseThrow(UsernameNotFoundException)` | Si el correo no existe. El mensaje es genérico a propósito: no revela qué correos están registrados |
| `User.builder()` | Convierte tu `Usuario` en el objeto que entiende Spring Security (`UserDetails`) |
| `.password(hash)` | El **hash** BCrypt de la base, nunca la clave en texto plano |
| `.roles("CLIENTE")` | Agrega el prefijo y guarda `ROLE_CLIENTE`. Ese prefijo es el que usan después `hasRole(...)` |
| `.disabled(!activo)` | Un usuario con `activo = false` **no puede iniciar sesión** |
| `@Transactional(readOnly = true)` | `tipoUsuario` es `LAZY`: con `open-in-view: false` hay que leerlo dentro de una transacción o fallaría con `LazyInitializationException` |

**Efecto visible:** al existir tu propio `UserDetailsService`, Spring deja de crear su usuario de prueba y **desaparece del log** el aviso `Using generated security password`.

---

## 7. `JwtAuthFilter.java`

```java
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO.length()).trim();

            if (jwtService.esValido(token)) {
                try {
                    UserDetails usuario = userDetailsService.loadUserByUsername(jwtService.obtenerCorreo(token));
                    if (usuario.isEnabled()) {
                        UsernamePasswordAuthenticationToken autenticacion =
                                UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
                        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    }
                } catch (UsernameNotFoundException e) {
                    // El usuario ya no existe: se trata como petición sin autenticar
                }
            }
        }

        cadena.doFilter(request, response);
    }
}
```

| Pieza | Qué significa |
| --- | --- |
| `extends OncePerRequestFilter` | Spring garantiza que el filtro corre **una sola vez** por petición |
| `request.getHeader("Authorization")` | Lee la cabecera donde viaja el token |
| `startsWith("Bearer ")` | Solo actúa si la cabecera empieza con `Bearer ` (con espacio). Una cabecera `Basic ...` se ignora |
| `getAuthentication() == null` | No pisa una autenticación que ya exista |
| `jwtService.esValido(token)` | Firma y vencimiento. Si falla, **no se consulta la base de datos** |
| `loadUserByUsername(correo)` | Consulta la base **en cada petición** |
| `usuario.isEnabled()` | Si el usuario fue desactivado, no queda autenticado aunque su token siga vigente |
| `authenticated(usuario, null, authorities)` | El objeto «esta persona está autenticada y tiene estos roles». La credencial es `null` porque ya no hace falta |
| `SecurityContextHolder...setAuthentication` | Lo guarda para el resto de la petición. Es de donde luego sale `Authentication` en el controller |
| `catch UsernameNotFoundException` | El usuario fue borrado después de emitir el token: se trata como no autenticado, **sin error 500** |
| `cadena.doFilter(...)` | **Siempre** continúa; el filtro nunca responde por sí mismo |

**Por qué no lleva `@Component`:** si lo tuviera, Spring lo registraría solo y `@WebMvcTest` lo cargaría en cada test de controller, que fallaría por no encontrar `JwtService`. `SecurityConfig` lo crea con `new JwtAuthFilter(...)` y lo agrega a la cadena.

**Por qué consulta la base en cada petición, si el token ya dice quién es:** porque así desactivar a un usuario, borrarlo o **cambiarle el rol** surte efecto **de inmediato**. A cambio, hay una consulta por petición. El rol que decide es el de la base, no el que viaja dentro del token.

---

## 8. El login: `LoginRequest`, `AuthResponse`, `AuthService` y `AuthController`

```java
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String correo,

        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 72, message = "La clave no puede superar los 72 caracteres")
        String clave
) {
    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + "]";
    }
}

public record AuthResponse(String token, String tipo, String correo, String rol) { }
```

| Pieza | Qué significa |
| --- | --- |
| `LoginRequest` | Solo correo y clave. Misma idea que `UsuarioRequest`: validaciones en el DTO |
| `@Size(max = 72)` en la clave | BCrypt solo usa los primeros 72 bytes; además evita que alguien envíe un texto enorme |
| `toString()` sobrescrito | La clave **nunca aparece en un log**, aunque alguien escriba `log.info("{}", request)` |
| `AuthResponse` | Lo que recibe el frontend: el `token`, el `tipo` (`"Bearer"`), el `correo` y el `rol`. **El frontend usa `rol` para decidir qué botones mostrar** |

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String PREFIJO_ROL = "ROLE_";
    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correo(), request.clave()));
        } catch (AuthenticationException e) {
            log.warn("Intento de login fallido");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String rol = autenticacion.getAuthorities().iterator().next().getAuthority().substring(PREFIJO_ROL.length());
        String token = jwtService.generarToken(autenticacion.getName(), rol);
        return new AuthResponse(token, TIPO_TOKEN, autenticacion.getName(), rol);
    }
}
```

| Pieza | Qué significa |
| --- | --- |
| `AuthenticationManager` | El «verificador» de Spring Security. Recibe correo y clave, usa `UserDetailsServiceImpl` y BCrypt, y devuelve la persona autenticada o lanza una excepción |
| `UsernamePasswordAuthenticationToken(correo, clave)` | El «paquete» con las credenciales que se le entrega al verificador (todavía **sin** autenticar) |
| `catch (AuthenticationException e)` | Atrapa **todos** los fallos: correo inexistente, clave incorrecta y usuario desactivado |
| `throw new BadCredentialsException("Credenciales inválidas")` | Los tres fallos devuelven **el mismo** error, así nadie puede averiguar qué correos existen |
| `log.warn("Intento de login fallido")` | Registra el intento **sin** correo ni clave |
| `getAuthorities()...substring(5)` | Toma `ROLE_CLIENTE` y le quita `ROLE_` para dejar `CLIENTE` |
| `jwtService.generarToken(correo, rol)` | Crea el token |

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

`@SecurityRequirements` vacío **quita el candado de Swagger** en esta ruta, porque es pública. Lo mismo hace `UsuarioController.registrar`.

**El `AuthenticationManager` es un bean que arma `SecurityConfig`** (sección 10), con `DaoAuthenticationProvider` y el `PasswordEncoder`. Se escribió explícito para no depender de la configuración automática.

---

## 9. `ManejadorErroresSeguridad.java` y los dos handlers nuevos

**El problema:** los errores de seguridad ocurren en los **filtros**, antes de llegar a un controller. `GlobalExceptionHandler` solo ve lo que sale de un controller, así que un 401 o un 403 saldrían con la respuesta por defecto de Spring, distinta al resto.

**La solución:**

```java
@RequiredArgsConstructor
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
```

| Pieza | Qué significa |
| --- | --- |
| `AuthenticationEntryPoint` | «Qué hacer cuando alguien **sin sesión** pide una ruta protegida» → 401 |
| `AccessDeniedHandler` | «Qué hacer cuando alguien **con sesión** pide algo que su rol no permite» → 403 |
| `HandlerExceptionResolver` | La pieza de Spring que busca el `@ExceptionHandler` adecuado. Se le **reenvía** el error y responde `GlobalExceptionHandler` |
| `resolveException(..., null, ex)` | Resuelve la excepción como si hubiera salido de un controller |
| Sin `@Component` | `SecurityConfig` la crea con `new` |

Y en `GlobalExceptionHandler` se agregaron dos handlers:

| Excepción | Respuesta | Cuándo |
| --- | --- | --- |
| `BadCredentialsException` | **401** `Correo o contraseña incorrectos` | Login fallido |
| `AuthenticationException` | **401** `Se requiere un token válido en la cabecera Authorization: Bearer ...` | Ruta protegida sin token, o con token inválido o expirado |

Spring elige **el handler más específico**: `BadCredentialsException` es un tipo de `AuthenticationException`, así que el login fallido usa el primero y una ruta sin token usa el segundo. El 403 reutiliza el handler de `AccessDeniedException` que ya existía.

---

## 10. `SecurityConfig.java`: las reglas de acceso

```java
@Bean
public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                   PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(userDetailsService);
    proveedor.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(proveedor);
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
        UserDetailsService userDetailsService,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {

    ManejadorErroresSeguridad errores = new ManejadorErroresSeguridad(resolver);

    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                .requestMatchers("/api/usuarios/**").hasRole(ROL_ADMINISTRADOR)
                .anyRequest().authenticated())
        .exceptionHandling(e -> e
                .authenticationEntryPoint(errores)
                .accessDeniedHandler(errores))
        .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### 10.1 Las reglas, en una tabla

| Ruta | Quién entra | Por qué |
| --- | --- | --- |
| Swagger, `/v3/api-docs/**`, `/actuator/health` | Todos | Documentación y salud (Swagger está apagado en `prod`) |
| `POST /api/auth/login` | Todos | Para poder iniciar sesión hay que entrar sin sesión |
| `POST /api/usuarios` (registro) | Todos | Una persona nueva no tiene token |
| `GET /api/usuarios/me` | Cualquiera **con sesión** | Su propio perfil |
| Cualquier otra ruta de `/api/usuarios/**` (por ejemplo `GET /api/usuarios/7`) | Solo `ADMINISTRADOR` | Si fuera «cualquiera con sesión», un cliente leería los datos de otros cambiando el id |
| **Todo lo demás** | Cualquiera con token válido | `anyRequest().authenticated()`: lo olvidado queda **protegido por defecto**, no abierto |

### 10.2 Línea por línea

| Línea | Qué significa |
| --- | --- |
| `.csrf(disable)` | CSRF es un ataque a sesiones con *cookies*; esta API usa tokens en una cabecera, así que no aplica ([15, 3.4](15-seguridad-codigo-explicado.md#34-por-qué-se-desactiva-csrf)) |
| `.cors(Customizer.withDefaults())` | Aplica la configuración de `CorsConfig` antes que el resto de filtros ([20](20-configuracion-base-explicada.md#6-cors)) |
| `STATELESS` | Spring Security no crea sesión ni cookie; cada petición se valida sola con su token |
| `.requestMatchers(HttpMethod.POST, "...")` | La regla vale solo para ese verbo. `GET /api/usuarios` y `POST /api/usuarios` son reglas distintas |
| `.permitAll()` / `.authenticated()` / `.hasRole("X")` | Todos / con sesión / con sesión **y** ese rol (Spring agrega `ROLE_` solo) |
| `.exceptionHandling(...)` | Conecta `ManejadorErroresSeguridad` para el 401 y el 403 en JSON |
| `.addFilterBefore(new JwtAuthFilter(...), UsernamePasswordAuthenticationFilter.class)` | Pone nuestro filtro **antes** del filtro de login por formulario, para que la persona ya esté identificada cuando se evalúan las reglas |
| `@Qualifier("handlerExceptionResolver")` | Spring tiene varios `HandlerExceptionResolver`; este nombre elige el que usa los `@ExceptionHandler` |
| Desapareció `.httpBasic(...)` | Ya no se acepta usuario y clave en cada petición: solo tokens |

### 10.3 El orden de las reglas importa

Spring evalúa las reglas **de arriba hacia abajo y se detiene en la primera que coincide**:

- `GET /api/usuarios/me` va **antes** de `/api/usuarios/**`. Si estuviera después, `/me` caería en «solo administrador» y un cliente no podría ver su perfil.
- `anyRequest()` va **siempre al final**. Escribir una regla después lanza `Can't configure requestMatchers after anyRequest` al arrancar.

**Un detalle del controller:** `/me` está declarado antes de `/{id}`. Spring prefiere la ruta literal, así que `/api/usuarios/me` nunca se interpreta como un id.

---

## 11. Lo que cambió en `UsuarioController` y `UsuarioService`

```java
// UsuarioController
@PostMapping
@SecurityRequirements
@Operation(summary = "Registrar un usuario con rol CLIENTE (público)")
public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) { ... }

@GetMapping("/me")
@Operation(summary = "Ver mi propio perfil (cualquier usuario con sesión)")
public UsuarioResponse miPerfil(Authentication autenticacion) {
    return usuarioService.buscarPorCorreo(autenticacion.getName());
}

@GetMapping("/{id}")
@Operation(summary = "Buscar un usuario por id (solo administrador)")
public UsuarioResponse buscarPorId(@PathVariable Long id) { ... }

// UsuarioService
@Transactional(readOnly = true)
public UsuarioResponse buscarPorCorreo(String correo) {
    Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo));
    return usuarioMapper.toResponse(usuario);
}
```

| Pieza | Qué significa |
| --- | --- |
| `Authentication autenticacion` (parámetro) | Spring lo rellena solo con la persona que dejó autenticada `JwtAuthFilter`. `getName()` devuelve su correo |
| `miPerfil` | **La persona nunca elige un id**: el servidor usa el del token. Así nadie puede pedir el perfil ajeno por error o a propósito |
| `buscarPorCorreo` | Igual que `buscarPorId` pero por correo; mapea dentro de la transacción |
| `@SecurityRequirements` en `registrar` | Quita el candado de Swagger: el registro es público |

---

## 12. Los roles: qué hay hoy y qué viene

Los tres roles viven en la tabla `tipo_usuario` (`ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`) y vienen de `V2`. El panel que describiste tiene este reparto:

| Capacidad | CLIENTE | VENDEDOR | ADMINISTRADOR | Estado en la API |
| --- | --- | --- | --- | --- |
| Registrarse, iniciar sesión | Sí | Sí | Sí | **Hecho** |
| Ver su propio perfil (`GET /api/usuarios/me`) | Sí | Sí | Sí | **Hecho** |
| Ver un usuario por id | No | No | Sí | **Hecho** (solo administrador) |
| Ver la lista de usuarios | No | **Sí** (solo lectura) | Sí | Pendiente: `GET /api/usuarios` con `hasAnyRole` |
| Editar su **propio** perfil | Sí | Sí | Sí | Pendiente: `PUT /api/usuarios/me` |
| Editar o desactivar a **otros** | No | No | Sí | Pendiente |
| Crear vendedores | No | No | Sí | Pendiente: ruta exclusiva (el rol nunca viene del cliente) |
| Productos: crear, editar, eliminar, ajustar stock | No | **Sí** | Sí | Con el controller de `Producto` |
| Órdenes: ver las ventas | No | **Sí** | Sí | Con el controller de `Boleta` |
| Categorías y reportes | No | No | Sí | Con sus controllers |
| Panel de administración | No (sin botón) | Sí, reducido | Sí, completo | El frontend lo muestra según `AuthResponse.rol` |

**Cómo se agrega cada regla:** una línea en `SecurityConfig`, por ejemplo `.requestMatchers(HttpMethod.GET, "/api/usuarios").hasAnyRole("ADMINISTRADOR", "VENDEDOR")` **antes** de la regla de `/api/usuarios/**`. Las reglas de «solo lo propio» (editar el propio perfil) se resuelven en el service comparando el correo del token con el recurso.

**Ocultar un botón no protege nada.** El frontend usa `rol` para ocultar el panel a un cliente, pero cualquiera puede llamar a la API a mano. La protección real está en `SecurityConfig`.

---

## 13. Las pruebas (56 en total)

| Archivo | Tests | Qué comprueba |
| --- | --- | --- |
| `JwtServiceTest` | 6 | Token válido con correo y rol; **expirado**, **firmado con otra clave**, **alterado**, texto que no es un token (vacío y `null`) y secreto de menos de 32 bytes |
| `UserDetailsServiceImplTest` | 3 | Carga el hash y `ROLE_...`; usuario inactivo queda deshabilitado; correo inexistente |
| `JwtAuthFilterTest` | 6 | Token válido autentica; sin cabecera, cabecera `Basic` e inválido **no** autentican (el inválido ni toca la base); inactivo e inexistente no autentican y no lanzan error |
| `AuthServiceTest` | 3 | Login correcto con el rol sin prefijo; credenciales incorrectas no generan token; usuario desactivado recibe **el mismo** error |
| `AuthControllerTest` | 3 | 200 con token; 401 con credenciales incorrectas; 400 con correo vacío |
| `UsuarioControllerTest` | 14 | Los 9 de antes, más: sin token 401 en JSON, cliente en ruta de administrador 403, `/me`, token Bearer válido a través del filtro y token inválido 401 |
| `UsuarioServiceTest` | 6 | Los 5 de antes y `buscarPorCorreo` inexistente |
| `UsuarioRequestTest`, `RunUtilTest`, `PageResponseTest` | 10 + 4 + 1 | Sin cambios |

```bash
./mvnw test -Dtest="*Test"        # esperado: 56 tests, 0 fallos
```

`UsuarioControllerTest` ahora lleva tres `@MockitoBean` (`UsuarioService`, `JwtService` y `UserDetailsService`) porque `SecurityConfig` los necesita para armar la cadena.

---

## 14. Cómo probarlo a mano

Con la app arrancada y Swagger en `/swagger-ui/index.html`:

| # | Prueba | Esperado |
| --- | --- | --- |
| 1 | `POST /api/auth/login` con `ADMIN_EMAIL` y `ADMIN_PASSWORD` de tu `.env` | 200 con `token`, `tipo`, `correo` y `rol: ADMINISTRADOR` |
| 2 | Botón **Authorize**: pega el token **sin** escribir `Bearer` (Swagger lo agrega) | El candado se cierra |
| 3 | `GET /api/usuarios/me` | 200 con el administrador |
| 4 | `GET /api/usuarios/1` con el token | 200 |
| 5 | Mismo `GET` con **Logout** en Authorize | 401 `Se requiere un token válido...` |
| 6 | `POST /api/usuarios` registra un cliente (sin token) | 201 |
| 7 | Login con el cliente; con su token, `GET /api/usuarios/me` | 200 |
| 8 | Con el token del cliente, `GET /api/usuarios/1` | **403** `No tienes permiso...` |
| 9 | Login con clave incorrecta, y con un correo que no existe | **El mismo** 401 `Correo o contraseña incorrectos` |
| 10 | Poner `activo = 0` a un usuario en la base y hacer login | 401 (igual que clave incorrecta). Si ya tenía un token, también deja de funcionar |

Con `curl`:

```bash
curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"admin@correo.cl","clave":"TU_CLAVE"}'

curl -i localhost:8080/api/usuarios/me -H "Authorization: Bearer EL_TOKEN"
```

---

## 15. Qué se verificó y qué no

| Comprobación | Estado |
| --- | --- |
| Código de los pasos 1 a 5 presente en tu repositorio | **Verificado** (20 oct, comparación de archivos) |
| `JwtService` y su test (6) | **Confirmado por ti** (paso 1) |
| `UserDetailsServiceImpl`, `JwtAuthFilter` y sus tests | Entregados; seguiste con el paso siguiente sin reportar fallos |
| Los 56 tests sin fallos | **Pendiente**: no hay salida de `./mvnw test` posterior a los pasos 4 y 5 |
| La app arranca con `SecurityConfig` nueva (bean `handlerExceptionResolver`, constructor de `DaoAuthenticationProvider`) | **Pendiente** |
| Flujo completo de la sección 14 en Swagger contra MySQL | **Pendiente** |
| Que desaparezca el aviso `Using generated security password` del log | **Pendiente** |
| Que `UsuarioService`, `UsuarioController` y los tests nuevos no se ejecutaron donde se escribieron | No se pudo compilar fuera de tu máquina |

---

## 16. Errores relacionados

| Síntoma | Causa | Solución |
| --- | --- | --- |
| La app no arranca: `WeakKeyException` o «The specified key byte array is ... bits» | `JWT_SECRET` mide menos de 32 caracteres | Alargar el secreto en el `.env` |
| `Could not resolve placeholder 'jwt.secret'` | Falta la clave en `application.yaml` | Revisar el bloque `jwt:` |
| La app no arranca: `No qualifying bean ... HandlerExceptionResolver` | El nombre del `@Qualifier` | Debe ser `handlerExceptionResolver` |
| `UsuarioControllerTest` falla con `No qualifying bean of type '...JwtService'` | Falta un `@MockitoBean` | Agregar `JwtService` y `UserDetailsService` al test |
| Login correcto, pero todas las rutas dan 401 | No se envía la cabecera, o se escribió `Bearer` doble en Swagger | `Authorization: Bearer <token>` (en Swagger solo el token) |
| Un cliente recibe 403 en `GET /api/usuarios/7` | Es lo esperado: solo el administrador | Usar `/api/usuarios/me` |
| `/api/usuarios/me` da 403 a un cliente | La regla de `/me` está **después** de `/api/usuarios/**` | Reordenar ([10.3](#103-el-orden-de-las-reglas-importa)) |
| 401 con un token que funcionaba | Expiró (1 hora), o el usuario fue desactivado o borrado | Iniciar sesión de nuevo |
| El navegador bloquea las llamadas del frontend | CORS | `CORS_ORIGINS` con el origen del frontend ([20, sección 6](20-configuracion-base-explicada.md#6-cors)) |
| 401 al llamar a `POST /api/usuarios` | La regla del registro falta o está tras `/api/usuarios/**` | Debe ir antes |
| Dos clases con el mismo nombre en `main` y `test` | Se copió una clase a `src/test` | Borrar la copia de `test` |

---

## 17. Decisiones y límites

| Tema | Decisión o límite |
| --- | --- |
| Mismo error para correo inexistente, clave incorrecta y usuario desactivado | Evita que se descubran los correos registrados |
| `GET /api/usuarios/{id}` solo para administrador | Un cliente no debe leer a otros; el perfil propio es `/me` |
| El rol que decide es el de la **base de datos**, no el del token | Cambiar un rol o desactivar a alguien surte efecto de inmediato; cuesta una consulta por petición |
| El token **no se puede revocar** antes de que expire | Cerrar sesión es solo borrar el token en el frontend. Vida corta (1 hora) para limitar el daño |
| Sin *refresh token* | Al vencer, hay que iniciar sesión de nuevo. Se puede agregar más adelante |
| Sin límite de intentos de login | Pendiente (fuerza bruta) |
| Dónde guarda el frontend el token | `localStorage` es lo más simple pero legible por un script malicioso (XSS); se decide en la fase del frontend |
| Sin HTTPS | En producción es obligatorio: el token y la clave viajarían en texto plano |

---

## 18. Lo que viene

| Paso | Qué |
| --- | --- |
| **Confirmar** | Los 56 tests, el arranque y el flujo de la sección 14 |
| **Limpieza** | Mover `JwtServiceTest` y `UserDetailsServiceImplTest` a `src/test/.../security/` (están en `service/`); borrar los `// NUEVO` de `UsuarioService`; borrar `docs/11-cambios-recientes.md` |
| **Usuarios** | `GET /api/usuarios` (lista paginada, administrador y vendedor), `PUT /api/usuarios/me`, y una ruta de administrador para crear vendedores |
| **Primera rebanada** | `Categoria` siguiendo la [receta de la guía 19](19-validacion-run-service-y-controller-explicados.md#13-la-receta-para-la-próxima-entidad), con sus reglas por rol |
| **Después** | `Producto` (vendedor y administrador), `Carrito` y `Boleta` |
