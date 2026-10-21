# Guía 07. Entidades y repositories

> **En una frase:** una **entidad** es una clase Java que representa una fila de una tabla; un **repository** es la interfaz que busca y guarda esas filas sin que escribas SQL.

## La idea

La entidad es la **ficha** de un libro; el repository es el **bibliotecario**. Tú le dices «tráeme los libros cuyo autor sea X» con el nombre de un método, y él arma la consulta.

```
Tabla tipo_usuario                    Objetos Java
| 1 | ADMINISTRADOR |    -->    new TipoUsuario()  -> id=1, nombre="ADMINISTRADOR"
| 2 | VENDEDOR      |
| 3 | CLIENTE       |
```

## Las piezas existentes

| Entidad | Tabla | Detalle |
| --- | --- | --- |
| `TipoUsuario` | `tipo_usuario` | Los roles |
| `Region`, `Comuna` | `region`, `comuna` | Ids **manuales** (`Integer`, sin `@GeneratedValue`) |
| `Usuario` | `usuario` | `@ManyToOne` a `TipoUsuario` y `Comuna`; `activo = true` por defecto |

Cada una tiene su repository (`extends JpaRepository<Entidad, TipoDelId>`). Faltan 8 entidades (ver [hoja de ruta](16-hoja-de-ruta.md)).

## Cómo se escribe una entidad

| Anotación | Qué hace |
| --- | --- |
| `@Entity` + `@Table(name = "...")` | Esta clase es esa tabla |
| `@Id` + `@GeneratedValue(IDENTITY)` | La clave primaria que asigna la base (`AUTO_INCREMENT`) |
| `@Column(name = "...")` | A qué columna corresponde el atributo. **Todas llevan el nombre explícito** |
| `@ManyToOne(fetch = LAZY)` + `@JoinColumn` | Relación hacia otra tabla; `LAZY` = se carga solo cuando se necesita |
| `@JdbcTypeCode(SqlTypes.CHAR)` | Para `CHAR(1)` (el dígito verificador) |
| Lombok (`@Getter`, `@Setter`...) | Genera los getters y setters |

## Cómo se lee el nombre de un método de repository

Spring corta el nombre por las **mayúsculas** y busca cada trozo como **atributo de la clase Java**:

```
findByCorreo        -> WHERE correo = ?
existsByRun         -> ¿hay al menos una fila con ese run?
existsByTipoUsuarioNombre -> usuario.tipoUsuario.nombre = ?   (Spring hace el JOIN)
```

| Palabra | Efecto |
| --- | --- |
| `findBy` / `existsBy` / `countBy` / `deleteBy` | Buscar / saber si existe / contar / borrar |
| `And`, `Or` | Unir condiciones |

**Qué devolver:** `Optional<Entidad>` si la columna es `UNIQUE`; `List<Entidad>` si puede haber varias (**nunca** `Optional<List<...>>`); `boolean` si solo quieres saber si existe.

## Reglas que no hay que olvidar

1. **La entidad refleja a `V1`, no al revés.** `unique`, `nullable` y `length` en `@Column` son documentación: no crean ni verifican nada.
2. **`validate` comprueba** que existan tabla, columnas y tipos; **no** comprueba índices, claves únicas ni claves foráneas.
3. **Los métodos usan atributos Java, no columnas:** `findByNombre`, no `findByNombreComuna`.
4. **Evita guiones bajos en los atributos Java**: en los nombres de método el `_` tiene significado especial.
5. **El valor por defecto de la base no se aplica si Hibernate manda `NULL`**: inicializa en Java (`private Boolean activo = true;`).
6. **`int` frente a `Integer`:** usa `Integer`/`Long` para ids.

## Si algo falla

| Mensaje | Dónde mirar |
| --- | --- |
| `Schema-validation: missing table [x]` | `@Table` de la entidad |
| `Schema-validation: missing column [c] in table [t]` | `@Column(name)` o `@Table` de la entidad de `t` |
| `Schema-validation: wrong column type` | Tipo Java o falta de `@JdbcTypeCode` |
| `No property 'x' found for type 'T'` | Nombre del método del repository: usa atributos Java |
| `Error creating bean 'xRepository'` | Busca `Cannot create query for method [...]` más abajo en el log |

## Para saber más

[Referencia 13: entidades](../referencia/13-entidades-explicadas.md) · [Referencia 14: repositories](../referencia/14-repositories-explicados.md) · [Referencia 06: mapeo tabla → entidad](../referencia/06-base-de-datos.md#mapeo-a-entidades-java)
