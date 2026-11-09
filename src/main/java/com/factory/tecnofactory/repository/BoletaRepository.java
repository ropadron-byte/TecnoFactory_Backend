package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Boleta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    Page<Boleta> findByUsuarioCorreoOrderByFechaDesc(String correo, Pageable pageable);

    Page<Boleta> findAllByOrderByFechaDesc(Pageable pageable);
}
