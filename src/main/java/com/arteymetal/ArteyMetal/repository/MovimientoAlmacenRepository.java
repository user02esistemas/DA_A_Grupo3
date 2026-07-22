package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.MovimientoAlmacen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoAlmacenRepository extends JpaRepository<MovimientoAlmacen, Long> {

    Page<MovimientoAlmacen> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT m FROM MovimientoAlmacen m WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(m.concepto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.producto.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.producto.codigo) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MovimientoAlmacen> search(@Param("search") String search, Pageable pageable);

    Page<MovimientoAlmacen> findByTipo(String tipo, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoAlmacen m WHERE m.tipo = :tipo AND m.createdAt >= :inicio AND m.createdAt < :fin")
    Long sumCantidadByTipoAndDate(@Param("tipo") String tipo, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoAlmacen m WHERE m.tipo = :tipo AND m.createdAt >= :inicio AND m.createdAt < :fin")
    Long sumCantidadByTipoAndFecha(@Param("tipo") String tipo, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    List<MovimientoAlmacen> findByPedidoIdOrderByIdDesc(Long pedidoId);
}
