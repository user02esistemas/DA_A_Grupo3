package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    @Query("SELECT p FROM Producto p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Producto> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoria IS NULL OR :categoria = '' OR p.categoria = :categoria) AND " +
           "(:activo IS NULL OR p.activo = :activo)")
    Page<Producto> searchWithFilters(@Param("search") String search,
                                     @Param("categoria") String categoria,
                                     @Param("activo") Boolean activo,
                                     Pageable pageable);

    Page<Producto> findByActivoTrue(Pageable pageable);

    Page<Producto> findByCategoria(String categoria, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.stockActual), 0) FROM Producto p")
    Long sumStockActual();

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoria(String categoria);

    boolean existsByCodigo(String codigo);
}
