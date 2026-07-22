package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Page<Venta> findAllByOrderByFechaVentaDesc(Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.clienteNombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.tipoVenta) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Venta> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.clienteNombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.tipoVenta) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:tipoVenta IS NULL OR :tipoVenta = '' OR v.tipoVenta = :tipoVenta)")
    Page<Venta> searchWithFilters(@Param("search") String search,
                                  @Param("tipoVenta") String tipoVenta,
                                  Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.cajaApertura.id = :cajaId AND " +
           "(:search IS NULL OR :search = '' OR LOWER(v.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.clienteNombre) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.tipoVenta) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:tipoVenta IS NULL OR :tipoVenta = '' OR v.tipoVenta = :tipoVenta)")
    Page<Venta> searchWithFiltersByCaja(@Param("cajaId") Long cajaId,
                                        @Param("search") String search,
                                        @Param("tipoVenta") String tipoVenta,
                                        Pageable pageable);

    Page<Venta> findByCajaAperturaId(Long cajaAperturaId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.montoCobrado), 0) FROM Venta v WHERE v.fechaVenta BETWEEN :inicio AND :fin")
    BigDecimal sumMontoCobradoByFechaVentaBetween(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaVenta = :fecha")
    Long countByFechaVenta(@Param("fecha") LocalDate fecha);

    Page<Venta> findByFechaVentaBetween(LocalDate inicio, LocalDate fin, Pageable pageable);

    Page<Venta> findByTipoVenta(String tipoVenta, Pageable pageable);

    Optional<Venta> findByCodigo(String codigo);

    List<Venta> findByCajaAperturaId(Long cajaAperturaId);

    List<Venta> findAllByOrderByFechaVentaDesc();

    @Query("SELECT COALESCE(SUM(v.montoCobrado), 0) FROM Venta v WHERE v.cajaApertura.id = :cajaAperturaId")
    BigDecimal sumTotalByCajaAperturaId(@Param("cajaAperturaId") Long cajaAperturaId);
}
