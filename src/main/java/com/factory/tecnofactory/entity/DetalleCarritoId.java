package com.factory.tecnofactory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Clave primaria compuesta de detalle_carrito. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DetalleCarritoId implements Serializable {

    @Column(name = "carrito_id_carrito")
    private Long carritoId;

    @Column(name = "producto_id_producto")
    private Long productoId;
}
