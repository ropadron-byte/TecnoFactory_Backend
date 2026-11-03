package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {
}
