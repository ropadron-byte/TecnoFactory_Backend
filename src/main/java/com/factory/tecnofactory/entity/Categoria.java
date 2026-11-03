package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

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

    // Imágenes compartidas. La relación vive en categoria_imagen y la maneja Categoria.
    @ManyToMany
    @JoinTable(
            name = "categoria_imagen",
            joinColumns = @JoinColumn(name = "categoria_id_categoria"),
            inverseJoinColumns = @JoinColumn(name = "imagen_id_url"))
    @BatchSize(size = 50)
    private Set<ImagenUrl> imagenesCategoria = new HashSet<>();
}
