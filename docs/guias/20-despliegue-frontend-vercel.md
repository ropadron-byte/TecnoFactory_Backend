# Guía 20. Publicar el frontend en Vercel y conectarlo a la API del servidor

> **En una frase:** el frontend vive en Vercel (HTTPS), la API vive en el servidor `eva03`; para que el navegador no los bloquee, Vercel actúa de **intermediario** con una regla `rewrites` en `vercel.json`, y el navegador solo habla con Vercel.

## El problema

La web desplegada en `https://tecno-factory-react-version.vercel.app` pedía los datos a `http://localhost:8080`. Eso falla por dos motivos distintos:

| Problema | Por qué ocurre |
| --- | --- |
| `localhost` | El código de React corre **en el navegador de quien visita la página**, así que `localhost` es el computador de esa persona, no tu servidor |
| *Mixed content* | Una página `https://` no puede llamar a una dirección `http://` (la IP y el puerto sin certificado): el navegador la bloquea |

## La idea: el recepcionista

Sin `vercel.json`, el navegador llama directo a la API (otra «casa»: necesita HTTPS y permiso CORS). Con `rewrites`, el navegador solo le habla a Vercel, que **reenvía** la petición al servidor y devuelve la respuesta. Para el navegador todo es el mismo sitio.

```
Navegador ──https──▶ Vercel (tecno-factory-react-version.vercel.app)
                        │  /api/**     → reenvía al backend
                        │  /uploads/** → reenvía al backend
                        │  lo demás    → index.html (React Router)
                        ▼
                     Servidor eva03 ──▶ (Nginx, opcional) ──▶ contenedor tecnofactory-backend (8081→8080)
```

## Lo que se hizo (23 nov 2026)

| # | Dónde | Qué | Para qué |
| --- | --- | --- | --- |
| 1 | Frontend | `vercel.json` con tres reglas (`/api`, `/uploads` y el resto a `index.html`) | Que el navegador nunca llame a `localhost` ni a una IP |
| 2 | Servidor | Backend en Docker publicado en el puerto **8081** del host (`8081→8080`) | El 8080 del host ya lo usa otro proceso Java (ver «Trampas») |
| 3 | Servidor | Nginx como proxy inverso hacia `127.0.0.1:8081` (`/etc/nginx/conf.d/tecnofactory.conf`) | Dar HTTPS al backend |
| 4 | Servidor | `setsebool -P httpd_can_network_connect 1` | SELinux impide que Nginx se conecte a otro puerto si no se activa |
| 5 | Servidor | Certificado de Let's Encrypt para `186-64-113-33.sslip.io` con Certbot | Un dominio válido para la IP del servidor (ver «Por qué sslip.io») |

### `vercel.json` (en el repositorio del **frontend**)

```json
{
  "rewrites": [
    { "source": "/api/:path*",     "destination": "http://186.64.113.33:8081/api/:path*" },
    { "source": "/uploads/:path*", "destination": "http://186.64.113.33:8081/uploads/:path*" },
    { "source": "/(.*)",           "destination": "/index.html" }
  ]
}
```

- El **orden importa**: Vercel usa la primera regla que coincide; la última (`/(.*)`) debe ir al final.
- `/uploads/**` está porque las imágenes de productos las sirve el backend (`/uploads/productos/<uuid>.jpg`).
- Este archivo **no está en este repositorio**: vive en el del frontend (`TecnoFactory_ReactVersion`).

### Cómo debe llamar React

Con `rewrites`, el frontend usa **rutas relativas** (`/api/v1/categorias`), sin dominio. Por eso no hay `localhost` que buscar:

```js
const API_URL = import.meta.env.VITE_API_URL ?? "";   // vacío = mismo origen
fetch(`${API_URL}/api/v1/categorias`);
```

En local, el backend sigue en `http://localhost:8080`; en Vercel la variable queda **sin definir** (o vacía) y las llamadas pasan por `vercel.json`. Si alguna vez se define `VITE_API_URL` en Vercel con una dirección absoluta, `vercel.json` deja de usarse y vuelven el *mixed content* y el CORS.

