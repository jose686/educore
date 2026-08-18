package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa un cupón de promoción (Descuento o Acceso Temporal).
 */
@Entity
@Table(name = "cupones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCupon tipo;

    @Column(name = "descuento_porcentaje")
    private Integer descuentoPorcentaje; // Usado para DESCUENTO (ej: 15)

    @Column(name = "dias_acceso")
    private Integer diasAcceso; // Usado para ACCESO_TEMPORAL (ej: 15)

    @Column(name = "curso_id")
    private Long cursoId; // Usado para ACCESO_TEMPORAL (indica a qué curso se da acceso)

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
