package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Pedido;
import com.arteymetal.ArteyMetal.entity.PedidoDisenoArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoDisenoArchivoRepository extends JpaRepository<PedidoDisenoArchivo, Long> {
    List<PedidoDisenoArchivo> findByPedidoId(Long pedidoId);

    List<PedidoDisenoArchivo> findByPedido(Pedido pedido);
}
