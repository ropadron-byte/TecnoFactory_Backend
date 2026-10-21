# 02. Agregar una entidad completa

Receta para cada tabla nueva (Categoría, Producto, Boleta...). Ejemplo con **`Categoria`**. Sigue el orden: cada paso se apoya en el anterior.

## Checklist

| # | Paso | Archivo | Detalle |
| --- | --- | --- | --- |
| 1 | Revisar la tabla en `V1` | `db/migration/V1__...sql` | La base manda; la entidad la copia |
| 2 | **Entidad** | `model/Categoria.java` | `@Entity`, `@Table(name=...)`, cada `@Column(name=...)`, `@Id` + `IDENTITY` |
| 3 | **Repository** | `repository/CategoriaRepository.java` | `extends JpaRepository<Categoria, Long>`; consultas por **atributo Java** |
| 4 | **Request** | `dto/CategoriaRequest.java` | Solo lo que el cliente puede mandar, con validaciones. **Sin `id` ni `activo`** |
| 5 | **Response** | `dto/CategoriaResponse.java` | Lo que se devuelve; nunca la entidad |
| 6 | **Mapper** | `mapper/CategoriaMapper.java` | `@Mapper(componentModel = "spring")` (MapStruct) |
| 7 | **Service** | `service/CategoriaService.java` | `@Transactional`; reglas de negocio; devuelve DTOs |
| 8 | **Controller** | `controller/CategoriaController.java` | Sin lógica; `@Valid`; códigos HTTP correctos |
| 9 | **Reglas de rol** | `security/SecurityConfig.java` | Lo específico antes de lo general |
| 10 | **Tests** | `test/...` | Service (Mockito) y controller (`@WebMvcTest`) |
| 11 | **Probar en Swagger** | | Con y sin token, con cada rol |
| 12 | **Documentar** | `docs/estado/cambios-recientes.md` | Qué se hizo y qué se probó |

## Piezas clave

**Entidad** (la base manda):

```java
@Entity
@Table(name = "categoria")
@Getter @Setter @NoArgsConstructor
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "nombre_categoria", nullable = false, length = 100)
    private String nombre;
}
```

**Repository:**

```java
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombre(String nombre);   // "Nombre" es el ATRIBUTO Java, no la columna
}
```

**Service** (patrón):

```java
@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService {
    private final CategoriaRepository repo;
    private final CategoriaMapper mapper;

    public CategoriaResponse crear(CategoriaRequest req) {
        if (repo.existsByNombre(req.nombre()))
            throw new ReglaNegocioException("La categoría ya existe");
        return mapper.toResponse(repo.save(mapper.toEntity(req)));
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return repo.findById(id).map(mapper::toResponse)
            .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
    }
}
```

**Controller** (patrón): `POST` → `201` + `Location`; `GET /{id}` → `200`; los errores salen solos por `GlobalExceptionHandler`.

**Regla de rol** (ejemplo: lee cualquiera con sesión; escribe vendedor o administrador):

```java
.requestMatchers(HttpMethod.GET, "/api/categorias/**").authenticated()
.requestMatchers("/api/categorias/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
```

## Antes de decir «terminé»

- [ ] La app arranca (`validate` revisa que la entidad calce con la base).
- [ ] `./mvnw test -Dtest="*Test"` en verde.
- [ ] Probé sin token (401), con un rol sin permiso (403) y con el rol correcto.
- [ ] Un dato inválido da 400; un duplicado da 409; un id inexistente da 404.
- [ ] El Response no contiene nada que no deba verse.

## Para saber más

[Guía 07](../guias/07-entidades-y-repositories.md) · [Guía 08](../guias/08-dto-mapper-y-validaciones.md) · [Guía 09](../guias/09-service-y-controller.md) · [Referencia 19 (el ejemplo completo con `Usuario`)](../referencia/19-validacion-run-service-y-controller-explicados.md) · [Referencia 21 (seguridad)](../referencia/21-seguridad-jwt-y-roles-explicada.md)
