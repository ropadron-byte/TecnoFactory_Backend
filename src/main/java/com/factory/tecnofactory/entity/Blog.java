package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "blog")
@Getter
@Setter
@NoArgsConstructor
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_blog")
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "visible", nullable = false)
    private Boolean visible = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    /** Imagen de portada: una sola por blog. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imagen_principal_id")
    private ImagenUrl imagenPrincipal;

    /** Galería del blog. La misma imagen puede estar en varios blogs. */
    @ManyToMany
    @JoinTable(
            name = "blog_imagen",
            joinColumns = @JoinColumn(name = "blog_id_blog"),
            inverseJoinColumns = @JoinColumn(name = "imagen_id_url"))
    @BatchSize(size = 50)
    private Set<ImagenUrl> imagenes = new HashSet<>();
}
