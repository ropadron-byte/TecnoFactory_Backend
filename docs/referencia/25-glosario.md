# 25. Glosario

Las palabras que aparecen en el proyecto, explicadas sin jerga. Orden alfabético.

| Término | Qué significa |
| --- | --- |
| **API REST** | Un servidor que responde a peticiones HTTP (`GET`, `POST`...) con datos en JSON. TecnoFactory es una. |
| **Asignación masiva** | Ataque en que el cliente manda campos que no debería poder fijar (`rol`, `activo`). Se evita con DTOs que no los tienen. |
| **Autenticación** | Comprobar **quién eres** (login). |
| **Autorización** | Decidir **qué puedes hacer** (roles). |
| **Baja lógica** | «Borrar» marcando `activo = false` en lugar de eliminar la fila. |
| **Bean** | Un objeto que Spring crea y reparte por ti (service, repository, configuración). |
| **Bean Validation** | Reglas declaradas con anotaciones (`@NotBlank`, `@Size`, `@Email`). |
| **BCrypt** | Algoritmo que convierte una clave en un hash irreversible y con sal; sirve para guardar claves sin guardarlas. |
| **Bearer** | «Portador»: quien trae el token tiene el acceso. Se envía como `Authorization: Bearer <token>`. |
| **Checksum** | Huella de un archivo. Flyway guarda la de cada migración para detectar si la cambiaron. |
| **Claims** | Los datos dentro de un JWT (`sub`, `rol`, `exp`). |
| **Controller** | Clase que recibe la petición HTTP y la entrega al service. Sin lógica. |
| **CORS** | Regla del navegador: una página en un origen solo puede llamar a otro origen si ese servidor lo permite. |
| **CSRF** | Ataque que aprovecha las cookies de sesión. No aplica con JWT en cabecera, por eso está desactivado. |
| **DDL / DML** | DDL cambia la **estructura** (`CREATE TABLE`); DML cambia los **datos** (`INSERT`, `UPDATE`). |
| **`ddl-auto: validate`** | Hibernate solo comprueba que las entidades calcen con las tablas. |
| **DTO** | Objeto que solo transporta datos entre el cliente y el service (`Request`, `Response`). |
| **Entidad** | Clase Java que representa una fila de una tabla (`@Entity`). |
| **Filtro** | Código que ve cada petición antes de que llegue al controller (`JwtAuthFilter`). |
| **Flyway** | Herramienta que aplica las migraciones SQL en orden y recuerda cuáles ya corrió. |
| **FK (clave foránea)** | Columna que apunta a la clave de otra tabla y exige que exista. |
| **Hash** | Resultado de aplicar una función irreversible a un texto. |
| **Hibernate** | La librería que traduce entidades Java a SQL (implementa JPA). |
| **HS256** | Algoritmo con que se firma el JWT usando `JWT_SECRET`. |
| **JAR** | Archivo que empaqueta la aplicación para ejecutarla con `java -jar`. |
| **JPA** | Estándar de Java para mapear objetos a tablas. |
| **JSON** | Formato de texto para datos (`{"clave": "valor"}`). |
| **JWT** | *JSON Web Token*: texto firmado con tres partes que dice quién eres y cuándo expira. No está cifrado, solo firmado. |
| **`LAZY`** | La relación se carga **solo cuando se usa**, no junto con la entidad. |
| **Lombok** | Genera código repetitivo (`@Getter`, `@Slf4j`...). |
| **MapStruct** | Genera el código que convierte entidades en DTOs y viceversa. |
| **Mock** | Objeto falso que reemplaza a uno real en una prueba. |
| **Módulo 11** | Cálculo del dígito verificador del RUN chileno. |
| **Migración** | Archivo SQL versionado (`V1__...sql`) que cambia la base. |
| **Perfil** | Conjunto de configuración (`local`, `prod`) que se activa por nombre. |
| **PK (clave primaria)** | Columna que identifica de forma única cada fila. |
| **Repository** | Interfaz que consulta la base (`UsuarioRepository`). Spring la implementa. |
| **Request / Response** | El DTO que **entra** y el que **sale** de un endpoint. |
| **`RESTRICT` / `CASCADE`** | Qué hacer al borrar: impedirlo, o borrar también lo dependiente. |
| **Rol** | Nivel de permisos: `ADMINISTRADOR`, `VENDEDOR`, `CLIENTE`. |
| **RUN** | Rol Único Nacional chileno (el número) y su dígito verificador (`runDv`). |
| **Service** | Clase con la lógica de negocio y la transacción. |
| **Stateless** | El servidor no recuerda sesiones; cada petición trae su token. |
| **Swagger / OpenAPI** | Página que describe y permite probar la API. |
| **Token** | Texto que prueba tu identidad sin enviar la clave cada vez. |
| **Transacción** | Un grupo de operaciones que se guardan **todas o ninguna**. |
| **401 / 403** | «No sé quién eres» / «Sé quién eres, pero no puedes». |
| **UserDetails** | Representación que Spring Security usa de un usuario (correo, hash, roles). |
| **`@Valid`** | Pide a Spring que ejecute las validaciones del DTO. |
| **Variable de entorno** | Valor que el sistema operativo le pasa al programa (`DB_PASSWORD`). |
| **Volumen (Docker)** | Carpeta persistente donde MySQL guarda los datos aunque el contenedor se borre. |
| **YAML** | Formato de los archivos `application*.yaml`; la sangría importa. |

## Para saber más

[Referencia 01: arquitectura y stack](01-arquitectura-y-stack.md) · [Guía 01: el proyecto en una página](../guias/01-el-proyecto-en-una-pagina.md)
