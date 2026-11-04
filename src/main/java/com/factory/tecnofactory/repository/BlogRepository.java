package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    @EntityGraph(attributePaths = {"imagenPrincipal", "usuario"})
    Page<Blog> findByVisibleTrue(Pageable pageable);

    /** Listado de gestión: todos los blogs, visibles o no. */
    @Override
    @EntityGraph(attributePaths = {"imagenPrincipal", "usuario"})
    Page<Blog> findAll(Pageable pageable);

    /** Blogs que usan la imagen como portada (la portada también debería estar en la galería). */
    List<Blog> findByImagenPrincipalId(Long idImagen);
}
