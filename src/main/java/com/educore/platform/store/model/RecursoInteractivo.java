package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;
import java.util.HashSet;

/**
 * Entidad que representa un recurso interactivo HTML5 reutilizable en cursos y/o publicado como minijuego.
 */
@Entity
@Table(name = "recursos_interactivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecursoInteractivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador", nullable = false, unique = true)
    private String identificador;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "html_url", nullable = false)
    private String htmlUrl;

    @Column(name = "imagen_portada_url")
    private String imagenPortadaUrl;



    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "recurso_etiquetas",
        joinColumns = @JoinColumn(name = "recurso_id")
    )
    @Column(name = "etiqueta")
    @Builder.Default
    private Set<String> etiquetas = new HashSet<>();

    @Column(name = "es_gratis", nullable = false)
    @Builder.Default
    private boolean esGratis = true;

    @Column(name = "coste_creditos", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer costeCreditos = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Transient
    private String resolvedHtmlUrl;

    public String getResolvedHtmlUrl() {
        return resolvedHtmlUrl != null ? resolvedHtmlUrl : htmlUrl;
    }

    public Set<String> getEtiquetas() {
        if (etiquetas == null) {
            etiquetas = new HashSet<>();
        }
        return etiquetas;
    }
}
