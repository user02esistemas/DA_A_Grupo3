package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Pedido;
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
public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    Page<Pedido> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombreCliente) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.tipoProducto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.estado) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Pedido> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombreCliente) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.tipoProducto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.estado) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:estado IS NULL OR :estado = '' OR p.estado = :estado) AND " +
           "(:estadoPersonalizacion IS NULL OR :estadoPersonalizacion = '' OR p.estadoPersonalizacion = :estadoPersonalizacion)")
    Page<Pedido> searchWithFilters(@Param("search") String search,
                                   @Param("estado") String estado,
                                   @Param("estadoPersonalizacion") String estadoPersonalizacion,
                                   Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE p.usuario.id = :usuarioId AND " +
           "(:search IS NULL OR :search = '' OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombreCliente) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.tipoProducto) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.estado) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:estado IS NULL OR :estado = '' OR p.estado = :estado) AND " +
           "(:estadoPersonalizacion IS NULL OR :estadoPersonalizacion = '' OR p.estadoPersonalizacion = :estadoPersonalizacion)")
    Page<Pedido> searchWithFiltersByUsuario(@Param("usuarioId") Long usuarioId,
                                            @Param("search") String search,
                                            @Param("estado") String estado,
                                            @Param("estadoPersonalizacion") String estadoPersonalizacion,
                                            Pageable pageable);

    List<Pedido> findByEstadoPersonalizacionIn(List<String> estadosPersonalizacion);

    List<Pedido> findByEstadoIn(List<String> estados);

    long countByEstado(String estado);

    @Query("SELECT COALESCE(MAX(p.id), 0) FROM Pedido p")
    Long maxId();

    List<Pedido> findByEstadoOrderByIdDesc(String estado);

    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findAllByOrderByIdDesc();
}
