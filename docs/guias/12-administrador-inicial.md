# Guía 12. Administrador inicial

> **En una frase:** al arrancar, la app crea sola al primer administrador a partir de dos variables de entorno, y no lo duplica si ya existe.

## La idea

Sin un administrador, nadie puede crear productos ni categorías. Pero una cuenta con clave real **no puede venir dentro de una migración**: quedaría para siempre en Git. Por eso se crea **por código**, con la clave tomada del entorno y guardada como hash.

## Las decisiones

| Decisión | Motivo |
| --- | --- |
| Correo y clave salen de `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Nada secreto queda en Git |
| Se crea **solo si no existe** un administrador | Reiniciar mil veces no duplica |
| La clave se guarda con **BCrypt** | Si alguien lee la base, no obtiene la clave |
| El rol se busca **por nombre** (`ADMINISTRADOR`) | Los ids los asigna `AUTO_INCREMENT` |
| El resto de los datos son **valores de relleno** fijos (RUN `11111111-1`, comuna `13101`...) | Son datos de sistema; se pueden editar después |

## Las piezas

| Pieza | Qué hace |
| --- | --- |
| `AdministradorInicialRunner` | Corre **después** del arranque, con Tomcat ya arriba, y lee las dos variables |
| `DatosAdministrador` | Un `record` con los datos a guardar |
| `UsuarioService.crearAdministradorSiNoExiste` | Comprueba (`existsByTipoUsuarioNombre`), busca rol y comuna, hashea y guarda |

## Cómo se usa

```bash
set -a; source .env; set +a      # carga ADMIN_EMAIL y ADMIN_PASSWORD
./mvnw spring-boot:run           # (con las demás variables del perfil local)
```

| Caso | Esperado en el log |
| --- | --- |
| Primera vez | `Administrador inicial creado con el correo ...` |
| Segundo arranque | `Ya existe un administrador: no se crea otro` y **ningún** `insert` |
| Sin las variables | `WARN ... sin definir` y la app **sí** arranca |

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
2. **No uses `V4` con un hash pegado**: queda en Git para siempre.
3. **Tras `docker compose down -v`** el administrador se vuelve a crear al arrancar (se reaplican `V1`, `V2` y `V3`).
4. **Pendiente:** confirmar el segundo arranque (`Ya existe un administrador`).

## Si algo falla

| Síntoma | Causa | Qué hacer |
| --- | --- | --- |
| No se crea el administrador y no hay error | Faltan las variables (hay un `WARN`) | Cárgalas y reinicia |
| `Rol no encontrado` | `V2` no se aplicó | Revisa `flyway_schema_history` |
| El login del administrador da 401 | Clave distinta a `ADMIN_PASSWORD`, o el administrador se creó antes con otra | Borra la fila (solo desarrollo) y reinicia |

## Para saber más

[Referencia 17: código línea por línea, orden de arranque y comandos](../referencia/17-administrador-inicial-explicado.md)
