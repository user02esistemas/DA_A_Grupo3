package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cajas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;
}
