# 08. Seguridad

## Lo que ya está resuelto

| Medida | Dónde | Estado |
| --- | --- | --- |
| La app usa un usuario de MySQL sin DDL en producción | `tecnofactory_app` | **Probado** |
| Flyway usa un usuario separado | `tecnofactory_migrator` | **Probado** |
| Sin valores por defecto en prod | `application-prod.yaml` | Hecho |
| Swagger y `api-docs` apagados en prod | `application-prod.yaml` | Hecho |
| Errores sin mensaje, rastro ni detalle de validación en prod | `application-prod.yaml` | Hecho |
| Actuator publica solo `health`, sin detalles | `application-prod.yaml` | Hecho |
| Secretos fuera del código, en `.env` | `.env` | Hecho (verificar `.gitignore`) |

## Lo que falta

| Pendiente | Riesgo mientras no exista |
| --- | --- |
| **`SecurityConfig`** (hoy vacía) | Spring protege todo con un usuario temporal `user`: la API es inutilizable y no hay roles |
| **JWT** (`JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl`) | No hay login real |
| **BCrypt** para contraseñas | Nunca guardar texto plano en `USUARIO.contrasenia` |
| **CORS** (`CorsConfig`) | El navegador bloquea las peticiones de React |
| **`GlobalExceptionHandler`** | En prod los errores salen sin formato útil para el frontend |
| **HTTPS** | Las claves y tokens viajarían en texto plano |
| **Límite de intentos de login** | Fuerza bruta contra cuentas |
| **Validación de subida de archivos** | Subida de archivos maliciosos |

## Diseño previsto del control de acceso

| Ruta | Acceso |
| --- | --- |
| `POST /api/auth/**` (registro, login) | Público |
| `GET /api/productos/**`, `GET /api/categorias/**` | Público |
| `POST/PUT/DELETE` de catálogo | Administrador |
| Carrito y boletas propias | Usuario autenticado |
| Gestión de usuarios y roles | Administrador |
| `/actuator/health` | Público (solo `UP`/`DOWN`) |
| Swagger | Solo en `local` |

Los tokens JWT se firman con `JWT_SECRET` (mínimo 32 caracteres) y duran 1 hora (`jwt.expiration-ms`).

## Qué NO hacer

- No subir el `.env` a Git ni compartir sus valores por chat.
- No usar `root` en la app en producción.
- No dejar `ports: "3306:3306"` en un servidor.
- No guardar números de tarjeta en el pago simulado.
- No devolver entidades JPA por la API (filtrarían el hash de la contraseña).
- No poner secretos en los yaml: van dentro del JAR y a Git.

## Lista de verificación antes de un despliegue real

- [ ] `.env` de producción con claves **nuevas** (`openssl rand -hex 32`), `chmod 600`.
- [ ] `git ls-files | grep -i env` no devuelve nada y `.gitignore` contiene `.env`.
- [ ] `SecurityConfig` escrita: rutas públicas y protegidas probadas.
- [ ] Contraseñas hasheadas con BCrypt (incluido el administrador inicial).
- [ ] CORS limitado al dominio real del frontend.
- [ ] Puerto 3306 sin publicar.
- [ ] HTTPS con Nginx o Caddy.
- [ ] Swagger apagado y errores sin detalle (probado como en el Escenario 2).
- [ ] Respaldos automáticos guardados fuera del servidor y **restauración probada**.
- [ ] Firewall: solo 80/443 (y SSH restringido).
- [ ] Imágenes subidas en un volumen.
