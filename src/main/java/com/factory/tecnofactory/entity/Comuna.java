package com.factory.tecnofactory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comuna")
@Getter
@Setter
@NoArgsConstructor
public class Comuna {

    @Id
    @Column(name = "id_comuna")
    private int id;

    @Column(name = "nombre_comuna", nullable = false, unique = true)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id_region", nullable = false)
    private Region region;

}

//
//CREATE TABLE comuna (
//        id_comuna INT NOT NULL,
//        nombre_comuna VARCHAR(255) NOT NULL,
//region_id_region INT NOT NULL,
//CONSTRAINT comuna_PK PRIMARY KEY (id_comuna)
//) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



