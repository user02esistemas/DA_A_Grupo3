package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comprobantes_venta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @ToString.Exclude
    private Venta venta;

    @Column(length = 30)
    private String tipoComprobante;

    @Column(length = 10)
    private String serie;

    @Column(length = 20)
    private String correlativo;

    @Column(length = 50)
    private String codigo;

    @Column(length = 25)
    private String documentoCliente;

    @Column(length = 255)
    private String nombreCliente;

    @Column(length = 255)
    private String direccionCliente;
}
