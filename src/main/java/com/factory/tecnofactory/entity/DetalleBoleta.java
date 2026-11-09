package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_boleta")
@Getter
@Setter
@NoArgsConstructor
public class DetalleBoleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_boleta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boleta_id_boleta", nullable = false)
    private Boleta boleta;

    // Copia del nombre y precio al momento de la compra: no cambian si se edita el producto
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "precio_unitario", nullable = false)
    private Long precioUnitario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id_producto", nullable = false)
    private Producto producto;

    @Column(name = "descuento", nullable = false)
    private Integer descuento;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;
}
