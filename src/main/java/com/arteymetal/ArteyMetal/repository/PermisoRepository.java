package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
}
