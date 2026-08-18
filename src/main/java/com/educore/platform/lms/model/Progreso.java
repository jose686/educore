package com.educore.platform.lms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de dominio que registra el progreso de un estudiante en una lección específica.
 * Mantiene desacoplamiento referenciando al estudiante mediante su ID numérico.
 */
@Entity
@Table(
    name = "progresos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_student_leccion",
        columnNames = {"student_id", "leccion_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia al ID del estudiante (Usuario) en el módulo 'users'.
     * Mapeado como un valor básico para mantener el aislamiento entre módulos.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leccion_id", nullable = false)
    private Leccion leccion;

    @Column(nullable = false)
    @Builder.Default
    private boolean completado = false;

    private LocalDateTime fechaCompletado;
}
