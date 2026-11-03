package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    @EntityGraph(attributePaths = {"imagenPrincipal", "usuario"})
    Page<Blog> findByVisibleTrue(Pageable pageable);
}
