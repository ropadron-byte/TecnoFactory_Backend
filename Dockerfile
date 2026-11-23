# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Wrapper de Maven: fija la versión de Maven que declara .mvn/wrapper/maven-wrapper.properties.
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Evita fallos típicos si mvnw se editó en Windows (saltos de línea CRLF o sin permiso de ejecución).
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# El pom primero: mientras no cambie, Docker reutiliza las dependencias ya descargadas.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline

COPY src ./src
# Los tests de Testcontainers necesitan Docker dentro del build; se corren aparte (./mvnw test).
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests && \
    mv target/TecnoFactory-*.jar target/app.jar

# ---------- Etapa 2: imagen final (solo JRE) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: si alguien vulnera la app, no es root dentro del contenedor.
RUN addgroup -S -g 1000 app && adduser -S -u 1000 -G app app \
    && mkdir -p /app/uploads && chown app:app /app/uploads

COPY --from=build --chown=app:app /build/target/app.jar app.jar

USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOADS_DIR=/app/uploads \
    TZ=America/Santiago \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
VOLUME /app/uploads

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
