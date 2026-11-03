package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.ImagenUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImagenUrlRepository extends JpaRepository<ImagenUrl, Long> {

    Optional<ImagenUrl> findByUrl(String url);

    boolean existsByUrl(String url);
}
