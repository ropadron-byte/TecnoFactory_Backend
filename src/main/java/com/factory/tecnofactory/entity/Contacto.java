package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mensaje enviado por cualquier visitante desde el formulario de contacto. */
@Entity
@Table(name = "contacto")
@Getter
@Setter
@NoArgsConstructor
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contacto")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 250)
    private String correo;

    @Column(name = "comentario", nullable = false, length = 500)
    private String comentario;
}
