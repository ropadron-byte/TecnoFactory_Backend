package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Long precio;

    @Column(name = "stock", nullable = false)
    private Long stock;

    @Column(name = "stock_critico", nullable = false)
    private Long stockCritico;

    @Column(name = "descuento", nullable = false)
    private Integer descuento = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Tabla intermedia producto_categoria. Se guarda y se borra junto con el producto.
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<ProductoCategoria> categorias = new HashSet<>();
}
