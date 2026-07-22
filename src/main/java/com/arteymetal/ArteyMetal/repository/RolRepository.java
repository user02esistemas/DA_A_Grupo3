package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);

    @Query("SELECT r FROM Rol r WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(r.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Rol> search(@Param("search") String search, Pageable pageable);

    Page<Rol> findAll(Pageable pageable);

    java.util.List<Rol> findAllByOrderByNombre();

    Page<Rol> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String nombre, String descripcion, Pageable pageable);
}
