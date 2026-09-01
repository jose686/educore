package com.educore.platform.lms.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio que representa un Curso en el Aula Virtual (LMS).
 * Se relaciona con los usuarios (profesores) únicamente a través de su ID numérico.
 */
@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String imagenUrl;

    @Column(nullable = false)
    private Double precio;

    /**
     * Referencia al ID del profesor (Usuario) en el módulo 'users'.
     * Mapeado como un valor básico para mantener el aislamiento entre módulos.
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("orden ASC")
    private List<Modulo> modulos = new ArrayList<>();

    public List<Modulo> getModulos() {
        if (this.modulos == null) {
            this.modulos = new ArrayList<>();
        }
        return this.modulos;
    }

    public boolean isGratis() {
        return precio == null || precio <= 0.0;
    }

    public String getImagenPortadaUrl() {
        return this.imagenUrl;
    }
}
