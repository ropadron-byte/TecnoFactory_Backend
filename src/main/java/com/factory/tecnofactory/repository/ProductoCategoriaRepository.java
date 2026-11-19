package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.ProductoCategoria;
import com.factory.tecnofactory.entity.ProductoCategoriaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoCategoriaRepository extends JpaRepository<ProductoCategoria, ProductoCategoriaId> {

    /** Productos activos de la categoría (lo que ve el público). */
    long countByCategoria_IdAndProducto_ActivoTrue(Long idCategoria);

    /** Cualquier producto, activo o no: la categoría no se puede borrar si tiene alguno. */
    boolean existsByCategoria_Id(Long idCategoria);
}
