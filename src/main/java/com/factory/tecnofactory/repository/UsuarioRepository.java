package com.factory.tecnofactory.repository;


import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByRun(Integer run);

    Boolean existsByRun(Integer run);
    Boolean existsByCorreo(String correo);
    Boolean existsByTipoUsuarioNombre(String nombre);

    Optional<List<Usuario>> findAllByTipoUsuarioNombre(String nombre);

}
