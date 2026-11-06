# Guía 12. Administrador inicial

> **En una frase:** al arrancar, la app crea sola al primer administrador a partir de variables de entorno, y no lo duplica si ya existe.

## La idea

Sin un administrador, nadie puede crear productos ni categorías. Pero una cuenta con clave real **no puede venir dentro de una migración**: quedaría para siempre en Git. Por eso se crea **por código**, con la clave tomada del entorno y guardada como hash.

## Las decisiones

| Decisión | Motivo |
| --- | --- |
| Todos los datos salen de variables `ADMIN_*` | Nada secreto ni personal queda en Git |
| Si un `ADMIN_*` opcional falta, se usa un **valor de relleno** y se escribe un `WARN` | La app arranca igual en desarrollo, pero deja rastro de que falta configurar |
| Se crea **solo si no existe** un administrador | Reiniciar mil veces no duplica |
| La clave se guarda con **BCrypt** | Si alguien lee la base, no obtiene la clave |
| La clave debe medir **8 a 72 caracteres** y no superar **72 bytes UTF-8** | Es el límite real de BCrypt: más allá se corta en silencio |
| Se rechazan los valores de ejemplo de `.env.example` | Evita desplegar con `CAMBIAR` o `cambia_esta_clave` |
| El rol se busca **por nombre** (`ADMINISTRADOR`) | Los ids los asigna `AUTO_INCREMENT` |

## Las piezas

| Pieza | Qué hace |
| --- | --- |
| `AdministradorInicialRunner` | Corre **después** del arranque, con Tomcat ya arriba, lee las variables `ADMIN_*` y valida los datos |
| `ValidadorClaves` | Comprueba el largo de `ADMIN_PASSWORD` y detecta los valores de ejemplo |
| `DatosAdministrador` | Un `record` con los datos a guardar |
| `UsuarioService.crearAdministradorSiNoExiste` | Comprueba (`existsByTipoUsuarioNombre`), busca rol y comuna, hashea y guarda |

## Cómo se usa

```bash
set -a; source .env; set +a      # carga ADMIN_EMAIL, ADMIN_PASSWORD y los ADMIN_* opcionales
./mvnw spring-boot:run           # (con las demás variables del perfil local)
```

| Caso | Esperado |
| --- | --- |
| Primera vez | `Administrador inicial creado con el correo ...` |
| Segundo arranque | `Ya existe un administrador: no se crea otro` y **ningún** `insert` |
| Sin `ADMIN_EMAIL` o `ADMIN_PASSWORD` | `WARN ... sin definir` y la app **sí** arranca, sin crear el administrador |
| Faltan datos opcionales | Se usan valores de relleno y aparece `WARN ... usa datos de relleno` |
| Clave con el valor de ejemplo | La app **no arranca**: `ADMIN_PASSWORD conserva el valor de ejemplo de .env.example` |
| Clave de menos de 8 o más de 72 caracteres (o 72 bytes) | La app **no arranca** con el mensaje de `ValidadorClaves` |
| Correo, RUN, fecha o teléfono inválidos | La app **no arranca** indicando la variable a corregir |

Comprobar la fila:

```bash
docker exec -it tecnofactory-db mysql -uroot -p tecnofactory -e "
SELECT u.correo, t.nombre_tipo_usuario AS rol, LEFT(u.contrasenia,7) AS prefijo,
       LENGTH(u.contrasenia) AS largo, u.activo
FROM usuario u JOIN tipo_usuario t ON t.id_tipo_usuario = u.tipo_usuario_id_tipo_usuario;"
```

Esperado: tu correo, `ADMINISTRADOR`, `$2a$10$`, `60`, `1`.

## Reglas que no hay que olvidar

1. **La clave nunca va a un log ni a un mensaje de error.**
2. **No uses una migración con un hash pegado**: queda en Git para siempre.
3. **Tras `docker compose down -v`** el administrador se vuelve a crear al arrancar (se reaplican todas las migraciones).
4. **Cambiar un `ADMIN_*` después del primer arranque no modifica la fila existente**: solo se usa cuando aún no hay administrador.
5. **No dejes los valores de ejemplo**: `ConfiguracionValidator` y el runner rechazan los placeholders al arrancar.

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| No se crea el administrador y no hay error | Faltan las variables (hay un `WARN`) | Cárgalas y reinicia |
| `ADMIN_PASSWORD conserva el valor de ejemplo` | El `.env` sigue con el placeholder de `.env.example` | Genera una clave real (`openssl rand -hex 16`) |
| `ADMIN_PASSWORD debe tener entre 8 y 72 caracteres...` | Clave demasiado corta o larga (o con acentos que superan 72 bytes) | Usa una clave de 8 a 72 caracteres ASCII |
| `ADMIN_EMAIL no tiene un formato válido` / `ADMIN_RUN no es un número válido` / `ADMIN_FECHA_NACIMIENTO debe ser anterior a hoy` | Dato mal escrito en el `.env` | Corrige la variable que indica el mensaje |
| `Rol no encontrado` | `V2` no se aplicó | Revisa `flyway_schema_history` |
| El login del administrador da 401 | Clave distinta a `ADMIN_PASSWORD`, o el administrador se creó antes con otra | Borra la fila (solo desarrollo) y reinicia |

## Para saber más

[Referencia 17: código línea por línea, orden de arranque y comandos](../referencia/17-administrador-inicial-explicado.md)
