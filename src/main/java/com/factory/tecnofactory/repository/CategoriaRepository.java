package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
