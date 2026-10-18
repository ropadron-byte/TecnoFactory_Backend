package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
public class Region {

    @Id
    @Column(name = "id_region")
    private int id;

    @Column(name = "nombre_region", nullable = false, unique = true)
    private String nombre;

}

//
//CREATE TABLE region (
//        id_region INT NOT NULL,
//        nombre_region VARCHAR(255) NOT NULL,
//CONSTRAINT region_PK PRIMARY KEY (id_region)
//) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


//package com.factory.tecnofactory.entity;
//
//import jakarta.persistence.*;
//        import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(name = "tipo_usuario")
//@Getter
//@Setter
//@NoArgsConstructor
//public class TipoUsuario {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id_tipo_usuario")
//    private Long id;
//
//    @Column(name = "nombre_tipo_usuario", nullable = false, unique = true)
//    private String nombre;
//}