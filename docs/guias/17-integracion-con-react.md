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
| Formato de fechas | `AAAA-MM-DD` (fecha) y hora de Chile |
| Dinero | Pesos enteros; el formato (`$12.990`) lo pone el frontend |

CORS solo se aplica a `/api/**`.

## Flujo de sesión

```
1. POST /api/auth/login  {correo, clave}  ->  {token, tipo, correo, rol}
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
  const r = await fetch(`${API}/api/auth/login`, {
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
| `VENDEDOR` | Panel reducido (productos, stock, ventas, usuarios en lectura) |
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

`POST /api/usuarios` no necesita token. Valida en el formulario las mismas reglas que la API (RUN con dígito verificador, clave de 8 a 72 caracteres, teléfono de 8 a 14 dígitos) para dar respuesta inmediata, pero **la API vuelve a validar**: nunca confíes solo en el frontend. Para el selector de comuna harán falta endpoints de regiones y comunas (aún no existen).

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

## Para saber más

[Referencia 23: catálogo de endpoints](../referencia/23-catalogo-de-endpoints.md) · [Guía 11: seguridad y login](11-seguridad-y-login.md) · [Guía 10: errores](10-errores-de-la-api.md)
