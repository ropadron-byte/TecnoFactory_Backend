package com.factory.tecnofactory.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "run", nullable = false, unique = true)
    private Integer run;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "run_dv", nullable = false, unique = true, length = 1)
    private String run_dv;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "direccion", nullable = false)
    private String direccion;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fecha_nacimiento;

    @Column(name = "telefono", nullable = false, length = 15)
    private String telefono;

    @Column(name = "contrasenia", nullable = false)
    private String contrasenia;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_usuario_id_tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comuna_id_comuna", nullable = false)
    private Comuna comuna;


}



