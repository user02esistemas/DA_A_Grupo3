package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_diseno_archivos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDisenoArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @ToString.Exclude
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_producto_id")
    @ToString.Exclude
    private PedidoProducto pedidoProducto;

    @Column(length = 20)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String archivoPath;

    @Column(length = 255)
    private String nombreOriginal;

    @Column(length = 100)
    private String mimeType;

    private Long tamanoBytes;
}
