package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_aperturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaApertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    @ToString.Exclude
    private Caja caja;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    @Column(nullable = false)
    private BigDecimal montoInicial;

    private LocalDateTime fechaCierre;
    private BigDecimal montoFinal;

    @Builder.Default
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "abierta";

    @Column(columnDefinition = "text")
    private String observaciones;
}
