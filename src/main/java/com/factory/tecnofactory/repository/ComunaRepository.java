package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Comuna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComunaRepository extends JpaRepository<Comuna, Integer> {

    List<Comuna> findByNombre(String nombre);
}
