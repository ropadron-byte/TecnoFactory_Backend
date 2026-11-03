package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_categoria")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductoCategoria {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private ProductoCategoriaId id;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id_producto", nullable = false)
    private Producto producto;

    @MapsId("categoriaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id_categoria", nullable = false)
    private Categoria categoria;

    /** El producto y la categoría ya deben tener id (estar guardados). */
    public ProductoCategoria(Producto producto, Categoria categoria) {
        this.id = new ProductoCategoriaId(producto.getId(), categoria.getId());
        this.producto = producto;
        this.categoria = categoria;
    }
}
