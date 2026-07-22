package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.PedidoProductoArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoProductoArchivoRepository extends JpaRepository<PedidoProductoArchivo, Long> {

    List<PedidoProductoArchivo> findByPedidoProductoId(Long pedidoProductoId);
}
