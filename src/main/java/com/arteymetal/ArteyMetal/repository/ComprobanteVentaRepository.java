package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.ComprobanteVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ComprobanteVentaRepository extends JpaRepository<ComprobanteVenta, Long> {
    Optional<ComprobanteVenta> findByVentaId(Long ventaId);
}
