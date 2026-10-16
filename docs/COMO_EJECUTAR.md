# Cómo ejecutar TecnoFactory

Guía para arrancar el backend en **local** (desarrollo) y en **producción**.

---

## Tabla de contenidos

- [Resumen: local frente a producción](#resumen-local-frente-a-producción)
- [Requisitos](#requisitos)
- [Archivo .env](#archivo-env)
- [Ejecutar en local](#ejecutar-en-local)
- [Ejecutar en producción](#ejecutar-en-producción)
- [Errores comunes](#errores-comunes)
- [Comandos de Docker de referencia](#comandos-de-docker-de-referencia)

---

## Resumen: local frente a producción

El perfil se elige al arrancar con la variable `SPRING_PROFILES_ACTIVE`. Spring carga siempre `application.yaml` y encima aplica el archivo del perfil elegido.

| | Local | Producción |
| --- | --- | --- |
| Perfil | `local` | `prod` |
| Archivo de configuración | `application-local.yaml` | `application-prod.yaml` |
| Tablas | Las crea Hibernate (`ddl-auto: update`) | Las crea Flyway; Hibernate solo valida (`validate`) |
| Flyway | Apagado | Encendido |
| Swagger | Visible | Apagado |
| Errores de la API | Con mensaje y rastro opcional (`?trace=true`) | Sin detalles |
| Logs | `INFO` y tu código en `DEBUG`; muestra el SQL | `WARN` y tu código en `INFO`; sin SQL |
| `/actuator/health` | Con detalles | Solo `UP` o `DOWN` |
| Caché Redis | Apagado | Apagado (por ahora) |
| Claves | Sin valor por defecto | Sin valor por defecto |

---

## Requisitos

- **JDK 21** completo, no solo el JRE. Comprueba con `javac -version`.
- **Docker** y **Docker Compose**. Comprueba con `docker compose version`.
- Ejecuta todos los comandos **desde la carpeta raíz del proyecto**, la que contiene `mvnw` y `docker-compose.yml`:

```bash
cd ~/Escritorio/Proyectos/TecnoFactory
```

---

## Archivo .env

Crea un archivo `.env` junto a `docker-compose.yml`. Docker Compose lo lee solo.

```env
DB_PASSWORD=la_clave_que_elijas
JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres
```

| Variable | Qué es | Notas |
| --- | --- | --- |
| `DB_PASSWORD` | Clave del usuario `root` de MySQL | MySQL la guarda **la primera vez que se crea el volumen**; cambiar el `.env` después no la modifica |
| `JWT_SECRET` | Clave con la que se firman los tokens de login | Mínimo 32 caracteres. Genera una con `openssl rand -base64 48` |

Reglas:

- El `.env` **no se sube a Git**. Comprueba que `.gitignore` lo incluya.
- Evita el símbolo `$` en las contraseñas, porque Compose lo interpreta como una variable.
- Spring **no lee** el `.env`: las variables hay que dárselas a la aplicación al arrancarla (ver abajo).

---

## Ejecutar en local

### 1. Levantar MySQL

```bash
docker compose up -d
docker compose ps
```

El servicio `tecnofactory-db` debe aparecer como `healthy`. Si dice `starting`, espera unos segundos y repite. Si ya estaba corriendo, no pasa nada por ejecutar `up` otra vez.

### 2. Variables de entorno que necesita el perfil local

| Variable | Valor en local |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` |
| `DB_URL` | `jdbc:mysql://localhost:3306/tecnofactory` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | La misma que pusiste en el `.env` |
| `JWT_SECRET` | Cualquier texto de 32 caracteres o más |

`tecnofactory` en la URL debe coincidir con `MYSQL_DATABASE` del compose, y `3306` con el puerto publicado.

### 3. Arrancar la aplicación

**Opción A: terminal, todo en un comando**

```bash
SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
DB_PASSWORD=la_misma_del_env \
JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres \
./mvnw spring-boot:run
```

Cada `\` al final de línea significa que el comando continúa abajo; no debe haber espacios después de él.

**Opción B: terminal, cargando `DB_PASSWORD` y `JWT_SECRET` desde el `.env`**

```bash
set -a
source .env
set +a

SPRING_PROFILES_ACTIVE=local \
DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
DB_USER=root \
./mvnw spring-boot:run
```

`set -a` hace que todo lo que se declare a continuación se exporte; `source .env` lee el archivo; `set +a` vuelve al comportamiento normal. Cada línea del `.env` debe ser `NOMBRE=valor`, sin espacios alrededor del `=`.

**Opción C: IntelliJ**

*Run, Edit Configurations*, abre la configuración de la aplicación y en **Environment variables** escribe (separadas por `;`):

```
SPRING_PROFILES_ACTIVE=local;DB_URL=jdbc:mysql://localhost:3306/tecnofactory;DB_USER=root;DB_PASSWORD=la_misma_del_env;JWT_SECRET=una_clave_larga_de_al_menos_32_caracteres
```

### 4. Comprobar que funciona

En el log de arranque deben aparecer estas líneas:

```
The following 1 profile is active: "local"
HikariPool-1 - Start completed.
Tomcat started on port 8080 (http)
Started TecnoFactoryApplication in ... seconds
```

Mientras `SecurityConfig` esté vacío, Spring Security protege **todas** las rutas con un usuario temporal. En el log aparece una línea `Using generated security password: ...`. Para abrir Swagger o Actuator:

- Usuario: `user`
- Contraseña: la que muestra esa línea del log (cambia en cada arranque)

| Recurso | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Estado de la app | http://localhost:8080/actuator/health |

Este comportamiento desaparece cuando escribas tu `SecurityConfig`.

### 5. Detener

- La aplicación: `Ctrl + C` en la terminal, o el botón de detener en IntelliJ.
- MySQL: `docker compose stop` (conserva los datos).

---

## Ejecutar en producción

### Antes de ejecutar: qué debe estar listo

El perfil `prod` usa `ddl-auto: validate` y Flyway, así que **no crea las tablas**: las tablas deben existir por migración.

- [ ] `src/main/resources/db/migration/V1__esquema_inicial.sql` contiene el SQL de todas tus tablas.
- [ ] Las entidades coinciden con esas tablas; si no, la aplicación no arranca y el error indica qué falta.
- [ ] El `.env` tiene un `JWT_SECRET` nuevo y generado, distinto al de desarrollo.
- [ ] La `DB_PASSWORD` es segura y no es `root`.
- [ ] El `SecurityConfig` está escrito (de lo contrario, toda la API pide el usuario temporal).

Para generar el esquema a partir de tu base de desarrollo ya diseñada:

```bash
docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --no-data tecnofactory' > V1__esquema_inicial.sql
```

Revisa el archivo resultante, quita la tabla `flyway_schema_history` si aparece y colócalo en `db/migration`.

### Variables que necesita el perfil prod

El perfil `prod` **no tiene valores por defecto**: si falta una variable, la aplicación se detiene y el error dice cuál.

| Variable | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | Dirección de MySQL: `jdbc:mysql://localhost:3306/tecnofactory` si corre en la misma máquina, o `jdbc:mysql://db:3306/tecnofactory` si la app corre en Docker |
| `DB_USER` | `root` |
| `DB_PASSWORD` | Clave segura de MySQL |
| `JWT_SECRET` | Clave generada, de 32 caracteres o más |

### Opción A: ejecutar el JAR directamente

1. Levanta MySQL: `docker compose up -d` y comprueba `healthy`.
2. Construye el JAR (sin tests, que necesitan Docker y una base):

   ```bash
   ./mvnw clean package -DskipTests
   ```

3. Ejecútalo con las variables:

   ```bash
   SPRING_PROFILES_ACTIVE=prod \
   DB_URL=jdbc:mysql://localhost:3306/tecnofactory \
   DB_USER=root \
   DB_PASSWORD=la_clave_segura \
   JWT_SECRET=la_clave_generada \
   java -jar target/TecnoFactory-0.0.1-SNAPSHOT.jar
   ```

### Opción B: backend dentro de Docker

Esta opción empaqueta la aplicación en su propio contenedor, junto a MySQL.

**1. Escribe el `Dockerfile`** (en la raíz del proyecto):

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Línea | Qué hace |
| --- | --- |
| `FROM ... AS build` | Primera etapa: imagen con el JDK completo para compilar |
| `COPY .mvn/ ...`, `COPY mvnw pom.xml ./`, `COPY src/ src/` | Copia solo lo necesario; el `.env` queda fuera de la imagen |
| `RUN ./mvnw -q package -DskipTests` | Genera el JAR |
| `FROM eclipse-temurin:21-jre` | Segunda etapa: imagen liviana, solo para ejecutar |
| `COPY --from=build ...` | Trae únicamente el JAR de la primera etapa |
| `EXPOSE 8080` | Documenta el puerto de la app |
| `ENTRYPOINT [...]` | Comando que arranca la aplicación |

**2. Agrega el servicio `backend`** en `docker-compose.yml`, bajo `services:`, con la misma indentación que `db:` y antes de `volumes:`:

```yaml
  backend:
    build: .
    container_name: tecnofactory-backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/tecnofactory
      DB_USER: root
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
```

| Línea | Qué hace |
| --- | --- |
| `build: .` | Construye la imagen con el `Dockerfile` de esta carpeta |
| `depends_on` con `service_healthy` | Espera a que MySQL esté `healthy` antes de arrancar |
| `SPRING_PROFILES_ACTIVE: prod` | Activa el perfil de producción |
| `DB_URL: ...@db:3306` | Usa `db`, el nombre del servicio MySQL: dentro de Docker `localhost` sería el propio contenedor |
| `DB_PASSWORD`, `JWT_SECRET` | Compose los toma del `.env` |
| `ports: "8080:8080"` | Hace accesible la API en el puerto 8080 |

**3. Construye y levanta todo:**

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

No ejecutes a la vez la aplicación desde IntelliJ y el contenedor del backend: ambos usan el puerto 8080.

### Comprobar que arrancó en producción

- El log debe mostrar `The following 1 profile is active: "prod"`.
- Swagger **no debe cargar** (`/swagger-ui/index.html` responde error en vez de la pantalla).
- `/actuator/health` responde solo `UP` o `DOWN`, sin detalles.
- Una ruta que falle no muestra mensajes internos ni rastros del error.

### Recomendaciones para un servidor real

- Quita el `ports: "3306:3306"` del servicio `db`: el backend llega a MySQL por la red interna de Docker y la base no debe quedar expuesta a internet.
- Pon un servidor intermedio con HTTPS (Nginx o Caddy) delante del backend.
- Haz respaldos periódicos y guárdalos fuera de esa máquina:

  ```bash
  docker exec tecnofactory-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" tecnofactory' > respaldo.sql
  ```

- Las imágenes de productos que se suban deberán guardarse en un volumen, o se perderán al recrear el contenedor.

---

## Errores comunes

| Síntoma | Causa | Solución |
| --- | --- | --- |
| `cannot read distributionUrl property` | El archivo `.mvn/wrapper/maven-wrapper.properties` está incompleto | Recrearlo con `wrapperVersion`, `distributionType` y `distributionUrl` completos |
| `./mvnw: No existe el archivo o el directorio` | Estás en otra carpeta | `cd` a la raíz del proyecto |
| `Could not resolve placeholder 'DB_URL'` | Falta una variable de entorno | Define la variable que indica el mensaje |
| `Access denied for user 'root'` | `DB_PASSWORD` no coincide con la de MySQL | Usa la clave del `.env`; si la cambiaste después de crear el volumen, ver más abajo |
| `Communications link failure` o `Connection refused` | MySQL no está corriendo | `docker compose up -d` y esperar `healthy` |
| `Schema-validation: missing table` (solo en prod) | Las tablas no existen o no coinciden con las entidades | Completar `V1__esquema_inicial.sql` |
| `Port 8080 was already in use` | Otra instancia de la app usa el puerto | Detén la otra instancia |
| El `Dockerfile` está vacío al ejecutar `docker compose up` | El servicio `backend` no tiene receta | Escribir el `Dockerfile` o dejar el bloque comentado |
| Swagger y Actuator piden usuario y contraseña | `SecurityConfig` aún está vacío | Usar `user` y la clave del log, o escribir el `SecurityConfig` |

Para reiniciar la base desde cero (**borra todos los datos**):

```bash
docker compose down -v
docker compose up -d
```

---

## Comandos de Docker de referencia

| Comando | Acción |
| --- | --- |
| `docker compose up -d` | Levanta los servicios en segundo plano |
| `docker compose up -d --build` | Levanta y reconstruye la imagen del backend |
| `docker compose ps` | Muestra el estado (`healthy` cuando está listo) |
| `docker compose logs -f db` | Muestra los logs de MySQL en vivo |
| `docker compose logs -f backend` | Muestra los logs del backend en vivo |
| `docker compose stop` | Detiene los contenedores y conserva los datos |
| `docker compose down` | Elimina los contenedores y conserva los datos |
| `docker compose down -v` | Elimina contenedores **y datos** |
| `docker exec -it tecnofactory-db mysql -uroot -p tecnofactory` | Abre una consola de MySQL |
