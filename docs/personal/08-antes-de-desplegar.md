# 08. Antes de desplegar

Lista para revisar **antes** de publicar la API en un servidor real. Nada de esto está hecho todavía, salvo lo marcado.

## Secretos

- [ ] `JWT_SECRET` nuevo, aleatorio, de 32 o más caracteres, **distinto** del de desarrollo.
- [ ] `ADMIN_PASSWORD` fuerte y distinta; cámbiala tras el primer ingreso.
- [ ] `DB_PASSWORD` distinta de la de desarrollo.
- [ ] El `.env` **no** está en Git (`git check-ignore .env`). *(Ya ignorado.)*
- [ ] Ninguna clave aparece en un log ni en un mensaje de error.

## Base de datos

- [ ] La app usa el usuario `tecnofactory_app` (no `root`).
- [ ] Migraciones por `tecnofactory_migrator`.
- [ ] `ddl-auto: validate` (no `update`). *(Ya así.)*
- [ ] Copias de seguridad programadas.
- [ ] MySQL **no** expuesto a internet.

## Perfil de producción

- [ ] `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Swagger apagado. *(Ya en `prod`.)*
- [ ] Logs en `WARN` para librerías e `INFO` para tu código. *(Ya en `prod`.)*
- [ ] `show-sql` apagado.
- [ ] `CORS_ORIGINS` con el dominio real del frontend (no `localhost`).

## Seguridad pendiente

- [ ] **HTTPS** (sin él, las claves y los tokens viajan en claro).
- [ ] **Límite de intentos de login** (contra fuerza bruta).
- [ ] Revisar que cada ruta nueva tenga su regla de rol.
- [ ] Decidir dónde guarda el frontend el token.

## Aplicación

- [ ] `Dockerfile` del backend y compose de producción (fase 9).
- [ ] `./mvnw test -Dtest="*Test"` en verde.
- [ ] `contextLoads` en verde contra una base real.
- [ ] `/actuator/health` responde `UP`.
- [ ] Zona horaria `America/Santiago` verificada en fechas guardadas.

## Prueba final

1. Arranca desde cero con una base vacía.
2. Comprueba que se crean las 13 tablas y el administrador.
3. Inicia sesión; prueba 401, 403 y 200 con cada rol.
4. Reinicia: no debe duplicarse el administrador.

## Para saber más

[Guía 04: perfiles](../guias/04-perfiles-y-configuracion-base.md) · [Guía 06](../guias/06-ejecutar-local-y-prod.md) · [Guía 16: hoja de ruta](../guias/16-hoja-de-ruta.md)
