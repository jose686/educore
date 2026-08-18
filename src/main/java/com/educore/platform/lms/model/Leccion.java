package com.educore.platform.lms.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad de dominio que representa una lección dentro de un módulo.
 */
@Entity
@Table(name = "lecciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    /**
     * El contenido principal de la lección (texto/markdown).
     * Se usará en el futuro para alimentar sistemas de IA (RAG / Preguntas y Respuestas).
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    /**
     * Ruta al archivo de video en el servidor o CDN gestionado por module-media.
     */
    private String videoUrl;

    @Column(name = "es_video_local", nullable = true)
    private Boolean esVideoLocal;

    public Boolean getEsVideoLocal() {
        return esVideoLocal != null ? esVideoLocal : false;
    }


    /**
     * Ruta o identificador del script JS para inyectar interactividad personalizada
     * (ej. minijuegos de ajedrez).
     */
    @Column(name = "ruta_script_interactivo")
    private String rutaScriptInteractivo;

    @Column(nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @OneToMany(mappedBy = "leccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<Progreso> progresos = new java.util.ArrayList<>();
}

