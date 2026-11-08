package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carrito")
@Getter
@Setter
@NoArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito")
    private Long id;

    // Un usuario tiene un solo carrito (usuario_id_usuario es UNIQUE en la base)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<DetalleCarrito> detalles = new HashSet<>();
}
