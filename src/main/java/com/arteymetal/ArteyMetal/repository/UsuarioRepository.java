package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByName(String name);

    Optional<Usuario> findByEmailOrName(String email, String name);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Usuario> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:rolId IS NULL OR u.rol.id = :rolId) AND " +
           "(:activo IS NULL OR u.activo = :activo)")
    Page<Usuario> searchWithFilters(@Param("search") String search, @Param("rolId") Long rolId, @Param("activo") Boolean activo, Pageable pageable);

    boolean existsByRolId(Long rolId);
}