## Qué pasa con CORS

Las peticiones pasan por Vercel, así que **parecen** del mismo sitio. Pero Vercel reenvía la cabecera `Origin` y el navegador la envía en los `POST`, `PUT`, `PATCH` y `DELETE`. Para Spring, un `Origin` distinto del `Host` es una petición CORS y la compara con `CORS_ORIGINS` (ver [guía 04](04-perfiles-y-configuracion-base.md)). Si el dominio no está, responde **403** «Invalid CORS request», aunque el `GET` funcione.

En el `.env` del servidor (`/opt/tecnofactory/.env`):

```env
CORS_ORIGINS=https://tecno-factory-react-version.vercel.app
```

Sin `/` final y sin `*`. Después hay que **recrear** el contenedor para que lea el valor: `docker compose -f docker-compose.prod.yml up -d backend`.

> Las *preview deployments* de Vercel usan otros dominios (`...-git-rama-usuario.vercel.app`). Si quieres probar escrituras desde una de ellas, agrégala a `CORS_ORIGINS` (separadas por coma) o prueba en el dominio principal.

## ⚠ Regla de oro: reconstruir la imagen al desplegar

Cada vez que cambie el código **o un SQL de `src/main/resources/db/migration`**, levanta producción así:

```bash
docker compose -f docker-compose.prod.yml up -d --build --wait --wait-timeout 900
```

Las opciones importan: `-f` elige el archivo de producción, `--build` mete el JAR nuevo en la imagen, `--wait` espera a que el backend esté sano y `--wait-timeout` da margen en una VPS lenta.

¿Por qué `--build` es obligatorio? Porque el JAR se hornea en la imagen: se compila en la máquina de desarrollo, se copia como `app.jar` y `Dockerfile.runtime` lo mete en la imagen. En producción el backend solo monta `uploads/`; el servicio `db` monta `docker/mysql-init`, **nunca `db/migration`**. Un SQL nuevo escrito en el host no existe dentro del contenedor si la imagen no se reconstruye con el JAR nuevo.

En eva03 (1 CPU y 962 MiB) **ya no se compila en el servidor**: `Dockerfile.runtime` solo copia el JAR preconstruido, así que el build baja de ~208 s a segundos. El healthcheck usa `start-period=900s` para no declarar *unhealthy* mientras arranca. Cada cambio de código exige recompilar el JAR en desarrollo y volver a copiarlo.

## Por qué sslip.io

`eva03.drr.lat` es solo el nombre interno del servidor: **no existe en el DNS público** (Certbot respondió `NXDOMAIN`), así que Let's Encrypt no puede emitir un certificado para él. `sslip.io` devuelve la IP que lleva el nombre: `186-64-113-33.sslip.io` resuelve a `186.64.113.33` sin registrar nada, y Let's Encrypt sí puede validarlo.

- El certificado vence el **21 feb 2027**; Certbot dejó una tarea programada de renovación. Comprobar: `certbot renew --dry-run`.
- Depende de un servicio de terceros. Si algún día falla, se reemplaza por un dominio propio.

## ⚠ Pendiente importante: el tramo Vercel → servidor sigue en HTTP

Hoy `vercel.json` apunta a `http://186.64.113.33:8081`. El navegador ya habla HTTPS con Vercel, pero **de Vercel al servidor viajan sin cifrar** las claves del login y los tokens JWT. El HTTPS de Nginx ya está listo; falta usarlo:

```json
{ "source": "/api/:path*",     "destination": "https://186-64-113-33.sslip.io/api/:path*" },
{ "source": "/uploads/:path*", "destination": "https://186-64-113-33.sslip.io/uploads/:path*" },
```

Después de comprobar que funciona, **cierra el 8081 al exterior** para que solo Nginx llegue al backend. En `docker-compose.prod.yml` del servidor:

```yaml
    ports:
      - "127.0.0.1:8081:8080"
```

`firewalld` está apagado en el servidor, así que hoy nada filtra los puertos publicados por Docker.

## Cómo comprobarlo

