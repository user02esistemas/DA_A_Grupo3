package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    private BigDecimal precioUnitario;

    @Builder.Default
    private Integer cantidad = 1;

    private BigDecimal total;

    @Builder.Default
    private Integer orden = 0;

    @Builder.Default
    private Integer cantidadRecoge = 0;

    @OneToMany(mappedBy = "pedidoProducto", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<PedidoProductoArchivo> archivos;

    @OneToMany(mappedBy = "pedidoProducto", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<PedidoDisenoArchivo> archivosDiseno;
}
