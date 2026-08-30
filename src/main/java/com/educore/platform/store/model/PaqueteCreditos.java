package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Entidad que representa un paquete de créditos de recarga.
 */
@Entity
@Table(name = "paquetes_creditos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaqueteCreditos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer creditos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    private String badge;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "orden_visualizacion", nullable = false)
    @Builder.Default
    private Integer ordenVisualizacion = 0;
}
