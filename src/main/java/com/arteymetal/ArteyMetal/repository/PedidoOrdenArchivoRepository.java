package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.PedidoOrdenArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoOrdenArchivoRepository extends JpaRepository<PedidoOrdenArchivo, Long> {
    List<PedidoOrdenArchivo> findByPedidoId(Long pedidoId);
}
