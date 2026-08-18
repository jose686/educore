package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad que representa un paquete de cursos (Bundle o Promoción) en oferta.
 * Permite agrupar varios cursos de LMS bajo un precio consolidado, aislando completamente
 * la lógica comercial y de promociones de la entidad principal Curso.
 */
@Entity
@Table(name = "promociones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "precio_oferta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioOferta;

    /**
     * Lista de identificadores de los cursos asociados (LMS) incluidos en este bundle.
     * Mapeado como una colección de elementos básicos para garantizar el desacoplamiento
     * de módulos en la base de datos sin relaciones de clave foránea directas con Curso.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "promocion_cursos",
        joinColumns = @JoinColumn(name = "promocion_id")
    )
    @Column(name = "lms_curso_id")
    @Builder.Default
    private Set<Long> lmsCursoIds = new HashSet<>();
}
