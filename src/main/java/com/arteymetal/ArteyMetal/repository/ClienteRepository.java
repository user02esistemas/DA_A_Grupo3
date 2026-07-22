package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.nombreCompleto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.telefono) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.correo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.documento) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Cliente> search(@Param("search") String search, Pageable pageable);

    Page<Cliente> findAll(Pageable pageable);

    Boolean existsByDocumento(String documento);

    Optional<Cliente> findByDocumento(String documento);

    List<Cliente> findByNombreCompletoContainingIgnoreCase(String nombreCompleto);
}
