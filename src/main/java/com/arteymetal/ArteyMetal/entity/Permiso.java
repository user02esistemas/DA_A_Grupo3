package com.arteymetal.ArteyMetal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permisos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @ManyToMany(mappedBy = "permisos")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.Set<Rol> roles;
}
