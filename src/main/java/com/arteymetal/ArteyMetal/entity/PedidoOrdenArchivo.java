package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_orden_archivos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoOrdenArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @Column(nullable = false, length = 255)
    private String archivoPath;

    @Column(length = 255)
    private String nombreOriginal;

    @Column(length = 100)
    private String mimeType;

    private Long tamanoBytes;
}
