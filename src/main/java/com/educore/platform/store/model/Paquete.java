package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un paquete agrupado (Bundle) de cursos.
 */
@Entity
@Table(name = "paquetes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    /**
     * IDs de los cursos contenidos en este paquete.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "paquete_cursos",
        joinColumns = @JoinColumn(name = "paquete_id")
    )
    @Column(name = "curso_id")
    @Builder.Default
    private Set<Long> cursoIds = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