```bash
# 1. Desde tu PC, directo al servidor (HTTPS de Nginx)
curl -i https://186-64-113-33.sslip.io/api/v1/categorias        # 200 con JSON

# 2. A través de Vercel (lo que ve el navegador): debe dar lo mismo
curl -i https://tecno-factory-react-version.vercel.app/api/v1/categorias

# 3. Una escritura, para probar CORS (debe dar 401 o 400, NO 403 «Invalid CORS request»)
curl -i -X POST https://tecno-factory-react-version.vercel.app/api/v1/auth/login \
  -H "Origin: https://tecno-factory-react-version.vercel.app" \
  -H "Content-Type: application/json" -d '{"correo":"x@x.cl","clave":"mala"}'
```

En el navegador: F12 → **Network**; las peticiones deben ir a `tecno-factory-react-version.vercel.app/api/v1/...` (no a `localhost` ni a la IP) y responder 200.

## Si algo falla

| Síntoma | Causa | Solución |
| --- | --- | --- |
| La consola dice `localhost:8080` | Vercel sirve una compilación vieja o hay una URL absoluta en el código | Haz *Redeploy* sin caché y busca `localhost` en `src/` y en las variables de Vercel |
| `Mixed Content` | El frontend llama a `http://...` directo | Usa rutas relativas (`/api/v1/...`) |
| 404 al abrir `/api/v1/...` en Vercel | `vercel.json` no está en la **raíz** del repositorio o no se subió | `git add vercel.json && git push` |
| 403 solo en `POST`/`PUT`/`DELETE`; el `GET` funciona | Falta el dominio de Vercel en `CORS_ORIGINS` | Defínelo y recrea el contenedor |
| 502 / 504 desde Vercel | El backend está caído o el puerto no es accesible | `docker ps` en el servidor; `curl` al 8081 desde fuera |
| Las imágenes de productos no cargan | Falta la regla `/uploads/:path*` | Agrégala antes de la regla de `index.html` |
| Certbot: `NXDOMAIN` | Usaste `eva03.drr.lat` | Usa `186-64-113-33.sslip.io` |
| Falla la subida de una imagen grande | Límite de tamaño de cuerpo en el reenvío de Vercel (**no comprobado**) | Prueba con una imagen de casi 5 MB; el backend acepta hasta 5 MB |

## Trampas del servidor compartido

1. **No mates procesos ajenos.** En `eva03` conviven los proyectos de varias personas. El Java de `127.0.0.1:8080` y los contenedores `boostedup-*` y `splendor-*` **no son de TecnoFactory**; no uses `kill` sobre ellos ni edites otros archivos de `/etc/nginx/conf.d/`.
2. **`ss -tulnp | grep 8080` también muestra el 18080**; lee la columna de la dirección completa.
3. **El compose del repositorio publica `8080:8080`**, pero en el servidor corre como `8081→8080` porque el 8080 está ocupado. Si recreas el contenedor con el archivo del repositorio, puede fallar o cambiar de puerto: deja el repositorio igual al servidor.
4. **SSH con contraseña de `root` expuesto a internet**: el servidor registró decenas de intentos fallidos en minutos. Conviene usar llave SSH y desactivar `PasswordAuthentication`, o instalar `fail2ban`.

## Reglas

1. **El frontend nunca lleva una IP ni `localhost` en el código**: usa rutas relativas y deja que `vercel.json` decida el destino.
2. **Todo tramo que lleve claves o tokens va por HTTPS**, incluido el de Vercel al servidor.
3. **Un dominio de frontend nuevo exige actualizar `CORS_ORIGINS`.**
4. **No expongas puertos de aplicaciones al exterior** si ya hay un proxy delante.

## Para saber más

[Guía 17: usar la API desde React](17-integracion-con-react.md) · [Guía 04: perfiles y CORS](04-perfiles-y-configuracion-base.md) · [Referencia 07: ejecución local y prod](../referencia/07-ejecucion-local-y-prod.md) · [Referencia 24: decisión D37](../referencia/24-decisiones-de-diseno.md) · [Manual 08: antes de desplegar](../personal/08-antes-de-desplegar.md)
