# Guía 01. El proyecto en una página

> **En una frase:** TecnoFactory es una API REST (el «backend») de una tienda de tecnología. Guarda productos, usuarios y compras en MySQL, y la consume un frontend en React.

## La idea

Piensa en un **restaurante**: el cliente (el frontend) hace un pedido, el mesero lo lleva a la cocina, la cocina usa la despensa y el mesero trae el plato. Cada persona hace **una** tarea. El código está organizado igual, en capas.

| En el restaurante | En el código | Carpeta |
| --- | --- | --- |
| Mesero | `controller`: recibe la petición, la valida y responde | `controller/` |
| Cocinero | `service`: aplica las reglas del negocio | `service/` |
| Despensa | `repository` + base de datos | `repository/`, MySQL |
| Hoja del pedido / plato servido | `dto`: lo que entra y lo que sale | `dto/` |
| Traductor entre cocina y plato | `mapper`: entidad ⇄ DTO | `mapper/` |
| Ingredientes | `entity`: una clase por tabla | `entity/` |
| Libro de reclamos | `exception`: errores con un formato único | `exception/` |
| Guardia de la puerta | `security`: login, token y roles | `security/` |

## Con qué está hecho

| Pieza | Versión o herramienta | Para qué |
| --- | --- | --- |
| Lenguaje | Java 21 | Todo el backend |
| Framework | Spring Boot 4.1.1 | Arma la app, el servidor web y la inyección de dependencias |
| Base de datos | MySQL 8.4 en Docker | Guarda los datos |
| Migraciones | Flyway | Crea y versiona las tablas con archivos SQL |
| Acceso a datos | Spring Data JPA + Hibernate | Convierte clases Java en filas |
| Seguridad | Spring Security + JWT (jjwt 0.12.6) + BCrypt | Login, tokens y roles |
| Mapeo | MapStruct 1.6.3 | Copia entidad ⇄ DTO sin escribirlo a mano |
| Validación | Bean Validation (`@Valid`) | Revisa lo que llega |
| Documentación de la API | springdoc (Swagger) | Probar los endpoints desde el navegador |
| Pruebas | JUnit, Mockito, MockMvc | Probar sin base de datos |

Redis, correo y caché están en el `pom.xml` pero **todavía no se usan**.

## El camino de una petición

```
Frontend --> Seguridad (¿trae token válido?) --> Controller (@Valid)
         --> Service (reglas del negocio) --> Repository --> MySQL
         <-- DTO de respuesta (sin contraseña) <-- Service <--
Cualquier error --> GlobalExceptionHandler --> JSON uniforme (ErrorResponse)
```

## Reglas que no hay que olvidar

1. **Una clase `public` por archivo.**
2. **El controller no tiene lógica de negocio**; el service sí.
3. **Nunca se devuelve una entidad**: siempre un DTO.
4. **El esquema lo manda Flyway (`V1`...)**, no las entidades.
5. **Lo que no está permitido en `SecurityConfig` queda protegido.**

## Para saber más

- Detalle del stack y de los paquetes: [referencia 01](../referencia/01-arquitectura-y-stack.md)
- Cómo viaja una petición, paso a paso: [personal 06](../personal/06-como-viaja-una-peticion.md)
- Qué falta construir: [guía 16](16-hoja-de-ruta.md)
