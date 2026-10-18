package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.Region;
import com.factory.tecnofactory.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComunaRepository extends JpaRepository<Comuna, Integer> {

    Optional<List<Comuna>> findByNombre(String nombre);
}

