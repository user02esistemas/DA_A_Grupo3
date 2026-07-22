package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto_imagenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    private Producto producto;

    @Column(nullable = false, length = 255)
    private String archivoPath;

    @Column(length = 255)
    private String nombreOriginal;

    @Column(length = 100)
    private String mimeType;

    private Long tamanoBytes;
}
