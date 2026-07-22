package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Pedido;
import com.arteymetal.ArteyMetal.entity.PedidoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoProductoRepository extends JpaRepository<PedidoProducto, Long> {

    List<PedidoProducto> findByPedidoId(Long pedidoId);

    List<PedidoProducto> findByPedido(Pedido pedido);
}
