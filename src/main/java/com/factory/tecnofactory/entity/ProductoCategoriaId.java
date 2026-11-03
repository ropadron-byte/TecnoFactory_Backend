package com.factory.tecnofactory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Clave primaria compuesta de producto_categoria. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductoCategoriaId implements Serializable {

    @Column(name = "producto_id_producto")
    private Long productoId;

    @Column(name = "categoria_id_categoria")
    private Long categoriaId;
}
