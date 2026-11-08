package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_carrito")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetalleCarrito {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private DetalleCarritoId id;

    @MapsId("carritoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id_carrito", nullable = false)
    private Carrito carrito;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /** El carrito y el producto ya deben tener id (estar guardados). */
    public DetalleCarrito(Carrito carrito, Producto producto, Integer cantidad) {
        this.id = new DetalleCarritoId(carrito.getId(), producto.getId());
        this.carrito = carrito;
        this.producto = producto;
        this.cantidad = cantidad;
    }
}
