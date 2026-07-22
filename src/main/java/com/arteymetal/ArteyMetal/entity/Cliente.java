package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombreCompleto;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String correo;

    @Column(length = 25)
    private String documento;

    @Column(columnDefinition = "text")
    private String direccion;

    @Column(columnDefinition = "text")
    private String observaciones;

    @OneToMany(mappedBy = "cliente")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pedido> pedidos;
}
