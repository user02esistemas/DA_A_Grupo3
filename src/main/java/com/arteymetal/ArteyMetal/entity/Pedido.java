package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String codigo;

    @Column(length = 255)
    private String nombreProducto;

    @Column(nullable = false, length = 255)
    private String nombreCliente;

    @Column(length = 20)
    private String telefonoCliente;

    @Column(length = 255)
    private String tipoProducto;

    @Column(columnDefinition = "text")
    private String detalleTrabajo;

    @Builder.Default
    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "registrado";

    @Column(length = 40)
    @Builder.Default
    private String estadoPersonalizacion = "sin_iniciar";

    @Column(length = 30)
    @Builder.Default
    private String estadoPago = "pendiente_adelanto";

    @Column(length = 20)
    @Builder.Default
    private String modalidadPago = "dos_pagos";

    private BigDecimal montoTotal;
    private BigDecimal montoAdelanto;
    private BigDecimal montoSaldo;

    private LocalDate fechaEntregaCompromiso;

    @Column(length = 20)
    @Builder.Default
    private String tipoEntrega = "local";

    @Column(length = 255)
    private String direccionEntrega;

    @Column(length = 120)
    private String distritoEntrega;

    @Column(length = 255)
    private String referenciaEntrega;

    @Column(length = 120)
    private String nombreRecibe;

    @Column(length = 20)
    private String telefonoRecibe;

    private BigDecimal costoDelivery;

    @Column(length = 25)
    private String documentoCliente;

    @Column(length = 255)
    private String correoCliente;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(length = 255)
    private String archivoDisenoPath;

    private LocalDate fechaInicioDiseno;
    private LocalDate fechaAprobacionDiseno;

    @Column(columnDefinition = "text")
    private String observacionesPersonalizacion;

    @Column(length = 30)
    private String medioPagoAdelanto;

    @Column(length = 120)
    private String referenciaPagoAdelanto;

    @Column(length = 255)
    private String voucherPagoAdelantoPath;

    @Column(length = 30)
    @Builder.Default
    private String estadoPagoAdelanto = "pendiente";

    @Column(length = 30)
    private String medioPagoSaldo;

    @Column(length = 120)
    private String referenciaPagoSaldo;

    @Column(length = 255)
    private String voucherPagoSaldoPath;

    @Column(length = 30)
    @Builder.Default
    private String estadoPagoSaldo = "pendiente";

    @Column(length = 255)
    private String comprobantePagoPath;

    @Column(length = 20)
    private String tipoPago;

    @Column(length = 120)
    private String codigoPostalEntrega;

    @Column(columnDefinition = "json")
    private String productosPersonalizados;

    @Column(columnDefinition = "json")
    private String comprobantePago;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @ToString.Exclude
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PedidoProducto> productos;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PedidoDisenoArchivo> archivosDiseno;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PedidoOrdenArchivo> archivosOrden;

    @OneToMany(mappedBy = "pedido")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Venta> ventas;
}
