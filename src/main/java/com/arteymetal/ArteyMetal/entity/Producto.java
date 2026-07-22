package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(length = 40)
    private String categoria;

    @Column(columnDefinition = "text")
    private String descripcion;

    private BigDecimal precioReferencia;

    @Builder.Default
    @Column(nullable = false)
    private Integer stockTienda = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer stockAlmacen = 0;

    @Column(nullable = false)
    private Integer stockActual;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @PrePersist
    @PreUpdate
    public void calcularStockActual() {
        this.stockActual = (this.stockTienda != null ? this.stockTienda : 0)
                         + (this.stockAlmacen != null ? this.stockAlmacen : 0);
    }

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductoImagen> imagenes;
}
