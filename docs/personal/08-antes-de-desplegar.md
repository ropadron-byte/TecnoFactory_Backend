# 08. Antes de desplegar

Lista para revisar **antes** de publicar la API en un servidor real. Nada de esto está hecho todavía, salvo lo marcado.

## Secretos

- [ ] `JWT_SECRET` nuevo, aleatorio, de 32 o más **bytes**, **distinto** del de desarrollo.
- [ ] `ADMIN_PASSWORD` fuerte, de 8 a 72 caracteres, y distinta; cámbiala tras el primer ingreso.
- [ ] `DB_PASSWORD` distinta de la de desarrollo.
- [ ] Ninguna variable conserva los valores de ejemplo de `.env.example` (`CAMBIAR`, `cambia_esta_clave`): `ConfiguracionValidator` **impide arrancar** si los detecta.
- [ ] El `.env` **no** está en Git (`git check-ignore .env`). *(Ya ignorado.)*
- [ ] Ninguna clave aparece en un log ni en un mensaje de error.
- [ ] Los `ADMIN_*` opcionales están definidos (o se aceptan los valores de relleno y su `WARN`): `ADMIN_RUN`, `ADMIN_RUN_DV`, `ADMIN_NOMBRE`, `ADMIN_APELLIDOS`, `ADMIN_FECHA_NACIMIENTO`, `ADMIN_DIRECCION`, `ADMIN_TELEFONO`, `ADMIN_COMUNA`.

## Base de datos

- [ ] La app usa el usuario `tecnofactory_app` (no `root`).
- [ ] Migraciones por `tecnofactory_migrator`.
- [ ] `ddl-auto: validate` (no `update`). *(Ya así.)*
- [ ] Copias de seguridad programadas.
- [ ] MySQL **no** expuesto a internet.

Comprobar que la app **no** puede cambiar el esquema:

```sql
SHOW GRANTS FOR 'tecnofactory_app'@'%';
```

Debe mostrar **solo** `SELECT, INSERT, UPDATE, DELETE` sobre `tecnofactory.*` (sin `CREATE`, `ALTER`, `DROP` ni `GRANT OPTION`). Si aparece algo más, revócalo antes de desplegar.

## Perfil de producción

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Swagger apagado. *(Ya en `prod`.)*
- [ ] Logs en `WARN` para librerías e `INFO` para tu código. *(Ya en `prod`.)*
- [ ] `show-sql` apagado.
- [ ] `CORS_ORIGINS` con el dominio real del frontend (no `localhost`).

## Seguridad pendiente

- [ ] **HTTPS** (sin él, las claves y los tokens viajan en claro).
- [ ] **Límite de intentos de login** (contra fuerza bruta).
- [ ] Revisar que cada ruta nueva tenga su regla de rol: `SecurityConfig` termina en `anyRequest().denyAll()`.
- [ ] Confirmar que `GET /api/imagenes/**` exige personal y que `/uploads/**` sigue público con `X-Content-Type-Options: nosniff` y `Content-Security-Policy: default-src 'none'`.
- [ ] Confirmar que el blog público (incluido `/api/blogs/{id}`) **no** devuelve `autorCorreo`; el correo solo sale en `/api/blogs/gestion/{id}`.
- [ ] Decidir dónde guarda el frontend el token.

## Aplicación

- [ ] `Dockerfile` del backend y compose de producción (fase 9).
- [ ] `./mvnw test -Dtest="*Test"` en verde. *(6 nov: 204 tests, 0 fallos.)*
- [ ] `contextLoads` en verde contra una base real.
- [ ] Arranque con una clave de ejemplo: debe **fallar** con un mensaje que nombre la variable, sin imprimir el valor.
- [ ] Arranque con `ADMIN_PASSWORD` de 7 caracteres o con más de 72 bytes: debe **fallar**.
- [ ] `/actuator/health` responde `UP`.
- [ ] Zona horaria `America/Santiago` verificada en fechas guardadas.

## Prueba final

1. Arranca desde cero con una base vacía.
2. Comprueba que se crean las tablas y el administrador.
3. Inicia sesión; prueba 401, 403 y 200 con cada rol.
4. Reinicia: no debe duplicarse el administrador.

## Para saber más

[Guía 04: perfiles](../guias/04-perfiles-y-configuracion-base.md) · [Guía 06](../guias/06-ejecutar-local-y-prod.md) · [Guía 16: hoja de ruta](../guias/16-hoja-de-ruta.md)
