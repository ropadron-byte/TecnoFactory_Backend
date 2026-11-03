package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

/**
 * Catálogo de imágenes. Una fila por URL; la misma imagen puede estar
 * asociada a varios blogs, productos y categorías.
 */
@Entity
@Table(name = "imagen_url")
@Getter
@Setter
@NoArgsConstructor
public class ImagenUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_url")
    private Long id;

    @Column(name = "url", nullable = false, length = 1000, unique = true)
    private String url;

    @ManyToMany(mappedBy = "imagenes", targetEntity = Blog.class)
    @BatchSize(size = 50)
    private Set<Blog> blogs = new HashSet<>();

    @ManyToMany(mappedBy = "imagenesProducto", targetEntity = Producto.class)
    @BatchSize(size = 50)
    private Set<Producto> productos = new HashSet<>();

    @ManyToMany(mappedBy = "imagenesCategoria", targetEntity = Categoria.class)
    @BatchSize(size = 50)
    private Set<Categoria> categorias = new HashSet<>();
}
