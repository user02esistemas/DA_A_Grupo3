package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.CajaApertura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaAperturaRepository extends JpaRepository<CajaApertura, Long> {

    Optional<CajaApertura> findByIdAndEstado(Long id, String estado);

    List<CajaApertura> findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long usuarioId, String estado);

    Optional<CajaApertura> findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long usuarioId, String estado);

    Page<CajaApertura> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT ca FROM CajaApertura ca WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(ca.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ca.usuario.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CajaApertura> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT ca FROM CajaApertura ca WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(ca.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ca.usuario.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:estado IS NULL OR :estado = '' OR ca.estado = :estado)")
    Page<CajaApertura> searchWithFilters(@Param("search") String search, @Param("estado") String estado, Pageable pageable);

    long countByCajaIdAndEstado(Long cajaId, String estado);

    boolean existsByCajaId(Long cajaId);

    List<CajaApertura> findByUsuarioIdAndEstado(Long usuarioId, String estado);
}
