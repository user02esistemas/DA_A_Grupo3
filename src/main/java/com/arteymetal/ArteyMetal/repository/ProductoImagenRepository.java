package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProductoId(Long productoId);
}
