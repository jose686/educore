package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que representa un curso ofertado como producto en el catálogo del
 * e-commerce.
 * Utiliza UUID como identificador principal.
 */
@Entity
@Table(name = "productos_cursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCurso {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "descripcion_corta", length = 500)
    private String descripcionCorta;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "imagen_portada_url")
    private String imagenPortadaUrl;

    /**
     * Estado del producto: "PUBLISHED" o "DRAFT".
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "DRAFT";

    /**
     * ID del curso en el módulo 'lms' (aula virtual).
     * Mapeado como un valor numérico para mantener el aislamiento del módulo.
     */
    @Column(name = "lms_curso_id", nullable = true)
    private Long lmsCursoId;
}
