package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String tipoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @ToString.Exclude
    private Pedido pedido;

    @Column(length = 255)
    private String clienteNombre;

    @Column(nullable = false)
    private LocalDate fechaVenta;

    @Column(nullable = false)
    private BigDecimal montoTotal;

    private BigDecimal montoCobrado;

    @Column(length = 30)
    @Builder.Default
    private String estadoPago = "pagado_completo";

    @Column(columnDefinition = "text")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_apertura_id")
    @ToString.Exclude
    private CajaApertura cajaApertura;

    @Column(length = 30)
    private String metodoPago;

    private BigDecimal montoEfectivo;
    private BigDecimal montoDigital;
    private BigDecimal vuelto;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<VentaDetalle> detalles;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<ComprobanteVenta> comprobantes;

    public ComprobanteVenta getComprobante() {
        if (comprobantes == null || comprobantes.isEmpty()) return null;
        return comprobantes.get(comprobantes.size() - 1);
    }
}
