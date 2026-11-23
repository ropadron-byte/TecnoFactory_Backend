# Guía 17. Usar la API desde React

> **En una frase:** el frontend inicia sesión, guarda el token, lo manda en cada petición y reacciona a los códigos 401 y 403; **la seguridad real siempre la decide la API**.

## La idea

React es la **ventanilla de atención**: muestra formularios y botones. La API es la **bóveda**: decide quién entra. Que la ventanilla oculte un botón es cortesía; la bóveda es la que cierra la puerta.

## Lo que el frontend necesita saber

| Dato | Valor |
| --- | --- |
| URL base (local) | `http://localhost:8080` |
| Origen permitido por CORS | `http://localhost:5173` (Vite). Otro origen → variable `CORS_ORIGINS` |
| Cabeceras permitidas | `Authorization` y `Content-Type` |
| Métodos permitidos | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Vida del token | 1 hora |
| Login con Google | `VITE_GOOGLE_CLIENT_ID` (el mismo `GOOGLE_CLIENT_ID` del backend) |
| Formato de fechas | `AAAA-MM-DD` (fecha) y hora de Chile |
| Dinero | Pesos enteros; el formato (`$12.990`) lo pone el frontend |

CORS solo se aplica a `/api/v1/**`.

> **En producción (Vercel) no se llama a la API por su dirección.** El frontend usa rutas relativas (`/api/v1/...`) y `vercel.json` las reenvía al servidor, así que el navegador nunca ve `localhost` ni la IP. Aun así, `CORS_ORIGINS` debe incluir el dominio de Vercel. Ver [guía 20](20-despliegue-frontend-vercel.md).

## Estado real del cliente React

El [README](../../README.md) ya documenta la app React. Hoy conviven dos estilos de llamada en el cliente:

| Parte del frontend | Cómo se conecta |
| --- | --- |
| Catálogo (categorías y productos) | Usa el prefijo `/api/v1`: `GET /api/v1/categorias` y `GET /api/v1/productos`; sincroniza las respuestas a `localStorage` como caché |
| Blog e imágenes | Vienen de la API (`/api/v1/blogs/**`, `/api/v1/imagenes/**` y `/uploads/**`) |
| Carrito y órdenes | Siguen en `localStorage` de la tienda; todavía no consumen `/api/v1/carrito/**` ni `/api/v1/boletas/**` |

> **Estado real del cliente React (corregido el 20 nov):** las dos formas de llamar de
> `src/services/api.js` siguen vivas, pero **ambas usan `/api/v1`**. El estilo directo
> (`peticion(ruta, opciones)` → `fetch(API_URL + ruta)`) recibe rutas completas ya versionadas,
> y el estilo `{ get, post, put, patch, eliminar }` agrega el prefijo `API_V1`. Antes las
> llamadas de auth, usuarios, blogs e imágenes iban sin versión y caían en
> `anyRequest().denyAll()` (**401** sin sesión, **403** con ella): el login respondía
> «Se requiere un token válido» sin llegar a comprobar credenciales. Ya están corregidas en
> los servicios y en los tests. No hay proxy de Vite ni capa de compatibilidad en el backend,
> así que cualquier ruta nueva debe llevar `/api/v1` desde el principio.

## Flujo de sesión

```
1. POST /api/v1/auth/login  {correo, clave}  ->  {token, tipo, correo, rol}
2. Guardar el token (y el rol, para mostrar u ocultar el panel)
3. En cada llamada:  Authorization: Bearer <token>
4. Si la API responde 401 -> el token venció o no sirve: volver al login
5. Si responde 403 -> la persona no tiene permiso: mostrar un aviso, no cerrar la sesión
6. Cerrar sesión = borrar el token (el servidor no guarda sesiones)
```

## Ejemplo mínimo (JavaScript)

```js
const API = "http://localhost:8080";

export async function login(correo, clave) {
  const r = await fetch(`${API}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ correo, clave }),
  });
  if (!r.ok) throw await r.json();          // trae el ErrorResponse
  return r.json();                          // { token, tipo, correo, rol }
}

