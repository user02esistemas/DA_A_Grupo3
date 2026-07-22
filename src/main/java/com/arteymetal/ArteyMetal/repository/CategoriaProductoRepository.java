package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {
    List<CategoriaProducto> findByActivoTrue();
    List<CategoriaProducto> findAllByOrderByNombre();
    List<CategoriaProducto> findByActivoTrueOrderByNombre();

    boolean existsBySlug(String slug);
}
