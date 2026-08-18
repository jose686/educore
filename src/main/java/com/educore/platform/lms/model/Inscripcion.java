package com.educore.platform.lms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa la matriculación de un estudiante en un curso.
 * Si fechaFin es null, el acceso es permanente.
 * Si fechaFin tiene valor y es futuro, el acceso es temporal.
 */
@Entity
@Table(
    name = "inscripciones",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_estudiante_curso",
        columnNames = {"student_id", "lms_curso_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lms_curso_id", nullable = false)
    private Long lmsCursoId;

    @Column(name = "fecha_inscripcion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaInscripcion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;
}
