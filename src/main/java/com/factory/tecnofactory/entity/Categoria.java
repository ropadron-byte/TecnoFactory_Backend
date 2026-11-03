package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla categoria. Se incluye porque ProductoCategoria la necesita.
 * Si tu compañero ya creó esta entidad, usa la suya y borra este archivo
 * (solo hace falta que tenga getId() y getNombre()).
 */
@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "nombre_categoria", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion_categoria", nullable = false)
    private String descripcion;
}