export async function api(ruta, opciones = {}) {
  const token = sessionStorage.getItem("token");
  const r = await fetch(`${API}${ruta}`, {
    ...opciones,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  });
  if (r.status === 401) { sessionStorage.removeItem("token"); /* ir al login */ }
  if (!r.ok) throw await r.json();
  return r.status === 204 ? null : r.json();
}
```

Una sola función `api()` para todas las llamadas evita repetir la cabecera y centraliza el manejo del 401.

## Login con Google

El backend **no redirige** a Google: React carga Google Identity Services, muestra el botón,
recibe un **ID token** (`credential`) y lo envía a `POST /api/v1/auth/google`.

**1. Cargar el script** en `index.html` (o con un `<script>` dinámico):

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

**2. Configurar el Client ID** en `.env` del frontend (Vite):

```env
VITE_GOOGLE_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

Debe ser **el mismo** Client ID que `GOOGLE_CLIENT_ID` en el backend y tener
`http://localhost:5173` como origen autorizado en Google Cloud.

**3. Componente del botón y los tres finales posibles:**

```jsx
import { useEffect, useRef, useState } from "react";

const API = "http://localhost:8080";

export function BotonGoogle({ onSesion, onCompletarPerfil }) {
  const contenedor = useRef(null);

  useEffect(() => {
    const iniciar = () => {
      window.google.accounts.id.initialize({
        client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
        callback: async ({ credential }) => {
          const r = await fetch(`${API}/api/v1/auth/google`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ credential }),
          });

          if (r.status === 200) {
            const sesion = await r.json();        // { token, tipo, correo, rol }
            sessionStorage.setItem("token", sesion.token);
            onSesion(sesion);
          } else if (r.status === 202) {
            const pendiente = await r.json();     // { registroToken, correo, nombre, apellidos }
            onCompletarPerfil(pendiente);         // muestra el formulario
          } else if (r.status === 409) {
            const error = await r.json();
            alert(error.mensaje);                 // hay que entrar con contraseña y vincular
          } else {
            alert("No se pudo iniciar sesión con Google");
          }
        },
      });

      window.google.accounts.id.renderButton(contenedor.current, {
        theme: "outline",
        size: "large",
        text: "continue_with",
      });
    };

    if (window.google?.accounts?.id) iniciar();
    else window.addEventListener("load", iniciar, { once: true });
  }, [onSesion, onCompletarPerfil]);

  return <div ref={contenedor} />;
}
```

**4. Formulario para completar el perfil** (solo cuando la API respondió **202**). El correo,
el nombre y los apellidos vienen de Google y no se editan; el `registroToken` viaja tal cual:

