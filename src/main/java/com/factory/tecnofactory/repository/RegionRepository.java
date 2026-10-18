package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Region;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    Optional<List<Region>> findByNombre(String nombre);


//    Boolean existsByCorreo(String correo);
//    Boolean existsByTipoUsuarioNombre(String nombre);
//
//    Optional<List<Usuario>> findAllByTipoUsuarioNombre(String nombre);

}
