package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.Caja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {

    List<Caja> findByActivaTrue();

    Page<Caja> findAll(Pageable pageable);
}
