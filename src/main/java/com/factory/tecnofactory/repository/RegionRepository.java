package com.factory.tecnofactory.repository;

import com.factory.tecnofactory.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Integer> {

    List<Region> findByNombre(String nombre);
}
