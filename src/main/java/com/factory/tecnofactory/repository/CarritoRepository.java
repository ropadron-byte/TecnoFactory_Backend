package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioCorreo(String correo);
}
