package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Page<Producto> findByActivoTrue(Pageable pageable);

    // categorias (Set<ProductoCategoria>) -> categoria -> id
    Page<Producto> findByActivoTrueAndCategorias_Categoria_Id(Long idCategoria, Pageable pageable);

    Page<Producto> findByActivoTrueAndDescuentoGreaterThan(Integer descuento, Pageable pageable);

    // Stock crítico: quedan igual o menos unidades que el mínimo definido
    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockCritico order by p.stock asc")
    List<Producto> findCriticos();
}