```jsx
async function completarPerfil(pendiente, datos) {
  const r = await fetch(`${API}/api/v1/auth/google/completar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ...datos, registroToken: pendiente.registroToken }),
  });
  if (r.status === 201) {
    const sesion = await r.json();
    sessionStorage.setItem("token", sesion.token);
    return sesion;
  }
  throw await r.json();   // 400 detalles por campo, 401 token vencido, 409 RUN/correo repetido
}
```

`datos` lleva `run`, `runDv`, `fechaNacimiento`, `direccion`, `telefono` e `idComuna`.

**5. Vincular Google a una cuenta que ya tiene contraseña.** Después de un login normal
(`POST /api/v1/auth/login`) y con el token guardado, se envía el `credential` de Google:

```js
await api("/api/v1/auth/google/vincular", {
  method: "POST",
  body: JSON.stringify({ credential }),
}); // 204: listo. El correo de Google debe coincidir con el de la cuenta.
```

| Respuesta de `/api/v1/auth/google` | Qué hace React |
| --- | --- |
| **200** | Guarda el `token` y entra |
| **202** | Guarda el `registroToken` en memoria y muestra el formulario de perfil |
| **409** | Avisa que debe iniciar sesión con contraseña y luego vincular |
| **401** | El token de Google no sirve: mostrar error y no crear nada |

**El `registroToken` no es un token de sesión**: no lo mandes como `Bearer` y no lo guardes
en `localStorage`; solo sirve para el paso de completar el perfil y dura 10 minutos.

## Cómo mostrar los errores

La API siempre devuelve el mismo JSON de error:

```json
{ "status": 400, "mensaje": "Datos inválidos", "detalles": { "correo": "El correo no tiene un formato válido" } }
```

| Código | Qué hace el frontend |
| --- | --- |
| 400 | Pinta cada mensaje de `detalles` junto a su campo del formulario |
| 401 | Si venía de un login: «correo o clave incorrectos». Si venía de otra llamada: volver al login |
| 403 | «No tienes permiso para esto» |
| 404 | «No se encontró» |
| 409 | Muestra `mensaje` (por ejemplo «El correo ya está registrado») |
| 5xx | «Algo salió mal, intenta más tarde» (nunca muestres detalles técnicos) |

**Los nombres de `detalles` son los nombres de los campos del JSON** (`runDv`, `fechaNacimiento`...), lo que permite asociarlos directamente a cada input.

## Panel según el rol

El login devuelve `rol`. Con él, React decide qué mostrar:

| Rol | Muestra |
| --- | --- |
| `ADMINISTRADOR` | Panel completo |
| `VENDEDOR` | Panel reducido: blogs e imágenes, listado de boletas y lectura de `/api/v1/productos/admin` y `/api/v1/productos/criticos`; **no** escribe productos ni categorías (eso es solo de `ADMINISTRADOR`) |
| `CLIENTE` | Sin panel y sin botón |

> **Recuerda:** esto es solo **comodidad visual**. Cualquiera puede abrir las herramientas del navegador y mostrar un botón oculto; lo que lo detiene es la respuesta 403 de la API.

## Dónde guardar el token (decisión abierta)

| Opción | Ventaja | Riesgo |
| --- | --- | --- |
| `sessionStorage` | Se borra al cerrar la pestaña; simple | Un script malicioso (XSS) podría leerlo |
| `localStorage` | La sesión sobrevive al cerrar el navegador | Igual de expuesto a XSS y dura más |
| Cookie `HttpOnly` | JavaScript no puede leerla | Exige cambios en la API (cookies, CORS con credenciales, CSRF) |

Para empezar, `sessionStorage` con vida de token corta (1 hora) es un compromiso razonable. **Nunca** guardes la clave.

## Registro de clientes

`POST /api/v1/usuarios` no necesita token. Valida en el formulario las mismas reglas que la API (RUN con dígito verificador, clave de 8 a 72 caracteres, teléfono de 8 a 14 dígitos) para dar respuesta inmediata, pero **la API vuelve a validar**: nunca confíes solo en el frontend. Para el selector de comuna harán falta endpoints de regiones y comunas (aún no existen).

## Reglas

1. **Nunca confíes en el frontend** para proteger algo.
2. **Un solo punto de llamadas** (`api()`), no `fetch` suelto por todos lados.
3. **401 = volver al login; 403 = avisar.** No son lo mismo.
4. **No muestres mensajes técnicos** de un 500.
5. **En producción**, define `CORS_ORIGINS` con el dominio real y usa HTTPS.

## Si algo falla

| Síntoma | Causa | Solución |
| --- | --- | --- |
| Error de CORS en la consola | Tu origen no está permitido | `CORS_ORIGINS=http://localhost:3000` (u otro) y reinicia |
| El login funciona en Swagger pero no desde React | Falta `Content-Type: application/json` | Agrégalo |
| 401 en todo tras iniciar sesión | La cabecera no se envía o dice `Bearer Bearer` | Revisa `Authorization: Bearer <token>` |
| El preflight (`OPTIONS`) falla | Cabecera no permitida | Solo `Authorization` y `Content-Type` están permitidas |
| En Vercel la consola muestra `localhost:8080` o *Mixed Content* | El frontend usa una URL absoluta y se salta `vercel.json` | Rutas relativas y *Redeploy*; ver [guía 20](20-despliegue-frontend-vercel.md) |
| En Vercel el `GET` funciona pero el `POST` da 403 | Falta el dominio de Vercel en `CORS_ORIGINS` | Defínelo en el `.env` del servidor y recrea el contenedor |
| El login da 401 con «Se requiere un token válido» | La ruta quedó sin `/api/v1` (antes pasaba en auth/usuarios/blogs/imágenes) | Versiona la ruta en el servicio; ver [Estado real del cliente React](#estado-real-del-cliente-react) |

## Para saber más

[Guía 20: publicar en Vercel](20-despliegue-frontend-vercel.md) · [Referencia 23: catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md) · [Guía 11: seguridad y login](11-seguridad-y-login.md) · [Guía 10: errores](10-errores-de-la-api.md) · [Referencia 29: login con Google](../referencia/29-login-con-google-explicado.md)
